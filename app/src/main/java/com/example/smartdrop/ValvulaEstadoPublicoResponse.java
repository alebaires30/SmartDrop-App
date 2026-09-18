package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ValvulaEstadoPublicoResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("valvula")
    private ValvulaData valvula;

    public boolean isOk() { return ok; }
    public ValvulaData getValvula() { return valvula; }

    public static class ValvulaData {
        @SerializedName("id_valvula")
        private int idValvula;
        @SerializedName("nombre")
        private String nombre;
        @SerializedName("estado")
        private String estado;
        @SerializedName("clase")
        private String clase;
        @SerializedName("ultima_actualizacion")
        private String ultimaActualizacion;

        public int getIdValvula() { return idValvula; }
        public String getNombre() { return nombre; }
        public String getEstado() { return estado; }
        public String getClase() { return clase; }
        public String getUltimaActualizacion() { return ultimaActualizacion; }
    }
}
