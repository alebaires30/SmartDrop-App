package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReporteCatalogoResponse {
    public static class TipoProblema {
        @SerializedName("key") private String key;
        @SerializedName("label") private String label;
        @SerializedName("descripcion") private String descripcion;

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getDescripcion() { return descripcion; }
    }

    @SerializedName("ok") private boolean ok;
    @SerializedName("tipos") private List<TipoProblema> tipos;
    @SerializedName("max_adjuntos") private int maxAdjuntos;

    public boolean isOk() { return ok; }
    public List<TipoProblema> getTipos() { return tipos; }
    public int getMaxAdjuntos() { return maxAdjuntos; }
}
