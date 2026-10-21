package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: the room data that flags a challenge room. A room's rule is found in this order: its exact template id (a hand-picked room), then the first {@code patterns}
 * entry whose text the template's name contains (all the forges, all the barracks), then its kind's folder ({@code treasure}, {@code set}, {@code hall} ...), and a
 * room that none of them mentions does not lock. The first matching level wins whole (a level does not inherit from the next), so a hand-picked room can opt OUT
 * with {@code "lock": false} as easily as in. The parser is strict and loud like the encounter tables' (an unknown key, a bad band or a negative number is a
 * line of text for the log and for the shipped-file test) and never throws: a file that is not a JSON object gives the empty table and one problem. Pure: Gson and
 * plain enums only.
 */
public final class LockdownTable {
    public static final LockdownTable EMPTY = new LockdownTable(Map.of(), Map.of(), List.of());

    /** One {@code patterns} entry: a name fragment and the rule for templates that contain it. */
    public record Pattern(String match, LockdownRule rule) {}

    /** The parse: the table (never null) and every problem found. */
    public record Parsed(LockdownTable table, List<String> problems) {
        public boolean clean() { return problems.isEmpty(); }
    }

    private static final Set<String> ROOT_KEYS = Set.of("comment", "defaults", "kinds", "templates", "patterns");
    private static final Set<String> RULE_KEYS = Set.of("lock", "chance", "band", "waves", "minFloor", "minArea", "note", "match");

    private final Map<String, LockdownRule> kinds, templates;
    private final List<Pattern> patterns;

    public LockdownTable(Map<String, LockdownRule> kinds, Map<String, LockdownRule> templates, List<Pattern> patterns) {
        this.kinds = Map.copyOf(kinds);
        this.templates = Map.copyOf(templates);
        this.patterns = List.copyOf(patterns);
    }

    public Map<String, LockdownRule> kinds() { return kinds; }

    public Map<String, LockdownRule> templates() { return templates; }

    public List<Pattern> patterns() { return patterns; }

    public boolean isEmpty() { return kinds.isEmpty() && templates.isEmpty() && patterns.isEmpty(); }

    /** {@code nusmp:mega/hall/barracks_a} and {@code mega/hall/barracks_a} are both {@code mega/hall/barracks_a}. */
    public static String normalise(String templateId) {
        if (templateId == null) return "";
        String s = templateId.trim().toLowerCase(Locale.ROOT);
        int colon = s.indexOf(':');
        return colon >= 0 ? s.substring(colon + 1) : s;
    }

    /** The rule of a room: its template, else a pattern, else its kind, else {@link LockdownRule#NONE}. */
    public LockdownRule ruleFor(String templateId, RoomKind kind) {
        String t = normalise(templateId);
        LockdownRule exact = templates.get(t);
        if (exact != null) return exact;
        String name = t.contains("/") ? t.substring(t.lastIndexOf('/') + 1) : t;
        for (Pattern p : patterns) if (name.contains(p.match())) return p.rule();
        if (kind != null) {
            LockdownRule k = kinds.get(kindKey(kind));
            if (k != null) return k;
        }
        return LockdownRule.NONE;
    }

    // ---------------------------------------------------------------- the parser
    public static Parsed parse(String name, String json) {
        List<String> problems = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement e = JsonParser.parseString(json == null ? "" : json);
            if (!e.isJsonObject()) {
                problems.add(name + ": the file is not a JSON object");
                return new Parsed(EMPTY, problems);
            }
            root = e.getAsJsonObject();
        } catch (RuntimeException bad) {
            problems.add(name + ": not valid JSON (" + String.valueOf(bad.getMessage()).replace('\n', ' ') + ")");
            return new Parsed(EMPTY, problems);
        }
        for (String k : root.keySet()) if (!ROOT_KEYS.contains(k)) problems.add(name + ": unknown key '" + k + "'");
        JsonObject defaults = root.has("defaults") && root.get("defaults").isJsonObject() ? root.getAsJsonObject("defaults") : new JsonObject();
        LockdownRule base = rule(name + " defaults", defaults, new LockdownRule(true, 1.0, Band.STANDARD, 1, 1, 0, ""), problems);

        Map<String, LockdownRule> kinds = new LinkedHashMap<>();
        if (root.has("kinds") && root.get("kinds").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("kinds").entrySet()) {
                String key = e.getKey().toLowerCase(Locale.ROOT);
                RoomKind rk = kindOf(key);
                if (rk == null) {
                    problems.add(name + ": kinds." + e.getKey() + " is not a room kind");
                    continue;
                }
                if (!e.getValue().isJsonObject()) {
                    problems.add(name + ": kinds." + e.getKey() + " is not an object");
                    continue;
                }
                kinds.put(kindKey(rk), rule(name + " kinds." + e.getKey(), e.getValue().getAsJsonObject(), base, problems));
            }
        }
        Map<String, LockdownRule> templates = new LinkedHashMap<>();
        if (root.has("templates") && root.get("templates").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("templates").entrySet()) {
                if (!e.getValue().isJsonObject()) {
                    problems.add(name + ": templates." + e.getKey() + " is not an object");
                    continue;
                }
                String id = normalise(e.getKey());
                if (!id.startsWith("mega/")) problems.add(name + ": templates." + e.getKey() + " is not a mega template id");
                templates.put(id, rule(name + " templates." + e.getKey(), e.getValue().getAsJsonObject(), base, problems));
            }
        }
        List<Pattern> patterns = new ArrayList<>();
        if (root.has("patterns") && root.get("patterns").isJsonArray()) {
            JsonArray a = root.getAsJsonArray("patterns");
            for (int i = 0; i < a.size(); i++) {
                if (!a.get(i).isJsonObject()) {
                    problems.add(name + ": patterns[" + i + "] is not an object");
                    continue;
                }
                JsonObject o = a.get(i).getAsJsonObject();
                String match = o.has("match") && o.get("match").isJsonPrimitive() ? o.get("match").getAsString().trim().toLowerCase(Locale.ROOT) : "";
                if (match.length() < 3) {
                    problems.add(name + ": patterns[" + i + "] needs a 'match' of at least three characters");
                    continue;
                }
                patterns.add(new Pattern(match, rule(name + " patterns[" + i + "]", o, base, problems)));
            }
        }
        return new Parsed(new LockdownTable(kinds, templates, patterns), problems);
    }

    /** The key of a kind in the file: the folder its templates live in ({@code set}, {@code hall}), which is what the owner sees; a kind with no folder uses its enum name. */
    public static String kindKey(RoomKind k) { return k.folder().isEmpty() ? k.name().toLowerCase(Locale.ROOT) : k.folder(); }

    /** The kind a key of the file names (its folder or its enum name, any case), or null. */
    static RoomKind kindOf(String key) {
        for (RoomKind k : RoomKind.values()) if (kindKey(k).equals(key) || k.name().toLowerCase(Locale.ROOT).equals(key)) return k;
        return null;
    }

    private static LockdownRule rule(String where, JsonObject o, LockdownRule base, List<String> problems) {
        for (String k : o.keySet()) if (!RULE_KEYS.contains(k)) problems.add(where + ": unknown key '" + k + "'");
        boolean lock = bool(where, o, "lock", base.lock(), problems);
        double chance = number(where, o, "chance", base.chance(), 0.0, 1.0, problems);
        Band band = base.band();
        if (o.has("band")) {
            Band b = o.get("band").isJsonPrimitive() ? Band.byName(o.get("band").getAsString()) : null;
            if (b == null || b == Band.NOTHING) problems.add(where + ": band must be minor, standard, hard or ambush");
            else band = b;
        }
        int waves = (int) number(where, o, "waves", base.waves(), 1, LockdownRule.MAX_WAVES, problems);
        int minFloor = (int) number(where, o, "minFloor", base.minFloor(), 1, 1000, problems);
        int minArea = (int) number(where, o, "minArea", base.minArea(), 0, 100000, problems);
        String note = o.has("note") && o.get("note").isJsonPrimitive() ? o.get("note").getAsString() : base.note();
        return new LockdownRule(lock, chance, band, waves, minFloor, minArea, note);
    }

    private static boolean bool(String where, JsonObject o, String key, boolean def, List<String> problems) {
        if (!o.has(key)) return def;
        JsonElement e = o.get(key);
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
        problems.add(where + ": '" + key + "' must be true or false");
        return def;
    }

    private static double number(String where, JsonObject o, String key, double def, double lo, double hi, List<String> problems) {
        if (!o.has(key)) return def;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            problems.add(where + ": '" + key + "' must be a number");
            return def;
        }
        double v = e.getAsDouble();
        if (v < lo || v > hi) {
            problems.add(where + ": '" + key + "' " + v + " is outside " + lo + ".." + hi + " (clamped)");
            return Math.max(lo, Math.min(hi, v));
        }
        return v;
    }
}
