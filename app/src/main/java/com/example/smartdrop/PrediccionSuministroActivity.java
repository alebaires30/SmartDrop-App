package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    private TextView tvSinDatos, tvResultado;
    private Button btnPredicciones;
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
        tvResultado = findViewById(R.id.tvResultadoPrediccion);
        btnPredicciones = findViewById(R.id.btnRealizarPredicciones);
        btnPredicciones.setOnClickListener(v -> realizarPredicciones());

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

    private void realizarPredicciones() {
        btnPredicciones.setEnabled(false);
        tvResultado.setVisibility(View.GONE);
        PrediccionRunner.ejecutar(this, new PrediccionRunner.Listener() {
            @Override
            public void onTerminado(JsonObject resultado) {
                btnPredicciones.setEnabled(true);
                tvResultado.setText(resumen(resultado));
                tvResultado.setVisibility(View.VISIBLE);
                cargarZonas();
            }

            @Override
            public void onError(String mensaje) {
                btnPredicciones.setEnabled(true);
                tvResultado.setText(mensaje);
                tvResultado.setVisibility(View.VISIBLE);
            }
        });
    }

    private String resumen(JsonObject r) {
        JsonArray tanques = r.has("tanques") && r.get("tanques").isJsonArray() ? r.getAsJsonArray("tanques") : new JsonArray();
        int enRiesgo = 0;
        for (JsonElement e : tanques) {
            String riesgo = e.getAsJsonObject().get("nivel_riesgo").getAsString();
            if (riesgo.equals("medio") || riesgo.equals("alto") || riesgo.equals("critico")) enRiesgo++;
        }
        JsonObject fugas = r.has("fugas") && r.get("fugas").isJsonObject() ? r.getAsJsonObject("fugas") : new JsonObject();
        int posibles = fugas.has("posibles_fugas") ? fugas.get("posibles_fugas").getAsInt() : 0;
        int avisos = fugas.has("alertas_creadas") ? fugas.get("alertas_creadas").getAsInt() : 0;
        String duracion = r.has("duracion_s") ? r.get("duracion_s").getAsString() : "?";
        String texto = "Predicción terminada en " + duracion + " s: " + tanques.size() + " tanques analizados, "
                + enRiesgo + " con riesgo de desabasto, " + posibles + " posible(s) fuga(s)";
        if (avisos > 0) texto += " (" + avisos + " aviso(s) nuevo(s))";
        if (r.has("modelos") && r.get("modelos").isJsonObject()
                && r.getAsJsonObject("modelos").has("error")) {
            texto += ".\n" + r.getAsJsonObject("modelos").get("error").getAsString();
        }
        return texto + ".";
    }

    private void cargarZonas() {
        swipeRefresh.setRefreshing(true);
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerResumenZonas().enqueue(new Callback<ZonasSummaryResponse>() {
            @Override
            public void onResponse(Call<ZonasSummaryResponse> call, Response<ZonasSummaryResponse> response) {
                swipeRefresh.setRefreshing(false);
                if (response.code() == 403) {
                    Idioma.toast(PrediccionSuministroActivity.this,
                            "Esta sección es solo para administradores", Toast.LENGTH_LONG);
                    finish();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Idioma.toast(PrediccionSuministroActivity.this,
                            "No se pudieron cargar las zonas", Toast.LENGTH_SHORT);
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
                Idioma.toast(PrediccionSuministroActivity.this,
                        "Sin conexión con el servidor", Toast.LENGTH_SHORT);
            }
        });
    }
}
