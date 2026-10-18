package com.example.smartdrop;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

/**
 * Traduce la severidad que manda el servidor ("verde", "amarillo", "rojo", "gris", "azul")
 * al vocabulario de estado único de la app: ok, warn, bad, none e info.
 * Mismo color en toda la app y siempre acompañado de una palabra (nunca solo color).
 */
public class ColorSeveridad {

    public static final String OK = "ok";
    public static final String WARN = "warn";
    public static final String BAD = "bad";
    public static final String NONE = "none";
    public static final String INFO = "info";

    /** Estado de diseño para una severidad del servidor. null se trata como "verde" (como antes). */
    public static String estadoDe(String severidad) {
        if (severidad == null) return OK;
        switch (severidad) {
            case "rojo":     return BAD;
            case "amarillo": return WARN;
            case "gris":     return NONE;
            case "azul":     return INFO;
            default:         return OK;
        }
    }

    /** Color de texto / icono del estado (≥ 4.5:1 sobre surface y sobre su fondo de pill). */
    @ColorInt
    public static int colorDe(Context context, String severidad) {
        switch (estadoDe(severidad)) {
            case BAD:  return ContextCompat.getColor(context, R.color.bad);
            case WARN: return ContextCompat.getColor(context, R.color.warn);
            case NONE: return ContextCompat.getColor(context, R.color.none);
            case INFO: return ContextCompat.getColor(context, R.color.anil);
            default:   return ContextCompat.getColor(context, R.color.ok);
        }
    }

    /** Color de fondo suave del estado (pill, aviso, icon chip). */
    @ColorInt
    public static int fondoDe(Context context, String severidad) {
        switch (estadoDe(severidad)) {
            case BAD:  return ContextCompat.getColor(context, R.color.bad_bg);
            case WARN: return ContextCompat.getColor(context, R.color.warn_bg);
            case NONE: return ContextCompat.getColor(context, R.color.none_bg);
            case INFO: return ContextCompat.getColor(context, R.color.anil_soft);
            default:   return ContextCompat.getColor(context, R.color.ok_bg);
        }
    }

    /** Fondo de la pill de estado (@drawable/bg_status_*). */
    @DrawableRes
    public static int fondoPillDe(String severidad) {
        switch (estadoDe(severidad)) {
            case BAD:  return R.drawable.bg_status_bad;
            case WARN: return R.drawable.bg_status_warn;
            case NONE: return R.drawable.bg_status_none;
            case INFO: return R.drawable.bg_status_info;
            default:   return R.drawable.bg_status_ok;
        }
    }

    /** Punto de 8dp del estado (@drawable/status_dot_*). */
    @DrawableRes
    public static int puntoDe(String severidad) {
        switch (estadoDe(severidad)) {
            case BAD:  return R.drawable.status_dot_bad;
            case WARN: return R.drawable.status_dot_warn;
            case NONE: return R.drawable.status_dot_none;
            case INFO: return R.drawable.status_dot_info;
            default:   return R.drawable.status_dot_ok;
        }
    }

    /** Icono vectorial del estado (se tiñe con colorDe). */
    @DrawableRes
    public static int iconoResDe(String severidad) {
        switch (estadoDe(severidad)) {
            case BAD:  return R.drawable.ic_alert_circle;
            case WARN: return R.drawable.ic_warning;
            case NONE:
            case INFO: return R.drawable.ic_info;
            default:   return R.drawable.ic_check_circle;
        }
    }

    /**
     * Convierte un TextView en pill de estado (punto + palabra) para la severidad dada.
     * Úsalo sobre un TextView con style="@style/Widget.SmartDrop.StatusPill" (padding y tipografía);
     * solo cambia fondo, color y punto. El texto lo pone quien llama.
     */
    /** Estado de un reporte (pendiente, en_proceso, resuelto) a la severidad que entiende aplicarPill. */
    public static String deEstadoReporte(String estado) {
        if (estado == null) return "gris";
        switch (estado) {
            case "resuelto":   return "verde";
            case "en_proceso": return "azul";
            case "pendiente":  return "amarillo";
            default:           return "gris";
        }
    }

    public static void aplicarPill(TextView pill, String severidad) {
        Context context = pill.getContext();
        pill.setBackgroundResource(fondoPillDe(severidad));
        pill.setTextColor(colorDe(context, severidad));
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(puntoDe(severidad), 0, 0, 0);
    }

    /**
     * Marcador de texto para anteponer al estado. Antes eran emoji; ahora un punto neutro que
     * toma el color del texto (el TextView ya se pinta con colorDe). Mejor aún: aplicarPill().
     */
    public static String iconoDe(String severidad) {
        return "●";
    }

}
