package com.newuniverse.nusmp.elite.items.weapon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the ability files are data from a datapack, so they are parsed defensively and with no Minecraft types (testable): every number is range-checked, a wrong technique kind or a malformed effect
 * is reported (and the effect dropped) instead of crashing, and a file with no usable primary technique is refused as a whole (the loader then keeps the built-in copy of that weapon's file).
 *
 * <pre>
 * { "item": "nusmp:void_edge",
 *   "primary": { "kind": "rift_pull", "name": "Rift Cut", "desc": "...", "cooldown": 200, "damage": 9, "range": 10, "radius": 5, "pull": 0.9,
 *                "color": "8A4BFF", "dark": true, "vfx": "SPATIAL_RIFT", "effects": [ { "id": "minecraft:blindness", "ticks": 40 } ] },
 *   "alt": { ... },
 *   "swing": { "damage": 5, "effects": [], "strip": 1 },
 *   "skill": "nusmp:void_edge", "skill_multiplier": 1.35 }
 * </pre>
 */
public final class AbilityParser {
    private AbilityParser() {}

    /** The technique kinds the runner implements. */
    public static final Set<String> KINDS = Set.of("line_slash", "chain_marks", "rift_pull", "blink", "nova", "ward");

    public record Result(WeaponAbilities abilities, List<String> errors) {
        public boolean ok() { return abilities != null && errors.isEmpty(); }
    }

    public static Result parse(String json) {
        List<String> errors = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return new Result(null, List.of("the file is not a JSON object"));
            root = el.getAsJsonObject();
        } catch (RuntimeException ex) {
            return new Result(null, List.of("not valid JSON (" + ex.getMessage() + ")"));
        }
        return parse(root, errors);
    }

    public static Result parse(JsonObject root) { return parse(root, new ArrayList<>()); }

    private static Result parse(JsonObject root, List<String> errors) {
        String item = str(root, "item", "");
        if (item.isEmpty()) errors.add("'item' is missing");
        AbilitySpec primary = root.has("primary") && root.get("primary").isJsonObject() ? spec(item + " primary", root.getAsJsonObject("primary"), errors) : null;
        if (primary == null) {
            errors.add(item + ": no usable 'primary' technique");
            return new Result(null, errors);
        }
        AbilitySpec alt = root.has("alt") && root.get("alt").isJsonObject() ? spec(item + " alt", root.getAsJsonObject("alt"), errors) : null;
        if (alt == null) alt = primary;
        float swingDamage = 0f, swingHeal = 0f;
        int swingStrip = 0;
        List<AbilitySpec.EffectSpec> swingEffects = List.of();
        if (root.has("swing") && root.get("swing").isJsonObject()) {
            JsonObject s = root.getAsJsonObject("swing");
            swingDamage = (float) num(item + " swing", s, "damage", 0, 0, 200, errors);
            swingHeal = (float) num(item + " swing", s, "heal", 0, 0, 100, errors);
            swingStrip = (int) num(item + " swing", s, "strip", 0, 0, 10, errors);
            swingEffects = effects(item + " swing", s.get("effects"), errors);
        }
        String skill = str(root, "skill", "");
        double mult = num(item, root, "skill_multiplier", 1.0, 0.1, 10.0, errors);
        return new Result(new WeaponAbilities(item, primary, alt, swingDamage, swingEffects, swingHeal, swingStrip, skill, mult), errors);
    }

    private static AbilitySpec spec(String where, JsonObject o, List<String> errors) {
        String kind = str(o, "kind", "");
        if (!KINDS.contains(kind)) {
            errors.add(where + ": unknown kind '" + kind + "' (known: " + new java.util.TreeSet<>(KINDS) + ")");
            return null;
        }
        String name = str(o, "name", "");
        if (name.isEmpty()) errors.add(where + ": 'name' is missing");
        int cooldown = (int) num(where, o, "cooldown", 200, 1, 72000, errors);
        float damage = (float) num(where, o, "damage", 0, 0, 200, errors);
        double range = num(where, o, "range", 8, 0, 64, errors);
        double radius = num(where, o, "radius", 4, 0, 32, errors);
        double width = num(where, o, "width", 1.5, 0.1, 16, errors);
        int count = (int) num(where, o, "count", 1, 1, 16, errors);
        double pull = num(where, o, "pull", 0, 0, 3, errors);
        double knock = num(where, o, "knockback", 0, 0, 4, errors);
        int strip = (int) num(where, o, "strip", 0, 0, 10, errors);
        float heal = (float) num(where, o, "heal", 0, 0, 100, errors);
        double runeScale = num(where, o, "rune_scale", 0, 0, 2, errors);
        int color = color(where, o.get("color"), errors);
        boolean dark = o.has("dark") && o.get("dark").isJsonPrimitive() && o.get("dark").getAsBoolean();
        return new AbilitySpec(kind, name, str(o, "desc", ""), cooldown, damage, range, radius, width, count, pull, knock, strip, heal, runeScale, color, dark, str(o, "vfx", ""),
                effects(where, o.get("effects"), errors), effects(where, o.get("self_effects"), errors));
    }

    private static List<AbilitySpec.EffectSpec> effects(String where, JsonElement el, List<String> errors) {
        List<AbilitySpec.EffectSpec> out = new ArrayList<>();
        if (el == null || el.isJsonNull()) return out;
        if (!el.isJsonArray()) { errors.add(where + ": 'effects' must be an array"); return out; }
        JsonArray a = el.getAsJsonArray();
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).isJsonObject()) { errors.add(where + " effect " + i + ": not an object"); continue; }
            JsonObject e = a.get(i).getAsJsonObject();
            String id = str(e, "id", "");
            if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) { errors.add(where + " effect " + i + ": bad id '" + id + "'"); continue; }
            int ticks = (int) num(where + " effect " + i, e, "ticks", 60, 1, 72000, errors);
            int amp = (int) num(where + " effect " + i, e, "amp", 0, 0, 9, errors);
            out.add(new AbilitySpec.EffectSpec(id, ticks, amp));
        }
        return out;
    }

    private static int color(String where, JsonElement el, List<String> errors) {
        if (el == null || el.isJsonNull()) return 0xFFFFFF;
        try {
            if (el.getAsJsonPrimitive().isNumber()) return el.getAsInt() & 0xFFFFFF;
            String s = el.getAsString().replace("#", "").replace("0x", "");
            return (int) (Long.parseLong(s, 16) & 0xFFFFFF);
        } catch (RuntimeException ex) {
            errors.add(where + ": bad 'color' (use a hex string like \"8A4BFF\")");
            return 0xFFFFFF;
        }
    }

    private static String str(JsonObject o, String k, String def) {
        JsonElement e = o.get(k);
        return e == null || !e.isJsonPrimitive() ? def : e.getAsString();
    }

    private static double num(String where, JsonObject o, String k, double def, double lo, double hi, List<String> errors) {
        JsonElement e = o.get(k);
        if (e == null || e.isJsonNull()) return def;
        double v;
        try {
            v = e.getAsDouble();
        } catch (RuntimeException ex) {
            errors.add(where + ": '" + k + "' must be a number");
            return def;
        }
        if (Double.isNaN(v) || v < lo || v > hi) {
            errors.add(where + ": '" + k + "' = " + v + " is outside " + lo + ".." + hi + "; clamped");
            return Math.max(lo, Math.min(hi, Double.isNaN(v) ? def : v));
        }
        return v;
    }
}
