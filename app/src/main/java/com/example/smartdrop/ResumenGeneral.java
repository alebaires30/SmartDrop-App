package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ResumenGeneral {
    @SerializedName("mensaje") private String mensaje;
    @SerializedName("color") private String color;

    public String getMensaje() { return mensaje; }
    public String getColor() { return color; }
}