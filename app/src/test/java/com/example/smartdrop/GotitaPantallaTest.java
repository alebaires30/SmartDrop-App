package com.example.smartdrop;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Qué pantallas y qué preguntas mandan a Gotita lo que se ve (texto, partes y captura). */
public class GotitaPantallaTest {

    @Test
    public void soloSeMandaSiLaPreguntaEsSobreLaPantalla() {
        assertTrue(Gotita.mandarPantalla("tanque", false, "¿Qué estoy viendo?", false));
        assertTrue(Gotita.mandarPantalla("graficas", false, "¿Qué muestran estas gráficas?", false));
        assertTrue(Gotita.mandarPantalla("presion", false, "Explain this screen", false));
        // El botón "Explícame esta pantalla".
        assertTrue(Gotita.mandarPantalla("consumo", false, "lo que sea", true));
        assertFalse(Gotita.mandarPantalla("tanque", false, "¿Cuánta agua me queda?", false));
        assertFalse(Gotita.mandarPantalla("inicio", false, "¿Cómo está mi agua hoy?", false));
    }

    @Test
    public void nuncaDesdeLaCuentaElLoginNiElRegistro() {
        for (String id : new String[]{"perfil", "configuracion", "main", "register"}) {
            assertFalse(id, Gotita.mandarPantalla(id, false, "Explícame esta pantalla", true));
        }
    }

    @Test
    public void nuncaDesdeLasPantallasDelAdminConDatosDeOtrasPersonas() {
        for (String id : new String[]{"panel", "reportes_admin", "prediccion_fugas", "prediccion_suministro",
                "zonapredicciondetalle", "comunidad", "reportedetalle", "valvulas", "buscar"}) {
            assertFalse(id, Gotita.mandarPantalla(id, true, "¿Qué muestra esta pantalla?", true));
        }
        // Al usuario normal la válvula no le muestra quién la abrió; las gráficas del admin son promedios.
        assertTrue(Gotita.mandarPantalla("valvulas", false, "¿Qué muestra esta pantalla?", false));
        assertTrue(Gotita.mandarPantalla("graficas", true, "¿Qué muestran estas gráficas?", false));
    }
}
