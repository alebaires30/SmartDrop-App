package com.example.smartdrop;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresionActivity extends AppCompatActivity {

    private ImageButton btnVolver;
    private TextView tvValvula, tvPresionActual, tvEstadoPresion, tvUltimoAn, tvDescripcionPresion;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long INTERVALO_POLLING_MS = 10000;
    private Runnable tareaPolling;
    private static final int ID_VALVULA = 2;


    private boolean cargaEnProgreso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presion);

        btnVolver            = findViewById(R.id.btnVolver);
        tvPresionActual      = findViewById(R.id.tvPresionActual);
        tvEstadoPresion      = findViewById(R.id.tvEstadoPresion);
        tvUltimoAn           = findViewById(R.id.tvUltimoAn);
        tvDescripcionPresion = findViewById(R.id.tvDescripcionPresion);
        tvValvula = findViewById(R.id.tvValvula);


        btnVolver.setOnClickListener(v -> finish());

        tareaPolling = () -> {
            cargarPresion();
            cargarEstadoValvula();
            handler.postDelayed(tareaPolling, INTERVALO_POLLING_MS);
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tareaPolling);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tareaPolling);
    }

    private void cargarPresion() {
        if (cargaEnProgreso) return;
        cargaEnProgreso = true;


        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoAgua().enqueue(new Callback<EstadoAguaResponse>() {
            @Override
            public void onResponse(Call<EstadoAguaResponse> call, Response<EstadoAguaResponse> response) {
                cargaEnProgreso = false;
                if (!response.isSuccessful() || response.body() == null || response.body().getPresion() == null) return;
                PresionData p = response.body().getPresion();

                tvPresionActual.setText(String.format(java.util.Locale.getDefault(), "Presión actual: %.1f %s", p.getValor(), p.getUnidad()));
                tvEstadoPresion.setText(ColorSeveridad.iconoDe(p.getColor()) + " Presión: " + p.getEstado());
                tvEstadoPresion.setTextColor(ColorSeveridad.colorDe(p.getColor()));
                tvDescripcionPresion.setText(p.getDescripcion());
                tvUltimoAn.setText("Actualizado: " + formatearHora(p.getFecha()));

            }

            @Override
            public void onFailure(Call<EstadoAguaResponse> call, Throwable t) {
                cargaEnProgreso = false;
            }

        });
    }

    private void cargarEstadoValvula() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoValvula(ID_VALVULA).enqueue(new retrofit2.Callback<ValvulaEstadoResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ValvulaEstadoResponse> call, retrofit2.Response<ValvulaEstadoResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                boolean abierta = "abierta".equalsIgnoreCase(response.body().getEstadoActual());
                tvValvula.setText("Válvula: " + (abierta ? "ABIERTA" : "CERRADA"));
            }

            @Override
            public void onFailure(retrofit2.Call<ValvulaEstadoResponse> call, Throwable t) { }
        });
    }

    private String formatearHora(String fechaIso) {
        try {
            String limpio = fechaIso.length() > 19 ? fechaIso.substring(0, 19) : fechaIso;
            java.text.SimpleDateFormat entrada = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            entrada.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.text.SimpleDateFormat salida = new java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault());
            return salida.format(entrada.parse(limpio));
        } catch (Exception e) {
            return "--:--";
        }
    }
}