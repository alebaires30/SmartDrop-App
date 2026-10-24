package com.example.smartdrop;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.Call;

/**
 * Gotita, la asistente de SmartDrop en la app: conversación, voz, "Oye Gotita", modo sencillo/técnico e
 * idioma (español/inglés). Es la misma Gotita en toda la app; cada pantalla la muestra con GotitaVista.
 *
 * Gotita solo responde, explica y te lleva a otras pantallas. Lo único que puede cambiar es la pantalla
 * (solo a las permitidas para la persona), la parte de la pantalla que se ve, el modo de explicar, el idioma y
 * la velocidad de su voz.
 *
 * La conversación se guarda en el servidor (20 días) con un número. Mientras la app siga abierta se sigue la
 * misma; si se cierra la app, la próxima vez empieza una nueva (las anteriores quedan en el historial).
 */
final class Gotita implements GotitaVoz.Escucha, GotitaOido.Escucha {

    interface Vista {
        Activity actividad();
        boolean panelAbierto();
        void alCambiarEstado(String estado);
        void alCambiarMensajes();
        void alCambiarAjustes();
        void alOirParcial(String texto);
        void alMostrarBurbuja(String texto, String destino, boolean esAviso);
        void alNavegar(String destino);
        void alPedirMicrofono();
        JsonArray secciones();
        void alMostrarSeccion(String id);
        void alCambiarConversacion();
    }

    static final class Mensaje {
        final String rol; // usuario · gotita · aviso · error
        String texto;
        final String destino;

        Mensaje(String rol, String texto, String destino) {
            this.rol = rol;
            this.texto = texto;
            this.destino = destino;
        }
    }

    static final String REPOSO = "reposo";
    static final String VIGILANDO = "vigilando";
    static final String ESCUCHANDO = "escuchando";
    static final String PENSANDO = "pensando";
    static final String HABLANDO = "hablando";

    private static final int MAX_CHAT = 60;
    private static final int MAX_HISTORIAL = 6;
    private static final int MAX_PREGUNTA = 600;
    private static final Pattern PIDE_PANTALLA = Pattern.compile(
            "(esta pantalla|lo que (veo|estoy viendo|sale|aparece)|qu[eé] (estoy viendo|veo|es esto|significa esto)"
                    + "|esta gr[aá]fica|este gr[aá]fico|expl[ií]ca(me)? (esto|la pantalla)|this screen"
                    + "|what am i (seeing|looking at)|this chart)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static Gotita instancia;

    private final Context app;
    private final SharedPreferences ajustes;
    private final GotitaVoz voz;
    private final GotitaOido oido;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final List<Mensaje> chat = new ArrayList<>();
    private Vista vista;
    private String estado = REPOSO;
    private Call llamada;
    private Mensaje enCurso;
    private String navegarA;
    private boolean activa = true;
    private boolean estadoConsultado;
    private boolean escuchaPendiente;
    private Integer conversacionId; // número de la conversación en el servidor (null = nueva)

    static synchronized Gotita de(Context context) {
        if (instancia == null) instancia = new Gotita(context.getApplicationContext());
        return instancia;
    }

    private Gotita(Context app) {
        this.app = app;
        ajustes = app.getSharedPreferences("gotita", Context.MODE_PRIVATE);
        voz = GotitaVoz.de(app);
        voz.setEscucha(this);
        oido = new GotitaOido(app);
        oido.setEscucha(this);
        aplicarVoz();
    }

    // ── Ajustes ──

    String modo() { return ajustes.getString("modo", "sencillo"); }
    String idioma() { return ajustes.getString("idioma", "es"); }
    boolean vozLenta() { return ajustes.getBoolean("lenta", false); }
    boolean escuchaActiva() { return ajustes.getBoolean("escucha", false); }
    boolean responderConVoz() { return ajustes.getBoolean("voz", true); }
    boolean leerAvisos() { return ajustes.getBoolean("leer_avisos", true); }
    boolean activa() { return activa; }
    String estado() { return estado; }
    List<Mensaje> mensajes() { return Collections.unmodifiableList(chat); }
    boolean microfonoDisponible() { return GotitaOido.disponible(app); }
    boolean escuchandoPregunta() { return oido.getModo() == GotitaOido.Modo.COMANDO; }
    Integer conversacionId() { return conversacionId; }

    boolean esAdmin() {
        return app.getSharedPreferences("sesion", Context.MODE_PRIVATE).getInt("id_rol", 0) == 2;
    }

    private void aplicarVoz() {
        voz.configurar(idioma(), vozLenta());
        oido.setIdioma(idioma());
    }

    void cambiarModo(String modo, boolean avisar) {
        if (!"sencillo".equals(modo) && !"tecnico".equals(modo)) return;
        ajustes.edit().putString("modo", modo).apply();
        avisarCambio();
        if (avisar) confirmar(t("tecnico".equals(modo) ? "modo_tecnico" : "modo_sencillo"));
    }

    void cambiarVelocidad(boolean lenta, boolean avisar) {
        ajustes.edit().putBoolean("lenta", lenta).apply();
        aplicarVoz();
        avisarCambio();
        if (avisar) confirmar(t(lenta ? "lenta" : "normal"));
    }

    void cambiarIdioma(String idioma, boolean avisar) {
        if (!"es".equals(idioma) && !"en".equals(idioma)) return;
        ajustes.edit().putString("idioma", idioma).apply();
        aplicarVoz();
        avisarCambio();
        if (avisar) confirmar(t("idioma_cambiado"));
    }

    void cambiarVoz(boolean activa) {
        ajustes.edit().putBoolean("voz", activa).apply();
        if (!activa) voz.parar();
        avisarCambio();
    }

    void cambiarLeerAvisos(boolean activo) {
        ajustes.edit().putBoolean("leer_avisos", activo).apply();
        avisarCambio();
    }

    /** Interruptor "Oye Gotita". Si falta el permiso del micrófono, se pide y se activa al darlo. */
    void cambiarEscucha(boolean activa, boolean tienePermiso) {
        if (activa && !tienePermiso) {
            escuchaPendiente = true;
            if (vista != null) vista.alPedirMicrofono();
            avisarCambio();
            return;
        }
        boolean cambio = activa != escuchaActiva();
        ajustes.edit().putBoolean("escucha", activa).apply();
        if (activa) oido.vigilar();
        else oido.apagarVigilancia();
        ponerEstado(estadoBase());
        avisarCambio();
        // Gotita explica para qué sirve el interruptor al encenderlo o apagarlo.
        if (cambio) confirmar(t(activa ? "escucha_encendida" : "escucha_apagada"));
    }

    /** Respuesta del permiso del micrófono (lo llama la pantalla). */
    void permisoMicrofono(boolean concedido) {
        if (concedido && escuchaPendiente) {
            escuchaPendiente = false;
            cambiarEscucha(true, true);
        } else if (concedido) {
            escucharPregunta();
        } else {
            escuchaPendiente = false;
            agregar("error", t("mic_bloqueado"), null);
            avisarCambio();
        }
    }

    private void avisarCambio() {
        if (vista != null) vista.alCambiarAjustes();
    }

    // ── Pantallas ──

    void conectar(Vista vista) {
        this.vista = vista;
        if (!estadoConsultado) {
            estadoConsultado = true;
            GotitaCliente.estado(app, (activaServidor, rol, vozNatural) -> {
                activa = activaServidor;
                voz.setNatural(vozNatural); // voz de ElevenLabs si el servidor la tiene
                avisarCambio();
            });
        }
    }

    void desconectar(Vista vista) {
        if (this.vista == vista) this.vista = null;
    }

    void reanudar(boolean tienePermiso) {
        if (escuchaActiva() && tienePermiso) oido.vigilar();
        ponerEstado(voz.hablando() ? HABLANDO : (llamada != null ? PENSANDO : estadoBase()));
    }

    /** La app pasa a segundo plano: deja de escuchar (privacidad y batería), pero termina de hablar. */
    void pausar() {
        oido.detener(true);
    }

    /** Al cerrar sesión: olvida la conversación y se calla. */
    void olvidar() {
        parar();
        oido.detener(false);
        chat.clear();
        conversacionId = null;
        estadoConsultado = false;
        activa = true;
    }

    private String estadoBase() {
        return escuchaActiva() && oido.getModo() != GotitaOido.Modo.APAGADO ? VIGILANDO : REPOSO;
    }

    private void ponerEstado(String nuevo) {
        estado = nuevo;
        if (vista != null) vista.alCambiarEstado(nuevo);
    }

    // ── Conversación ──

    private Mensaje agregar(String rol, String texto, String destino) {
        Mensaje mensaje = new Mensaje(rol, texto, destino);
        chat.add(mensaje);
        while (chat.size() > MAX_CHAT) chat.remove(0);
        if (vista != null) vista.alCambiarMensajes();
        return mensaje;
    }

    /** Borra la conversación actual (también del historial) y empieza una nueva. */
    void borrarConversacion() {
        if (conversacionId != null) GotitaCliente.borrar(app, conversacionId, json -> { });
        nuevaConversacion();
        confirmar(t("borrado"));
    }

    /** Empieza una conversación nueva; la anterior queda en el historial. */
    void nuevaConversacion() {
        parar();
        chat.clear();
        conversacionId = null;
        if (vista != null) {
            vista.alCambiarMensajes();
            vista.alCambiarConversacion();
        }
    }

    /** Sigue una conversación del historial (el JSON que manda el servidor). */
    void abrirConversacion(JsonObject datos) {
        if (datos == null || !datos.has("id") || !datos.has("mensajes")) return;
        parar();
        chat.clear();
        for (JsonElement elemento : datos.getAsJsonArray("mensajes")) {
            JsonObject m = elemento.getAsJsonObject();
            String rol = m.has("rol") && "gotita".equals(m.get("rol").getAsString()) ? "gotita" : "usuario";
            chat.add(new Mensaje(rol, m.has("texto") ? m.get("texto").getAsString() : "", null));
        }
        while (chat.size() > MAX_CHAT) chat.remove(0);
        conversacionId = datos.get("id").getAsInt();
        if (vista != null) {
            vista.alCambiarMensajes();
            vista.alCambiarConversacion();
        }
    }

    private void confirmar(String texto) {
        agregar("gotita", texto, null);
        if (vista != null && !vista.panelAbierto()) vista.alMostrarBurbuja(texto, null, false);
        if (responderConVoz()) voz.decir(texto);
    }

    void escucharPregunta() {
        if (!microfonoDisponible()) {
            agregar("error", t("sin_microfono"), null);
            return;
        }
        voz.parar();
        oido.escucharPregunta();
    }

    void parar() {
        if (llamada != null) {
            llamada.cancel();
            llamada = null;
        }
        voz.parar();
        if (oido.getModo() == GotitaOido.Modo.COMANDO) oido.detener(true);
        ponerEstado(estadoBase());
    }

    void preguntar(String texto, boolean conCaptura) {
        preguntar(texto, conCaptura, false);
    }

    /** porVoz: la pregunta llegó por el micrófono o con "Oye Gotita". */
    void preguntar(String texto, boolean conCaptura, boolean porVoz) {
        String pregunta = texto == null ? "" : texto.trim();
        if (pregunta.isEmpty()) return;
        if (pregunta.length() > MAX_PREGUNTA) pregunta = pregunta.substring(0, MAX_PREGUNTA);

        Runnable local = comandoLocal(pregunta);
        if (local != null) {
            agregar("usuario", pregunta, null);
            local.run();
            if (ESCUCHANDO.equals(estado)) ponerEstado(estadoBase());
            return;
        }

        if (llamada != null) llamada.cancel();
        voz.parar();
        JsonArray historial = new JsonArray();
        int desde = Math.max(0, chat.size() - MAX_HISTORIAL * 2);
        int incluidos = 0;
        for (int i = chat.size() - 1; i >= desde && incluidos < MAX_HISTORIAL; i--) {
            Mensaje m = chat.get(i);
            if (!"usuario".equals(m.rol) && !"gotita".equals(m.rol)) continue;
            JsonObject item = new JsonObject();
            item.addProperty("rol", m.rol);
            item.addProperty("texto", m.texto.length() > 800 ? m.texto.substring(0, 800) : m.texto);
            historial.add(item);
            incluidos++;
        }
        JsonArray ordenado = new JsonArray();
        for (int i = historial.size() - 1; i >= 0; i--) ordenado.add(historial.get(i));

        agregar("usuario", pregunta, null);
        ponerEstado(PENSANDO);

        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("texto", pregunta);
        cuerpo.addProperty("modo", modo());
        cuerpo.addProperty("idioma", idioma());
        cuerpo.add("historial", ordenado);
        // Vacío = conversación nueva; si no, el servidor sigue esa conversación.
        if (conversacionId != null) cuerpo.addProperty("conversacion", conversacionId);
        else cuerpo.add("conversacion", JsonNull.INSTANCE);
        // Con el chat cerrado, Gotita responde por voz y en la burbuja (no se abre el chat), pero queda guardado.
        if (porVoz && vista != null && !vista.panelAbierto()) cuerpo.addProperty("por_voz", true);
        Activity actividad = vista != null ? vista.actividad() : null;
        if (actividad != null) {
            JsonObject pantalla = new JsonObject();
            pantalla.addProperty("id", GotitaPantalla.id(actividad));
            pantalla.addProperty("titulo", GotitaPantalla.titulo(actividad));
            pantalla.addProperty("texto", GotitaPantalla.texto(actividad));
            pantalla.add("secciones", vista.secciones());
            cuerpo.add("pantalla", pantalla);
            if (conCaptura || PIDE_PANTALLA.matcher(pregunta).find()) {
                String captura = GotitaPantalla.captura(actividad);
                if (captura != null) cuerpo.addProperty("captura", captura);
            }
        }

        enCurso = null;
        navegarA = null;
        final Call[] esta = new Call[1];
        esta[0] = GotitaCliente.preguntar(app, cuerpo, new GotitaCliente.Eventos() {
            @Override
            public void alTexto(String frase) {
                if (llamada != esta[0]) return;
                if (enCurso == null) {
                    enCurso = agregar("gotita", frase, null);
                } else {
                    enCurso.texto = enCurso.texto + " " + frase;
                    if (vista != null) vista.alCambiarMensajes();
                }
                if (vista != null && !vista.panelAbierto()) vista.alMostrarBurbuja(enCurso.texto, null, false);
                if (responderConVoz()) voz.decir(frase);
            }

            @Override
            public void alAccion(JsonObject accion) {
                if (llamada != esta[0]) return;
                ejecutarAccion(accion);
            }

            @Override
            public void alError(String mensaje) {
                if (llamada != esta[0]) return;
                llamada = null;
                String texto = mensaje != null && !mensaje.isEmpty() ? mensaje : t("sin_conexion");
                agregar("error", texto, null);
                if (vista != null && !vista.panelAbierto()) vista.alMostrarBurbuja(texto, null, false);
                ponerEstado(estadoBase());
            }

            @Override
            public void alConversacion(int id) {
                if (llamada != esta[0]) return;
                conversacionId = id;
                if (vista != null) vista.alCambiarConversacion();
            }

            @Override
            public void alFin() {
                if (llamada != esta[0]) return;
                llamada = null;
                if (PENSANDO.equals(estado)) ponerEstado(voz.hablando() ? HABLANDO : estadoBase());
                if (navegarA != null && vista != null) {
                    String destino = navegarA;
                    navegarA = null;
                    vista.alNavegar(destino);
                }
            }
        });
        llamada = esta[0];
    }

    private void ejecutarAccion(JsonObject accion) {
        String tipo = accion.has("tipo") ? accion.get("tipo").getAsString() : "";
        switch (tipo) {
            case "modo":
                cambiarModo(accion.get("modo").getAsString(), false);
                break;
            case "velocidad":
                cambiarVelocidad("lenta".equals(accion.get("velocidad").getAsString()), false);
                break;
            case "idioma":
                cambiarIdioma(accion.get("idioma").getAsString(), false);
                break;
            case "navegar":
                if (accion.has("destino")) navegarA = accion.get("destino").getAsString();
                break;
            case "seccion":
                if (accion.has("seccion") && vista != null) vista.alMostrarSeccion(accion.get("seccion").getAsString());
                break;
            default:
                break;
        }
    }

    // ── Órdenes que Gotita entiende al instante, sin preguntarle a Claude ──

    private static boolean es(String texto, String patron) {
        return Pattern.compile("^(?:" + GotitaOido.NOMBRE + "[\\s,]*)?" + patron + "$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(texto).matches();
    }

    private Runnable comandoLocal(String texto) {
        String t = texto.trim().replaceAll("[.!¡]+$", "");
        String pedir = "(?:(?:pon(?:te|le)?|cambia(?:r)?(?: a)?|activa(?:r)?|usa(?:r)?)\\s+)?(?:el\\s+)?";
        String habla = "(?:(?:h[aá]blame|habla(?:me)?|resp[oó]nde(?:me)?|cambia(?:r)?|p[aá]sate|pon(?:te)?)\\s+)?(?:(?:en|a|al)\\s+)?";
        if (GotitaOido.PARAR.matcher(t).matches()) return this::parar;
        if (es(t, pedir + "modo\\s+t[eé]cnico") || es(t, "technical mode")) return () -> cambiarModo("tecnico", true);
        if (es(t, pedir + "modo\\s+(?:sencillo|simple|f[aá]cil)") || es(t, "simple mode")) {
            return () -> cambiarModo("sencillo", true);
        }
        if (es(t, "(?:habla(?:me)?\\s+)?m[aá]s\\s+(?:lento|despacio)") || es(t, "(?:speak\\s+)?slower")) {
            return () -> cambiarVelocidad(true, true);
        }
        if (es(t, "(?:velocidad\\s+normal|habla(?:me)?\\s+normal|m[aá]s\\s+r[aá]pido)") || es(t, "(?:normal speed|faster)")) {
            return () -> cambiarVelocidad(false, true);
        }
        if (es(t, habla + "ingl[eé]s") || es(t, "(?:(?:speak|talk|answer)(?: to me)?(?: in)?\\s+)?english")) {
            return () -> cambiarIdioma("en", true);
        }
        if (es(t, habla + "espa[nñ]ol") || es(t, "(?:(?:speak|talk|answer)(?: to me)?(?: in)?\\s+)?spanish")) {
            return () -> cambiarIdioma("es", true);
        }
        return null;
    }

    // ── Avisos de sensores y fugas ──

    /** Si la app está abierta, Gotita cuenta el aviso en su burbuja y, si se pidió, en voz alta. */
    static void anunciarAviso(Context context, String texto, String destino) {
        if (texto == null || texto.trim().isEmpty()) return;
        Gotita gotita;
        synchronized (Gotita.class) {
            gotita = instancia; // Si nunca se abrió una pantalla, la app está cerrada: basta la notificación.
        }
        if (gotita == null) return;
        gotita.principal.post(() -> {
            if (gotita.vista == null) return; // App cerrada: basta con la notificación.
            gotita.agregar("aviso", texto, destino);
            if (!gotita.vista.panelAbierto()) gotita.vista.alMostrarBurbuja(texto, destino, true);
            boolean ocupada = PENSANDO.equals(gotita.estado) || ESCUCHANDO.equals(gotita.estado);
            if (gotita.leerAvisos() && gotita.responderConVoz() && !ocupada) gotita.voz.decir(texto);
        });
    }

    // ── Voz y oído ──

    @Override
    public void alEmpezarAHablar() {
        ponerEstado(HABLANDO);
    }

    @Override
    public void alTerminarDeHablar() {
        if (HABLANDO.equals(estado)) ponerEstado(llamada != null ? PENSANDO : estadoBase());
    }

    @Override
    public void alOirParcial(String texto) {
        if (vista != null) vista.alOirParcial(texto);
    }

    @Override
    public void alOirPregunta(String texto) {
        if (vista != null) vista.alOirParcial(null);
        if (texto == null || texto.trim().isEmpty()) {
            // Se tocó el micrófono pero no llegó nada: se dice en vez de quedarse callada.
            agregar("error", t("no_te_escuche"), null);
            if (vista != null && !vista.panelAbierto()) vista.alMostrarBurbuja(t("no_te_escuche"), null, false);
            ponerEstado(estadoBase());
            return;
        }
        preguntar(texto, false, true);
    }

    @Override
    public void alDespertar() {
        voz.parar();
        if (vista != null && !vista.panelAbierto()) vista.alMostrarBurbuja(t("te_escucho"), null, false);
    }

    @Override
    public void alPedirParar() {
        parar();
    }

    @Override
    public void alSinPermiso() {
        ajustes.edit().putBoolean("escucha", false).apply();
        agregar("error", t("mic_bloqueado"), null);
        ponerEstado(REPOSO);
        avisarCambio();
    }

    @Override
    public void alCambiarEscucha(boolean escuchandoPregunta) {
        ponerEstado(escuchandoPregunta ? ESCUCHANDO : (llamada != null ? PENSANDO : estadoBase()));
    }

    // ── Textos (en el idioma en que habla Gotita) ──

    private static final Map<String, String> ES = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String[]> SUGERENCIAS_ES = new HashMap<>();
    private static final Map<String, String[]> SUGERENCIAS_EN = new HashMap<>();

    static {
        ES.put(REPOSO, "Lista para ayudarte");
        ES.put(VIGILANDO, "Di “Oye Gotita” cuando me necesites");
        ES.put(ESCUCHANDO, "Te escucho…");
        ES.put(PENSANDO, "Pensando…");
        ES.put(HABLANDO, "Hablando…");
        ES.put("bienvenida", "Hola, soy Gotita. Puedo explicarte lo que ves, contarte cómo está tu agua y llevarte a otra pantalla. Escríbeme, toca el micrófono o di “Oye Gotita”.");
        ES.put("bienvenida_admin", "Hola, soy Gotita. Puedo resumirte alertas, fugas y reportes, explicarte cualquier pantalla y llevarte a donde necesites.");
        ES.put("sin_microfono", "Este teléfono no tiene reconocimiento de voz disponible. Puedes escribirme.");
        ES.put("mic_bloqueado", "Necesito permiso para usar el micrófono. Puedes darlo en los ajustes de la app.");
        ES.put("sin_conexion", "No me pude conectar. Revisa tu internet e inténtalo de nuevo.");
        ES.put("explicar_pantalla", "Explícame esta pantalla");
        ES.put("modo_tecnico", "Listo, te explico en modo técnico.");
        ES.put("modo_sencillo", "Listo, te explico en modo sencillo.");
        ES.put("lenta", "Listo, hablaré más despacio.");
        ES.put("normal", "Listo, vuelvo a la velocidad normal.");
        ES.put("idioma_cambiado", "Listo, ahora te hablo en español.");
        ES.put("te_escucho", "Te escucho.");
        ES.put("borrado", "Listo, empezamos de nuevo.");
        ES.put("escucha_encendida", "Listo: mientras la app esté abierta te escucho. Di “Oye Gotita” y tu pregunta, por ejemplo: “Oye Gotita, ¿cómo está mi tanque?”.");
        ES.put("escucha_apagada", "Listo, ya no escucho. Puedes hablarme con el botón del micrófono o escribirme.");
        ES.put("no_te_escuche", "No te escuché. Revisa que el micrófono no esté tapado ni en silencio y vuelve a intentarlo.");
        ES.put("escribe", "Escribe tu pregunta…");
        ES.put("ver", "Ver");
        ES.put("autor", "Gotita");
        ES.put("tu", "Tú");
        ES.put("aviso", "Aviso");
        ES.put("historial", "Conversaciones");
        ES.put("nueva", "Nueva conversación");
        ES.put("sin_historial", "Aún no tienes conversaciones guardadas.");
        ES.put("nota_historial", "Se borran solas a los 20 días.");
        ES.put("borrar_todo", "Borrar todo");
        ES.put("confirmar_borrar_todo", "¿Borrar todas tus conversaciones con Gotita? No se pueden recuperar.");
        ES.put("borrar_una", "Borrar conversación");
        ES.put("historial_error", "No pude cargar el historial. Revisa tu internet.");
        ES.put("ayer", "Ayer");
        ES.put("si", "Sí, borrar");
        ES.put("no", "Cancelar");

        EN.put(REPOSO, "Ready to help");
        EN.put(VIGILANDO, "Say “Hey Gotita” whenever you need me");
        EN.put(ESCUCHANDO, "I'm listening…");
        EN.put(PENSANDO, "Thinking…");
        EN.put(HABLANDO, "Speaking…");
        EN.put("bienvenida", "Hi, I'm Gotita. I can explain what you see, tell you how your water is doing and take you to another screen. Type, tap the microphone or say “Hey Gotita”.");
        EN.put("bienvenida_admin", "Hi, I'm Gotita. I can summarize alerts, leaks and reports, explain any screen and take you wherever you need.");
        EN.put("sin_microfono", "Voice recognition isn't available on this phone. You can type to me.");
        EN.put("mic_bloqueado", "I need permission to use the microphone. You can allow it in the app settings.");
        EN.put("sin_conexion", "I couldn't connect. Check your internet and try again.");
        EN.put("explicar_pantalla", "Explain this screen");
        EN.put("modo_tecnico", "Done, I'll explain things in technical mode.");
        EN.put("modo_sencillo", "Done, I'll explain things in simple mode.");
        EN.put("lenta", "Done, I'll speak more slowly.");
        EN.put("normal", "Done, back to normal speed.");
        EN.put("idioma_cambiado", "Done, I'll speak English now.");
        EN.put("te_escucho", "I'm listening.");
        EN.put("borrado", "Done, let's start over.");
        EN.put("escucha_encendida", "Done: while the app is open I am listening. Say “Hey Gotita” and your question, for example: “Hey Gotita, how is my tank?”.");
        EN.put("escucha_apagada", "Done, I'm no longer listening. You can talk to me with the microphone button or type.");
        EN.put("no_te_escuche", "I didn't hear you. Check that the microphone isn't covered or muted and try again.");
        EN.put("escribe", "Type your question…");
        EN.put("ver", "View");
        EN.put("autor", "Gotita");
        EN.put("tu", "You");
        EN.put("aviso", "Notice");
        EN.put("historial", "Conversations");
        EN.put("nueva", "New conversation");
        EN.put("sin_historial", "You don't have saved conversations yet.");
        EN.put("nota_historial", "They are deleted automatically after 20 days.");
        EN.put("borrar_todo", "Delete all");
        EN.put("confirmar_borrar_todo", "Delete all your conversations with Gotita? They cannot be recovered.");
        EN.put("borrar_una", "Delete conversation");
        EN.put("historial_error", "I couldn't load the history. Check your internet.");
        EN.put("ayer", "Yesterday");
        EN.put("si", "Yes, delete");
        EN.put("no", "Cancel");

        SUGERENCIAS_ES.put("inicio", new String[]{"¿Cómo está mi agua hoy?", "¿Tengo alguna alerta?"});
        SUGERENCIAS_ES.put("tanque", new String[]{"¿Cuánta agua me queda?", "¿Qué significa este porcentaje?"});
        SUGERENCIAS_ES.put("presion", new String[]{"¿La presión está bien?", "¿Qué es la presión del agua?"});
        SUGERENCIAS_ES.put("calidad", new String[]{"¿El TDS está dentro del rango?", "¿Qué es el TDS?"});
        SUGERENCIAS_ES.put("consumo", new String[]{"¿Cuánto gasté hoy?", "¿Cómo puedo ahorrar agua?"});
        SUGERENCIAS_ES.put("retroalimentacion", new String[]{"¿Gasto más que otros hogares?"});
        SUGERENCIAS_ES.put("recomendaciones", new String[]{"Dame un consejo para ahorrar"});
        SUGERENCIAS_ES.put("alertas", new String[]{"Resúmeme mis alertas"});
        SUGERENCIAS_ES.put("comunidad", new String[]{"¿Qué problemas hay en mi zona?"});
        SUGERENCIAS_ES.put("panel", new String[]{"Resumen de alertas y reportes de hoy", "¿Hay posibles fugas?"});
        SUGERENCIAS_ES.put("graficas", new String[]{"¿Qué muestran estas gráficas?"});
        SUGERENCIAS_ES.put("valvulas", new String[]{"¿Qué muestra esta pantalla?"});
        SUGERENCIAS_ES.put("prediccion_fugas", new String[]{"¿Qué viviendas tienen posible fuga?"});
        SUGERENCIAS_ES.put("prediccion_suministro", new String[]{"¿Qué zona tiene más riesgo?"});
        SUGERENCIAS_ES.put("reportes_admin", new String[]{"¿Cuántos reportes hay pendientes?"});
        SUGERENCIAS_ES.put("otro", new String[]{"¿Qué puedo hacer aquí?"});

        SUGERENCIAS_EN.put("inicio", new String[]{"How is my water today?", "Do I have any alerts?"});
        SUGERENCIAS_EN.put("tanque", new String[]{"How much water is left?", "What does this percentage mean?"});
        SUGERENCIAS_EN.put("presion", new String[]{"Is the pressure OK?", "What is water pressure?"});
        SUGERENCIAS_EN.put("calidad", new String[]{"Is TDS within range?", "What is TDS?"});
        SUGERENCIAS_EN.put("consumo", new String[]{"How much did I use today?", "How can I save water?"});
        SUGERENCIAS_EN.put("retroalimentacion", new String[]{"Do I use more than other homes?"});
        SUGERENCIAS_EN.put("recomendaciones", new String[]{"Give me a water-saving tip"});
        SUGERENCIAS_EN.put("alertas", new String[]{"Summarize my alerts"});
        SUGERENCIAS_EN.put("comunidad", new String[]{"What problems are there in my area?"});
        SUGERENCIAS_EN.put("panel", new String[]{"Summary of today's alerts and reports", "Are there possible leaks?"});
        SUGERENCIAS_EN.put("graficas", new String[]{"What do these charts show?"});
        SUGERENCIAS_EN.put("valvulas", new String[]{"What does this screen show?"});
        SUGERENCIAS_EN.put("prediccion_fugas", new String[]{"Which homes may have a leak?"});
        SUGERENCIAS_EN.put("prediccion_suministro", new String[]{"Which zone has the highest risk?"});
        SUGERENCIAS_EN.put("reportes_admin", new String[]{"How many reports are pending?"});
        SUGERENCIAS_EN.put("otro", new String[]{"What can I do here?"});
    }

    String t(String clave) {
        Map<String, String> textos = "en".equals(idioma()) ? EN : ES;
        String texto = textos.get(clave);
        return texto != null ? texto : ES.getOrDefault(clave, clave);
    }

    String[] sugerencias(String pantalla) {
        Map<String, String[]> lista = "en".equals(idioma()) ? SUGERENCIAS_EN : SUGERENCIAS_ES;
        String[] propias = lista.get(pantalla);
        return propias != null ? propias : lista.get("otro");
    }
}
