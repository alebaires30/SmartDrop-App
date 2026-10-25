package com.example.smartdrop;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Manual de usuario. Muestra la misma página del manual que la web (/manual/?app=1), así el contenido se
 * actualiza en un solo lugar. La página no tiene datos de nadie, por eso no hace falta mandar la sesión.
 */
public class ManualUsuarioActivity extends BaseActivity {

    private WebView web;
    private View cargando;
    private View error;
    private String direccion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_usuario);

        web = findViewById(R.id.webManual);
        cargando = findViewById(R.id.cargandoManual);
        error = findViewById(R.id.errorManual);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnReintentarManual).setOnClickListener(v -> cargar());

        boolean oscuro = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        direccion = ApiClient.getBaseUrl() + "manual/?app=1" + (oscuro ? "&tema=oscuro" : "");

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
                if (destino.getHost() != null && destino.getHost().equals(propio.getHost())) {
                    return false; // Los saltos dentro del manual se quedan aquí.
                }
                startActivity(new Intent(Intent.ACTION_VIEW, destino)); // Enlaces de afuera: en el navegador.
                return true;
            }

            @Override
            public void onPageFinished(WebView vista, String url) {
                cargando.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView vista, WebResourceRequest peticion, WebResourceError fallo) {
                if (peticion.isForMainFrame()) {
                    cargando.setVisibility(View.GONE);
                    web.setVisibility(View.INVISIBLE);
                    error.setVisibility(View.VISIBLE);
                }
            }
        });
        cargar();
    }

    private void cargar() {
        error.setVisibility(View.GONE);
        cargando.setVisibility(View.VISIBLE);
        web.setVisibility(View.VISIBLE);
        web.loadUrl(direccion);
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
