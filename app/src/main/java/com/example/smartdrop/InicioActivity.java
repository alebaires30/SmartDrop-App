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

import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InicioActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private TextView tvResumenGeneral;

    private CardView cardPresion, cardCalidad, cardNivel, cardConsumo, cardValvula;
    private TextView tvPresionValor, tvCalidadValor, tvNivelPorcentaje, tvNivelLitros;
    private TextView tvValvulaEstadoInicio, tvValvulaActualizacionInicio;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long INTERVALO_POLLING_MS = 10000;
    private Runnable tareaPolling;
    private boolean cargaEnProgreso = false;
    private static final int ID_VALVULA = 1;

    private TextView tvConsumoLitros;
    private boolean primeraCargaCompleta = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu        = findViewById(R.id.btnMenu);

        cardPresion = findViewById(R.id.cardPresion);
        cardCalidad = findViewById(R.id.cardCalidad);
        cardNivel   = findViewById(R.id.cardNivel);
        cardConsumo = findViewById(R.id.cardConsumo);
        cardValvula = findViewById(R.id.cardValvulaInicio);

        tvPresionValor    = findViewById(R.id.tvPresionValor);
        tvCalidadValor    = findViewById(R.id.tvCalidadValor);
        tvNivelPorcentaje = findViewById(R.id.tvNivelPorcentaje);
        tvNivelLitros     = findViewById(R.id.tvNivelLitros);
        tvResumenGeneral = findViewById(R.id.tvResumenGeneral);
        tvConsumoLitros = findViewById(R.id.tvConsumoLitros);
        tvValvulaEstadoInicio = findViewById(R.id.tvValvulaEstadoInicio);
        tvValvulaActualizacionInicio = findViewById(R.id.tvValvulaActualizacionInicio);

        float alphaInicial = 0.5f;
        cardPresion.setAlpha(alphaInicial);
        cardCalidad.setAlpha(alphaInicial);
        cardNivel.setAlpha(alphaInicial);
        cardConsumo.setAlpha(alphaInicial);
        if (cardValvula != null) cardValvula.setAlpha(alphaInicial);

        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        View header = navigationView.getHeaderView(0);
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
        if (cardValvula != null) {
            cardValvula.setOnClickListener(v -> startActivity(new Intent(this, ValvulaActivity.class)));
        }

        tareaPolling = () -> {
            cargarEstadoAgua();
            cargarEstadoValvula();
            handler.postDelayed(tareaPolling, INTERVALO_POLLING_MS);
        };
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

        if (q.contains("valv") || q.contains("abrir") || q.contains("cerrar") || q.contains("historial")) {
            intent = new Intent(this, ValvulaActivity.class);
        } else if (q.contains("presion") || q.contains("red")) {
            intent = new Intent(this, PresionActivity.class);
        } else if (q.contains("nivel") || q.contains("agua") || q.contains("tanque")) {
            intent = new Intent(this, NivelTanqueActivity.class);
        } else if (q.contains("calidad") || q.contains("ph") || q.contains("cloro")) {
            intent = new Intent(this, CalidadActivity.class);
        } else if (q.contains("consumo") || q.contains("gasto") || q.contains("pago")) {
            intent = new Intent(this, ConsumoActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No se encontró: " + query, Toast.LENGTH_SHORT).show();
        }
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

    private void cargarEstadoValvula() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoValvulaPublico().enqueue(new Callback<ValvulaEstadoPublicoResponse>() {
            @Override
            public void onResponse(Call<ValvulaEstadoPublicoResponse> call, Response<ValvulaEstadoPublicoResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    ValvulaEstadoPublicoResponse.ValvulaData v = response.body().getValvula();
                    boolean abierta = "open".equalsIgnoreCase(v.getClase());
                    tvValvulaEstadoInicio.setText(abierta ? "🟢 Válvula: ABIERTA" : "🔴 Válvula: CERRADA");
                    tvValvulaEstadoInicio.setTextColor(abierta ? Color.parseColor("#27AE60") : Color.parseColor("#E74C3C"));
                    tvValvulaActualizacionInicio.setText("Última actualización: " + v.getUltimaActualizacion());
                    if (cardValvula != null) {
                        cardValvula.setVisibility(View.VISIBLE);
                        cardValvula.setAlpha(1f);
                    }
                } else {
                    // Si falla, intentamos ocultar o mostrar error silencioso
                    if (cardValvula != null) cardValvula.setAlpha(0.3f);
                }
            }
            @Override
            public void onFailure(Call<ValvulaEstadoPublicoResponse> call, Throwable t) {
                if (cardValvula != null) cardValvula.setAlpha(0.3f);
            }
        });
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
                    tvNivelPorcentaje.setText(String.format(Locale.getDefault(), "%.0f %%", r.getNivel().getPorcentaje()));
                    tvNivelPorcentaje.setTextColor(ColorSeveridad.colorDe(r.getNivel().getColor()));
                    tvNivelLitros.setText(String.format(Locale.getDefault(), "%.0fL disponibles", r.getNivel().getLitrosDisponibles()));
                }
                if (r.getConsumo() != null) {
                    tvConsumoLitros.setText(String.format(Locale.getDefault(), "%.1fL", r.getConsumo().getLitrosHoy()));
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
