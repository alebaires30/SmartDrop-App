package com.example.smartdrop;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conversión de las fechas ISO 8601 que envía el servidor a textos cortos en hora local. */
final class Fechas {

    private static final Pattern DESFASE = Pattern.compile("([+-]\\d{2}:\\d{2})$");

    private Fechas() { }

    /** Fecha ISO (terminada en Z, en un desfase ±hh:mm o sin zona = UTC) a Date; null si no es válida. */
    static Date parse(String iso) {
        if (iso == null || iso.length() < 19) return null;
        try {
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Matcher desfase = DESFASE.matcher(iso);
            formato.setTimeZone(TimeZone.getTimeZone(desfase.find() ? "GMT" + desfase.group(1) : "UTC"));
            return formato.parse(iso.substring(0, 19));
        } catch (ParseException e) {
            return null;
        }
    }

    /** Fecha con el patrón indicado en hora local (p. ej. "hh:mm a"), o `siFalla` si la fecha no es válida. */
    static String formatear(String iso, String patron, String siFalla) {
        Date fecha = parse(iso);
        return fecha == null ? siFalla : new SimpleDateFormat(patron, Locale.getDefault()).format(fecha);
    }

    /** Fecha y hora local corta ("06/10 14:25"), o cadena vacía si la fecha no es válida. */
    static String corta(String iso) {
        return formatear(iso, "dd/MM HH:mm", "");
    }
}
