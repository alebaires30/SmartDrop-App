package com.example.smartdrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** "Oye Gotita" solo despierta a Gotita al inicio de la frase: lo demás no sale del teléfono. */
public class GotitaOidoTest {

    @Test
    public void despiertaConOyeHeyOEyAlInicio() {
        assertEquals("cómo está mi tanque", GotitaOido.trasDespertar("oye gotita cómo está mi tanque"));
        assertEquals("hay alertas?", GotitaOido.trasDespertar("Oye, Gotita, ¿hay alertas?"));
        assertEquals("how is my tank", GotitaOido.trasDespertar("Hey Gotita how is my tank"));
        assertEquals("", GotitaOido.trasDespertar("ey gotita"));
        assertEquals("", GotitaOido.trasDespertar("¡Oye Gotita!"));
    }

    @Test
    public void aceptaComoSueleEntenderElTelefonoGotitaSoloDespuesDelLlamado() {
        assertEquals("qué tal", GotitaOido.trasDespertar("oye godita qué tal"));
        assertEquals("qué tal", GotitaOido.trasDespertar("oye go tita qué tal"));
        assertNull(GotitaOido.trasDespertar("godita qué tal"));
    }

    @Test
    public void unaFraseQueSoloMencionaGotitaSeIgnora() {
        assertNull(GotitaOido.trasDespertar("gotita cómo está mi tanque"));
        assertNull(GotitaOido.trasDespertar("le dije a mi hermana oye gotita cómo está el tanque"));
        assertNull(GotitaOido.trasDespertar("se cayó una gotita del grifo"));
        assertNull(GotitaOido.trasDespertar("ok gotita apaga todo"));
        assertNull(GotitaOido.trasDespertar("I got it a minute ago"));
        assertNull(GotitaOido.trasDespertar("oye got it a question"));
        assertNull(GotitaOido.trasDespertar("oye cotita qué tal"));
        assertNull(GotitaOido.trasDespertar("oye gotitaaa"));
        assertNull(GotitaOido.trasDespertar(""));
        assertNull(GotitaOido.trasDespertar(null));
    }

    @Test
    public void pararNecesitaElNombreYLaFraseCompleta() {
        assertTrue(GotitaOido.pideParar("gotita para"));
        assertTrue(GotitaOido.pideParar("Oye Gotita, ya basta"));
        assertTrue(GotitaOido.pideParar("gotita cállate."));
        assertFalse(GotitaOido.pideParar("para"));
        assertFalse(GotitaOido.pideParar("la gotita para el tanque"));
        assertFalse(GotitaOido.pideParar("oye gotita para qué sirve la válvula"));
    }
}
