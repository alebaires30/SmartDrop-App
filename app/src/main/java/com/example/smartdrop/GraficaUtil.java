package com.example.smartdrop;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.List;

/** Estilo compacto y uniforme para las gráficas de la app: pocas etiquetas, sin puntos y líneas suaves. */
final class GraficaUtil {

    private GraficaUtil() { }

    static void estilo(LineChart chart, Context context) {
        int texto = ContextCompat.getColor(context, R.color.text_secondary);
        int rejilla = ContextCompat.getColor(context, R.color.stroke_soft);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setExtraOffsets(4f, 4f, 8f, 6f);
        chart.setNoDataText("Sin datos para mostrar");
        chart.setNoDataTextColor(texto);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setLabelCount(4);
        x.setAvoidFirstLastClipping(true);
        x.setTextColor(texto);
        x.setTextSize(10f);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setTextColor(texto);
        chart.getAxisLeft().setTextSize(10f);
        chart.getAxisLeft().setLabelCount(4, false);
        chart.getAxisLeft().setGridColor(rejilla);
        chart.getAxisLeft().setDrawAxisLine(false);

        Legend leyenda = chart.getLegend();
        leyenda.setTextColor(texto);
        leyenda.setTextSize(11f);
        leyenda.setForm(Legend.LegendForm.LINE);
        leyenda.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        leyenda.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        leyenda.setWordWrapEnabled(true);
    }

    static void etiquetas(LineChart chart, List<String> etiquetas) {
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetas));
    }

    /** Línea principal: suave, sin puntos y con relleno degradado opcional. */
    static LineDataSet linea(List<Entry> datos, String nombre, int color, boolean relleno) {
        LineDataSet set = new LineDataSet(datos, nombre);
        set.setColor(color);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        set.setHighLightColor(color);
        set.setDrawHorizontalHighlightIndicator(false);
        if (relleno) {
            set.setDrawFilled(true);
            GradientDrawable degradado = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{ (color & 0x00FFFFFF) | 0x55000000, (color & 0x00FFFFFF) | 0x05000000 });
            set.setFillDrawable(degradado);
        }
        return set;
    }

    /** Línea secundaria punteada (rangos mínimo/máximo esperados). */
    static LineDataSet lineaPunteada(List<Entry> datos, String nombre, int color) {
        LineDataSet set = linea(datos, nombre, color, false);
        set.setLineWidth(1.2f);
        set.enableDashedLine(10f, 8f, 0f);
        return set;
    }
}
