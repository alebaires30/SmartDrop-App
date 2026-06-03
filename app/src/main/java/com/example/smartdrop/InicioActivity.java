package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;

public class InicioActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private TextView tvUsuarioDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        // ── Referencias ───────────────────────────────────────
        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu        = findViewById(R.id.btnMenu);

        // ── Mostrar nombre del usuario en el header del drawer ─
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");

        // Acceder al header del NavigationView
        android.view.View header = navigationView.getHeaderView(0);
        tvUsuarioDrawer = header.findViewById(R.id.tvUsuarioDrawer);
        tvUsuarioDrawer.setText(nombre);

        btnMenu.setOnClickListener(v -> {
            android.util.Log.d("DRAWER", "Botón menú presionado");
            drawerLayout.openDrawer(GravityCompat.START);
        });

        ImageButton btnMenu2 = findViewById(R.id.btnMenu2);
        btnMenu2.setOnClickListener(v -> {
            android.util.Log.d("DRAWER", "Botón menú 2 presionado");
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // ── Abrir el drawer al presionar el botón menú ────────
        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // ── Manejar los items del menú ────────────────────────
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.drawer_inicio) {
                drawerLayout.closeDrawer(GravityCompat.START);

            } else if (id == R.id.drawer_cerrar_sesion) {
                cerrarSesion();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }



    private void cerrarSesion() {
        // Limpiar los datos de sesión guardados
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Redirigir al login y limpiar el back stack
        Intent intent = new Intent(InicioActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Cerrar drawer al presionar atrás si está abierto
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}
