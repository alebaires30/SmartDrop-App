package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReporteDetalleResponse {
    @SerializedName("ok") private boolean ok;
    @SerializedName("reporte") private Reporte reporte;
    @SerializedName("mensajes") private List<ReporteMensaje> mensajes;
    @SerializedName("error") private String error;

    public boolean isOk() { return ok; }
    public Reporte getReporte() { return reporte; }
    public List<ReporteMensaje> getMensajes() { return mensajes; }
    public String getError() { return error; }
}
