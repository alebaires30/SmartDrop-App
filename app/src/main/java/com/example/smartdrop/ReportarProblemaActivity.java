package com.example.smartdrop;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

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
    private TextView tvCatalogoEstado;
    private LinearLayout layoutTipos;
    private Button btnEnviar, btnAdjuntar;
    private String tipoSeleccionado = null;
    private String tipoSeleccionadoLabel = null;
    private final List<MaterialCardView> tarjetasTipos = new ArrayList<>();
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
        tvCatalogoEstado = findViewById(R.id.tvCatalogoEstado);
        layoutTipos = findViewById(R.id.layoutTipos);
        btnEnviar = findViewById(R.id.btnEnviarReporte);
        btnAdjuntar = findViewById(R.id.btnAdjuntar);

        btnVolver.setOnClickListener(v -> finish());
        btnEnviar.setEnabled(false);
        etDescripcion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarEstadoEnvio();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
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
                if (!response.isSuccessful() || response.body() == null || !response.body().isOk()) {
                    tvCatalogoEstado.setText("No se pudieron cargar las categorías. Intenta volver a abrir el formulario.");
                    return;
                }
                maxAdjuntos = Math.max(1, response.body().getMaxAdjuntos());
                poblarTipos(response.body().getTipos());
            }

            @Override
            public void onFailure(Call<ReporteCatalogoResponse> call, Throwable t) {
                tvCatalogoEstado.setText("Sin conexión para cargar las categorías.");
                Idioma.toast(ReportarProblemaActivity.this,
                        "Sin conexión al cargar tipos", Toast.LENGTH_SHORT);
            }
        });
    }

    private void poblarTipos(List<ReporteCatalogoResponse.TipoProblema> tipos) {
        layoutTipos.removeAllViews();
        tarjetasTipos.clear();
        if (tipos == null || tipos.isEmpty()) {
            tvCatalogoEstado.setText("No hay categorías disponibles.");
            return;
        }
        tvCatalogoEstado.setVisibility(View.GONE);
        for (ReporteCatalogoResponse.TipoProblema tipo : tipos) {
            MaterialCardView tarjeta = new MaterialCardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(10);
            tarjeta.setLayoutParams(params);
            tarjeta.setRadius(dp(14));
            tarjeta.setCardElevation(dp(1));
            tarjeta.setUseCompatPadding(true);
            tarjeta.setStrokeWidth(dp(1));
            tarjeta.setClickable(true);
            tarjeta.setFocusable(true);
            tarjeta.setTag(tipo.getKey());

            LinearLayout contenido = new LinearLayout(this);
            contenido.setOrientation(LinearLayout.VERTICAL);
            contenido.setPadding(dp(14), dp(12), dp(14), dp(12));
            tarjeta.addView(contenido);

            TextView label = new TextView(this);
            label.setText(tipo.getLabel());
            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            label.setTextSize(15);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            contenido.addView(label);

            if (tipo.getDescripcion() != null && !tipo.getDescripcion().trim().isEmpty()) {
                TextView descripcion = new TextView(this);
                descripcion.setText(tipo.getDescripcion());
                descripcion.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                descripcion.setTextSize(13);
                LinearLayout.LayoutParams descripcionParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                descripcionParams.topMargin = dp(3);
                contenido.addView(descripcion, descripcionParams);
            }

            tarjeta.setOnClickListener(v -> seleccionarTipo(tipo.getKey(), tipo.getLabel()));
            tarjetasTipos.add(tarjeta);
            layoutTipos.addView(tarjeta);
        }
        actualizarEstiloTipos();
    }

    private void seleccionarTipo(String key, String label) {
        tipoSeleccionado = key;
        tipoSeleccionadoLabel = label;
        tvTipoSeleccionado.setText("Seleccionado: " + label);
        tvTipoSeleccionado.setVisibility(View.VISIBLE);
        etDescripcion.setHint("otro".equals(key)
                ? "Describe el otro problema con detalle (mínimo 10 caracteres)…"
                : "Describe el problema con detalle (mínimo 10 caracteres)…");
        actualizarEstiloTipos();
        actualizarEstadoEnvio();
    }

    private void actualizarEstiloTipos() {
        if (layoutTipos == null) return;
        for (MaterialCardView tarjeta : tarjetasTipos) {
            LinearLayout contenido = (LinearLayout) tarjeta.getChildAt(0);
            TextView label = (TextView) contenido.getChildAt(0);
            boolean seleccionada = tipoSeleccionado != null
                    && tipoSeleccionado.equals(tarjeta.getTag());
            tarjeta.setSelected(seleccionada);
            tarjeta.setCardBackgroundColor(ContextCompat.getColor(this,
                    seleccionada ? R.color.bg_lavender : R.color.bg_card));
            tarjeta.setStrokeColor(ContextCompat.getColor(this,
                    seleccionada ? R.color.brand_accent : R.color.stroke_soft));
            label.setTextColor(ContextCompat.getColor(this,
                    seleccionada ? R.color.brand_on_card : R.color.text_primary));
            label.setText(tipoLabelDe(tarjeta));
        }
    }

    private String tipoLabelDe(MaterialCardView tarjeta) {
        LinearLayout contenido = (LinearLayout) tarjeta.getChildAt(0);
        TextView label = (TextView) contenido.getChildAt(0);
        CharSequence texto = label.getText();
        return texto.toString();
    }

    private void actualizarEstadoEnvio() {
        if (btnEnviar == null || etDescripcion == null) return;
        btnEnviar.setEnabled(tipoSeleccionado != null
                && etDescripcion.getText().toString().trim().length() >= 10);
    }

    private void actualizarListaAdjuntos() {
        if (adjuntos.isEmpty()) {
            tvAdjuntosSeleccionados.setText("Sin archivos seleccionados");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Uri uri : adjuntos) {
            sb.append(nombreArchivo(uri)).append('\n');
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
            Idioma.toast(this, "Selecciona el tipo y describe el problema (mín. 10 caracteres)",
                    Toast.LENGTH_LONG);
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
                    Idioma.toast(ReportarProblemaActivity.this,
                            "Reporte enviado. El equipo lo revisará pronto.", Toast.LENGTH_LONG);
                    finish();
                } else {
                    String error = response.body() != null ? response.body().getError() : null;
                    Idioma.toast(ReportarProblemaActivity.this,
                            "No se pudo enviar: " + (error != null ? error : "error " + response.code()),
                            Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(Call<ReporteCrearResponse> call, Throwable t) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText(R.string.enviar_reporte);
                Idioma.toast(ReportarProblemaActivity.this,
                        "Sin conexión. Inténtalo de nuevo.", Toast.LENGTH_LONG);
            }
        });
    }
}
