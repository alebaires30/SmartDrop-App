package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

/** Resumen agregado por zona (GET /v1/ml/zones/summary/). */
public class ZonaResumen {

    @SerializedName("zone_id")
    private int zoneId;

    @SerializedName("zone_name")
    private String zoneName;

    @SerializedName("n_homes")
    private int nHomes;

    @SerializedName("nivel_actual_litros")
    private double nivelActualLitros;

    @SerializedName("porcentaje_llenado")
    private double porcentajeLlenado;

    @SerializedName("consumo_total_lph")
    private double consumoTotalLph;

    @SerializedName("nivel_riesgo")
    private String nivelRiesgo;

    @SerializedName("anomalias_activas_24h")
    private int anomaliasActivas24h;

    @SerializedName("nic")
    private String nic;

    @SerializedName("zona")
    private String zona;

    @SerializedName("direccion")
    private String direccion;

    @SerializedName("calidad_datos")
    private String calidadDatos;

    @SerializedName("probabilidad_desabasto")
    private Double probabilidadDesabasto;

    @SerializedName("horas_hasta_desabasto")
    private Double horasHastaDesabasto;

    @SerializedName("probabilidad_fuga")
    private Double probabilidadFuga;

    public int getZoneId() { return zoneId; }
    public String getZoneName() { return zoneName; }
    public int getNHomes() { return nHomes; }
    public double getNivelActualLitros() { return nivelActualLitros; }
    public double getPorcentajeLlenado() { return porcentajeLlenado; }
    public double getConsumoTotalLph() { return consumoTotalLph; }
    public String getNivelRiesgo() { return nivelRiesgo; }
    public int getAnomaliasActivas24h() { return anomaliasActivas24h; }
    public String getNic() { return nic; }
    public String getZona() { return zona; }
    public String getDireccion() { return direccion; }
    public String getCalidadDatos() { return calidadDatos; }
    public Double getProbabilidadDesabasto() { return probabilidadDesabasto; }
    public Double getHorasHastaDesabasto() { return horasHastaDesabasto; }
    public Double getProbabilidadFuga() { return probabilidadFuga; }
}
