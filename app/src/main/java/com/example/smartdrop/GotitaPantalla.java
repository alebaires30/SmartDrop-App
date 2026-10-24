package com.example.smartdrop;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import androidx.cardview.widget.CardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lo que Gotita "ve": el nombre de la pantalla, todo su texto (también lo que está más abajo), sus partes
 * (cada título) y, cuando se pide, una captura. También abre las pantallas a las que Gotita puede llevar a la
 * persona y baja la vista hasta una parte de la pantalla.
 */
final class GotitaPantalla {

    static final String ETIQUETA_GOTITA = "gotita";
    private static final int MAX_TEXTO = 12000;
    private static final int MAX_SECCIONES = 40;
    private static final float TITULO_SP = 18f; // un texto de este tamaño o más se toma como título
    private static final int ANCHO_CAPTURA = 900;

    private static final Map<String, String> IDS = new HashMap<>();
    private static final Map<String, Class<? extends Activity>> PANTALLAS = new HashMap<>();

    static {
        IDS.put("InicioActivity", "inicio");
        IDS.put("NivelTanqueActivity", "tanque");
        IDS.put("PresionActivity", "presion");
        IDS.put("CalidadActivity", "calidad");
        IDS.put("ConsumoActivity", "consumo");
        IDS.put("AlertasActivity", "alertas");
        IDS.put("RecomendacionesActivity", "recomendaciones");
        IDS.put("RetroalimentacionActivity", "retroalimentacion");
        IDS.put("ReportarProblemaActivity", "reportar");
        IDS.put("ComunidadActivity", "comunidad");
        IDS.put("PerfilActivity", "perfil");
        IDS.put("ConfiguracionActivity", "configuracion");
        IDS.put("VincularViviendaActivity", "vincular_vivienda");
        IDS.put("AdminDashboardActivity", "panel");
        IDS.put("GraficasMonitoreoActivity", "graficas");
        IDS.put("ValvulaActivity", "valvulas");
        IDS.put("PrediccionSuministroActivity", "prediccion_suministro");
        IDS.put("PrediccionFugasActivity", "prediccion_fugas");
        IDS.put("BusquedaActivity", "buscar");

        PANTALLAS.put("inicio", InicioActivity.class);
        PANTALLAS.put("tanque", NivelTanqueActivity.class);
        PANTALLAS.put("presion", PresionActivity.class);
        PANTALLAS.put("calidad", CalidadActivity.class);
        PANTALLAS.put("consumo", ConsumoActivity.class);
        PANTALLAS.put("alertas", AlertasActivity.class);
        PANTALLAS.put("recomendaciones", RecomendacionesActivity.class);
        PANTALLAS.put("retroalimentacion", RetroalimentacionActivity.class);
        PANTALLAS.put("reportar", ReportarProblemaActivity.class);
        PANTALLAS.put("comunidad", ComunidadActivity.class);
        PANTALLAS.put("perfil", PerfilActivity.class);
        PANTALLAS.put("configuracion", ConfiguracionActivity.class);
        PANTALLAS.put("vincular_vivienda", VincularViviendaActivity.class);
        PANTALLAS.put("panel", AdminDashboardActivity.class);
        PANTALLAS.put("graficas", GraficasMonitoreoActivity.class);
        PANTALLAS.put("valvulas", ValvulaActivity.class);
        PANTALLAS.put("prediccion_suministro", PrediccionSuministroActivity.class);
        PANTALLAS.put("prediccion_fugas", PrediccionFugasActivity.class);
        PANTALLAS.put("reportes_admin", ComunidadActivity.class);
        PANTALLAS.put("buscar", BusquedaActivity.class);
    }

    private GotitaPantalla() { }

    static String id(Activity activity) {
        String nombre = activity.getClass().getSimpleName();
        if ("ComunidadActivity".equals(nombre) && activity.getIntent().getBooleanExtra("es_admin", false)) {
            return "reportes_admin";
        }
        String id = IDS.get(nombre);
        return id != null ? id : nombre.replace("Activity", "").toLowerCase(java.util.Locale.ROOT);
    }

    static String titulo(Activity activity) {
        CharSequence titulo = activity.getTitle();
        return titulo == null ? "" : titulo.toString();
    }

    /** Texto de la pantalla (sin Gotita ni lo que el usuario está escribiendo), máximo 12000 letras. */
    static String texto(Activity activity) {
        View raiz = activity.findViewById(android.R.id.content);
        StringBuilder texto = new StringBuilder();
        recorrer(raiz, texto);
        String limpio = texto.toString().replaceAll("[ \\t]+", " ").replaceAll("\\n\\s*\\n+", "\n").trim();
        return limpio.length() > MAX_TEXTO ? limpio.substring(0, MAX_TEXTO) : limpio;
    }

    private static void recorrer(View vista, StringBuilder texto) {
        if (vista == null || !vista.isShown() || ETIQUETA_GOTITA.equals(vista.getTag())) return;
        if (texto.length() > MAX_TEXTO) return;
        if (vista instanceof EditText) return;
        if (vista instanceof TextView) {
            CharSequence contenido = ((TextView) vista).getText();
            if (contenido != null && contenido.toString().trim().length() > 0) {
                texto.append(contenido.toString().trim()).append('\n');
            }
        } else if (vista.getContentDescription() != null && !(vista instanceof ViewGroup)) {
            texto.append(vista.getContentDescription()).append('\n');
        }
        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) recorrer(grupo.getChildAt(i), texto);
        }
    }

    // ── Partes de la pantalla ──

    /** Los títulos de la pantalla (id y texto) para que Gotita pueda bajar hasta ellos. Llena "vistas". */
    static JsonArray secciones(Activity activity, Map<String, View> vistas) {
        vistas.clear();
        JsonArray lista = new JsonArray();
        buscarTitulos(activity.findViewById(android.R.id.content), lista, vistas, new HashSet<>());
        return lista;
    }

    private static void buscarTitulos(View vista, JsonArray lista, Map<String, View> vistas, Set<String> usados) {
        if (vista == null || !vista.isShown() || ETIQUETA_GOTITA.equals(vista.getTag())) return;
        if (lista.size() >= MAX_SECCIONES || vista instanceof Toolbar || vista instanceof EditText) return;
        if (vista instanceof TextView) {
            TextView texto = (TextView) vista;
            String contenido = texto.getText() == null ? "" : texto.getText().toString().replaceAll("\\s+", " ").trim();
            float sp = texto.getTextSize() / vista.getResources().getDisplayMetrics().scaledDensity;
            // Un título: letra grande, corto y con palabras (no un número grande como "74 %").
            if (sp >= TITULO_SP && contenido.length() >= 3 && contenido.length() <= 60
                    && contenido.replaceAll("[^\\p{L}]", "").length() >= 3) {
                String base = Normalizer.normalize(contenido, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
                if (base.length() > 34) base = base.substring(0, 34);
                if (base.isEmpty()) base = "parte";
                String id = base;
                for (int n = 2; usados.contains(id); n++) id = base + "_" + n;
                usados.add(id);
                vistas.put(id, tarjetaDe(vista));
                JsonObject item = new JsonObject();
                item.addProperty("id", id);
                item.addProperty("titulo", contenido);
                lista.add(item);
            }
            return;
        }
        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) buscarTitulos(grupo.getChildAt(i), lista, vistas, usados);
        }
    }

    /** Si el título está dentro de una tarjeta, se resalta la tarjeta completa. */
    private static View tarjetaDe(View titulo) {
        View actual = titulo;
        while (actual.getParent() instanceof View) {
            actual = (View) actual.getParent();
            if (actual instanceof CardView) return actual; // también las MaterialCardView
            if (actual.getId() == android.R.id.content) break;
        }
        return titulo;
    }

    /** Baja (o sube) la vista hasta esa parte de la pantalla y la marca un momento. */
    static void mostrar(View parte) {
        if (parte == null || !parte.isAttachedToWindow()) return;
        parte.requestRectangleOnScreen(new Rect(0, 0, parte.getWidth(), parte.getHeight()), false);
        GradientDrawable marco = new GradientDrawable();
        marco.setColor(Color.TRANSPARENT);
        marco.setCornerRadius(12 * parte.getResources().getDisplayMetrics().density);
        marco.setStroke(Math.round(3 * parte.getResources().getDisplayMetrics().density),
                ContextCompat.getColor(parte.getContext(), R.color.anil));
        marco.setBounds(0, 0, parte.getWidth(), parte.getHeight());
        parte.getOverlay().add(marco);
        parte.postDelayed(() -> parte.getOverlay().remove(marco), 3500);
    }

    /** Foto pequeña de la pantalla actual, sin Gotita; null si no se pudo. */
    static String captura(Activity activity) {
        View raiz = activity.getWindow().getDecorView().getRootView();
        View capa = raiz.findViewWithTag(ETIQUETA_GOTITA);
        int visibilidad = capa != null ? capa.getVisibility() : View.GONE;
        try {
            if (raiz.getWidth() == 0 || raiz.getHeight() == 0) return null;
            if (capa != null) capa.setVisibility(View.INVISIBLE);
            float escala = Math.min(1f, (float) ANCHO_CAPTURA / raiz.getWidth());
            Bitmap imagen = Bitmap.createBitmap(Math.round(raiz.getWidth() * escala),
                    Math.round(raiz.getHeight() * escala), Bitmap.Config.RGB_565);
            Canvas lienzo = new Canvas(imagen);
            lienzo.drawColor(Color.WHITE);
            lienzo.scale(escala, escala);
            raiz.draw(lienzo);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            imagen.compress(Bitmap.CompressFormat.JPEG, 70, salida);
            imagen.recycle();
            return "data:image/jpeg;base64," + Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        } finally {
            if (capa != null) capa.setVisibility(visibilidad);
        }
    }

    /** Prepara la pantalla a la que Gotita quiere llevar, o null si no existe en la app. */
    static Intent intentPara(Activity desde, String destino, boolean esAdmin) {
        Class<? extends Activity> pantalla = PANTALLAS.get(destino);
        if (pantalla == null) return null;
        Intent intent = new Intent(desde, pantalla);
        if ("reportes_admin".equals(destino) || (esAdmin && ("alertas".equals(destino) || "comunidad".equals(destino)))) {
            intent.putExtra("es_admin", true);
        }
        if ("valvulas".equals(destino)) intent.putExtra("id_valvula", 1);
        return intent;
    }
}
