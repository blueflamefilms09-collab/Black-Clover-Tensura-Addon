package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Why: a mechanic says "a band of undead" or "a swarm", not "three zombies": which monsters stand for a group at which depth is DATA ({@code data/nusmp/mega/mechanic_groups.json}),
 * so the owner (or the foundry's 400 mobs, which may be named there by id behind a presence check) can change the cast without touching code. A roster entry has an entity id, a
 * minimum tier, a weight and optionally a variant (written into the monster's data when the entity type is the foundry's) and an {@code optional} flag (skipped silently when the
 * entity type does not exist). This class is the pure part: parsing and the weighted pick for a tier. The built-in default is used when the file is missing or broken. Pure.
 */
public final class MobGroups {
    private MobGroups() {}

    public record Entry(String id, String variant, int tier, int weight, boolean optional) {}

    private static volatile Map<String, List<Entry>> groups = defaults();

    public static Map<String, List<Entry>> groups() { return groups; }

    public static void install(Map<String, List<Entry>> g) { groups = g == null || g.isEmpty() ? defaults() : Map.copyOf(g); }

    /** Parses the data file: {@code {"group": [{"id": "minecraft:zombie", "tier": 1, "weight": 10}, ...]}}. Bad entries are skipped; the problems are returned in {@code problems}. */
    public static Map<String, List<Entry>> parse(String json, List<String> problems) {
        Map<String, List<Entry>> out = new LinkedHashMap<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (e.getKey().startsWith("_") || !e.getValue().isJsonArray()) continue;
                List<Entry> list = new ArrayList<>();
                for (JsonElement x : (JsonArray) e.getValue()) {
                    if (!x.isJsonObject()) { problems.add(e.getKey() + ": an entry is not an object"); continue; }
                    JsonObject o = x.getAsJsonObject();
                    if (!o.has("id")) { problems.add(e.getKey() + ": an entry has no id"); continue; }
                    list.add(new Entry(o.get("id").getAsString(), o.has("variant") ? o.get("variant").getAsString() : "", o.has("tier") ? Math.max(1, Math.min(5, o.get("tier").getAsInt())) : 1,
                            o.has("weight") ? Math.max(1, o.get("weight").getAsInt()) : 10, o.has("optional") && o.get("optional").getAsBoolean()));
                }
                if (!list.isEmpty()) out.put(e.getKey(), List.copyOf(list));
            }
        } catch (RuntimeException ex) {
            problems.add("mechanic_groups.json: " + ex.getMessage());
        }
        return out;
    }

    /** One weighted pick among the entries of the group whose tier is not above {@code tier}; the first entry of the group when nothing fits; null for an unknown group. */
    public static Entry pick(String group, int tier, SplittableRandom rng, java.util.function.Predicate<Entry> usable) {
        List<Entry> list = groups.get(group);
        if (list == null || list.isEmpty()) return null;
        int total = 0;
        for (Entry e : list) if (e.tier() <= tier && usable.test(e)) total += e.weight();
        if (total <= 0) {
            for (Entry e : list) if (usable.test(e)) return e;
            return null;
        }
        int r = rng.nextInt(total);
        for (Entry e : list) {
            if (e.tier() > tier || !usable.test(e)) continue;
            r -= e.weight();
            if (r < 0) return e;
        }
        return list.get(0);
    }

    private static Map<String, List<Entry>> defaults() {
        Map<String, List<Entry>> m = new LinkedHashMap<>();
        m.put("undead", List.of(new Entry("minecraft:zombie", "", 1, 10, false), new Entry("minecraft:skeleton", "", 1, 10, false), new Entry("minecraft:husk", "", 2, 8, false),
                new Entry("minecraft:stray", "", 2, 8, false), new Entry("minecraft:wither_skeleton", "", 4, 6, false)));
        m.put("beast", List.of(new Entry("minecraft:spider", "", 1, 10, false), new Entry("minecraft:cave_spider", "", 1, 8, false), new Entry("minecraft:hoglin", "", 3, 6, false),
                new Entry("minecraft:zoglin", "", 4, 6, false), new Entry("minecraft:ravager", "", 5, 3, false)));
        m.put("construct", List.of(new Entry("minecraft:vindicator", "", 1, 10, false), new Entry("minecraft:pillager", "", 2, 8, false), new Entry("minecraft:ravager", "", 4, 4, false)));
        m.put("fiend", List.of(new Entry("minecraft:zombified_piglin", "", 1, 8, false), new Entry("minecraft:piglin_brute", "", 2, 8, false), new Entry("minecraft:blaze", "", 3, 6, false),
                new Entry("minecraft:wither_skeleton", "", 4, 6, false)));
        m.put("arcane", List.of(new Entry("minecraft:witch", "", 1, 10, false), new Entry("minecraft:vex", "", 2, 8, false), new Entry("minecraft:evoker", "", 3, 6, false),
                new Entry("minecraft:illusioner", "", 4, 4, false)));
        m.put("swarm", List.of(new Entry("minecraft:silverfish", "", 1, 10, false), new Entry("minecraft:endermite", "", 1, 8, false), new Entry("minecraft:cave_spider", "", 2, 8, false),
                new Entry("minecraft:vex", "", 3, 4, false)));
        m.put("elemental", List.of(new Entry("minecraft:slime", "", 1, 10, false), new Entry("minecraft:magma_cube", "", 2, 8, false), new Entry("minecraft:blaze", "", 3, 6, false),
                new Entry("minecraft:breeze", "", 4, 6, false)));
        m.put("knight", List.of(new Entry("minecraft:vindicator", "", 1, 10, false), new Entry("minecraft:piglin_brute", "", 2, 8, false), new Entry("minecraft:pillager", "", 2, 6, false),
                new Entry("minecraft:evoker", "", 4, 4, false), new Entry("minecraft:ravager", "", 5, 3, false)));
        return m;
    }
}
