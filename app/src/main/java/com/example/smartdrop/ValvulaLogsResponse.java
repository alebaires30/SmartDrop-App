package com.example.smartdrop;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ValvulaLogsResponse {

    @SerializedName("logs")
    private List<ValvulaLogs> logs;

    @SerializedName("advertencia")
    private String advertencia;

    // Getters
    public List<ValvulaLogs> getLogs() { return logs; }
    public String getAdvertencia() { return advertencia; }

    // Setters
    public void setLogs(List<ValvulaLogs> logs) { this.logs = logs; }
    public void setAdvertencia(String advertencia) { this.advertencia = advertencia; }
}


