package com.example.smartdrop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adapter de tarjetas de zona para la pantalla de predicción de suministro. */
public class ZonasPrediccionAdapter extends RecyclerView.Adapter<ZonasPrediccionAdapter.ViewHolder> {

    public interface OnZonaClickListener {
        void onZonaClick(ZonaResumen zona);
    }

    private final List<ZonaResumen> zonas = new ArrayList<>();
    private final OnZonaClickListener listener;

    public ZonasPrediccionAdapter(OnZonaClickListener listener) {
        this.listener = listener;
    }

    public void setZonas(List<ZonaResumen> nuevas) {
        zonas.clear();
        if (nuevas != null) zonas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_zona_prediccion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ZonaResumen z = zonas.get(position);

        h.tvNombre.setText(z.getNic() != null && !z.getNic().isEmpty()
                ? z.getZoneName() + " · " + z.getNic() : z.getZoneName());
        h.tvNivel.setText(String.format(Locale.getDefault(),
                "Nivel: %.0f L (%.1f%%)", z.getNivelActualLitros(), z.getPorcentajeLlenado()));
        h.progreso.setProgress((int) Math.round(z.getPorcentajeLlenado()));
        String ubicacion = z.getDireccion() != null && !z.getDireccion().isEmpty() ? z.getDireccion() : "";
        if (z.getZona() != null && !z.getZona().isEmpty()) ubicacion += (ubicacion.isEmpty() ? "" : " · ") + z.getZona();
        h.tvConsumo.setText(ubicacion.isEmpty()
                ? String.format(Locale.getDefault(), "Consumo: %.2f L/h", z.getConsumoTotalLph())
                : ubicacion + String.format(Locale.getDefault(), "%nConsumo: %.2f L/h", z.getConsumoTotalLph()));

        if (z.getAnomaliasActivas24h() > 0) {
            h.tvAnomalias.setText("Posible fuga");
            h.tvAnomalias.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_alert));
        } else {
            h.tvAnomalias.setText("Sin fugas");
            h.tvAnomalias.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_secondary));
        }

        String riesgo = z.getNivelRiesgo() == null ? "sin_datos" : z.getNivelRiesgo();
        switch (riesgo) {
            case "critico":
                h.tvRiesgo.setText("CRÍTICO");
                h.tvRiesgo.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_alert));
                break;
            case "alto":
                h.tvRiesgo.setText("RIESGO ALTO");
                h.tvRiesgo.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.status_orange));
                break;
            case "medio":
            case "moderado":
                h.tvRiesgo.setText("RIESGO MEDIO");
                h.tvRiesgo.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.status_yellow));
                break;
            case "bajo":
                h.tvRiesgo.setText("RIESGO BAJO");
                h.tvRiesgo.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.status_green));
                break;
            default:
                h.tvRiesgo.setText("SIN DATOS");
                h.tvRiesgo.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_secondary));
                break;
        }

        String detalle = textoPrediccion(z);
        h.tvPrediccion.setVisibility(detalle.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvPrediccion.setText(detalle);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onZonaClick(z);
        });
    }

    private static String textoPrediccion(ZonaResumen z) {
        String nota = "";
        String calidad = z.getCalidadDatos();
        if ("plana".equals(calidad)) nota = "Lecturas constantes: no hay patrón que analizar";
        else if ("insuficiente".equals(calidad)) nota = "Historial insuficiente";
        else if ("sin_lecturas_recientes".equals(calidad)) nota = "El sensor no está reportando";
        else if ("sin_datos".equals(calidad)) nota = "Sin lecturas sincronizadas";
        else if ("sin_tanque".equals(calidad)) nota = "Vivienda sin tanque registrado";
        if (!nota.isEmpty()) return nota;

        String riesgo = z.getNivelRiesgo() == null ? "" : z.getNivelRiesgo();
        if (riesgo.equals("critico")) return "El tanque ya está en nivel crítico";
        if (z.getHorasHastaDesabasto() != null) {
            double prob = z.getProbabilidadDesabasto() == null ? 0 : z.getProbabilidadDesabasto() * 100;
            return String.format(Locale.getDefault(), "Desabasto estimado en ≈ %.0f h (%.0f%%)",
                    z.getHorasHastaDesabasto(), prob);
        }
        if (riesgo.equals("bajo")) return "Sin riesgo de desabasto en 72 h";
        return "";
    }

    @Override
    public int getItemCount() {
        return zonas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvRiesgo, tvNivel, tvConsumo, tvAnomalias, tvPrediccion;
        ProgressBar progreso;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvZonaNombre);
            tvRiesgo = itemView.findViewById(R.id.tvZonaRiesgo);
            tvNivel = itemView.findViewById(R.id.tvZonaNivel);
            progreso = itemView.findViewById(R.id.progresoZona);
            tvConsumo = itemView.findViewById(R.id.tvZonaConsumo);
            tvAnomalias = itemView.findViewById(R.id.tvZonaAnomalias);
            tvPrediccion = itemView.findViewById(R.id.tvZonaPrediccion);
        }
    }
}
