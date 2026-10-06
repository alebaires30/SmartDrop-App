package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertasActivity extends BaseActivity {

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutAlertas;
    private TextView tvEstado;
    private boolean solicitudEnCurso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas);

        swipeRefresh = findViewById(R.id.swipeRefreshAlertas);
        layoutAlertas = findViewById(R.id.layoutAlertas);
        tvEstado = findViewById(R.id.tvEstadoAlertas);
        findViewById(R.id.btnVolverAlertas).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::cargarAlertas);
        cargarAlertas();
    }

    private void cargarAlertas() {
        if (solicitudEnCurso) {
            swipeRefresh.setRefreshing(false);
            return;
        }
        solicitudEnCurso = true;
        swipeRefresh.setRefreshing(true);
        tvEstado.setVisibility(View.VISIBLE);
        tvEstado.setText("Consultando el estado de los sensores...");
        layoutAlertas.removeAllViews();

        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerResumen().enqueue(new Callback<ResumenDashboardResponse>() {
            @Override
            public void onResponse(Call<ResumenDashboardResponse> call,
                                   Response<ResumenDashboardResponse> response) {
                finalizarCarga();
                if (!response.isSuccessful() || response.body() == null) {
                    mostrarError("No se pudieron cargar las alertas. Desliza hacia abajo para reintentar.");
                    return;
                }
                mostrarAlertas(response.body());
            }

            @Override
            public void onFailure(Call<ResumenDashboardResponse> call, Throwable error) {
                finalizarCarga();
                if (!call.isCanceled()) {
                    mostrarError("No hay conexión con el servidor. Desliza hacia abajo para reintentar.");
                }
            }
        });
    }

    private void finalizarCarga() {
        solicitudEnCurso = false;
        swipeRefresh.setRefreshing(false);
    }

    private void mostrarError(String mensaje) {
        layoutAlertas.removeAllViews();
        tvEstado.setVisibility(View.VISIBLE);
        tvEstado.setText(mensaje);
    }

    private void mostrarAlertas(ResumenDashboardResponse respuesta) {
        List<AlertaSensor> alertas = new ArrayList<>();
        agregarSiActiva(alertas, "Flujo de agua", "flujo", respuesta.getFlujo());
        agregarSiActiva(alertas, "Presión", "presion", respuesta.getPresion());
        agregarSiActiva(alertas, "Nivel del tanque", "nivel", respuesta.getNivel());

        layoutAlertas.removeAllViews();
        if (alertas.isEmpty()) {
            tvEstado.setVisibility(View.VISIBLE);
            tvEstado.setText("No hay alertas activas. Los sensores están dentro de sus rangos.");
            return;
        }

        tvEstado.setVisibility(View.VISIBLE);
        tvEstado.setText(alertas.size() == 1
                ? "Hay 1 parámetro fuera de rango. Toca la alerta para ver su gráfica."
                : "Hay " + alertas.size() + " parámetros fuera de rango. Toca una alerta para ver su gráfica.");
        for (AlertaSensor alerta : alertas) {
            layoutAlertas.addView(crearTarjeta(alerta));
        }
    }

    private void agregarSiActiva(List<AlertaSensor> alertas, String nombre,
                                 String parametro, ValorActual valor) {
        if (valor != null && valor.isFueraDeRango()) {
            alertas.add(new AlertaSensor(nombre, parametro, valor));
        }
    }

    private CardView crearTarjeta(AlertaSensor alerta) {
        CardView tarjeta = new CardView(this);
        LinearLayout.LayoutParams tarjetaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tarjetaParams.bottomMargin = dp(12);
        tarjeta.setLayoutParams(tarjetaParams);
        tarjeta.setRadius(dp(16));
        tarjeta.setCardElevation(dp(3));
        tarjeta.setCardBackgroundColor(getColor(R.color.bg_card));
        tarjeta.setUseCompatPadding(true);
        tarjeta.setClickable(true);
        tarjeta.setFocusable(true);
        tarjeta.setForeground(getDrawable(android.R.drawable.list_selector_background));

        LinearLayout contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(dp(16), dp(14), dp(16), dp(14));
        tarjeta.addView(contenido);

        TextView titulo = new TextView(this);
        String estado = alerta.valor.getEstado();
        titulo.setText(alerta.nombre + ("Baja".equals(estado) ? ": valor bajo"
                : "Alta".equals(estado) ? ": valor alto" : " fuera de rango"));
        titulo.setTextColor(getColor(R.color.text_alert));
        titulo.setTextSize(15);
        titulo.setGravity(Gravity.CENTER_VERTICAL);
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);
        contenido.addView(titulo);

        TextView valor = new TextView(this);
        String unidad = alerta.valor.getUnidad() == null ? "" : " " + alerta.valor.getUnidad();
        valor.setText(String.format(Locale.getDefault(), "%.2f%s", alerta.valor.getValor(), unidad));
        valor.setTextColor(getColor(R.color.text_primary));
        valor.setTextSize(23);
        valor.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams valorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        valorParams.topMargin = dp(8);
        contenido.addView(valor, valorParams);

        String rango = rangoNormal(alerta.valor, unidad);
        if (!rango.isEmpty()) {
            TextView tvRango = new TextView(this);
            tvRango.setText(rango);
            tvRango.setTextColor(getColor(R.color.text_primary));
            tvRango.setTextSize(13);
            LinearLayout.LayoutParams rangoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rangoParams.topMargin = dp(4);
            contenido.addView(tvRango, rangoParams);
        }

        TextView fecha = new TextView(this);
        String fechaCorta = PrediccionFugasActivity.fechaCorta(alerta.valor.getFecha());
        String origen = alerta.valor.getNic() == null || alerta.valor.getNic().isEmpty() ? "" : alerta.valor.getNic() + " · ";
        fecha.setText(origen + (fechaCorta.isEmpty() ? "Lectura reciente" : "Lectura del " + fechaCorta));
        fecha.setTextColor(getColor(R.color.text_secondary));
        fecha.setTextSize(12);
        LinearLayout.LayoutParams fechaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fechaParams.topMargin = dp(4);
        contenido.addView(fecha, fechaParams);

        TextView accion = new TextView(this);
        accion.setText("Ver gráfica →");
        accion.setTextColor(getColor(R.color.brand_accent));
        accion.setTextSize(13);
        accion.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams accionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        accionParams.topMargin = dp(12);
        contenido.addView(accion, accionParams);

        tarjeta.setOnClickListener(v -> {
            Intent intent = new Intent(AlertasActivity.this, GraficasMonitoreoActivity.class);
            intent.putExtra("parametro_inicial", alerta.parametro);
            startActivity(intent);
        });
        return tarjeta;
    }

    /** "Rango normal: 1 – 25 L/min" (o solo el límite que exista). */
    private static String rangoNormal(ValorActual valor, String unidad) {
        Double min = valor.getRangoMin(), max = valor.getRangoMax();
        if (min != null && max != null) {
            return String.format(Locale.getDefault(), "Rango normal: %s – %s%s", corto(min), corto(max), unidad);
        }
        if (min != null) return "Mínimo normal: " + corto(min) + unidad;
        if (max != null) return "Máximo normal: " + corto(max) + unidad;
        return "";
    }

    private static String corto(double numero) {
        return numero == Math.rint(numero) ? String.valueOf((long) numero) : String.format(Locale.getDefault(), "%.1f", numero);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class AlertaSensor {
        final String nombre;
        final String parametro;
        final ValorActual valor;

        AlertaSensor(String nombre, String parametro, ValorActual valor) {
            this.nombre = nombre;
            this.parametro = parametro;
            this.valor = valor;
        }
    }
}
