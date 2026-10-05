package com.example.smartdrop;

import android.graphics.Color;
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

        h.tvNombre.setText(z.getZoneName());
        h.tvNivel.setText(String.format(Locale.getDefault(),
                "Nivel: %.0f L (%.1f%%)", z.getNivelActualLitros(), z.getPorcentajeLlenado()));
        h.progreso.setProgress((int) Math.round(z.getPorcentajeLlenado()));
        h.tvConsumo.setText(String.format(Locale.getDefault(),
                "Consumo: %.1f L/h  •  %d viviendas", z.getConsumoTotalLph(), z.getNHomes()));

        if (z.getAnomaliasActivas24h() > 0) {
            h.tvAnomalias.setText(String.format(Locale.getDefault(),
                    "⚠ %d anomalías 24h", z.getAnomaliasActivas24h()));
            h.tvAnomalias.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_alert));
        } else {
            h.tvAnomalias.setText("Sin anomalías");
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
            case "moderado":
                h.tvRiesgo.setText("RIESGO MODERADO");
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

        h.tvPrediccion.setVisibility(View.GONE);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onZonaClick(z);
        });
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
