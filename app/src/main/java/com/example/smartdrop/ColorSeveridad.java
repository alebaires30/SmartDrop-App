package com.example.smartdrop;

import android.graphics.Color;


public class ColorSeveridad {

    public static int colorDe(String severidad) {
        if (severidad == null) return Color.parseColor("#27AE60");
        switch (severidad) {
            case "rojo":     return Color.parseColor("#E74C3C");
            case "amarillo": return Color.parseColor("#F1C40F");
            case "gris":     return Color.parseColor("#9E9EBE");
            default:         return Color.parseColor("#27AE60");
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