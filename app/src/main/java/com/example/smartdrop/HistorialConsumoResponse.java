package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Consumo por hora de las últimas 48 h y pronóstico de la próxima hora (GET /v1/ml/zones/{id}/consumption-history/). */
public class HistorialConsumoResponse {

    @SerializedName("historial")
    private List<Hora> historial;

    @SerializedName("pronostico")
    private Pronostico pronostico;

    public List<Hora> getHistorial() { return historial; }
    public Pronostico getPronostico() { return pronostico; }

    public static class Hora {
        @SerializedName("ts") private String fecha;
        @SerializedName("litros") private double litros;

        public String getFecha() { return fecha; }
        public double getLitros() { return litros; }
    }

    public static class Pronostico {
        @SerializedName("ts") private String fecha;
        @SerializedName("p10") private double minimo;
        @SerializedName("p50") private double esperado;
        @SerializedName("p90") private double maximo;

        public String getFecha() { return fecha; }
        public double getMinimo() { return minimo; }
        public double getEsperado() { return esperado; }
        public double getMaximo() { return maximo; }
    }
}
