package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComunidadActivity extends BaseActivity {

    private ReportesAdapter adapter;
    private TextView tvVacio;
    private boolean esAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comunidad);

        esAdmin = getIntent().getBooleanExtra("es_admin", false);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        RecyclerView recycler = findViewById(R.id.recyclerComunidad);
        tvVacio = findViewById(R.id.tvComunidadVacia);
        View btnReportar = findViewById(R.id.btnReportarComunidad);
        TextView tvTituloToolbar = findViewById(R.id.tvComunidadTitulo);

        if (esAdmin) {
            if (tvTituloToolbar != null) tvTituloToolbar.setText("Problemas reportados");
            if (btnReportar != null) btnReportar.setVisibility(View.GONE);
        }

        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        if (btnReportar != null) {
            btnReportar.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportarProblemaActivity.class)));
        }

        adapter = new ReportesAdapter(reporte -> {
            Intent intent = new Intent(this, ReporteDetalleActivity.class);
            intent.putExtra("id_reporte", reporte.getIdReporte());
            intent.putExtra("es_admin", esAdmin);
            startActivity(intent);
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarComunidad();
    }

    private void cargarComunidad() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerReportesComunidad().enqueue(new Callback<ReporteListResponse>() {
            @Override
            public void onResponse(Call<ReporteListResponse> call, Response<ReporteListResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) return;
                adapter.setReportes(response.body().getReportes());
                tvVacio.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ReporteListResponse> call, Throwable t) {
                Idioma.toast(ComunidadActivity.this,
                        "Sin conexión al cargar la comunidad", Toast.LENGTH_SHORT);
            }
        });
    }
}
