package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner asked that the room generator choose among the old and the new puzzles by theme and depth with weights he can tune, so every number of that choice is in ONE data file
 * ({@code data/nusmp/mega/puzzles/weights.json}, a datapack may replace it) and this class is its pure reader. It holds: whether puzzles are on, how many a floor may carry, how big a room
 * must be, how likely a room of each kind is to hold a puzzle at all, how much weight the OLD puzzles (the library's puzzle mechanics, the Spire's sigil rooms) keep in that draw, which
 * themes carry which flavour tags, and per puzzle its weight, the tiers it appears in, the roads it likes, the flavours it likes and dislikes and how well it suits each kind of room. A bad
 * file never breaks the game: unknown keys and bad numbers are reported and skipped, and the built-in defaults (every puzzle, equal weight) stay in force. Pure (Gson only).
 */
public final class PuzzleWeights {
    /** One puzzle's line of the file. */
    public record TypeW(String id, double weight, int tierLo, int tierHi, double holy, double abyssal, Set<String> likes, Set<String> dislikes, Map<String, Double> rooms) {}

    public final boolean enabled;
    public final int maxPerFloor, minSpan, bypassSeconds;
    public final double repeatDecay, familyDecay, likeBoost, dislikeCut;
    public final Map<String, Double> roomChance, legacy;
    public final List<String> openRooms;
    public final Map<String, List<String>> themeTags;
    public final Map<String, TypeW> types;

    private PuzzleWeights(boolean enabled, int maxPerFloor, int minSpan, int bypassSeconds, double repeatDecay, double familyDecay, double likeBoost, double dislikeCut, Map<String, Double> roomChance,
                          Map<String, Double> legacy, List<String> openRooms, Map<String, List<String>> themeTags, Map<String, TypeW> types) {
        this.enabled = enabled;
        this.maxPerFloor = maxPerFloor;
        this.minSpan = minSpan;
        this.bypassSeconds = bypassSeconds;
        this.repeatDecay = repeatDecay;
        this.familyDecay = familyDecay;
        this.likeBoost = likeBoost;
        this.dislikeCut = dislikeCut;
        this.roomChance = roomChance;
        this.legacy = legacy;
        this.openRooms = openRooms;
        this.themeTags = themeTags;
        this.types = types;
    }

    private static volatile PuzzleWeights current = defaults();

    public static PuzzleWeights get() { return current; }

    public static void install(PuzzleWeights w) { current = w == null ? defaults() : w; }

    /** The built-in fallback: every puzzle, equal weight, every tier. */
    public static PuzzleWeights defaults() {
        Map<String, TypeW> t = new LinkedHashMap<>();
        for (String id : PuzzleCatalog.ids()) t.put(id, new TypeW(id, 10, 1, 5, 1, 1, Set.of(), Set.of(), Map.of()));
        Map<String, Double> chance = new LinkedHashMap<>();
        chance.put("puzzle", 1.0);
        chance.put("hall", 0.10);
        chance.put("treasure", 0.10);
        chance.put("set", 0.08);
        chance.put("cavern", 0.05);
        Map<String, Double> legacy = new LinkedHashMap<>();
        legacy.put("puzzle", 30.0);
        return new PuzzleWeights(true, 3, 13, 420, 0.3, 0.6, 2.5, 0.4, chance, legacy, List.of("mega/puzzle/gallery"), Map.of(), t);
    }

    // ---------------------------------------------------------------- reading
    private static double num(JsonObject o, String k, double def, List<String> problems, String where) {
        JsonElement e = o.get(k);
        if (e == null) return def;
        try {
            return e.getAsDouble();
        } catch (RuntimeException ex) {
            problems.add(where + "." + k + " is not a number");
            return def;
        }
    }

    private static Map<String, Double> numMap(JsonObject o, String k, List<String> problems) {
        Map<String, Double> out = new LinkedHashMap<>();
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            if (en.getKey().startsWith("_")) continue;
            try {
                out.put(en.getKey(), en.getValue().getAsDouble());
            } catch (RuntimeException ex) {
                problems.add(k + "." + en.getKey() + " is not a number");
            }
        }
        return out;
    }

    private static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e != null && e.isJsonArray()) for (JsonElement x : (JsonArray) e) if (x.isJsonPrimitive()) out.add(x.getAsString());
        return out;
    }

    /** Parses the file; the problems list gets one line per thing skipped. Never throws; a file that is not an object gives the defaults. */
    public static PuzzleWeights parse(String json, List<String> problems) {
        PuzzleWeights def = defaults();
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            problems.add("weights.json is not a JSON object: " + e.getMessage());
            return def;
        }
        boolean enabled = !root.has("enabled") || root.get("enabled").getAsBoolean();
        int maxPerFloor = (int) Math.max(0, Math.min(12, num(root, "maxPerFloor", def.maxPerFloor, problems, "root")));
        int minSpan = (int) Math.max(7, Math.min(60, num(root, "minSpan", def.minSpan, problems, "root")));
        int bypass = (int) Math.max(60, Math.min(3600, num(root, "bypassSeconds", def.bypassSeconds, problems, "root")));
        double repeat = Math.max(0, Math.min(1, num(root, "repeatDecay", def.repeatDecay, problems, "root")));
        double family = Math.max(0, Math.min(1, num(root, "familyDecay", def.familyDecay, problems, "root")));
        double like = Math.max(1, Math.min(10, num(root, "likeBoost", def.likeBoost, problems, "root")));
        double dislike = Math.max(0, Math.min(1, num(root, "dislikeCut", def.dislikeCut, problems, "root")));
        Map<String, Double> chance = numMap(root, "roomChance", problems);
        if (chance.isEmpty()) chance = def.roomChance;
        Map<String, Double> legacy = numMap(root, "legacy", problems);
        List<String> open = strings(root.get("openRooms"));
        if (!root.has("openRooms")) open = def.openRooms;
        Map<String, List<String>> tags = new LinkedHashMap<>();
        if (root.has("themeTags") && root.get("themeTags").isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : root.getAsJsonObject("themeTags").entrySet()) if (!en.getKey().startsWith("_")) tags.put(en.getKey(), strings(en.getValue()));
        }
        Map<String, TypeW> types = new LinkedHashMap<>();
        if (root.has("types") && root.get("types").isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : root.getAsJsonObject("types").entrySet()) {
                String id = en.getKey();
                if (id.startsWith("_")) continue;
                if (!PuzzleCatalog.has(id)) { problems.add("types." + id + ": no such puzzle (skipped)"); continue; }
                if (!en.getValue().isJsonObject()) { problems.add("types." + id + " is not an object"); continue; }
                JsonObject o = en.getValue().getAsJsonObject();
                double w = Math.max(0, num(o, "weight", 10, problems, "types." + id));
                int lo = 1, hi = 5;
                List<String> tiers = new ArrayList<>();
                if (o.has("tiers") && o.get("tiers").isJsonArray() && o.getAsJsonArray("tiers").size() == 2) {
                    lo = Math.max(1, Math.min(5, o.getAsJsonArray("tiers").get(0).getAsInt()));
                    hi = Math.max(lo, Math.min(5, o.getAsJsonArray("tiers").get(1).getAsInt()));
                }
                double holy = 1, abyssal = 1;
                if (o.has("road") && o.get("road").isJsonObject()) {
                    holy = Math.max(0, num(o.getAsJsonObject("road"), "holy", 1, problems, "types." + id + ".road"));
                    abyssal = Math.max(0, num(o.getAsJsonObject("road"), "abyssal", 1, problems, "types." + id + ".road"));
                }
                types.put(id, new TypeW(id, w, lo, hi, holy, abyssal, new HashSet<>(strings(o.get("likes"))), new HashSet<>(strings(o.get("dislikes"))), numMap(o, "rooms", problems)));
            }
        }
        for (String id : PuzzleCatalog.ids()) if (!types.containsKey(id)) { problems.add("types." + id + ": missing (using the default line)"); types.put(id, def.types.get(id)); }
        return new PuzzleWeights(enabled, maxPerFloor, minSpan, bypass, repeat, family, like, dislike, Collections.unmodifiableMap(chance), Collections.unmodifiableMap(legacy), Collections.unmodifiableList(open),
                Collections.unmodifiableMap(tags), Collections.unmodifiableMap(types));
    }

    // ---------------------------------------------------------------- questions
    public List<String> tagsOf(String theme) { return theme == null ? List.of() : themeTags.getOrDefault(theme, List.of()); }

    /** The first tag of a theme that {@link Palettes#base} has a floor look for, else "". */
    public String flavourOf(String theme) {
        for (String t : tagsOf(theme)) if (!Palettes.base(t).equals(Palettes.base(null))) return t;
        return "";
    }

    /** Does a room of this template hold no puzzle apparatus of its own (an open gallery)? */
    public boolean isOpenRoom(String template) {
        if (template == null) return false;
        String t = template.indexOf(':') >= 0 ? template.substring(template.indexOf(':') + 1) : template;
        for (String p : openRooms) if (t.startsWith(p)) return true;
        return false;
    }

    /**
     * The weight of one puzzle for a room: the file's weight, zero outside its tiers, times its road factor, times the like boost or dislike cut for the theme's flavour tags, times how well it suits
     * the room kind, times the decay for every copy of it (and of its family) already on the floor.
     */
    public double weightOf(TypeW t, int tier, boolean abyssal, String theme, String roomKind, int sameUsed, int familyUsed) {
        if (t == null || tier < t.tierLo() || tier > t.tierHi()) return 0.0;
        double w = t.weight() * (abyssal ? t.abyssal() : t.holy());
        boolean liked = false, disliked = false;
        for (String tag : tagsOf(theme)) { liked |= t.likes().contains(tag); disliked |= t.dislikes().contains(tag); }
        if (liked) w *= likeBoost;
        if (disliked) w *= dislikeCut;
        w *= t.rooms().getOrDefault(roomKind, 1.0);
        w *= Math.pow(repeatDecay, sameUsed) * Math.pow(familyDecay, familyUsed);
        return Math.max(0.0, w);
    }

    /** The kinds of rooms the file mentions (for the tests). */
    public Set<String> roomKinds() {
        Set<String> out = new HashSet<>(roomChance.keySet());
        out.addAll(legacy.keySet());
        return out;
    }

    /** For the tests and the admin command: a copy of the type lines by id. */
    public Map<String, TypeW> typeMap() { return new HashMap<>(types); }
}
