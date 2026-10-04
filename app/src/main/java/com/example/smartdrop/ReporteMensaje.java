package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ReporteMensaje {
    @SerializedName("id_mensaje") private int idMensaje;
    @SerializedName("mensaje") private String mensaje;
    @SerializedName("fecha_envio") private String fechaEnvio;
    @SerializedName("propio") private boolean propio;
    @SerializedName("es_admin_emisor") private boolean esAdminEmisor;
    @SerializedName("autor_nombre") private String autorNombre;

    public int getIdMensaje() { return idMensaje; }
    public String getMensaje() { return mensaje; }
    public String getFechaEnvio() { return fechaEnvio; }
    public boolean isPropio() { return propio; }
    public boolean isEsAdminEmisor() { return esAdminEmisor; }
    public String getAutorNombre() { return autorNombre; }
}
