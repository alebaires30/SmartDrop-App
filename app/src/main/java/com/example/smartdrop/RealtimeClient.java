package com.example.smartdrop;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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

        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String token = preferences.getString("access_token", "");
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

    public static synchronized void disconnect() {
        if (socket != null) {
            socket.close(1000, "logout");
            socket = null;
        }
    }
}
