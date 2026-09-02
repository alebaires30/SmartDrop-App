package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ValorActual {
    @SerializedName("valor") private double valor;
    @SerializedName("unidad") private String unidad;
    @SerializedName("fecha") private String fecha;
    @SerializedName("fuera_de_rango") private boolean fueraDeRango;

    public double getValor() { return valor; }
    public String getUnidad() { return unidad; }
    public String getFecha() { return fecha; }
    public boolean isFueraDeRango() { return fueraDeRango; }
}
