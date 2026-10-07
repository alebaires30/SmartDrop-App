package com.example.smartdrop;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InicioActivity extends BaseActivity {

    private final BroadcastReceiver realtimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cargarEstadoAgua();
            cargarEstadoValvula();
        }
    };

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private TextView tvResumenGeneral;

    private CardView cardPresion, cardCalidad, cardNivel, cardConsumo, cardValvulaInicio;
    private TextView tvPresionValor, tvCalidadValor, tvNivelPorcentaje, tvNivelLitros;
    private TextView tvValvulaEstadoInicio, tvValvulaActualizacionInicio;

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
        // Tema e idioma del servidor (pudieron cambiar en la web) y avisos push según el perfil.
        PreferenciasRemotas.sincronizar(this);
        AvisosUsuario.iniciar(this);

        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu        = findViewById(R.id.btnMenu);

        cardPresion = findViewById(R.id.cardPresion);
        cardCalidad = findViewById(R.id.cardCalidad);
        cardNivel   = findViewById(R.id.cardNivel);
        cardConsumo = findViewById(R.id.cardConsumo);
        cardValvulaInicio = findViewById(R.id.cardValvulaInicio);

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
        if (cardValvulaInicio != null) {
            cardValvulaInicio.setAlpha(alphaInicial);
        }

        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        View header = navigationView.getHeaderView(0);
        TextView tvUsuarioDrawer = header.findViewById(R.id.tvUsuarioDrawer);
        tvUsuarioDrawer.setText(nombre);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        ImageButton btnPerfil = findViewById(R.id.btnPerfil);
        if (btnPerfil != null) {
            btnPerfil.setOnClickListener(v -> startActivity(new Intent(InicioActivity.this, PerfilActivity.class)));
        }

        ImageButton btnSearchInicio = findViewById(R.id.btnSearchInicio);
        if (btnSearchInicio != null) {
            btnSearchInicio.setOnClickListener(v -> startActivity(new Intent(InicioActivity.this, BusquedaActivity.class)));
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_cerrar_sesion) {
                cerrarSesion();
            } else if (id == R.id.drawer_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
            } else if (id == R.id.drawer_reportar) {
                startActivity(new Intent(this, ReportarProblemaActivity.class));
            } else if (id == R.id.drawer_historial) {
                startActivity(new Intent(this, ComunidadActivity.class));
            } else if (id == R.id.drawer_consumo) {
                startActivity(new Intent(this, ConsumoActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        cardPresion.setOnClickListener(v -> startActivity(new Intent(this, PresionActivity.class)));
        cardCalidad.setOnClickListener(v -> startActivity(new Intent(this, CalidadActivity.class)));
        cardNivel.setOnClickListener(v -> startActivity(new Intent(this, NivelTanqueActivity.class)));
        cardConsumo.setOnClickListener(v -> startActivity(new Intent(this, ConsumoActivity.class)));
        if (cardValvulaInicio != null) {
            cardValvulaInicio.setOnClickListener(v -> {
                Intent intent = new Intent(InicioActivity.this, ValvulaActivity.class);
                intent.putExtra("id_valvula", 1);
                startActivity(intent);
            });
        }

        tareaPolling = () -> {
            cargarEstadoAgua();
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
                    tvResumenGeneral.setText(r.getResumenGeneral().getMensaje());
                    tvResumenGeneral.setTextColor(ColorSeveridad.colorDe(InicioActivity.this, r.getResumenGeneral().getColor()));
                }

                if (r.getPresion() != null) {
                    tvPresionValor.setText(r.getPresion().getEstado());
                    tvPresionValor.setTextColor(ColorSeveridad.colorDe(InicioActivity.this, r.getPresion().getColor()));
                }
                if (r.getCalidad() != null) {
                    tvCalidadValor.setText(r.getCalidad().getEstado());
                    tvCalidadValor.setTextColor(ColorSeveridad.colorDe(InicioActivity.this, r.getCalidad().getColor()));
                }
                if (r.getNivel() != null) {
                    tvNivelPorcentaje.setText(String.format(Locale.getDefault(), "%.0f %%", r.getNivel().getPorcentaje()));
                    tvNivelPorcentaje.setTextColor(ColorSeveridad.colorDe(InicioActivity.this, r.getNivel().getColor()));
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
                    if (cardValvulaInicio != null) {
                        cardValvulaInicio.animate().alpha(1f).setDuration(duracion).start();
                    }
                }
            }

            @Override
            public void onFailure(Call<EstadoAguaResponse> call, Throwable t) {
                cargaEnProgreso = false;
            }
        });
    }

    private void cargarEstadoValvula() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoValvula(1).enqueue(new Callback<ValvulaEstadoResponse>() {
            @Override
            public void onResponse(Call<ValvulaEstadoResponse> call, Response<ValvulaEstadoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String estado = response.body().getEstadoActual();
                    boolean abierta = "abierta".equalsIgnoreCase(estado);
                    if (tvValvulaEstadoInicio != null) {
                        tvValvulaEstadoInicio.setText(abierta ? "Con suministro · Abierta" : "Sin suministro · Cerrada");
                        tvValvulaEstadoInicio.setTextColor(ContextCompat.getColor(InicioActivity.this, abierta ? R.color.success : R.color.danger));
                    }
                    if (tvValvulaActualizacionInicio != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                        tvValvulaActualizacionInicio.setText("Actualizado: " + sdf.format(new Date()));
                    }
                }
            }

            @Override
            public void onFailure(Call<ValvulaEstadoResponse> call, Throwable t) {
                if (tvValvulaEstadoInicio != null) {
                    tvValvulaEstadoInicio.setText("Estado no disponible");
                }
            }
        });
    }

    private void cerrarSesion() {
        AvisosUsuario.cancelar(this);
        Idioma.limpiarSesion(this);
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
