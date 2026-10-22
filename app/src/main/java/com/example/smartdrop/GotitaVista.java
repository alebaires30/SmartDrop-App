package com.example.smartdrop;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Gotita en cada pantalla de la app: botón con la gota en la esquina, burbuja para frases y avisos, y el panel
 * de conversación que sube desde abajo (con el historial de conversaciones). Cada pantalla tiene el suyo; lo
 * que Gotita recuerda está en Gotita. Si el panel se queda abierto 2 minutos sin usarse, se esconde solo.
 */
final class GotitaVista implements Gotita.Vista {

    static final int PERMISO_MICROFONO = 701;
    private static final long INACTIVIDAD_MS = 2 * 60 * 1000;

    private final Activity actividad;
    private final Gotita gotita;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final FrameLayout capa;
    private final FrameLayout boton;
    private final ImageView gota;
    private final LinearLayout burbuja;
    private final TextView burbujaTexto;
    private final Button burbujaVer;
    private BottomSheetDialog panel;
    private View vistaPanel;
    private TextView parcial;
    private ValueAnimator animacion;
    private boolean bocaAbierta;
    private boolean actualizando;
    private String destinoBurbuja;
    private final Map<String, View> secciones = new HashMap<>();

    private final Runnable ocultarBurbuja = this::ocultarBurbuja;
    // Esconde el panel si nadie lo usa en 2 minutos (la conversación sigue igual al volver a abrirlo).
    private final Runnable revisarInactividad = this::cerrarSiNadieLoUsa;
    private final Runnable mover = new Runnable() {
        @Override
        public void run() {
            bocaAbierta = !bocaAbierta;
            gota.setImageResource(bocaAbierta ? R.drawable.ic_gotita_hablando : R.drawable.ic_gotita);
            principal.postDelayed(this, 170);
        }
    };

    /** Pone a Gotita en la pantalla si hay sesión iniciada (no en el login, registro ni la portada). */
    static GotitaVista adjuntar(Activity actividad) {
        String nombre = actividad.getClass().getSimpleName();
        if ("SplashActivity".equals(nombre) || "MainActivity".equals(nombre) || "RegisterActivity".equals(nombre)) {
            return null;
        }
        if (!Sesion.activa(actividad)) return null; // los tokens van cifrados: se pregunta a Sesion
        View contenido = actividad.findViewById(android.R.id.content);
        if (!(contenido instanceof FrameLayout)) return null;
        return new GotitaVista(actividad, (FrameLayout) contenido);
    }

    private GotitaVista(Activity actividad, FrameLayout contenido) {
        this.actividad = actividad;
        gotita = Gotita.de(actividad);

        capa = new FrameLayout(actividad);
        capa.setTag(GotitaPantalla.ETIQUETA_GOTITA);
        capa.setClipChildren(false);
        capa.setClipToPadding(false);
        contenido.addView(capa, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Botón redondo con la gota
        boton = new FrameLayout(actividad);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.OVAL);
        fondo.setColor(ContextCompat.getColor(actividad, R.color.surface));
        fondo.setStroke(dp(1), ContextCompat.getColor(actividad, R.color.line));
        boton.setBackground(fondo);
        boton.setElevation(dp(8));
        boton.setClickable(true);
        boton.setFocusable(true);
        boton.setContentDescription("Gotita, tu asistente. Toca para abrir el chat; mantén presionado para hablarle.");
        gota = new ImageView(actividad);
        gota.setImageResource(R.drawable.ic_gotita);
        boton.addView(gota, new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER));
        FrameLayout.LayoutParams lpBoton = new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM | Gravity.END);
        boolean conBarraInferior = tieneBarraInferior(contenido);
        lpBoton.setMargins(0, 0, dp(16), dp(conBarraInferior ? 88 : 20));
        capa.addView(boton, lpBoton);
        boton.setOnClickListener(v -> abrirPanel());
        boton.setOnLongClickListener(v -> {
            hablar();
            return true;
        });

        // Burbuja de frases y avisos (encima del botón)
        burbuja = (LinearLayout) LayoutInflater.from(actividad).inflate(R.layout.item_chat_ajeno, capa, false);
        burbuja.setPadding(0, 0, 0, 0);
        LinearLayout tarjeta = (LinearLayout) burbuja.getChildAt(0);
        tarjeta.setElevation(dp(6));
        ((TextView) tarjeta.findViewById(R.id.tvChatAutor)).setText("Gotita");
        burbujaTexto = tarjeta.findViewById(R.id.tvChatTexto);
        burbujaTexto.setMaxLines(5);
        burbujaTexto.setEllipsize(TextUtils.TruncateAt.END);
        tarjeta.findViewById(R.id.tvChatFecha).setVisibility(View.GONE);
        burbujaVer = new MaterialButton(actividad, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        burbujaVer.setVisibility(View.GONE);
        tarjeta.addView(burbujaVer);
        FrameLayout.LayoutParams lpBurbuja = new FrameLayout.LayoutParams(dp(300),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        lpBurbuja.setMargins(dp(16), 0, dp(16), lpBoton.bottomMargin + dp(70));
        capa.addView(burbuja, lpBurbuja);
        burbuja.setVisibility(View.GONE);
        tarjeta.setOnClickListener(v -> abrirPanel());
        burbujaVer.setOnClickListener(v -> {
            ocultarBurbuja();
            if (destinoBurbuja != null) alNavegar(destinoBurbuja);
        });

        // Bordes del sistema (barra de gestos) para que el botón no quede debajo
        ViewCompat.setOnApplyWindowInsetsListener(capa, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(capa);

        gotita.conectar(this);
        alCambiarAjustes();
        alCambiarEstado(gotita.estado());
    }

    /** ¿La pantalla tiene barra de botones abajo? Entonces la gota se pone más arriba para no taparla. */
    private static boolean tieneBarraInferior(View vista) {
        if (vista instanceof com.google.android.material.bottomnavigation.BottomNavigationView) return true;
        if (!(vista instanceof ViewGroup)) return false;
        ViewGroup grupo = (ViewGroup) vista;
        for (int i = 0; i < grupo.getChildCount(); i++) {
            if (tieneBarraInferior(grupo.getChildAt(i))) return true;
        }
        return false;
    }

    private int dp(int valor) {
        return Math.round(valor * actividad.getResources().getDisplayMetrics().density);
    }

    private boolean tienePermisoMicrofono() {
        return ContextCompat.checkSelfPermission(actividad, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void hablar() {
        if (gotita.escuchandoPregunta()) {
            gotita.parar();
        } else if (!tienePermisoMicrofono()) {
            alPedirMicrofono();
        } else {
            gotita.escucharPregunta();
        }
    }

    // ── Cuando la pantalla se abre, se pausa o se cierra ──

    void reanudar() {
        gotita.conectar(this);
        gotita.reanudar(tienePermisoMicrofono());
        alCambiarAjustes();
        alCambiarMensajes();
    }

    void pausar() {
        gotita.pausar();
        gotita.desconectar(this);
        detenerAnimaciones();
    }

    private void cerrarSiNadieLoUsa() {
        if (!panelAbierto()) return;
        String estado = gotita.estado();
        if (Gotita.PENSANDO.equals(estado) || Gotita.ESCUCHANDO.equals(estado) || Gotita.HABLANDO.equals(estado)) {
            marcarActividad(); // mientras piensa, escucha o habla no se esconde
        } else {
            panel.dismiss();
        }
    }

    private void marcarActividad() {
        principal.removeCallbacks(revisarInactividad);
        if (panelAbierto()) principal.postDelayed(revisarInactividad, INACTIVIDAD_MS);
    }

    void destruir() {
        gotita.desconectar(this);
        principal.removeCallbacksAndMessages(null);
        if (panel != null) panel.dismiss();
    }

    void alResultadoPermiso(int codigo, int[] resultados) {
        if (codigo != PERMISO_MICROFONO) return;
        boolean concedido = resultados.length > 0 && resultados[0] == PackageManager.PERMISSION_GRANTED;
        gotita.permisoMicrofono(concedido);
    }

    // ── Lo que Gotita le pide a la pantalla ──

    @Override
    public Activity actividad() {
        return actividad;
    }

    @Override
    public boolean panelAbierto() {
        return panel != null && panel.isShowing();
    }

    @Override
    public JsonArray secciones() {
        return GotitaPantalla.secciones(actividad, secciones);
    }

    @Override
    public void alMostrarSeccion(String id) {
        View parte = secciones.get(id);
        if (parte == null) return;
        // El panel tapa la pantalla: se esconde para que se vea la parte (la conversación sigue igual).
        if (panelAbierto()) panel.dismiss();
        principal.postDelayed(() -> GotitaPantalla.mostrar(parte), 250);
    }

    @Override
    public void alCambiarConversacion() {
        if (vistaPanel != null && vistaPanel.findViewById(R.id.gotitaHistorial).getVisibility() == View.VISIBLE) {
            cargarHistorial();
        }
    }

    @Override
    public void alPedirMicrofono() {
        ActivityCompat.requestPermissions(actividad, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISO_MICROFONO);
    }

    @Override
    public void alCambiarEstado(String estado) {
        marcarActividad();
        detenerAnimaciones();
        GradientDrawable fondo = (GradientDrawable) boton.getBackground();
        boolean escuchando = Gotita.ESCUCHANDO.equals(estado);
        fondo.setStroke(dp(escuchando ? 3 : 1), ContextCompat.getColor(actividad,
                escuchando ? R.color.agua : Gotita.VIGILANDO.equals(estado) ? R.color.agua_claro : R.color.line));
        switch (estado) {
            case Gotita.HABLANDO:
                principal.post(mover);
                break;
            case Gotita.ESCUCHANDO:
                animacion = ObjectAnimator.ofFloat(gota, View.SCALE_X, 1f, 1.1f);
                animacion.addUpdateListener(a -> gota.setScaleY((float) a.getAnimatedValue()));
                animacion.setDuration(600);
                break;
            case Gotita.PENSANDO:
                animacion = ObjectAnimator.ofFloat(gota, View.ROTATION, -7f, 7f);
                animacion.setDuration(450);
                break;
            default:
                animacion = ObjectAnimator.ofFloat(gota, View.TRANSLATION_Y, 0f, -dp(3));
                animacion.setDuration(1800);
                break;
        }
        if (animacion != null) {
            animacion.setRepeatMode(ValueAnimator.REVERSE);
            animacion.setRepeatCount(ValueAnimator.INFINITE);
            animacion.start();
        }
        if (vistaPanel != null) {
            ((TextView) vistaPanel.findViewById(R.id.gotitaEstado)).setText(gotita.t(estado));
            boolean ocupada = Gotita.PENSANDO.equals(estado) || Gotita.HABLANDO.equals(estado);
            vistaPanel.findViewById(R.id.gotitaParar).setVisibility(ocupada ? View.VISIBLE : View.GONE);
            ((ImageButton) vistaPanel.findViewById(R.id.gotitaMic)).setColorFilter(ContextCompat.getColor(actividad,
                    escuchando ? R.color.bad : R.color.anil));
        }
    }

    private void detenerAnimaciones() {
        principal.removeCallbacks(mover);
        if (animacion != null) {
            animacion.cancel();
            animacion = null;
        }
        bocaAbierta = false;
        gota.setImageResource(R.drawable.ic_gotita);
        gota.setScaleX(1f);
        gota.setScaleY(1f);
        gota.setRotation(0f);
        gota.setTranslationY(0f);
    }

    @Override
    public void alCambiarMensajes() {
        marcarActividad();
        if (vistaPanel == null) return;
        LinearLayout lista = vistaPanel.findViewById(R.id.gotitaChat);
        lista.removeAllViews();
        LayoutInflater inflador = LayoutInflater.from(actividad);
        if (gotita.mensajes().isEmpty()) {
            TextView hola = new TextView(actividad);
            hola.setText(gotita.t(gotita.esAdmin() ? "bienvenida_admin" : "bienvenida"));
            hola.setTextAppearance(R.style.TextAppearance_SmartDrop_Body);
            hola.setTextColor(ContextCompat.getColor(actividad, R.color.ink_2));
            hola.setGravity(Gravity.CENTER);
            hola.setPadding(dp(8), dp(24), dp(8), dp(8));
            lista.addView(hola);
        }
        for (Gotita.Mensaje mensaje : gotita.mensajes()) {
            boolean propio = "usuario".equals(mensaje.rol);
            View fila = inflador.inflate(propio ? R.layout.item_chat_propio : R.layout.item_chat_ajeno, lista, false);
            TextView autor = fila.findViewById(R.id.tvChatAutor);
            TextView texto = fila.findViewById(R.id.tvChatTexto);
            fila.findViewById(R.id.tvChatFecha).setVisibility(View.GONE);
            autor.setText(propio ? gotita.t("tu") : "aviso".equals(mensaje.rol) ? gotita.t("aviso") : gotita.t("autor"));
            texto.setText(mensaje.texto);
            if ("error".equals(mensaje.rol)) texto.setTextColor(ContextCompat.getColor(actividad, R.color.bad));
            if (mensaje.destino != null) {
                Button ver = new MaterialButton(actividad, null, androidx.appcompat.R.attr.borderlessButtonStyle);
                ver.setText(gotita.t("ver"));
                String destino = mensaje.destino;
                ver.setOnClickListener(v -> alNavegar(destino));
                ((LinearLayout) ((LinearLayout) fila).getChildAt(0)).addView(ver);
            }
            lista.addView(fila);
        }
        if (parcial != null && parcial.getParent() == null && parcial.getText().length() > 0) lista.addView(parcial);
        NestedScrollView scroll = vistaPanel.findViewById(R.id.gotitaScroll);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void alOirParcial(String texto) {
        if (texto == null) {
            if (parcial != null && parcial.getParent() != null) ((ViewGroup) parcial.getParent()).removeView(parcial);
            parcial = null;
            return;
        }
        if (!panelAbierto()) {
            alMostrarBurbuja(texto + "…", null, false);
            return;
        }
        if (parcial == null) {
            parcial = new TextView(actividad);
            parcial.setTextAppearance(R.style.TextAppearance_SmartDrop_Body);
            parcial.setTextColor(ContextCompat.getColor(actividad, R.color.ink_3));
            parcial.setGravity(Gravity.END);
            parcial.setPadding(dp(56), dp(4), dp(8), dp(4));
        }
        parcial.setText(texto + "…");
        if (parcial.getParent() == null) ((LinearLayout) vistaPanel.findViewById(R.id.gotitaChat)).addView(parcial);
        NestedScrollView scroll = vistaPanel.findViewById(R.id.gotitaScroll);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void alMostrarBurbuja(String texto, String destino, boolean esAviso) {
        if (panelAbierto()) return;
        destinoBurbuja = destino;
        burbujaTexto.setText(texto);
        burbujaVer.setText(gotita.t("ver"));
        burbujaVer.setVisibility(destino != null ? View.VISIBLE : View.GONE);
        LinearLayout tarjeta = (LinearLayout) burbuja.getChildAt(0);
        tarjeta.setBackgroundResource(esAviso ? R.drawable.bg_status_warn : R.drawable.bg_chat_ajeno);
        burbuja.setVisibility(View.VISIBLE);
        principal.removeCallbacks(ocultarBurbuja);
        principal.postDelayed(ocultarBurbuja, esAviso ? 20000 : 12000);
    }

    private void ocultarBurbuja() {
        principal.removeCallbacks(ocultarBurbuja);
        burbuja.setVisibility(View.GONE);
    }

    @Override
    public void alNavegar(String destino) {
        Intent intent = GotitaPantalla.intentPara(actividad, destino, gotita.esAdmin());
        if (intent == null) return;
        if (intent.getComponent() != null && actividad.getClass().getName().equals(intent.getComponent().getClassName())
                && !"reportes_admin".equals(destino)) {
            return; // Ya está en esa pantalla.
        }
        if (panel != null) panel.dismiss();
        actividad.startActivity(intent);
    }

    @Override
    public void alCambiarAjustes() {
        capa.setVisibility(gotita.activa() ? View.VISIBLE : View.GONE);
        if (vistaPanel == null) return;
        actualizando = true;
        ((MaterialButtonToggleGroup) vistaPanel.findViewById(R.id.gotitaModo))
                .check("tecnico".equals(gotita.modo()) ? R.id.gotitaModoTecnico : R.id.gotitaModoSencillo);
        ((MaterialButtonToggleGroup) vistaPanel.findViewById(R.id.gotitaIdioma))
                .check("en".equals(gotita.idioma()) ? R.id.gotitaIdiomaEn : R.id.gotitaIdiomaEs);
        ((MaterialButtonToggleGroup) vistaPanel.findViewById(R.id.gotitaVelocidad))
                .check(gotita.vozLenta() ? R.id.gotitaVelocidadLenta : R.id.gotitaVelocidadNormal);
        MaterialSwitch escucha = vistaPanel.findViewById(R.id.gotitaEscucha);
        escucha.setChecked(gotita.escuchaActiva());
        escucha.setEnabled(gotita.microfonoDisponible());
        ((MaterialSwitch) vistaPanel.findViewById(R.id.gotitaVoz)).setChecked(gotita.responderConVoz());
        ((MaterialSwitch) vistaPanel.findViewById(R.id.gotitaLeerAvisos)).setChecked(gotita.leerAvisos());
        ((EditText) vistaPanel.findViewById(R.id.gotitaTexto)).setHint(gotita.t("escribe"));
        ((TextView) vistaPanel.findViewById(R.id.gotitaEstado)).setText(gotita.t(gotita.estado()));
        ((TextView) vistaPanel.findViewById(R.id.gotitaHistorialTitulo)).setText(gotita.t("historial"));
        ((Button) vistaPanel.findViewById(R.id.gotitaNueva)).setText(gotita.t("nueva"));
        ((TextView) vistaPanel.findViewById(R.id.gotitaHistorialNota)).setText(gotita.t("nota_historial"));
        ((Button) vistaPanel.findViewById(R.id.gotitaHistorialTodo)).setText(gotita.t("borrar_todo"));
        pintarSugerencias();
        actualizando = false;
        marcarActividad();
    }

    // ── Panel ──

    private void abrirPanel() {
        ocultarBurbuja();
        if (panel == null) crearPanel();
        alCambiarAjustes();
        alCambiarMensajes();
        alCambiarEstado(gotita.estado());
        panel.show();
        marcarActividad();
    }

    private void crearPanel() {
        panel = new BottomSheetDialog(actividad);
        vistaPanel = LayoutInflater.from(actividad).inflate(R.layout.gotita_panel, null, false);
        panel.setContentView(vistaPanel);
        panel.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        panel.getBehavior().setSkipCollapsed(true);

        vistaPanel.findViewById(R.id.gotitaCerrar).setOnClickListener(v -> panel.dismiss());
        panel.setOnDismissListener(d -> principal.removeCallbacks(revisarInactividad));

        // Historial
        View historial = vistaPanel.findViewById(R.id.gotitaHistorial);
        vistaPanel.findViewById(R.id.gotitaHistorialBoton).setOnClickListener(v ->
                mostrarHistorial(historial.getVisibility() != View.VISIBLE));
        vistaPanel.findViewById(R.id.gotitaNueva).setOnClickListener(v -> {
            gotita.nuevaConversacion();
            mostrarHistorial(false);
        });
        vistaPanel.findViewById(R.id.gotitaHistorialTodo).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(actividad)
                        .setMessage(gotita.t("confirmar_borrar_todo"))
                        .setNegativeButton(gotita.t("no"), null)
                        .setPositiveButton(gotita.t("si"), (d, w) -> GotitaCliente.borrar(actividad, null, json -> {
                            gotita.nuevaConversacion();
                            cargarHistorial();
                        }))
                        .show());
        vistaPanel.findViewById(R.id.gotitaScroll).setOnScrollChangeListener(
                (View.OnScrollChangeListener) (v, x, y, viejoX, viejoY) -> marcarActividad());
        View ajustes = vistaPanel.findViewById(R.id.gotitaAjustes);
        vistaPanel.findViewById(R.id.gotitaAjustesBoton).setOnClickListener(v ->
                ajustes.setVisibility(ajustes.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        ((MaterialButtonToggleGroup) vistaPanel.findViewById(R.id.gotitaModo)).addOnButtonCheckedListener((g, id, marcado) -> {
            if (!actualizando && marcado) gotita.cambiarModo(id == R.id.gotitaModoTecnico ? "tecnico" : "sencillo", false);
        });
        ((MaterialButtonToggleGroup) vistaPanel.findViewById(R.id.gotitaIdioma)).addOnButtonCheckedListener((g, id, marcado) -> {
            if (!actualizando && marcado) gotita.cambiarIdioma(id == R.id.gotitaIdiomaEn ? "en" : "es", true);
        });
        ((MaterialButtonToggleGroup) vistaPanel.findViewById(R.id.gotitaVelocidad)).addOnButtonCheckedListener((g, id, marcado) -> {
            if (!actualizando && marcado) gotita.cambiarVelocidad(id == R.id.gotitaVelocidadLenta, false);
        });
        ((MaterialSwitch) vistaPanel.findViewById(R.id.gotitaEscucha)).setOnCheckedChangeListener((b, activo) -> {
            if (!actualizando) gotita.cambiarEscucha(activo, tienePermisoMicrofono());
        });
        ((MaterialSwitch) vistaPanel.findViewById(R.id.gotitaVoz)).setOnCheckedChangeListener((b, activo) -> {
            if (!actualizando) gotita.cambiarVoz(activo);
        });
        ((MaterialSwitch) vistaPanel.findViewById(R.id.gotitaLeerAvisos)).setOnCheckedChangeListener((b, activo) -> {
            if (!actualizando) gotita.cambiarLeerAvisos(activo);
        });
        vistaPanel.findViewById(R.id.gotitaBorrar).setOnClickListener(v -> gotita.borrarConversacion());

        EditText texto = vistaPanel.findViewById(R.id.gotitaTexto);
        // Una pregunta larga se ve en varias líneas, pero la tecla Enter la envía.
        texto.setHorizontallyScrolling(false);
        texto.setMaxLines(4);
        texto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int i, int a, int b) { }
            @Override public void onTextChanged(CharSequence c, int i, int a, int b) { marcarActividad(); }
            @Override public void afterTextChanged(Editable e) { }
        });
        View.OnClickListener enviar = v -> {
            String pregunta = texto.getText().toString();
            texto.setText("");
            gotita.preguntar(pregunta, false);
        };
        vistaPanel.findViewById(R.id.gotitaEnviar).setOnClickListener(enviar);
        texto.setOnEditorActionListener((v, accion, evento) -> {
            boolean enter = evento != null && evento.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && evento.getAction() == android.view.KeyEvent.ACTION_DOWN;
            if (accion == EditorInfo.IME_ACTION_SEND || enter) {
                enviar.onClick(v);
                return true;
            }
            return false;
        });
        vistaPanel.findViewById(R.id.gotitaMic).setOnClickListener(v -> hablar());
        vistaPanel.findViewById(R.id.gotitaParar).setOnClickListener(v -> gotita.parar());
    }

    private void pintarSugerencias() {
        if (vistaPanel == null) return;
        ChipGroup grupo = vistaPanel.findViewById(R.id.gotitaSugerencias);
        grupo.removeAllViews();
        Chip explicar = new Chip(actividad);
        explicar.setCheckable(false);
        explicar.setText(gotita.t("explicar_pantalla"));
        explicar.setOnClickListener(v -> gotita.preguntar(gotita.t("explicar_pantalla"), true));
        grupo.addView(explicar);
        for (String sugerencia : gotita.sugerencias(GotitaPantalla.id(actividad))) {
            Chip chip = new Chip(actividad);
            chip.setCheckable(false);
            chip.setText(sugerencia);
            chip.setOnClickListener(v -> gotita.preguntar(sugerencia, false));
            grupo.addView(chip);
        }
    }

    // ── Historial de conversaciones ──

    private void mostrarHistorial(boolean mostrar) {
        if (vistaPanel == null) return;
        vistaPanel.findViewById(R.id.gotitaHistorial).setVisibility(mostrar ? View.VISIBLE : View.GONE);
        if (mostrar) cargarHistorial();
        marcarActividad();
    }

    private void cargarHistorial() {
        GotitaCliente.conversaciones(actividad, json -> {
            if (vistaPanel == null) return;
            LinearLayout lista = vistaPanel.findViewById(R.id.gotitaHistorialLista);
            lista.removeAllViews();
            if (json == null || !json.has("conversaciones")) {
                lista.addView(textoSimple(gotita.t("historial_error")));
                return;
            }
            JsonArray conversaciones = json.getAsJsonArray("conversaciones");
            if (conversaciones.size() == 0) {
                lista.addView(textoSimple(gotita.t("sin_historial")));
                return;
            }
            for (JsonElement elemento : conversaciones) lista.addView(filaHistorial(elemento.getAsJsonObject()));
        });
    }

    private TextView textoSimple(String contenido) {
        TextView texto = new TextView(actividad);
        texto.setText(contenido);
        texto.setTextAppearance(R.style.TextAppearance_SmartDrop_Caption);
        texto.setPadding(dp(8), dp(12), dp(8), dp(12));
        return texto;
    }

    private View filaHistorial(JsonObject conversacion) {
        int id = conversacion.get("id").getAsInt();
        String titulo = conversacion.has("titulo") ? conversacion.get("titulo").getAsString() : "";
        Integer actual = gotita.conversacionId();

        LinearLayout fila = new LinearLayout(actividad);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(dp(8), dp(4), 0, dp(4));
        if (actual != null && actual == id) fila.setBackgroundResource(R.color.agua_soft);

        LinearLayout textos = new LinearLayout(actividad);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView nombre = new TextView(actividad);
        nombre.setText(titulo);
        nombre.setTextAppearance(R.style.TextAppearance_SmartDrop_BodyStrong);
        nombre.setMaxLines(1);
        nombre.setEllipsize(TextUtils.TruncateAt.END);
        TextView cuando = new TextView(actividad);
        cuando.setText(fechaCorta(conversacion.has("actualizada") ? conversacion.get("actualizada").getAsString() : null));
        cuando.setTextAppearance(R.style.TextAppearance_SmartDrop_Caption);
        textos.addView(nombre);
        textos.addView(cuando);
        textos.setClickable(true);
        textos.setFocusable(true);
        textos.setOnClickListener(v -> GotitaCliente.conversacion(actividad, id, json -> {
            if (json == null) {
                Toast.makeText(actividad, gotita.t("historial_error"), Toast.LENGTH_SHORT).show();
                return;
            }
            gotita.abrirConversacion(json);
            mostrarHistorial(false);
        }));
        fila.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton borrar = new ImageButton(actividad);
        borrar.setImageResource(R.drawable.ic_trash);
        borrar.setColorFilter(ContextCompat.getColor(actividad, R.color.ink_2));
        borrar.setBackgroundResource(android.R.color.transparent);
        borrar.setContentDescription(gotita.t("borrar_una") + ": " + titulo);
        borrar.setOnClickListener(v -> GotitaCliente.borrar(actividad, id, json -> {
            Integer ahora = gotita.conversacionId();
            if (ahora != null && ahora == id) gotita.nuevaConversacion();
            cargarHistorial();
        }));
        fila.addView(borrar, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return fila;
    }

    private String fechaCorta(String iso) {
        Date fecha = Fechas.parse(iso);
        if (fecha == null) return "";
        if (DateUtils.isToday(fecha.getTime())) {
            return DateUtils.formatDateTime(actividad, fecha.getTime(), DateUtils.FORMAT_SHOW_TIME);
        }
        if (DateUtils.isToday(fecha.getTime() + DateUtils.DAY_IN_MILLIS)) return gotita.t("ayer");
        return DateUtils.formatDateTime(actividad, fecha.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
    }
}
