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
        if (path.startsWith("black_sword_greed")) return "berserk";
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
