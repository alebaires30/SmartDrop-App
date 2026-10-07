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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Avisos automáticos de fuga para el administrador: consulta los avisos sin leer que genera el servidor y
 * muestra los nuevos como notificación del sistema, tanto con la app abierta como en segundo plano.
 */
public final class AvisosFuga {

    /** Recibe los avisos nuevos de una revisión hecha con la app abierta. */
    public interface Listener {
        void onAvisosNuevos(List<AlertaFuga> nuevos, boolean notificados);
    }

    private static final String CANAL = "avisos_fuga";
    private static final String PREFS = "avisos_fuga";
    private static final String TRABAJO = "avisos_fuga_periodico";
    private static final long MINUTOS_ENTRE_REVISIONES = 15; // mínimo que permite Android para trabajo periódico
    private static final int MAX_NOTIFICACIONES = 3;
    private static final int CODIGO_PERMISO = 501;

    private AvisosFuga() { }

    /** True si hay una sesión de administrador guardada en este dispositivo. */
    public static boolean esAdminConSesion(Context context) {
        SharedPreferences sesion = context.getApplicationContext().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        return sesion.getInt("id_rol", 0) == 2 && !sesion.getString("access_token", "").isEmpty();
    }

    /** Pide el permiso de notificaciones (Android 13+) la primera vez que el admin abre su panel. */
    public static void pedirPermiso(Activity activity) {
        crearCanal(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, CODIGO_PERMISO);
        }
    }

    /** Programa la revisión en segundo plano; sigue activa aunque la app se cierre o el teléfono se reinicie. */
    public static void programar(Context context) {
        Constraints conRed = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest trabajo = new PeriodicWorkRequest.Builder(
                AvisosFugaWorker.class, MINUTOS_ENTRE_REVISIONES, TimeUnit.MINUTES)
                .setConstraints(conRed)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, trabajo);
    }

    /** Detiene la revisión en segundo plano y retira las notificaciones (al cerrar sesión). */
    public static void cancelar(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(TRABAJO);
        quitarNotificaciones(context);
    }

    public static void quitarNotificaciones(Context context) {
        NotificationManagerCompat.from(context.getApplicationContext()).cancelAll();
    }

    /** Revisa ahora sin bloquear la pantalla. El listener solo se llama si hay avisos nuevos. */
    public static void revisarAhora(Context context, Listener listener) {
        Context app = context.getApplicationContext();
        if (!esAdminConSesion(app)) return;
        ApiClient.getClientAutenticado(app).create(ApiService.class).obtenerAlertasFuga(1)
                .enqueue(new Callback<AlertasFugaResponse>() {
                    @Override
                    public void onResponse(Call<AlertasFugaResponse> call, Response<AlertasFugaResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        List<AlertaFuga> nuevos = procesar(app, response.body());
                        if (!nuevos.isEmpty() && listener != null) {
                            listener.onAvisosNuevos(nuevos, puedeNotificar(app));
                        }
                    }

                    @Override
                    public void onFailure(Call<AlertasFugaResponse> call, Throwable t) {
                        // Sin conexión momentánea: se reintenta en la siguiente revisión.
                    }
                });
    }

    /** Revisión síncrona para el trabajo en segundo plano. Devuelve false si no hubo conexión con el servidor. */
    static boolean revisarBloqueando(Context context) {
        Context app = context.getApplicationContext();
        try {
            Response<AlertasFugaResponse> response = ApiClient.getClientAutenticado(app)
                    .create(ApiService.class).obtenerAlertasFuga(1).execute();
            if (response.isSuccessful() && response.body() != null) procesar(app, response.body());
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Notifica los avisos que este dispositivo aún no había mostrado y los devuelve (del más antiguo al más nuevo). */
    private static synchronized List<AlertaFuga> procesar(Context app, AlertasFugaResponse respuesta) {
        List<AlertaFuga> nuevos = new ArrayList<>();
        if (respuesta.getAlertas() == null) return nuevos;

        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int idUsuario = app.getSharedPreferences("sesion", Context.MODE_PRIVATE).getInt("id_usuario", 0);
        String clave = "ultimo_id_" + idUsuario;
        long ultimo = prefs.getLong(clave, 0);

        for (AlertaFuga alerta : respuesta.getAlertas()) {
            if (!alerta.isLeida() && alerta.getIdAlerta() > ultimo) nuevos.add(alerta);
        }
        if (nuevos.isEmpty()) return nuevos;
        Collections.sort(nuevos, (a, b) -> Long.compare(a.getIdAlerta(), b.getIdAlerta()));
        prefs.edit().putLong(clave, nuevos.get(nuevos.size() - 1).getIdAlerta()).apply();

        if (puedeNotificar(app)) {
            int desde = Math.max(0, nuevos.size() - MAX_NOTIFICACIONES);
            for (AlertaFuga alerta : nuevos.subList(desde, nuevos.size())) notificar(app, alerta);
        }
        return nuevos;
    }

    private static boolean puedeNotificar(Context app) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(app).areNotificationsEnabled();
    }

    private static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel canal = new NotificationChannel(
                CANAL, Idioma.t(context, "Avisos de fuga"), NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(Idioma.t(context, "Posibles fugas detectadas automáticamente en las viviendas"));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(canal);
    }

    private static void notificar(Context app, AlertaFuga alerta) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        crearCanal(app);

        Intent intent = new Intent(app, PrediccionFugasActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int id = (int) (alerta.getIdAlerta() % Integer.MAX_VALUE);
        PendingIntent abrir = PendingIntent.getActivity(
                app, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CANAL)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(Idioma.t(app, titulo(alerta)))
                .setContentText(Idioma.t(app, resumen(alerta)))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(Idioma.t(app, detalle(alerta))))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(abrir);
        NotificationManagerCompat.from(app).notify(id, builder.build());
    }

    static String titulo(AlertaFuga alerta) {
        String nic = alerta.getNic() == null || alerta.getNic().isEmpty() ? "una vivienda" : alerta.getNic();
        if (alerta.getProbabilidad() == null) return "Posible fuga en " + nic;
        return String.format(Locale.getDefault(), "Posible fuga en %s (%.0f %%)", nic, alerta.getProbabilidad() * 100);
    }

    static String resumen(AlertaFuga alerta) {
        String clave = FugaTexto.datosClave(alerta.getMetricas(), alerta.getInicio());
        StringBuilder texto = new StringBuilder();
        if (alerta.getDireccion() != null && !alerta.getDireccion().isEmpty()) texto.append(alerta.getDireccion());
        if (!clave.isEmpty()) {
            if (texto.length() > 0) texto.append(" · ");
            texto.append(clave.split("  ·  ")[0]);
        }
        return texto.length() > 0 ? texto.toString() : "Toca para ver los detalles";
    }

    /** Texto ampliado de la notificación: qué pasa, datos clave y qué hacer (sin el bloque técnico). */
    static String detalle(AlertaFuga alerta) {
        StringBuilder texto = new StringBuilder();
        if (alerta.getDireccion() != null && !alerta.getDireccion().isEmpty()) texto.append(alerta.getDireccion()).append('\n');
        if (alerta.getResumen() != null && !alerta.getResumen().isEmpty()) texto.append(alerta.getResumen()).append('\n');
        String clave = FugaTexto.datosClave(alerta.getMetricas(), alerta.getInicio());
        if (!clave.isEmpty()) texto.append(clave).append('\n');
        texto.append("Qué hacer: ").append(alerta.getAccion() == null ? FugaTexto.ACCION_POR_DEFECTO : alerta.getAccion());
        return texto.toString();
    }
}
