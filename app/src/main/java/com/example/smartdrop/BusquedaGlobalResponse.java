package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BusquedaGlobalResponse {
    @SerializedName("ok")
    private boolean ok;
    @SerializedName("secciones")
    private List<Seccion> secciones;

    public boolean isOk() { return ok; }
    public List<Seccion> getSecciones() { return secciones; }

    public static class Seccion {
        @SerializedName("titulo")
        private String titulo;
        @SerializedName("url")
        private String url;
        @SerializedName("contenido")
        private String contenido;

        public String getTitulo() { return titulo; }
        public String getUrl() { return url; }
        public String getContenido() { return contenido; }
    }
}
