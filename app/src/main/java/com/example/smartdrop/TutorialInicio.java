package com.example.smartdrop;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

/**
 * Tutorial de 5 pasos para usuarios nuevos (PB045), igual al de la web.
 * Se abre solo la primera vez que el usuario llega a Inicio y se puede volver a ver desde el menú
 * (Cuenta → Ver tutorial). Solo recuerda en el teléfono que ya se vio; no toca sensores ni datos.
 */
final class TutorialInicio {

    private static final String PREFS = "tutorial";
    private static final String CLAVE_VISTO = "visto_inicio";

    private static final String[][] PASOS = {
            {"Cómo ver el nivel del tanque",
                    "Bienvenido a SmartDrop. En Inicio ves el nivel de tu tanque, la presión y la calidad del agua. "
                            + "Toca cualquier tarjeta para ver más detalle."},
            {"Cómo consultar tu consumo",
                    "En Consumo ves cuántos litros usas por día, semana o mes. En Retroalimentación y Consejos de "
                            + "ahorro te decimos cómo vas y qué puedes mejorar."},
            {"Cómo entender las alertas",
                    "La campana te avisa si hay una fuga, si el agua no está bien o si viene un corte. Revísala "
                            + "cuando tenga un número."},
            {"Cómo reportar problemas",
                    "Si no te llega agua o ves una fuga, entra a Reportar un problema. Puedes adjuntar fotos y "
                            + "hablar con el equipo por el chat del reporte."},
            {"Dónde encontrar el manual completo",
                    "El Manual de usuario está en el menú, en Cuenta. Tiene buscador y lo puedes descargar en PDF "
                            + "para leerlo sin internet."},
    };

    private TutorialInicio() {
    }

    /** Lo muestra solo si en este teléfono todavía no se ha visto. */
    static void mostrarSiEsNuevo(Activity actividad) {
        SharedPreferences prefs = actividad.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        if (!prefs.getBoolean(CLAVE_VISTO, false)) {
            mostrar(actividad);
        }
    }

    static void mostrar(Activity actividad) {
        if (actividad.isFinishing() || actividad.isDestroyed()) return;
        final int[] paso = {0};
        AlertDialog dialogo = new AlertDialog.Builder(actividad)
                .setTitle(PASOS[0][0])
                .setMessage(PASOS[0][1])
                .setPositiveButton("Siguiente", null)
                .setNegativeButton("Saltar", null)
                .setNeutralButton("Atrás", null)
                .create();
        // Al cerrarlo de cualquier forma (Listo, Saltar o tocando afuera) ya cuenta como visto.
        dialogo.setOnDismissListener(d -> marcarVisto(actividad));
        dialogo.setOnShowListener(d -> {
            Button siguiente = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
            Button saltar = dialogo.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button atras = dialogo.getButton(AlertDialog.BUTTON_NEUTRAL);
            Runnable pintar = () -> {
                boolean ultimo = paso[0] == PASOS.length - 1;
                dialogo.setTitle("Paso " + (paso[0] + 1) + " de " + PASOS.length + ": " + PASOS[paso[0]][0]);
                dialogo.setMessage(PASOS[paso[0]][1]);
                siguiente.setText(ultimo ? "Listo" : "Siguiente");
                atras.setVisibility(paso[0] == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
                saltar.setVisibility(ultimo ? android.view.View.GONE : android.view.View.VISIBLE);
            };
            siguiente.setOnClickListener(v -> {
                if (paso[0] < PASOS.length - 1) {
                    paso[0]++;
                    pintar.run();
                } else {
                    dialogo.dismiss();
                }
            });
            atras.setOnClickListener(v -> {
                if (paso[0] > 0) {
                    paso[0]--;
                    pintar.run();
                }
            });
            saltar.setOnClickListener(v -> dialogo.dismiss());
            pintar.run();
        });
        dialogo.show();
    }

    private static void marcarVisto(Activity actividad) {
        actividad.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().putBoolean(CLAVE_VISTO, true).apply();
    }
}
