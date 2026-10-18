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

        // Mismo estilo que las demás gráficas de la app (GraficaUtil): ejes discretos, sin rejilla vertical.
        GraficaUtil.estilo(lineChart, this);
        lineChart.getLegend().setEnabled(false);

        cargarConsumo();
    }

    private void seleccionarTab(String periodo) {
        periodoActual = periodo;
        int colorActivo = ContextCompat.getColor(this, R.color.ink);
        int colorInactivo = ContextCompat.getColor(this, R.color.ink_2);
        tabDia.setTextColor(periodo.equals("dia") ? colorActivo : colorInactivo);
        tabSemana.setTextColor(periodo.equals("semana") ? colorActivo : colorInactivo);
        tabMes.setTextColor(periodo.equals("mes") ? colorActivo : colorInactivo);
        // Control segmentado: la opción elegida queda resaltada y se anuncia como seleccionada.
        tabDia.setBackgroundResource(periodo.equals("dia") ? R.drawable.bg_segment_active : 0);
        tabSemana.setBackgroundResource(periodo.equals("semana") ? R.drawable.bg_segment_active : 0);
        tabMes.setBackgroundResource(periodo.equals("mes") ? R.drawable.bg_segment_active : 0);
        tabDia.setSelected(periodo.equals("dia"));
        tabSemana.setSelected(periodo.equals("semana"));
        tabMes.setSelected(periodo.equals("mes"));
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
                ColorSeveridad.aplicarPill(tvEstadoConsumo, r.getColor());
                tvConsumoTotal.setText(String.format(java.util.Locale.getDefault(), "%.1f %s", r.getConsumoTotal(), r.getUnidad()));
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

        int colorLinea = ContextCompat.getColor(this, R.color.agua);
        LineDataSet set = GraficaUtil.linea(entradas, "Consumo (L)", colorLinea, true);

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetas));
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }
}
