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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Predicción de fugas (solo admin): estado del monitor automático, avisos sin leer y la última
 * evaluación de cada vivienda, con las posibles fugas primero y todos sus detalles.
 */
public class PrediccionFugasActivity extends BaseActivity {

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
                    Idioma.toast(PrediccionFugasActivity.this,
                            "Esta sección es solo para administradores", Toast.LENGTH_LONG);
                    finish();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Idioma.toast(PrediccionFugasActivity.this,
                            "No se pudo cargar el estado de las viviendas", Toast.LENGTH_SHORT);
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
                Idioma.toast(PrediccionFugasActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT);
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
                    Idioma.toast(PrediccionFugasActivity.this,
                            "No se pudieron marcar como leídos", Toast.LENGTH_SHORT);
                    return;
                }
                AvisosFuga.quitarNotificaciones(PrediccionFugasActivity.this);
                cargarAvisos();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnMarcarLeidos.setEnabled(true);
                Idioma.toast(PrediccionFugasActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT);
            }
        });
    }

    private void pintarMonitor(FugasResponse datos) {
        FugasResponse.Monitor monitor = datos.getMonitor();
        int cada = monitor == null ? 10 : (int) Math.round(monitor.getCadaMinutos());
        Date ultimo = monitor == null ? null : Fechas.parse(monitor.getUltimoCiclo());
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
}
