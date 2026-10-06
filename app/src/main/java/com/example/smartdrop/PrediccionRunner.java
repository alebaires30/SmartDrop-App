package com.example.smartdrop;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ejecuta "Realizar predicciones": lanza el análisis en el servidor, consulta su progreso y muestra una
 * pantalla de carga solo si tarda más de un segundo.
 */
public final class PrediccionRunner {

    public interface Listener {
        void onTerminado(JsonObject resultado);
        void onError(String mensaje);
    }

    private static final long MOSTRAR_DIALOGO_TRAS_MS = 1000;
    private static final long INTERVALO_CONSULTA_MS = 1000;

    private final Activity activity;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ApiService api;

    private AlertDialog dialogo;
    private TextView tvPaso, tvPorcentaje;
    private ProgressBar progreso;
    private ObjectAnimator animacionGota;
    private PrediccionJobResponse ultimo;
    private boolean terminado = false;

    private PrediccionRunner(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.api = ApiClient.getClientAutenticado(activity).create(ApiService.class);
    }

    public static void ejecutar(Activity activity, Listener listener) {
        new PrediccionRunner(activity, listener).iniciar();
    }

    private void iniciar() {
        handler.postDelayed(this::mostrarDialogoSiSigue, MOSTRAR_DIALOGO_TRAS_MS);
        api.ejecutarPredicciones().enqueue(new Callback<PrediccionJobResponse>() {
            @Override
            public void onResponse(Call<PrediccionJobResponse> call, Response<PrediccionJobResponse> response) {
                if (response.code() == 403) {
                    terminarConError("Esta sección es solo para administradores");
                } else if (!response.isSuccessful() || response.body() == null) {
                    terminarConError("No se pudo iniciar el análisis");
                } else {
                    procesar(response.body());
                }
            }

            @Override
            public void onFailure(Call<PrediccionJobResponse> call, Throwable t) {
                terminarConError("Sin conexión con el servidor");
            }
        });
    }

    private void procesar(PrediccionJobResponse job) {
        if (terminado) return;
        ultimo = job;
        pintar(job);
        if (job.isRunning()) {
            handler.postDelayed(() -> consultar(job.getJobId()), INTERVALO_CONSULTA_MS);
        } else if (job.isDone()) {
            terminar();
            listener.onTerminado(job.getResult() != null ? job.getResult() : new JsonObject());
        } else {
            terminarConError(job.getError() != null && !job.getError().isEmpty()
                    ? job.getError() : "La predicción falló");
        }
    }

    private void consultar(String jobId) {
        if (terminado) return;
        api.obtenerEstadoPrediccion(jobId).enqueue(new Callback<PrediccionJobResponse>() {
            @Override
            public void onResponse(Call<PrediccionJobResponse> call, Response<PrediccionJobResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    procesar(response.body());
                } else {
                    terminarConError("No se pudo consultar el progreso");
                }
            }

            @Override
            public void onFailure(Call<PrediccionJobResponse> call, Throwable t) {
                // Un fallo momentáneo de red no cancela el análisis: se reintenta.
                handler.postDelayed(() -> consultar(jobId), INTERVALO_CONSULTA_MS * 2);
            }
        });
    }

    private void mostrarDialogoSiSigue() {
        if (terminado || dialogo != null || activity.isFinishing() || activity.isDestroyed()) return;
        View vista = LayoutInflater.from(activity).inflate(R.layout.dialog_prediccion_progreso, null);
        tvPaso = vista.findViewById(R.id.tvPasoPrediccion);
        tvPorcentaje = vista.findViewById(R.id.tvPorcentajePrediccion);
        progreso = vista.findViewById(R.id.progresoPrediccion);
        View gota = vista.findViewById(R.id.ivGota);
        animacionGota = ObjectAnimator.ofFloat(gota, View.TRANSLATION_Y, 0f, 18f);
        animacionGota.setDuration(700);
        animacionGota.setRepeatMode(ValueAnimator.REVERSE);
        animacionGota.setRepeatCount(ValueAnimator.INFINITE);
        animacionGota.start();

        dialogo = new AlertDialog.Builder(activity).setView(vista).setCancelable(false).create();
        dialogo.show();
        if (ultimo != null) pintar(ultimo);
    }

    private void pintar(PrediccionJobResponse job) {
        if (dialogo == null) return;
        tvPaso.setText(job.getStep() == null || job.getStep().isEmpty() ? "…" : job.getStep());
        progreso.setProgress(job.getProgress());
        tvPorcentaje.setText(job.getProgress() + " %");
    }

    private void terminar() {
        terminado = true;
        handler.removeCallbacksAndMessages(null);
        if (animacionGota != null) animacionGota.cancel();
        if (dialogo != null && dialogo.isShowing() && !activity.isFinishing() && !activity.isDestroyed()) {
            dialogo.dismiss();
        }
        dialogo = null;
    }

    private void terminarConError(String mensaje) {
        if (terminado) return;
        terminar();
        listener.onError(mensaje);
    }
}