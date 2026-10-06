package com.example.smartdrop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {

    public interface OnReporteClickListener {
        void onReporteClick(Reporte reporte);
    }

    private final List<Reporte> reportes = new ArrayList<>();
    private final OnReporteClickListener listener;

    public ReportesAdapter(OnReporteClickListener listener) {
        this.listener = listener;
    }

    public void setReportes(List<Reporte> nuevos) {
        reportes.clear();
        if (nuevos != null) reportes.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reporte reporte = reportes.get(position);
        holder.tvTipo.setText(reporte.getTipoLabel());
        holder.tvDescripcion.setText(reporte.getDescripcion());
        holder.tvEstado.setText(reporte.getEstadoLabel());
        holder.tvFecha.setText(reporte.getFechaReporte() != null && reporte.getFechaReporte().length() >= 16
                ? reporte.getFechaReporte().substring(0, 16).replace('T', ' ')
                : reporte.getFechaReporte());
        holder.tvAdjuntos.setText(reporte.getNAdjuntos() > 0
                ? reporte.getNAdjuntos() + (reporte.getNAdjuntos() == 1 ? " adjunto" : " adjuntos") : "");
        if (reporte.getAutorNombre() != null && !reporte.getAutorNombre().isEmpty()) {
            holder.tvAutor.setText(reporte.getAutorNombre());
            holder.tvAutor.setVisibility(View.VISIBLE);
        } else {
            holder.tvAutor.setVisibility(View.GONE);
        }

        int colorEstado;
        switch (reporte.getEstado() != null ? reporte.getEstado() : "") {
            case "resuelto": colorEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.success_dark); break;
            case "en_proceso": colorEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_blue); break;
            default: colorEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_amber); break;
        }
        holder.tvEstado.setTextColor(colorEstado);

        holder.itemView.setOnClickListener(v -> listener.onReporteClick(reporte));
    }

    @Override
    public int getItemCount() { return reportes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvDescripcion, tvEstado, tvFecha, tvAdjuntos, tvAutor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipo = itemView.findViewById(R.id.tvReporteTipo);
            tvDescripcion = itemView.findViewById(R.id.tvReporteDescripcion);
            tvEstado = itemView.findViewById(R.id.tvReporteEstado);
            tvFecha = itemView.findViewById(R.id.tvReporteFecha);
            tvAdjuntos = itemView.findViewById(R.id.tvReporteAdjuntos);
            tvAutor = itemView.findViewById(R.id.tvReporteAutor);
        }
    }
}
