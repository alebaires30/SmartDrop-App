package com.example.smartdrop;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Detalle de predicción de suministro de una zona (solo admin).
 * Muestra el estado actual, la última predicción de desabasto y las gráficas del nivel proyectado
 * del tanque y del consumo por hora (igual que la web).
 */
public class ZonaPrediccionDetalleActivity extends BaseActivity {

    private int zoneId;
    private TextView tvTitulo, tvNivel, tvConsumo, tvAnomalias;
    private TextView tvHoras, tvProbabilidad, tvRiesgo, tvRango, tvGenerada;
    private ProgressBar progresoNivel;
    private View cardPrediccion, tvSinPrediccion;
    private LineChart chartTrayectoria, chartConsumo;
    private ProgressBar progresoTrayectoria, progresoConsumo;
    private TextView tvSubConsumo;
    private ApiService api;

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

        chartTrayectoria = findViewById(R.id.chartTrayectoria);
        chartConsumo = findViewById(R.id.chartConsumo);
        progresoTrayectoria = findViewById(R.id.progresoTrayectoria);
        progresoConsumo = findViewById(R.id.progresoConsumo);
        tvSubConsumo = findViewById(R.id.tvSubConsumo);
        GraficaUtil.estilo(chartTrayectoria, this);
        GraficaUtil.estilo(chartConsumo, this);
        chartConsumo.getLegend().setEnabled(false);

        if (zoneName != null) tvTitulo.setText(zoneName);

        api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        cargarEstado();
        cargarTrayectoria();
        cargarConsumo();
    }

    private void cargarTrayectoria() {
        api.obtenerTrayectoriaTanque(zoneId).enqueue(new Callback<TrayectoriaTanqueResponse>() {
            @Override
            public void onResponse(Call<TrayectoriaTanqueResponse> call, Response<TrayectoriaTanqueResponse> response) {
                progresoTrayectoria.setVisibility(View.GONE);
                List<TrayectoriaTanqueResponse.Punto> puntos = response.isSuccessful() && response.body() != null
                        ? response.body().getTrayectoria() : null;
                if (puntos == null || puntos.isEmpty()) {
                    chartTrayectoria.setNoDataText(Idioma.t(ZonaPrediccionDetalleActivity.this, "Aún no hay proyección. Realiza las predicciones."));
                    chartTrayectoria.clear();
                    return;
                }
                List<Entry> esperado = new ArrayList<>(), minimo = new ArrayList<>(), maximo = new ArrayList<>();
                List<String> etiquetas = new ArrayList<>();
                for (int i = 0; i < puntos.size(); i++) {
                    TrayectoriaTanqueResponse.Punto p = puntos.get(i);
                    esperado.add(new Entry(i, (float) p.getEsperado()));
                    minimo.add(new Entry(i, (float) p.getMinimo()));
                    maximo.add(new Entry(i, (float) p.getMaximo()));
                    etiquetas.add(hora(p.getFecha()));
                }
                int morado = ContextCompat.getColor(ZonaPrediccionDetalleActivity.this, R.color.brand_accent);
                int suave = ContextCompat.getColor(ZonaPrediccionDetalleActivity.this, R.color.brand_soft);
                LineDataSet setEsperado = GraficaUtil.linea(esperado, "Nivel esperado", morado, true);
                LineDataSet setMaximo = GraficaUtil.lineaPunteada(maximo, "Máximo", suave);
                LineDataSet setMinimo = GraficaUtil.lineaPunteada(minimo, "Mínimo", suave);
                GraficaUtil.etiquetas(chartTrayectoria, etiquetas);
                chartTrayectoria.getAxisLeft().setAxisMinimum(0f);
                chartTrayectoria.setData(new LineData(setMaximo, setEsperado, setMinimo));
                chartTrayectoria.invalidate();
            }

            @Override
            public void onFailure(Call<TrayectoriaTanqueResponse> call, Throwable t) {
                progresoTrayectoria.setVisibility(View.GONE);
                chartTrayectoria.setNoDataText(Idioma.t(ZonaPrediccionDetalleActivity.this, "Sin conexión con el servidor"));
                chartTrayectoria.invalidate();
            }
        });
    }

    private void cargarConsumo() {
        api.obtenerHistorialConsumo(zoneId).enqueue(new Callback<HistorialConsumoResponse>() {
            @Override
            public void onResponse(Call<HistorialConsumoResponse> call, Response<HistorialConsumoResponse> response) {
                progresoConsumo.setVisibility(View.GONE);
                HistorialConsumoResponse datos = response.isSuccessful() ? response.body() : null;
                List<HistorialConsumoResponse.Hora> horas = datos == null ? null : datos.getHistorial();
                if (horas == null || horas.isEmpty()) {
                    chartConsumo.setNoDataText(Idioma.t(ZonaPrediccionDetalleActivity.this, "Sin consumo registrado todavía"));
                    chartConsumo.clear();
                    return;
                }
                List<Entry> entradas = new ArrayList<>();
                List<String> etiquetas = new ArrayList<>();
                double total = 0;
                for (int i = 0; i < horas.size(); i++) {
                    entradas.add(new Entry(i, (float) horas.get(i).getLitros()));
                    etiquetas.add(hora(horas.get(i).getFecha()));
                    total += horas.get(i).getLitros();
                }
                int azul = ContextCompat.getColor(ZonaPrediccionDetalleActivity.this, R.color.brand_blue);
                GraficaUtil.etiquetas(chartConsumo, etiquetas);
                chartConsumo.getAxisLeft().setAxisMinimum(0f);
                chartConsumo.setData(new LineData(GraficaUtil.linea(entradas, "Consumo (L)", azul, true)));
                chartConsumo.invalidate();

                String texto = String.format(Locale.getDefault(), "Total: %.1f L en las últimas %d h", total, horas.size());
                HistorialConsumoResponse.Pronostico p = datos.getPronostico();
                if (p != null) {
                    texto += String.format(Locale.getDefault(),
                            " · próxima hora ≈ %.2f L (entre %.2f y %.2f)", p.getEsperado(), p.getMinimo(), p.getMaximo());
                }
                tvSubConsumo.setText(texto);
            }

            @Override
            public void onFailure(Call<HistorialConsumoResponse> call, Throwable t) {
                progresoConsumo.setVisibility(View.GONE);
                chartConsumo.setNoDataText(Idioma.t(ZonaPrediccionDetalleActivity.this, "Sin conexión con el servidor"));
                chartConsumo.invalidate();
            }
        });
    }

    /** Hora local corta para el eje X ("14:00"; con día si no es hoy: "07/10 14:00"). */
    private static String hora(String iso) {
        Date fecha = Fechas.parse(iso);
        if (fecha == null) return "";
        SimpleDateFormat dia = new SimpleDateFormat("yyyyMMdd", Locale.US);
        boolean hoy = dia.format(fecha).equals(dia.format(new Date()));
        return new SimpleDateFormat(hoy ? "HH:mm" : "dd/MM HH:mm", Locale.getDefault()).format(fecha);
    }

    private void cargarEstado() {
        api.obtenerEstadoZona(zoneId).enqueue(new Callback<ZonaEstadoResponse>() {
            @Override
            public void onResponse(Call<ZonaEstadoResponse> call, Response<ZonaEstadoResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Idioma.toast(ZonaPrediccionDetalleActivity.this,
                            "No se pudo cargar el estado de la zona", Toast.LENGTH_SHORT);
                    return;
                }
                mostrar(response.body());
            }

            @Override
            public void onFailure(Call<ZonaEstadoResponse> call, Throwable t) {
                Idioma.toast(ZonaPrediccionDetalleActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT);
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
                    "%d anomalías detectadas en las últimas 24 h", e.getAnomaliasActivas24h()));
            tvAnomalias.setTextColor(ContextCompat.getColor(this, R.color.text_alert));
        } else {
            tvAnomalias.setText("Sin anomalías en las últimas 24 h");
            tvAnomalias.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        }

        ShortagePrediction p = e.getUltimaPrediccionDesabasto();
        if (p == null) {
            cardPrediccion.setVisibility(View.GONE);
            tvSinPrediccion.setVisibility(View.VISIBLE);
            return;
        }

        cardPrediccion.setVisibility(View.VISIBLE);
        tvSinPrediccion.setVisibility(View.GONE);
        pintarRiesgo(p);
        if (p.getGeneratedAt() != null) {
            String generada = Fechas.corta(p.getGeneratedAt());
            tvGenerada.setText(generada.isEmpty() ? "" : "Actualizada: " + generada);
        }

        int horizonte = p.getHorizonteHoras() != null ? p.getHorizonteHoras() : 72;
        if (p.getMedianHoursToShortage() == null) {
            // Riesgo bajo: el tanque no llega al nivel crítico dentro del horizonte.
            tvHoras.setText("Sin desabasto previsto");
            tvRango.setText(String.format(Locale.getDefault(),
                    "El tanque se mantiene sobre el nivel crítico en las próximas %d h.", horizonte));
            tvProbabilidad.setText(p.getProbabilidadDesabastoHorizonte() == null ? "" : String.format(Locale.getDefault(),
                    "Probabilidad de quedarse sin agua: %.0f%%", p.getProbabilidadDesabastoHorizonte() * 100));
            return;
        }

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

    }

    private void pintarRiesgo(ShortagePrediction p) {
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
            case "medio":
            case "moderado":
                tvRiesgo.setText("Nivel de riesgo: MODERADO");
                tvRiesgo.setTextColor(ContextCompat.getColor(this, R.color.status_yellow));
                break;
            default:
                tvRiesgo.setText("Nivel de riesgo: BAJO");
                tvRiesgo.setTextColor(ContextCompat.getColor(this, R.color.status_green));
                break;
        }

    }
}
