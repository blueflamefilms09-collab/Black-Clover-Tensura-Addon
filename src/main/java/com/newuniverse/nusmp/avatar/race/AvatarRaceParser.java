package com.newuniverse.nusmp.avatar.race;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.dnd.Stat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: race files are written by hand by the server owner and by datapack makers, so the reader must be strict about what is wrong (a bad number, an unknown
 * personality) and tolerant about what is merely missing (every field has a default). It is pure (Gson only, no Minecraft) so the shipped files and bad
 * files are both unit tested; a file that throws here is skipped and logged by {@link AvatarRaces}, never allowed to break the reload.
 */
public final class AvatarRaceParser {
    private AvatarRaceParser() {}

    public static AvatarRace parse(String id, JsonObject o) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("race id is empty");
        String display = str(o, "display_name", id);
        String description = str(o, "description", "");
        float scale = (float) num(o, "default_scale", 1.0);
        if (!(scale >= 0.3f && scale <= 4.0f)) throw new IllegalArgumentException(id + ": default_scale must be between 0.3 and 4.0, got " + scale);
        Map<String, Double> attrs = new LinkedHashMap<>();
        if (o.has("base_attributes")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("base_attributes").entrySet()) attrs.put(e.getKey(), asDouble(id, e.getKey(), e.getValue()));
        Map<String, Double> mult = new LinkedHashMap<>();
        if (o.has("stat_multipliers")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("stat_multipliers").entrySet()) {
                Stat s = Stat.byName(e.getKey());
                if (s == null) throw new IllegalArgumentException(id + ": unknown stat '" + e.getKey() + "'");
                double v = asDouble(id, e.getKey(), e.getValue());
                if (!(v > 0.0 && v <= 5.0)) throw new IllegalArgumentException(id + ": stat multiplier out of range (0, 5]: " + v);
                mult.put(s.name().toLowerCase(java.util.Locale.ROOT), v);
            }
        }
        double cost = Math.max(0.0, num(o, "base_magicule_cost", 0.0)), ep = num(o, "base_ep", 100.0);
        if (!(ep > 0.0)) throw new IllegalArgumentException(id + ": base_ep must be positive");
        String evolves = idOnly(str(o, "evolves_to_race_id", ""));
        boolean dnd = bool(o, "requires_dnd_roll", true), selectable = bool(o, "selectable", true);
        List<String> conditions = new ArrayList<>();
        if (o.has("connection_conditions")) for (JsonElement e : o.getAsJsonArray("connection_conditions")) conditions.add(e.getAsString());
        List<AvatarPersonality> allowed = new ArrayList<>();
        if (o.has("allowed_personalities")) {
            for (JsonElement e : o.getAsJsonArray("allowed_personalities")) {
                AvatarPersonality p = AvatarPersonality.byName(e.getAsString());
                if (p == null) throw new IllegalArgumentException(id + ": unknown personality '" + e.getAsString() + "'");
                allowed.add(p);
            }
        }
        Map<String, Integer> hints = new LinkedHashMap<>();
        if (o.has("appearance")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("appearance").entrySet()) hints.put(e.getKey(), asColorOrInt(id, e.getKey(), e.getValue()));
        return new AvatarRace(id, display, description, scale, Map.copyOf(attrs), Map.copyOf(mult), cost, ep, evolves, dnd, selectable, List.copyOf(conditions), List.copyOf(allowed), Map.copyOf(hints));
    }

    /** "nusmp:goblin" and "goblin" are the same race. */
    static String idOnly(String s) {
        int c = s.indexOf(':');
        return c >= 0 ? s.substring(c + 1) : s;
    }

    private static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }

    private static double num(JsonObject o, String k, double d) { return o.has(k) ? asDouble("", k, o.get(k)) : d; }

    private static boolean bool(JsonObject o, String k, boolean d) { return o.has(k) ? o.get(k).getAsBoolean() : d; }

    private static double asDouble(String id, String key, JsonElement e) {
        try {
            double v = e.getAsDouble();
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new IllegalArgumentException(id + ": '" + key + "' is not a finite number");
            return v;
        } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException ex) {
            throw new IllegalArgumentException(id + ": '" + key + "' is not a number", ex);
        }
    }

    private static int asColorOrInt(String id, String key, JsonElement e) {
        try {
            if (e.getAsJsonPrimitive().isString()) {
                String s = e.getAsString().trim();
                if (s.startsWith("#")) return Integer.parseInt(s.substring(1), 16);
                if (s.startsWith("0x") || s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16);
                return Integer.parseInt(s);
            }
            return e.getAsInt();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(id + ": appearance '" + key + "' is not a colour or a number", ex);
        }
    }

    /** Convenience for tests and tools. */
    public static JsonArray emptyArray() { return new JsonArray(); }
}
