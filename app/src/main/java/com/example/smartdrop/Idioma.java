package com.example.smartdrop;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.LayoutInflaterCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Idioma de la interfaz (preferencia "Idioma español" del perfil, la misma de la web).
 *
 * En inglés, cada vista de texto se traduce al crearse y cada vez que el código le cambia el texto, así
 * que las pantallas no necesitan llamar a la traducción una por una. Toasts, diálogos sin vista propia,
 * notificaciones y gráficas usan {@link #t}. El catálogo viene del servidor y la app trae una copia.
 */
public final class Idioma {

    public static final String ESPANOL = "es";
    public static final String INGLES = "en";
    private static final String PREFS_CATALOGO = "idioma_catalogo";
    private static final String ASSET_CATALOGO = "i18n_en.json";

    private static Traductor traductor;
    private static boolean traduciendo;
    /** Contexto de la app para traducir donde no hay uno a mano (gráficas, utilidades). */
    private static Context app;

    private Idioma() { }

    public static String actual(Context context) {
        app = context.getApplicationContext();
        return app.getSharedPreferences("sesion", Context.MODE_PRIVATE)
                .getString("idioma", ESPANOL);
    }

    public static boolean enIngles(Context context) {
        return INGLES.equals(actual(context));
    }

    /** Texto en el idioma elegido (sin cambios en español o si no está en el catálogo). */
    public static String t(Context context, CharSequence texto) {
        if (texto == null) return null;
        String original = texto.toString();
        if (!enIngles(context)) return original;
        Traductor activo = traductor(context);
        return activo == null ? original : activo.traducir(original);
    }

    /** Igual que {@link #t(Context, CharSequence)} con el contexto de la app (gráficas y utilidades sin contexto). */
    public static String t(CharSequence texto) {
        return app == null ? (texto == null ? null : texto.toString()) : t(app, texto);
    }

    public static void toast(Context context, CharSequence mensaje, int duracion) {
        Toast.makeText(context, t(context, mensaje), duracion).show();
    }

    /** Guarda el idioma y vuelve a crear la pantalla para mostrarla traducida. */
    public static void cambiar(Activity activity, String codigo) {
        String idioma = INGLES.equals(codigo) ? INGLES : ESPANOL;
        if (idioma.equals(actual(activity))) return;
        activity.getApplicationContext().getSharedPreferences("sesion", Context.MODE_PRIVATE)
                .edit().putString("idioma", idioma).apply();
        if (INGLES.equals(idioma)) actualizarCatalogo(activity, null);
        activity.recreate();
    }

    /** Cierra la sesión del teléfono conservando el idioma y el tema (la web los recuerda igual). */
    public static void limpiarSesion(Context context) {
        android.content.SharedPreferences sesion = context.getApplicationContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String idioma = sesion.getString("idioma", ESPANOL);
        boolean oscuro = sesion.getBoolean("modo_oscuro", false);
        sesion.edit().clear().putString("idioma", idioma).putBoolean("modo_oscuro", oscuro).apply();
    }

    // ── Catálogo ─────────────────────────────────────────────────────────────

    private static synchronized Traductor traductor(Context context) {
        if (traductor != null) return traductor;
        String json = context.getApplicationContext().getSharedPreferences(PREFS_CATALOGO, Context.MODE_PRIVATE)
                .getString(INGLES, null);
        if (json == null) json = leerAsset(context);
        try {
            traductor = json == null ? null : Traductor.desdeJson(json);
        } catch (RuntimeException e) {
            traductor = null;
        }
        return traductor;
    }

    private static String leerAsset(Context context) {
        try (InputStream entrada = context.getAssets().open(ASSET_CATALOGO);
             BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            StringBuilder texto = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) texto.append(linea).append('\n');
            return texto.toString();
        } catch (IOException e) {
            return null;
        }
    }

    /** Descarga la versión más reciente del catálogo (si falla, se sigue usando la copia local). */
    public static void actualizarCatalogo(Context context, @Nullable Runnable listo) {
        Context app = context.getApplicationContext();
        ApiClient.getClient().create(ApiService.class).obtenerCatalogo(INGLES).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try (ResponseBody cuerpo = response.body()) {
                    if (!response.isSuccessful() || cuerpo == null) return;
                    String json = cuerpo.string();
                    Traductor nuevo = Traductor.desdeJson(json);
                    app.getSharedPreferences(PREFS_CATALOGO, Context.MODE_PRIVATE).edit().putString(INGLES, json).apply();
                    synchronized (Idioma.class) {
                        traductor = nuevo;
                    }
                    if (listo != null) listo.run();
                } catch (IOException | RuntimeException ignored) {
                    // Catálogo inválido o sin red: se mantiene el anterior.
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { }
        });
    }

    // ── Traducción automática de las vistas ──────────────────────────────────

    /** Se llama antes de super.onCreate(): traduce cada vista de texto que se infla en la pantalla. */
    static void instalar(AppCompatActivity activity) {
        if (!enIngles(activity)) return;
        LayoutInflater inflater = activity.getLayoutInflater();
        if (inflater.getFactory2() != null) return;
        LayoutInflaterCompat.setFactory2(inflater, new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
                View vista = activity.getDelegate().createView(parent, name, context, attrs);
                if (vista instanceof TextView) observar((TextView) vista);
                return vista;
            }

            @Override
            public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
                return onCreateView(null, name, context, attrs);
            }
        });
    }

    /** Traduce un árbol de vistas ya creado (vistas armadas en código o que no pasaron por la fábrica). */
    public static void traducirArbol(View vista) {
        if (vista == null || !enIngles(vista.getContext())) return;
        if (vista instanceof TextView) observar((TextView) vista);
        CharSequence descripcion = vista.getContentDescription();
        if (descripcion != null) {
            String traducida = t(vista.getContext(), descripcion);
            if (!traducida.contentEquals(descripcion)) vista.setContentDescription(traducida);
        }
        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) traducirArbol(grupo.getChildAt(i));
        }
    }

    /** Traduce el texto actual y los cambios futuros de la vista (nunca lo que escribe el usuario). */
    static void observar(TextView vista) {
        Context context = vista.getContext();
        if (vista.getHint() != null) vista.setHint(t(context, vista.getHint()));
        if (vista instanceof EditText || vista.getTag(R.id.tag_traductor) != null) return;
        vista.setTag(R.id.tag_traductor, Boolean.TRUE);
        traducirTexto(vista);
        vista.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                traducirTexto(vista);
            }
        });
    }

    private static void traducirTexto(TextView vista) {
        if (traduciendo) return;
        CharSequence texto = vista.getText();
        if (texto == null || texto.length() == 0) return;
        String traducido = t(vista.getContext(), texto);
        if (traducido.contentEquals(texto)) return;
        traduciendo = true;
        try {
            vista.setText(traducido);
        } finally {
            traduciendo = false;
        }
    }

    /** True si la pantalla se creó con otro idioma (para recrearla al volver a ella). */
    static boolean cambioDesde(String idiomaAlCrear, Context context) {
        return idiomaAlCrear != null && !idiomaAlCrear.equals(actual(context));
    }
}
