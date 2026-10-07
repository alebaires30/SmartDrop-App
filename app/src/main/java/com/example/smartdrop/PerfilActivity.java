package com.example.smartdrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Mi perfil" de la app: los mismos datos y opciones que el perfil web (información personal editable,
 * notificaciones y preferencias). Las preferencias se guardan en el servidor y se comparten con la web.
 */
public class PerfilActivity extends BaseActivity {

    /** Campo del servidor → fila del perfil. */
    private static final class Opcion {
        final String campo;
        final String titulo;
        final String descripcion;
        final int icono;

        Opcion(String campo, String titulo, String descripcion, int icono) {
            this.campo = campo;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.icono = icono;
        }
    }

    private static final Opcion[] NOTIFICACIONES = {
            new Opcion("alertas_nivel", "Alertas de nivel", "Tanque bajo o fuera de rango", R.drawable.ic_bell),
            new Opcion("suministro", "Suministro", "Presión y flujo fuera de rango", R.drawable.ic_water_drop),
            new Opcion("calidad", "Calidad", "Calidad del agua (TDS)", R.drawable.ic_shield),
            new Opcion("consumo_elevado", "Consumo elevado", "Consumo mayor que el mes anterior", R.drawable.ic_chart),
    };
    private static final Opcion[] PREFERENCIAS = {
            new Opcion("modo_oscuro", "Modo oscuro", "Colores oscuros en la app y la web", R.drawable.ic_moon),
            new Opcion("idioma", "Idioma español", "Apágalo para usar SmartDrop en inglés", R.drawable.ic_language),
            new Opcion("reportes_semanales", "Reportes semanales", "Resumen de tu consumo cada 7 días", R.drawable.ic_chart),
    };

    private final Map<String, SwitchMaterial> interruptores = new LinkedHashMap<>();
    private TextView tvIniciales, tvNombre, tvEmail, tvRol, tvReporte, tvEstado;
    private LinearLayout layoutDatos, layoutReporte;
    private SwipeRefreshLayout swipe;
    private ApiService api;
    private Perfil.Datos perfil;
    /** Evita guardar cuando el interruptor se mueve por código (al pintar lo que vino del servidor). */
    private boolean pintando;

    /** Título de la barra superior (la pantalla del admin muestra "Configuración"). */
    protected String titulo() {
        return "Mi Perfil";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        api = ApiClient.getClientAutenticado(this).create(ApiService.class);

        ((TextView) findViewById(R.id.tvTituloPerfil)).setText(titulo());
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        tvIniciales = findViewById(R.id.tvPerfilIniciales);
        tvNombre = findViewById(R.id.tvPerfilNombre);
        tvEmail = findViewById(R.id.tvPerfilEmail);
        tvRol = findViewById(R.id.tvPerfilRol);
        tvReporte = findViewById(R.id.tvReporteSemanal);
        tvEstado = findViewById(R.id.tvEstadoPreferencias);
        layoutDatos = findViewById(R.id.layoutDatosPersonales);
        layoutReporte = findViewById(R.id.layoutReporteSemanal);
        swipe = findViewById(R.id.swipePerfil);
        swipe.setOnRefreshListener(this::cargar);

        agregarFilas(findViewById(R.id.layoutNotificaciones), NOTIFICACIONES);
        agregarFilas(findViewById(R.id.layoutPreferencias), PREFERENCIAS);
        findViewById(R.id.btnEditarPerfil).setOnClickListener(v -> editarPerfil());
        ((Button) findViewById(R.id.btnCerrarSesionPerfil)).setOnClickListener(v -> cerrarSesion());

        // Mientras llega el servidor se muestra lo guardado en el teléfono.
        SharedPreferences sesion = getSharedPreferences("sesion", MODE_PRIVATE);
        tvNombre.setText(sesion.getString("nombre", ""));
        tvEmail.setText(sesion.getString("email", ""));
        tvRol.setText(sesion.getString("nombre_rol", ""));
        pintando = true;
        interruptores.get("modo_oscuro").setChecked(sesion.getBoolean("modo_oscuro", false));
        interruptores.get("idioma").setChecked(!Idioma.enIngles(this));
        pintando = false;
        cargar();
    }

    private void agregarFilas(LinearLayout contenedor, Opcion[] opciones) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Opcion opcion : opciones) {
            View fila = inflater.inflate(R.layout.item_preferencia, contenedor, false);
            ((ImageView) fila.findViewById(R.id.ivPreferenciaIcono)).setImageResource(opcion.icono);
            ((TextView) fila.findViewById(R.id.tvPreferenciaTitulo)).setText(opcion.titulo);
            ((TextView) fila.findViewById(R.id.tvPreferenciaDescripcion)).setText(opcion.descripcion);
            SwitchMaterial interruptor = fila.findViewById(R.id.switchPreferencia);
            interruptor.setContentDescription(opcion.titulo);
            interruptor.setOnCheckedChangeListener((boton, activo) -> {
                if (!pintando) guardar(opcion.campo, activo);
            });
            fila.setOnClickListener(v -> interruptor.toggle());
            interruptores.put(opcion.campo, interruptor);
            contenedor.addView(fila);
            Idioma.traducirArbol(fila);
        }
    }

    private void cargar() {
        swipe.setRefreshing(true);
        api.obtenerPerfil().enqueue(new Callback<Perfil.Datos>() {
            @Override
            public void onResponse(Call<Perfil.Datos> call, Response<Perfil.Datos> response) {
                swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    tvEstado.setText("No se pudo cargar el perfil. Desliza hacia abajo para reintentar.");
                    return;
                }
                mostrar(response.body());
            }

            @Override
            public void onFailure(Call<Perfil.Datos> call, Throwable t) {
                swipe.setRefreshing(false);
                tvEstado.setText("Sin conexión con el servidor. Desliza hacia abajo para reintentar.");
            }
        });
    }

    private void mostrar(Perfil.Datos datos) {
        perfil = datos;
        tvIniciales.setText(datos.iniciales == null || datos.iniciales.isEmpty() ? "?" : datos.iniciales);
        tvNombre.setText(datos.nombreCompleto);
        tvEmail.setText(datos.correo);
        tvRol.setText(datos.rolLabel);
        getSharedPreferences("sesion", MODE_PRIVATE).edit()
                .putString("nombre", datos.nombre).putString("email", datos.correo).apply();

        layoutDatos.removeAllViews();
        agregarDato("Nombre", datos.nombreCompleto);
        agregarDato("Correo electrónico", datos.correo);
        agregarDato("Dirección", vacio(datos.direccion) ? "Sin vivienda vinculada" : datos.direccion);
        agregarDato("Teléfono", vacio(datos.telefono) ? "No configurado" : datos.telefono);
        if (!vacio(datos.miembroDesde)) agregarDato("Cuenta activa desde", datos.miembroDesde);

        pintarPreferencias(datos.preferencias, datos.reporteSemanal);
        tvEstado.setText("");
    }

    private void agregarDato(String etiqueta, String valor) {
        View fila = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, layoutDatos, false);
        TextView titulo = fila.findViewById(android.R.id.text1);
        TextView detalle = fila.findViewById(android.R.id.text2);
        titulo.setText(etiqueta);
        titulo.setTextSize(12);
        titulo.setTextColor(getColor(R.color.text_secondary));
        detalle.setText(valor);
        detalle.setTextSize(15);
        detalle.setTextColor(getColor(R.color.text_primary));
        fila.setPadding(0, dp(4), 0, dp(4));
        layoutDatos.addView(fila);
        Idioma.traducirArbol(titulo);
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private void pintarPreferencias(Perfil.Preferencias prefs, Perfil.ReporteSemanal reporte) {
        if (prefs == null) return;
        pintando = true;
        marcar("alertas_nivel", prefs.alertasNivel);
        marcar("suministro", prefs.suministro);
        marcar("calidad", prefs.calidad);
        marcar("consumo_elevado", prefs.consumoElevado);
        marcar("modo_oscuro", prefs.modoOscuro);
        marcar("reportes_semanales", prefs.reportesSemanales);
        if (prefs.idioma != null) marcar("idioma", Idioma.ESPANOL.equals(prefs.idioma));
        pintando = false;

        boolean hayReporte = reporte != null && !vacio(reporte.mensaje);
        layoutReporte.setVisibility(hayReporte ? View.VISIBLE : View.GONE);
        if (hayReporte) tvReporte.setText(reporte.mensaje);

        // La web pudo cambiar el tema o el idioma: se aplican también aquí.
        aplicarTema(Boolean.TRUE.equals(prefs.modoOscuro));
        if (prefs.idioma != null) Idioma.cambiar(this, prefs.idioma);
    }

    private void marcar(String campo, Boolean valor) {
        SwitchMaterial interruptor = interruptores.get(campo);
        if (interruptor != null && valor != null) interruptor.setChecked(valor);
    }

    private void guardar(String campo, boolean activo) {
        Map<String, Object> cambio = new HashMap<>();
        cambio.put(campo, "idioma".equals(campo) ? (activo ? Idioma.ESPANOL : Idioma.INGLES) : activo);
        tvEstado.setText("Guardando…");
        if ("modo_oscuro".equals(campo)) aplicarTema(activo);
        api.actualizarPreferencias(cambio).enqueue(new Callback<Perfil.PreferenciasResponse>() {
            @Override
            public void onResponse(Call<Perfil.PreferenciasResponse> call, Response<Perfil.PreferenciasResponse> response) {
                Perfil.PreferenciasResponse cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null || !cuerpo.ok) {
                    revertir(campo, activo);
                    return;
                }
                tvEstado.setText("Preferencia guardada. También se aplica en la web.");
                boolean hayReporte = cuerpo.reporteSemanal != null && !vacio(cuerpo.reporteSemanal.mensaje);
                layoutReporte.setVisibility(hayReporte ? View.VISIBLE : View.GONE);
                if (hayReporte) tvReporte.setText(cuerpo.reporteSemanal.mensaje);
                if ("idioma".equals(campo)) Idioma.cambiar(PerfilActivity.this, activo ? Idioma.ESPANOL : Idioma.INGLES);
                // Las categorías de notificación cambian qué avisos llegan: se revisa de nuevo.
                AvisosUsuario.revisarAhora(PerfilActivity.this);
            }

            @Override
            public void onFailure(Call<Perfil.PreferenciasResponse> call, Throwable t) {
                revertir(campo, activo);
            }
        });
    }

    private void revertir(String campo, boolean activo) {
        pintando = true;
        marcar(campo, !activo);
        pintando = false;
        if ("modo_oscuro".equals(campo)) aplicarTema(!activo);
        tvEstado.setText("No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.");
    }

    private void aplicarTema(boolean oscuro) {
        SharedPreferences sesion = getSharedPreferences("sesion", MODE_PRIVATE);
        if (sesion.getBoolean("modo_oscuro", false) == oscuro) return;
        sesion.edit().putBoolean("modo_oscuro", oscuro).apply();
        AppCompatDelegate.setDefaultNightMode(oscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void editarPerfil() {
        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        formulario.setPadding(dp(20), dp(8), dp(20), 0);
        EditText nombre = campo(formulario, "Nombre", perfil == null ? "" : perfil.nombre, InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText apellido = campo(formulario, "Apellido", perfil == null ? "" : perfil.apellido, InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText correo = campo(formulario, "Correo electrónico", perfil == null ? "" : perfil.correo,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        Idioma.traducirArbol(formulario);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(Idioma.t(this, "Editar perfil"))
                .setView(formulario)
                .setNegativeButton(Idioma.t(this, "Cancelar"), null)
                .setPositiveButton(Idioma.t(this, "Guardar cambios"), null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Map<String, String> datos = new HashMap<>();
            datos.put("nombre", nombre.getText().toString().trim());
            datos.put("apellido", apellido.getText().toString().trim());
            datos.put("correo", correo.getText().toString().trim());
            enviarEdicion(datos, dialogo);
        }));
        dialogo.show();
    }

    private EditText campo(LinearLayout formulario, String etiqueta, String valor, int tipo) {
        EditText campo = new EditText(this);
        campo.setHint(etiqueta);
        campo.setText(valor == null ? "" : valor);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | tipo);
        campo.setSingleLine(true);
        formulario.addView(campo);
        return campo;
    }

    private void enviarEdicion(Map<String, String> datos, AlertDialog dialogo) {
        api.editarPerfil(datos).enqueue(new Callback<Perfil.EdicionResponse>() {
            @Override
            public void onResponse(Call<Perfil.EdicionResponse> call, Response<Perfil.EdicionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().perfil != null) {
                    dialogo.dismiss();
                    mostrar(response.body().perfil);
                    Idioma.toast(PerfilActivity.this, "Tu información personal se actualizó correctamente.", Toast.LENGTH_SHORT);
                    return;
                }
                String error = "No se pudo guardar la información.";
                if (response.code() == 409) error = "Este correo ya está registrado.";
                else if (response.code() == 400) error = "Revisa los datos: nombre, apellido y un correo válido.";
                Idioma.toast(PerfilActivity.this, error, Toast.LENGTH_LONG);
            }

            @Override
            public void onFailure(Call<Perfil.EdicionResponse> call, Throwable t) {
                Idioma.toast(PerfilActivity.this, "Sin conexión con el servidor", Toast.LENGTH_SHORT);
            }
        });
    }

    protected void cerrarSesion() {
        AvisosUsuario.cancelar(this);
        AvisosFuga.cancelar(this);
        Idioma.limpiarSesion(this);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
