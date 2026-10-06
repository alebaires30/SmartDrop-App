package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RetroalimentacionActivity extends BaseActivity {

    private ImageButton btnVolver;
    private TextView tvMensaje, tvValorGota, tvComparacionMes, tvRacha;
    private android.widget.ProgressBar progressConsumo;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retroalimentacion);

        btnVolver        = findViewById(R.id.btnVolver);
        tvMensaje        = findViewById(R.id.tvMensaje);
        tvValorGota      = findViewById(R.id.tvValorGota);
        tvComparacionMes = findViewById(R.id.tvComparacionMes);
        tvRacha          = findViewById(R.id.tvRacha);
        progressConsumo  = findViewById(R.id.progressConsumo);
        bottomNav        = findViewById(R.id.bottomNavRetro);

        btnVolver.setOnClickListener(v -> finish());
        bottomNav.setSelectedItemId(R.id.nav_retroalimentacion);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_consumo) {
                startActivity(new Intent(this, ConsumoActivity.class));
                finish();
            } else if (id == R.id.nav_recomendaciones) {
                startActivity(new Intent(this, RecomendacionesActivity.class));
                finish();
            }
            return true;
        });

        cargarRetroalimentacion();
    }

    private void cargarRetroalimentacion() {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "");

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerRetroalimentacion().enqueue(new Callback<RetroalimentacionResponse>() {
            @Override
            public void onResponse(Call<RetroalimentacionResponse> call, Response<RetroalimentacionResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                RetroalimentacionResponse r = response.body();

                tvMensaje.setText(r.getMensaje().replace("!", ", " + nombre + "!"));
                tvValorGota.setText(String.format(java.util.Locale.getDefault(), "%.1fL", r.getConsumoHoyLitros()));

                double referencia = (r.getPromedioHabitualLitros() != null && r.getPromedioHabitualLitros() > 0)
                        ? r.getPromedioHabitualLitros() * 2 : 10.0;
                int porcentajeConsumo = (int) Math.round(Math.min(100,
                        (r.getConsumoHoyLitros() / referencia) * 100));
                progressConsumo.setProgress(porcentajeConsumo);
                progressConsumo.setProgressTintList(ColorStateList.valueOf(
                        ColorSeveridad.colorDe(RetroalimentacionActivity.this, r.getColor())));

                if (r.getComparacionMes() != null) {
                    tvComparacionMes.setText(r.getComparacionMes().getTexto());
                }
                tvRacha.setText(r.getRachaTexto());
            }

            @Override
            public void onFailure(Call<RetroalimentacionResponse> call, Throwable t) { }
        });
    }
}
