package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ReporteMensajeResponse {
    @SerializedName("ok") private boolean ok;
    @SerializedName("mensaje") private ReporteMensaje mensaje;
    @SerializedName("error") private String error;

    public boolean isOk() { return ok; }
    public ReporteMensaje getMensaje() { return mensaje; }
    public String getError() { return error; }
}
