package com.example.smartdrop;

import java.util.Locale;

/** Reglas del comando de la válvula que comparte la app con el servidor (App/valve_service.py). */
final class ValvulaComando {

    static final String ABRIR = "ABRIR";
    static final String CERRAR = "CERRAR";
    static final int MIN_SEGUNDOS = 5;
    static final int MAX_SEGUNDOS = 120;

    private ValvulaComando() { }

    /** "abrir" / " Abrir " → "ABRIR"; null si no es abrir ni cerrar. */
    static String normalizarAccion(String accion) {
        if (accion == null) return null;
        String valor = accion.trim().toUpperCase(Locale.ROOT);
        return ABRIR.equals(valor) || CERRAR.equals(valor) ? valor : null;
    }

    /** Duración del temporizador: 0 = sin temporizador; entre 5 y 120 = válida. */
    static boolean duracionValida(int segundos) {
        return segundos == 0 || (segundos >= MIN_SEGUNDOS && segundos <= MAX_SEGUNDOS);
    }

    /** Lo que escribe el usuario en "Tiempo personalizado"; -1 si no es un número entero. */
    static int leerSegundos(String texto) {
        if (texto == null) return -1;
        String limpio = texto.trim();
        if (limpio.isEmpty() || limpio.length() > 6) return -1;
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 75 → "1:15"; 9 → "0:09". */
    static String cuentaRegresiva(long segundos) {
        long s = Math.max(0, segundos);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /** 95 → "1 min 35 s"; 3700 → "1 h 1 min". */
    static String duracionTexto(long segundos) {
        long total = Math.max(0, segundos);
        if (total < 60) return total + " s";
        long horas = total / 3600;
        long minutos = (total % 3600) / 60;
        long segs = total % 60;
        if (horas > 0) return minutos > 0 ? horas + " h " + minutos + " min" : horas + " h";
        return segs > 0 ? minutos + " min " + segs + " s" : minutos + " min";
    }
}
