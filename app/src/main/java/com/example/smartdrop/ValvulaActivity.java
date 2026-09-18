package com.example.smartdrop;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
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
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ValvulaActivity extends AppCompatActivity {

    private TextView tvEstadoDetalle, tvTiempoRestante, tvMensajeSoloAdmin;
    private LinearLayout layoutControlesAdmin;
    private Button btnAbrir, btnCerrar, btn10s, btn30s, btn60s, btn120s, btnPersonalizado, btnCancelar;
    private RecyclerView rvLogs;
    private ValvulaLogsAdapter adapter;
    private CountDownTimer countDownTimer;
    private static int ID_VALVULA = 1; // Por defecto 1, se intentará detectar
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoRefresh;
    private long ultimoUpdateMillis = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_valvula_logs);

        // Intentar obtener ID de válvula dinámico si se pasa por intent
        ID_VALVULA = getIntent().getIntExtra("id_valvula", 1);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarValvula);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Válvula Principal");
        }

        // Inicializar Vistas
        tvEstadoDetalle = findViewById(R.id.tvValvulaEstadoDetalle);
        tvTiempoRestante = findViewById(R.id.tvTiempoRestanteAdmin);
        tvMensajeSoloAdmin = findViewById(R.id.tvMensajeSoloAdmin);
        layoutControlesAdmin = findViewById(R.id.layoutControlesAdmin);

        btnAbrir = findViewById(R.id.btnAbrirValvula);
        btnCerrar = findViewById(R.id.btnCerrarValvula);
        btn10s = findViewById(R.id.btn10s);
        btn30s = findViewById(R.id.btn30s);
        btn60s = findViewById(R.id.btn60s);
        btn120s = findViewById(R.id.btn120s);
        btnPersonalizado = findViewById(R.id.btnPersonalizado);
        btnCancelar = findViewById(R.id.btnCancelarTemporizador);
        rvLogs = findViewById(R.id.rvLogsValvula);

        // ESCENARIO 3 (PB050): Restricción de Usuario Normal
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        int idRol = prefs.getInt("id_rol", 1); // 2 = Admin según MainActivity.java
        
        if (idRol != 2) {
            if (layoutControlesAdmin != null) layoutControlesAdmin.setVisibility(View.GONE);
            if (tvMensajeSoloAdmin != null) tvMensajeSoloAdmin.setVisibility(View.VISIBLE);
        }

        // Configurar RecyclerView
        adapter = new ValvulaLogsAdapter();
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setAdapter(adapter);

        // Listeners Admin
        btnAbrir.setOnClickListener(v -> enviarComandoControl("ABRIR", 0));
        btnCerrar.setOnClickListener(v -> enviarComandoControl("CERRAR", 0));
        btn10s.setOnClickListener(v -> enviarComandoControl("ABRIR", 10));
        btn30s.setOnClickListener(v -> enviarComandoControl("ABRIR", 30));
        btn60s.setOnClickListener(v -> enviarComandoControl("ABRIR", 60));
        btn120s.setOnClickListener(v -> enviarComandoControl("ABRIR", 120));
        btnPersonalizado.setOnClickListener(v -> mostrarDialogoPersonalizado());
        btnCancelar.setOnClickListener(v -> cancelarTemporizador());

        // Carga Inicial
        Toast.makeText(this, "Conectado a Válvula ID: " + ID_VALVULA, Toast.LENGTH_SHORT).show();
        cargarEstado();
        cargarHistorialValvula();

        // Refresco automático cada 10s (PB050 Escenario 1)
        autoRefresh = new Runnable() {
            @Override
            public void run() {
                cargarEstado();
                cargarHistorialValvula();
                actualizarTextoUpdate();
                handler.postDelayed(this, 10000);
            }
        };
    }

    private void actualizarTextoUpdate() {
        if (ultimoUpdateMillis == 0) return;
        long diff = (System.currentTimeMillis() - ultimoUpdateMillis) / 1000;
        TextView tvUpdate = findViewById(R.id.tvValvulaActualizacionInicio); // Reutilizando id o buscando uno adecuado
        if (tvUpdate != null) {
            tvUpdate.setText("Última actualización: hace " + diff + " segundos");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(autoRefresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(autoRefresh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        
        searchView.setQueryHint("Ctrl+F: Buscar en todo...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
        return true;
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
        api.obtenerEstadoValvula(ID_VALVULA).enqueue(new Callback<ValvulaEstadoResponse>() {
            @Override
            public void onResponse(Call<ValvulaEstadoResponse> call, Response<ValvulaEstadoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ultimoUpdateMillis = System.currentTimeMillis();
                    String estado = response.body().getEstadoActual();
                    boolean abierta = "abierta".equalsIgnoreCase(estado);
                    tvEstadoDetalle.setText(abierta ? "🟢 Válvula: ABIERTA" : "🔴 Válvula: CERRADA");
                    tvEstadoDetalle.setTextColor(abierta ? Color.parseColor("#27AE60") : Color.parseColor("#E74C3C"));
                }
            }
            @Override
            public void onFailure(Call<ValvulaEstadoResponse> call, Throwable t) {}
        });
    }

    private void enviarComandoControl(String accion, int duracion) {
        // Escenario 4 PB053: Seguridad
        if (duracion > 120) {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Límite de Seguridad")
                .setMessage("El tiempo máximo permitido es 120 seg. ¿Deseas ajustar a 2 minutos?")
                .setPositiveButton("Sí, ajustar", (d, w) -> ejecutarPeticion("ABRIR", 120))
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
        api.controlarValvula(ID_VALVULA, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ValvulaActivity.this, "Comando " + accion + " ejecutado con éxito", Toast.LENGTH_SHORT).show();
                    if (accion.equals("ABRIR") && duracion > 0) {
                        iniciarContador(duracion);
                    } else if (accion.equals("CERRAR")) {
                        if (countDownTimer != null) countDownTimer.cancel();
                        tvTiempoRestante.setText("0s");
                    }
                    cargarEstado();
                    cargarHistorialValvula();
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
        api.obtenerLogsValvula(ID_VALVULA).enqueue(new Callback<ValvulaLogsResponse>() {
            @Override
            public void onResponse(Call<ValvulaLogsResponse> call, Response<ValvulaLogsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setLogs(response.body().getLogs());
                }
            }
            @Override
            public void onFailure(Call<ValvulaLogsResponse> call, Throwable t) {}
        });
    }

    private void iniciarContador(int segundos) {
        if (countDownTimer != null) countDownTimer.cancel();
        btnCancelar.setVisibility(View.VISIBLE);
        countDownTimer = new CountDownTimer(segundos * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTiempoRestante.setText((millisUntilFinished / 1000) + "s");
            }
            @Override
            public void onFinish() {
                tvTiempoRestante.setText("0s");
                btnCancelar.setVisibility(View.GONE);
                enviarComandoControl("CERRAR", 0);
            }
        }.start();
    }

    private void cancelarTemporizador() {
        enviarComandoControl("CERRAR", 0);
    }

    private void mostrarDialogoPersonalizado() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Ej: 45");
        new AlertDialog.Builder(this)
                .setTitle("Tiempo Personalizado")
                .setMessage("Ingresa duración (5-120 seg):")
                .setView(input)
                .setPositiveButton("Ejecutar", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        int seg = Integer.parseInt(val);
                        if (seg >= 5) enviarComandoControl("ABRIR", seg);
                        else Toast.makeText(this, "Mínimo 5s", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
