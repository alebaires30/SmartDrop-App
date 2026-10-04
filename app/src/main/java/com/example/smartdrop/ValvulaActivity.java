package com.example.smartdrop;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ValvulaActivity extends BaseActivity {

    private TextView tvEstadoDetalle, tvTiempoRestante, tvMensajeSoloAdmin, tvAdvertenciaLogs, tvLogsVacio;
    private LinearLayout layoutTemporizadorActivo;
    private CardView cardControlesAdmin, cardLogsAdmin;
    private Button btnAbrir, btnCerrar, btn10s, btn30s, btn60s, btn120s, btnPersonalizado, btnCancelar;
    private RecyclerView rvLogs;
    private ValvulaLogsAdapter adapter;
    private CountDownTimer countDownTimer;

    private static int idValvula = 1;
    private int idRol = 1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoRefreshRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_valvula_logs);

        // ID de válvula pasado por Intent
        idValvula = getIntent().getIntExtra("id_valvula", 1);

        // Verificación de Rol (2 = Admin, 1 = Usuario Común)
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        idRol = prefs.getInt("id_rol", 1);

        // Configuración de Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarValvula);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Control de Electroválvula");
        }

        // Vistas
        tvEstadoDetalle = findViewById(R.id.tvValvulaEstadoDetalle);
        tvTiempoRestante = findViewById(R.id.tvTiempoRestanteAdmin);
        tvMensajeSoloAdmin = findViewById(R.id.tvMensajeSoloAdmin);
        tvAdvertenciaLogs = findViewById(R.id.tvAdvertenciaLogs);
        tvLogsVacio = findViewById(R.id.tvLogsVacio);
        layoutTemporizadorActivo = findViewById(R.id.layoutTemporizadorActivo);
        cardControlesAdmin = findViewById(R.id.cardControlesAdmin);
        cardLogsAdmin = findViewById(R.id.cardLogsAdmin);

        btnAbrir = findViewById(R.id.btnAbrirValvula);
        btnCerrar = findViewById(R.id.btnCerrarValvula);
        btn10s = findViewById(R.id.btn10s);
        btn30s = findViewById(R.id.btn30s);
        btn60s = findViewById(R.id.btn60s);
        btn120s = findViewById(R.id.btn120s);
        btnPersonalizado = findViewById(R.id.btnPersonalizado);
        btnCancelar = findViewById(R.id.btnCancelarTemporizador);
        rvLogs = findViewById(R.id.rvLogsValvula);

        // Configurar visibilidad según el Rol
        if (idRol == 2) {
            if (cardControlesAdmin != null) cardControlesAdmin.setVisibility(View.VISIBLE);
            if (cardLogsAdmin != null) cardLogsAdmin.setVisibility(View.VISIBLE);
            if (tvMensajeSoloAdmin != null) tvMensajeSoloAdmin.setVisibility(View.GONE);

            // Action Listeners de Administrador
            btnAbrir.setOnClickListener(v -> enviarComandoControl("ABRIR", 0));
            btnCerrar.setOnClickListener(v -> enviarComandoControl("CERRAR", 0));
            btn10s.setOnClickListener(v -> enviarComandoControl("ABRIR", 10));
            btn30s.setOnClickListener(v -> enviarComandoControl("ABRIR", 30));
            btn60s.setOnClickListener(v -> enviarComandoControl("ABRIR", 60));
            btn120s.setOnClickListener(v -> enviarComandoControl("ABRIR", 120));
            btnPersonalizado.setOnClickListener(v -> mostrarDialogoPersonalizado());

            // Configuración del RecyclerView
            adapter = new ValvulaLogsAdapter();
            rvLogs.setLayoutManager(new LinearLayoutManager(this));
            rvLogs.setAdapter(adapter);
        } else {
            if (cardControlesAdmin != null) cardControlesAdmin.setVisibility(View.GONE);
            if (cardLogsAdmin != null) cardLogsAdmin.setVisibility(View.GONE);
            if (tvMensajeSoloAdmin != null) tvMensajeSoloAdmin.setVisibility(View.VISIBLE);
        }

        // Listener común para cancelar temporizador
        btnCancelar.setOnClickListener(v -> enviarComandoControl("CERRAR", 0));

        // Carga inicial
        cargarEstado();
        if (idRol == 2) {
            cargarHistorialValvula();
        }

        // Refresco automático cada 10 segundos
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                cargarEstado();
                if (idRol == 2) {
                    cargarHistorialValvula();
                }
                handler.postDelayed(this, 10000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(autoRefreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cargarEstado() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoValvula(idValvula).enqueue(new Callback<ValvulaEstadoResponse>() {
            @Override
            public void onResponse(Call<ValvulaEstadoResponse> call, Response<ValvulaEstadoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ValvulaEstadoResponse estadoData = response.body();
                    String estado = estadoData.getEstadoActual();
                    boolean abierta = "abierta".equalsIgnoreCase(estado);

                    tvEstadoDetalle.setText(abierta ? "🟢 Válvula: ABIERTA" : "🔴 Válvula: CERRADA");
                    tvEstadoDetalle.setTextColor(abierta ? Color.parseColor("#27AE60") : Color.parseColor("#E74C3C"));

                    TemporizadorActivo temp = estadoData.getTemporizadorActivo();
                    if (abierta && temp != null && temp.getSegundosRestantes() > 0) {
                        actualizarTemporizadorUI(temp.getSegundosRestantes());
                    } else {
                        ocultarTemporizadorUI();
                    }
                }
            }

            @Override
            public void onFailure(Call<ValvulaEstadoResponse> call, Throwable t) {
                // Silencioso en polling
            }
        });
    }

    private void actualizarTemporizadorUI(int segundosRestantes) {
        if (layoutTemporizadorActivo != null) {
            layoutTemporizadorActivo.setVisibility(View.VISIBLE);
        }
        if (btnCancelar != null) {
            btnCancelar.setVisibility(View.VISIBLE);
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(segundosRestantes * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long segs = millisUntilFinished / 1000;
                if (tvTiempoRestante != null) {
                    tvTiempoRestante.setText(segs + "s");
                }
            }

            @Override
            public void onFinish() {
                if (tvTiempoRestante != null) {
                    tvTiempoRestante.setText("0s");
                }
                ocultarTemporizadorUI();
                cargarEstado();
            }
        }.start();
    }

    private void ocultarTemporizadorUI() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (tvTiempoRestante != null) {
            tvTiempoRestante.setText("0s");
        }
        if (btnCancelar != null) {
            btnCancelar.setVisibility(View.GONE);
        }
        if (layoutTemporizadorActivo != null) {
            layoutTemporizadorActivo.setVisibility(View.GONE);
        }
    }

    private void enviarComandoControl(String accion, int duracion) {
        if (duracion > 120) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Límite de Seguridad")
                    .setMessage("El tiempo máximo permitido es 120 segundos. ¿Deseas ajustar a 2 minutos?")
                    .setPositiveButton("Sí, ajustar", (dialog, which) -> ejecutarPeticion(accion, 120))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        ejecutarPeticion(accion, duracion);
    }

    private void ejecutarPeticion(String accion, int duracion) {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombreUsuario = prefs.getString("nombre", "Admin");
        String origenStr = duracion > 0 ? "App (Temporizado)" : "App (Manual)";

        ValvulaControlRequest request = new ValvulaControlRequest(accion, duracion, nombreUsuario, origenStr);

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.controlarValvula(idValvula, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ValvulaActivity.this, "Comando " + accion + " ejecutado con éxito", Toast.LENGTH_SHORT).show();
                    if ("ABRIR".equalsIgnoreCase(accion) && duracion > 0) {
                        actualizarTemporizadorUI(duracion);
                    } else if ("CERRAR".equalsIgnoreCase(accion)) {
                        ocultarTemporizadorUI();
                    }
                    cargarEstado();
                    if (idRol == 2) {
                        cargarHistorialValvula();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Toast.makeText(ValvulaActivity.this, "Error Servidor: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ValvulaActivity.this, "Error de servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ValvulaActivity.this, "Fallo de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cargarHistorialValvula() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerLogsValvula(idValvula).enqueue(new Callback<ValvulaLogsResponse>() {
            @Override
            public void onResponse(Call<ValvulaLogsResponse> call, Response<ValvulaLogsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ValvulaLogsResponse body = response.body();

                    // Advertencia opcional
                    if (tvAdvertenciaLogs != null) {
                        if (body.getAdvertencia() != null && !body.getAdvertencia().trim().isEmpty()) {
                            tvAdvertenciaLogs.setText(body.getAdvertencia());
                            tvAdvertenciaLogs.setVisibility(View.VISIBLE);
                        } else {
                            tvAdvertenciaLogs.setVisibility(View.GONE);
                        }
                    }

                    // Manejo de lista vacía o nula
                    List<ValvulaLogs> logs = body.getLogs();
                    if (logs == null || logs.isEmpty()) {
                        if (tvLogsVacio != null) tvLogsVacio.setVisibility(View.VISIBLE);
                        if (rvLogs != null) rvLogs.setVisibility(View.GONE);
                        if (adapter != null) adapter.setLogs(null);
                    } else {
                        if (tvLogsVacio != null) tvLogsVacio.setVisibility(View.GONE);
                        if (rvLogs != null) rvLogs.setVisibility(View.VISIBLE);
                        if (adapter != null) adapter.setLogs(logs);
                    }
                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "Error " + response.code();
                        Toast.makeText(ValvulaActivity.this, "Error al obtener historial: " + err, Toast.LENGTH_SHORT).show();
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFailure(Call<ValvulaLogsResponse> call, Throwable t) {
                // Silencioso en polling
            }
        });
    }

    private void mostrarDialogoPersonalizado() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Ej: 45");

        new AlertDialog.Builder(this)
                .setTitle("Tiempo Personalizado")
                .setMessage("Ingresa la duración en segundos (5-120 seg):")
                .setView(input)
                .setPositiveButton("Ejecutar", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try {
                            int seg = Integer.parseInt(val);
                            if (seg >= 5) {
                                enviarComandoControl("ABRIR", seg);
                            } else {
                                Toast.makeText(this, "El tiempo mínimo es 5s", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Valor no válido", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
