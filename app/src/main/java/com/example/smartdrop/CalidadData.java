package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class CalidadData {
    @SerializedName("valor_ppm") private double valorPpm;
    @SerializedName("estrellas") private int estrellas;
    @SerializedName("estado") private String estado;
    @SerializedName("descripcion") private String descripcion;
    @SerializedName("texto_anomalias") private String textoAnomalias;
    @SerializedName("fecha") private String fecha;
    @SerializedName("color") private String color;

    public double getValorPpm() { return valorPpm; }
    public int getEstrellas() { return estrellas; }
    public String getEstado() { return estado; }
    public String getDescripcion() { return descripcion; }
    public String getTextoAnomalias() { return textoAnomalias; }
    public String getFecha() { return fecha; }

    public String getColor() { return color; }
}