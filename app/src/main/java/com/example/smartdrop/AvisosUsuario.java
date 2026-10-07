package com.example.smartdrop;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Notificaciones push del usuario según sus preferencias (nivel, suministro, calidad, consumo elevado y
 * reporte semanal). El servidor ya filtra por las categorías activadas; aquí se decide qué es nuevo:
 * un sensor avisa cuando pasa de Normal a Alta/Baja y una alerta guardada avisa una sola vez.
 */
public final class AvisosUsuario {

    private static final String CANAL = "avisos_usuario";
    private static final String PREFS = "avisos_usuario";
    private static final String TRABAJO = "avisos_usuario_periodico";
    private static final String CLAVE_INICIADO = "iniciado";
    private static final String CLAVE_NOTIFICADAS = "alertas_notificadas";
    private static final long MINUTOS_ENTRE_REVISIONES = 15; // mínimo que permite Android para trabajo periódico
    private static final int MAX_RECORDADAS = 200;
    private static final int CODIGO_PERMISO = 502;
    private static final int ID_BASE = 7000;

    private AvisosUsuario() { }

    static boolean tieneSesion(Context context) {
        SharedPreferences sesion = context.getApplicationContext().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        return !sesion.getString("access_token", "").isEmpty();
    }

    /** Pide el permiso de notificaciones (Android 13+) y programa la revisión en segundo plano. */
    public static void iniciar(Activity activity) {
        crearCanal(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, CODIGO_PERMISO);
        }
        Constraints conRed = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest trabajo = new PeriodicWorkRequest.Builder(
                AvisosUsuarioWorker.class, MINUTOS_ENTRE_REVISIONES, TimeUnit.MINUTES)
                .setConstraints(conRed)
                .build();
        WorkManager.getInstance(activity.getApplicationContext())
                .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, trabajo);
        revisarAhora(activity);
    }

    /** Detiene las revisiones y olvida lo notificado (al cerrar sesión). */
    public static void cancelar(Context context) {
        Context app = context.getApplicationContext();
        WorkManager.getInstance(app).cancelUniqueWork(TRABAJO);
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    /** Revisión con la app abierta, sin bloquear la pantalla. */
    public static void revisarAhora(Context context) {
        Context app = context.getApplicationContext();
        if (!tieneSesion(app)) return;
        ApiClient.getClientAutenticado(app).create(ApiService.class).obtenerAvisos()
                .enqueue(new Callback<Perfil.AvisosResponse>() {
                    @Override
                    public void onResponse(Call<Perfil.AvisosResponse> call, Response<Perfil.AvisosResponse> response) {
                        if (response.isSuccessful() && response.body() != null) procesar(app, response.body());
                    }

                    @Override
                    public void onFailure(Call<Perfil.AvisosResponse> call, Throwable t) {
                        // Sin conexión momentánea: se reintenta en la siguiente revisión.
                    }
                });
    }

    /** Revisión síncrona para el trabajo en segundo plano. Devuelve false si no hubo conexión. */
    static boolean revisarBloqueando(Context context) {
        Context app = context.getApplicationContext();
        try {
            Response<Perfil.AvisosResponse> response = ApiClient.getClientAutenticado(app)
                    .create(ApiService.class).obtenerAvisos().execute();
            if (response.isSuccessful() && response.body() != null) procesar(app, response.body());
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static synchronized void procesar(Context app, Perfil.AvisosResponse respuesta) {
        if (respuesta.avisos == null) return;
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // La primera revisión solo registra el estado actual: así no llegan de golpe avisos viejos.
        boolean primera = !prefs.getBoolean(CLAVE_INICIADO, false);
        Set<String> notificadas = new HashSet<>(prefs.getStringSet(CLAVE_NOTIFICADAS, new HashSet<>()));
        SharedPreferences.Editor editor = prefs.edit();

        for (Perfil.Aviso aviso : respuesta.avisos) {
            if (aviso.clave == null) continue;
            if ("sensor".equals(aviso.tipo)) {
                String anterior = prefs.getString("estado_" + aviso.clave, "Normal");
                String actual = aviso.estado == null ? "Normal" : aviso.estado;
                editor.putString("estado_" + aviso.clave, actual);
                if (!primera && !"Normal".equals(actual) && !actual.equals(anterior)) notificar(app, aviso, true);
            } else if (!notificadas.contains(aviso.clave)) {
                notificadas.add(aviso.clave);
                if (!primera) notificar(app, aviso, false);
            }
        }
        if (notificadas.size() > MAX_RECORDADAS) notificadas.clear();
        editor.putStringSet(CLAVE_NOTIFICADAS, notificadas).putBoolean(CLAVE_INICIADO, true).apply();
    }

    private static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel canal = new NotificationChannel(
                CANAL, Idioma.t(context, "Alertas de tu vivienda"), NotificationManager.IMPORTANCE_DEFAULT);
        canal.setDescription(Idioma.t(context, "Nivel, suministro, calidad, consumo y reportes semanales"));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(canal);
    }

    private static void notificar(Context app, Perfil.Aviso aviso, boolean esSensor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        crearCanal(app);
        Intent intent = new Intent(app, esSensor ? AlertasActivity.class : InicioActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int id = ID_BASE + (aviso.clave.hashCode() & 0x0FFFFFFF) % 100000;
        PendingIntent abrir = PendingIntent.getActivity(
                app, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String mensaje = Idioma.t(app, aviso.mensaje == null ? "" : aviso.mensaje);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CANAL)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(aviso.titulo == null ? "SmartDrop" : Idioma.t(app, aviso.titulo))
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(abrir);
        NotificationManagerCompat.from(app).notify(id, builder.build());
    }
}
