package com.newuniverse.nusmp.skilltree.rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Why: the owner's rarity spec wants the decennial Guardian's book to be THEMED ("the Floor 50 fire boss always drops the same Epic book", in every world): the book's own background theme (one of
 * the 15 backgrounds of the book table: magma, void, stellar, forest, ocean, storm, ruin, blood, holy, frost, tower, city, dream, sand, iron) has to fit the sector's Region Theme (the dungeon's 27 themes:
 * flame, frost, necropolis, ...). This is the table between the two, and the deterministic pick that uses it. It is pure (the theme enters as its saved id string, so no dungeon class is loaded, and a unit
 * test walks every {@code RegionTheme} to prove that none was left without an entry: a theme added later fails that test instead of silently dropping "any book").
 *
 * <p>The pick is a function of (floor, theme id, rarity) and of the list of books, NOTHING else: no world seed, no dice, no clock, so the same boss gives the same book in every world and on every
 * restart. The candidates are the books of the wanted rarity that are not blocked and not mythic; those whose background is one of the theme's backgrounds come first; when none fits the whole rarity
 * list is the list ("unmapped = any"); when the rarity itself has no usable book the pick steps down one rarity at a time. The seeded start index is where the pity rule walks from: a player who
 * already owns the chosen book gets the NEXT book of the same list (wrapping round), then the next, exactly like the pity of the monster drops; when the themed list is entirely owned the walk goes on through the
 * whole rarity list, and only a player who owns every book of the rarity is handed the seeded one again (worth nothing but the mastery of a duplicate, the same as the old pity). Cost: one sort of at most a few
 * hundred small records per Guardian death, no allocation per tick.
 */
public final class BossThemes {
    private BossThemes() {}

    /** The 15 backgrounds of the book table, the vocabulary of {@code tools/lines/catalog.py BACKGROUNDS}. */
    public static final List<String> BACKGROUNDS = List.of("magma", "void", "stellar", "forest", "ocean", "storm", "ruin", "blood", "holy", "frost", "tower", "city", "dream", "sand", "iron");

    private static final Map<String, List<String>> THEMES = new LinkedHashMap<>();

    private static void t(String theme, String... backgrounds) { THEMES.put(theme, List.of(backgrounds)); }

    static {
        t("gravity", "iron", "tower", "ruin");                  // weights, falling stone, lightning rods
        t("blood", "blood", "magma");                           // nether bricks, bone, the Crimson Reliquary
        t("spatial", "void", "dream", "stellar");               // folded space, mirrors, doors that lead nowhere
        t("flame", "magma", "iron");                            // blackstone, lava forges
        t("frost", "frost");                                    // packed ice, snow
        t("verdant", "forest", "ruin");                         // an overgrown ruin
        t("storm", "storm", "iron");                            // oxidised copper, lightning rods
        t("shadow", "void", "ruin", "blood");                   // blackstone, sculk, soul lanterns
        t("astral", "stellar", "holy");                         // a cold observatory of stars
        t("radiant", "holy", "stellar", "city");                // white quartz and gold, Light Rukh
        t("abyss", "void", "blood", "dream");                   // dark organic rock, sculk, soul fire
        t("feywild_grove", "forest", "dream");                  // the Feywild, glowing flowers and tricks
        t("dwarven_forgehold", "iron", "magma");                // a dwarven hold, anvils, lava channels
        t("crystal_geode", "stellar", "dream", "ruin");         // a giant amethyst geode
        t("sunken_temple", "ocean", "holy", "ruin");            // a flooded temple of a forgotten good god
        t("holy_church", "holy", "city");                       // a cathedral, stained glass
        t("celestial_court", "holy", "stellar", "city");        // the marble terraces of a heavenly court
        t("sky_citadel", "storm", "tower", "holy");             // a cloud giant's castle in the sky
        t("mountain_monastery", "tower", "sand", "forest");     // a monks' retreat of timber and paper
        t("cursed_archive", "ruin", "void", "dream");           // a library of forbidden knowledge
        t("necropolis", "sand", "ruin", "blood");               // a city of the dead
        t("vampire_castle", "blood", "tower", "city");          // Ravenloft, a grey gothic keep
        t("haunted_manor", "dream", "void", "city");            // a haunted house
        t("fungal_underdark", "forest", "void", "dream");       // the Underdark, huge mushrooms
        t("infernal_foundry", "magma", "iron", "blood");        // the Nine Hells as a factory
        t("drowned_crypt", "ocean", "ruin", "void");            // a sunken crypt
        t("spider_warrens", "void", "forest", "dream");         // a spider queen's warren
    }

    /** The theme ids that have an entry, in table order. */
    public static List<String> themeIds() { return List.copyOf(THEMES.keySet()); }

    /** The backgrounds that fit a theme id (case-insensitive); an empty list for an unknown or null id, which the pick reads as "any". */
    public static List<String> backgroundsOf(String themeId) {
        if (themeId == null) return List.of();
        List<String> l = THEMES.get(themeId.toLowerCase(Locale.ROOT));
        return l == null ? List.of() : l;
    }

    // ------------------------------------------------------------------ the deterministic pick
    /**
     * One book as the pick sees it.
     *
     * @param id      the line's bare id ({@code lady_luck}); the list is ordered by it
     * @param rarity  its rarity
     * @param theme   its background theme, one of {@link #BACKGROUNDS} (empty when unknown: it fits no theme but is still in the rarity list)
     * @param blocked true when its franchise is on the blacklist
     */
    public record Cand(String id, BookRarity rarity, String theme, boolean blocked) {}

    /** SplitMix64's golden constant. */
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** FNV-1a over the characters: a stable 64-bit number for a name, the same on every Java version. */
    static long fnv(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    /** The seed of a (floor, theme, rarity): the same three values give the same number in every world. The sign of the floor does not matter. */
    public static long seed(int floor, String themeId, BookRarity rarity) {
        int depth = floor == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(floor);
        String theme = themeId == null ? "" : themeId.toLowerCase(Locale.ROOT);
        return mix(fnv(theme) ^ mix(depth * GOLDEN + rarity.rank() + 1));
    }

    /**
     * The book a Guardian of this (floor, theme) drops.
     *
     * @param all    every book of the table (any order; copied and sorted by id, so the caller's order never matters)
     * @param wanted the rarity the depth asks for ({@link RarityRules#guardianRarity}); a mythic ask is treated as epic
     * @param owned  does the opener already own the book with this id (null: owns nothing)
     * @return the book's id, or null when the table has no usable book at all
     */
    public static String pick(List<Cand> all, BookRarity wanted, int floor, String themeId, Predicate<String> owned) {
        if (all == null || all.isEmpty() || wanted == null) return null;
        List<Cand> sorted = new ArrayList<>(all.size());
        for (Cand c : all) if (c != null && !c.blocked() && c.rarity() != BookRarity.MYTHIC && c.id() != null) sorted.add(c);
        sorted.sort(Comparator.comparing(Cand::id));
        BookRarity r = wanted == BookRarity.MYTHIC ? BookRarity.EPIC : wanted;
        for (int rank = r.rank(); rank >= 0; rank--) {
            BookRarity level = BookRarity.fromRank(rank);
            List<Cand> pool = new ArrayList<>();
            for (Cand c : sorted) if (c.rarity() == level) pool.add(c);
            if (pool.isEmpty()) continue;
            List<String> fits = backgroundsOf(themeId);
            List<Cand> themed = new ArrayList<>();
            if (!fits.isEmpty()) for (Cand c : pool) if (fits.contains(c.theme())) themed.add(c);
            long seed = seed(floor, themeId, level);
            String chosen = walk(themed.isEmpty() ? pool : themed, seed, owned);
            if (chosen != null && !themed.isEmpty() && owns(owned, chosen)) {
                String wider = walk(pool, seed, owned);                          // the themed list is all owned: go on through the whole rarity
                if (wider != null && !owns(owned, wider)) chosen = wider;
            }
            if (chosen != null) return chosen;
        }
        return null;
    }

    private static boolean owns(Predicate<String> owned, String id) { return owned != null && owned.test(id); }

    /** From the seeded start, the first book not owned (wrapping round); the seeded book itself when every one is owned. */
    private static String walk(List<Cand> list, long seed, Predicate<String> owned) {
        int n = list.size();
        if (n == 0) return null;
        int start = (int) Math.floorMod(seed, (long) n);
        for (int k = 0; k < n; k++) {
            Cand c = list.get((start + k) % n);
            if (!owns(owned, c.id())) return c.id();
        }
        return list.get(start).id();
    }
}
