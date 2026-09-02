package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class ComparacionData {
    @SerializedName("porcentaje") private Double porcentaje;
    @SerializedName("texto") private String texto;
    public Double getPorcentaje() { return porcentaje; }
    public String getTexto() { return texto; }
}