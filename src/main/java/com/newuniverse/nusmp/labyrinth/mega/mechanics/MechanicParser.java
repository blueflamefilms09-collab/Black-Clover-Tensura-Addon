package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Why: the thousand mechanics are data files that a datapack may extend or fix, and in a game that cannot be run while writing it the worst outcome is a silent drop. So this
 * parser is strict and loud like the encounter tables' (EncounterTableParser): a mechanic that is meaningless (no id, unknown category, no effects, a primitive the catalogue
 * does not know) is dropped WITH a one-line problem naming it; a mechanic with a bad detail keeps going with the value clamped. {@code catalog} may be null (then only the
 * structure is checked, which is what the tools that have no catalogue use). Pure (Gson).
 */
public final class MechanicParser {
    private MechanicParser() {}

    public record Parsed(List<MechanicDef> defs, List<String> problems) {}

    private static final Pattern ID = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+){1,4}");
    public static final String[] REWARDS = {"none", "loot", "xp", "heal", "mana"};

    /** Parses one data file: {@code {"category": "...", "mechanics": [ ... ]}} (a bare array of mechanics is accepted too). */
    public static Parsed parseFile(String fileName, String json, OpCatalog catalog) {
        List<String> problems = new ArrayList<>();
        List<MechanicDef> out = new ArrayList<>();
        JsonArray arr;
        try {
            JsonElement root = JsonParser.parseString(json == null ? "" : json);
            if (root.isJsonArray()) arr = root.getAsJsonArray();
            else if (root.isJsonObject() && root.getAsJsonObject().has("mechanics")) arr = root.getAsJsonObject().getAsJsonArray("mechanics");
            else { problems.add(fileName + ": neither an array nor an object with 'mechanics'"); return new Parsed(out, problems); }
        } catch (RuntimeException e) {
            problems.add(fileName + ": not valid JSON (" + String.valueOf(e.getMessage()).replace('\n', ' ') + ")");
            return new Parsed(out, problems);
        }
        for (int i = 0; i < arr.size(); i++) {
            if (!arr.get(i).isJsonObject()) { problems.add(fileName + "[" + i + "]: not an object"); continue; }
            MechanicDef d = parseOne(fileName + "[" + i + "]", arr.get(i).getAsJsonObject(), catalog, problems);
            if (d != null) out.add(d);
        }
        return new Parsed(out, problems);
    }

    /** Parses one mechanic object; null (with problems appended) when it cannot be used. */
    public static MechanicDef parseOne(String where, JsonObject o, OpCatalog catalog, List<String> problems) {
        String id = str(o, "id", "");
        if (!ID.matcher(id).matches()) { problems.add(where + ": bad id '" + id + "'"); return null; }
        where = id;
        MechCategory cat = MechCategory.fromId(str(o, "cat", ""));
        if (cat == null) { problems.add(where + ": unknown category '" + str(o, "cat", "") + "'"); return null; }
        String name = str(o, "name", ""), desc = str(o, "desc", ""), src = str(o, "src", "");
        if (name.isBlank() || desc.isBlank()) { problems.add(where + ": name and desc must not be empty"); return null; }
        if (src.isBlank()) { problems.add(where + ": the source (series or rulebook) must not be empty"); return null; }
        Scope scope = Scope.fromId(str(o, "scope", "room"));
        if (scope == null) { problems.add(where + ": unknown scope"); return null; }
        Road road = Road.fromId(str(o, "road", "both"));
        if (road == null) { problems.add(where + ": unknown road"); return null; }
        int tmin = 1, tmax = Scaling.TIERS;
        if (o.has("tiers") && o.get("tiers").isJsonArray() && o.getAsJsonArray("tiers").size() == 2) {
            tmin = o.getAsJsonArray("tiers").get(0).getAsInt();
            tmax = o.getAsJsonArray("tiers").get(1).getAsInt();
        }
        if (tmin < 1 || tmax > Scaling.TIERS || tmin > tmax) { problems.add(where + ": bad tier range " + tmin + ".." + tmax); tmin = 1; tmax = Scaling.TIERS; }
        List<String> themes = new ArrayList<>();
        if (o.has("themes") && o.get("themes").isJsonArray()) for (JsonElement e : o.getAsJsonArray("themes")) themes.add(e.getAsString());
        int cost = clamp(where, "cost", intOf(o, "cost", 3), 1, 12, problems);
        int weight = clamp(where, "weight", intOf(o, "weight", 10), 1, 100, problems);

        MechanicDef.Trigger trigger;
        if (o.has("trigger") && o.get("trigger").isJsonObject()) {
            JsonObject t = o.getAsJsonObject("trigger");
            trigger = new MechanicDef.Trigger(str(t, "kind", "floor_enter"), Args.fromJson(t, OpCatalog.TRIGGER_KEYS));
        } else { problems.add(where + ": no trigger"); return null; }
        MechanicDef.Counter counter = new MechanicDef.Counter("none", Args.EMPTY);
        if (o.has("counter") && o.get("counter").isJsonObject()) {
            JsonObject c = o.getAsJsonObject("counter");
            counter = new MechanicDef.Counter(str(c, "kind", "none"), Args.fromJson(c, OpCatalog.COUNTER_KEYS));
        }
        List<MechanicDef.Effect> effects = new ArrayList<>();
        if (o.has("effects") && o.get("effects").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("effects")) {
                if (!e.isJsonObject()) { problems.add(where + ": an effect is not an object"); continue; }
                JsonObject x = e.getAsJsonObject();
                String op = str(x, "op", "");
                if (op.isEmpty()) { problems.add(where + ": an effect has no op"); continue; }
                effects.add(new MechanicDef.Effect(op, Args.fromJson(x, OpCatalog.EFFECT_KEYS), Math.max(0, intOf(x, "delay", 0)), Math.max(0.0, Math.min(1.0, dbl(x, "chance", 1.0))),
                        Math.max(1, intOf(x, "times", 1)), Math.max(1, intOf(x, "every", 20)), str(x, "if", "")));
            }
        }
        String reward = str(o, "reward", "none");
        boolean known = false;
        for (String r : REWARDS) if (r.equals(reward)) known = true;
        if (!known) { problems.add(where + ": unknown reward '" + reward + "'"); reward = "none"; }
        MechanicDef d = new MechanicDef(id, name, desc, str(o, "flavor", ""), cat, src, str(o, "base", id), scope, tmin, tmax, road, List.copyOf(themes), cost, weight, trigger,
                List.copyOf(effects), counter, reward, str(o, "sig", ""));
        if (catalog != null) {
            List<String> p = catalog.check(d);
            if (!p.isEmpty()) {
                problems.addAll(p);
                for (String s : p) if (s.contains("unknown primitive") || s.contains("unknown trigger") || s.contains("unknown counter") || s.contains("no effects")) return null;
            }
        }
        if (effects.isEmpty()) { problems.add(where + ": no usable effects"); return null; }
        return d;
    }

    private static String str(JsonObject o, String k, String def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    private static int intOf(JsonObject o, String k, int def) {
        JsonElement e = o.get(k);
        try { return e != null && e.isJsonPrimitive() ? e.getAsInt() : def; } catch (RuntimeException bad) { return def; }
    }

    private static double dbl(JsonObject o, String k, double def) {
        JsonElement e = o.get(k);
        try { return e != null && e.isJsonPrimitive() ? e.getAsDouble() : def; } catch (RuntimeException bad) { return def; }
    }

    private static int clamp(String where, String k, int v, int lo, int hi, List<String> problems) {
        if (v >= lo && v <= hi) return v;
        problems.add(where + ": " + k + " " + v + " is outside " + lo + ".." + hi);
        return Math.max(lo, Math.min(hi, v));
    }
}
