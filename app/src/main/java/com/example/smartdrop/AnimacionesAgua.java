package com.example.smartdrop;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Movimiento del tema agua (igual que la web): burbujas que suben, olas que se mecen, tarjetas que
 * aparecen y el agua que sube hasta el nivel del tanque.
 *
 * Si el usuario quitó las animaciones del sistema (Accesibilidad → Quitar animaciones) no se mueve
 * nada: todo queda en su lugar final. Las animaciones en bucle se cancelan cuando la vista sale de
 * la pantalla, para no retener la actividad.
 */
public final class AnimacionesAgua {

    private AnimacionesAgua() {
    }

    /** false si el sistema tiene las animaciones desactivadas. */
    public static boolean activas(Context context) {
        float escala = Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return escala > 0f;
    }

    /** Cada hijo del contenedor es una burbuja: sube desde abajo y se desvanece, en bucle. */
    public static void burbujas(ViewGroup contenedor) {
        if (contenedor == null) return;
        if (!activas(contenedor.getContext())) {
            contenedor.setVisibility(View.GONE);
            return;
        }
        contenedor.post(() -> {
            float recorrido = contenedor.getHeight() * 0.75f;
            List<Animator> enCurso = new ArrayList<>();
            for (int i = 0; i < contenedor.getChildCount(); i++) {
                View burbuja = contenedor.getChildAt(i);
                burbuja.setAlpha(0f);
                ObjectAnimator subir = ObjectAnimator.ofFloat(burbuja, View.TRANSLATION_Y, 0f, -recorrido);
                ObjectAnimator brillo = ObjectAnimator.ofFloat(burbuja, View.ALPHA, 0f, 1f, 0.9f, 0f);
                subir.setRepeatCount(ValueAnimator.INFINITE);
                brillo.setRepeatCount(ValueAnimator.INFINITE);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(subir, brillo);
                set.setDuration(7000L + (i * 1700L) % 5000L);
                set.setStartDelay(i * 1100L);
                set.setInterpolator(new LinearInterpolator());
                set.start();
                enCurso.add(set);
            }
            cancelarAlSalir(contenedor, enCurso);
        });
    }

    /** Las olas se mecen de lado, despacio. La vista debe sobresalir unos dp por cada lado. */
    public static void olas(View olas) {
        if (olas == null || !activas(olas.getContext())) return;
        float distancia = dp(olas.getContext(), 28);
        ObjectAnimator vaiven = ObjectAnimator.ofFloat(olas, View.TRANSLATION_X, 0f, -distancia);
        vaiven.setDuration(5200);
        vaiven.setRepeatMode(ValueAnimator.REVERSE);
        vaiven.setRepeatCount(ValueAnimator.INFINITE);
        vaiven.setInterpolator(new AccelerateDecelerateInterpolator());
        vaiven.start();
        List<Animator> enCurso = new ArrayList<>();
        enCurso.add(vaiven);
        cancelarAlSalir(olas, enCurso);
    }

    /** Flota arriba y abajo suavemente (el tanque sobre la escena). */
    public static void flotar(View vista) {
        if (vista == null || !activas(vista.getContext())) return;
        ObjectAnimator flotar = ObjectAnimator.ofFloat(vista, View.TRANSLATION_Y, 0f, -dp(vista.getContext(), 6));
        flotar.setDuration(3000);
        flotar.setRepeatMode(ValueAnimator.REVERSE);
        flotar.setRepeatCount(ValueAnimator.INFINITE);
        flotar.setInterpolator(new AccelerateDecelerateInterpolator());
        flotar.start();
        List<Animator> enCurso = new ArrayList<>();
        enCurso.add(flotar);
        cancelarAlSalir(vista, enCurso);
    }

    /** Aparece subiendo y fundiéndose. */
    public static void entrada(View vista, long retrasoMs) {
        if (vista == null || !activas(vista.getContext())) return;
        vista.setAlpha(0f);
        vista.setTranslationY(dp(vista.getContext(), 24));
        vista.animate().alpha(1f).translationY(0f).setStartDelay(retrasoMs).setDuration(520)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    /** Solo sube a su lugar (para vistas cuya transparencia maneja otra parte del código). */
    public static void subir(View vista, long retrasoMs) {
        if (vista == null || !activas(vista.getContext())) return;
        vista.setTranslationY(dp(vista.getContext(), 28));
        vista.animate().translationY(0f).setStartDelay(retrasoMs).setDuration(520)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    /**
     * Sube el nivel de una imagen con recorte (por ejemplo la columna de columna_agua_nivel.xml)
     * desde su nivel actual hasta el destino (0–10000), como si se llenara de agua.
     */
    public static void llenar(ImageView imagen, int nivelDestino) {
        if (imagen == null || imagen.getDrawable() == null) return;
        int destino = Math.max(0, Math.min(10000, nivelDestino));
        int desde = imagen.getDrawable().getLevel();
        if (!activas(imagen.getContext()) || desde == destino) {
            imagen.setImageLevel(destino);
            return;
        }
        ValueAnimator llenar = ValueAnimator.ofInt(desde, destino);
        llenar.setDuration(1100);
        llenar.setInterpolator(new DecelerateInterpolator());
        llenar.addUpdateListener(a -> imagen.setImageLevel((int) a.getAnimatedValue()));
        llenar.start();
    }

    /** Llena una barra de progreso (por ejemplo la del nivel del tanque) hasta el valor, de a poco. */
    public static void progreso(ProgressBar barra, int valor) {
        if (barra == null) return;
        int destino = Math.max(0, Math.min(barra.getMax(), valor));
        if (!activas(barra.getContext()) || barra.getProgress() == destino) {
            barra.setProgress(destino);
            return;
        }
        ObjectAnimator llenar = ObjectAnimator.ofInt(barra, "progress", barra.getProgress(), destino);
        llenar.setDuration(1100);
        llenar.setInterpolator(new DecelerateInterpolator());
        llenar.start();
    }

    private static void cancelarAlSalir(View vista, List<Animator> animaciones) {
        vista.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                for (Animator a : animaciones) a.cancel();
                v.removeOnAttachStateChangeListener(this);
            }
        });
    }

    private static float dp(Context context, int valor) {
        return valor * context.getResources().getDisplayMetrics().density;
    }
}
