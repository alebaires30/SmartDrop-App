package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("access")
    private String access;

    @SerializedName("refresh")
    private String refresh;

    @SerializedName("id_rol")
    private int idRol;

    @SerializedName("id_usuario")
    private int idUsuario;

    @SerializedName("nombre")
    private String nombre;

    public String getMensaje()  { return mensaje; }
    public String getAccess()   { return access; }
    public String getRefresh()  { return refresh; }
    public int getIdRol()       { return idRol; }
    public int getIdUsuario()   { return idUsuario; }
    public String getNombre()   { return nombre; }
}