package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class EstadoAguaResponse {
    @SerializedName("presion") private PresionData presion;
    @SerializedName("calidad") private CalidadData calidad;
    @SerializedName("nivel") private NivelData nivel;
    @SerializedName("resumen_general") private ResumenGeneral resumenGeneral;

    @SerializedName("consumo") private ConsumoResumen consumo;

    public PresionData getPresion() { return presion; }
    public CalidadData getCalidad() { return calidad; }
    public NivelData getNivel() { return nivel; }

    public ResumenGeneral getResumenGeneral() { return resumenGeneral; }

    public ConsumoResumen getConsumo() { return consumo; }

}