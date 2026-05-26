package com.wallet.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal, zero-dependency JSON serialiser/deserialiser for flat objects.
 *
 * <p>Supports only top-level key–value pairs where values are one of:
 * {@code String}, {@code BigDecimal}, {@code Boolean}, or {@code null}.
 * Nested objects and arrays are not supported.
 */
public final class Json {

    private Json() {}

    /**
     * Parses a flat JSON object string into a {@link Map}.
     * Values are typed as {@link String}, {@link BigDecimal}, {@link Boolean}, or {@code null}.
     *
     * @param json JSON text to parse
     * @return ordered map of key–value pairs
     * @throws IllegalArgumentException if the input is not a JSON object or is malformed
     */
    public static Map<String, Object> parse(String json) {
        json = json.strip();
        if (json.isEmpty() || json.charAt(0) != '{') {
            throw new IllegalArgumentException("Expected JSON object");
        }
        var result = new LinkedHashMap<String, Object>();
        int i = 1;
        int len = json.length();

        while (i < len) {
            i = skipWhitespaceAndCommas(json, i, len);
            if (i >= len || json.charAt(i) == '}') break;

            // key
            if (json.charAt(i) != '"') throw new IllegalArgumentException("Expected key at index " + i);
            i++;
            int ks = i;
            while (i < len && json.charAt(i) != '"') { if (json.charAt(i) == '\\') i++; i++; }
            String key = unescape(json.substring(ks, i));
            i++; // closing quote

            // colon
            while (i < len && (json.charAt(i) == ' ' || json.charAt(i) == ':')) i++;

            // value
            char c = json.charAt(i);
            if (c == '"') {
                i++;
                int vs = i;
                while (i < len && json.charAt(i) != '"') { if (json.charAt(i) == '\\') i++; i++; }
                result.put(key, unescape(json.substring(vs, i)));
                i++;
            } else if (c == 'n') { result.put(key, null);         i += 4;
            } else if (c == 't') { result.put(key, Boolean.TRUE);  i += 4;
            } else if (c == 'f') { result.put(key, Boolean.FALSE); i += 5;
            } else {
                int ns = i;
                while (i < len && "0123456789.-eE+".indexOf(json.charAt(i)) >= 0) i++;
                result.put(key, new BigDecimal(json.substring(ns, i)));
            }
        }
        return result;
    }

    /**
     * Serialises a map to a JSON object string.
     * Supports {@link String}, {@link Boolean}, {@code null}, and anything whose
     * {@link Object#toString()} produces a valid JSON literal (e.g. {@link BigDecimal}).
     *
     * @param fields ordered map of key–value pairs to serialise
     * @return JSON object string
     */
    public static String toJson(Map<String, ?> fields) {
        var sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : fields.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(entry.getKey())).append("\":");
            appendValue(sb, entry.getValue());
        }
        return sb.append('}').toString();
    }

    private static void appendValue(StringBuilder sb, Object v) {
        if (v == null)                  sb.append("null");
        else if (v instanceof String s) sb.append('"').append(escape(s)).append('"');
        else if (v instanceof Boolean b) sb.append(b);
        else                            sb.append(v);
    }

    private static int skipWhitespaceAndCommas(String s, int i, int len) {
        while (i < len) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ',') i++;
            else break;
        }
        return i;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\")
                .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }
}
