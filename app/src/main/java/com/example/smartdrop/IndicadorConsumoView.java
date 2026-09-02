package com.example.smartdrop;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** Gota que se "llena" según el consumo — dibujada con Path, no es una imagen estática. */
public class IndicadorConsumoView extends View {

    private double porcentajeLlenado = 0;
    private int colorRelleno = Color.parseColor("#7B2FBE");

    public IndicadorConsumoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void actualizar(double porcentajeLlenado, String severidad) {
        this.porcentajeLlenado = Math.max(0, Math.min(100, porcentajeLlenado));
        this.colorRelleno = ColorSeveridad.colorDe(severidad);
        invalidate();
    }

    private Path construirRutaGota(float w, float h) {
        Path path = new Path();
        float cx = w / 2f;
        float radio = w / 2f;
        float centroCirculoY = h - radio;

        path.moveTo(cx, 0);
        path.cubicTo(cx + radio * 1.1f, h * 0.35f, cx + radio, centroCirculoY - radio * 0.6f, cx + radio, centroCirculoY);
        path.arcTo(new RectF(cx - radio, centroCirculoY - radio, cx + radio, centroCirculoY + radio), 0, 180, false);
        path.cubicTo(cx - radio, centroCirculoY - radio * 0.6f, cx - radio * 1.1f, h * 0.35f, cx, 0);
        path.close();
        return path;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        Path gota = construirRutaGota(w, h);

        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(Color.parseColor("#EDE7F6"));
        canvas.drawPath(gota, fondo);

        float alturaLlenado = h * (float) (porcentajeLlenado / 100.0);
        Path pathRelleno = new Path();
        pathRelleno.addRect(new RectF(0, h - alturaLlenado, w, h), Path.Direction.CW);

        Path resultado = new Path();
        resultado.op(gota, pathRelleno, Path.Op.INTERSECT);

        Paint pinturaRelleno = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinturaRelleno.setColor(colorRelleno);
        canvas.drawPath(resultado, pinturaRelleno);

        Paint contorno = new Paint(Paint.ANTI_ALIAS_FLAG);
        contorno.setStyle(Paint.Style.STROKE);
        contorno.setStrokeWidth(6f);
        contorno.setColor(Color.parseColor("#7B2FBE"));
        canvas.drawPath(gota, contorno);
    }
}