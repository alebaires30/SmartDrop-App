package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de predicción de suministro (solo admin).
 * Lista las zonas con su nivel, riesgo y anomalías; al tocar una zona abre el detalle.
 */
public class PrediccionSuministroActivity extends BaseActivity {

    private RecyclerView recyclerZonas;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvSinDatos;
    private ZonasPrediccionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediccion_suministro);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        recyclerZonas = findViewById(R.id.recyclerZonas);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvSinDatos = findViewById(R.id.tvSinDatos);

        adapter = new ZonasPrediccionAdapter(zona -> {
            Intent intent = new Intent(this, ZonaPrediccionDetalleActivity.class);
            intent.putExtra("zone_id", zona.getZoneId());
            intent.putExtra("zone_name", zona.getZoneName());
            startActivity(intent);
        });

        recyclerZonas.setLayoutManager(new LinearLayoutManager(this));
        recyclerZonas.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::cargarZonas);

        cargarZonas();
    }

    private void cargarZonas() {
        swipeRefresh.setRefreshing(true);
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerResumenZonas().enqueue(new Callback<ZonasSummaryResponse>() {
            @Override
            public void onResponse(Call<ZonasSummaryResponse> call, Response<ZonasSummaryResponse> response) {
                swipeRefresh.setRefreshing(false);
                if (response.code() == 403) {
                    Toast.makeText(PrediccionSuministroActivity.this,
                            "Esta sección es solo para administradores", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PrediccionSuministroActivity.this,
                            "No se pudieron cargar las zonas", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<ZonaResumen> zonas = response.body().getZonas();
                adapter.setZonas(zonas);
                boolean vacio = zonas == null || zonas.isEmpty();
                tvSinDatos.setVisibility(vacio ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ZonasSummaryResponse> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PrediccionSuministroActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
