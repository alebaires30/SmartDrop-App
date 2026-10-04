package com.example.smartdrop;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Activity base que aplica los insets del sistema (barra de estado y barra de
 * navegación/gestos) como padding al layout raíz, para que el contenido no se
 * dibuje debajo de ellas (edge-to-edge de Android 15+).
 *
 * Uso: hacer que cada Activity extienda BaseActivity en vez de AppCompatActivity.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        aplicarInsets();
    }

    private void aplicarInsets() {
        View raiz = findViewById(android.R.id.content);
        if (!(raiz instanceof ViewGroup)) return;
        View contenido = ((ViewGroup) raiz).getChildAt(0);
        if (contenido == null) return;

        final int paddingIzq = contenido.getPaddingLeft();
        final int paddingTop = contenido.getPaddingTop();
        final int paddingDer = contenido.getPaddingRight();
        final int paddingBot = contenido.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(contenido, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Solo aplicar padding manual si el contenido NO es scrollable.
            // ScrollView/NestedScrollView manejan sus propios insets.
            if (!(v instanceof android.widget.ScrollView)
                    && !(v instanceof androidx.core.widget.NestedScrollView)
                    && !(v instanceof androidx.recyclerview.widget.RecyclerView)) {
                v.setPadding(
                        paddingIzq + barras.left,
                        paddingTop + barras.top,
                        paddingDer + barras.right,
                        paddingBot + Math.max(barras.bottom, ime.bottom));
            }

            // NO consumir: permite que vistas hijas (teclado, scroll) reaccionen.
            return insets;
        });

        ViewCompat.requestApplyInsets(contenido);
    }
}
