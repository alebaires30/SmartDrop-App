package com.example.smartdrop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Convierte una respuesta de error del servidor en un mensaje corto para el usuario.
 *
 * El backend responde {"error": "..."} (o {"detail": "..."} en errores de DRF). Con 429 el servidor
 * limitó las peticiones (rate limiting): se muestra su mensaje ("Espera N segundos...").
 * Nunca se muestra el cuerpo crudo: solo el texto de esos campos, y si es demasiado largo (una traza o
 * una página de error) se usa el mensaje genérico.
 */
final class ErrorApi {

    static final int DEMASIADAS_SOLICITUDES = 429;
    /** Un mensaje pensado para el usuario cabe de sobra; más largo ya no es un mensaje. */
    static final int LARGO_MAXIMO = 300;

    private ErrorApi() { }

    /** Mensaje del servidor si viene en el cuerpo; si no, uno genérico según el código HTTP. */
    static String mensaje(int codigo, String cuerpo) {
        String delServidor = mensajeDelCuerpo(cuerpo);
        if (delServidor != null) return delServidor;
        switch (codigo) {
            case 400: return "Los datos enviados no son válidos.";
            case 401: return "Tu sesión expiró. Vuelve a iniciar sesión.";
            case 403: return "No tienes permiso para hacer esto.";
            case 404: return "No se encontró lo que buscabas.";
            case 409: return "Ese dato ya está registrado.";
            case DEMASIADAS_SOLICITUDES: return "Demasiadas solicitudes. Espera un momento e inténtalo de nuevo.";
            default:
                return codigo >= 500
                        ? "El servidor no pudo completar la acción. Intenta de nuevo en unos minutos."
                        : "Error inesperado (" + codigo + "). Intenta de nuevo.";
        }
    }

    /** Texto de "error" o "detail" del JSON; null si el cuerpo no es JSON o no trae mensaje. */
    static String mensajeDelCuerpo(String cuerpo) {
        if (cuerpo == null || cuerpo.trim().isEmpty()) return null;
        try {
            JsonElement raiz = JsonParser.parseString(cuerpo);
            if (!raiz.isJsonObject()) return null;
            JsonObject json = raiz.getAsJsonObject();
            for (String clave : new String[]{"error", "detail", "mensaje"}) {
                if (json.has(clave) && json.get(clave).isJsonPrimitive()) {
                    String texto = json.get(clave).getAsString().trim();
                    if (!texto.isEmpty() && texto.length() <= LARGO_MAXIMO) return texto;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    /** Lee el cuerpo de error de Retrofit sin lanzar excepciones. */
    static String cuerpo(retrofit2.Response<?> respuesta) {
        try {
            return respuesta.errorBody() != null ? respuesta.errorBody().string() : null;
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    /** Atajo para usar directamente con una respuesta de Retrofit que no fue exitosa. */
    static String de(retrofit2.Response<?> respuesta) {
        return mensaje(respuesta.code(), cuerpo(respuesta));
    }

    /** Mensaje del servidor si lo trae; si no, el texto dado (cuando el código ya dice qué pasó). */
    static String de(retrofit2.Response<?> respuesta, String porDefecto) {
        return mensajeOPorDefecto(cuerpo(respuesta), porDefecto);
    }

    static String mensajeOPorDefecto(String cuerpo, String porDefecto) {
        String delServidor = mensajeDelCuerpo(cuerpo);
        return delServidor != null ? delServidor : porDefecto;
    }
}
