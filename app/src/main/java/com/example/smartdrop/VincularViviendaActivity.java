package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VincularViviendaActivity extends AppCompatActivity {

    EditText numeroCuenta, nombreTitular;
    Button btnVincular;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vincular_vivienda);

        numeroCuenta  = findViewById(R.id.numeroCuenta);
        nombreTitular = findViewById(R.id.nombreTitular);
        btnVincular   = findViewById(R.id.btnVincular);

        btnVincular.setOnClickListener(v -> intentarVincular());
    }

    private void intentarVincular() {
        String cuentaVal = numeroCuenta.getText().toString().trim();
        String nombreVal = nombreTitular.getText().toString().trim();

        if (cuentaVal.isEmpty() || nombreVal.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        VincularViviendaRequest request = new VincularViviendaRequest(cuentaVal, nombreVal);

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.vincularVivienda(request).enqueue(new Callback<VincularViviendaResponse>() {
            @Override
            public void onResponse(Call<VincularViviendaResponse> call, Response<VincularViviendaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(VincularViviendaActivity.this,
                            " " + response.body().getMensaje(), Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(VincularViviendaActivity.this, InicioActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                } else {
                    String mensajeError = "No se pudo vincular la vivienda.";
                    if (response.errorBody() != null) {
                        try {
                            VincularViviendaResponse errorResp = new Gson().fromJson(
                                    response.errorBody().charStream(), VincularViviendaResponse.class);
                            if (errorResp != null && errorResp.getError() != null) {
                                mensajeError = errorResp.getError();
                            }
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(VincularViviendaActivity.this, mensajeError, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<VincularViviendaResponse> call, Throwable t) {
                Toast.makeText(VincularViviendaActivity.this,
                        "No se pudo conectar al servidor: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}