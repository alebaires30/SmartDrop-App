package com.example.smartdrop;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Voz de Gotita: la voz natural de ElevenLabs (el audio lo prepara el servidor) o, si no está, la voz del
 * teléfono. Lee frase por frase y sigue hablando aunque se cambie de pantalla.
 */
final class GotitaVoz extends UtteranceProgressListener implements TextToSpeech.OnInitListener {

    interface Escucha {
        void alEmpezarAHablar();
        void alTerminarDeHablar();
    }

    // Tono casi natural y velocidad normal: así suena más suave y de corrido.
    private static final float TONO = 1.04f;
    private static final float VELOCIDAD_NORMAL = 0.98f;
    private static final float VELOCIDAD_LENTA = 0.82f;
    // Pistas en el nombre de las voces femeninas, que suenan más suaves.
    private static final String[] VOCES_SUAVES = {"female", "sfb", "sfg", "tpf", "iob", "eea", "eec", "esd"};

    private static GotitaVoz instancia;

    private final TextToSpeech tts;
    private final Context app;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final List<String> pendientes = new ArrayList<>();
    private final Deque<Frase> colaNatural = new ArrayDeque<>();
    private MediaPlayer reproductor;
    private Frase sonandoAhora; // la frase cuyo audio se está reproduciendo
    private boolean natural;
    private boolean sonando;
    private int generacion;
    private boolean lista;
    private int enCola;
    private long contador;
    private String idioma = "es";
    private boolean lenta;
    private Escucha escucha;

    static synchronized GotitaVoz de(Context context) {
        if (instancia == null) instancia = new GotitaVoz(context.getApplicationContext());
        return instancia;
    }

    private GotitaVoz(Context app) {
        this.app = app;
        tts = new TextToSpeech(app, this);
    }

    void setEscucha(Escucha escucha) {
        this.escucha = escucha;
    }

    @Override
    public void onInit(int status) {
        principal.post(() -> {
            lista = status == TextToSpeech.SUCCESS;
            if (!lista) return;
            tts.setOnUtteranceProgressListener(this);
            aplicarVoz();
            List<String> textos = new ArrayList<>(pendientes);
            pendientes.clear();
            for (String texto : textos) decir(texto);
        });
    }

    /** Idioma ("es"/"en") y velocidad; se aplica a las frases siguientes. */
    void configurar(String idioma, boolean lenta) {
        boolean cambio = !idioma.equals(this.idioma) || lenta != this.lenta;
        this.idioma = idioma;
        this.lenta = lenta;
        if (lista && cambio) aplicarVoz();
    }

    private void aplicarVoz() {
        boolean ingles = "en".equals(idioma);
        Locale[] preferidos = ingles
                ? new Locale[]{Locale.US, Locale.UK, Locale.ENGLISH}
                : new Locale[]{new Locale("es", "US"), new Locale("es", "MX"), new Locale("es", "419"),
                        new Locale("es", "ES"), new Locale("es")};
        for (Locale locale : preferidos) {
            int resultado = tts.setLanguage(locale);
            if (resultado != TextToSpeech.LANG_MISSING_DATA && resultado != TextToSpeech.LANG_NOT_SUPPORTED) break;
        }
        Voice suave = vozSuave(ingles ? "en" : "es");
        if (suave != null) tts.setVoice(suave);
        tts.setPitch(TONO);
        tts.setSpeechRate(lenta ? VELOCIDAD_LENTA : VELOCIDAD_NORMAL);
    }

    /** Mejor voz instalada del idioma: sin internet, de buena calidad y, si se puede, femenina. */
    private Voice vozSuave(String lenguaje) {
        Set<Voice> voces;
        try {
            voces = tts.getVoices();
        } catch (RuntimeException e) {
            return null;
        }
        if (voces == null) return null;
        Voice mejor = null;
        int mejorPuntaje = -1;
        Locale actual = tts.getVoice() != null ? tts.getVoice().getLocale() : null;
        for (Voice voz : voces) {
            if (voz.getLocale() == null || !lenguaje.equals(voz.getLocale().getLanguage())) continue;
            if (voz.getFeatures() != null && voz.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                continue;
            }
            int puntaje = 0;
            if (!voz.isNetworkConnectionRequired()) puntaje += 4;
            if (voz.getQuality() >= Voice.QUALITY_HIGH) puntaje += 2;
            if (actual != null && actual.equals(voz.getLocale())) puntaje += 2;
            String nombre = voz.getName().toLowerCase(Locale.ROOT);
            for (String suave : VOCES_SUAVES) {
                if (nombre.contains(suave)) { puntaje += 3; break; }
            }
            if (puntaje > mejorPuntaje) { mejor = voz; mejorPuntaje = puntaje; }
        }
        return mejor;
    }

    /** Voz natural de ElevenLabs (desde el servidor). Si se apaga o falla, se usa la voz del teléfono. */
    void setNatural(boolean natural) {
        this.natural = natural;
    }

    void decir(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        if (natural) {
            // El audio de cada frase se pide de una vez, mientras suena la anterior: así no hay pausas.
            Frase frase = new Frase(texto);
            colaNatural.add(frase);
            enCola++;
            pedirAudio(frase, generacion);
            reproducirSiguiente();
            return;
        }
        if (!lista) { pendientes.add(texto); return; }
        enCola++;
        tts.speak(texto, TextToSpeech.QUEUE_ADD, null, "gotita-" + (contador++));
    }

    void parar() {
        pendientes.clear();
        generacion++;
        for (Frase frase : colaNatural) frase.borrar();
        colaNatural.clear();
        soltarReproductor();
        // El audio que estaba sonando tampoco se queda en la caché.
        if (sonandoAhora != null) {
            sonandoAhora.borrar();
            sonandoAhora = null;
        }
        if (lista) tts.stop();
        if (enCola > 0) {
            enCola = 0;
            if (escucha != null) escucha.alTerminarDeHablar();
        }
    }

    // ── Voz natural ──

    private static final class Frase {
        final String texto;
        File archivo;
        boolean lista;
        boolean fallo;

        Frase(String texto) {
            this.texto = texto;
        }

        void borrar() {
            if (archivo != null) archivo.delete();
            archivo = null;
        }
    }

    private void pedirAudio(Frase frase, int pedido) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("texto", frase.texto);
        cuerpo.addProperty("idioma", idioma);
        cuerpo.addProperty("lenta", lenta);
        Request peticion = new Request.Builder()
                .url(ApiClient.getBaseUrl() + "api/asistente/voz/")
                .header("Accept", "audio/mpeg")
                .post(RequestBody.create(cuerpo.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();
        ApiClient.getHttpAutenticado(app).newCall(peticion).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                principal.post(() -> listo(frase, pedido, null));
            }

            @Override
            public void onResponse(Call call, Response response) {
                File archivo = null;
                try (ResponseBody audio = response.body()) {
                    if (response.isSuccessful() && audio != null) {
                        archivo = File.createTempFile("gotita", ".mp3", app.getCacheDir());
                        try (FileOutputStream salida = new FileOutputStream(archivo)) {
                            salida.write(audio.bytes());
                        }
                    }
                } catch (IOException e) {
                    if (archivo != null) archivo.delete();
                    archivo = null;
                }
                File resultado = archivo;
                principal.post(() -> listo(frase, pedido, resultado));
            }
        });
    }

    private void listo(Frase frase, int pedido, File archivo) {
        if (pedido != generacion) {
            if (archivo != null) archivo.delete(); // Se pidió parar mientras llegaba.
            return;
        }
        frase.archivo = archivo;
        frase.fallo = archivo == null;
        frase.lista = true;
        reproducirSiguiente();
    }

    private void reproducirSiguiente() {
        if (sonando) return;
        Frase frase = colaNatural.peek();
        if (frase == null || !frase.lista) return;
        if (frase.fallo) {
            // La voz natural no respondió: lo que falta se dice con la voz del teléfono.
            natural = false;
            List<String> textos = new ArrayList<>();
            for (Frase pendiente : colaNatural) {
                textos.add(pendiente.texto);
                pendiente.borrar();
            }
            colaNatural.clear();
            enCola = Math.max(0, enCola - textos.size());
            for (String texto : textos) decir(texto);
            return;
        }
        colaNatural.poll();
        sonandoAhora = frase;
        try {
            reproductor = new MediaPlayer();
            reproductor.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            reproductor.setDataSource(frase.archivo.getAbsolutePath());
            reproductor.setOnCompletionListener(mp -> terminarFrase(frase));
            reproductor.setOnErrorListener((mp, que, extra) -> {
                terminarFrase(frase);
                return true;
            });
            reproductor.prepare();
            sonando = true;
            if (escucha != null) escucha.alEmpezarAHablar();
            reproductor.start();
        } catch (IOException | RuntimeException e) {
            terminarFrase(frase);
        }
    }

    private void terminarFrase(Frase frase) {
        soltarReproductor();
        frase.borrar();
        if (sonandoAhora == frase) sonandoAhora = null;
        unaMenos();
        reproducirSiguiente();
    }

    private void soltarReproductor() {
        sonando = false;
        if (reproductor != null) {
            try {
                reproductor.release();
            } catch (RuntimeException ignored) {
                // Ya estaba liberado.
            }
            reproductor = null;
        }
    }

    boolean hablando() {
        return enCola > 0;
    }

    @Override
    public void onStart(String id) {
        principal.post(() -> { if (escucha != null) escucha.alEmpezarAHablar(); });
    }

    @Override
    public void onDone(String id) {
        principal.post(this::unaMenos);
    }

    @Override
    public void onError(String id) {
        principal.post(this::unaMenos);
    }

    @Override
    public void onStop(String id, boolean interrumpida) {
        principal.post(this::unaMenos);
    }

    private void unaMenos() {
        if (enCola <= 0) return;
        enCola--;
        if (enCola == 0 && escucha != null) escucha.alTerminarDeHablar();
    }
}
