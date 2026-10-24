package com.example.smartdrop;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Gotita le pregunta al servidor de SmartDrop (nunca directo a Claude: la llave solo está en el servidor).
 * La respuesta llega frase por frase para que Gotita empiece a hablar sin esperar el final.
 * También trae el historial de conversaciones (las de los últimos 20 días), que es el mismo de la web.
 */
final class GotitaCliente {

    interface Eventos {
        void alTexto(String frase);
        void alAccion(JsonObject accion);
        void alError(String mensaje);
        void alConversacion(int id);
        void alFin();
    }

    /** Respuesta del historial: el JSON del servidor, o null si no se pudo. */
    interface AlJson {
        void listo(JsonObject json);
    }

    interface AlEstado {
        void listo(boolean activa, String rol, boolean vozNatural);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private GotitaCliente() { }

    private static OkHttpClient http(Context context, int segundos) {
        return ApiClient.getHttpAutenticado(context).newBuilder()
                .readTimeout(segundos, TimeUnit.SECONDS)
                .build();
    }

    /** ¿Gotita está activa en el servidor? (si no, el botón no se muestra). */
    static void estado(Context context, AlEstado alEstado) {
        Request peticion = new Request.Builder().url(ApiClient.getBaseUrl() + "api/asistente/").get().build();
        http(context, 15).newCall(peticion).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Sin conexión: se muestra igual; Gotita avisará si no puede responder.
                PRINCIPAL.post(() -> alEstado.listo(true, null, false));
            }

            @Override
            public void onResponse(Call call, Response response) {
                boolean activa = true;
                boolean vozNatural = false;
                String rol = null;
                try (ResponseBody cuerpo = response.body()) {
                    if (response.code() == 503) activa = false;
                    if (response.isSuccessful() && cuerpo != null) {
                        JsonObject json = new JsonParser().parse(cuerpo.string()).getAsJsonObject();
                        activa = !json.has("activa") || json.get("activa").getAsBoolean();
                        rol = json.has("rol") && !json.get("rol").isJsonNull() ? json.get("rol").getAsString() : null;
                        vozNatural = json.has("voz_natural") && json.get("voz_natural").getAsBoolean();
                    }
                } catch (IOException | RuntimeException ignored) {
                    // Respuesta inesperada: se deja visible.
                }
                boolean finalActiva = activa;
                boolean finalVozNatural = vozNatural;
                String finalRol = rol;
                PRINCIPAL.post(() -> alEstado.listo(finalActiva, finalRol, finalVozNatural));
            }
        });
    }

    /** Envía una pregunta. Devuelve la llamada para poder cancelarla (botón Parar o nueva pregunta). */
    static Call preguntar(Context context, JsonObject cuerpo, Eventos eventos) {
        Request peticion = new Request.Builder()
                .url(ApiClient.getBaseUrl() + "api/asistente/turno/")
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(cuerpo.toString(), JSON))
                .build();
        Call llamada = http(context, 90).newCall(peticion);
        llamada.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) PRINCIPAL.post(() -> eventos.alError(null));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody cuerpoRespuesta = response.body()) {
                    if (cuerpoRespuesta == null) {
                        PRINCIPAL.post(() -> eventos.alError(null));
                        return;
                    }
                    if (!response.isSuccessful()) {
                        String mensaje = null;
                        try {
                            JsonObject json = new JsonParser().parse(cuerpoRespuesta.string()).getAsJsonObject();
                            if (json.has("error")) mensaje = json.get("error").getAsString();
                            else if (json.has("detail")) mensaje = json.get("detail").getAsString();
                        } catch (RuntimeException ignored) {
                            // La respuesta de error no traía mensaje.
                        }
                        String finalMensaje = mensaje;
                        PRINCIPAL.post(() -> eventos.alError(finalMensaje));
                        return;
                    }
                    leerEventos(cuerpoRespuesta.source(), call, eventos);
                } catch (IOException e) {
                    if (!call.isCanceled()) PRINCIPAL.post(() -> eventos.alError(null));
                }
            }
        });
        return llamada;
    }

    private static void leerEventos(BufferedSource fuente, Call call, Eventos eventos) throws IOException {
        String tipo = "message";
        StringBuilder datos = new StringBuilder();
        String linea;
        while (!call.isCanceled() && (linea = fuente.readUtf8Line()) != null) {
            if (linea.isEmpty()) {
                despachar(tipo, datos.toString(), eventos);
                tipo = "message";
                datos.setLength(0);
            } else if (linea.startsWith("event:")) {
                tipo = linea.substring(6).trim();
            } else if (linea.startsWith("data:")) {
                datos.append(linea.substring(5).trim());
            }
        }
        if (!call.isCanceled()) PRINCIPAL.post(eventos::alFin);
    }

    private static void despachar(String tipo, String datos, Eventos eventos) {
        if (datos.isEmpty()) return;
        JsonObject json;
        try {
            json = new JsonParser().parse(datos).getAsJsonObject();
        } catch (RuntimeException e) {
            return; // Evento dañado: se ignora.
        }
        switch (tipo) {
            case "texto":
                if (json.has("t")) {
                    String frase = json.get("t").getAsString();
                    PRINCIPAL.post(() -> eventos.alTexto(frase));
                }
                break;
            case "accion":
                PRINCIPAL.post(() -> eventos.alAccion(json));
                break;
            case "error":
                String mensaje = json.has("mensaje") ? json.get("mensaje").getAsString() : null;
                PRINCIPAL.post(() -> eventos.alError(mensaje));
                break;
            case "conversacion":
                if (json.has("id") && json.get("id").isJsonPrimitive()) {
                    int id = json.get("id").getAsInt();
                    PRINCIPAL.post(() -> eventos.alConversacion(id));
                }
                break;
            default:
                break;
        }
    }

    // ── Historial ──

    private static String urlHistorial(Integer id) {
        return ApiClient.getBaseUrl() + "api/asistente/conversaciones/" + (id != null ? id + "/" : "");
    }

    /** Lista de conversaciones guardadas. */
    static void conversaciones(Context context, AlJson alJson) {
        pedirJson(context, new Request.Builder().url(urlHistorial(null)).get().build(), alJson);
    }

    /** Los mensajes de una conversación. */
    static void conversacion(Context context, int id, AlJson alJson) {
        pedirJson(context, new Request.Builder().url(urlHistorial(id)).get().build(), alJson);
    }

    /** Borra una conversación (o todas si id es null). */
    static void borrar(Context context, Integer id, AlJson alJson) {
        pedirJson(context, new Request.Builder().url(urlHistorial(id)).delete().build(), alJson);
    }

    private static void pedirJson(Context context, Request peticion, AlJson alJson) {
        http(context, 20).newCall(peticion).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                PRINCIPAL.post(() -> alJson.listo(null));
            }

            @Override
            public void onResponse(Call call, Response response) {
                JsonObject json = null;
                try (ResponseBody cuerpo = response.body()) {
                    if (response.isSuccessful() && cuerpo != null) {
                        json = new JsonParser().parse(cuerpo.string()).getAsJsonObject();
                    }
                } catch (IOException | RuntimeException ignored) {
                    json = null; // Respuesta inesperada: se toma como error.
                }
                JsonObject finalJson = json;
                PRINCIPAL.post(() -> alJson.listo(finalJson));
            }
        });
    }
}
