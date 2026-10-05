package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Respuesta de GET /v1/ml/zones/summary/. */
public class ZonasSummaryResponse {

    @SerializedName("zonas")
    private List<ZonaResumen> zonas;

    public List<ZonaResumen> getZonas() { return zonas; }
}
