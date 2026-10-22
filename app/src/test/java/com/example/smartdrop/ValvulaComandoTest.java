package com.example.smartdrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ValvulaComandoTest {

    @Test
    public void normalizaLaAccionComoLaEsperaElServidor() {
        assertEquals("ABRIR", ValvulaComando.normalizarAccion(" abrir "));
        assertEquals("CERRAR", ValvulaComando.normalizarAccion("Cerrar"));
        assertNull(ValvulaComando.normalizarAccion("explotar"));
        assertNull(ValvulaComando.normalizarAccion(null));
    }

    @Test
    public void duracionValidaEsCeroOEntre5y120() {
        assertTrue(ValvulaComando.duracionValida(0));
        assertTrue(ValvulaComando.duracionValida(5));
        assertTrue(ValvulaComando.duracionValida(120));
        assertFalse(ValvulaComando.duracionValida(4));
        assertFalse(ValvulaComando.duracionValida(121));
        assertFalse(ValvulaComando.duracionValida(-10));
    }

    @Test
    public void leeSoloNumerosEnteros() {
        assertEquals(45, ValvulaComando.leerSegundos(" 45 "));
        assertEquals(-1, ValvulaComando.leerSegundos("4.5"));
        assertEquals(-1, ValvulaComando.leerSegundos("abc"));
        assertEquals(-1, ValvulaComando.leerSegundos(""));
        assertEquals(-1, ValvulaComando.leerSegundos("99999999999"));
        assertEquals(-1, ValvulaComando.leerSegundos(null));
    }

    @Test
    public void formateaLaCuentaRegresivaYLaDuracion() {
        assertEquals("1:15", ValvulaComando.cuentaRegresiva(75));
        assertEquals("0:09", ValvulaComando.cuentaRegresiva(9));
        assertEquals("0:00", ValvulaComando.cuentaRegresiva(-3));
        assertEquals("1 min 35 s", ValvulaComando.duracionTexto(95));
        assertEquals("1 h 1 min", ValvulaComando.duracionTexto(3700));
        assertEquals("40 s", ValvulaComando.duracionTexto(40));
    }
}
