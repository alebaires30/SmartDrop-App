package com.example.smartdrop;

import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
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

public class GraficasMonitoreoActivity extends BaseActivity {

    private final BroadcastReceiver realtimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cargarDatos(false);
        }
    };

    private TabLayout tabParametros;
    private ChipGroup chipGroupPeriodo;
    private CardView cardAlertaRango;
    private TextView tvAlertaRango, tvTituloGrafica, tvUltimaActualizacion, tvResumenGrafica;
    private android.view.View layoutCargando;
    private LineChart lineChart;
    private SwitchMaterial switchComparar;
    private ImageButton btnBack;

    private String parametroActual = "flujo";
    private String periodoActual   = "hoy";
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable tareaPolling;
    private boolean cargaEnProgreso = false;
    private boolean recargaPendiente = false;
    /** Muestra "Recopilando datos…" si la consulta tarda más de este tiempo (rangos con muchas lecturas). */
    private static final long RETRASO_CARGANDO_MS = 300;
    private final Runnable mostrarCargando = () -> layoutCargando.setVisibility(android.view.View.VISIBLE);

    private static final SimpleDateFormat FORMATO_API;
    private static final SimpleDateFormat FORMATO_HORA;
    private static final SimpleDateFormat FORMATO_DIA;
    static {
        FORMATO_API = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        FORMATO_API.setTimeZone(TimeZone.getTimeZone("UTC"));
        FORMATO_HORA = new SimpleDateFormat("HH:mm", Locale.getDefault());
        FORMATO_DIA = new SimpleDateFormat("dd/MM", Locale.getDefault());
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
        tvResumenGrafica      = findViewById(R.id.tvResumenGrafica);
        layoutCargando        = findViewById(R.id.layoutCargando);

        btnBack.setOnClickListener(v -> finish());

        String parametroInicial = getIntent().getStringExtra("parametro_inicial");
        if (parametroInicial != null) parametroActual = parametroInicial;
        seleccionarTabInicial();

        configurarListeners();
        configurarChart();

        tareaPolling = () -> {
            cargarDatos(false);
            handler.postDelayed(tareaPolling, intervaloSegunPeriodo());
        };
        registerReceiver(realtimeReceiver,
            new IntentFilter(RealtimeClient.ACTION_SENSOR_READING),
            Context.RECEIVER_NOT_EXPORTED);
        RealtimeClient.connect(this);
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

    @Override
    protected void onDestroy() {
        unregisterReceiver(realtimeReceiver);
        super.onDestroy();
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
                if (!switchComparar.isChecked()) cargarDatos(true);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Escenario 2: cambio de período
        chipGroupPeriodo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipHoy) periodoActual = "hoy";
            else if (checkedId == R.id.chipSemana) periodoActual = "semana";
            else if (checkedId == R.id.chipMes) periodoActual = "mes";
            cargarDatos(true);
        });

        // Escenario 4: modo comparativo
        switchComparar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tabParametros.setVisibility(isChecked ? android.view.View.GONE : android.view.View.VISIBLE);
            cargarDatos(true);
        });
    }

    private void configurarChart() {
        GraficaUtil.estilo(lineChart, this);
        lineChart.setNoDataText("Cargando…");
    }

    private String[] calcularRangoFechas() {
        Calendar hasta = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar desde = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        switch (periodoActual) {
            case "semana": desde.add(Calendar.DAY_OF_YEAR, -7); break;
            case "mes":    desde.add(Calendar.DAY_OF_YEAR, -30); break;
            default:
                // "Hoy" = últimas 24 horas (no desde medianoche), así siempre
                // se incluyen las lecturas más recientes aunque sean de ayer.
                desde.add(Calendar.DAY_OF_YEAR, -1);
        }
        return new String[]{ FORMATO_API.format(desde.getTime()), FORMATO_API.format(hasta.getTime()) };
    }

    /** @param visible true cuando el usuario cambió algo (muestra la pantalla de carga si tarda). */
    private void cargarDatos(boolean visible) {
        if (cargaEnProgreso) {
            // Un cambio del usuario durante una consulta en curso se aplica al terminar ésta.
            if (visible) recargaPendiente = true;
            return;
        }
        cargaEnProgreso = true;
        if (visible || lineChart.getData() == null) {
            handler.removeCallbacks(mostrarCargando);
            handler.postDelayed(mostrarCargando, RETRASO_CARGANDO_MS);
        }

        String[] rango = calcularRangoFechas();
        String parametros = switchComparar.isChecked() ? "flujo,presion,nivel" : parametroActual;

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerGraficas(parametros, rango[0], rango[1]).enqueue(new Callback<GraficasResponse>() {
            @Override
            public void onResponse(@NonNull Call<GraficasResponse> call, @NonNull Response<GraficasResponse> response) {
                terminarCarga();
                if (recargaPendiente) {
                    recargaPendiente = false;
                    cargarDatos(true);
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    if (switchComparar.isChecked()) pintarComparativo(response.body());
                    else pintarIndividual(response.body());
                } else {
                    Toast.makeText(GraficasMonitoreoActivity.this, "No se pudo cargar la gráfica.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<GraficasResponse> call, @NonNull Throwable t) {
                terminarCarga();
                Toast.makeText(GraficasMonitoreoActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void terminarCarga() {
        cargaEnProgreso = false;
        handler.removeCallbacks(mostrarCargando);
        layoutCargando.setVisibility(android.view.View.GONE);
    }

    //  Escenario 1 y 3
    private void pintarIndividual(GraficasResponse body) {
        ParametroData datoParametro = obtenerParametro(body, parametroActual);

        if (datoParametro == null || datoParametro.getDatos() == null || datoParametro.getDatos().isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            tvTituloGrafica.setText(tituloParametro(parametroActual) + " — sin datos en este período");
            tvUltimaActualizacion.setText("Sin lecturas registradas");
            tvResumenGrafica.setVisibility(android.view.View.GONE);
            cardAlertaRango.setVisibility(android.view.View.GONE);
            return;
        }

        List<SerieDato> datos = datoParametro.getDatos();
        List<Entry> entradasNormales = new ArrayList<>();
        List<Entry> entradasAlerta = new ArrayList<>();
        List<String> etiquetasHora = new ArrayList<>();
        boolean hayAlerta = false;
        double suma = 0, minimo = Double.MAX_VALUE, maximo = -Double.MAX_VALUE;

        for (int i = 0; i < datos.size(); i++) {
            SerieDato d = datos.get(i);
            entradasNormales.add(new Entry(i, (float) d.getValor()));
            etiquetasHora.add(formatearEtiqueta(d.getFecha()));
            suma += d.getValor();
            minimo = Math.min(minimo, d.getValor());
            maximo = Math.max(maximo, d.getValor());
            if (d.isFueraDeRango()) {
                entradasAlerta.add(new Entry(i, (float) d.getValor()));
                hayAlerta = true;
            }
        }

        int colorNormal = ContextCompat.getColor(this, R.color.brand_accent);
        LineDataSet setNormal = GraficaUtil.linea(entradasNormales, tituloParametro(parametroActual), colorNormal, true);

        LineData lineData;
        if (!entradasAlerta.isEmpty()) {
            LineDataSet setAlerta = new LineDataSet(entradasAlerta, "Fuera de rango");
            int colorAlerta = ContextCompat.getColor(this, R.color.text_alert);
            setAlerta.setColor(colorAlerta);
            setAlerta.setCircleColor(colorAlerta);
            setAlerta.setDrawCircleHole(false);
            setAlerta.setLineWidth(0f);
            setAlerta.enableDashedLine(0f, 1f, 0f);
            setAlerta.setCircleRadius(3f);
            setAlerta.setDrawValues(false);
            lineData = new LineData(setNormal, setAlerta);
        } else {
            lineData = new LineData(setNormal);
        }

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetasHora));
        lineChart.getXAxis().setDrawLabels(true);
        lineChart.getLegend().setEnabled(!entradasAlerta.isEmpty());
        lineChart.setData(lineData);
        lineChart.invalidate();

        String unidad = datoParametro.getUnidad() == null ? "" : datoParametro.getUnidad();
        tvTituloGrafica.setText(tituloParametro(parametroActual) + (unidad.isEmpty() ? "" : " (" + unidad + ")"));
        double actual = datos.get(datos.size() - 1).getValor();
        tvResumenGrafica.setText(String.format(Locale.getDefault(),
                "Actual %s  ·  Promedio %s  ·  Mín %s  ·  Máx %s",
                numero(actual), numero(suma / datos.size()), numero(minimo), numero(maximo)));
        tvResumenGrafica.setVisibility(android.view.View.VISIBLE);
        String ultima = "Última lectura: " + formatearHora(datos.get(datos.size() - 1).getFecha());
        tvUltimaActualizacion.setText(AvisosFuga.esAdminConSesion(this) ? "Promedio de todas las viviendas · " + ultima : ultima);

        if (hayAlerta) {
            cardAlertaRango.setVisibility(android.view.View.VISIBLE);
            tvAlertaRango.setText("Valor fuera de rango detectado en " + tituloParametro(parametroActual));
        } else {
            cardAlertaRango.setVisibility(android.view.View.GONE);
        }
    }

    //Escenario 4
    private void pintarComparativo(GraficasResponse body) {
        List<LineDataSet> conjuntos = new ArrayList<>();
        boolean hayAlerta = false;

        hayAlerta |= agregarSerieComparativa(body.getFlujo(), "Flujo", R.color.brand_blue, conjuntos);
        hayAlerta |= agregarSerieComparativa(body.getPresion(), "Presión", R.color.status_orange, conjuntos);
        hayAlerta |= agregarSerieComparativa(body.getNivel(), "Nivel", R.color.success, conjuntos);

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
        lineChart.getXAxis().setDrawLabels(false);
        tvResumenGrafica.setText("Cada línea muestra la tendencia de su parámetro en el período elegido.");
        tvResumenGrafica.setVisibility(android.view.View.VISIBLE);
        lineChart.setData(lineData);
        lineChart.getLegend().setEnabled(true);
        lineChart.invalidate();

        tvTituloGrafica.setText("Comparación: Flujo, Presión y Nivel");
        tvUltimaActualizacion.setText("Modo comparativo activo");

        cardAlertaRango.setVisibility(hayAlerta ? android.view.View.VISIBLE : android.view.View.GONE);
        if (hayAlerta) tvAlertaRango.setText("Uno o más parámetros están fuera de rango");
    }

    private boolean agregarSerieComparativa(ParametroData datoParametro, String nombre, int colorRes, List<LineDataSet> destino) {
        if (datoParametro == null || datoParametro.getDatos() == null || datoParametro.getDatos().isEmpty()) return false;

        List<Entry> entradas = new ArrayList<>();
        boolean hayAlerta = false;
        List<SerieDato> datos = datoParametro.getDatos();

        for (int i = 0; i < datos.size(); i++) {
            entradas.add(new Entry(i, (float) datos.get(i).getValor()));
            if (datos.get(i).isFueraDeRango()) hayAlerta = true;
        }

        LineDataSet set = GraficaUtil.linea(entradas, nombre, ContextCompat.getColor(this, colorRes), false);
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

    /** Hora ("14:30") para "Hoy"; día ("06/10") para semana y mes. */
    private String formatearEtiqueta(String fechaIso) {
        if (periodoActual.equals("hoy")) return formatearHora(fechaIso);
        try {
            java.util.Date fecha = FORMATO_API.parse(fechaIso.length() > 19 ? fechaIso.substring(0, 19) : fechaIso);
            return FORMATO_DIA.format(fecha);
        } catch (Exception e) {
            return "";
        }
    }

    private static String numero(double valor) {
        return String.format(Locale.getDefault(), Math.abs(valor) < 10 ? "%.2f" : "%.1f", valor);
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
