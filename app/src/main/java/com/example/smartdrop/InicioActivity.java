package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InicioActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private TextView tvResumenGeneral;

    private CardView cardPresion, cardCalidad, cardNivel, cardConsumo;
    private TextView tvPresionValor, tvCalidadValor, tvNivelPorcentaje, tvNivelLitros;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long INTERVALO_POLLING_MS = 10000;
    private Runnable tareaPolling;
    private boolean cargaEnProgreso = false;

    private TextView tvConsumoLitros;
    private boolean primeraCargaCompleta = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu        = findViewById(R.id.btnMenu);

        cardPresion = findViewById(R.id.cardPresion);
        cardCalidad = findViewById(R.id.cardCalidad);
        cardNivel   = findViewById(R.id.cardNivel);
        cardConsumo = findViewById(R.id.cardConsumo);

        tvPresionValor    = findViewById(R.id.tvPresionValor);
        tvCalidadValor    = findViewById(R.id.tvCalidadValor);
        tvNivelPorcentaje = findViewById(R.id.tvNivelPorcentaje);
        tvNivelLitros     = findViewById(R.id.tvNivelLitros);
        tvResumenGeneral = findViewById(R.id.tvResumenGeneral);
        tvConsumoLitros = findViewById(R.id.tvConsumoLitros);

        float alphaInicial = 0.5f;
        cardPresion.setAlpha(alphaInicial);
        cardCalidad.setAlpha(alphaInicial);
        cardNivel.setAlpha(alphaInicial);
        cardConsumo.setAlpha(alphaInicial);

        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        android.view.View header = navigationView.getHeaderView(0);
        TextView tvUsuarioDrawer = header.findViewById(R.id.tvUsuarioDrawer);
        tvUsuarioDrawer.setText(nombre);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_cerrar_sesion) {
                cerrarSesion();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        cardPresion.setOnClickListener(v -> startActivity(new Intent(this, PresionActivity.class)));
        cardCalidad.setOnClickListener(v -> startActivity(new Intent(this, CalidadActivity.class)));
        cardNivel.setOnClickListener(v -> startActivity(new Intent(this, NivelTanqueActivity.class)));
        cardConsumo.setOnClickListener(v -> startActivity(new Intent(this, ConsumoActivity.class)));

        tareaPolling = () -> {
            cargarEstadoAgua();
            handler.postDelayed(tareaPolling, INTERVALO_POLLING_MS);
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tareaPolling);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tareaPolling);
    }

    private void cargarEstadoAgua() {
        if (cargaEnProgreso) return;
        cargaEnProgreso = true;


        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoAgua().enqueue(new Callback<EstadoAguaResponse>() {
            @Override
            public void onResponse(Call<EstadoAguaResponse> call, Response<EstadoAguaResponse> response) {
                cargaEnProgreso = false;
                if (!response.isSuccessful() || response.body() == null) return;
                EstadoAguaResponse r = response.body();

                if (r.getResumenGeneral() != null) {
                    tvResumenGeneral.setText(ColorSeveridad.iconoDe(r.getResumenGeneral().getColor()) + " " + r.getResumenGeneral().getMensaje());
                    tvResumenGeneral.setTextColor(ColorSeveridad.colorDe(r.getResumenGeneral().getColor()));
                }

                if (r.getPresion() != null) {
                    tvPresionValor.setText(ColorSeveridad.iconoDe(r.getPresion().getColor()) + " " + r.getPresion().getEstado());
                    tvPresionValor.setTextColor(ColorSeveridad.colorDe(r.getPresion().getColor()));
                }
                if (r.getCalidad() != null) {
                    tvCalidadValor.setText(ColorSeveridad.iconoDe(r.getCalidad().getColor()) + " " + r.getCalidad().getEstado());
                    tvCalidadValor.setTextColor(ColorSeveridad.colorDe(r.getCalidad().getColor()));
                }
                if (r.getNivel() != null) {
                    tvNivelPorcentaje.setText(String.format(java.util.Locale.getDefault(), "%.0f %%", r.getNivel().getPorcentaje()));
                    tvNivelPorcentaje.setTextColor(ColorSeveridad.colorDe(r.getNivel().getColor()));
                    tvNivelLitros.setText(String.format(java.util.Locale.getDefault(), "%.0fL disponibles", r.getNivel().getLitrosDisponibles()));
                }
                if (r.getConsumo() != null) {
                    tvConsumoLitros.setText(String.format(java.util.Locale.getDefault(), "%.1fL", r.getConsumo().getLitrosHoy()));
                }


                if (!primeraCargaCompleta) {
                    primeraCargaCompleta = true;
                    long duracion = 400;
                    cardPresion.animate().alpha(1f).setDuration(duracion).start();
                    cardCalidad.animate().alpha(1f).setDuration(duracion).start();
                    cardNivel.animate().alpha(1f).setDuration(duracion).start();
                    cardConsumo.animate().alpha(1f).setDuration(duracion).start();
                }
            }

            @Override
            public void onFailure(Call<EstadoAguaResponse> call, Throwable t) {
                cargaEnProgreso = false;
            }
        });
    }

    private void cerrarSesion() {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(InicioActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}