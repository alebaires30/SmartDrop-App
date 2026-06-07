package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NivelTanqueActivity extends AppCompatActivity {

    private boolean litros = true;

    TextView tvConversion,
            tvCapacidad,
            tvDisponible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nivel_tanque);

        tvConversion = findViewById(R.id.tvConversion);
        tvCapacidad = findViewById(R.id.tvCapacidad);
        tvDisponible = findViewById(R.id.tvDisponible);

        ImageButton btnVolver = findViewById(R.id.btnVolver);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_tanque);
        btnVolver.setOnClickListener(v -> finish());

        tvConversion.setOnClickListener(v -> {

            litros = !litros;

            if(litros){

                tvConversion.setText("Cambiar conversión: Litros (L)");

                tvCapacidad.setText(
                        "Capacidad del tanque: 600L");

                tvDisponible.setText(
                        "540 litros disponibles (90%)");

            }else{

                tvConversion.setText(
                        "Cambiar conversión: Barriles");

                tvCapacidad.setText(
                        "Capacidad del tanque: 3.77 barriles");

                tvDisponible.setText(
                        "3.4 barriles disponibles (90%)");
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_tanque) {
                return true;
            }

            if (id == R.id.nav_calidad) {
                startActivity(new Intent(this, CalidadActivity.class));
                finish();
                return true;
            }

            if (id == R.id.nav_presion) {
                startActivity(new Intent(this, PresionActivity.class));
                finish();
                return true;
            }

            if (id == R.id.nav_consumo) {
                //startActivity(new Intent(this, ConsumoActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }
}