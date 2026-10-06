package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ValorActual {
    @SerializedName("valor") private double valor;
    @SerializedName("unidad") private String unidad;
    @SerializedName("fecha") private String fecha;
    @SerializedName("fuera_de_rango") private boolean fueraDeRango;
    /** "Alta", "Baja" o "Normal" según los límites del sensor. */
    @SerializedName("estado") private String estado;
    @SerializedName("rango_min") private Double rangoMin;
    @SerializedName("rango_max") private Double rangoMax;
    /** NIC de la vivienda que registró la lectura (puede venir vacío). */
    @SerializedName("nic") private String nic;

    public double getValor() { return valor; }
    public String getUnidad() { return unidad; }
    public String getFecha() { return fecha; }
    public boolean isFueraDeRango() { return fueraDeRango; }
    public String getEstado() { return estado; }
    public Double getRangoMin() { return rangoMin; }
    public Double getRangoMax() { return rangoMax; }
    public String getNic() { return nic; }
}
