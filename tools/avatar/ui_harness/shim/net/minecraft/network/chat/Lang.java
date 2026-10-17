package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Why: harness shim: the language table and the %s formatting of Minecraft's translatable text. */
public final class Lang {
    private Lang() {}
    private static final Map<String, String> TABLE = new HashMap<>();

    static {
        for (String f : System.getProperty("harness.lang", "").split(",")) {
            if (f.isBlank()) continue;
            try {
                JsonObject o = JsonParser.parseString(Files.readString(Path.of(f.strip()))).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) TABLE.put(e.getKey(), e.getValue().getAsString());
            } catch (Exception e) {
                System.err.println("harness: cannot read " + f + ": " + e);
            }
        }
    }

    static String get(String key, String fallback) { return TABLE.getOrDefault(key, fallback == null ? key : fallback); }

    static String format(String template, Object[] args) {
        StringBuilder b = new StringBuilder();
        int next = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c != '%' || i + 1 >= template.length()) { b.append(c); continue; }
            char n = template.charAt(i + 1);
            if (n == '%') { b.append('%'); i++; continue; }
            int j = i + 1;
            int index = -1;
            while (j < template.length() && Character.isDigit(template.charAt(j))) j++;
            if (j < template.length() && template.charAt(j) == '$' && j > i + 1) { index = Integer.parseInt(template.substring(i + 1, j)) - 1; j++; }
            else j = i + 1;
            if (j < template.length() && (template.charAt(j) == 's' || template.charAt(j) == 'd')) {
                int at = index >= 0 ? index : next++;
                Object a = at < args.length ? args[at] : "";
                b.append(a instanceof Component comp ? comp.getString() : String.valueOf(a));
                i = j;
            } else b.append(c);
        }
        return b.toString();
    }
}
