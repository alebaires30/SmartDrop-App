package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
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
                    if (response.body().getLogs() != null) {
                        adapterLogs.setLogs(response.body().getLogs());
                    }
                }
            }
            @Override
            public void onFailure(Call<ValvulaLogsResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Error al obtener historial", Toast.LENGTH_SHORT).show();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                realizarBusquedaGlobal(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    private void realizarBusquedaGlobal(String query) {
        String q = query.toLowerCase().trim();
        Intent intent = null;

        // Búsqueda proactiva por palabras clave
        if (q.contains("valv") || q.contains("abrir") || q.contains("cerrar") || q.contains("log") || q.contains("historial")) {
            intent = new Intent(this, ValvulaActivity.class);
        } else if (q.contains("presion") || q.contains("red") || q.contains("fuerza")) {
            intent = new Intent(this, PresionActivity.class);
        } else if (q.contains("nivel") || q.contains("agua") || q.contains("tanque") || q.contains("lleno")) {
            intent = new Intent(this, NivelTanqueActivity.class);
        } else if (q.contains("calidad") || q.contains("ph") || q.contains("cloro") || q.contains("sucio") || q.contains("limpia")) {
            intent = new Intent(this, CalidadActivity.class);
        } else if (q.contains("consumo") || q.contains("gasto") || q.contains("pago") || q.contains("litro")) {
            intent = new Intent(this, ConsumoActivity.class);
        } else if (q.contains("grafic") || q.contains("monitoreo") || q.contains("analis")) {
            intent = new Intent(this, GraficasMonitoreoActivity.class);
        }

        if (intent != null) {
            Toast.makeText(this, "Navegando a: " + query, Toast.LENGTH_SHORT).show();
            startActivity(intent);
        } else {
            // Si no es palabra clave, intentar búsqueda en API
            ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
            api.buscarGlobal(query).enqueue(new Callback<BusquedaGlobalResponse>() {
                @Override
                public void onResponse(Call<BusquedaGlobalResponse> call, Response<BusquedaGlobalResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getSecciones().isEmpty()) {
                        // Por simplicidad, tomamos el primer resultado
                        Toast.makeText(AdminDashboardActivity.this, "Resultado encontrado: " + response.body().getSecciones().get(0).getTitulo(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminDashboardActivity.this, "No se encontró '" + query + "'. Prueba con 'válvula', 'presión' o 'calidad'.", Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onFailure(Call<BusquedaGlobalResponse> call, Throwable t) {
                    Toast.makeText(AdminDashboardActivity.this, "Error de búsqueda", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
