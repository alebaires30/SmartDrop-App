package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class RetroalimentacionResponse {
    @SerializedName("consumo_hoy_litros") private double consumoHoyLitros;
    @SerializedName("promedio_habitual_litros") private Double promedioHabitualLitros;
    @SerializedName("estado") private String estado;
    @SerializedName("color") private String color;
    @SerializedName("mensaje") private String mensaje;
    @SerializedName("comparacion_mes") private ComparacionData comparacionMes;
    @SerializedName("racha_dias") private int rachaDias;
    @SerializedName("racha_texto") private String rachaTexto;

    public double getConsumoHoyLitros() { return consumoHoyLitros; }
    public Double getPromedioHabitualLitros() { return promedioHabitualLitros; }
    public String getEstado() { return estado; }
    public String getColor() { return color; }
    public String getMensaje() { return mensaje; }
    public ComparacionData getComparacionMes() { return comparacionMes; }
    public int getRachaDias() { return rachaDias; }
    public String getRachaTexto() { return rachaTexto; }
}