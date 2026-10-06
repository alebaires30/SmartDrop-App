package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Nivel proyectado del tanque (GET /v1/ml/zones/{id}/tank-trajectory/). */
public class TrayectoriaTanqueResponse {

    @SerializedName("trajectory")
    private List<Punto> trayectoria;

    public List<Punto> getTrayectoria() { return trayectoria; }

    public static class Punto {
        @SerializedName("target_ts") private String fecha;
        @SerializedName("p10_litros") private double minimo;
        @SerializedName("p50_litros") private double esperado;
        @SerializedName("p90_litros") private double maximo;

        public String getFecha() { return fecha; }
        public double getMinimo() { return minimo; }
        public double getEsperado() { return esperado; }
        public double getMaximo() { return maximo; }
    }
}
