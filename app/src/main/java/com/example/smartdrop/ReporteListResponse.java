package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReporteListResponse {
    @SerializedName("ok") private boolean ok;
    @SerializedName("reportes") private List<Reporte> reportes;
    @SerializedName("error") private String error;

    public boolean isOk() { return ok; }
    public List<Reporte> getReportes() { return reportes; }
    public String getError() { return error; }
}
