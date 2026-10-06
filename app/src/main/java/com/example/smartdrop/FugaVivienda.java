package com.example.smartdrop;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Predicción de fuga de una vivienda (GET /v1/ml/leaks/). */
public class FugaVivienda {

    @SerializedName("home_id")
    private int homeId;

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

    @SerializedName("calidad_datos")
    private String calidadDatos;

    @SerializedName("detalle_datos")
    private String detalleDatos;

    @SerializedName("probabilidad")
    private Double probabilidad;

    @SerializedName("porcentaje")
    private Double porcentaje;

    @SerializedName("nivel_riesgo")
    private String nivelRiesgo;

    @SerializedName("posible_fuga")
    private boolean posibleFuga;

    @SerializedName("causas")
    private List<String> causas;

    @SerializedName("metricas")
    private JsonObject metricas;

    @SerializedName("evaluado")
    private String evaluado;

    public int getHomeId() { return homeId; }
    public String getNic() { return nic; }
    public String getDireccion() { return direccion; }
    public String getZona() { return zona; }
    public String getTitular() { return titular; }
    public String getTelefono() { return telefono; }
    public String getCalidadDatos() { return calidadDatos; }
    public String getDetalleDatos() { return detalleDatos; }
    public Double getProbabilidad() { return probabilidad; }
    public Double getPorcentaje() { return porcentaje; }
    public String getNivelRiesgo() { return nivelRiesgo; }
    public boolean isPosibleFuga() { return posibleFuga; }
    public List<String> getCausas() { return causas; }
    public JsonObject getMetricas() { return metricas; }
    public String getEvaluado() { return evaluado; }
}