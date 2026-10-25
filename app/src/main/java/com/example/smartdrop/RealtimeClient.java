package com.example.smartdrop;

import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class RealtimeClient {
    public static final String ACTION_SENSOR_READING =
            "com.example.smartdrop.SENSOR_READING";
    public static final String EXTRA_SENSOR_ID = "id_sensor";
    public static final String EXTRA_VALUE = "valor";
    public static final String EXTRA_TIMESTAMP = "fecha_registro";

    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static WebSocket socket;

    private RealtimeClient() { }

    public static synchronized void connect(Context context) {
        if (socket != null) return;

        String token = Sesion.accessToken(context);
        if (token.isEmpty()) return;

        Request request = new Request.Builder()
                .url(ApiClient.getRealtimeUrl())
                .addHeader("Authorization", "Bearer " + token)
                .build();

        socket = CLIENT.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject payload = new JSONObject(text);
                    if ("leak_alert".equals(payload.optString("event"))) {
                        avisoDeFuga(context, payload);
                        return;
                    }
                    if (!"sensor_reading".equals(payload.optString("event"))) return;

                    Intent intent = new Intent(ACTION_SENSOR_READING)
                            .setPackage(context.getPackageName())
                            .putExtra(EXTRA_SENSOR_ID, payload.optInt("id_sensor"))
                            .putExtra(EXTRA_VALUE, payload.optDouble("valor"))
                            .putExtra(EXTRA_TIMESTAMP, payload.optString("fecha_registro"));
                    context.sendBroadcast(intent);
                } catch (Exception ignored) {
                    // Ignore malformed realtime events and keep the socket alive.
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                synchronized (RealtimeClient.class) {
                    socket = null;
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                synchronized (RealtimeClient.class) {
                    socket = null;
                }
            }
        });
    }

    /**
     * Fuga detectada (PB027): se muestra como notificación en ese mismo momento, sin esperar la revisión
     * de cada 15 minutos. Al vecino le llega "Fuga detectada en su sector"; al admin, el aviso completo.
     */
    private static void avisoDeFuga(Context context, JSONObject payload) {
        if ("admin".equals(payload.optString("para"))) {
            AvisosFuga.revisarAhora(context, null);
        } else {
            AvisosUsuario.avisoEnVivo(context, payload.optLong("id_alerta"),
                    payload.optString("titulo", "Fuga detectada en su sector"), payload.optString("mensaje"));
        }
    }

    public static synchronized void disconnect() {
        if (socket != null) {
            socket.close(1000, "logout");
            socket = null;
        }
    }
}
