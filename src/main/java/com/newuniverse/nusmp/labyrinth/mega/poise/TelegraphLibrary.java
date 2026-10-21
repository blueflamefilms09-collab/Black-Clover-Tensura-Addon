package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryAbility;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryArchetype;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryLibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: which Foundry abilities are TELEGRAPHED, how, and which ULTIMATE each archetype unlocks in desperation are design choices the owner will tune, so they are data
 * ({@code data/nusmp/mega/poise/telegraphs.json}) over the abilities that already exist, read by a strict parser in the style of {@link FoundryLibrary}: every problem is one line of text,
 * a row that cannot work is dropped, a number out of range is clamped. {@code kinds} gives a default per ability KIND (every cleave warns with a cone), {@code overrides} gives a row for one
 * ability ID (arrow rain draws a circle on the target, the plain volley draws nothing), {@code ultimates} names, per archetype, the ability of {@code abilities} (rows in exactly the format of
 * {@code _abilities.json}, parsed by the same {@link FoundryLibrary#parseAbilities}) that a desperate mob adds to its own. Immutable once built; {@link #check} audits it against the
 * foundry's own library (every override names a real ability, every archetype of the contract has an ultimate that is not a passive).
 */
public final class TelegraphLibrary {
    public static final TelegraphLibrary EMPTY = new TelegraphLibrary(Map.of(), Map.of(), Map.of(), Map.of());

    private final Map<FoundryAbility.Kind, TelegraphDef> kinds;
    private final Map<String, TelegraphDef> overrides;
    private final Map<String, String> ultimateOf;
    private final Map<String, FoundryAbility> ultimates;

    public TelegraphLibrary(Map<FoundryAbility.Kind, TelegraphDef> kinds, Map<String, TelegraphDef> overrides, Map<String, String> ultimateOf, Map<String, FoundryAbility> ultimates) {
        this.kinds = Collections.unmodifiableMap(new LinkedHashMap<>(kinds));
        this.overrides = Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
        this.ultimateOf = Collections.unmodifiableMap(new LinkedHashMap<>(ultimateOf));
        this.ultimates = Collections.unmodifiableMap(new LinkedHashMap<>(ultimates));
    }

    /** The result of a parse: the library (never null) and every problem found. */
    public record Parsed(TelegraphLibrary library, List<String> problems) {}

    public Map<FoundryAbility.Kind, TelegraphDef> kinds() { return kinds; }

    public Map<String, TelegraphDef> overrides() { return overrides; }

    public Map<String, String> ultimateIds() { return ultimateOf; }

    public Map<String, FoundryAbility> ultimateRows() { return ultimates; }

    /** The telegraph of an ability: its own row, else its kind's, else null (a heal, a summon, the passive burst: nothing to show, no armor). A row with shape NONE counts as no telegraph. */
    public TelegraphDef def(FoundryAbility a) {
        if (a == null || a.passive()) return null;
        TelegraphDef d = overrides.get(a.id());
        if (d == null) d = kinds.get(a.kind());
        return d == null || d.shape() == TelegraphShape.NONE ? null : d;
    }

    /** The ultimate a desperate mob of this archetype adds, or null. */
    public FoundryAbility ultimate(String archetype) {
        String id = archetype == null ? null : ultimateOf.get(archetype);
        return id == null ? null : ultimates.get(id);
    }

    // ---------------------------------------------------------------- parsing
    public static Parsed parse(String json) {
        List<String> problems = new ArrayList<>();
        Map<FoundryAbility.Kind, TelegraphDef> kinds = new LinkedHashMap<>();
        Map<String, TelegraphDef> overrides = new LinkedHashMap<>();
        Map<String, String> ultOf = new LinkedHashMap<>();
        Map<String, FoundryAbility> rows = new LinkedHashMap<>();
        JsonObject root;
        try {
            JsonElement e = JsonParser.parseString(json);
            if (!e.isJsonObject()) return new Parsed(EMPTY, List.of("telegraphs: not a JSON object"));
            root = e.getAsJsonObject();
        } catch (RuntimeException ex) {
            return new Parsed(EMPTY, List.of("telegraphs: not valid JSON: " + ex.getMessage()));
        }
        if (obj(root, "kinds") != null) {
            for (Map.Entry<String, JsonElement> en : obj(root, "kinds").entrySet()) {
                FoundryAbility.Kind k = FoundryAbility.Kind.of(en.getKey());
                if (k == null) {
                    problems.add("kinds: unknown ability kind '" + en.getKey() + "'");
                    continue;
                }
                TelegraphDef d = row("kind " + en.getKey(), en.getValue(), problems);
                if (d != null) kinds.put(k, d);
            }
        } else {
            problems.add("telegraphs: no 'kinds' object");
        }
        if (obj(root, "overrides") != null) {
            for (Map.Entry<String, JsonElement> en : obj(root, "overrides").entrySet()) {
                TelegraphDef d = row("override " + en.getKey(), en.getValue(), problems);
                if (d != null) overrides.put(en.getKey(), d);
            }
        }
        if (root.has("abilities")) {
            FoundryLibrary.Parsed<FoundryAbility> p = FoundryLibrary.parseAbilities(root.toString());
            for (String s : p.problems()) problems.add("ultimate rows: " + s);
            rows.putAll(p.rows());
        }
        if (obj(root, "ultimates") != null) {
            for (Map.Entry<String, JsonElement> en : obj(root, "ultimates").entrySet()) {
                String id = en.getValue().isJsonPrimitive() ? en.getValue().getAsString() : "";
                if (!rows.containsKey(id)) {
                    problems.add("ultimates: '" + en.getKey() + "' names '" + id + "', which is not a row of 'abilities'");
                    continue;
                }
                ultOf.put(en.getKey(), id);
            }
        } else {
            problems.add("telegraphs: no 'ultimates' object");
        }
        return new Parsed(new TelegraphLibrary(kinds, overrides, ultOf, rows), problems);
    }

    private static TelegraphDef row(String where, JsonElement e, List<String> problems) {
        if (!e.isJsonObject()) {
            problems.add(where + ": not an object");
            return null;
        }
        JsonObject o = e.getAsJsonObject();
        TelegraphShape shape = TelegraphShape.of(str(o, "shape", "none"));
        if (shape == null) {
            problems.add(where + ": unknown shape '" + str(o, "shape", "") + "'");
            return null;
        }
        return new TelegraphDef(shape, bool(o, "armor", false), (int) num(o, "minWind", 0), bool(o, "target", false), num(o, "r", 3.0), num(o, "angle", 90.0), num(o, "width", 1.0));
    }

    /**
     * Audits the table against the foundry's own library: every override names a real ability, every ultimate row is a castable ability (not a passive, with a cooldown) that the main
     * library does not already have under that id, and every archetype of the contract has an ultimate. Returns the problems (empty = fine).
     */
    public List<String> check(FoundryLibrary lib) {
        List<String> bad = new ArrayList<>();
        for (String id : overrides.keySet()) if (lib.ability(id) == null && !ultimates.containsKey(id)) bad.add("override '" + id + "' is neither an ability of the foundry library nor an ultimate row");
        for (Map.Entry<String, String> en : ultimateOf.entrySet()) {
            if (lib.archetype(en.getKey()) == null) bad.add("ultimate for unknown archetype '" + en.getKey() + "'");
            FoundryAbility u = ultimates.get(en.getValue());
            if (u == null) continue;
            if (u.passive()) bad.add("ultimate '" + u.id() + "' is a passive");
            if (lib.ability(u.id()) != null) bad.add("ultimate '" + u.id() + "' reuses the id of a library ability");
            if (def(u) == null) bad.add("ultimate '" + u.id() + "' has no telegraph: an ultimate must warn");
        }
        for (String need : FoundryArchetype.CONTRACT) if (!ultimateOf.containsKey(need)) bad.add("archetype '" + need + "' has no ultimate");
        for (FoundryAbility.Kind k : kinds.keySet()) if (k == FoundryAbility.Kind.BURST) bad.add("kind BURST is a passive and cannot be telegraphed");
        return bad;
    }

    // ---------------------------------------------------------------- json helpers
    private static JsonObject obj(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonObject() ? o.getAsJsonObject(k) : null; }

    private static String str(JsonObject o, String k, String d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d; }

    private static boolean bool(JsonObject o, String k, boolean d) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsBoolean() : d;
        } catch (RuntimeException ex) {
            return d;
        }
    }

    private static double num(JsonObject o, String k, double d) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : d;
        } catch (RuntimeException ex) {
            return d;
        }
    }
}
