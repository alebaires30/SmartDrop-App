package com.example.smartdrop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReporteDetalleActivity extends BaseActivity {

    private static final long INTERVALO_CHAT_MS = 3000;
    private boolean errorMostrado = false;

    private int idReporte;
    private ChatAdapter chatAdapter;
    private RecyclerView recyclerChat;
    private LinearLayout layoutAdjuntos;
    private TextView tvTipo, tvDescripcion, tvEstado, tvUbicacion, tvRespuestaAdmin;
    private View cardRespuesta;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tareaChat;

    private final ActivityResultLauncher<String[]> selectorEvidencia =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) return;
                subirEvidencia(new ArrayList<>(uris));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte_detalle);

        idReporte = getIntent().getIntExtra("id_reporte", 0);
        if (idReporte <= 0) {
            finish();
            return;
        }

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        tvTipo = findViewById(R.id.tvDetalleTipo);
        tvDescripcion = findViewById(R.id.tvDetalleDescripcion);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvUbicacion = findViewById(R.id.tvDetalleUbicacion);
        tvRespuestaAdmin = findViewById(R.id.tvRespuestaAdmin);
        cardRespuesta = findViewById(R.id.cardRespuesta);
        layoutAdjuntos = findViewById(R.id.layoutAdjuntos);
        recyclerChat = findViewById(R.id.recyclerChat);
        EditText etMensaje = findViewById(R.id.etMensaje);
        View btnEnviarMensaje = findViewById(R.id.btnEnviarMensaje);
        View btnAdjuntarEvidencia = findViewById(R.id.btnAdjuntarEvidencia);

        btnVolver.setOnClickListener(v -> finish());
        btnAdjuntarEvidencia.setOnClickListener(v ->
                selectorEvidencia.launch(new String[]{"image/*", "video/*"}));

        chatAdapter = new ChatAdapter();
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);

        btnEnviarMensaje.setOnClickListener(v -> {
            String texto = etMensaje.getText().toString().trim();
            if (texto.isEmpty()) return;
            etMensaje.setText("");
            enviarMensaje(texto);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tareaChat = new Runnable() {
            @Override
            public void run() {
                cargarDetalle();
                handler.postDelayed(this, INTERVALO_CHAT_MS);
            }
        };
        handler.post(tareaChat);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tareaChat != null) handler.removeCallbacks(tareaChat);
    }

    private void cargarDetalle() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerReporteDetalle(idReporte).enqueue(new Callback<ReporteDetalleResponse>() {
            @Override
            public void onResponse(Call<ReporteDetalleResponse> call, Response<ReporteDetalleResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) {
                    avisarErrorUnaVez("No se pudo cargar el reporte (" + response.code() + ")");
                    return;
                }
                errorMostrado = false;
                Reporte reporte = response.body().getReporte();
                if (reporte != null) pintarReporte(reporte);
                int antes = chatAdapter.getItemCount();
                chatAdapter.setMensajes(response.body().getMensajes());
                if (chatAdapter.getItemCount() != antes) {
                    recyclerChat.scrollToPosition(Math.max(0, chatAdapter.getItemCount() - 1));
                }
            }

            @Override
            public void onFailure(Call<ReporteDetalleResponse> call, Throwable t) {
                avisarErrorUnaVez("Error al cargar el reporte: " + t.getMessage());
            }
        });
    }

    private void avisarErrorUnaVez(String mensaje) {
        if (errorMostrado) return;
        errorMostrado = true;
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    private void pintarReporte(Reporte reporte) {
        tvTipo.setText(reporte.getTipoLabel());
        tvDescripcion.setText(reporte.getDescripcion());
        tvEstado.setText(reporte.getEstadoLabel());
        tvUbicacion.setText(reporte.getUbicacion() != null && !reporte.getUbicacion().isEmpty()
                ? "📍 " + reporte.getUbicacion() : "");
        tvUbicacion.setVisibility(reporte.getUbicacion() != null && !reporte.getUbicacion().isEmpty()
                ? View.VISIBLE : View.GONE);

        if (reporte.getRespuestaAdmin() != null && !reporte.getRespuestaAdmin().isEmpty()) {
            cardRespuesta.setVisibility(View.VISIBLE);
            tvRespuestaAdmin.setText(reporte.getRespuestaAdmin());
        } else {
            cardRespuesta.setVisibility(View.GONE);
        }

        layoutAdjuntos.removeAllViews();
        if (reporte.getAdjuntos() != null) {
            for (Adjunto adjunto : reporte.getAdjuntos()) {
                if (adjunto.esVideo()) {
                    TextView enlace = new TextView(this);
                    enlace.setText("🎬 " + (adjunto.getNombre() != null ? adjunto.getNombre() : "Video"));
                    enlace.setTextColor(ContextCompat.getColor(this, R.color.brand_on_card));
                    enlace.setPadding(0, 12, 0, 12);
                    String url = urlAbsoluta(adjunto.getUrl());
                    enlace.setOnClickListener(v ->
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
                    layoutAdjuntos.addView(enlace);
                } else {
                    ImageView imagen = new ImageView(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 420);
                    params.setMargins(0, 8, 0, 8);
                    imagen.setLayoutParams(params);
                    imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(this).load(urlAbsoluta(adjunto.getUrl())).into(imagen);
                    layoutAdjuntos.addView(imagen);
                }
            }
        }
    }

    private String urlAbsoluta(String url) {
        if (url == null) return "";
        if (url.startsWith("http")) return url;
        String base = BuildConfig.DEBUG
                ? "http://" + BuildConfig.API_HOST + ":8000"
                : "https://" + BuildConfig.API_HOST;
        return base + url;
    }

    private void enviarMensaje(String texto) {
        Map<String, String> body = new HashMap<>();
        body.put("mensaje", texto);
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.enviarMensajeReporte(idReporte, body).enqueue(new Callback<ReporteMensajeResponse>() {
            @Override
            public void onResponse(Call<ReporteMensajeResponse> call, Response<ReporteMensajeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    cargarDetalle();
                } else {
                    Toast.makeText(ReporteDetalleActivity.this,
                            "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ReporteMensajeResponse> call, Throwable t) {
                Toast.makeText(ReporteDetalleActivity.this,
                        "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void subirEvidencia(List<Uri> uris) {
        List<MultipartBody.Part> partes = new ArrayList<>();
        for (Uri uri : uris) {
            try {
                String nombre = "evidencia_" + System.currentTimeMillis();
                File destino = new File(getCacheDir(), nombre);
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(destino)) {
                    byte[] buffer = new byte[8192];
                    int leido;
                    while ((leido = in.read(buffer)) != -1) out.write(buffer, 0, leido);
                }
                String mime = getContentResolver().getType(uri);
                if (mime == null) mime = "application/octet-stream";
                partes.add(MultipartBody.Part.createFormData(
                        "adjuntos", nombre, RequestBody.create(destino, MediaType.parse(mime))));
            } catch (Exception ignored) { }
        }
        if (partes.isEmpty()) return;

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.adjuntarEvidencia(idReporte, partes).enqueue(new Callback<ReporteCrearResponse>() {
            @Override
            public void onResponse(Call<ReporteCrearResponse> call, Response<ReporteCrearResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    Toast.makeText(ReporteDetalleActivity.this,
                            "Evidencia agregada", Toast.LENGTH_SHORT).show();
                    cargarDetalle();
                }
            }

            @Override
            public void onFailure(Call<ReporteCrearResponse> call, Throwable t) {
                Toast.makeText(ReporteDetalleActivity.this,
                        "Sin conexión al subir evidencia", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
