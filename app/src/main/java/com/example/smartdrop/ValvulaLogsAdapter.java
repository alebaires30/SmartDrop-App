package com.example.smartdrop;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;

public class ValvulaLogsAdapter extends RecyclerView.Adapter<ValvulaLogsAdapter.ViewHolder> {

    private List<ValvulaLogs> listaOriginal = new ArrayList<>();
    private List<ValvulaLogs> listaFiltrada = new ArrayList<>();
    private String queryActual = "";

    public void setLogs(List<ValvulaLogs> logs) {
        this.listaOriginal = new ArrayList<>(logs);
        this.listaFiltrada = new ArrayList<>(logs);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        this.queryActual = query;
        if (query.isEmpty()) {
            listaFiltrada = new ArrayList<>(listaOriginal);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            List<ValvulaLogs> filtered = new ArrayList<>();
            for (ValvulaLogs log : listaOriginal) {
                if ((log.getAccion() != null && log.getAccion().toLowerCase().contains(lowerCaseQuery)) ||
                    (log.getUsuario() != null && log.getUsuario().toLowerCase().contains(lowerCaseQuery)) ||
                    (log.getOrigen() != null && log.getOrigen().toLowerCase().contains(lowerCaseQuery))) {
                    filtered.add(log);
                }
            }
            listaFiltrada = filtered;
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
        ValvulaLogs log = listaFiltrada.get(position);
        
        // Acción
        String accion = log.getAccion() != null ? log.getAccion().toUpperCase() : "ACCIÓN";
        setHighlightedText(holder.tvAccion, accion, queryActual);
        holder.tvAccion.setTextColor(accion.contains("ABRIR") ? 
                Color.parseColor("#27AE60") : Color.parseColor("#E74C3C"));

        // Tipo / Origen y Usuario
        String origen = log.getOrigen() != null ? log.getOrigen() : "Manual";
        String usuario = (log.getUsuario() != null && !log.getUsuario().isEmpty()) ? log.getUsuario() : "Sistema/Auto";
        
        String tipoUsuarioText;
        if (origen.toLowerCase().contains("temporizado") && log.getDetalle() != null && !log.getDetalle().isEmpty()) {
            tipoUsuarioText = usuario + " • Temporizado (" + log.getDetalle() + "s)";
        } else {
            tipoUsuarioText = usuario + " • " + (origen.equals("App (Manual)") ? "Manual" : origen);
        }
        setHighlightedText(holder.tvTipo, tipoUsuarioText, queryActual);

        // Fecha
        holder.tvFecha.setText(log.getFechaHora() != null ? log.getFechaHora().replace("T", " ") : "---");
    }

    private void setHighlightedText(TextView tv, String fullText, String query) {
        if (query == null || query.isEmpty()) {
            tv.setText(fullText);
            return;
        }
        SpannableString spannable = new SpannableString(fullText);
        String lowerFull = fullText.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int start = lowerFull.indexOf(lowerQuery);
        while (start >= 0) {
            int end = start + lowerQuery.length();
            spannable.setSpan(new BackgroundColorSpan(Color.YELLOW), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = lowerFull.indexOf(lowerQuery, end);
        }
        tv.setText(spannable);
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
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
