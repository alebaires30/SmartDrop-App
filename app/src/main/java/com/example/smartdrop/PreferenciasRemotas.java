package com.example.smartdrop;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Aplica en el teléfono el tema y el idioma guardados en el servidor (pudieron cambiarse desde la web). */
final class PreferenciasRemotas {

    private PreferenciasRemotas() { }

    static void sincronizar(Activity activity) {
        ApiClient.getClientAutenticado(activity).create(ApiService.class).obtenerPreferencias()
                .enqueue(new Callback<Perfil.PreferenciasResponse>() {
                    @Override
                    public void onResponse(Call<Perfil.PreferenciasResponse> call, Response<Perfil.PreferenciasResponse> response) {
                        Perfil.PreferenciasResponse cuerpo = response.body();
                        if (!response.isSuccessful() || cuerpo == null || cuerpo.preferencias == null
                                || activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        SharedPreferences sesion = activity.getSharedPreferences("sesion", Activity.MODE_PRIVATE);
                        boolean oscuro = Boolean.TRUE.equals(cuerpo.preferencias.modoOscuro);
                        if (sesion.getBoolean("modo_oscuro", false) != oscuro) {
                            sesion.edit().putBoolean("modo_oscuro", oscuro).apply();
                            AppCompatDelegate.setDefaultNightMode(
                                    oscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                        }
                    }

                    @Override
                    public void onFailure(Call<Perfil.PreferenciasResponse> call, Throwable t) {
                        // Sin conexión: se mantienen las preferencias guardadas en el teléfono.
                    }
                });
    }
}
