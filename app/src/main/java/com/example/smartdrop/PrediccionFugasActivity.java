package com.example.smartdrop;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Predicción de fugas (solo admin): estado del monitor automático, avisos sin leer y la última
 * evaluación de cada vivienda, con las posibles fugas primero y todos sus detalles.
 */
public class PrediccionFugasActivity extends BaseActivity {

    private static final Pattern DESFASE = Pattern.compile("([+-]\\d{2}:\\d{2})$");

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvMonitor, tvAvisos, tvSinDatos;
    private Button btnMarcarLeidos;
    private FugasAdapter adapter;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediccion_fugas);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvMonitor = findViewById(R.id.tvEstadoMonitor);
        tvAvisos = findViewById(R.id.tvAvisosSinLeer);
        tvSinDatos = findViewById(R.id.tvSinDatos);
        btnMarcarLeidos = findViewById(R.id.btnMarcarLeidos);
        btnMarcarLeidos.setOnClickListener(v -> marcarLeidos());

        adapter = new FugasAdapter();
        RecyclerView recycler = findViewById(R.id.recyclerFugas);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        swipeRefresh.setOnRefreshListener(this::cargar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        swipeRefresh.setRefreshing(true);
        api.obtenerFugas().enqueue(new Callback<FugasResponse>() {
            @Override
            public void onResponse(Call<FugasResponse> call, Response<FugasResponse> response) {
                swipeRefresh.setRefreshing(false);
                if (response.code() == 403) {
                    Toast.makeText(PrediccionFugasActivity.this,
                            "Esta sección es solo para administradores", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PrediccionFugasActivity.this,
                            "No se pudo cargar el estado de las viviendas", Toast.LENGTH_SHORT).show();
                    return;
                }
                FugasResponse datos = response.body();
                List<FugaVivienda> viviendas = datos.getViviendas();
                adapter.setViviendas(viviendas);
                tvSinDatos.setVisibility(viviendas == null || viviendas.isEmpty() ? View.VISIBLE : View.GONE);
                pintarMonitor(datos);
            }

            @Override
            public void onFailure(Call<FugasResponse> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PrediccionFugasActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
        cargarAvisos();
    }

    private void cargarAvisos() {
        api.obtenerAlertasFuga(1).enqueue(new Callback<AlertasFugaResponse>() {
            @Override
            public void onResponse(Call<AlertasFugaResponse> call, Response<AlertasFugaResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                int sinLeer = response.body().getNoLeidas();
                tvAvisos.setText(sinLeer == 0 ? "Sin avisos de fuga pendientes"
                        : sinLeer + (sinLeer == 1 ? " aviso de fuga sin leer" : " avisos de fuga sin leer"));
                tvAvisos.setTextColor(ContextCompat.getColor(PrediccionFugasActivity.this,
                        sinLeer == 0 ? R.color.text_secondary : R.color.text_alert));
                btnMarcarLeidos.setVisibility(sinLeer == 0 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<AlertasFugaResponse> call, Throwable t) { }
        });
    }

    private void marcarLeidos() {
        btnMarcarLeidos.setEnabled(false);
        Map<String, Object> cuerpo = Collections.singletonMap("todas", true);
        api.marcarAlertasFugaLeidas(cuerpo).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnMarcarLeidos.setEnabled(true);
                if (!response.isSuccessful()) {
                    Toast.makeText(PrediccionFugasActivity.this,
                            "No se pudieron marcar como leídos", Toast.LENGTH_SHORT).show();
                    return;
                }
                AvisosFuga.quitarNotificaciones(PrediccionFugasActivity.this);
                cargarAvisos();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnMarcarLeidos.setEnabled(true);
                Toast.makeText(PrediccionFugasActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pintarMonitor(FugasResponse datos) {
        FugasResponse.Monitor monitor = datos.getMonitor();
        int cada = monitor == null ? 10 : (int) Math.round(monitor.getCadaMinutos());
        Date ultimo = monitor == null ? null : parseFecha(monitor.getUltimoCiclo());
        boolean activo = false;
        String texto;
        if (ultimo == null) {
            texto = "La vigilancia automática aún no ha hecho su primer análisis (revisa cada " + cada + " min).";
        } else {
            long minutos = Math.max(0, (System.currentTimeMillis() - ultimo.getTime()) / 60000);
            if (minutos > cada * 2L + 1) {
                texto = "La vigilancia automática lleva " + minutos + " min sin analizar. Revisa que el servidor esté encendido.";
            } else {
                activo = "ok".equals(monitor.getEstado());
                int posibles = 0;
                if (datos.getViviendas() != null) {
                    for (FugaVivienda v : datos.getViviendas()) if (v.isPosibleFuga()) posibles++;
                }
                texto = String.format(Locale.getDefault(), "Vigilancia automática activa · último análisis hace %d min\n%s",
                        minutos, posibles == 0 ? "Ninguna vivienda con posible fuga"
                                : posibles + (posibles == 1 ? " vivienda con posible fuga" : " viviendas con posible fuga"));
            }
        }
        tvMonitor.setText(texto);
        tvMonitor.setTextColor(ContextCompat.getColor(this, activo ? R.color.status_green : R.color.status_amber));
    }

    /** Convierte una fecha ISO 8601 del servidor (terminada en Z o en un desfase ±hh:mm) a Date; null si no es válida. */
    static Date parseFecha(String iso) {
        if (iso == null || iso.length() < 19) return null;
        try {
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Matcher desfase = DESFASE.matcher(iso);
            formato.setTimeZone(TimeZone.getTimeZone(desfase.find() ? "GMT" + desfase.group(1) : "UTC"));
            return formato.parse(iso.substring(0, 19));
        } catch (ParseException e) {
            return null;
        }
    }

    /** Fecha y hora local corta ("06/10 14:25"), o cadena vacía si la fecha no es válida. */
    static String fechaCorta(String iso) {
        Date fecha = parseFecha(iso);
        return fecha == null ? "" : new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(fecha);
    }
}
