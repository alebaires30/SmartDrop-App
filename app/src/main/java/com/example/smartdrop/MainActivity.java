package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    EditText usuario, password;
    Button btnLogin, btnRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usuario     = findViewById(R.id.usuario);
        password    = findViewById(R.id.password);
        btnLogin    = findViewById(R.id.btnLogin);
        btnRegistro = findViewById(R.id.btnRegistro);

        btnRegistro.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> intentarLogin());
    }

    private void intentarLogin() {

        String correoVal = usuario.getText().toString().trim();
        String passVal   = password.getText().toString().trim();

        // Escenario 2: campos vacíos
        if (correoVal.isEmpty() || passVal.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construir el objeto que se envía al backend
        LoginRequest request = new LoginRequest(correoVal, passVal);

        // Llamar a la API
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<LoginResponse> call = api.loginUsuario(request);

        call.enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    LoginResponse body = response.body();

                    // Guardar token y datos de sesión en SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("access_token", body.getAccess());
                    editor.putString("nombre", body.getNombre());
                    editor.putInt("id_rol", body.getIdRol());
                    editor.putInt("id_usuario", body.getIdUsuario());
                    editor.apply();

                    Toast.makeText(MainActivity.this,
                            "¡Bienvenido " + body.getNombre() + "!",
                            Toast.LENGTH_SHORT).show();

                    // Escenario 1: redirigir según rol
                    // id_rol=1 usuario común, id_rol=2 administrador
                    Intent intent;
                    if (body.getIdRol() == 2) {
                        // redirige al dashboard de admin (por ahora va al mismo)
                        intent = new Intent(MainActivity.this, InicioActivity.class);
                    } else {
                        intent = new Intent(MainActivity.this, InicioActivity.class);
                    }
                    startActivity(intent);
                    finish();

                } else if (response.code() == 400) {
                    // Escenario 2 y 3: credenciales incorrectas o usuario no encontrado
                    Toast.makeText(MainActivity.this,
                            "Correo o contraseña incorrectos.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Error inesperado. Intenta de nuevo.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "No se pudo conectar al servidor: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}