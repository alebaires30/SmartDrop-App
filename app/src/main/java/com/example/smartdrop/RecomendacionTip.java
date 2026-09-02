package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class RecomendacionTip {
    @SerializedName("id") private String id;
    @SerializedName("titulo") private String titulo;
    @SerializedName("impacto") private String impacto;
    @SerializedName("icono_drawable") private String iconoDrawable;

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getImpacto() { return impacto; }
    public String getIconoDrawable() { return iconoDrawable; }
}