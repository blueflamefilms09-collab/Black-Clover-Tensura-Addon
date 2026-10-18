package com.newuniverse.nusmp.elite.world.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: one calamity is one JSON file ({@code data/nusmp/elite/world/calamities/<id>.json}), so a datapack can add, retune or switch off a calamity without code. This is the parsed, pure
 * form: which boss entity, how often, what wakes it (the triggers) and the words of its announcements. The designs (numbers, triggers, words) are the mod's own; the original is not available.
 *
 * <pre>
 * { "id": "gravebound_colossus", "entity": "nusmp:gravebound_colossus", "name": "Gravebound Colossus", "enabled": true, "weight": 3,
 *   "cooldownMinutes": 120, "warnSeconds": 20, "healthMult": 1.0, "damageMult": 1.0, "phases": [0.66, 0.33],
 *   "dimensions": ["minecraft:overworld"], "color": "#8B7B5A", "accent": "#D8C9A0",
 *   "triggers": [ {"type":"sustained_magicule","density":0.9,"seconds":1200}, {"type":"mother_rocks","count":3,"radius":48,"density":0.7,"seconds":120},
 *                 {"type":"night","density":0.6,"moonPhase":-1,"chance":0.02,"rocks":1}, {"type":"ritual"}, {"type":"command"} ],
 *   "announce": {"omen":"...","spawn":"...","phase":"...","defeat":"..."} }
 * </pre>
 */
public record CalamityDef(String id, String entity, String name, boolean enabled, int weight, int cooldownMinutes, int warnSeconds, double healthMult, double damageMult,
                          List<Double> phases, List<String> dimensions, int color, int accent, List<Trigger> triggers, Announce announce) {

    public enum Type {
        SUSTAINED_MAGICULE, MOTHER_ROCKS, NIGHT, RITUAL, COMMAND;

        public static Type of(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** One trigger. Unused fields stay at their defaults for the types that do not read them. */
    public record Trigger(Type type, double density, int seconds, int count, int radius, int moonPhase, double chance) {
        /** Does this trigger fire by itself (as opposed to a ritual or a command, which a person starts)? */
        public boolean automatic() { return type != Type.RITUAL && type != Type.COMMAND; }
    }

    /** The English words of the announcements (the lang keys {@code nusmp.elite.calamity.<id>.<kind>} translate them; these go in the events for other systems, e.g. the voice). */
    public record Announce(String omen, String spawn, String phase, String defeat) {}

    public CalamityDef {
        phases = phases == null ? List.of() : List.copyOf(phases);
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
        weight = Math.max(1, weight);
        cooldownMinutes = Math.max(0, cooldownMinutes);
        warnSeconds = Math.max(0, warnSeconds);
        healthMult = healthMult <= 0 ? 1.0 : healthMult;
        damageMult = damageMult <= 0 ? 1.0 : damageMult;
    }

    public boolean allowsDimension(String dim) { return dimensions.isEmpty() || dimensions.contains(dim); }

    public boolean hasTrigger(Type t) {
        for (Trigger x : triggers) if (x.type() == t) return true;
        return false;
    }

    public long cooldownTicks() { return cooldownMinutes * 60L * 20L; }

    /** Parses one file. Returns null when it has no id or entity (the loader logs and skips it). */
    public static CalamityDef parse(JsonObject o) {
        if (o == null || !o.has("id") || !o.has("entity")) return null;
        String id = o.get("id").getAsString();
        if (!id.matches("[a-z][a-z0-9_]*")) return null;
        List<Double> phases = new ArrayList<>();
        if (o.has("phases") && o.get("phases").isJsonArray()) for (JsonElement e : o.getAsJsonArray("phases")) phases.add(e.getAsDouble());
        phases.sort((a, b) -> Double.compare(b, a));                      // descending: 0.66, 0.33
        List<String> dims = new ArrayList<>();
        if (o.has("dimensions") && o.get("dimensions").isJsonArray()) for (JsonElement e : o.getAsJsonArray("dimensions")) dims.add(e.getAsString());
        List<Trigger> triggers = new ArrayList<>();
        if (o.has("triggers") && o.get("triggers").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("triggers");
            for (JsonElement e : arr) {
                if (!e.isJsonObject()) continue;
                JsonObject t = e.getAsJsonObject();
                Type type = Type.of(t.has("type") ? t.get("type").getAsString() : null);
                if (type == null) continue;
                triggers.add(new Trigger(type, num(t, "density", 0.9), (int) num(t, "seconds", 0), (int) num(t, "count", t.has("rocks") ? num(t, "rocks", 1) : 1),
                        (int) num(t, "radius", 48), (int) num(t, "moonPhase", -1), num(t, "chance", 1.0)));
            }
        }
        Announce ann = new Announce("", "", "", "");
        if (o.has("announce") && o.get("announce").isJsonObject()) {
            JsonObject a = o.getAsJsonObject("announce");
            ann = new Announce(str(a, "omen"), str(a, "spawn"), str(a, "phase"), str(a, "defeat"));
        }
        String name = o.has("name") ? o.get("name").getAsString() : id;
        return new CalamityDef(id, o.get("entity").getAsString(), name, !o.has("enabled") || o.get("enabled").getAsBoolean(), (int) num(o, "weight", 1), (int) num(o, "cooldownMinutes", 120),
                (int) num(o, "warnSeconds", 20), num(o, "healthMult", 1.0), num(o, "damageMult", 1.0), phases, dims, rgb(o, "color", 0x8B7B5A), rgb(o, "accent", 0xD8C9A0), triggers, ann);
    }

    private static double num(JsonObject o, String k, double d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : d; }

    private static String str(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : ""; }

    private static int rgb(JsonObject o, String k, int d) {
        if (!o.has(k)) return d;
        try {
            String s = o.get(k).getAsString().trim();
            if (s.startsWith("#")) s = s.substring(1);
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (RuntimeException e) {
            return d;
        }
    }
}
