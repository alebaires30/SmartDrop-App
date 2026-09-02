package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RecomendacionesResponse {
    @SerializedName("saludo") private String saludo;
    @SerializedName("tips") private List<RecomendacionTip> tips;
    public String getSaludo() { return saludo; }
    public List<RecomendacionTip> getTips() { return tips; }
}