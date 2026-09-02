package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class VincularViviendaResponse {
    @SerializedName("mensaje") private String mensaje;
    @SerializedName("error") private String error;
    @SerializedName("vivienda") private ViviendaData vivienda;

    public String getMensaje() { return mensaje; }
    public String getError() { return error; }
    public ViviendaData getVivienda() { return vivienda; }
}