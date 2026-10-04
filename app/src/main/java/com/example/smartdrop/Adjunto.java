package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class Adjunto {
    @SerializedName("id_adjunto") private int idAdjunto;
    @SerializedName("url") private String url;
    @SerializedName("tipo") private String tipo;
    @SerializedName("nombre") private String nombre;

    public int getIdAdjunto() { return idAdjunto; }
    public String getUrl() { return url; }
    public String getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public boolean esVideo() { return "video".equals(tipo); }
}
