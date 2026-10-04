package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialReportesActivity extends BaseActivity {

    private ReportesAdapter adapter;
    private TextView tvVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_reportes);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        RecyclerView recycler = findViewById(R.id.recyclerReportes);
        tvVacio = findViewById(R.id.tvReportesVacio);
        View btnComunidad = findViewById(R.id.btnComunidad);
        View btnNuevoReporte = findViewById(R.id.btnNuevoReporte);

        btnVolver.setOnClickListener(v -> finish());
        btnNuevoReporte.setOnClickListener(v ->
                startActivity(new Intent(this, ReportarProblemaActivity.class)));
        btnComunidad.setOnClickListener(v ->
                startActivity(new Intent(this, ComunidadActivity.class)));

        adapter = new ReportesAdapter(reporte -> {
            Intent intent = new Intent(this, ReporteDetalleActivity.class);
            intent.putExtra("id_reporte", reporte.getIdReporte());
            startActivity(intent);
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarReportes();
    }

    private void cargarReportes() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerMisReportes().enqueue(new Callback<ReporteListResponse>() {
            @Override
            public void onResponse(Call<ReporteListResponse> call, Response<ReporteListResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) return;
                adapter.setReportes(response.body().getReportes());
                tvVacio.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ReporteListResponse> call, Throwable t) {
                Toast.makeText(HistorialReportesActivity.this,
                        "Sin conexión al cargar reportes", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
