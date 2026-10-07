package com.example.smartdrop;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalidadActivity extends BaseActivity {

    private ImageButton btnVolver;
    private TextView tvEstado, tvEstrellas, tvDescripcion, tvUltimoAn, tvAnomalias;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long INTERVALO_POLLING_MS = 10000;
    private Runnable tareaPolling;

    private boolean cargaEnProgreso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calidad);

        btnVolver     = findViewById(R.id.btnVolver);
        tvEstado      = findViewById(R.id.tvEstado);
        tvEstrellas   = findViewById(R.id.tvEstrellas);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvUltimoAn    = findViewById(R.id.tvUltimoAn);
        tvAnomalias   = findViewById(R.id.tvAnomalias);

        btnVolver.setOnClickListener(v -> finish());

        configurarBottomNav(R.id.nav_calidad);

        tareaPolling = () -> {
            cargarCalidad();
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

    private void cargarCalidad() {
        if (cargaEnProgreso) return;
        cargaEnProgreso = true;

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoAgua().enqueue(new Callback<EstadoAguaResponse>() {
            @Override
            public void onResponse(Call<EstadoAguaResponse> call, Response<EstadoAguaResponse> response) {
                cargaEnProgreso = false;
                if (!response.isSuccessful() || response.body() == null || response.body().getCalidad() == null) return;
                CalidadData c = response.body().getCalidad();

                tvEstado.setText(ColorSeveridad.iconoDe(c.getColor()) + " " + c.getEstado());
                tvEstado.setTextColor(ColorSeveridad.colorDe(CalidadActivity.this, c.getColor()));
                tvDescripcion.setText(c.getDescripcion());
                tvAnomalias.setText(c.getTextoAnomalias());

                StringBuilder estrellas = new StringBuilder();
                for (int i = 0; i < 5; i++) estrellas.append(i < c.getEstrellas() ? "★" : "☆");
                tvEstrellas.setText(estrellas.toString());

                tvUltimoAn.setText("Último análisis: " + Fechas.formatear(c.getFecha(), "hh:mm a", "--:--"));
            }

            @Override
            public void onFailure(Call<EstadoAguaResponse> call, Throwable t) {
                cargaEnProgreso = false;
            }
        });
    }
}