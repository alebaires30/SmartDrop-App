package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

public class ViviendaData {
    @SerializedName("id_vivienda") private int idVivienda;
    @SerializedName("nic") private String nic;
    @SerializedName("direccion") private String direccion;
    @SerializedName("zona") private String zona;
    @SerializedName("codigo_medidor") private String codigoMedidor;
    @SerializedName("tipo_establecimiento") private String tipoEstablecimiento;
    @SerializedName("nombre_completo_titular") private String nombreCompletoTitular;

    public int getIdVivienda() { return idVivienda; }
    public String getNic() { return nic; }
    public String getDireccion() { return direccion; }
    public String getZona() { return zona; }
    public String getCodigoMedidor() { return codigoMedidor; }
    public String getTipoEstablecimiento() { return tipoEstablecimiento; }
    public String getNombreCompletoTitular() { return nombreCompletoTitular; }
}