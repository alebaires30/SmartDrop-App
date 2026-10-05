package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class PerfilActivity extends BaseActivity {

    private TextView tvNombre, tvEmail, tvRol;
    private SwitchCompat switchModoOscuro;
    private Button btnCerrarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        tvNombre = findViewById(R.id.tvPerfilNombre);
        tvEmail = findViewById(R.id.tvPerfilEmail);
        tvRol = findViewById(R.id.tvPerfilRol);
        switchModoOscuro = findViewById(R.id.switchModoOscuroPerfil);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesionPerfil);

        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        String email = prefs.getString("email", "usuario@smartdrop.com");
        String nombreRol = prefs.getString("nombre_rol", "Usuario Estándar");

        tvNombre.setText(nombre);
        tvEmail.setText(email);
        tvRol.setText(nombreRol);

        boolean modoOscuro = prefs.getBoolean("modo_oscuro", false);
        switchModoOscuro.setChecked(modoOscuro);

        switchModoOscuro.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("modo_oscuro", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        if (btnCerrarSesion != null) {
            btnCerrarSesion.setOnClickListener(v -> {
                prefs.edit().clear().apply();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
