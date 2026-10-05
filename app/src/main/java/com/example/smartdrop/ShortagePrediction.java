package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

/** Predicción de desabasto de una zona (ml_engine.ShortagePrediction). */
public class ShortagePrediction {

    @SerializedName("generated_at")
    private String generatedAt;

    @SerializedName("median_hours_to_shortage")
    private Double medianHoursToShortage;

    @SerializedName("p10_hours_to_shortage")
    private Double p10HoursToShortage;

    @SerializedName("p90_hours_to_shortage")
    private Double p90HoursToShortage;

    @SerializedName("probabilidad_desabasto_horizonte")
    private Double probabilidadDesabastoHorizonte;

    @SerializedName("horizonte_horas")
    private Integer horizonteHoras;

    @SerializedName("nivel_riesgo")
    private String nivelRiesgo;

    public String getGeneratedAt() { return generatedAt; }
    public Double getMedianHoursToShortage() { return medianHoursToShortage; }
    public Double getP10HoursToShortage() { return p10HoursToShortage; }
    public Double getP90HoursToShortage() { return p90HoursToShortage; }
    public Double getProbabilidadDesabastoHorizonte() { return probabilidadDesabastoHorizonte; }
    public Integer getHorizonteHoras() { return horizonteHoras; }
    public String getNivelRiesgo() { return nivelRiesgo; }
}
