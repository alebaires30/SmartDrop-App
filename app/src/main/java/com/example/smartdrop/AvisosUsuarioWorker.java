package com.example.smartdrop;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Revisión periódica en segundo plano de los avisos del usuario (la programa {@link AvisosUsuario#iniciar}). */
public class AvisosUsuarioWorker extends Worker {

    public AvisosUsuarioWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!AvisosUsuario.tieneSesion(context)) return Result.success();
        return AvisosUsuario.revisarBloqueando(context) ? Result.success() : Result.retry();
    }
}
