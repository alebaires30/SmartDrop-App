package com.example.smartdrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ErrorApiTest {

    @Test
    public void usaElMensajeDelServidorCuandoViene() {
        assertEquals("Comando inválido. Usa ABRIR o CERRAR.",
                ErrorApi.mensaje(400, "{\"error\": \"Comando inválido. Usa ABRIR o CERRAR.\"}"));
        assertEquals("Token invalido o expirado.", ErrorApi.mensaje(401, "{\"detail\": \"Token invalido o expirado.\"}"));
    }

    @Test
    public void elLimiteDePeticionesMuestraCuantoEsperar() {
        String cuerpo = "{\"error\": \"Demasiadas solicitudes. Espera 42 segundos e inténtalo de nuevo.\", \"detail\": \"x\"}";
        assertTrue(ErrorApi.mensaje(ErrorApi.DEMASIADAS_SOLICITUDES, cuerpo).contains("42 segundos"));
        assertTrue(ErrorApi.mensaje(429, null).startsWith("Demasiadas solicitudes"));
    }

    @Test
    public void siElCuerpoNoEsJsonUsaUnMensajeGenerico() {
        assertNull(ErrorApi.mensajeDelCuerpo("<html><body>502 Bad Gateway</body></html>"));
        assertTrue(ErrorApi.mensaje(502, "<html>").startsWith("El servidor no pudo"));
        assertEquals("No tienes permiso para hacer esto.", ErrorApi.mensaje(403, ""));
        assertEquals("Ese dato ya está registrado.", ErrorApi.mensaje(409, "[]"));
    }

    @Test
    public void nuncaMuestraElCuerpoCrudo() {
        // Un texto que no es un objeto JSON, o un "error" que es una traza larga, no se muestra.
        assertNull(ErrorApi.mensajeDelCuerpo("Internal Server Error"));
        assertNull(ErrorApi.mensajeDelCuerpo("\"texto suelto\""));
        StringBuilder traza = new StringBuilder("{\"error\": \"");
        for (int i = 0; i < 100; i++) traza.append("Traceback line ").append(i).append(' ');
        traza.append("\"}");
        assertNull(ErrorApi.mensajeDelCuerpo(traza.toString()));
        assertTrue(ErrorApi.mensaje(500, traza.toString()).startsWith("El servidor no pudo"));
        assertNull(ErrorApi.mensajeDelCuerpo("{\"error\": {\"campo\": [\"x\"]}}"));
    }

    @Test
    public void conTextoPorDefectoSoloLoCambiaSiElServidorExplicaElMotivo() {
        assertEquals("Correo o contraseña incorrectos.",
                ErrorApi.mensajeOPorDefecto("<html>400</html>", "Correo o contraseña incorrectos."));
        assertEquals("La contraseña es muy débil.",
                ErrorApi.mensajeOPorDefecto("{\"error\": \"La contraseña es muy débil.\"}", "x"));
        assertNull(ErrorApi.mensajeOPorDefecto(null, null));
    }
}
