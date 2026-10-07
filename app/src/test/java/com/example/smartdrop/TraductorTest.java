package com.example.smartdrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Traducción con el catálogo real que trae la app (el mismo que sirve el backend a la web). */
public class TraductorTest {

    private static Traductor traductor;

    @BeforeClass
    public static void cargarCatalogo() throws IOException {
        String json = new String(Files.readAllBytes(Paths.get("src/main/assets/i18n_en.json")), StandardCharsets.UTF_8);
        traductor = Traductor.desdeJson(json);
    }

    @Test
    public void catalogoNoVacio() {
        assertFalse(traductor.vacio());
    }

    @Test
    public void frasesExactasConEspaciosYMayusculas() {
        assertEquals("Dark mode", traductor.traducir("Modo oscuro"));
        assertEquals("  Log out  ", traductor.traducir("  Cerrar sesión  "));
        assertEquals("LOG OUT", traductor.traducir("CERRAR SESIÓN"));
        assertEquals("Save changes.", traductor.traducir("Guardar cambios."));
    }

    @Test
    public void plantillasConHuecosYValoresTraducidos() {
        assertEquals("Showing 96 points", traductor.traducir("Mostrando 96 puntos"));
        assertEquals("Possible leak in NIC-0016 (98 %)", traductor.traducir("Posible fuga en NIC-0016 (98 %)"));
        assertEquals("Low risk: ≈ 5h to the critical level", traductor.traducir("Riesgo Baja: ≈ 5h hasta nivel crítico"));
    }

    @Test
    public void segmentosYTextoEnVariasLineas() {
        assertEquals("Last reading: 10:30 AM", traductor.traducir("Última lectura: 10:30 AM"));
        assertEquals("Pressure: Normal", traductor.traducir("Presión: Normal"));
        assertEquals("Tank level: low value", traductor.traducir("Nivel del tanque: valor bajo"));
        assertEquals("Hide details ▴", traductor.traducir("Ocultar detalles ▴"));
        assertEquals("Change saved: Dark mode", traductor.traducir("Cambio guardado: Modo oscuro"));
    }

    @Test
    public void noTocaNombresNiNumeros() {
        assertEquals("María Fernanda Hernández López", traductor.traducir("María Fernanda Hernández López"));
        assertEquals("Calle Los Almendros #45, Soyapango", traductor.traducir("Calle Los Almendros #45, Soyapango"));
        assertEquals("12.5", traductor.traducir("12.5"));
        assertEquals("", traductor.traducir(""));
    }
}
