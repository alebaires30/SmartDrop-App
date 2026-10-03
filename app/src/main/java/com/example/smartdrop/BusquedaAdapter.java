package com.example.smartdrop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BusquedaAdapter extends RecyclerView.Adapter<BusquedaAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(BusquedaGlobalResponse.Seccion seccion);
    }

    private List<BusquedaGlobalResponse.Seccion> listaResultados = new ArrayList<>();
    private final OnItemClickListener listener;

    public BusquedaAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setResultados(List<BusquedaGlobalResponse.Seccion> resultados) {
        if (resultados != null) {
            this.listaResultados = new ArrayList<>(resultados);
        } else {
            this.listaResultados = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_busqueda_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusquedaGlobalResponse.Seccion item = listaResultados.get(position);

        holder.tvTitulo.setText(item.getTitulo() != null ? item.getTitulo() : "Sección");
        holder.tvUrl.setText(item.getUrl() != null ? item.getUrl() : "/");
        holder.tvContenido.setText(item.getContenido() != null ? item.getContenido() : "");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaResultados.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvUrl, tvContenido;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvResultTitulo);
            tvUrl = itemView.findViewById(R.id.tvResultUrl);
            tvContenido = itemView.findViewById(R.id.tvResultContenido);
        }
    }
}
