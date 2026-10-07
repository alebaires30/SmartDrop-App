package com.example.smartdrop;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Modelos del perfil, las preferencias y los avisos (auth/perfil/, auth/preferencias/, api/notificaciones/). */
public final class Perfil {

    private Perfil() { }

    /** Datos del perfil, los mismos que muestra "Mi perfil" en la web. */
    public static class Datos {
        @SerializedName("nombre") public String nombre;
        @SerializedName("apellido") public String apellido;
        @SerializedName("nombre_completo") public String nombreCompleto;
        @SerializedName("iniciales") public String iniciales;
        @SerializedName("correo") public String correo;
        @SerializedName("rol_label") public String rolLabel;
        @SerializedName("es_admin") public boolean esAdmin;
        @SerializedName("direccion") public String direccion;
        @SerializedName("telefono") public String telefono;
        @SerializedName("miembro_desde") public String miembroDesde;
        @SerializedName("preferencias") public Preferencias preferencias;
        @SerializedName("reporte_semanal") public ReporteSemanal reporteSemanal;
    }

    /** Preferencias compartidas con la web. Los nulos se omiten al enviar un cambio parcial. */
    public static class Preferencias {
        @SerializedName("alertas_nivel") public Boolean alertasNivel;
        @SerializedName("suministro") public Boolean suministro;
        @SerializedName("calidad") public Boolean calidad;
        @SerializedName("consumo_elevado") public Boolean consumoElevado;
        @SerializedName("modo_oscuro") public Boolean modoOscuro;
        @SerializedName("reportes_semanales") public Boolean reportesSemanales;
    }

    public static class ReporteSemanal {
        @SerializedName("mensaje") public String mensaje;
        @SerializedName("total_litros") public Double totalLitros;
        @SerializedName("variacion_porcentual") public Double variacionPorcentual;
    }

    public static class PreferenciasResponse {
        @SerializedName("ok") public boolean ok;
        @SerializedName("preferencias") public Preferencias preferencias;
        @SerializedName("reporte_semanal") public ReporteSemanal reporteSemanal;
    }

    public static class EdicionResponse {
        @SerializedName("ok") public boolean ok;
        @SerializedName("mensaje") public String mensaje;
        @SerializedName("error") public String error;
        @SerializedName("perfil") public Datos perfil;
    }

    /** Aviso para notificación push: `sensor:*` trae el estado actual, `alerta:*` una alerta nueva. */
    public static class Aviso {
        @SerializedName("clave") public String clave;
        @SerializedName("tipo") public String tipo;
        @SerializedName("estado") public String estado;
        @SerializedName("titulo") public String titulo;
        @SerializedName("mensaje") public String mensaje;
    }

    public static class AvisosResponse {
        @SerializedName("ok") public boolean ok;
        @SerializedName("preferencias") public Preferencias preferencias;
        @SerializedName("avisos") public List<Aviso> avisos;
    }
}
