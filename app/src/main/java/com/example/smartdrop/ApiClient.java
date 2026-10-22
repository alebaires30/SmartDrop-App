package com.example.smartdrop;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

        private static final String BASE_URL = BuildConfig.DEBUG
            ? "http://" + BuildConfig.API_HOST + ":8000/"
            : "https://" + BuildConfig.API_HOST + "/";

    public static String getRealtimeUrl() {
        return BuildConfig.DEBUG
            ? "ws://" + BuildConfig.API_HOST + ":8000/ws/sensors/"
            : "wss://" + BuildConfig.API_HOST + "/ws/sensors/";
    }

    private static Retrofit retrofit = null;
    private static Retrofit retrofitAutenticado = null;
    private static OkHttpClient httpAutenticado = null;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    /** Conexión con la sesión iniciada (Gotita la usa para recibir sus respuestas frase por frase). */
    public static OkHttpClient getHttpAutenticado(Context context) {
        getClientAutenticado(context);
        return httpAutenticado;
    }


    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }


    public static Retrofit getClientAutenticado(Context context) {
        if (retrofitAutenticado == null) {
            Context app = context.getApplicationContext();

            Interceptor authInterceptor = chain -> {
                String token = Sesion.accessToken(app);
                Request original = chain.request();
                Request nuevo = original.newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(nuevo);
            };

            // Las gráficas de semana/mes pueden tardar la primera vez que el servidor junta los datos.
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(authInterceptor)
                    .authenticator(new TokenAuthenticator(app))
                    .build();
            httpAutenticado = client;

            retrofitAutenticado = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitAutenticado;
    }

    /** Renueva el token de acceso con el token de renovación cuando el servidor responde 401. */
    private static class TokenAuthenticator implements Authenticator {
        private final Context app;

        TokenAuthenticator(Context app) {
            this.app = app;
        }

        @Override
        public synchronized Request authenticate(Route route, Response response) throws IOException {
            if (responseCount(response) >= 2) return null;
            String refresh = Sesion.refreshToken(app);
            if (refresh.isEmpty()) return null;

            String actual = Sesion.accessToken(app);
            String usado = response.request().header("Authorization");
            if (usado != null && !usado.equals("Bearer " + actual)) {
                // Otra petición ya renovó el token mientras esta esperaba.
                return response.request().newBuilder().header("Authorization", "Bearer " + actual).build();
            }

            String nuevo = renovar(refresh);
            if (nuevo == null) return null;
            Sesion.guardarAccess(app, nuevo);
            return response.request().newBuilder().header("Authorization", "Bearer " + nuevo).build();
        }

        /**
         * Pide un token de acceso nuevo; null si no se pudo. Solo un 401 cierra la sesión: un 503 (base de
         * datos caída), un 429 o la falta de red son pasajeros y la sesión se conserva.
         */
        private String renovar(String refresh) {
            JsonObject cuerpo = new JsonObject();
            cuerpo.addProperty("refresh", refresh);
            Request peticion = new Request.Builder()
                    .url(BASE_URL + "auth/refresh/")
                    .post(RequestBody.create(cuerpo.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response respuesta = new OkHttpClient().newCall(peticion).execute()) {
                if (respuesta.code() == 401) {
                    // Token vencido, cerrado o cuenta desactivada. Si mientras tanto se inició otra
                    // sesión (otro token de renovación), esa no se toca.
                    if (refresh.equals(Sesion.refreshToken(app))) Sesion.expirar(app);
                    return null;
                }
                if (!respuesta.isSuccessful() || respuesta.body() == null) return null;
                JsonObject json = JsonParser.parseString(respuesta.body().string()).getAsJsonObject();
                return json.has("access") ? json.get("access").getAsString() : null;
            } catch (IOException | RuntimeException e) {
                return null;
            }
        }

        private int responseCount(Response response) {
            int total = 1;
            while ((response = response.priorResponse()) != null) total++;
            return total;
        }
    }
}