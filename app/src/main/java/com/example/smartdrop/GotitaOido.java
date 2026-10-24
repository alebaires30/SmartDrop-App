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
 * - VIGILANDO: escucha seguido mientras la app está abierta y solo reacciona a "Oye Gotita" al inicio de la frase.
 * - COMANDO: escucha una pregunta (botón del micrófono o justo después de "Oye Gotita").
 * Se apaga al salir de la app y vuelve a encenderse al regresar, si el interruptor está encendido.
 * Se prefiere el reconocimiento dentro del teléfono (la voz no sale a la nube); si el teléfono no lo tiene
 * para el idioma, se usa el reconocimiento normal del sistema.
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

    // Formas en que el teléfono puede entender "Gotita" ("go tita", "godita"…). Solo cuentan justo después de
    // "Oye", "Hey" o "Ey": una frase que solo menciona la palabra no despierta a Gotita.
    static final String NOMBRE = "(?:gotita|gotitas|go\\s?tita|got\\s?ita|godita|gotica)";
    private static final String LLAMADO = "(?:oye|hey|ey)[\\s,]+";
    // "Oye Gotita" tiene que ir al inicio de la frase; lo que sigue es la pregunta.
    static final Pattern DESPERTAR = Pattern.compile(
            "^[\\s¡¿\"“]*" + LLAMADO + NOMBRE + "\\b[\\s,.:;!?¡¿]*(.*)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String ALTO =
            "(?:para(?:le)?|p[aá]rale|detente|alto|silencio|c[aá]llate|c[aá]llese|basta|ya basta|ya estuvo|ya no|ya|stop|be quiet|shut up)[\\s.!]*$";
    // Para lo escrito o lo dicho después de "Oye Gotita" (el nombre es opcional).
    static final Pattern PARAR = Pattern.compile("^(?:" + NOMBRE + "[\\s,]*)?" + ALTO,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    // Con el micrófono abierto solo calla a Gotita la frase entera "(Oye) Gotita, para".
    private static final Pattern PARAR_CON_NOMBRE = Pattern.compile(
            "^[\\s¡¿\"“]*(?:" + LLAMADO + ")?" + NOMBRE + "[\\s,]*" + ALTO, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

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
    // Mientras sea true se pide reconocer sin internet; se apaga si el teléfono no puede hacerlo.
    private boolean enElTelefono = true;
    private final Runnable tiempoComando = () -> terminarComando(ultimoParcial);

    GotitaOido(Context context) {
        app = context.getApplicationContext();
    }

    /** Lo que sigue a "Oye/Hey/Ey Gotita" ("" si no dijo más), o null si la frase no empieza así. */
    static String trasDespertar(String texto) {
        if (texto == null) return null;
        Matcher coincide = DESPERTAR.matcher(texto);
        if (!coincide.matches()) return null;
        return coincide.group(1) == null ? "" : coincide.group(1).trim();
    }

    /** ¿La frase completa es "(Oye) Gotita, para" (o callate, basta…)? */
    static boolean pideParar(String texto) {
        return texto != null && PARAR_CON_NOMBRE.matcher(texto).matches();
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

    /**
     * Deja de escuchar del todo (interruptor apagado, la app pasa a segundo plano o se cierra la sesión) y suelta
     * el reconocedor del sistema; al volver a escuchar se crea otro. Se llama en el hilo principal.
     */
    void detener(boolean recordarVigilancia) {
        vigilarDespues = recordarVigilancia && vigilarDespues;
        principal.removeCallbacksAndMessages(null);
        if (modo == Modo.COMANDO && escucha != null) escucha.alCambiarEscucha(false);
        modo = Modo.APAGADO;
        detenerReconocedor();
        destruirReconocedor();
    }

    void apagarVigilancia() {
        vigilarDespues = false;
        if (modo == Modo.VIGILANDO) detener(false);
        else if (modo == Modo.APAGADO) destruirReconocedor();
    }

    void reanudar() {
        if (vigilarDespues && modo == Modo.APAGADO) vigilar();
    }

    private void arrancar() {
        if (corriendo) return;
        if (reconocedor == null) {
            reconocedor = crearReconocedor();
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
        // Que la voz se reconozca en el teléfono, sin mandarla a la nube (si el teléfono tiene el idioma).
        if (enElTelefono) intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        try {
            reconocedor.startListening(intent);
            corriendo = true;
        } catch (RuntimeException e) {
            corriendo = false;
            reiniciar(1000);
        }
    }

    /** Android 12+: el reconocedor que trabaja dentro del teléfono, si lo hay; si no, el normal del sistema. */
    private SpeechRecognizer crearReconocedor() {
        if (enElTelefono && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && SpeechRecognizer.isOnDeviceRecognitionAvailable(app)) {
            try {
                return SpeechRecognizer.createOnDeviceSpeechRecognizer(app);
            } catch (RuntimeException ignored) {
                // No se pudo crear: se usa el normal.
            }
        }
        return SpeechRecognizer.createSpeechRecognizer(app);
    }

    private void destruirReconocedor() {
        if (reconocedor == null) return;
        try {
            reconocedor.destroy();
        } catch (RuntimeException ignored) {
            // Ya estaba liberado.
        }
        reconocedor = null;
        corriendo = false;
    }

    /** Errores con los que el teléfono dice que no puede reconocer sin internet (o ese idioma). */
    static boolean faltaReconocimientoLocal(int error) {
        return error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                || error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                || error == SpeechRecognizer.ERROR_SERVER || error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED;
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
        else destruirReconocedor(); // Sin "Oye Gotita" no se queda el micrófono preparado.
    }

    private void manejarVigilancia(String texto, boolean hablando) {
        // "Gotita, para" se atiende aquí mismo: no se manda nada al servidor.
        if (pideParar(texto)) {
            if (escucha != null) escucha.alPedirParar();
            return;
        }
        // Mientras Gotita habla, su propia voz llega al micrófono: solo se atiende "Gotita, para".
        if (hablando) return;
        // Solo despierta "Oye Gotita" al inicio; una frase que nada más menciona "gotita" se ignora y no sale
        // del teléfono.
        String resto = trasDespertar(texto);
        if (resto == null) return;
        if (resto.length() > 1) {
            if (escucha != null) escucha.alOirPregunta(resto);
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
        if (enElTelefono && faltaReconocimientoLocal(error)) {
            // El teléfono no reconoce sin internet (o no tiene el idioma): se usa el reconocimiento del sistema.
            enElTelefono = false;
            destruirReconocedor();
            if (modo != Modo.APAGADO) reiniciar(200);
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
