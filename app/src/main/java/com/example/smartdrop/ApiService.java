package com.example.smartdrop;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
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

    @GET("api/valvula/estado/")
    Call<ValvulaEstadoPublicoResponse> obtenerEstadoValvulaPublico();

    @POST("api/valvula/{id}/control/")
    Call<ResponseBody> controlarValvula(@Path("id") int idValvula, @Body ValvulaControlRequest request);

    @GET("api/buscar/")
    Call<BusquedaGlobalResponse> buscarGlobal(@Query("q") String termino);

    @GET("api/valvula/{id}/logs/")
    Call<ValvulaLogsResponse> obtenerLogsValvula(@Path("id") int idValvula);

    // ── Predicción de suministro (admin) ──
    @GET("v1/ml/zones/summary/")
    Call<ZonasSummaryResponse> obtenerResumenZonas();

    @GET("v1/ml/zones/{id}/current-status/")
    Call<ZonaEstadoResponse> obtenerEstadoZona(@Path("id") int zoneId);

    @POST("v1/ml/predictions/run/")
    Call<PrediccionJobResponse> ejecutarPredicciones();

    @GET("v1/ml/predictions/jobs/{id}/")
    Call<PrediccionJobResponse> obtenerEstadoPrediccion(@Path("id") String jobId);

    // ── Predicción de fugas y avisos (admin) ──
    @GET("v1/ml/leaks/")
    Call<FugasResponse> obtenerFugas();

    @GET("v1/ml/leaks/alerts/")
    Call<AlertasFugaResponse> obtenerAlertasFuga(@Query("solo_no_leidas") Integer soloNoLeidas);

    @POST("v1/ml/leaks/alerts/read/")
    Call<ResponseBody> marcarAlertasFugaLeidas(@Body Map<String, Object> cuerpo);

    // ── Sistema de reportes ──
    @GET("api/reportes/catalogo/")
    Call<ReporteCatalogoResponse> obtenerCatalogoReportes();

    @GET("api/reportes/")
    Call<ReporteListResponse> obtenerMisReportes();

    @GET("api/reportes/comunidad/")
    Call<ReporteListResponse> obtenerReportesComunidad();

    @Multipart
    @POST("api/reportes/")
    Call<ReporteCrearResponse> crearReporte(
            @PartMap Map<String, RequestBody> campos,
            @Part List<MultipartBody.Part> adjuntos
    );

    @GET("api/reportes/{id}/")
    Call<ReporteDetalleResponse> obtenerReporteDetalle(@Path("id") int idReporte);

    @POST("api/reportes/{id}/")
    Call<ReporteMensajeResponse> enviarMensajeReporte(
            @Path("id") int idReporte,
            @Body Map<String, String> mensaje
    );

    @Multipart
    @POST("api/reportes/{id}/adjuntar/")
    Call<ReporteCrearResponse> adjuntarEvidencia(
            @Path("id") int idReporte,
            @Part List<MultipartBody.Part> adjuntos
    );
}
