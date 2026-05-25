package com.example.smartdrop;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText nombre, password, confirmPassword;
    Button btnCrear;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nombre = findViewById(R.id.nombre);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnCrear = findViewById(R.id.btnCrear);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnCrear.setOnClickListener(v -> {

            String user = nombre.getText().toString().trim();
            String pass = password.getText().toString().trim();
            String confirm = confirmPassword.getText().toString().trim();

            if(user.isEmpty() || pass.isEmpty() || confirm.isEmpty()){
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!pass.equals(confirm)){
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("usuarios", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("usuario", user);
            editor.putString("password", pass);
            editor.apply();

            Toast.makeText(this, "Cuenta creada correctamente 🎉", Toast.LENGTH_SHORT).show();

            finish(); 
        });
    }
}