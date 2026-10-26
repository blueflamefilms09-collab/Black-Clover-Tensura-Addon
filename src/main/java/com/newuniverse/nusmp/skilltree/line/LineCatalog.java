package com.newuniverse.nusmp.skilltree.line;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: 275 generated lines of 15 to 30 nodes cannot each carry Java, so every node names ONE effect "kind" and gives numbers for it, and the vocabulary of kinds (which numbers a kind has, what range
 * each may take, which enum values it accepts) is written once, by the generator, in {@code tools/lines/catalog.json}. The engine needs that same vocabulary in two places that must never disagree with
 * the generator: the VALIDATOR ({@code RouteSchema} refuses a node whose kind, enum value or number is outside it, so a typo in a data file is a readable message and not a silent no-op) and the
 * EXECUTORS (a number the file leaves out falls back to the catalogue's base value, so a half-written node still behaves sanely). So the engine reads a verbatim copy of the file from the jar
 * ({@code /nusmp_lines/catalog.json}); a unit test fails the build when the copy and the generator's file differ, and another when a kind has no executor or an executor no kind. Pure (Gson only), no
 * Minecraft type, so the schema and the tests use it without a game. It is immutable after loading and safe to read from any thread. If the resource cannot be read the catalogue is empty and
 * {@link #loaded()} is false: the schema then skips the catalogue checks (and says so once) instead of refusing every line, because a broken jar must not look like 275 broken data files.
 */
public final class LineCatalog {
    /** The four groups of kinds: arts and finishers, passives, the stance, the attribute bonus. */
    public enum Group { ACTIVE, PASSIVE, STANCE, ATTR }

    /** One numeric parameter: the level-1 value, the legal range, and the growth per level. */
    public record Num(double base, double min, double max, double grow) {
        /** True when {@code v} is inside the range (a hair of slack for the generator's rounding). */
        public boolean accepts(double v) { return v >= min - 1e-6 && v <= max + 1e-6; }
    }

    /** One kind of the catalogue. */
    public record Kind(String name, Group group, Map<String, Num> nums, Map<String, List<String>> enums, String desc) {}

    private static final String RESOURCE = "/nusmp_lines/catalog.json";
    private static volatile LineCatalog instance;

    private final Map<String, Kind> active, passive, stance, attr;
    private final Map<String, Kind> evolution;
    private final List<String> backgrounds, vfx;
    private final Map<String, String> franchises;
    private final boolean loaded;

    private LineCatalog(Map<String, Kind> active, Map<String, Kind> passive, Map<String, Kind> stance, Map<String, Kind> attr, Map<String, Kind> evolution, List<String> backgrounds,
                        Map<String, String> franchises, List<String> vfx, boolean loaded) {
        this.active = active;
        this.evolution = evolution;
        this.passive = passive;
        this.stance = stance;
        this.attr = attr;
        this.backgrounds = backgrounds;
        this.franchises = franchises;
        this.vfx = vfx;
        this.loaded = loaded;
    }

    /** The catalogue of the jar (loaded once). */
    public static LineCatalog get() {
        LineCatalog c = instance;
        if (c == null) {
            synchronized (LineCatalog.class) {
                if (instance == null) instance = load();
                c = instance;
            }
        }
        return c;
    }

    private static LineCatalog load() {
        try (InputStream in = LineCatalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return empty();
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return empty();
        }
    }

    private static LineCatalog empty() {
        return new LineCatalog(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), false);
    }

    /** Parses the text of {@code catalog.json}. Never throws: unreadable text gives the empty catalogue. */
    public static LineCatalog parse(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            Map<String, String> fr = new LinkedHashMap<>();
            if (o.has("franchises")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("franchises").entrySet()) fr.put(e.getKey(), e.getValue().getAsString());
            return new LineCatalog(kinds(o, "active", Group.ACTIVE), kinds(o, "passive", Group.PASSIVE), kinds(o, "stance", Group.STANCE), kinds(o, "attr", Group.ATTR),
                    evolutionKinds(o), strings(o, "backgrounds"), Collections.unmodifiableMap(fr), strings(o, "vfx"), true);
        } catch (RuntimeException e) {
            return empty();
        }
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
        return Collections.unmodifiableList(out);
    }

    /**
     * 3.26.0 (Evolution skills): the counters a line's {@code evolution} block may watch ({@code kills}, {@code damage_taken}, ...). They have no numbers (the count is the route's own) and at most one enum
     * (the damage kind of {@code damage_taken}), so they are read as kinds of the ATTR group's shape with an empty number table; a catalogue file from before 3.26.0 simply has none.
     */
    private static Map<String, Kind> evolutionKinds(JsonObject root) {
        Map<String, Kind> out = new LinkedHashMap<>();
        if (!root.has("evolution") || !root.get("evolution").isJsonObject()) return Collections.unmodifiableMap(out);
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("evolution").entrySet()) {
            JsonObject k = e.getValue().getAsJsonObject();
            Map<String, List<String>> enums = new LinkedHashMap<>();
            if (k.has("enums")) {
                for (Map.Entry<String, JsonElement> p : k.getAsJsonObject("enums").entrySet()) {
                    List<String> vals = new ArrayList<>();
                    for (JsonElement v : p.getValue().getAsJsonArray()) vals.add(v.getAsString());
                    enums.put(p.getKey(), Collections.unmodifiableList(vals));
                }
            }
            out.put(e.getKey(), new Kind(e.getKey(), Group.ATTR, Map.of(), Collections.unmodifiableMap(enums), k.has("desc") ? k.get("desc").getAsString() : ""));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Kind> kinds(JsonObject root, String group, Group g) {
        Map<String, Kind> out = new LinkedHashMap<>();
        if (!root.has(group)) return Collections.unmodifiableMap(out);
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject(group).entrySet()) {
            JsonObject k = e.getValue().getAsJsonObject();
            Map<String, Num> nums = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> p : k.getAsJsonObject("params").entrySet()) {
                JsonObject n = p.getValue().getAsJsonObject();
                nums.put(p.getKey(), new Num(n.get("base").getAsDouble(), n.get("min").getAsDouble(), n.get("max").getAsDouble(), n.get("grow").getAsDouble()));
            }
            Map<String, List<String>> enums = new LinkedHashMap<>();
            if (k.has("enums")) {
                for (Map.Entry<String, JsonElement> p : k.getAsJsonObject("enums").entrySet()) {
                    List<String> vals = new ArrayList<>();
                    JsonArray a = p.getValue().getAsJsonArray();
                    for (JsonElement v : a) vals.add(v.getAsString());
                    enums.put(p.getKey(), Collections.unmodifiableList(vals));
                }
            }
            out.put(e.getKey(), new Kind(e.getKey(), g, Collections.unmodifiableMap(nums), Collections.unmodifiableMap(enums), k.has("desc") ? k.get("desc").getAsString() : ""));
        }
        return Collections.unmodifiableMap(out);
    }

    // ---------------------------------------------------------------- lookups
    /** False when the resource could not be read (see the class comment). */
    public boolean loaded() { return loaded; }

    /** The kind of that name in that group, or null. */
    public Kind kind(Group g, String name) {
        if (name == null) return null;
        return switch (g) {
            case ACTIVE -> active.get(name);
            case PASSIVE -> passive.get(name);
            case STANCE -> stance.get(name);
            case ATTR -> attr.get(name);
        };
    }

    /** The kind a node of this {@code type} may use under that name (active_ability: an art or the stance; passive_toggle: a passive; attribute_modifier: attr), or null. */
    public Kind kindForType(String type, String name) {
        if (type == null || name == null) return null;
        return switch (type) {
            case "active_ability" -> active.containsKey(name) ? active.get(name) : stance.get(name);
            case "passive_toggle" -> passive.get(name);
            case "attribute_modifier" -> attr.get(name);
            default -> null;
        };
    }

    /** Any kind of that name, whatever its group (used to infer the type of a node that gives only a kind), or null. */
    public Kind anyKind(String name) {
        if (name == null) return null;
        Kind k = active.get(name);
        if (k == null) k = passive.get(name);
        if (k == null) k = stance.get(name);
        if (k == null) k = attr.get(name);
        return k;
    }

    /** The node type a kind belongs to: {@code active_ability} for an art or the stance, {@code passive_toggle} for a passive, {@code attribute_modifier} for attr; null if unknown. */
    public static String typeOf(Group g) {
        return switch (g) {
            case ACTIVE, STANCE -> "active_ability";
            case PASSIVE -> "passive_toggle";
            case ATTR -> "attribute_modifier";
        };
    }

    /** 3.26.0: the evolution counters of the catalogue (a requirement must be one of these), with their enums (the damage kind of {@code damage_taken}). Empty for an old catalogue file. */
    public Map<String, Kind> evolution() { return evolution; }

    public Set<String> activeKinds() { return active.keySet(); }

    public Set<String> passiveKinds() { return passive.keySet(); }

    public Set<String> stanceKinds() { return stance.keySet(); }

    public Set<String> attrKinds() { return attr.keySet(); }

    /** Every kind name of every group. */
    public List<String> allKinds() {
        List<String> out = new ArrayList<>(active.keySet());
        out.addAll(passive.keySet());
        out.addAll(stance.keySet());
        out.addAll(attr.keySet());
        return out;
    }

    public List<String> backgrounds() { return backgrounds; }

    public List<String> vfx() { return vfx; }

    public Map<String, String> franchises() { return franchises; }

    /** The catalogue's base value of a number of a kind, or {@code fallback} when the kind or the parameter is unknown. */
    public double base(String kindName, String param, double fallback) {
        Kind k = anyKind(kindName);
        Num n = k == null ? null : k.nums().get(param);
        return n == null ? fallback : n.base();
    }
}
