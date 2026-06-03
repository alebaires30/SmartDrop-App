package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("correo")
    private String correo;

    @SerializedName("contrasena")
    private String contrasena;

    public LoginRequest() {}

    public LoginRequest(String correo, String contrasena) {
        this.correo = correo;
        this.contrasena = contrasena;
    }

    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
}