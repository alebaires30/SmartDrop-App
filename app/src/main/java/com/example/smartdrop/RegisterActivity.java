package com.example.smartdrop;


import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    //Referencias de los campos del fomulario
    EditText nombre, apellido, correo, password, confirmPassword;
    Button btnCrear;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nombre = findViewById(R.id.nombre);
        apellido = findViewById(R.id.apellido);
        correo = findViewById(R.id.correo);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnCrear = findViewById(R.id.btnCrear);

        btnCrear.setOnClickListener(v -> intentarRegistro());

    }
    private void intentarRegistro(){
        String nombreVal = nombre.getText().toString().trim();
        String apellidoVal = apellido.getText().toString().trim();
        String correoVal = correo.getText().toString().trim();
        String passVal = password.getText().toString().trim();
        String confirmVal = confirmPassword.getText().toString().trim();

        if (nombreVal.isEmpty() || apellidoVal.isEmpty() || confirmVal.isEmpty() || passVal.isEmpty() || confirmVal.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!passVal.equals(confirmVal)){
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }


        RegisterRequest request = new RegisterRequest(
                nombreVal,
                apellidoVal,
                correoVal,
                passVal
        );

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<RegisterResponse> call = api.registrarUsuario(request);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, "Registro exitoso " + response.body().getMensaje(), Toast.LENGTH_LONG).show();
                    finish();
                } else if (response.code() == 400) {
                    Toast.makeText(RegisterActivity.this, "El correo ya está registrado.", Toast.LENGTH_LONG).show();

                }else {
                    Toast.makeText(RegisterActivity.this, "Error inesperado. Intenta de nuevo.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {

                Toast.makeText(RegisterActivity.this, "No se pudo conectar al servidor: " + t.getMessage(), Toast.LENGTH_LONG).show();

            }
        });
    }
}