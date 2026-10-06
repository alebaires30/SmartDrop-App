package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Respuesta de GET /v1/ml/leaks/. */
public class FugasResponse {

    public static class Monitor {
        @SerializedName("ultimo_ciclo")
        private String ultimoCiclo;

        @SerializedName("estado")
        private String estado;

        @SerializedName("cada_minutos")
        private double cadaMinutos;

        public String getUltimoCiclo() { return ultimoCiclo; }
        public String getEstado() { return estado; }
        public double getCadaMinutos() { return cadaMinutos; }
    }

    @SerializedName("umbral_alerta")
    private double umbralAlerta;

    @SerializedName("posibles_fugas")
    private int posiblesFugas;

    @SerializedName("monitor")
    private Monitor monitor;

    @SerializedName("viviendas")
    private List<FugaVivienda> viviendas;

    public double getUmbralAlerta() { return umbralAlerta; }
    public int getPosiblesFugas() { return posiblesFugas; }
    public Monitor getMonitor() { return monitor; }
    public List<FugaVivienda> getViviendas() { return viviendas; }
}