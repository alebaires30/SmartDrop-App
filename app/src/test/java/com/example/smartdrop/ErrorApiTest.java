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
}
