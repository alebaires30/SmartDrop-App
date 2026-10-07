package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsumoActivity extends BaseActivity {

    private ImageButton btnVolver;
    private TextView tabDia, tabSemana, tabMes, tvEstadoConsumo, tvConsumoTotal, tvComparacion;
    private LineChart lineChart;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNav;

    private String periodoActual = "dia";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumo);

        btnVolver       = findViewById(R.id.btnVolver);
        tabDia          = findViewById(R.id.tabDia);
        tabSemana       = findViewById(R.id.tabSemana);
        tabMes          = findViewById(R.id.tabMes);
        tvEstadoConsumo = findViewById(R.id.tvEstadoConsumo);
        tvConsumoTotal  = findViewById(R.id.tvConsumoTotal);
        tvComparacion   = findViewById(R.id.tvComparacion);
        lineChart       = findViewById(R.id.lineChartConsumo);
        bottomNav       = findViewById(R.id.bottomNavConsumo);

        btnVolver.setOnClickListener(v -> finish());
        bottomNav.setSelectedItemId(R.id.nav_consumo);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_retroalimentacion) {
                startActivity(new Intent(this, RetroalimentacionActivity.class));
                finish();
            } else if (id == R.id.nav_recomendaciones) {
                startActivity(new Intent(this, RecomendacionesActivity.class));
                finish();
            }
            return true;
        });

        tabDia.setOnClickListener(v -> seleccionarTab("dia"));
        tabSemana.setOnClickListener(v -> seleccionarTab("semana"));
        tabMes.setOnClickListener(v -> seleccionarTab("mes"));

        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);

        int colorTextoChart = ContextCompat.getColor(this, R.color.text_secondary);
        lineChart.getXAxis().setTextColor(colorTextoChart);
        lineChart.getAxisLeft().setTextColor(colorTextoChart);
        lineChart.getAxisRight().setTextColor(colorTextoChart);
        lineChart.getLegend().setTextColor(colorTextoChart);

        cargarConsumo();
    }

    private void seleccionarTab(String periodo) {
        periodoActual = periodo;
        int colorActivo = ContextCompat.getColor(this, R.color.brand_on_card);
        int colorInactivo = ContextCompat.getColor(this, R.color.text_secondary);
        tabDia.setTextColor(periodo.equals("dia") ? colorActivo : colorInactivo);
        tabSemana.setTextColor(periodo.equals("semana") ? colorActivo : colorInactivo);
        tabMes.setTextColor(periodo.equals("mes") ? colorActivo : colorInactivo);
        cargarConsumo();
    }

    private void cargarConsumo() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerConsumo(periodoActual).enqueue(new Callback<ConsumoResponse>() {
            @Override
            public void onResponse(Call<ConsumoResponse> call, Response<ConsumoResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                ConsumoResponse r = response.body();

                tvEstadoConsumo.setText(r.getEstadoTexto());
                tvEstadoConsumo.setTextColor(ColorSeveridad.colorDe(ConsumoActivity.this, r.getColor()));
                tvConsumoTotal.setText(String.format(java.util.Locale.getDefault(), "%.1f%s", r.getConsumoTotal(), r.getUnidad()));
                tvComparacion.setText(r.getComparacion() != null ? r.getComparacion().getTexto() : "");

                pintarGrafica(r);
            }

            @Override
            public void onFailure(Call<ConsumoResponse> call, Throwable t) { }
        });
    }

    private void pintarGrafica(ConsumoResponse r) {
        if (r.getSerie() == null || r.getSerie().isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }

        List<Entry> entradas = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        for (int i = 0; i < r.getSerie().size(); i++) {
            entradas.add(new Entry(i, (float) r.getSerie().get(i).getLitros()));
            etiquetas.add(r.getSerie().get(i).getFecha());
        }

        LineDataSet set = new LineDataSet(entradas, Idioma.t(this, "Consumo (L)"));
        int colorLinea = ContextCompat.getColor(this, R.color.brand_accent);
        set.setColor(colorLinea);
        set.setCircleColor(colorLinea);
        set.setLineWidth(2f);
        set.setDrawValues(false);

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetas));
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }
}
