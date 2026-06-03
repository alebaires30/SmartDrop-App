package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("nombre")
    private String nombre;
    @SerializedName("apellido")
    private String apellido;
    @SerializedName("correo")
    private String correo;
    @SerializedName("contrasena")
    private String contrasena;

    public RegisterRequest() {}

    public RegisterRequest(String nombre, String apellido, String correo, String contrasena) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    public String getNombre() { return nombre; }
    public String getApellido() {return apellido; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
}
