package com.example.smartdrop;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Detalle de predicción de suministro de una zona (solo admin).
 * Muestra el estado actual y la última predicción de desabasto.
 */
public class ZonaPrediccionDetalleActivity extends BaseActivity {

    private int zoneId;
    private TextView tvTitulo, tvNivel, tvConsumo, tvAnomalias;
    private TextView tvHoras, tvProbabilidad, tvRiesgo, tvRango, tvGenerada;
    private ProgressBar progresoNivel;
    private View cardPrediccion, tvSinPrediccion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zona_prediccion_detalle);

        zoneId = getIntent().getIntExtra("zone_id", 0);
        String zoneName = getIntent().getStringExtra("zone_name");
        if (zoneId <= 0) {
            finish();
            return;
        }

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        tvTitulo = findViewById(R.id.tvTituloZona);
        tvNivel = findViewById(R.id.tvDetalleNivel);
        tvConsumo = findViewById(R.id.tvDetalleConsumo);
        tvAnomalias = findViewById(R.id.tvDetalleAnomalias);
        progresoNivel = findViewById(R.id.progresoNivel);

        cardPrediccion = findViewById(R.id.cardPrediccion);
        tvSinPrediccion = findViewById(R.id.tvSinPrediccion);
        tvHoras = findViewById(R.id.tvHorasRestantes);
        tvProbabilidad = findViewById(R.id.tvProbabilidad);
        tvRiesgo = findViewById(R.id.tvNivelRiesgo);
        tvRango = findViewById(R.id.tvRangoHoras);
        tvGenerada = findViewById(R.id.tvGenerada);

        if (zoneName != null) tvTitulo.setText(zoneName);

        cargarEstado();
    }

    private void cargarEstado() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoZona(zoneId).enqueue(new Callback<ZonaEstadoResponse>() {
            @Override
            public void onResponse(Call<ZonaEstadoResponse> call, Response<ZonaEstadoResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ZonaPrediccionDetalleActivity.this,
                            "No se pudo cargar el estado de la zona", Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrar(response.body());
            }

            @Override
            public void onFailure(Call<ZonaEstadoResponse> call, Throwable t) {
                Toast.makeText(ZonaPrediccionDetalleActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrar(ZonaEstadoResponse e) {
        tvTitulo.setText(e.getZoneName());
        tvNivel.setText(String.format(Locale.getDefault(),
                "%.0f L (%.1f%% de capacidad)", e.getNivelActualLitros(), e.getPorcentajeLlenado()));
        progresoNivel.setProgress((int) Math.round(e.getPorcentajeLlenado()));
        tvConsumo.setText(String.format(Locale.getDefault(),
                "Consumo actual: %.1f L/h", e.getConsumoActualLph()));

        if (e.getAnomaliasActivas24h() > 0) {
            tvAnomalias.setText(String.format(Locale.getDefault(),
                    "⚠ %d anomalías detectadas en las últimas 24 h", e.getAnomaliasActivas24h()));
            tvAnomalias.setTextColor(ContextCompat.getColor(this, R.color.text_alert));
        } else {
            tvAnomalias.setText("Sin anomalías en las últimas 24 h");
            tvAnomalias.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        }

        ShortagePrediction p = e.getUltimaPrediccionDesabasto();
        if (p == null || p.getMedianHoursToShortage() == null) {
            cardPrediccion.setVisibility(View.GONE);
            tvSinPrediccion.setVisibility(View.VISIBLE);
            return;
        }

        cardPrediccion.setVisibility(View.VISIBLE);
        tvSinPrediccion.setVisibility(View.GONE);

        double horas = p.getMedianHoursToShortage();
        int dias = (int) (horas / 24);
        int horasResto = (int) (horas % 24);
        tvHoras.setText(dias > 0
                ? String.format(Locale.getDefault(), "%d día(s) y %d h aprox.", dias, horasResto)
                : String.format(Locale.getDefault(), "%.0f h aprox.", horas));

        if (p.getP10HoursToShortage() != null && p.getP90HoursToShortage() != null) {
            tvRango.setText(String.format(Locale.getDefault(),
                    "Rango estimado: %.0f h – %.0f h",
                    p.getP10HoursToShortage(), p.getP90HoursToShortage()));
        }

        if (p.getProbabilidadDesabastoHorizonte() != null) {
            tvProbabilidad.setText(String.format(Locale.getDefault(),
                    "Probabilidad de desabasto (%d h): %.0f%%",
                    p.getHorizonteHoras() != null ? p.getHorizonteHoras() : 72,
                    p.getProbabilidadDesabastoHorizonte() * 100));
        }

        String riesgo = p.getNivelRiesgo() == null ? "sin_datos" : p.getNivelRiesgo();
        switch (riesgo) {
            case "critico":
                tvRiesgo.setText("Nivel de riesgo: CRÍTICO");
                tvRiesgo.setTextColor(ContextCompat.getColor(this, R.color.text_alert));
                break;
            case "alto":
                tvRiesgo.setText("Nivel de riesgo: ALTO");
                tvRiesgo.setTextColor(ContextCompat.getColor(this, R.color.status_orange));
                break;
            case "moderado":
                tvRiesgo.setText("Nivel de riesgo: MODERADO");
                tvRiesgo.setTextColor(ContextCompat.getColor(this, R.color.status_yellow));
                break;
            default:
                tvRiesgo.setText("Nivel de riesgo: BAJO");
                tvRiesgo.setTextColor(ContextCompat.getColor(this, R.color.status_green));
                break;
        }

        if (p.getGeneratedAt() != null) {
            tvGenerada.setText("Actualizada: " + p.getGeneratedAt().replace('T', ' ').substring(0,
                    Math.min(16, p.getGeneratedAt().length())));
        }
    }
}
