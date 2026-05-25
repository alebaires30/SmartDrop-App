package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    TextView txtBienvenida;
    Button btnLogout, btnComenzar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        txtBienvenida = findViewById(R.id.txtBienvenida);
        btnLogout = findViewById(R.id.btnLogout);
        btnComenzar = findViewById(R.id.btnComenzar);

        SharedPreferences prefs = getSharedPreferences("usuarios", MODE_PRIVATE);
        String usuario = prefs.getString("usuario", "Usuario");

        txtBienvenida.setText("¡Bienvenido/a " + usuario + "! 💜");

        btnComenzar.setOnClickListener(v -> {
            Toast.makeText(this, "¡Vamos a comenzar! ", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();

            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        });
    }
}