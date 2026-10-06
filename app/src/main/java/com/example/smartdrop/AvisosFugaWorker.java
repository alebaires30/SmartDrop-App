package com.example.smartdrop;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Revisión periódica en segundo plano de los avisos de fuga (la programa {@link AvisosFuga#programar}). */
public class AvisosFugaWorker extends Worker {

    public AvisosFugaWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!AvisosFuga.esAdminConSesion(context)) return Result.success();
        return AvisosFuga.revisarBloqueando(context) ? Result.success() : Result.retry();
    }
}
