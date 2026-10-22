package com.example.smartdrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FechasTest {

    private TimeZone zonaOriginal;
    private Locale localeOriginal;

    @Before
    public void usarHoraDeElSalvador() {
        zonaOriginal = TimeZone.getDefault();
        localeOriginal = Locale.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/El_Salvador"));
        Locale.setDefault(Locale.forLanguageTag("es-SV"));
    }

    @After
    public void restaurar() {
        TimeZone.setDefault(zonaOriginal);
        Locale.setDefault(localeOriginal);
    }

    @Test
    public void convierteUtcAHoraLocal() {
        assertEquals("21/10 22:32", Fechas.corta("2026-10-22T04:32:28+00:00"));
        assertEquals("22:32:28", Fechas.formatear("2026-10-22T04:32:28Z", "HH:mm:ss", ""));
        assertEquals("22:32:28", Fechas.formatear("2026-10-22T04:32:28.123456+00:00", "HH:mm:ss", ""));
    }

    @Test
    public void fechasInvalidasNoRompen() {
        assertNull(Fechas.parse(null));
        assertNull(Fechas.parse("ayer"));
        assertEquals("-", Fechas.formatear("no-es-fecha-valida-xx", "HH:mm", "-"));
    }
}
