package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;

public class TemporizadorActivo {
    @SerializedName("duracion_programada") private int duracionProgramada;
    @SerializedName("segundos_restantes") private int segundosRestantes;
    public int getDuracionProgramada() { return duracionProgramada; }
    public int getSegundosRestantes() { return segundosRestantes; }
}