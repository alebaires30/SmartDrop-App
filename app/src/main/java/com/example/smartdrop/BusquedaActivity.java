package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BusquedaActivity extends BaseActivity {

    private EditText etSearchQuery;
    private ImageButton btnSearchSubmit, btnSearchBack;
    private RecyclerView rvSearchResults;
    private ProgressBar pbSearchLoading;
    private TextView tvSearchEmpty;

    private BusquedaAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busqueda);

        // Vistas
        etSearchQuery = findViewById(R.id.etSearchQuery);
        btnSearchSubmit = findViewById(R.id.btnSearchSubmit);
        btnSearchBack = findViewById(R.id.btnSearchBack);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        pbSearchLoading = findViewById(R.id.pbSearchLoading);
        tvSearchEmpty = findViewById(R.id.tvSearchEmpty);

        // Configurar RecyclerView
        adapter = new BusquedaAdapter(this::navegarASeccion);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);

        // Listeners
        btnSearchBack.setOnClickListener(v -> finish());
        btnSearchSubmit.setOnClickListener(v -> ejecutarBusqueda());

        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                ejecutarBusqueda();
                return true;
            }
            return false;
        });

        // Query inicial opcional si se pasa por intent
        String queryInicial = getIntent().getStringExtra("query_inicial");
        if (queryInicial != null && !queryInicial.trim().isEmpty()) {
            etSearchQuery.setText(queryInicial);
            ejecutarBusqueda();
        }
    }

    private void ejecutarBusqueda() {
        String q = etSearchQuery.getText().toString().trim();
        if (q.isEmpty()) {
            Idioma.toast(this, "Ingresa un término para buscar", Toast.LENGTH_SHORT);
            return;
        }

        pbSearchLoading.setVisibility(View.VISIBLE);
        tvSearchEmpty.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.buscarGlobal(q).enqueue(new Callback<BusquedaGlobalResponse>() {
            @Override
            public void onResponse(Call<BusquedaGlobalResponse> call, Response<BusquedaGlobalResponse> response) {
                pbSearchLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    BusquedaGlobalResponse body = response.body();
                    List<BusquedaGlobalResponse.Seccion> secciones = body.getSecciones();

                    if (secciones == null || secciones.isEmpty()) {
                        tvSearchEmpty.setText("No se encontraron resultados para '" + q + "'");
                        tvSearchEmpty.setVisibility(View.VISIBLE);
                        adapter.setResultados(null);
                    } else {
                        tvSearchEmpty.setVisibility(View.GONE);
                        rvSearchResults.setVisibility(View.VISIBLE);
                        adapter.setResultados(secciones);
                    }
                } else {
                    tvSearchEmpty.setText("Error al consultar el servidor (" + response.code() + ")");
                    tvSearchEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<BusquedaGlobalResponse> call, Throwable t) {
                pbSearchLoading.setVisibility(View.GONE);
                tvSearchEmpty.setText("Error de conexión: " + t.getMessage());
                tvSearchEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void navegarASeccion(BusquedaGlobalResponse.Seccion seccion) {
        if (seccion == null) return;

        String url = seccion.getUrl() != null ? seccion.getUrl().toLowerCase().trim() : "";

        Intent intent = null;

        // Mapeo de URLs del backend Django a pantallas nativas Android
        if (url.equals("/") || url.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
            int idRol = prefs.getInt("id_rol", 1);
            if (idRol == 2) {
                intent = new Intent(this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(this, InicioActivity.class);
            }
        } else if (url.contains("valvula") || url.contains("electrovalvula")) {
            intent = new Intent(this, ValvulaActivity.class);
            intent.putExtra("id_valvula", 1);
        } else if (url.contains("tanque") || url.contains("nivel")) {
            intent = new Intent(this, NivelTanqueActivity.class);
        } else if (url.contains("presion")) {
            intent = new Intent(this, PresionActivity.class);
        } else if (url.contains("calidad")) {
            intent = new Intent(this, CalidadActivity.class);
        } else if (url.contains("consumo")) {
            intent = new Intent(this, ConsumoActivity.class);
        } else if (url.contains("retroalimentacion")) {
            intent = new Intent(this, RetroalimentacionActivity.class);
        } else if (url.contains("recomendaciones")) {
            intent = new Intent(this, RecomendacionesActivity.class);
        } else if (url.contains("admin-panel")) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if (url.contains("usuario")) {
            // Informatorio de perfil
            new AlertDialog.Builder(this)
                    .setTitle("Perfil de Usuario")
                    .setMessage(seccion.getContenido() != null ? seccion.getContenido() : "Información de perfil del usuario.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        if (intent != null) {
            startActivity(intent);
        } else {
            Idioma.toast(this, seccion.getTitulo() + ": " + seccion.getContenido(), Toast.LENGTH_LONG);
        }
    }
}
