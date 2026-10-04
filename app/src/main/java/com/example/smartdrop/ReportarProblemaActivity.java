package com.example.smartdrop;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

public class ReportarProblemaActivity extends BaseActivity {

    private EditText etDescripcion, etUbicacion, etZona;
    private TextView tvTipoSeleccionado, tvAdjuntosSeleccionados;
    private LinearLayout layoutTipos;
    private Button btnEnviar, btnAdjuntar;
    private String tipoSeleccionado = null;
    private int maxAdjuntos = 5;
    private final List<Uri> adjuntos = new ArrayList<>();

    private final ActivityResultLauncher<String[]> selectorArchivos =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                adjuntos.clear();
                if (uris != null) {
                    for (Uri uri : uris) {
                        if (adjuntos.size() < maxAdjuntos) {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            adjuntos.add(uri);
                        }
                    }
                }
                actualizarListaAdjuntos();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportar_problema);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        etDescripcion = findViewById(R.id.etDescripcion);
        etUbicacion = findViewById(R.id.etUbicacion);
        etZona = findViewById(R.id.etZona);
        tvTipoSeleccionado = findViewById(R.id.tvTipoSeleccionado);
        tvAdjuntosSeleccionados = findViewById(R.id.tvAdjuntosSeleccionados);
        layoutTipos = findViewById(R.id.layoutTipos);
        btnEnviar = findViewById(R.id.btnEnviarReporte);
        btnAdjuntar = findViewById(R.id.btnAdjuntar);

        btnVolver.setOnClickListener(v -> finish());
        btnEnviar.setEnabled(false);
        btnEnviar.setOnClickListener(v -> enviarReporte());
        btnAdjuntar.setOnClickListener(v ->
                selectorArchivos.launch(new String[]{"image/*", "video/*"}));

        cargarCatalogo();
    }

    private void cargarCatalogo() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerCatalogoReportes().enqueue(new Callback<ReporteCatalogoResponse>() {
            @Override
            public void onResponse(Call<ReporteCatalogoResponse> call, Response<ReporteCatalogoResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) return;
                maxAdjuntos = Math.max(1, response.body().getMaxAdjuntos());
                poblarTipos(response.body().getTipos());
            }

            @Override
            public void onFailure(Call<ReporteCatalogoResponse> call, Throwable t) {
                Toast.makeText(ReportarProblemaActivity.this,
                        "Sin conexión al cargar tipos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void poblarTipos(List<ReporteCatalogoResponse.TipoProblema> tipos) {
        if (tipos == null) return;
        for (ReporteCatalogoResponse.TipoProblema tipo : tipos) {
            TextView chip = new TextView(this);
            chip.setText(tipo.getLabel());
            chip.setPadding(28, 16, 28, 16);
            chip.setTextColor(0xFF3D3D3D);
            chip.setBackgroundResource(R.drawable.fondo_redondeado);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 12, 12);
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> seleccionarTipo(tipo.getKey(), tipo.getLabel()));
            layoutTipos.addView(chip);
        }
    }

    private void seleccionarTipo(String key, String label) {
        tipoSeleccionado = key;
        tvTipoSeleccionado.setText("Seleccionado: " + label);
        btnEnviar.setEnabled(true);
    }

    private void actualizarListaAdjuntos() {
        if (adjuntos.isEmpty()) {
            tvAdjuntosSeleccionados.setText("Sin archivos seleccionados");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Uri uri : adjuntos) {
            sb.append("📎 ").append(nombreArchivo(uri)).append('\n');
        }
        tvAdjuntosSeleccionados.setText(sb.toString().trim());
    }

    private String nombreArchivo(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        }
        return uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "adjunto";
    }

    private File copiarACache(Uri uri) {
        try {
            String nombre = nombreArchivo(uri);
            File destino = new File(getCacheDir(), System.currentTimeMillis() + "_" + nombre);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(destino)) {
                byte[] buffer = new byte[8192];
                int leido;
                while ((leido = in.read(buffer)) != -1) out.write(buffer, 0, leido);
            }
            return destino;
        } catch (Exception e) {
            return null;
        }
    }

    private void enviarReporte() {
        String descripcion = etDescripcion.getText().toString().trim();
        if (tipoSeleccionado == null || descripcion.length() < 10) {
            Toast.makeText(this, "Selecciona el tipo y describe el problema (mín. 10 caracteres)",
                    Toast.LENGTH_LONG).show();
            return;
        }
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando…");

        Map<String, RequestBody> campos = new HashMap<>();
        campos.put("tipo_problema", RequestBody.create(tipoSeleccionado, MediaType.parse("text/plain")));
        campos.put("descripcion", RequestBody.create(descripcion, MediaType.parse("text/plain")));
        String ubicacion = etUbicacion.getText().toString().trim();
        String zona = etZona.getText().toString().trim();
        if (!ubicacion.isEmpty()) campos.put("ubicacion", RequestBody.create(ubicacion, MediaType.parse("text/plain")));
        if (!zona.isEmpty()) campos.put("zona", RequestBody.create(zona, MediaType.parse("text/plain")));

        List<MultipartBody.Part> partes = new ArrayList<>();
        for (Uri uri : adjuntos) {
            File archivo = copiarACache(uri);
            if (archivo == null) continue;
            String mime = getContentResolver().getType(uri);
            if (mime == null) mime = "application/octet-stream";
            RequestBody cuerpo = RequestBody.create(archivo, MediaType.parse(mime));
            partes.add(MultipartBody.Part.createFormData("adjuntos", archivo.getName(), cuerpo));
        }

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.crearReporte(campos, partes).enqueue(new Callback<ReporteCrearResponse>() {
            @Override
            public void onResponse(Call<ReporteCrearResponse> call, Response<ReporteCrearResponse> response) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText(R.string.enviar_reporte);
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    Toast.makeText(ReportarProblemaActivity.this,
                            "Reporte enviado. El equipo lo revisará pronto.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String error = response.body() != null ? response.body().getError() : null;
                    Toast.makeText(ReportarProblemaActivity.this,
                            "No se pudo enviar: " + (error != null ? error : "error " + response.code()),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ReporteCrearResponse> call, Throwable t) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText(R.string.enviar_reporte);
                Toast.makeText(ReportarProblemaActivity.this,
                        "Sin conexión. Inténtalo de nuevo.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
