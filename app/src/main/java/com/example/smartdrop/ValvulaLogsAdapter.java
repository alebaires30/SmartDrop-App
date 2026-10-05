package com.example.smartdrop;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ValvulaLogsAdapter extends RecyclerView.Adapter<ValvulaLogsAdapter.ViewHolder> {

    private List<ValvulaLogs> logsList = new ArrayList<>();

    public void setLogs(List<ValvulaLogs> logs) {
        if (logs != null) {
            this.logsList = new ArrayList<>(logs);
        } else {
            this.logsList = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_valvula_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ValvulaLogs log = logsList.get(position);

        // Acción (ABRIR -> Verde #27AE60, CERRAR -> Rojo #E74C3C)
        String accion = log.getAccion() != null ? log.getAccion().toUpperCase() : "ACCIÓN";
        holder.tvAccion.setText(accion);
        boolean esAbrir = accion.contains("ABRIR") || accion.contains("OPEN");
        holder.tvAccion.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                esAbrir ? R.color.success : R.color.danger));

        // Etiqueta/Badge: {usuario} • {tipo_activacion}
        String usuario = (log.getUsuario() != null && !log.getUsuario().isEmpty()) ? log.getUsuario() : "Sistema";
        String origen = log.getOrigen() != null ? log.getOrigen() : "Manual";
        String detalle = log.getDetalle();

        String badgeText;
        if (origen.toLowerCase().contains("temporizado") || (detalle != null && !detalle.trim().isEmpty() && !detalle.equals("0"))) {
            String duracionTexto = (detalle != null && !detalle.trim().isEmpty()) ? detalle.trim() : "";
            if (!duracionTexto.endsWith("s") && !duracionTexto.isEmpty()) {
                duracionTexto += "s";
            }
            badgeText = usuario + " • Temporizado (" + duracionTexto + ")";
        } else {
            String origenLimpio = origen.replace("App (", "").replace(")", "");
            badgeText = usuario + " • " + origenLimpio;
        }

        holder.tvTipo.setText(badgeText);

        // Fecha/Hora legible
        String fecha = log.getFechaHora() != null ? log.getFechaHora().replace("T", " ") : "---";
        if (fecha.contains(".")) {
            fecha = fecha.substring(0, fecha.indexOf("."));
        }
        holder.tvFecha.setText(fecha);
    }

    @Override
    public int getItemCount() {
        return logsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAccion, tvTipo, tvFecha;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAccion = itemView.findViewById(R.id.tvLogAccion);
            tvTipo = itemView.findViewById(R.id.tvLogTipo);
            tvFecha = itemView.findViewById(R.id.tvLogFecha);
        }
    }
}
