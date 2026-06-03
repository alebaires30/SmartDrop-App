package com.example.smartdrop;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
public interface ApiService {

    //Corresponde al punto de acceso: POST (backend)
    @POST("auth/registro/")
    Call<RegisterResponse> registrarUsuario(@Body RegisterRequest request);

    @POST("auth/login/")
    Call<LoginResponse> loginUsuario(@Body LoginRequest request);
}
