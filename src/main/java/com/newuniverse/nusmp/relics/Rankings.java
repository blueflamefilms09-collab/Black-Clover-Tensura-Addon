package com.newuniverse.nusmp.relics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the owner wants rankings for everything in the mod, built like the relics' grades. This is the one generic, data-driven table (assets/anime_multiverse/rankings.json, docs/rankings.md):
 * item id (registry path) to a {@link Rank}: one of the relic {@link Grade}s plus an optional place inside it (1 = best). Pure (Gson and the Grade enum, no game state), so it is unit-tested.
 * The order of resolution lives in {@link #of}: explicit item, the Djinn order, a rule (prefix, suffix, contains; first match), then {@link WeaponGrades}. The game side adds the relic card's own
 * grade first and the tooltip category's default last ({@code tooltip/RankTooltip}).
 */
public final class Rankings {
    private Rankings() {}

    /** A rank: the grade, the place inside it (0 = not placed) out of {@code of} (0 = unknown), and where it came from ("item", "djinn", "rule", "weapon"). */
    public record Rank(Grade grade, int place, int of, String source) {
        /** "Genesis Grade", "God Grade #4". */
        public String label() { return grade.label() + (place > 0 ? " #" + place : ""); }
    }

    /** One loaded table; {@link #table()} is the mod's own. */
    public static final class Table {
        private final List<String> djinnOrder = new ArrayList<>();
        private final List<Object[]> djinnTiers = new ArrayList<>();       // {Grade, Integer upTo}
        private final Map<String, Rank> items = new LinkedHashMap<>();
        private final List<String[]> rules = new ArrayList<>();            // {kind, value, grade}

        public List<String> djinnOrder() { return java.util.Collections.unmodifiableList(djinnOrder); }

        public Map<String, Rank> items() { return java.util.Collections.unmodifiableMap(items); }

        public int rules() { return rules.size(); }

        /** The 1-based rank of a Djinn id (1 = the strongest), 0 if it is not listed. */
        public int djinnRank(String djinnId) { return djinnId == null ? 0 : djinnOrder.indexOf(djinnId.toLowerCase(Locale.ROOT)) + 1; }

        public int djinnCount() { return djinnOrder.size(); }

        /** The rank of a Djinn by its place in the order (null if it is not listed). */
        public Rank djinn(String djinnId) {
            int r = djinnRank(djinnId);
            if (r <= 0) return null;
            int from = 0;
            for (Object[] t : djinnTiers) {
                int upTo = (Integer) t[1];
                if (r <= upTo) return new Rank((Grade) t[0], r - from, upTo - from, "djinn");
                from = upTo;
            }
            Grade last = djinnTiers.isEmpty() ? Grade.LEGEND : (Grade) djinnTiers.get(djinnTiers.size() - 1)[0];
            return new Rank(last, 0, 0, "djinn");
        }

        /** The Djinn id of a Djinn weapon or Seal item path ("djinn_weapon_baal", "djinn_weapon_baal_dark", "djinn_seal_baal"), else null. */
        public String djinnIdOfItem(String path) {
            if (path == null) return null;
            String s = path;
            if (s.startsWith("djinn_weapon_")) s = s.substring("djinn_weapon_".length());
            else if (s.startsWith("djinn_seal_")) s = s.substring("djinn_seal_".length());
            else return null;
            if (s.endsWith("_dark")) s = s.substring(0, s.length() - 5);
            if (s.equals("belial_scythe")) s = "belial";                                       // the canon Belial scythe ranks as Belial
            return djinnRank(s) > 0 ? s : null;
        }

        /** The rank of an item path (lower-case registry path), or null if the table, its rules and {@link WeaponGrades} do not know it. */
        public Rank of(String path) {
            if (path == null) return null;
            String p = path.toLowerCase(Locale.ROOT);
            Rank r = items.get(p);
            if (r != null) return r;
            String dj = djinnIdOfItem(p);
            if (dj != null) return djinn(dj);
            for (String[] rule : rules) {
                boolean hit = switch (rule[0]) {
                    case "prefix" -> p.startsWith(rule[1]);
                    case "suffix" -> p.endsWith(rule[1]);
                    default -> p.contains(rule[1]);
                };
                if (hit) return new Rank(Grade.byName(rule[2]), 0, 0, "rule");
            }
            WeaponGrades.Entry w = WeaponGrades.of(p);
            if (w != null) return new Rank(Grade.values()[Math.max(0, Math.min(Grade.values().length - 1, w.grade()))], 0, 0, "weapon");
            return null;
        }
    }

    /** Parses a table. A bad entry (unknown grade, wrong shape) is skipped, never thrown: a broken table must not stop the game. */
    public static Table parse(JsonObject o) {
        Table t = new Table();
        try {
            if (o.has("djinnOrder")) for (JsonElement e : o.getAsJsonArray("djinnOrder")) t.djinnOrder.add(e.getAsString().toLowerCase(Locale.ROOT));
            if (o.has("djinnTiers")) for (JsonElement e : o.getAsJsonArray("djinnTiers")) {
                JsonArray a = e.getAsJsonArray();
                Grade g = Grade.byName(a.get(0).getAsString());
                if (g != null) t.djinnTiers.add(new Object[]{g, a.get(1).getAsInt()});
            }
            if (o.has("items")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("items").entrySet()) {
                String[] parts = e.getValue().getAsString().split(":");
                Grade g = Grade.byName(parts[0]);
                if (g == null) continue;
                int place = 0;
                if (parts.length > 1) {
                    try { place = Math.max(0, Integer.parseInt(parts[1].trim())); } catch (NumberFormatException ignored) { place = 0; }
                }
                t.items.put(e.getKey().toLowerCase(Locale.ROOT), new Rank(g, place, 0, "item"));
            }
            if (o.has("rules")) for (JsonElement e : o.getAsJsonArray("rules")) {
                JsonObject r = e.getAsJsonObject();
                String kind = r.has("prefix") ? "prefix" : r.has("suffix") ? "suffix" : r.has("contains") ? "contains" : null;
                if (kind == null || Grade.byName(r.get("rank").getAsString()) == null) continue;
                t.rules.add(new String[]{kind, r.get(kind).getAsString().toLowerCase(Locale.ROOT), r.get("rank").getAsString()});
            }
        } catch (RuntimeException broken) {
            // keep what was read
        }
        return t;
    }

    public static Table parse(String json) { return parse(JsonParser.parseString(json).getAsJsonObject()); }

    private static volatile Table loaded;

    /** The mod's table, read once from assets/anime_multiverse/rankings.json (empty, with only the WeaponGrades fallback, if the file is missing). */
    public static Table table() {
        Table t = loaded;
        if (t != null) return t;
        try (InputStream in = Rankings.class.getResourceAsStream("/assets/anime_multiverse/rankings.json")) {
            t = in == null ? new Table() : parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception ignored) {
            t = new Table();
        }
        loaded = t;
        return t;
    }

    public static Rank of(String path) { return table().of(path); }

    /** The Djinn's rank number (1 = strongest) in the mod's table, 0 if unknown. */
    public static int djinnRank(String djinnId) { return table().djinnRank(djinnId); }

    public static int djinnCount() { return table().djinnCount(); }
}
