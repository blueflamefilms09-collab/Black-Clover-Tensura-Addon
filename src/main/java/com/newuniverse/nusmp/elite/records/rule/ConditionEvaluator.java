package com.newuniverse.nusmp.elite.records.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: this is the whole unlock logic of the records system, one pure function per condition type, so every one of the twenty types is unit-tested without the game. {@link #evaluate} reads the
 * {@link Facts} snapshot only. The convention of the {@code Facts} keys (the server's {@link FactsBuilder} fills them) is:
 * <pre>
 * numbers: deaths, kills_all|hostile|passive|player|boss, distance_all|walk|sprint|swim|fly|elytra|boat|mount (metres), ep, single_hit, forge_count, forge_best_quality,
 *          ftb_chunks, calamity_damage, calamity_kills, walpurgis, war_kills, war_wins, demon_status (0 none 1 seed 2 true), hero_status (0 none 1 egg 2 true), higher_being (0 no 1 spiritual 2 divine)
 * counters: kills (entity id), calamity_kills (calamity id), forge_quality ("1".."6")
 * sets: biomes, structures, skills, skills:&lt;tensura skill type&gt;
 * </pre>
 * A type whose outside source is missing (see {@link CondType#source()}) answers UNAVAILABLE. Id parameters accept a trailing {@code *} as a prefix wildcard ({@code minecraft:village_*}) and a bare path
 * means the {@code minecraft} namespace.
 */
public final class ConditionEvaluator {
    private ConditionEvaluator() {}

    public static final String[] DISTANCE_MODES = {"all", "walk", "sprint", "swim", "fly", "elytra", "boat", "mount"};
    public static final String[] KILL_GROUPS = {"all", "hostile", "passive", "player", "boss"};
    public static final String[] SKILL_TYPES = {"resistance", "intrinsic", "common", "extra", "unique", "ultimate"};
    private static final String[] QUALITY_NAMES = {"common", "uncommon", "rare", "epic", "legendary", "mythical"};

    public static Eval evaluate(Condition c, Facts f) {
        if (c == null || f == null) return Eval.UNAVAILABLE;
        CondType t = c.type();
        if (!t.source().isEmpty() && f.unavailable(t.source())) return Eval.UNAVAILABLE;
        return switch (t) {
            case BIOME_DISCOVERY -> discovered(c, f, "biomes", "biome");
            case STRUCTURE_DISCOVERY -> discovered(c, f, "structures", "structure");
            case CALAMITY_DAMAGE -> Eval.atLeast(f.num("calamity_damage"), c.num("amount", 0));
            case CALAMITY_KILL -> calamityKill(c, f);
            case DEATH_COUNT -> Eval.atLeast(f.num("deaths"), count(c));
            case DISTANCE_TRAVELED -> Eval.atLeast(f.num("distance_" + mode(c)), c.num("meters", 0));
            case EP_REACHED -> Eval.atLeast(f.num("ep"), c.num("ep", 0));
            case FORGE_QUALITY -> Eval.atLeast(f.num("forge_best_quality"), quality(c, "quality", 1));
            case FORGE_COUNT -> forgeCount(c, f);
            case FTB_CHUNKS_CLAIMED -> Eval.atLeast(f.num("ftb_chunks"), count(c));
            case DEMON_LORD_STATUS -> Eval.atLeast(f.num("demon_status"), "seed".equalsIgnoreCase(c.str("status", "true")) ? 1 : 2);
            case HIGHER_BEING_STATUS -> Eval.atLeast(f.num("higher_being"), "divine".equalsIgnoreCase(c.str("status", "spiritual")) ? 2 : 1);
            case TRUE_HERO_STATUS -> Eval.atLeast(f.num("hero_status"), "egg".equalsIgnoreCase(c.str("status", "true")) ? 1 : 2);
            case KILL_ENTITY -> killEntity(c, f);
            case KILL_COUNT -> Eval.atLeast(f.num("kills_" + group(c)), count(c));
            case SINGLE_HIT_DAMAGE -> Eval.atLeast(f.num("single_hit"), c.num("damage", 0));
            case SKILL_LEARNED -> skill(c, f);
            case WALPURGIS_ATTENDED -> Eval.atLeast(f.num("walpurgis"), count(c));
            case WAR_KILLS -> Eval.atLeast(f.num("war_kills"), count(c));
            case WAR_WINS -> Eval.atLeast(f.num("war_wins"), count(c));
        };
    }

    // ------------------------------------------------------------------ the compound ones
    private static Eval discovered(Condition c, Facts f, String setName, String single) {
        Set<String> s = f.set(setName);
        String id = c.str(single, "");
        if (!id.isEmpty()) {
            for (String x : s) if (idMatches(id, x)) return Eval.atLeast(1, 1);
            return Eval.atLeast(0, 1);
        }
        String ns = c.str("namespace", "");
        int n = 0;
        for (String x : s) if (ns.isEmpty() || x.startsWith(ns + ":")) n++;
        return Eval.atLeast(n, count(c));
    }

    private static Eval calamityKill(Condition c, Facts f) {
        String id = c.str("calamity", "");
        if (id.isEmpty()) return Eval.atLeast(f.num("calamity_kills"), count(c));
        int n = 0;
        for (Map.Entry<String, Integer> e : f.counts("calamity_kills").entrySet()) if (idMatches(id, e.getKey())) n += e.getValue();
        return Eval.atLeast(n, count(c));
    }

    private static Eval forgeCount(Condition c, Facts f) {
        int min = (int) quality(c, "min_quality", 0);
        if (min <= 1) return Eval.atLeast(f.num("forge_count"), count(c));
        int n = 0;
        for (Map.Entry<String, Integer> e : f.counts("forge_quality").entrySet()) {
            try { if (Integer.parseInt(e.getKey()) >= min) n += e.getValue(); } catch (NumberFormatException ignored) { /* a foreign key: not counted */ }
        }
        return Eval.atLeast(n, count(c));
    }

    private static Eval killEntity(Condition c, Facts f) {
        List<String> ids = new ArrayList<>(c.strings("entities"));
        String one = c.str("entity", "");
        if (!one.isEmpty()) ids.add(one);
        int n = 0;
        for (Map.Entry<String, Integer> e : f.counts("kills").entrySet()) {
            for (String p : ids) if (idMatches(p, e.getKey())) { n += e.getValue(); break; }
        }
        return Eval.atLeast(n, count(c));
    }

    private static Eval skill(Condition c, Facts f) {
        String id = c.str("skill", "");
        if (!id.isEmpty()) {
            for (String x : f.set("skills")) if (idMatches(id, x)) return Eval.atLeast(1, 1);
            return Eval.atLeast(0, 1);
        }
        String type = c.str("skill_type", "");
        Set<String> s = f.set(type.isEmpty() ? "skills" : "skills:" + type.toLowerCase(Locale.ROOT));
        return Eval.atLeast(s.size(), count(c));
    }

    // ------------------------------------------------------------------ parameter readers
    private static double count(Condition c) { return Math.max(1, c.num("count", 1)); }

    private static String mode(Condition c) {
        String m = c.str("mode", "all").toLowerCase(Locale.ROOT);
        for (String x : DISTANCE_MODES) if (x.equals(m)) return m;
        return "all";
    }

    private static String group(Condition c) {
        String g = c.str("group", "all").toLowerCase(Locale.ROOT);
        for (String x : KILL_GROUPS) if (x.equals(g)) return g;
        return "all";
    }

    /** 1..6 from a number or a quality word ("rare", "mythical"/"mythic"); {@code def} when absent or unreadable. */
    static double quality(Condition c, String key, double def) {
        if (!c.has(key)) return def;
        String s = c.str(key, "").trim().toLowerCase(Locale.ROOT);
        if (s.equals("mythic")) return 6;
        for (int i = 0; i < QUALITY_NAMES.length; i++) if (QUALITY_NAMES[i].equals(s)) return i + 1;
        double v = c.num(key, def);
        return Math.max(0, Math.min(6, Math.floor(v)));
    }

    /** True when {@code have} is the id {@code pattern} names (exact, bare path = minecraft, trailing * = prefix). */
    public static boolean idMatches(String pattern, String have) {
        if (pattern == null || have == null) return false;
        String p = pattern.indexOf(':') < 0 ? "minecraft:" + pattern : pattern;
        if (p.endsWith("*")) return have.startsWith(p.substring(0, p.length() - 1));
        return p.equals(have);
    }

    // ------------------------------------------------------------------ validation (data files)
    /** Problems of a condition's parameters (empty when it is sound). Used when a data file is read and by the data tests. */
    public static List<String> validate(Condition c) {
        List<String> out = new ArrayList<>();
        CondType t = c.type();
        switch (t) {
            case BIOME_DISCOVERY -> needIdOrCount(c, out, "biome");
            case STRUCTURE_DISCOVERY -> needIdOrCount(c, out, "structure");
            case CALAMITY_DAMAGE -> positive(c, out, "amount");
            case CALAMITY_KILL, DEATH_COUNT, FTB_CHUNKS_CLAIMED, WALPURGIS_ATTENDED, WAR_KILLS, WAR_WINS -> positive(c, out, "count", true);
            case DISTANCE_TRAVELED -> {
                positive(c, out, "meters");
                oneOf(c, out, "mode", DISTANCE_MODES);
            }
            case EP_REACHED -> positive(c, out, "ep");
            case FORGE_QUALITY -> {
                if (!validQuality(c, "quality")) out.add("quality must be 1..6 or a quality word");
            }
            case FORGE_COUNT -> {
                positive(c, out, "count", true);
                if (c.has("min_quality") && !validQuality(c, "min_quality")) out.add("min_quality must be 1..6 or a quality word");
            }
            case DEMON_LORD_STATUS -> oneOf(c, out, "status", "seed", "true");
            case HIGHER_BEING_STATUS -> oneOf(c, out, "status", "spiritual", "divine");
            case TRUE_HERO_STATUS -> oneOf(c, out, "status", "egg", "true");
            case KILL_ENTITY -> {
                if (c.str("entity", "").isEmpty() && c.strings("entities").isEmpty()) out.add("kill_entity needs 'entity' or 'entities'");
                positive(c, out, "count", true);
            }
            case KILL_COUNT -> {
                positive(c, out, "count", true);
                oneOf(c, out, "group", KILL_GROUPS);
            }
            case SINGLE_HIT_DAMAGE -> positive(c, out, "damage");
            case SKILL_LEARNED -> {
                if (c.str("skill", "").isEmpty()) positive(c, out, "count", true);
                oneOf(c, out, "skill_type", SKILL_TYPES);
            }
        }
        return out;
    }

    /** True when the key holds a quality word or a whole number 1..6 (the reader clamps, the validator does not). */
    private static boolean validQuality(Condition c, String key) {
        if (!c.has(key)) return false;
        String s = c.str(key, "").trim().toLowerCase(Locale.ROOT);
        if (s.equals("mythic")) return true;
        for (String n : QUALITY_NAMES) if (n.equals(s)) return true;
        double v = c.num(key, 0);
        return v >= 1 && v <= 6 && v == Math.floor(v);
    }

    private static void needIdOrCount(Condition c, List<String> out, String idKey) {
        if (c.str(idKey, "").isEmpty()) positive(c, out, "count");
    }

    private static void positive(Condition c, List<String> out, String key) { positive(c, out, key, false); }

    private static void positive(Condition c, List<String> out, String key, boolean defaultsToOne) {
        if (!c.has(key)) {
            if (!defaultsToOne) out.add("'" + key + "' is required");
            return;
        }
        if (c.num(key, 0) <= 0) out.add("'" + key + "' must be above 0");
    }

    private static void oneOf(Condition c, List<String> out, String key, String... allowed) {
        if (!c.has(key)) return;
        String v = c.str(key, "").toLowerCase(Locale.ROOT);
        for (String a : allowed) if (a.equals(v)) return;
        out.add("'" + key + "' must be one of " + String.join("|", allowed));
    }
}
