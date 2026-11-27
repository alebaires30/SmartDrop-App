package com.example.smartdrop;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Manuales dentro de la app. Muestra la misma página del manual que la web, así el contenido se actualiza en un
 * solo lugar:
 * <ul>
 *   <li>Manual de usuario (/manual/?app=1): no tiene datos de nadie, se abre sin sesión.</li>
 *   <li>Manual del administrador (EXTRA_ADMIN): se pide con el token de la app, porque el servidor solo se lo
 *   muestra a administradores.</li>
 * </ul>
 */
public class ManualUsuarioActivity extends BaseActivity {

    static final String EXTRA_ADMIN = "manual_admin";
    /** Texto para abrir el manual ya buscando (viene del buscador de la app). */
    static final String EXTRA_BUSQUEDA = "manual_busqueda";

    private WebView web;
    private View cargando;
    private View error;
    private String direccion;
    private boolean esAdmin;
    private final Handler principal = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_usuario);

        esAdmin = getIntent().getBooleanExtra(EXTRA_ADMIN, false);
        web = findViewById(R.id.webManual);
        cargando = findViewById(R.id.cargandoManual);
        error = findViewById(R.id.errorManual);
        TextView titulo = findViewById(R.id.tituloManual);
        if (esAdmin) titulo.setText("Manual del administrador");
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnReintentarManual).setOnClickListener(v -> cargar());

        boolean oscuro = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        direccion = ApiClient.getBaseUrl() + "manual/?app=1" + (esAdmin ? "&tipo=admin" : "")
                + (oscuro ? "&tema=oscuro" : "");
        String busqueda = getIntent().getStringExtra(EXTRA_BUSQUEDA);
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            direccion += "&q=" + Uri.encode(busqueda.trim());
        }

        // El manual es solo texto: sin JavaScript ni acceso a archivos del teléfono.
        WebSettings ajustes = web.getSettings();
        ajustes.setJavaScriptEnabled(false);
        ajustes.setAllowFileAccess(false);
        ajustes.setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView vista, WebResourceRequest peticion) {
                Uri destino = peticion.getUrl();
                Uri propio = Uri.parse(direccion);
                boolean mismoServidor = destino.getHost() != null && destino.getHost().equals(propio.getHost());
                String ruta = destino.getPath() != null ? destino.getPath() : "";
                if (mismoServidor && ruta.endsWith(".pdf")) {
                    descargarPdf(destino); // "Descargar manual para uso sin internet"
                    return true;
                }
                if (mismoServidor && destino.getFragment() != null) {
                    return false; // Los saltos del índice se quedan dentro del manual.
                }
                if (mismoServidor && ruta.startsWith("/manual")) {
                    // El buscador del manual: se queda en esta pantalla.
                    if (!esAdmin) return false;
                    direccion = destino.toString(); // el del administrador se vuelve a pedir con el token
                    cargar();
                    return true;
                }
                startActivity(new Intent(Intent.ACTION_VIEW, destino)); // Otros enlaces: en el navegador.
                return true;
            }

            @Override
            public void onPageFinished(WebView vista, String url) {
                cargando.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView vista, WebResourceRequest peticion, WebResourceError fallo) {
                if (peticion.isForMainFrame()) mostrarError();
            }
        });
        web.setDownloadListener((url, agente, disposicion, tipo, largo) -> descargarPdf(Uri.parse(url)));
        cargar();
    }

    /** Guarda el PDF del manual en Descargas para leerlo sin internet. */
    private void descargarPdf(Uri archivo) {
        try {
            DownloadManager.Request pedido = new DownloadManager.Request(archivo)
                    .setTitle("Manual de SmartDrop")
                    .setDescription("Manual de usuario para leer sin internet")
                    .setMimeType("application/pdf")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            "SmartDrop-manual-de-usuario.pdf");
            DownloadManager gestor = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            gestor.enqueue(pedido);
            Toast.makeText(this, "Descargando el manual en la carpeta Descargas…", Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            // Teléfonos viejos sin permiso para escribir en Descargas: lo baja el navegador.
            startActivity(new Intent(Intent.ACTION_VIEW, archivo));
        }
    }

    private void cargar() {
        error.setVisibility(View.GONE);
        cargando.setVisibility(View.VISIBLE);
        web.setVisibility(View.VISIBLE);
        if (!esAdmin) {
            web.loadUrl(direccion);
            return;
        }
        // El del administrador se pide con el token (el cliente lo renueva solo si venció).
        Request peticion = new Request.Builder().url(direccion).get().build();
        ApiClient.getHttpAutenticado(this).newCall(peticion).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                principal.post(ManualUsuarioActivity.this::mostrarError);
            }

            @Override
            public void onResponse(Call call, Response respuesta) {
                String html = null;
                try (ResponseBody cuerpo = respuesta.body()) {
                    if (respuesta.isSuccessful() && cuerpo != null) html = cuerpo.string();
                } catch (IOException ignored) {
                    html = null;
                }
                String pagina = html;
                principal.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pagina == null) {
                        mostrarError();
                    } else {
                        web.loadDataWithBaseURL(direccion, pagina, "text/html", "utf-8", null);
                    }
                });
            }
        });
    }

    private void mostrarError() {
        cargando.setVisibility(View.GONE);
        web.setVisibility(View.INVISIBLE);
        error.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
