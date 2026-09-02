package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class PresionData {
    @SerializedName("valor") private double valor;
    @SerializedName("unidad") private String unidad;
    @SerializedName("estado") private String estado;
    @SerializedName("descripcion") private String descripcion;
    @SerializedName("fecha") private String fecha;
    @SerializedName("color") private String color;

    public double getValor() { return valor; }
    public String getUnidad() { return unidad; }
    public String getEstado() { return estado; }
    public String getDescripcion() { return descripcion; }
    public String getFecha() { return fecha; }

    public String getColor() { return color; }
}