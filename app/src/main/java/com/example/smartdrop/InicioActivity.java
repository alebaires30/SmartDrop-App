package com.example.smartdrop;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
    // Nivel del tanque en el héroe (tema agua): porcentaje, litros y barra de agua
    private TextView tvHeroTanque, tvHeroLitros;
    private ProgressBar progresoHeroTanque;
    // Tubería de la válvula (tema agua): null hasta la primera lectura
    private ImageView imgTuberia;
    private Boolean tuberiaAbierta = null;

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
        tvHeroTanque = findViewById(R.id.tvHeroTanque);
        tvHeroLitros = findViewById(R.id.tvHeroLitros);
        progresoHeroTanque = findViewById(R.id.progresoHeroTanque);
        imgTuberia = findViewById(R.id.imgTuberia);

        // Tema agua: burbujas y olas en la escena, la casa flota y las lecturas suben a su lugar.
        AnimacionesAgua.burbujas(findViewById(R.id.burbujasInicio));
        AnimacionesAgua.olas(findViewById(R.id.olasInicio));
        AnimacionesAgua.flotar(findViewById(R.id.ivCasaInicio));
        AnimacionesAgua.entrada(findViewById(R.id.cardResumenGeneral), 0);
        AnimacionesAgua.subir(cardPresion, 120);
        AnimacionesAgua.subir(cardCalidad, 160);
        AnimacionesAgua.subir(cardNivel, 220);
        AnimacionesAgua.subir(cardConsumo, 260);
        AnimacionesAgua.subir(cardValvulaInicio, 320);

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

        // Saludo según la hora del teléfono, con el primer nombre de quien usa la app.
        TextView tvSaludo = findViewById(R.id.tvSaludoInicio);
        if (tvSaludo != null) {
            int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String saludo = hora < 12 ? "Buenos días" : (hora < 19 ? "Buenas tardes" : "Buenas noches");
            String primerNombre = nombre == null ? "" : nombre.trim().split("\\s+")[0];
            tvSaludo.setText(primerNombre.isEmpty() || "Usuario".equals(primerNombre) ? saludo : saludo + ", " + primerNombre);
        }

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
            } else if (id == R.id.drawer_presion) {
                startActivity(new Intent(this, PresionActivity.class));
            } else if (id == R.id.drawer_calidad) {
                startActivity(new Intent(this, CalidadActivity.class));
            } else if (id == R.id.drawer_tanque) {
                startActivity(new Intent(this, NivelTanqueActivity.class));
            } else if (id == R.id.drawer_retro) {
                startActivity(new Intent(this, RetroalimentacionActivity.class));
            } else if (id == R.id.drawer_consejos) {
                startActivity(new Intent(this, RecomendacionesActivity.class));
            } else if (id == R.id.drawer_alertas) {
                startActivity(new Intent(this, AlertasActivity.class));
            } else if (id == R.id.drawer_manual) {
                startActivity(new Intent(this, ManualUsuarioActivity.class));
            }
            // "Inicio" solo cierra el menú: ya estás aquí.
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        cardPresion.setOnClickListener(v -> startActivity(new Intent(this, PresionActivity.class)));
        cardCalidad.setOnClickListener(v -> startActivity(new Intent(this, CalidadActivity.class)));
        cardNivel.setOnClickListener(v -> startActivity(new Intent(this, NivelTanqueActivity.class)));
        cardConsumo.setOnClickListener(v -> startActivity(new Intent(this, ConsumoActivity.class)));
        View btnReportarInicio = findViewById(R.id.btnReportarInicio);
        if (btnReportarInicio != null) {
            btnReportarInicio.setOnClickListener(v -> startActivity(new Intent(this, ReportarProblemaActivity.class)));
        }
        View btnConsejosInicio = findViewById(R.id.btnConsejosInicio);
        if (btnConsejosInicio != null) {
            btnConsejosInicio.setOnClickListener(v -> startActivity(new Intent(this, RecomendacionesActivity.class)));
        }
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
                    tvNivelLitros.setText(String.format(Locale.getDefault(), "%.0f L disponibles", r.getNivel().getLitrosDisponibles()));
                    // Nivel del tanque en el héroe: la barra de agua se llena hasta el nivel real.
                    double pct = Math.max(0, Math.min(100, r.getNivel().getPorcentaje()));
                    tvHeroTanque.setText(String.format(Locale.getDefault(), "%.0f %%", pct));
                    tvHeroLitros.setText(String.format(Locale.getDefault(), "%.0f L disponibles", r.getNivel().getLitrosDisponibles()));
                    AnimacionesAgua.progreso(progresoHeroTanque, (int) Math.round(pct));
                }
                if (r.getConsumo() != null) {
                    tvConsumoLitros.setText(String.format(Locale.getDefault(), "%.1f L", r.getConsumo().getLitrosHoy()));
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

    /** Tubería de la válvula: con la válvula abierta el agua avanza; cerrada, se detiene antes de la válvula. */
    private void mostrarTuberia(boolean abierta) {
        if (imgTuberia == null || (tuberiaAbierta != null && tuberiaAbierta == abierta)) return;
        tuberiaAbierta = abierta;
        imgTuberia.setImageResource(abierta ? R.drawable.tuberia_flujo : R.drawable.tuberia_cerrada);
        if (abierta && AnimacionesAgua.activas(this) && imgTuberia.getDrawable() instanceof Animatable) {
            ((Animatable) imgTuberia.getDrawable()).start();
        }
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
                        tvValvulaEstadoInicio.setText(abierta ? "Abierta: el agua llega a tu casa" : "Cerrada: sin suministro por ahora");
                        tvValvulaEstadoInicio.setTextColor(ContextCompat.getColor(InicioActivity.this, abierta ? R.color.ok : R.color.warn));
                    }
                    mostrarTuberia(abierta);
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
        Sesion.cerrar(this);
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
