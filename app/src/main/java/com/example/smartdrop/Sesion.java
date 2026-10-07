package com.example.smartdrop;

import android.content.Context;
import android.content.SharedPreferences;

/** Datos de la sesión guardados en el teléfono. */
public final class Sesion {

    private Sesion() { }

    /** Cierra la sesión del teléfono conservando el tema (la web lo recuerda igual). */
    public static void limpiar(Context context) {
        SharedPreferences sesion = context.getApplicationContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        boolean oscuro = sesion.getBoolean("modo_oscuro", false);
        sesion.edit().clear().putBoolean("modo_oscuro", oscuro).apply();
    }
}
