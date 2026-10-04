package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Reporte {
    @SerializedName("id_reporte") private int idReporte;
    @SerializedName("tipo_problema") private String tipoProblema;
    @SerializedName("tipo_label") private String tipoLabel;
    @SerializedName("descripcion") private String descripcion;
    @SerializedName("ubicacion") private String ubicacion;
    @SerializedName("estado") private String estado;
    @SerializedName("estado_label") private String estadoLabel;
    @SerializedName("prioridad") private String prioridad;
    @SerializedName("fecha_reporte") private String fechaReporte;
    @SerializedName("respuesta_admin") private String respuestaAdmin;
    @SerializedName("autor_nombre") private String autorNombre;
    @SerializedName("n_adjuntos") private int nAdjuntos;
    @SerializedName("adjuntos") private List<Adjunto> adjuntos;

    public int getIdReporte() { return idReporte; }
    public String getTipoProblema() { return tipoProblema; }
    public String getTipoLabel() { return tipoLabel != null ? tipoLabel : tipoProblema; }
    public String getDescripcion() { return descripcion; }
    public String getUbicacion() { return ubicacion; }
    public String getEstado() { return estado; }
    public String getEstadoLabel() { return estadoLabel != null ? estadoLabel : estado; }
    public String getPrioridad() { return prioridad; }
    public String getFechaReporte() { return fechaReporte; }
    public String getRespuestaAdmin() { return respuestaAdmin; }
    public String getAutorNombre() { return autorNombre; }
    public int getNAdjuntos() { return nAdjuntos; }
    public List<Adjunto> getAdjuntos() { return adjuntos; }
}
