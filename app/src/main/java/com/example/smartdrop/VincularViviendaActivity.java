package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VincularViviendaActivity extends BaseActivity {

    private EditText etNumeroCuenta, etNombreTitular;
    private Button btnVincular;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vincular_vivienda);

        etNumeroCuenta  = findViewById(R.id.etNumeroCuenta);
        etNombreTitular = findViewById(R.id.etNombreTitular);
        btnVincular     = findViewById(R.id.btnVincular);

        btnVincular.setOnClickListener(v -> intentarVincular());
    }

    private void intentarVincular() {
        String cuentaVal = etNumeroCuenta.getText().toString().trim();
        String nombreVal = etNombreTitular.getText().toString().trim();

        if (cuentaVal.isEmpty() || nombreVal.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnVincular.setEnabled(false);
        VincularViviendaRequest request = new VincularViviendaRequest(cuentaVal, nombreVal);
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.vincularVivienda(request).enqueue(new Callback<VincularViviendaResponse>() {
            @Override
            public void onResponse(Call<VincularViviendaResponse> call, Response<VincularViviendaResponse> response) {
                btnVincular.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(VincularViviendaActivity.this,
                            response.body().getMensaje() != null
                                    ? response.body().getMensaje()
                                    : "Vivienda vinculada exitosamente.",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(VincularViviendaActivity.this, InicioActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String mensajeError = "No se pudo vincular la vivienda.";
                    try {
                        if (response.errorBody() != null) {
                            VincularViviendaResponse errorResp = new Gson().fromJson(
                                    response.errorBody().charStream(), VincularViviendaResponse.class);
                            if (errorResp != null && errorResp.getError() != null) {
                                mensajeError = errorResp.getError();
                            }
                        }
                    } catch (Exception ignored) { }
                    Toast.makeText(VincularViviendaActivity.this,
                            mensajeError, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<VincularViviendaResponse> call, Throwable t) {
                btnVincular.setEnabled(true);
                Toast.makeText(VincularViviendaActivity.this,
                        "Sin conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
