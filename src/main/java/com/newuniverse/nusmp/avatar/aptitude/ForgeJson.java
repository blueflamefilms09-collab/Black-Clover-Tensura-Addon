package com.newuniverse.nusmp.avatar.aptitude;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/**
 * Why: the aptitude files and the skill files of the Soul Forge are written by hand by the server owner and by datapack makers, so both parsers need the
 * same strict-but-friendly readers (a wrong type names the file and the key instead of throwing a bare Gson exception, a colour may be written
 * "#B04A4A", "0xB04A4A" or as a number). Keeping them in one small pure class means the two parsers cannot drift apart. Gson only, no Minecraft.
 */
final class ForgeJson {
    private ForgeJson() {}

    static String str(String who, JsonObject o, String key, String fallback) {
        if (!o.has(key)) return fallback;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive()) throw new IllegalArgumentException(who + ": '" + key + "' must be text");
        return e.getAsString();
    }

    static double num(String who, JsonObject o, String key, double fallback) {
        if (!o.has(key)) return fallback;
        JsonElement e = o.get(key);
        try {
            double v = e.getAsDouble();
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new IllegalArgumentException(who + ": '" + key + "' is not a finite number");
            return v;
        } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException ex) {
            throw new IllegalArgumentException(who + ": '" + key + "' is not a number", ex);
        }
    }

    /** A colour as 0xRRGGBB: "#RRGGBB", "0xRRGGBB" or a plain number; the fallback when the key is missing. */
    static int colour(String who, JsonObject o, String key, int fallback) {
        if (!o.has(key)) return fallback;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive()) throw new IllegalArgumentException(who + ": '" + key + "' must be a colour");
        JsonPrimitive p = e.getAsJsonPrimitive();
        try {
            if (p.isNumber()) return p.getAsInt() & 0xFFFFFF;
            String s = p.getAsString().trim().toLowerCase(Locale.ROOT);
            if (s.startsWith("#")) s = s.substring(1);
            else if (s.startsWith("0x")) s = s.substring(2);
            if (s.length() != 6) throw new NumberFormatException(s);
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(who + ": '" + key + "' is not a colour (use #RRGGBB)", ex);
        }
    }

    /** True for "namespace:path" in the lower-case alphabet resource locations allow. */
    static boolean looksLikeResourceId(String s) {
        return s != null && s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }
}
