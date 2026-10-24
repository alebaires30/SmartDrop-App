package com.example.smartdrop;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oído de Gotita (reconocimiento de voz del teléfono). Dos modos:
 * - VIGILANDO: escucha seguido mientras la app está abierta y solo reacciona a "Oye Gotita".
 * - COMANDO: escucha una pregunta (botón del micrófono o justo después de "Oye Gotita").
 * Se apaga al salir de la app y vuelve a encenderse al regresar, si el interruptor está encendido.
 */
final class GotitaOido implements RecognitionListener {

    interface Escucha {
        void alOirParcial(String texto);
        void alOirPregunta(String texto);
        void alDespertar();
        void alPedirParar();
        void alSinPermiso();
        void alCambiarEscucha(boolean escuchandoPregunta);
    }

    enum Modo { APAGADO, VIGILANDO, COMANDO }

    // Formas en que el teléfono puede entender "Gotita" en español o inglés ("go tita", "godita"…).
    static final String NOMBRE = "(?:gotita|gotitas|go\\s?tita|got\\s?ita|got it a|godita|gotica|cotita)";
    static final Pattern DESPERTAR = Pattern.compile(
            "(?:^|[\\s,.¡!¿?])(?:(?:hey|oye|oiga|ok|okay|okey|hola|ey|ei|hi|mira|mirá|vaya|ve)[\\s,]+)?" + NOMBRE
                    + "\\b[\\s,.:;!?¡¿]*(.*)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static final Pattern PARAR = Pattern.compile(
            "^(?:" + NOMBRE + "[\\s,]*)?(?:para(?:le)?|p[aá]rale|detente|alto|silencio|c[aá]llate|c[aá]llese|basta|ya basta|ya estuvo|ya no|ya|stop|be quiet|shut up)[\\s.!]*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final Context app;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private SpeechRecognizer reconocedor;
    private Escucha escucha;
    private Modo modo = Modo.APAGADO;
    private boolean vigilarDespues;
    private boolean corriendo;
    private int reintentos;
    private String idioma = "es";
    private String ultimoParcial = "";
    private final Runnable tiempoComando = () -> terminarComando(ultimoParcial);

    GotitaOido(Context context) {
        app = context.getApplicationContext();
    }

    static boolean disponible(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context.getApplicationContext());
    }

    void setEscucha(Escucha escucha) {
        this.escucha = escucha;
    }

    void setIdioma(String idioma) {
        if (idioma.equals(this.idioma)) return;
        this.idioma = idioma;
        if (corriendo) reiniciar(200);
    }

    Modo getModo() {
        return modo;
    }

    /** Escucha continua de "Oye Gotita". */
    void vigilar() {
        vigilarDespues = true;
        if (modo == Modo.COMANDO) return;
        modo = Modo.VIGILANDO;
        arrancar();
    }

    /** Escucha una pregunta ahora (botón del micrófono). */
    void escucharPregunta() {
        modo = Modo.COMANDO;
        ultimoParcial = "";
        if (escucha != null) escucha.alCambiarEscucha(true);
        detenerReconocedor();
        arrancar();
        principal.removeCallbacks(tiempoComando);
        principal.postDelayed(tiempoComando, 10000);
    }

    /** Deja de escuchar del todo (interruptor apagado o la app pasa a segundo plano). */
    void detener(boolean recordarVigilancia) {
        vigilarDespues = recordarVigilancia && vigilarDespues;
        principal.removeCallbacksAndMessages(null);
        if (modo == Modo.COMANDO && escucha != null) escucha.alCambiarEscucha(false);
        modo = Modo.APAGADO;
        detenerReconocedor();
    }

    void apagarVigilancia() {
        vigilarDespues = false;
        if (modo == Modo.VIGILANDO) detener(false);
    }

    void reanudar() {
        if (vigilarDespues && modo == Modo.APAGADO) vigilar();
    }

    private void arrancar() {
        if (corriendo) return;
        if (reconocedor == null) {
            reconocedor = SpeechRecognizer.createSpeechRecognizer(app);
            reconocedor.setRecognitionListener(this);
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Entiende español e inglés: primero el idioma elegido y también el otro.
        // En teléfonos con Android 14 o más nuevo, además reconoce solo cuál de los dos se está hablando.
        String principalIdioma = "en".equals(idioma) ? "en-US" : "es-US";
        String otroIdioma = "en".equals(idioma) ? "es-US" : "en-US";
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, principalIdioma);
        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{otroIdioma});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ArrayList<String> idiomas = new ArrayList<>();
            idiomas.add(principalIdioma);
            idiomas.add(otroIdioma);
            intent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true);
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, idiomas);
        }
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, app.getPackageName());
        try {
            reconocedor.startListening(intent);
            corriendo = true;
        } catch (RuntimeException e) {
            corriendo = false;
            reiniciar(1000);
        }
    }

    private void detenerReconocedor() {
        if (reconocedor != null) {
            try {
                reconocedor.cancel();
            } catch (RuntimeException ignored) {
                // Ya estaba detenido.
            }
        }
        corriendo = false;
    }

    private void reiniciar(long espera) {
        principal.postDelayed(() -> {
            if (modo != Modo.APAGADO && !corriendo) arrancar();
        }, espera);
    }

    private void terminarComando(String texto) {
        principal.removeCallbacks(tiempoComando);
        if (modo != Modo.COMANDO) return;
        detenerReconocedor();
        modo = vigilarDespues ? Modo.VIGILANDO : Modo.APAGADO;
        if (escucha != null) {
            escucha.alCambiarEscucha(false);
            escucha.alOirPregunta(texto == null ? "" : texto.trim());
        }
        if (modo == Modo.VIGILANDO) reiniciar(600);
    }

    private void manejarVigilancia(String texto, boolean hablando) {
        Matcher coincide = DESPERTAR.matcher(texto);
        if (!coincide.find()) return;
        String resto = coincide.group(1) == null ? "" : coincide.group(1).trim();
        if (hablando) {
            // Mientras Gotita habla, su propia voz llega al micrófono: solo se atiende "Gotita, para".
            if (!resto.isEmpty() && PARAR.matcher(resto).matches() && escucha != null) escucha.alPedirParar();
            return;
        }
        if (resto.length() > 1) {
            if (PARAR.matcher(resto).matches()) {
                if (escucha != null) escucha.alPedirParar();
            } else if (escucha != null) {
                escucha.alOirPregunta(resto);
            }
            return;
        }
        if (escucha != null) escucha.alDespertar();
        escucharPregunta();
    }

    private static String primero(Bundle resultados) {
        if (resultados == null) return "";
        ArrayList<String> textos = resultados.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return textos == null || textos.isEmpty() || textos.get(0) == null ? "" : textos.get(0);
    }

    // ── Lo que avisa el micrófono ──

    @Override
    public void onResults(Bundle resultados) {
        corriendo = false;
        reintentos = 0;
        String texto = primero(resultados);
        if (modo == Modo.COMANDO) {
            terminarComando(texto.isEmpty() ? ultimoParcial : texto);
        } else if (modo == Modo.VIGILANDO) {
            manejarVigilancia(texto, GotitaVoz.de(app).hablando());
            if (modo == Modo.VIGILANDO) reiniciar(300);
        }
    }

    @Override
    public void onPartialResults(Bundle parciales) {
        String texto = primero(parciales);
        if (texto.isEmpty()) return;
        if (modo == Modo.COMANDO) {
            ultimoParcial = texto;
            if (escucha != null) escucha.alOirParcial(texto);
        } else if (modo == Modo.VIGILANDO && GotitaVoz.de(app).hablando()) {
            manejarVigilancia(texto, true);
        }
    }

    @Override
    public void onError(int error) {
        corriendo = false;
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            detener(false);
            if (escucha != null) escucha.alSinPermiso();
            return;
        }
        if (modo == Modo.COMANDO) {
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    || reintentos >= 2) {
                terminarComando(ultimoParcial);
            } else {
                reintentos++;
                reiniciar(400);
            }
            return;
        }
        if (modo == Modo.VIGILANDO) {
            reintentos = Math.min(reintentos + 1, 10);
            boolean silencio = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
            reiniciar(silencio ? 250 : Math.min(500L * reintentos, 5000));
        }
    }

    @Override public void onReadyForSpeech(Bundle params) { }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }
    @Override public void onEvent(int eventType, Bundle params) { }
}
