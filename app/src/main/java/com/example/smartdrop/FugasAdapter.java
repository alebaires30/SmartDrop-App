package com.example.smartdrop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tarjetas de la pantalla de predicción de fugas: una por vivienda, con el detalle completo si hay posible fuga. */
public class FugasAdapter extends RecyclerView.Adapter<FugasAdapter.ViewHolder> {

    private final List<FugaVivienda> viviendas = new ArrayList<>();

    public void setViviendas(List<FugaVivienda> nuevas) {
        viviendas.clear();
        if (nuevas != null) viviendas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fuga_vivienda, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        FugaVivienda v = viviendas.get(position);
        Context context = h.itemView.getContext();

        h.tvNic.setText(v.getNic());
        String ubicacion = v.getDireccion() == null ? "" : v.getDireccion();
        if (v.getZona() != null && !v.getZona().isEmpty()) ubicacion += (ubicacion.isEmpty() ? "" : " · ") + v.getZona();
        h.tvUbicacion.setText(ubicacion);

        String riesgo = v.getNivelRiesgo() == null ? "sin_datos" : v.getNivelRiesgo();
        int color;
        String etiqueta;
        if (v.isPosibleFuga()) {
            etiqueta = "POSIBLE FUGA";
            color = R.color.text_alert;
        } else if (riesgo.equals("alto") || riesgo.equals("medio")) {
            etiqueta = "RIESGO MEDIO";
            color = R.color.status_orange;
        } else if (riesgo.equals("bajo")) {
            etiqueta = "SIN FUGA";
            color = R.color.status_green;
        } else {
            etiqueta = "SIN ANÁLISIS";
            color = R.color.text_secondary;
        }
        h.tvRiesgo.setText(etiqueta);
        h.tvRiesgo.setTextColor(ContextCompat.getColor(context, color));

        if (v.getPorcentaje() == null) {
            h.progreso.setProgress(0);
            h.tvProbabilidad.setText("—");
        } else {
            h.progreso.setProgress((int) Math.round(v.getPorcentaje()));
            h.tvProbabilidad.setText(String.format(Locale.getDefault(), "%.0f %%", v.getPorcentaje()));
        }

        String nota = notaDatos(v);
        if (nota.isEmpty() && v.getEvaluado() != null) {
            String fecha = PrediccionFugasActivity.fechaCorta(v.getEvaluado());
            if (!fecha.isEmpty()) nota = "Analizada el " + fecha;
        }
        h.tvNota.setVisibility(nota.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvNota.setText(nota);

        if (v.isPosibleFuga()) {
            h.tvDetalle.setVisibility(View.VISIBLE);
            h.tvDetalle.setText(detalle(v));
        } else {
            h.tvDetalle.setVisibility(View.GONE);
        }
    }

    private static String notaDatos(FugaVivienda v) {
        String calidad = v.getCalidadDatos() == null ? "" : v.getCalidadDatos();
        String nota;
        switch (calidad) {
            case "plana": nota = "Lecturas constantes: no hay patrón que analizar"; break;
            case "insuficiente": nota = "Historial insuficiente"; break;
            case "sin_lecturas_recientes": nota = "El sensor no está reportando"; break;
            case "sin_datos": nota = "Sin lecturas sincronizadas"; break;
            default: return "";
        }
        if (v.getDetalleDatos() != null && !v.getDetalleDatos().isEmpty()) nota += "\n" + v.getDetalleDatos();
        return nota;
    }

    private static String detalle(FugaVivienda v) {
        StringBuilder texto = new StringBuilder();
        if (v.getCausas() != null && !v.getCausas().isEmpty()) {
            texto.append("Causas detectadas:");
            for (String causa : v.getCausas()) texto.append("\n• ").append(causa);
            texto.append("\n\n");
        }
        JsonObject m = v.getMetricas() == null ? new JsonObject() : v.getMetricas();
        String unidad = m.has("unidad_presion") && !m.get("unidad_presion").isJsonNull()
                ? m.get("unidad_presion").getAsString() : "";
        texto.append("Flujo mínimo (6 h): ").append(valor(m, "flujo_minimo_6h_lpm", "L/min", 3))
                .append(" (normal ").append(valor(m, "flujo_minimo_normal_lpm", "L/min", 3)).append(")");
        texto.append("\nPérdida estimada: ").append(valor(m, "perdida_estimada_lph", "L/h", 1));
        texto.append("\nAgua perdida estimada: ").append(valor(m, "litros_perdidos_estimados", "L", 1));
        String inicio = m.has("inicio_estimado") && !m.get("inicio_estimado").isJsonNull()
                ? PrediccionFugasActivity.fechaCorta(m.get("inicio_estimado").getAsString()) : "";
        texto.append("\nInicio estimado: ").append(inicio.isEmpty() ? "no determinado" : inicio);
        texto.append("\nPresión: ").append(valor(m, "presion_actual", unidad, 2))
                .append(" (normal ").append(valor(m, "presion_normal", unidad, 2))
                .append(", caída ").append(valor(m, "caida_presion", unidad, 2)).append(")");
        texto.append("\nTanque: ").append(valor(m, "nivel_tanque_litros", "L", 1))
                .append(" (").append(valor(m, "nivel_tanque_pct", "%", 0)).append(")");
        texto.append("\n\nTitular: ").append(textoO(v.getTitular())).append(" · Tel.: ").append(textoO(v.getTelefono()));
        return texto.toString();
    }

    private static String textoO(String valor) {
        return valor == null || valor.isEmpty() ? "sin dato" : valor;
    }

    private static String valor(JsonObject m, String clave, String unidad, int decimales) {
        if (!m.has(clave) || m.get(clave).isJsonNull()) return "sin dato";
        String numero = String.format(Locale.getDefault(), "%." + decimales + "f", m.get(clave).getAsDouble());
        return unidad == null || unidad.isEmpty() ? numero : numero + " " + unidad;
    }

    @Override
    public int getItemCount() {
        return viviendas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNic, tvRiesgo, tvUbicacion, tvProbabilidad, tvNota, tvDetalle;
        ProgressBar progreso;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNic = itemView.findViewById(R.id.tvFugaNic);
            tvRiesgo = itemView.findViewById(R.id.tvFugaRiesgo);
            tvUbicacion = itemView.findViewById(R.id.tvFugaUbicacion);
            progreso = itemView.findViewById(R.id.progresoFuga);
            tvProbabilidad = itemView.findViewById(R.id.tvFugaProbabilidad);
            tvNota = itemView.findViewById(R.id.tvFugaNota);
            tvDetalle = itemView.findViewById(R.id.tvFugaDetalle);
        }
    }
}
