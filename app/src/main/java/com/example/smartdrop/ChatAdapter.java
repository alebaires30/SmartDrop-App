package com.example.smartdrop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final int TIPO_PROPIO = 1;
    private static final int TIPO_AJENO = 0;

    private final List<ReporteMensaje> mensajes = new ArrayList<>();

    public void setMensajes(List<ReporteMensaje> nuevos) {
        mensajes.clear();
        if (nuevos != null) mensajes.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void agregar(ReporteMensaje mensaje) {
        mensajes.add(mensaje);
        notifyItemInserted(mensajes.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return mensajes.get(position).isPropio() ? TIPO_PROPIO : TIPO_AJENO;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TIPO_PROPIO ? R.layout.item_chat_propio : R.layout.item_chat_ajeno;
        View vista = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReporteMensaje mensaje = mensajes.get(position);
        holder.tvTexto.setText(mensaje.getMensaje());
        String autor = mensaje.isEsAdminEmisor() ? "🛡 " + mensaje.getAutorNombre() : mensaje.getAutorNombre();
        holder.tvAutor.setText(autor);
        String fecha = mensaje.getFechaEnvio();
        holder.tvFecha.setText(fecha != null && fecha.length() >= 16 ? fecha.substring(5, 16).replace('T', ' ') : "");
    }

    @Override
    public int getItemCount() { return mensajes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAutor, tvTexto, tvFecha;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAutor = itemView.findViewById(R.id.tvChatAutor);
            tvTexto = itemView.findViewById(R.id.tvChatTexto);
            tvFecha = itemView.findViewById(R.id.tvChatFecha);
        }
    }
}
