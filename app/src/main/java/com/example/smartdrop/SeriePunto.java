package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class SeriePunto {
    @SerializedName("fecha") private String fecha;
    @SerializedName("litros") private double litros;
    public String getFecha() { return fecha; }
    public double getLitros() { return litros; }
}