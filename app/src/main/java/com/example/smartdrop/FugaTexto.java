package com.example.smartdrop;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Textos cortos para mostrar una posible fuga (tarjetas, avisos y notificaciones) de forma fácil de leer. */
final class FugaTexto {

    static final String ACCION_POR_DEFECTO =
            "Revisar tuberías, llaves y sanitarios. Si el agua sigue corriendo sin uso, cerrar la válvula.";

    private FugaTexto() { }

    /** Datos clave en una línea: "Pérdida ≈ 13.2 L/h · Desde 05/10 12:00 · ≈ 169 L perdidos". */
    static String datosClave(JsonObject m, String inicio) {
        List<String> partes = new ArrayList<>();
        Double perdida = numero(m, "perdida_estimada_lph");
        if (perdida != null && perdida > 0) partes.add(String.format(Locale.getDefault(), "Pérdida ≈ %.1f L/h", perdida));
        if (inicio != null && !inicio.isEmpty()) partes.add("Desde " + inicio);
        Double perdidos = numero(m, "litros_perdidos_estimados");
        if (perdidos != null && perdidos > 0) partes.add(String.format(Locale.getDefault(), "≈ %.0f L perdidos", perdidos));
        return android.text.TextUtils.join("  ·  ", partes);
    }

    /** Detalle desplegable: por qué se detectó, mediciones y contacto del titular. */
    static String detalle(List<String> causas, JsonObject m, String titular, String telefono) {
        StringBuilder texto = new StringBuilder();
        if (causas != null && !causas.isEmpty()) {
            texto.append("Por qué se detectó:");
            for (String causa : causas) texto.append("\n• ").append(causa);
            texto.append("\n\n");
        }
        texto.append("Mediciones:");
        texto.append("\n• Flujo mínimo (6 h): ").append(valor(m, "flujo_minimo_6h_lpm", "L/min", 2))
                .append(" · normal ").append(valor(m, "flujo_minimo_normal_lpm", "L/min", 2));
        texto.append("\n• Presión: ").append(valor(m, "presion_actual", "", 2))
                .append(" · normal ").append(valor(m, "presion_normal", "", 2));
        texto.append("\n• Tanque: ").append(valor(m, "nivel_tanque_litros", "L", 1))
                .append(" (").append(valor(m, "nivel_tanque_pct", "%", 0)).append(")");
        texto.append("\n\nContacto: ").append(textoO(titular)).append(" · Tel. ").append(textoO(telefono));
        return texto.toString();
    }

    static Double numero(JsonObject m, String clave) {
        if (m == null || !m.has(clave) || m.get(clave).isJsonNull()) return null;
        try {
            return m.get(clave).getAsDouble();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String valor(JsonObject m, String clave, String unidad, int decimales) {
        Double numero = numero(m, clave);
        if (numero == null) return "sin dato";
        String texto = String.format(Locale.getDefault(), "%." + decimales + "f", numero);
        return unidad == null || unidad.isEmpty() ? texto : texto + " " + unidad;
    }

    private static String textoO(String valor) {
        return valor == null || valor.isEmpty() ? "sin dato" : valor;
    }
}
