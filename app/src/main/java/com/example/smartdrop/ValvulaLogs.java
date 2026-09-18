package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ValvulaLogs {
    @SerializedName("accion")
    private String accion;

    @SerializedName("detalle")
    private String detalle;

    @SerializedName("fecha_hora")
    private String fechaHora;

    @SerializedName("usuario")
    private String usuario;

    @SerializedName("origen")
    private String origen;

    // Getters
    public String getAccion() { return accion; }
    public String getDetalle() { return detalle; }
    public String getFechaHora() { return fechaHora; }
    public String getUsuario() { return usuario; }
    public String getOrigen() { return origen; }

    // Setters
    public void setAccion(String accion) { this.accion = accion; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public void setOrigen(String origen) { this.origen = origen; }
}
