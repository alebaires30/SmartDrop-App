package com.example.smartdrop;

import android.webkit.MimeTypeMap;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Adjunto {
    // Formatos de evidencia que acepta el servidor; los reconoce por la extensión del nombre del archivo.
    private static final Set<String> EXTENSIONES = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "mp4", "webm", "mov", "3gp"));
    public static final String FORMATO_NO_PERMITIDO =
            "Formato no permitido. Usa una foto (JPG, PNG, GIF, WEBP) o un video (MP4, WEBM, MOV, 3GP).";

    @SerializedName("id_adjunto") private int idAdjunto;
    @SerializedName("url") private String url;
    @SerializedName("tipo") private String tipo;
    @SerializedName("nombre") private String nombre;

    public int getIdAdjunto() { return idAdjunto; }
    public String getUrl() { return url; }
    public String getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public boolean esVideo() { return "video".equals(tipo); }

    /** Extensión (sin punto) para subir un archivo de este tipo MIME, o null si el servidor no la acepta. */
    public static String extensionPara(String mime) {
        if (mime == null) return null;
        mime = mime.toLowerCase(Locale.ROOT);
        if ("image/jpeg".equals(mime)) return "jpg";
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if (extension == null) return null;
        extension = extension.toLowerCase(Locale.ROOT);
        return EXTENSIONES.contains(extension) ? extension : null;
    }
}
