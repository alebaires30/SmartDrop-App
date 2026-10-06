package com.example.smartdrop;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Aviso automático de fuga para el administrador (GET /v1/ml/leaks/alerts/). */
public class AlertaFuga {

    @SerializedName("id_alerta")
    private long idAlerta;

    @SerializedName("fecha")
    private String fecha;

    @SerializedName("prioridad")
    private String prioridad;

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("nic")
    private String nic;

    @SerializedName("direccion")
    private String direccion;

    @SerializedName("zona")
    private String zona;

    @SerializedName("titular")
    private String titular;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("probabilidad")
    private Double probabilidad;

    @SerializedName("nivel_riesgo")
    private String nivelRiesgo;

    @SerializedName("metricas")
    private JsonObject metricas;

    @SerializedName("causas")
    private List<String> causas;

    @SerializedName("leida")
    private boolean leida;

    @SerializedName("resumen")
    private String resumen;

    @SerializedName("inicio")
    private String inicio;

    @SerializedName("accion")
    private String accion;

    public long getIdAlerta() { return idAlerta; }
    public String getFecha() { return fecha; }
    public String getPrioridad() { return prioridad; }
    public String getMensaje() { return mensaje; }
    public String getNic() { return nic; }
    public String getDireccion() { return direccion; }
    public String getZona() { return zona; }
    public String getTitular() { return titular; }
    public String getTelefono() { return telefono; }
    public Double getProbabilidad() { return probabilidad; }
    public String getNivelRiesgo() { return nivelRiesgo; }
    public JsonObject getMetricas() { return metricas; }
    public List<String> getCausas() { return causas; }
    public boolean isLeida() { return leida; }
    /** Frase sencilla de qué está pasando (la arma el servidor). */
    public String getResumen() { return resumen; }
    /** Inicio estimado ya formateado ("05/10 12:00"), o null. */
    public String getInicio() { return inicio; }
    public String getAccion() { return accion; }

    /** Pérdida estimada en L/h, o null si el servidor no la envió. */
    public Double getPerdidaEstimadaLph() {
        if (metricas == null || !metricas.has("perdida_estimada_lph") || metricas.get("perdida_estimada_lph").isJsonNull()) {
            return null;
        }
        return metricas.get("perdida_estimada_lph").getAsDouble();
    }
}