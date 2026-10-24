package com.example.smartdrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

/** Verifica que la app lee y envía exactamente lo que espera el backend (App/api/views_valves.py). */
public class ContratoValvulaTest {

    private final Gson gson = new Gson();

    @Test
    public void leeLaCuentaRegresivaDelEstado() {
        // Tal cual responde GET api/valvula/<id>/estado/.
        String json = "{\"id_valvula\":3,\"nombre\":\"Válvula principal\",\"estado_actual\":\"abierta\","
                + "\"ultima_apertura\":\"2026-10-24T10:00:00+00:00\",\"abierta_desde\":\"2026-10-24T10:00:00+00:00\","
                + "\"temporizador_activo\":{\"duracion_programada\":30,\"segundos_restantes\":12,"
                + "\"fin\":\"2026-10-24T10:00:30+00:00\"}}";
        ValvulaEstadoResponse estado = gson.fromJson(json, ValvulaEstadoResponse.class);
        assertEquals("abierta", estado.getEstadoActual());
        assertEquals(12, estado.getTemporizadorActivo().getSegundosRestantes());
        assertEquals(30, estado.getTemporizadorActivo().getDuracionProgramada());
        assertEquals("2026-10-24T10:00:00+00:00", estado.getAbiertaDesde());
        // "Abierta desde las hh:mm:ss" sale de esta hora (10:00 UTC).
        assertEquals(Fechas.parse("2026-10-24T10:00:00Z"), Fechas.parse(estado.getAbiertaDesde()));
    }

    @Test
    public void sinTemporizadorElCampoLlegaNulo() {
        ValvulaEstadoResponse estado = gson.fromJson(
                "{\"estado_actual\": \"cerrada\", \"temporizador_activo\": null, \"abierta_desde\": null}",
                ValvulaEstadoResponse.class);
        assertNull(estado.getTemporizadorActivo());
        assertNull(estado.getAbiertaDesde());
    }

    @Test
    public void elComandoSoloMandaAccionYDuracion() {
        JsonObject cuerpo = gson.toJsonTree(new ValvulaControlRequest("ABRIR", 30, null, null)).getAsJsonObject();
        assertEquals("ABRIR", cuerpo.get("accion").getAsString());
        assertEquals(30, cuerpo.get("duracion").getAsInt());
        assertFalse(cuerpo.has("usuario"));
        assertFalse(cuerpo.has("origen"));
    }
}
