package com.example.smartdrop;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NivelTanqueActivity extends AppCompatActivity {

    private ImageButton btnVolver;
    private TextView tvConversion, tvCapacidad, tvDisponible, tvBomba, tvUltima, tvAutonomia;

    private boolean mostrandoLitros = true;
    private NivelData ultimoNivel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long INTERVALO_POLLING_MS = 10000;
    private Runnable tareaPolling;

    private boolean cargaEnProgreso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nivel_tanque);

        btnVolver    = findViewById(R.id.btnVolver);
        tvConversion = findViewById(R.id.tvConversion);
        tvCapacidad  = findViewById(R.id.tvCapacidad);
        tvDisponible = findViewById(R.id.tvDisponible);
        tvBomba      = findViewById(R.id.tvBomba);
        tvUltima     = findViewById(R.id.tvUltima);
        tvAutonomia  = findViewById(R.id.tvAutonomia);

        btnVolver.setOnClickListener(v -> finish());

        // Escenario: tocar el texto de conversión alterna Litros <-> Barriles
        tvConversion.setOnClickListener(v -> {
            mostrandoLitros = !mostrandoLitros;
            pintarNivel();
        });

        tareaPolling = () -> {
            cargarNivel();
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

    private void cargarNivel() {
        if (cargaEnProgreso) return;
        cargaEnProgreso = true;

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerEstadoAgua().enqueue(new Callback<EstadoAguaResponse>() {
            @Override
            public void onResponse(Call<EstadoAguaResponse> call, Response<EstadoAguaResponse> response) {
                cargaEnProgreso = false;
                if (!response.isSuccessful() || response.body() == null || response.body().getNivel() == null) return;
                ultimoNivel = response.body().getNivel();
                pintarNivel();
            }

            @Override
            public void onFailure(Call<EstadoAguaResponse> call, Throwable t) {
                cargaEnProgreso = false;
            }
        });
    }

    private void pintarNivel() {
        if (ultimoNivel == null) return;

        String icono = ColorSeveridad.iconoDe(ultimoNivel.getColor());
        String textoPrincipal;

        if (mostrandoLitros) {
            tvConversion.setText("Cambiar conversión: Litros (L)");
            tvCapacidad.setText(String.format(java.util.Locale.getDefault(), "Capacidad de tanque: %.0fL", ultimoNivel.getCapacidadMaximaLitros()));
            textoPrincipal = String.format(java.util.Locale.getDefault(), "%.0f litros disponibles (%.0f%%)", ultimoNivel.getLitrosDisponibles(), ultimoNivel.getPorcentaje());
        } else {
            tvConversion.setText("Cambiar conversión: Barriles");
            tvCapacidad.setText(String.format(java.util.Locale.getDefault(), "Capacidad de tanque: %.2f barriles", ultimoNivel.getCapacidadMaximaBarriles()));
            textoPrincipal = String.format(java.util.Locale.getDefault(), "%.2f barriles disponibles (%.0f%%)", ultimoNivel.getBarrilesDisponibles(), ultimoNivel.getPorcentaje());
        }

        // Escenario 3: si no está en verde, se agrega el mensaje de precaución
        if (!"verde".equals(ultimoNivel.getColor())) {
            textoPrincipal += "\n" + ultimoNivel.getMensaje();
        }

        tvDisponible.setText(icono + " " + textoPrincipal);
        tvDisponible.setTextColor(ColorSeveridad.colorDe(ultimoNivel.getColor()));

        tvUltima.setText("Última lectura: " + formatearHora(ultimoNivel.getFecha()));
    }

    private String formatearHora(String fechaIso) {
        try {
            String limpio = fechaIso.length() > 19 ? fechaIso.substring(0, 19) : fechaIso;
            java.text.SimpleDateFormat entrada = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            entrada.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.text.SimpleDateFormat salida = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
            return salida.format(entrada.parse(limpio));
        } catch (Exception e) {
            return "--:--";
        }
    }
}