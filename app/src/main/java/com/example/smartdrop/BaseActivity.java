package com.example.smartdrop;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;

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
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        boolean modoOscuro = prefs.getBoolean("modo_oscuro", false);
        if (modoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
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

            // Si la pantalla tiene barra de navegación inferior, el padding de
            // la barra de gestos lo recibe la propia barra (para quedar pegada
            // justo arriba de los botones del teléfono), no el contenedor.
            BottomNavigationView bottomNav = buscarBottomNav(v);
            if (bottomNav != null) {
                v.setPadding(
                        paddingIzq + barras.left,
                        paddingTop + barras.top,
                        paddingDer + barras.right,
                        paddingBot);
                bottomNav.setPadding(
                        bottomNav.getPaddingLeft(),
                        bottomNav.getPaddingTop(),
                        bottomNav.getPaddingRight(),
                        Math.max(barras.bottom, ime.bottom));
            } else if (!(v instanceof ScrollView)
                    && !(v instanceof NestedScrollView)
                    && !(v instanceof RecyclerView)) {
                v.setPadding(
                        paddingIzq + barras.left,
                        paddingTop + barras.top,
                        paddingDer + barras.right,
                        paddingBot + Math.max(barras.bottom, ime.bottom));
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(contenido);
    }

    /** Barra inferior compartida por las pantallas de tanque, calidad, presión y consumo. */
    protected void configurarBottomNav(int itemActual) {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        nav.setSelectedItemId(itemActual);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == itemActual) return true;
            if (id == R.id.nav_tanque) {
                startActivity(new Intent(this, NivelTanqueActivity.class));
            } else if (id == R.id.nav_calidad) {
                startActivity(new Intent(this, CalidadActivity.class));
            } else if (id == R.id.nav_presion) {
                startActivity(new Intent(this, PresionActivity.class));
            } else if (id == R.id.nav_consumo) {
                startActivity(new Intent(this, ConsumoActivity.class));
            }
            finish();
            return true;
        });
    }

    protected int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private BottomNavigationView buscarBottomNav(View vista) {
        if (vista instanceof BottomNavigationView) return (BottomNavigationView) vista;
        if (!(vista instanceof ViewGroup)) return null;
        ViewGroup grupo = (ViewGroup) vista;
        for (int i = 0; i < grupo.getChildCount(); i++) {
            BottomNavigationView resultado = buscarBottomNav(grupo.getChildAt(i));
            if (resultado != null) return resultado;
        }
        return null;
    }
}
