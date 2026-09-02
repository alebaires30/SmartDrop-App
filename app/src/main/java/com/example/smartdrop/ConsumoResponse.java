package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ConsumoResponse {
    @SerializedName("periodo") private String periodo;
    @SerializedName("unidad") private String unidad;
    @SerializedName("consumo_total") private double consumoTotal;
    @SerializedName("estado_texto") private String estadoTexto;
    @SerializedName("color") private String color;
    @SerializedName("comparacion") private ComparacionData comparacion;
    @SerializedName("serie") private List<SeriePunto> serie;
    @SerializedName("punto_maximo") private SeriePunto puntoMaximo;

    public String getPeriodo() { return periodo; }
    public String getUnidad() { return unidad; }
    public double getConsumoTotal() { return consumoTotal; }
    public String getEstadoTexto() { return estadoTexto; }
    public String getColor() { return color; }
    public ComparacionData getComparacion() { return comparacion; }
    public List<SeriePunto> getSerie() { return serie; }
    public SeriePunto getPuntoMaximo() { return puntoMaximo; }
}