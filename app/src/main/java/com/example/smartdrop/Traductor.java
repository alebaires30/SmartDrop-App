package com.example.smartdrop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduce textos de la interfaz con el catálogo español→inglés del servidor (el mismo que usa la web).
 *
 * Orden: frase exacta → plantilla con huecos ("Hay {0} alertas") → por segmentos ("Última lectura: 10:30").
 * Lo que no está en el catálogo (nombres, direcciones, texto escrito por el usuario) se deja igual.
 * Es Java puro para poder probarlo sin Android.
 */
final class Traductor {

    private static final Pattern HUECO = Pattern.compile("\\{(\\d+)\\}");
    /** Separadores entre segmentos traducibles por separado (se conservan tal cual). */
    private static final Pattern SEPARADOR = Pattern.compile(
            "(\\s*[:·•●|\\n]\\s*|\\s+[-–—]\\s+|\\s*\\(\\s*|\\s*\\)\\s*|\\s*,\\s+|\\s+→\\s+)");
    private static final Pattern FINAL = Pattern.compile("^(.*?)([.…!?:]+)$", Pattern.DOTALL);
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern SIN_LETRAS = Pattern.compile("^[^\\p{L}]*$");
    private static final int MAX_CACHE = 600;

    private final Map<String, String> frases = new HashMap<>();
    private final Map<String, String> frasesMayusculas = new HashMap<>();
    private final List<Plantilla> plantillas = new ArrayList<>();
    private final Map<String, String> cache = Collections.synchronizedMap(new LinkedHashMap<String, String>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHE;
        }
    });

    private static final class Plantilla {
        final Pattern patron;
        final String ingles;

        Plantilla(Pattern patron, String ingles) {
            this.patron = patron;
            this.ingles = ingles;
        }
    }

    /** Catálogo con el formato {"frases": {es: en}, "plantillas": {"es {0}": "en {0}"}}. */
    static Traductor desdeJson(String json) {
        Traductor traductor = new Traductor();
        JsonObject raiz = new JsonParser().parse(json).getAsJsonObject();
        if (raiz.has("frases")) {
            for (Map.Entry<String, JsonElement> frase : raiz.getAsJsonObject("frases").entrySet()) {
                traductor.agregarFrase(frase.getKey(), frase.getValue().getAsString());
            }
        }
        if (raiz.has("plantillas")) {
            List<Map.Entry<String, JsonElement>> entradas = new ArrayList<>(raiz.getAsJsonObject("plantillas").entrySet());
            // Las plantillas con más texto fijo van primero (son más específicas).
            Collections.sort(entradas, (a, b) -> literal(b.getKey()) - literal(a.getKey()));
            for (Map.Entry<String, JsonElement> plantilla : entradas) {
                traductor.agregarPlantilla(plantilla.getKey(), plantilla.getValue().getAsString());
            }
        }
        return traductor;
    }

    private static int literal(String plantilla) {
        return HUECO.matcher(plantilla).replaceAll("").length();
    }

    void agregarFrase(String es, String en) {
        frases.put(es, en);
        frasesMayusculas.put(es.toUpperCase(Locale.ROOT), en.toUpperCase(Locale.ROOT));
    }

    void agregarPlantilla(String es, String en) {
        StringBuilder regex = new StringBuilder("^");
        Matcher hueco = HUECO.matcher(es);
        int desde = 0;
        while (hueco.find()) {
            regex.append(Pattern.quote(es.substring(desde, hueco.start()))).append("(.+?)");
            desde = hueco.end();
        }
        regex.append(Pattern.quote(es.substring(desde))).append("$");
        plantillas.add(new Plantilla(Pattern.compile(regex.toString(), Pattern.DOTALL), en));
    }

    boolean vacio() {
        return frases.isEmpty() && plantillas.isEmpty();
    }

    /** Texto traducido, o el mismo texto si no hay traducción. */
    String traducir(String texto) {
        if (texto == null || texto.isEmpty() || SIN_LETRAS.matcher(texto).matches()) return texto;
        String guardado = cache.get(texto);
        if (guardado != null) return guardado;
        String resultado = conEspacios(texto, true);
        cache.put(texto, resultado);
        return resultado;
    }

    /** Conserva los espacios del principio y del final; traduce el núcleo. */
    private String conEspacios(String texto, boolean porSegmentos) {
        int inicio = 0;
        int fin = texto.length();
        while (inicio < fin && Character.isWhitespace(texto.charAt(inicio))) inicio++;
        while (fin > inicio && Character.isWhitespace(texto.charAt(fin - 1))) fin--;
        if (inicio == fin) return texto;
        String nucleo = texto.substring(inicio, fin);
        String traducido = nucleo(nucleo);
        if (traducido == null && porSegmentos) traducido = segmentos(nucleo);
        return traducido == null ? texto : texto.substring(0, inicio) + traducido + texto.substring(fin);
    }

    /** Frase exacta, en mayúsculas, sin la puntuación final o por plantilla; null si no hay. */
    private String nucleo(String texto) {
        String exacta = exacta(texto);
        if (exacta != null) return exacta;
        // Textos repartidos en varias líneas: se comparan con los espacios normalizados.
        String compacto = ESPACIOS.matcher(texto).replaceAll(" ");
        if (!compacto.equals(texto)) return nucleo(compacto);
        Matcher puntuacion = FINAL.matcher(texto);
        if (puntuacion.matches() && !puntuacion.group(1).isEmpty()) {
            String sinPunto = exacta(puntuacion.group(1));
            if (sinPunto != null) return sinPunto + puntuacion.group(2);
        }
        for (Plantilla plantilla : plantillas) {
            Matcher coincidencia = plantilla.patron.matcher(texto);
            if (!coincidencia.matches()) continue;
            // Cada hueco se rellena con su valor, traducido si también es un texto del catálogo ("Baja",
            // otra frase…).
            Matcher hueco = HUECO.matcher(plantilla.ingles);
            StringBuffer salida = new StringBuffer();
            while (hueco.find()) {
                int grupo = Integer.parseInt(hueco.group(1)) + 1;
                String valor = grupo <= coincidencia.groupCount() ? coincidencia.group(grupo) : "";
                hueco.appendReplacement(salida, Matcher.quoteReplacement(conEspacios(valor, true)));
            }
            hueco.appendTail(salida);
            return salida.toString();
        }
        return null;
    }

    private String exacta(String texto) {
        String directa = frases.get(texto);
        if (directa != null) return directa;
        if (texto.equals(texto.toUpperCase(Locale.ROOT)) && !texto.equals(texto.toLowerCase(Locale.ROOT))) {
            return frasesMayusculas.get(texto);
        }
        return null;
    }

    /** Traduce cada parte entre separadores ("Última lectura: 10:30 AM" → "Last reading: 10:30 AM"). */
    private String segmentos(String texto) {
        Matcher separador = SEPARADOR.matcher(texto);
        StringBuilder salida = new StringBuilder();
        int desde = 0;
        boolean cambio = false;
        while (separador.find()) {
            String parte = texto.substring(desde, separador.start());
            String traducida = parte.isEmpty() ? parte : conEspacios(parte, false);
            cambio |= !traducida.equals(parte);
            salida.append(traducida).append(separador.group());
            desde = separador.end();
        }
        if (desde == 0) return null;
        String resto = texto.substring(desde);
        String restoTraducido = resto.isEmpty() ? resto : conEspacios(resto, false);
        cambio |= !restoTraducido.equals(resto);
        salida.append(restoTraducido);
        return cambio ? salida.toString() : null;
    }
}
