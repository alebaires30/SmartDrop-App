package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MisViviendasResponse {
    @SerializedName("viviendas") private List<ViviendaData> viviendas;
    public List<ViviendaData> getViviendas() { return viviendas; }
}