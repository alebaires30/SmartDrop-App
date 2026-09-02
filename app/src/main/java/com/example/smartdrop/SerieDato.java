package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class SerieDato {
    @SerializedName("fecha") private String fecha;
    @SerializedName("valor") private double valor;
    @SerializedName("fuera_de_rango") private boolean fueraDeRango;

    public String getFecha() { return fecha; }
    public double getValor() { return valor; }
    public boolean isFueraDeRango() { return fueraDeRango; }
}