package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ParametroData {
    @SerializedName("unidad") private String unidad;
    @SerializedName("rango_min") private Double rangoMin;
    @SerializedName("rango_max") private Double rangoMax;
    @SerializedName("datos") private List<SerieDato> datos;

    public String getUnidad() { return unidad; }
    public Double getRangoMin() { return rangoMin; }
    public Double getRangoMax() { return rangoMax; }
    public List<SerieDato> getDatos() { return datos; }
}