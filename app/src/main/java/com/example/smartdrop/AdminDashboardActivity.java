package com.example.smartdrop;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AdminDashboardActivity extends AppCompatActivity {

    private final BroadcastReceiver realtimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cargarResumen();
        }
    };

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;

    private CardView cardFlujo, cardPresion, cardNivelAdmin, cardAlertasActivas;
    private TextView tvFlujoValor, tvPresionValorAdmin, tvNivelValorAdmin, tvAlertasCount;

    private CardView cardAlertaGeneral;
    private TextView tvAlertaGeneral;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long INTERVALO_POLLING_MS = 10000;
    private Runnable tareaPolling;
    private boolean cargaEnProgreso = false;
    private boolean primeraCargaCompleta = false;

    private TextView tvValvulaEstadoAdmin;
    private static final int ID_VALVULA = 1;

    private ValvulaLogsAdapter adapterLogs = new ValvulaLogsAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationViewAdmin);
        btnMenu        = findViewById(R.id.btnMenu);
        
        ImageButton btnSearchAdmin = findViewById(R.id.btnSearchAdmin);
        if (btnSearchAdmin != null) {
            btnSearchAdmin.setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, BusquedaActivity.class)));
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

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

        tvValvulaEstadoAdmin = findViewById(R.id.tvValvulaEstadoAdmin);

        CardView cardValvula = findViewById(R.id.cardValvula);
        if (cardValvula != null) {
            cardValvula.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, ValvulaActivity.class);
                startActivity(intent);
            });
        }

        float alphaInicial = 0.5f;
        cardFlujo.setAlpha(alphaInicial);
        cardPresion.setAlpha(alphaInicial);
        cardNivelAdmin.setAlpha(alphaInicial);
        cardAlertasActivas.setAlpha(alphaInicial);

        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Administrador");

        View header = navigationView.getHeaderView(0);
        TextView tvUsuarioDrawer = header.findViewById(R.id.tvUsuarioDrawer);
        tvUsuarioDrawer.setText(nombre);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_admin_dashboard) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.drawer_admin_graficas) {
                startActivity(new Intent(AdminDashboardActivity.this, GraficasMonitoreoActivity.class));
            } else if (id == R.id.drawer_admin_configuracion) {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.drawer_admin_cerrar_sesion) {
                cerrarSesion();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        cardFlujo.setOnClickListener(v -> abrirGraficas("flujo"));
        cardPresion.setOnClickListener(v -> abrirGraficas("presion"));
        cardNivelAdmin.setOnClickListener(v -> abrirGraficas("nivel"));

        inicializarRecyclerView();
        cargarHistorialValvula();

        tareaPolling = () -> {
            cargarResumen();
            cargarEstadoValvula();
            handler.postDelayed(tareaPolling, INTERVALO_POLLING_MS);
        };
        ContextCompat.registerReceiver(this, realtimeReceiver,
            new IntentFilter(RealtimeClient.ACTION_SENSOR_READING),
            ContextCompat.RECEIVER_NOT_EXPORTED);
        RealtimeClient.connect(this);
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

    @Override
    protected void onDestroy() {
        unregisterReceiver(realtimeReceiver);
        super.onDestroy();
    }

    private void cargarResumen() {
        if (cargaEnProgreso) return;
        cargaEnProgreso = true;
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerResumen().enqueue(new Callback<ResumenDashboardResponse>() {
            @Override
            public void onResponse(Call<ResumenDashboardResponse> call, Response<ResumenDashboardResponse> response) {
                cargaEnProgreso = false;
                if (!response.isSuccessful() || response.body() == null) return;
                ResumenDashboardResponse r = response.body();
                if (r.getFlujo() != null) {
                    tvFlujoValor.setText(String.format(Locale.getDefault(), "%.2f %s",
                            r.getFlujo().getValor(), r.getFlujo().getUnidad()));
                }
                if (r.getPresion() != null) {
                    tvPresionValorAdmin.setText(String.format(Locale.getDefault(), "%.1f %s",
                            r.getPresion().getValor(), r.getPresion().getUnidad()));
                }
                if (r.getNivel() != null) {
                    tvNivelValorAdmin.setText(String.format(Locale.getDefault(), "%.0f %%", r.getNivel().getValor()));
                }
                tvAlertasCount.setText(r.getTotalAlertas() + " activas");
                if (r.getTotalAlertas() > 0) {
                    cardAlertaGeneral.setVisibility(View.VISIBLE);
                    tvAlertaGeneral.setText("⚠️ " + r.getTotalAlertas() + " parámetro(s) fuera de rango");
                } else {
                    cardAlertaGeneral.setVisibility(View.GONE);
                }
                if (!primeraCargaCompleta) {
                    primeraCargaCompleta = true;
                    cardFlujo.animate().alpha(1f).setDuration(400).start();
                    cardPresion.animate().alpha(1f).setDuration(400).start();
                    cardNivelAdmin.animate().alpha(1f).setDuration(400).start();
                    cardAlertasActivas.animate().alpha(1f).setDuration(400).start();
                }
            }
            @Override
            public void onFailure(Call<ResumenDashboardResponse> call, Throwable t) {
                cargaEnProgreso = false;
                Toast.makeText(AdminDashboardActivity.this, "No se pudo cargar el resumen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarEstadoValvula() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoValvula(ID_VALVULA).enqueue(new Callback<ValvulaEstadoResponse>() {
            @Override
            public void onResponse(Call<ValvulaEstadoResponse> call, Response<ValvulaEstadoResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                String estado = response.body().getEstadoActual();
                boolean abierta = "abierta".equalsIgnoreCase(estado);
                tvValvulaEstadoAdmin.setText(abierta ? "🟢 ABIERTA" : "🔴 CERRADA");
                tvValvulaEstadoAdmin.setTextColor(abierta ? Color.parseColor("#27AE60") : Color.parseColor("#E74C3C"));
            }
            @Override
            public void onFailure(Call<ValvulaEstadoResponse> call, Throwable t) { }
        });
    }

    private void inicializarRecyclerView() {
        RecyclerView rvLogs = findViewById(R.id.rvLogsValvula);
        if (rvLogs != null) {
            rvLogs.setLayoutManager(new LinearLayoutManager(this));
            rvLogs.setAdapter(adapterLogs);
        }
    }

    private void cargarHistorialValvula() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerLogsValvula(ID_VALVULA).enqueue(new Callback<ValvulaLogsResponse>() {
            @Override
            public void onResponse(Call<ValvulaLogsResponse> call, Response<ValvulaLogsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ValvulaLogs> logs = response.body().getLogs();
                    if (logs != null && !logs.isEmpty()) {
                        adapterLogs.setLogs(logs);
                    } else {
                        adapterLogs.setLogs(null);
                    }
                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "Error " + response.code();
                        Toast.makeText(AdminDashboardActivity.this, "Error al obtener historial: " + err, Toast.LENGTH_SHORT).show();
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFailure(Call<ValvulaLogsResponse> call, Throwable t) {
                // Silencioso en polling
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
