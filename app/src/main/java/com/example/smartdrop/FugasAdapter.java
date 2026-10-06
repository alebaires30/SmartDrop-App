package com.example.smartdrop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tarjetas de la pantalla de predicción de fugas: una por vivienda. Las posibles fugas muestran un resumen
 * sencillo (qué pasa, cuánto se pierde y qué hacer) y el detalle técnico se despliega con "Ver más detalles".
 */
public class FugasAdapter extends RecyclerView.Adapter<FugasAdapter.ViewHolder> {

    private final List<FugaVivienda> viviendas = new ArrayList<>();
    private final Set<Integer> expandidas = new HashSet<>();

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
            etiqueta = "VIGILAR";
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
        h.tvProbabilidad.setText(v.getPorcentaje() == null ? ""
                : String.format(Locale.getDefault(), "%.0f %%", v.getPorcentaje()));
        h.tvProbabilidad.setTextColor(ContextCompat.getColor(context, v.isPosibleFuga() ? R.color.text_alert : R.color.text_secondary));

        String nota = notaDatos(v);
        if (nota.isEmpty() && v.getEvaluado() != null && !v.isPosibleFuga()) {
            String fecha = PrediccionFugasActivity.fechaCorta(v.getEvaluado());
            if (!fecha.isEmpty()) nota = "Analizada el " + fecha;
        }
        h.tvNota.setVisibility(nota.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvNota.setText(nota);

        if (!v.isPosibleFuga()) {
            h.layoutInfo.setVisibility(View.GONE);
            h.tvVerMas.setVisibility(View.GONE);
            h.tvDetalle.setVisibility(View.GONE);
            h.itemView.setOnClickListener(null);
            return;
        }

        h.layoutInfo.setVisibility(View.VISIBLE);
        h.tvResumen.setText(v.getResumen() == null || v.getResumen().isEmpty()
                ? "Se detectó un posible escape de agua en esta vivienda." : v.getResumen());
        String clave = FugaTexto.datosClave(v.getMetricas(), v.getInicio());
        h.tvDatosClave.setVisibility(clave.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvDatosClave.setText(clave);
        h.tvAccion.setText("Qué hacer: " + (v.getAccion() == null ? FugaTexto.ACCION_POR_DEFECTO : v.getAccion()));

        boolean abierta = expandidas.contains(v.getHomeId());
        h.tvVerMas.setVisibility(View.VISIBLE);
        h.tvVerMas.setText(abierta ? "Ocultar detalles ▴" : "Ver más detalles ▾");
        h.tvDetalle.setVisibility(abierta ? View.VISIBLE : View.GONE);
        if (abierta) h.tvDetalle.setText(FugaTexto.detalle(v.getCausas(), v.getMetricas(), v.getTitular(), v.getTelefono()));
        View.OnClickListener alternar = view -> {
            if (!expandidas.remove(v.getHomeId())) expandidas.add(v.getHomeId());
            int posicion = h.getAdapterPosition();
            if (posicion != RecyclerView.NO_POSITION) notifyItemChanged(posicion);
        };
        h.tvVerMas.setOnClickListener(alternar);
        h.itemView.setOnClickListener(alternar);
    }

    private static String notaDatos(FugaVivienda v) {
        String calidad = v.getCalidadDatos() == null ? "" : v.getCalidadDatos();
        switch (calidad) {
            case "plana": return "Lecturas constantes: no hay patrón que analizar";
            case "insuficiente": return "Aún no hay suficiente historial";
            case "sin_lecturas_recientes": return "El sensor no está reportando";
            case "sin_datos": return "Sin lecturas sincronizadas";
            default: return "";
        }
    }

    @Override
    public int getItemCount() {
        return viviendas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNic, tvRiesgo, tvUbicacion, tvProbabilidad, tvNota, tvDetalle;
        TextView tvResumen, tvDatosClave, tvAccion, tvVerMas;
        View layoutInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNic = itemView.findViewById(R.id.tvFugaNic);
            tvRiesgo = itemView.findViewById(R.id.tvFugaRiesgo);
            tvUbicacion = itemView.findViewById(R.id.tvFugaUbicacion);
            tvProbabilidad = itemView.findViewById(R.id.tvFugaProbabilidad);
            tvNota = itemView.findViewById(R.id.tvFugaNota);
            tvDetalle = itemView.findViewById(R.id.tvFugaDetalle);
            layoutInfo = itemView.findViewById(R.id.layoutFugaInfo);
            tvResumen = itemView.findViewById(R.id.tvFugaResumen);
            tvDatosClave = itemView.findViewById(R.id.tvFugaDatosClave);
            tvAccion = itemView.findViewById(R.id.tvFugaAccion);
            tvVerMas = itemView.findViewById(R.id.tvFugaVerMas);
        }
    }
}
