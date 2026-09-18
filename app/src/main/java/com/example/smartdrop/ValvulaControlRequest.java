package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ValvulaControlRequest {
    @SerializedName("accion")
    private String accion;

    @SerializedName("duracion")
    private int duracion;

    @SerializedName("usuario")
    private String usuario;

    @SerializedName("origen")
    private String origen;

    public ValvulaControlRequest(String accion, int duracion, String usuario, String origen) {
        this.accion = accion;
        this.duracion = duracion;
        this.usuario = usuario;
        this.origen = origen;
    }

    // Getters and Setters
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public int getDuracion() { return duracion; }
    public void setDuracion(int duracion) { this.duracion = duracion; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
}
