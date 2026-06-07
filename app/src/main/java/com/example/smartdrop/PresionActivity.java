package com.example.smartdrop;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PresionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presion);

        ImageButton btnVolver = findViewById(R.id.btnVolver);

        btnVolver.setOnClickListener(v -> finish());

        BottomNavigationView bottomNav =
                findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_presion);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_tanque) {
                startActivity(new Intent(this,
                        NivelTanqueActivity.class));
                finish();
                return true;
            }

            if (id == R.id.nav_calidad) {
                startActivity(new Intent(this,
                        CalidadActivity.class));
                finish();
                return true;
            }

            if (id == R.id.nav_presion) {
                startActivity(new Intent(this, PresionActivity.class));
                finish();
                return true;
            }

            if (id == R.id.nav_consumo) {
                return true;
            }

            return false;
        });
    }
}