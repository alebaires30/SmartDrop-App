package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

/** Estado actual de una zona (GET /v1/ml/zones/{id}/current-status/). */
public class ZonaEstadoResponse {

    @SerializedName("zone_id")
    private int zoneId;

    @SerializedName("zone_name")
    private String zoneName;

    @SerializedName("nivel_actual_litros")
    private double nivelActualLitros;

    @SerializedName("porcentaje_llenado")
    private double porcentajeLlenado;

    @SerializedName("consumo_actual_lph")
    private double consumoActualLph;

    @SerializedName("alert_state")
    private String alertState;

    @SerializedName("ultima_prediccion_desabasto")
    private ShortagePrediction ultimaPrediccionDesabasto;

    @SerializedName("anomalias_activas_24h")
    private int anomaliasActivas24h;

    public int getZoneId() { return zoneId; }
    public String getZoneName() { return zoneName; }
    public double getNivelActualLitros() { return nivelActualLitros; }
    public double getPorcentajeLlenado() { return porcentajeLlenado; }
    public double getConsumoActualLph() { return consumoActualLph; }
    public String getAlertState() { return alertState; }
    public ShortagePrediction getUltimaPrediccionDesabasto() { return ultimaPrediccionDesabasto; }
    public int getAnomaliasActivas24h() { return anomaliasActivas24h; }
}
