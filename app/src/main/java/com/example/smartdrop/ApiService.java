package com.example.smartdrop;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
public interface ApiService {

    //Corresponde al punto de acceso: POST (backend)
    @POST("auth/registro/")
    Call<RegisterResponse> registrarUsuario(@Body RegisterRequest request);

    @POST("auth/login/")
    Call<LoginResponse> loginUsuario(@Body LoginRequest request);

    @GET("api/graficas/")
    Call<GraficasResponse> obtenerGraficas(
            @Query("parametros") String parametros,
            @Query("desde") String desde,
            @Query("hasta") String hasta
    );
    @GET("api/resumen/")
    Call<ResumenDashboardResponse> obtenerResumen();

    @GET("auth/mis-viviendas/")
    Call<MisViviendasResponse> obtenerMisViviendas();

    @POST("auth/vincular-vivienda/")
    Call<VincularViviendaResponse> vincularVivienda(@Body VincularViviendaRequest request);

    @GET("auth/estado-agua/")
    Call<EstadoAguaResponse> obtenerEstadoAgua();

    @GET("auth/consumo/")
    Call<ConsumoResponse> obtenerConsumo(@Query("periodo") String periodo);

    @GET("auth/retroalimentacion/")
    Call<RetroalimentacionResponse> obtenerRetroalimentacion();

    @GET("auth/recomendaciones/")
    Call<RecomendacionesResponse> obtenerRecomendaciones();

    @GET("api/valvula/{id}/estado/")
    Call<ValvulaEstadoResponse> obtenerEstadoValvula(@Path("id") int idValvula);

    @retrofit2.http.POST("api/valvula/{id}/abrir-remoto/")
    Call<okhttp3.ResponseBody> abrirValvulaRemoto(@Path("id") int idValvula, @Body java.util.Map<String, Object> body);

    @retrofit2.http.POST("api/valvula/{id}/cerrar-remoto/")
    Call<okhttp3.ResponseBody> cerrarValvulaRemoto(@Path("id") int idValvula, @Body java.util.Map<String, Object> body);




}
