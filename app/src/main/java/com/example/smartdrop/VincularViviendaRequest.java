package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class VincularViviendaRequest {
    @SerializedName("numero_cuenta") private String numeroCuenta;
    @SerializedName("nombre_completo_titular") private String nombreCompletoTitular;

    public VincularViviendaRequest(String numeroCuenta, String nombreCompletoTitular) {
        this.numeroCuenta = numeroCuenta;
        this.nombreCompletoTitular = nombreCompletoTitular;
    }
}