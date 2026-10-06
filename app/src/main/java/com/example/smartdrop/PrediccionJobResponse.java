package com.example.smartdrop;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/** Estado de una ejecución de "Realizar predicciones" (POST/GET /v1/ml/predictions/...). */
public class PrediccionJobResponse {

    @SerializedName("job_id")
    private String jobId;

    @SerializedName("status")
    private String status;

    @SerializedName("step")
    private String step;

    @SerializedName("progress")
    private int progress;

    @SerializedName("error")
    private String error;

    @SerializedName("result")
    private JsonObject result;

    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public String getStep() { return step; }
    public int getProgress() { return progress; }
    public String getError() { return error; }
    public JsonObject getResult() { return result; }

    public boolean isRunning() { return "running".equals(status); }
    public boolean isDone() { return "done".equals(status); }
}