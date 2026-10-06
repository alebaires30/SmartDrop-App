package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Respuesta de GET /v1/ml/leaks/alerts/. */
public class AlertasFugaResponse {

    @SerializedName("no_leidas")
    private int noLeidas;

    @SerializedName("alertas")
    private List<AlertaFuga> alertas;

    public int getNoLeidas() { return noLeidas; }
    public List<AlertaFuga> getAlertas() { return alertas; }
}