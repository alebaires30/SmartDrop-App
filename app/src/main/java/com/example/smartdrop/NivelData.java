package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class NivelData {
    @SerializedName("porcentaje") private double porcentaje;
    @SerializedName("litros_disponibles") private double litrosDisponibles;
    @SerializedName("capacidad_maxima_litros") private double capacidadMaximaLitros;
    @SerializedName("barriles_disponibles") private double barrilesDisponibles;
    @SerializedName("capacidad_maxima_barriles") private double capacidadMaximaBarriles;
    @SerializedName("fecha") private String fecha;
    @SerializedName("bomba_estado") private String bombaEstado;
    @SerializedName("autonomia_estimada_texto") private String autonomiaEstimadaTexto;
    @SerializedName("estado") private String estado;
    @SerializedName("mensaje") private String mensaje;
    @SerializedName("color") private String color;

    public double getPorcentaje() { return porcentaje; }
    public double getLitrosDisponibles() { return litrosDisponibles; }
    public double getCapacidadMaximaLitros() { return capacidadMaximaLitros; }
    public double getBarrilesDisponibles() { return barrilesDisponibles; }
    public double getCapacidadMaximaBarriles() { return capacidadMaximaBarriles; }
    public String getFecha() { return fecha; }
    public String getBombaEstado() { return bombaEstado; }
    public String getAutonomiaEstimadaTexto() { return autonomiaEstimadaTexto; }

    public String getEstado() { return estado; }
    public String getMensaje() { return mensaje; }

    public String getColor() { return color; }
}