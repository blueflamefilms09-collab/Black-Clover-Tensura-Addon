package com.newuniverse.nusmp.franchise;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 3.88.0 (task 28): which franchise creative tab an item belongs to. Pure (unit-tested): the franchise id from {@link FranchiseMap} plus a few id rules for items the map does not list
 * (beast wills, rank cores, Greed, devil items, Djinn items, hero-weapon gems). Every franchise tab is an extra view; the older tabs keep their items.
 */
public final class FranchiseTabs {
    private FranchiseTabs() {}

    /** Tab ids, in display order. Each has the lang key {@code itemGroup.anime_multiverse.franchise_<id>}. */
    public static final List<String> TABS = List.of("black_clover", "tensura", "berserk", "fairy_tail", "magi", "hero_weapons", "anime_worlds");

    /**
     * 4.96.0: one more tab per verse (anime world), so the owner can see which verse has too many items and which has too few. Appended after {@link #TABS}; the lang key is
     * {@code itemGroup.anime_multiverse.franchise_<id>}. An item can be in its older tab and its verse tab. Dragon Ball and Shield Hero come first.
     */
    public static final List<String> VERSES = List.of("dragon_ball", "shield_hero", "danmachi", "seven_deadly_sins", "naruto", "bleach", "one_piece", "hunter_x_hunter", "jojo",
            "solo_leveling", "dnd", "rezero", "fate", "overlord", "konosuba", "mushoku_tensei", "eminence_in_shadow", "tower_of_god", "fullmetal_alchemist", "sao_log_horizon",
            "jujutsu_kaisen", "frieren", "delicious_in_dungeon", "made_in_abyss", "tomb_raider_king", "yu_gi_oh", "exiled_heavy_knight", "reincarnated_as_a_sword");

    /** Item counts (distinct items) that the report calls thin / heavy. */
    public static final int THIN = 15, HEAVY = 200;

    /** The report word for a verse with {@code items} distinct items. */
    public static String status(int items) {
        if (items <= 0) return "EMPTY: needs content";
        if (items < THIN) return "thin: needs more";
        if (items > HEAVY) return "heavy: a lot already";
        return "ok";
    }

    /** Dragon Ball by id: the balls, the radar and the Saiyan alloy set (the older tabs know nothing about Dragon Ball). */
    public static boolean dragonBall(String path) {
        return path != null && (path.startsWith("dragon_ball_") || path.equals("dragon_radar") || path.startsWith("saiyan_alloy"));
    }

    /** The verse tab of an item (a member of {@link #VERSES}), or null. */
    public static String verseOf(String path, String franchise) {
        if (dragonBall(path)) return "dragon_ball";
        return franchise != null && VERSES.contains(franchise) ? franchise : null;
    }

    /** Every tab the item is shown in: its older franchise tab ({@link #tabOf}) and its verse tab. Empty when it has none. */
    public static List<String> tabsOf(String path, String franchise) {
        String primary = tabOf(path, franchise);
        String verse = verseOf(path, franchise);
        if (primary == null && verse == null) return List.of();
        if (verse == null || verse.equals(primary)) return List.of(primary);
        return primary == null ? List.of(verse) : List.of(primary, verse);
    }

    private static final Pattern BOSS_CORE = Pattern.compile("core_(aegis|striker|ascendant|wellspring_(magicule|aura|ki|nihility))|(aegis|striker|ascendant|wellspring_(magicule|aura|ki|nihility))_(helmet|chestplate|leggings|boots)");

    /** The boss drop framework (bossdrops): the 23 boss trophies, the Trophy Plate armour and Lucifero's armour. */
    private static final Pattern BOSS_DROP = Pattern.compile("trophy_[a-z_]+|lucifero_(helmet|chestplate|leggings|boots)");

    private static final Pattern RANK_CORE =Pattern.compile("core_[a-z]{1,2}_(standard|beast|ancient)");

    /** The tab id of an item path, or null when the item belongs to no franchise tab. {@code franchise} is {@link FranchiseMap#franchiseOf}'s answer (may be null). */
    public static String tabOf(String path, String franchise) {
        if (franchise != null && !franchise.isEmpty()) {
            switch (franchise) {
                case "black_clover": return "black_clover";
                case "tensura": return "tensura";
                case "berserk": return "berserk";
                case "fairy_tail": return "fairy_tail";
                case "magi": return "magi";
                case "shield_hero": return "hero_weapons";
                default: return "anime_worlds";
            }
        }
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("black_sword_greed") || com.newuniverse.nusmp.sinweapons.SinRules.IDS.contains(path)) return "berserk";
        if (BOSS_CORE.matcher(path).matches()) return "tensura";          // the Boss Cores and their armour (labyrinth/mega/cores): Tensura-side progression gear
        if (BOSS_DROP.matcher(path).matches()) return path.matches("trophy_(minotaur|black_goliath|amphisbaena|udaeus)") ? "anime_worlds" : "black_clover";      // the boss trophies and the armour the grimoire bosses drop (bossdrops)
        if (path.startsWith("beast_will") || RANK_CORE.matcher(path).matches()) return "tensura";
        if (path.startsWith("saiyan_alloy")) return "anime_worlds";          // the craftable Saiyan alloy set (Dragon Ball has no tab of its own)
        if (path.startsWith("djinn_")) return "magi";
        if (path.contains("grimoire") || path.startsWith("devil_")) return "black_clover";
        if (path.startsWith("calling_gem") || path.startsWith("gem_fragment") || path.equals("gneiss_crystal") || path.equals("release_crystal") || path.equals("purification_charm")) return "hero_weapons";
        return null;
    }
}
