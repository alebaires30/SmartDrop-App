package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText usuario, password;
    Button btnLogin, btnRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usuario = findViewById(R.id.usuario);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegistro = findViewById(R.id.btnRegistro);

        SharedPreferences prefs = getSharedPreferences("usuarios", MODE_PRIVATE);


        btnRegistro.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });


        btnLogin.setOnClickListener(v -> {

            String savedUser = prefs.getString("usuario", "");
            String savedPass = prefs.getString("password", "");

            String userInput = usuario.getText().toString().trim();
            String passInput = password.getText().toString().trim();


            Toast.makeText(this,
                    "Guardado: " + savedUser + " / " + savedPass +
                            "\nIngresado: " + userInput + " / " + passInput,
                    Toast.LENGTH_LONG).show();

            if(userInput.equalsIgnoreCase(savedUser) && passInput.equals(savedPass)){

                Toast.makeText(this, "Login correcto 🎉", Toast.LENGTH_SHORT).show();

                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                finish();

            } else {
                Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}