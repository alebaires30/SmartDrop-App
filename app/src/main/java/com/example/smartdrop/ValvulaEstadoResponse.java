package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class ValvulaEstadoResponse {
    @SerializedName("estado_actual") private String estadoActual;
    @SerializedName("temporizador_activo") private TemporizadorActivo temporizadorActivo;
    /** Hora ISO en que se abrió (null si está cerrada). */
    @SerializedName("abierta_desde") private String abiertaDesde;
    public String getEstadoActual() { return estadoActual; }
    public TemporizadorActivo getTemporizadorActivo() { return temporizadorActivo; }
    public String getAbiertaDesde() { return abiertaDesde; }
}
