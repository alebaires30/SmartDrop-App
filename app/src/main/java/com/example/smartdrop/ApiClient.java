package com.example.smartdrop;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8000/";

    private static Retrofit retrofit = null;
    private static Retrofit retrofitAutenticado = null;


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
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences("sesion", Context.MODE_PRIVATE);

            Interceptor authInterceptor = chain -> {
                String token = prefs.getString("access_token", "");
                Request original = chain.request();
                Request nuevo = original.newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(nuevo);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .build();

            retrofitAutenticado = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitAutenticado;
    }
}