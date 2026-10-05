package com.example.smartdrop;

import android.content.Context;

import androidx.core.content.ContextCompat;


public class ColorSeveridad {

    public static int colorDe(Context context, String severidad) {
        if (severidad == null) return ContextCompat.getColor(context, R.color.success);
        switch (severidad) {
            case "rojo":     return ContextCompat.getColor(context, R.color.danger);
            case "amarillo": return ContextCompat.getColor(context, R.color.status_yellow);
            case "gris":     return ContextCompat.getColor(context, R.color.text_muted);
            default:         return ContextCompat.getColor(context, R.color.success);
        }
    }

    public static String iconoDe(String severidad) {
        if (severidad == null) return "✅";
        switch (severidad) {
            case "rojo":     return "⛔";
            case "amarillo": return "⚠️";
            case "gris":     return "ℹ️";
            default:         return "✅";
        }
    }
}