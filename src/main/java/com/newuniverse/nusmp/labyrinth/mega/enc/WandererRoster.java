package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Why: which big things walk the halls is the most flavourful and the most tweakable part of the Dungeon Master, so it is a list in a data file
 * ({@code data/nusmp/mega/wanderers.json}), not a switch in Java: an entity id, how likely it is, from which threat level it may appear, the
 * name the party will use for it, and two multipliers that make the same body a different animal (a Floor Warden with more health, a Ravager
 * that is a little slower). The file is read with the same strictness as the encounter tables: every problem is a line of text that the loader
 * logs and the shipped-file test fails on, and an entity that is not installed is simply never drawn (the game passes a {@code usable}
 * predicate), so the list can name mods the player may not have. Pure (Gson, no Minecraft).
 */
public record WandererRoster(List<Entry> entries) {
    /** One kind of wanderer. {@code healthScale} multiplies its maximum health, {@code speed} its movement speed (1.0 = as the body is). */
    public record Entry(String entity, int weight, int minThreat, int maxThreat, String name, double healthScale, double speed) {
        public boolean fitsThreat(int threat) { return threat >= minThreat && threat <= maxThreat; }
    }

    /** The roster and every problem found while reading it. */
    public record Parsed(WandererRoster roster, List<String> problems) {
        public boolean clean() { return roster != null && problems.isEmpty(); }
    }

    public static final WandererRoster EMPTY = new WandererRoster(List.of());

    private static final Pattern ENTITY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> ROOT_KEYS = Set.of("comment", "entries");
    private static final Set<String> ENTRY_KEYS = Set.of("entity", "weight", "minThreat", "maxThreat", "name", "healthScale", "speed", "comment");

    public WandererRoster {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    /** One entry by weight among those whose threat window holds {@code threat} and which the game can spawn; null when there is none. */
    public Entry pick(int threat, Predicate<Entry> usable, Random r) {
        List<Entry> pool = new ArrayList<>();
        int total = 0;
        for (Entry e : entries) {
            if (!e.fitsThreat(threat) || (usable != null && !usable.test(e))) continue;
            pool.add(e);
            total += e.weight();
        }
        if (pool.isEmpty()) return null;
        int roll = r.nextInt(total);
        for (Entry e : pool) {
            roll -= e.weight();
            if (roll < 0) return e;
        }
        return pool.get(pool.size() - 1);
    }

    public static Parsed parse(String json) {
        List<String> problems = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement e = JsonParser.parseString(json == null ? "" : json);
            if (!e.isJsonObject()) {
                problems.add("wanderers: the file is not a JSON object");
                return new Parsed(null, problems);
            }
            root = e.getAsJsonObject();
        } catch (RuntimeException bad) {
            problems.add("wanderers: not valid JSON (" + String.valueOf(bad.getMessage()).replace('\n', ' ') + ")");
            return new Parsed(null, problems);
        }
        for (Map.Entry<String, JsonElement> k : root.entrySet()) if (!ROOT_KEYS.contains(k.getKey())) problems.add("wanderers: unknown key '" + k.getKey() + "'");
        List<Entry> out = new ArrayList<>();
        JsonElement list = root.get("entries");
        if (list == null || !list.isJsonArray()) {
            problems.add("wanderers: 'entries' must be an array");
            return new Parsed(new WandererRoster(out), problems);
        }
        JsonArray arr = list.getAsJsonArray();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < arr.size(); i++) {
            if (!arr.get(i).isJsonObject()) {
                problems.add("wanderers: entry #" + i + " is not an object");
                continue;
            }
            JsonObject o = arr.get(i).getAsJsonObject();
            String entity = string(o, "entity", "");
            String where = "wanderers entry " + (entity.isEmpty() ? "#" + i : "'" + entity + "'");
            if (!ENTITY_ID.matcher(entity).matches()) {
                problems.add(where + ": 'entity' must be an entity id such as nusmp:floor_warden");
                continue;
            }
            if (!seen.add(entity)) {
                problems.add(where + ": listed twice, the second one is dropped");
                continue;
            }
            for (Map.Entry<String, JsonElement> k : o.entrySet()) if (!ENTRY_KEYS.contains(k.getKey())) problems.add(where + ": unknown key '" + k.getKey() + "'");
            int weight = integer(where, o, "weight", 5, 1, 1000, problems);
            int minThreat = integer(where, o, "minThreat", 1, 1, 100000, problems);
            int maxThreat = integer(where, o, "maxThreat", 100000, 1, 100000, problems);
            if (maxThreat < minThreat) {
                problems.add(where + ": maxThreat is below minThreat, the window is ignored");
                maxThreat = 100000;
            }
            String name = string(o, "name", "");
            if (name.isEmpty()) name = entity.substring(entity.indexOf(':') + 1).replace('_', ' ');
            double health = number(where, o, "healthScale", 1.0, 0.25, 6.0, problems), speed = number(where, o, "speed", 1.0, 0.4, 1.6, problems);
            out.add(new Entry(entity, weight, minThreat, maxThreat, name, health, speed));
        }
        return new Parsed(new WandererRoster(out), problems);
    }

    private static String string(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString().trim() : def;
    }

    private static boolean isNumber(JsonElement e) { return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber(); }

    private static int integer(String where, JsonObject o, String key, int def, int lo, int hi, List<String> problems) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (!isNumber(e) || e.getAsDouble() != Math.rint(e.getAsDouble())) {
            problems.add(where + ": '" + key + "' must be a whole number");
            return def;
        }
        double v = e.getAsDouble();
        if (v < lo || v > hi) {
            problems.add(where + ": '" + key + "' " + e.getAsString() + " is outside " + lo + ".." + hi + ", clamped");
            return (int) Math.max(lo, Math.min(hi, v));
        }
        return (int) v;
    }

    private static double number(String where, JsonObject o, String key, double def, double lo, double hi, List<String> problems) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (!isNumber(e)) {
            problems.add(where + ": '" + key + "' must be a number");
            return def;
        }
        double v = e.getAsDouble();
        if (Double.isNaN(v) || v < lo || v > hi) {
            problems.add(where + ": '" + key + "' " + e.getAsString() + " is outside " + lo + ".." + hi + ", clamped");
            return Double.isNaN(v) ? def : Math.max(lo, Math.min(hi, v));
        }
        return v;
    }
}
