package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import retrofit2.Call;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;

    private CardView cardFlujo, cardPresion, cardNivelAdmin, cardAlertasActivas;

    private TextView tvFlujoValor, tvPresionValorAdmin, tvNivelValorAdmin, tvAlertasCount;

    private CardView cardAlertaGeneral;
    private TextView tvAlertaGeneral;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final long INTERVALO_POLLING_MS = 10000;
    private Runnable tareaPolling;
    private boolean cargaEnProgreso = false;
    private boolean primeraCargaCompleta = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationViewAdmin);
        btnMenu        = findViewById(R.id.btnMenu);
        cardAlertaGeneral = findViewById(R.id.cardAlertaGeneral);
        tvAlertaGeneral    = findViewById(R.id.tvAlertaGeneral);


        cardFlujo          = findViewById(R.id.cardFlujo);
        cardPresion        = findViewById(R.id.cardPresion);
        cardNivelAdmin     = findViewById(R.id.cardNivelAdmin);
        cardAlertasActivas = findViewById(R.id.cardAlertasActivas);

        tvFlujoValor        = findViewById(R.id.tvFlujoValor);
        tvPresionValorAdmin = findViewById(R.id.tvPresionValorAdmin);
        tvNivelValorAdmin   = findViewById(R.id.tvNivelValorAdmin);
        tvAlertasCount      = findViewById(R.id.tvAlertasCount);

        float alphaInicial = 0.5f;
        cardFlujo.setAlpha(alphaInicial);
        cardPresion.setAlpha(alphaInicial);
        cardNivelAdmin.setAlpha(alphaInicial);
        cardAlertasActivas.setAlpha(alphaInicial);


        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Administrador");

        android.view.View header = navigationView.getHeaderView(0);
        TextView tvUsuarioDrawer = header.findViewById(R.id.tvUsuarioDrawer);
        tvUsuarioDrawer.setText(nombre);


        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Navegación del menú lateral
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.drawer_admin_dashboard) {
                drawerLayout.closeDrawer(GravityCompat.START);

            } else if (id == R.id.drawer_admin_graficas) {
                startActivity(new Intent(AdminDashboardActivity.this, GraficasMonitoreoActivity.class));

            } else if (id == R.id.drawer_admin_configuracion) {
                android.widget.Toast.makeText(this, "Próximamente", android.widget.Toast.LENGTH_SHORT).show();

            } else if (id == R.id.drawer_admin_cerrar_sesion) {
                cerrarSesion();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        cardFlujo.setOnClickListener(v -> abrirGraficas("flujo"));
        cardPresion.setOnClickListener(v -> abrirGraficas("presion"));
        cardNivelAdmin.setOnClickListener(v -> abrirGraficas("nivel"));

        tareaPolling = () -> {
            cargarResumen();
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

    private void cargarResumen() {
        if (cargaEnProgreso) return;
        cargaEnProgreso = true;

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);

        api.obtenerResumen().enqueue(new retrofit2.Callback<ResumenDashboardResponse>() {
            @Override
            public void onResponse(Call<ResumenDashboardResponse> call, Response<ResumenDashboardResponse> response) {
                cargaEnProgreso = false;
                if (!response.isSuccessful() || response.body() == null) return;
                ResumenDashboardResponse r = response.body();

                if (r.getFlujo() != null) {
                    tvFlujoValor.setText(String.format(java.util.Locale.getDefault(), "%.2f %s",
                            r.getFlujo().getValor(), r.getFlujo().getUnidad()));
                }
                if (r.getPresion() != null) {
                    tvPresionValorAdmin.setText(String.format(java.util.Locale.getDefault(), "%.1f %s",
                            r.getPresion().getValor(), r.getPresion().getUnidad()));
                }
                if (r.getNivel() != null) {
                    tvNivelValorAdmin.setText(String.format(java.util.Locale.getDefault(), "%.0f %%", r.getNivel().getValor()));
                }

                tvAlertasCount.setText(r.getTotalAlertas() + " activas");

                if (r.getTotalAlertas() > 0) {
                    cardAlertaGeneral.setVisibility(android.view.View.VISIBLE);
                    tvAlertaGeneral.setText("⚠️ " + r.getTotalAlertas() + " parámetro(s) fuera de rango");
                } else {
                    cardAlertaGeneral.setVisibility(android.view.View.GONE);
                }


                if (!primeraCargaCompleta) {
                    primeraCargaCompleta = true;
                    long duracion = 400;
                    cardFlujo.animate().alpha(1f).setDuration(duracion).start();
                    cardPresion.animate().alpha(1f).setDuration(duracion).start();
                    cardNivelAdmin.animate().alpha(1f).setDuration(duracion).start();
                    cardAlertasActivas.animate().alpha(1f).setDuration(duracion).start();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResumenDashboardResponse> call, Throwable t) {
                cargaEnProgreso = false;
                android.widget.Toast.makeText(AdminDashboardActivity.this,
                        "No se pudo cargar el resumen: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }

        });

    }


    private void abrirGraficas(String parametro) {
        Intent intent = new Intent(AdminDashboardActivity.this, GraficasMonitoreoActivity.class);
        intent.putExtra("parametro_inicial", parametro);
        startActivity(intent);
    }

    private void cerrarSesion() {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
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