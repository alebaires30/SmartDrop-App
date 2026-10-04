package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReporteCrearResponse {
    @SerializedName("ok") private boolean ok;
    @SerializedName("reporte") private Reporte reporte;
    @SerializedName("adjuntos_guardados") private int adjuntosGuardados;
    @SerializedName("adjuntos_errores") private List<String> adjuntosErrores;
    @SerializedName("error") private String error;

    public boolean isOk() { return ok; }
    public Reporte getReporte() { return reporte; }
    public int getAdjuntosGuardados() { return adjuntosGuardados; }
    public List<String> getAdjuntosErrores() { return adjuntosErrores; }
    public String getError() { return error; }
}
