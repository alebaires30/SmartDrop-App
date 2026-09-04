package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class ValvulaEstadoResponse {
    @SerializedName("estado_actual") private String estadoActual;
    @SerializedName("temporizador_activo") private TemporizadorActivo temporizadorActivo;
    public String getEstadoActual() { return estadoActual; }
    public TemporizadorActivo getTemporizadorActivo() { return temporizadorActivo; }
}