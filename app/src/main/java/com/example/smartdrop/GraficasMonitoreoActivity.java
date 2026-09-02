package com.example.smartdrop;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GraficasMonitoreoActivity extends AppCompatActivity {

    private TabLayout tabParametros;
    private ChipGroup chipGroupPeriodo;
    private CardView cardAlertaRango;
    private TextView tvAlertaRango, tvTituloGrafica, tvUltimaActualizacion;
    private LineChart lineChart;
    private SwitchMaterial switchComparar;
    private ImageButton btnBack;

    private String parametroActual = "flujo";
    private String periodoActual   = "hoy";
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable tareaPolling;
    private boolean cargaEnProgreso = false;

    private static final SimpleDateFormat FORMATO_API;
    private static final SimpleDateFormat FORMATO_HORA;
    static {
        FORMATO_API = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        FORMATO_API.setTimeZone(TimeZone.getTimeZone("UTC"));
        FORMATO_HORA = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graficas_monitoreo);

        btnBack               = findViewById(R.id.btnBack);
        tabParametros         = findViewById(R.id.tabParametros);
        chipGroupPeriodo      = findViewById(R.id.chipGroupPeriodo);
        cardAlertaRango       = findViewById(R.id.cardAlertaRango);
        tvAlertaRango         = findViewById(R.id.tvAlertaRango);
        tvTituloGrafica       = findViewById(R.id.tvTituloGrafica);
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion);
        lineChart             = findViewById(R.id.lineChart);
        switchComparar        = findViewById(R.id.switchComparar);

        btnBack.setOnClickListener(v -> finish());

        String parametroInicial = getIntent().getStringExtra("parametro_inicial");
        if (parametroInicial != null) parametroActual = parametroInicial;
        seleccionarTabInicial();

        configurarListeners();
        configurarChart();

        tareaPolling = () -> {
            cargarDatos();
            handler.postDelayed(tareaPolling, intervaloSegunPeriodo());
        };
        handler.post(tareaPolling);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (tareaPolling != null) handler.post(tareaPolling);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tareaPolling != null) handler.removeCallbacks(tareaPolling);
    }

    private long intervaloSegunPeriodo() {
        switch (periodoActual) {
            case "semana": return 30000;
            case "mes":    return 60000;
            default:       return 10000; // "hoy"
        }
    }

    private void seleccionarTabInicial() {
        int index = 0;
        if (parametroActual.equals("presion")) index = 1;
        else if (parametroActual.equals("nivel")) index = 2;
        TabLayout.Tab tab = tabParametros.getTabAt(index);
        if (tab != null) tab.select();
    }

    private void configurarListeners() {
        // Escenario 1: cambio de parámetro
        tabParametros.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: parametroActual = "flujo"; break;
                    case 1: parametroActual = "presion"; break;
                    case 2: parametroActual = "nivel"; break;
                }
                if (!switchComparar.isChecked()) cargarDatos();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Escenario 2: cambio de período
        chipGroupPeriodo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipHoy) periodoActual = "hoy";
            else if (checkedId == R.id.chipSemana) periodoActual = "semana";
            else if (checkedId == R.id.chipMes) periodoActual = "mes";
            cargarDatos();
        });

        // Escenario 4: modo comparativo
        switchComparar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tabParametros.setVisibility(isChecked ? android.view.View.GONE : android.view.View.VISIBLE);
            cargarDatos();
        });
    }

    private void configurarChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);
    }

    private String[] calcularRangoFechas() {
        Calendar hasta = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar desde = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        switch (periodoActual) {
            case "semana": desde.add(Calendar.DAY_OF_YEAR, -7); break;
            case "mes":    desde.add(Calendar.DAY_OF_YEAR, -30); break;
            default:
                desde.set(Calendar.HOUR_OF_DAY, 0);
                desde.set(Calendar.MINUTE, 0);
                desde.set(Calendar.SECOND, 0);
        }
        return new String[]{ FORMATO_API.format(desde.getTime()), FORMATO_API.format(hasta.getTime()) };
    }

    private void cargarDatos() {
        if (cargaEnProgreso) return;
        cargaEnProgreso = true;

        String[] rango = calcularRangoFechas();
        String parametros = switchComparar.isChecked() ? "flujo,presion,nivel" : parametroActual;

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerGraficas(parametros, rango[0], rango[1]).enqueue(new Callback<GraficasResponse>() {
            @Override
            public void onResponse(@NonNull Call<GraficasResponse> call, @NonNull Response<GraficasResponse> response) {
                cargaEnProgreso = false;
                if (response.isSuccessful() && response.body() != null) {
                    if (switchComparar.isChecked()) pintarComparativo(response.body());
                    else pintarIndividual(response.body());
                } else {
                    Toast.makeText(GraficasMonitoreoActivity.this, "No se pudo cargar la gráfica.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<GraficasResponse> call, @NonNull Throwable t) {
                cargaEnProgreso = false;
                Toast.makeText(GraficasMonitoreoActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    //  Escenario 1 y 3
    private void pintarIndividual(GraficasResponse body) {
        ParametroData datoParametro = obtenerParametro(body, parametroActual);

        if (datoParametro == null || datoParametro.getDatos() == null || datoParametro.getDatos().isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            tvTituloGrafica.setText(tituloParametro(parametroActual) + " — sin datos en este período");
            tvUltimaActualizacion.setText("Sin lecturas registradas");
            cardAlertaRango.setVisibility(android.view.View.GONE);
            return;
        }

        List<SerieDato> datos = datoParametro.getDatos();
        List<Entry> entradasNormales = new ArrayList<>();
        List<Entry> entradasAlerta = new ArrayList<>();
        List<String> etiquetasHora = new ArrayList<>();
        boolean hayAlerta = false;

        for (int i = 0; i < datos.size(); i++) {
            SerieDato d = datos.get(i);
            entradasNormales.add(new Entry(i, (float) d.getValor()));
            etiquetasHora.add(formatearHora(d.getFecha()));
            if (d.isFueraDeRango()) {
                entradasAlerta.add(new Entry(i, (float) d.getValor()));
                hayAlerta = true;
            }
        }

        LineDataSet setNormal = new LineDataSet(entradasNormales, tituloParametro(parametroActual));
        setNormal.setColor(android.graphics.Color.parseColor("#7B2FBE"));
        setNormal.setCircleColor(android.graphics.Color.parseColor("#7B2FBE"));
        setNormal.setLineWidth(2f);
        setNormal.setCircleRadius(3f);
        setNormal.setDrawValues(false);

        LineData lineData;
        if (!entradasAlerta.isEmpty()) {
            LineDataSet setAlerta = new LineDataSet(entradasAlerta, "Fuera de rango");
            setAlerta.setColor(android.graphics.Color.parseColor("#C0392B"));
            setAlerta.setCircleColor(android.graphics.Color.parseColor("#C0392B"));
            setAlerta.setLineWidth(0f);
            setAlerta.setCircleRadius(5f);
            setAlerta.setDrawValues(false);
            lineData = new LineData(setNormal, setAlerta);
        } else {
            lineData = new LineData(setNormal);
        }

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetasHora));
        lineChart.setData(lineData);
        lineChart.invalidate();

        tvTituloGrafica.setText(tituloParametro(parametroActual) + " (" + datoParametro.getUnidad() + ")");
        tvUltimaActualizacion.setText("Última lectura: " + formatearHora(datos.get(datos.size() - 1).getFecha()));

        if (hayAlerta) {
            cardAlertaRango.setVisibility(android.view.View.VISIBLE);
            tvAlertaRango.setText("⚠ Valor fuera de rango detectado en " + tituloParametro(parametroActual));
        } else {
            cardAlertaRango.setVisibility(android.view.View.GONE);
        }
    }

    //Escenario 4
    private void pintarComparativo(GraficasResponse body) {
        List<LineDataSet> conjuntos = new ArrayList<>();
        boolean hayAlerta = false;

        hayAlerta |= agregarSerieComparativa(body.getFlujo(), "Flujo", "#3498DB", conjuntos);
        hayAlerta |= agregarSerieComparativa(body.getPresion(), "Presión", "#E67E22", conjuntos);
        hayAlerta |= agregarSerieComparativa(body.getNivel(), "Nivel", "#27AE60", conjuntos);

        if (conjuntos.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            tvTituloGrafica.setText("Comparación — sin datos en este período");
            return;
        }

        // Nota: cada parámetro se grafica por su propio índice de punto
        // (no están alineados al mismo instante exacto, cada sensor
        // reporta en momentos distintos). Sirve para comparar tendencias,
        // no para leer un valor exacto simultáneo entre parámetros.
        LineData lineData = new LineData(new ArrayList<>(conjuntos));
        lineChart.getXAxis().setValueFormatter(null);
        lineChart.setData(lineData);
        lineChart.getLegend().setEnabled(true);
        lineChart.invalidate();

        tvTituloGrafica.setText("Comparación: Flujo, Presión y Nivel");
        tvUltimaActualizacion.setText("Modo comparativo activo");

        cardAlertaRango.setVisibility(hayAlerta ? android.view.View.VISIBLE : android.view.View.GONE);
        if (hayAlerta) tvAlertaRango.setText("⚠ Uno o más parámetros están fuera de rango");
    }

    private boolean agregarSerieComparativa(ParametroData datoParametro, String nombre, String colorHex, List<LineDataSet> destino) {
        if (datoParametro == null || datoParametro.getDatos() == null || datoParametro.getDatos().isEmpty()) return false;

        List<Entry> entradas = new ArrayList<>();
        boolean hayAlerta = false;
        List<SerieDato> datos = datoParametro.getDatos();

        for (int i = 0; i < datos.size(); i++) {
            entradas.add(new Entry(i, (float) datos.get(i).getValor()));
            if (datos.get(i).isFueraDeRango()) hayAlerta = true;
        }

        LineDataSet set = new LineDataSet(entradas, nombre);
        set.setColor(android.graphics.Color.parseColor(colorHex));
        set.setCircleColor(android.graphics.Color.parseColor(colorHex));
        set.setLineWidth(2f);
        set.setCircleRadius(2.5f);
        set.setDrawValues(false);
        destino.add(set);

        return hayAlerta;
    }

    private ParametroData obtenerParametro(GraficasResponse body, String tipo) {
        switch (tipo) {
            case "presion": return body.getPresion();
            case "nivel":   return body.getNivel();
            default:        return body.getFlujo();
        }
    }

    private String tituloParametro(String tipo) {
        switch (tipo) {
            case "presion": return "Presión de la red";
            case "nivel":   return "Nivel de tanque";
            default:        return "Flujo de agua";
        }
    }

    private String formatearHora(String fechaIso) {
        try {
            java.util.Date fecha = FORMATO_API.parse(fechaIso.length() > 19 ? fechaIso.substring(0, 19) : fechaIso);
            return FORMATO_HORA.format(fecha);
        } catch (Exception e) {
            return "--:--";
        }
    }
}