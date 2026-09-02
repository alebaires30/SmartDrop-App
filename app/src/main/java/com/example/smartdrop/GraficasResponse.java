package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class GraficasResponse {
    @SerializedName("flujo") private ParametroData flujo;
    @SerializedName("presion") private ParametroData presion;
    @SerializedName("nivel") private ParametroData nivel;

    public ParametroData getFlujo() { return flujo; }
    public ParametroData getPresion() { return presion; }
    public ParametroData getNivel() { return nivel; }
}