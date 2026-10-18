package com.example.smartdrop;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NivelTanqueActivity extends BaseActivity {

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

        configurarBottomNav(R.id.nav_tanque);

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

        String textoPrincipal;

        if (mostrandoLitros) {
            tvConversion.setText("Ver en barriles");
            tvCapacidad.setText(String.format(java.util.Locale.getDefault(), "Capacidad del tanque: %.0f L", ultimoNivel.getCapacidadMaximaLitros()));
            textoPrincipal = String.format(java.util.Locale.getDefault(), "%.0f litros disponibles (%.0f%%)", ultimoNivel.getLitrosDisponibles(), ultimoNivel.getPorcentaje());
        } else {
            tvConversion.setText("Ver en litros");
            tvCapacidad.setText(String.format(java.util.Locale.getDefault(), "Capacidad del tanque: %.2f barriles", ultimoNivel.getCapacidadMaximaBarriles()));
            textoPrincipal = String.format(java.util.Locale.getDefault(), "%.2f barriles disponibles (%.0f%%)", ultimoNivel.getBarrilesDisponibles(), ultimoNivel.getPorcentaje());
        }

        // Escenario 3: si no está en verde, se agrega el mensaje de precaución
        if (!"verde".equals(ultimoNivel.getColor())) {
            textoPrincipal += "\n" + ultimoNivel.getMensaje();
        }

        tvDisponible.setText(textoPrincipal);
        // La columna de agua sube hasta el nivel real. El agua ocupa de y=20 a y=180 en un lienzo de
        // 200 (columna_agua_nivel.xml): nivel del recorte = (20 + 160 × fracción) / 200 × 10000.
        android.widget.ImageView agua = findViewById(R.id.ivColumnaAgua);
        android.view.View columna = findViewById(R.id.columnaTanque);
        if (agua != null) {
            double pct = Math.max(0, Math.min(100, ultimoNivel.getPorcentaje()));
            AnimacionesAgua.llenar(agua, (int) Math.round((20 + 160 * pct / 100.0) / 200.0 * 10000));
            if (columna != null) {
                columna.setContentDescription(String.format(java.util.Locale.getDefault(), "Tanque de tu casa al %.0f %%", pct));
            }
        }
        tvDisponible.setTextColor(ColorSeveridad.colorDe(this, ultimoNivel.getColor()));

        tvUltima.setText("Última lectura: " + Fechas.formatear(ultimoNivel.getFecha(), "hh:mm a", "--:--"));
    }
}
