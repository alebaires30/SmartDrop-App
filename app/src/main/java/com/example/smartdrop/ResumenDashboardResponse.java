package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ResumenDashboardResponse {
    @SerializedName("flujo") private ValorActual flujo;
    @SerializedName("presion") private ValorActual presion;
    @SerializedName("nivel") private ValorActual nivel;
    @SerializedName("total_alertas") private int totalAlertas;

    public ValorActual getFlujo() { return flujo; }
    public ValorActual getPresion() { return presion; }
    public ValorActual getNivel() { return nivel; }
    public int getTotalAlertas() { return totalAlertas; }
}