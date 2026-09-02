package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class ConsumoResumen {
    @SerializedName("litros_hoy") private double litrosHoy;
    public double getLitrosHoy() { return litrosHoy; }
}