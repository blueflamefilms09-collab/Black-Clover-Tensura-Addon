package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the ability library and the archetypes are two data files ({@code _abilities.json}, {@code _archetypes.json}) that the owner will tune by hand, so their parser must be
 * strict and loud (the pattern of {@code EncounterTableParser}): every problem becomes one line of text that the loader logs and the shipped-file test turns into a failing
 * build; a row that cannot work is dropped, a number out of range is clamped and reported, and one bad row never costs the rest. Gson only, no Minecraft type.
 */
public final class FoundryLibrary {
    private final Map<String, FoundryAbility> abilities;
    private final Map<String, FoundryArchetype> archetypes;

    public static final FoundryLibrary EMPTY = new FoundryLibrary(Map.of(), Map.of());

    public FoundryLibrary(Map<String, FoundryAbility> abilities, Map<String, FoundryArchetype> archetypes) {
        this.abilities = Collections.unmodifiableMap(new LinkedHashMap<>(abilities));
        this.archetypes = Collections.unmodifiableMap(new LinkedHashMap<>(archetypes));
    }

    /** The result of a parse: the rows that were usable and every problem found. */
    public record Parsed<T>(Map<String, T> rows, List<String> problems) {}

    public FoundryAbility ability(String id) { return id == null ? null : abilities.get(id); }

    public FoundryArchetype archetype(String id) { return id == null ? null : archetypes.get(id); }

    public Map<String, FoundryAbility> abilities() { return abilities; }

    public Map<String, FoundryArchetype> archetypes() { return archetypes; }

    // ---------------------------------------------------------------- parsing
    public static Parsed<FoundryAbility> parseAbilities(String json) {
        Map<String, FoundryAbility> out = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        JsonArray arr = array(json, "abilities", problems);
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) {
                problems.add("abilities: an entry is not an object");
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            String id = str(o, "id", "");
            if (!id.matches("[a-z0-9_]{1,40}")) {
                problems.add("abilities: bad id '" + id + "'");
                continue;
            }
            if (out.containsKey(id)) {
                problems.add("ability '" + id + "' is listed twice");
                continue;
            }
            FoundryAbility.Kind kind = FoundryAbility.Kind.of(str(o, "kind", ""));
            if (kind == null) {
                problems.add("ability '" + id + "': unknown kind '" + str(o, "kind", "") + "'");
                continue;
            }
            FoundryAbility a = FoundryAbility.clamped(id, kind, (int) num(o, "cd", 100), num(o, "min", 0), num(o, "max", 0), (int) num(o, "wind", 8), num(o, "pow", 1.0),
                    (int) num(o, "dur", 0), (int) num(o, "n", 0), num(o, "r", 0), (int) num(o, "amp", 0), num(o, "hp", 0), str(o, "align", "road"), str(o, "snd", ""));
            if (a.cd() == 0 && !a.passive()) problems.add("ability '" + id + "': cooldown 0 is only for a passive (burst)");
            if (a.max() > 0.0 && a.min() > a.max()) problems.add("ability '" + id + "': min is above max");
            if (!List.of("road", "light", "dark").contains(a.align())) problems.add("ability '" + id + "': align '" + a.align() + "' is not road, light or dark");
            out.put(id, a);
        }
        return new Parsed<>(out, problems);
    }

    public static Parsed<FoundryArchetype> parseArchetypes(String json) {
        Map<String, FoundryArchetype> out = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        JsonArray arr = array(json, "archetypes", problems);
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) {
                problems.add("archetypes: an entry is not an object");
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            String id = str(o, "id", "");
            if (!id.matches("[a-z0-9_]{1,40}")) {
                problems.add("archetypes: bad id '" + id + "'");
                continue;
            }
            FoundryArchetype.Style style = FoundryArchetype.Style.of(str(o, "style", ""));
            if (style == null) {
                problems.add("archetype '" + id + "': unknown style '" + str(o, "style", "") + "'");
                continue;
            }
            out.put(id, new FoundryArchetype(id, clamp(num(o, "hp", 1), 0.2, 4), clamp(num(o, "dmg", 1), 0.2, 4), clamp(num(o, "speed", 1), 0.4, 1.8), clamp(num(o, "armor", 1), 0, 3),
                    style, clamp(num(o, "near", 0), 0, 40), clamp(num(o, "far", 0), 0, 40), clamp(num(o, "keep", 0), 0, 40), (int) clamp(num(o, "ability", 30), 4, 400)));
        }
        for (String need : FoundryArchetype.CONTRACT) if (!out.containsKey(need)) problems.add("archetype '" + need + "' of the contract is missing");
        return new Parsed<>(out, problems);
    }

    private static JsonArray array(String json, String key, List<String> problems) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonObject() && root.getAsJsonObject().has(key) && root.getAsJsonObject().get(key).isJsonArray()) return root.getAsJsonObject().getAsJsonArray(key);
            problems.add("no '" + key + "' array");
        } catch (RuntimeException ex) {
            problems.add("not valid JSON: " + ex.getMessage());
        }
        return new JsonArray();
    }

    static String str(JsonObject o, String k, String d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d;
    }

    static double num(JsonObject o, String k, double d) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : d;
        } catch (RuntimeException ex) {
            return d;
        }
    }

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }
}
