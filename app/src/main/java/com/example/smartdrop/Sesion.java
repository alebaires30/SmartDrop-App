package com.example.smartdrop;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Datos de la sesión guardados en el teléfono. */
public final class Sesion {

    private static final String PREFS = "sesion";
    // Los tokens se guardan cifrados (AES-GCM) con una clave del almacén de claves de Android, que no sale
    // del teléfono. Las versiones anteriores los guardaban sin cifrar en "access_token" y "refresh_token":
    // esos valores se borran y cuentan como sesión cerrada (la app siempre empieza en el inicio de sesión).
    private static final String ACCESS = "access_cifrado";
    private static final String REFRESH = "refresh_cifrado";
    private static final String[] CLAVES_ANTIGUAS = {"access_token", "refresh_token"};
    private static final String ALIAS_CLAVE = "smartdrop_sesion";
    private static final String CIFRADO = "AES/GCM/NoPadding";

    // Tokens ya descifrados: así no se consulta el almacén de claves en cada petición.
    private static final Map<String, String> enMemoria = new HashMap<>();

    private Sesion() { }

    public static synchronized String accessToken(Context context) {
        return leer(context, ACCESS);
    }

    public static synchronized String refreshToken(Context context) {
        return leer(context, REFRESH);
    }

    /** True si hay una sesión iniciada en este teléfono. */
    public static boolean activa(Context context) {
        return !accessToken(context).isEmpty();
    }

    /** Guarda los tokens que devuelve el inicio de sesión. */
    public static synchronized void guardarTokens(Context context, String access, String refresh) {
        guardar(context, ACCESS, access);
        guardar(context, REFRESH, refresh);
    }

    /** Guarda el token de acceso renovado. */
    public static synchronized void guardarAccess(Context context, String access) {
        guardar(context, ACCESS, access);
    }

    /**
     * Cierra la sesión: corta el tiempo real, borra los datos del teléfono y pide al servidor que invalide
     * el token de renovación (sin esperar respuesta ni mostrar errores).
     */
    public static void cerrar(Context context) {
        Context app = context.getApplicationContext();
        String refresh = refreshToken(app); // antes de borrarlo
        RealtimeClient.disconnect();
        limpiar(app);
        if (refresh.isEmpty()) return;

        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("refresh", refresh);
        ApiClient.getClient().create(ApiService.class).cerrarSesion(cuerpo).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // En el teléfono la sesión ya está cerrada; el token vence solo en el servidor.
            }
        });
    }

    /**
     * El servidor rechazó el token de renovación (vencido, cerrado o cuenta desactivada): se borra la sesión
     * y, si la app está en pantalla, se vuelve al inicio de sesión.
     */
    static void expirar(Context context) {
        Context app = context.getApplicationContext();
        RealtimeClient.disconnect();
        AvisosUsuario.cancelar(app);
        AvisosFuga.cancelar(app);
        limpiar(app);

        ActivityManager.RunningAppProcessInfo proceso = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(proceso);
        if (proceso.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(app, "Tu sesión terminó. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(app, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app.startActivity(intent);
        });
    }

    /** Borra la sesión del teléfono conservando el tema (la web lo recuerda igual). */
    private static synchronized void limpiar(Context context) {
        enMemoria.clear();
        SharedPreferences sesion = prefs(context);
        boolean oscuro = sesion.getBoolean("modo_oscuro", false);
        sesion.edit().clear().putBoolean("modo_oscuro", oscuro).apply();
        // Gotita se calla y olvida la conversación y los ajustes de esta cuenta (en el hilo principal:
        // expirar() llega desde un hilo de OkHttp).
        Gotita.olvidarSesion(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String leer(Context context, String clave) {
        String texto = enMemoria.get(clave);
        if (texto != null) return texto;

        SharedPreferences prefs = prefs(context);
        for (String antigua : CLAVES_ANTIGUAS) {
            if (prefs.contains(antigua)) prefs.edit().remove(antigua).apply();
        }
        texto = descifrar(prefs.getString(clave, ""));
        enMemoria.put(clave, texto);
        return texto;
    }

    private static void guardar(Context context, String clave, String valor) {
        String texto = valor == null ? "" : valor;
        enMemoria.put(clave, texto);
        String cifrado = cifrar(texto);
        // Si el almacén de claves falla, el token queda solo en memoria: nunca se escribe sin cifrar.
        SharedPreferences.Editor editor = prefs(context).edit();
        if (cifrado == null) editor.remove(clave);
        else editor.putString(clave, cifrado);
        editor.apply();
    }

    private static SecretKey clave() throws Exception {
        KeyStore almacen = KeyStore.getInstance("AndroidKeyStore");
        almacen.load(null);
        Key existente = almacen.getKey(ALIAS_CLAVE, null);
        if (existente instanceof SecretKey) return (SecretKey) existente;

        KeyGenerator generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generador.init(new KeyGenParameterSpec.Builder(ALIAS_CLAVE,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generador.generateKey();
    }

    /** Devuelve "iv:datos" en Base64, o null si no se pudo cifrar. */
    private static String cifrar(String texto) {
        if (texto.isEmpty()) return "";
        try {
            Cipher cifrador = Cipher.getInstance(CIFRADO);
            cifrador.init(Cipher.ENCRYPT_MODE, clave()); // el almacén de claves genera un IV nuevo cada vez
            byte[] datos = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(cifrador.getIV(), Base64.NO_WRAP) + ":"
                    + Base64.encodeToString(datos, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    /** Devuelve el token descifrado, o "" si no hay o no se puede descifrar (equivale a sesión cerrada). */
    private static String descifrar(String guardado) {
        int separador = guardado.indexOf(':');
        if (separador <= 0) return "";
        try {
            byte[] iv = Base64.decode(guardado.substring(0, separador), Base64.NO_WRAP);
            byte[] datos = Base64.decode(guardado.substring(separador + 1), Base64.NO_WRAP);
            Cipher cifrador = Cipher.getInstance(CIFRADO);
            cifrador.init(Cipher.DECRYPT_MODE, clave(), new GCMParameterSpec(128, iv));
            return new String(cifrador.doFinal(datos), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
