package com.newuniverse.nusmp.labyrinth.mega;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: each Region (one sector of the Mega-Dungeon) has ONE "Relic Theme", and the theme is what makes two sectors feel different without a
 * second set of templates: the block palette (a vanilla family swapped for another by {@code nusmp:region_theme}), the ecology (which
 * factions and encounter tables dominate), the catastrophe the Crucible prefers and the pool of relics its pedestals may offer all read the
 * same id. The enum only NAMES the themes (it must exist at class-load time everywhere, in saved data, in processors, in JUnit); what a
 * theme MEANS is data ({@code data/nusmp/mega/themes/<id>.json}, read by {@code mega.theme.ThemeDefs}), so a datapack can retune a theme
 * without a mod update. Entries are only ever APPENDED (the ordinal is never saved: saved data and JSON use {@link #id()}).
 */
public enum RegionTheme {
    /** The baseline: the deepslate palette every template is drawn in, so this theme swaps nothing. Weights, falling stone, lightning rods. */
    GRAVITY(3, 3),
    /** Nether bricks and crimson wood: blood, bone, torture, the Crimson Reliquary. */
    BLOOD(3, 3),
    /** End stone, purpur, amethyst, crying obsidian: folded space, mirrors, doors that lead nowhere. */
    SPATIAL(3, 3),
    /** Blackstone, gilded blackstone, magma, lava forges. */
    FLAME(3, 3),
    /** Packed ice, snow, spruce and calcite. */
    FROST(3, 3),
    /** Mossy stone, moss, azalea, cherry and vines: an overgrown ruin. */
    VERDANT(3, 3),
    /** Oxidised copper, tuff, lightning rods. */
    STORM(3, 3),
    /** Polished blackstone, sculk, soul lanterns. */
    SHADOW(3, 3),
    /** Quartz, calcite, prismarine and gold: a cold observatory of stars. */
    ASTRAL(3, 3),
    /** 1.55.0, the Holy road: white quartz, smooth stone, gold and calcite, light everywhere, the air thick with Light Rukh. The Holy chain draws it most often. */
    RADIANT(3, 3),
    /** 1.55.0, the Abyssal road: dark organic rock, deepslate with sculk, moss and netherrack growing through it, soul fire. The Abyssal chain draws it most often. */
    ABYSS(3, 3);

    /**
     * The owner's newest brief fixes the Crucible at EXACTLY three distinct Unique-or-higher relics (one pick per region, so one high-tier relic per ten
     * floors), so every theme's range is 3..3. The range fields stay (the pure draw below still honours them) so a different count is a one-line edit.
     */
    private final int minRelics, maxRelics;

    RegionTheme(int minRelics, int maxRelics) {
        this.minRelics = minRelics;
        this.maxRelics = maxRelics;
    }

    /** Fewest relics the Crucible of a sector of this theme offers (1..3). */
    public int minRelics() { return minRelics; }

    /** Most relics the Crucible of a sector of this theme offers (1..3). */
    public int maxRelics() { return maxRelics; }

    /**
     * How many relics (1 to 3) the Crucible of sector {@code sectorId} offers: a deterministic draw in this theme's range from the dungeon seed.
     * The brief says the count is "determined by the sector's thematic NBT": the theme IS that NBT (saved in the sector), and a pure enum method
     * keeps the generator free of the theme data loader (which needs the game). A theme with a range of one number always gives it.
     */
    public int relicCount(long dungeonSeed, int sectorId) {
        if (maxRelics <= minRelics) return Math.max(1, Math.min(3, minRelics));
        long z = dungeonSeed * 0xD1B54A32D192ED03L + sectorId * 0x9E3779B97F4A7C15L + ordinal() * 0x94D049BB133111EBL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return Math.max(1, Math.min(3, minRelics + (int) Math.floorMod(z, (long) (maxRelics - minRelics + 1))));
    }

    /** The lower-case id used in saved data, JSON and lang keys ({@code gravity}). */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The theme with this id (case-insensitive), or {@link #GRAVITY} for null or an unknown id (an old world must still load). */
    public static RegionTheme fromId(String id) {
        if (id == null) return GRAVITY;
        for (RegionTheme t : values()) if (t.id().equalsIgnoreCase(id)) return t;
        return GRAVITY;
    }

    /**
     * 1.55.0: the polarity-aware draw. The first sector of a road wears the road's own signature ({@link #RADIANT} for Holy, {@link #ABYSS} for Abyssal), so the
     * two roads look different from the first step; later sectors are drawn with weights that favour the themes that fit the road (Holy: radiant, astral,
     * frost, spatial, storm; Abyssal: abyss, shadow, blood, flame, verdant) three to one over the rest, never equal to the previous sector's theme. Deterministic
     * (splitmix64 of the dungeon seed, the chain index and the polarity). The older three-argument {@link #pick} is kept for single-chain worlds.
     */
    public static RegionTheme pick(long dungeonSeed, int chainIndex, RegionTheme previous, Polarity polarity) {
        if (polarity == null) return pick(dungeonSeed, chainIndex, previous);
        if (chainIndex <= 0) return polarity == Polarity.HOLY ? RADIANT : ABYSS;
        List<RegionTheme> pool = new ArrayList<>();
        for (RegionTheme t : values()) {
            if (t == previous) continue;
            int w = polarity == Polarity.HOLY ? (t.favoursHoly() ? 3 : 1) : (t.favoursAbyss() ? 3 : 1);
            for (int i = 0; i < w; i++) pool.add(t);
        }
        if (pool.isEmpty()) return GRAVITY;
        long z = dungeonSeed * 0x9E3779B97F4A7C15L + (chainIndex * 2L + polarity.ordinal()) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return pool.get((int) Math.floorMod(z, (long) pool.size()));
    }

    /** Does this theme suit the Holy road (bright, cold, clean)? */
    public boolean favoursHoly() { return this == RADIANT || this == ASTRAL || this == FROST || this == SPATIAL || this == STORM; }

    /** Does this theme suit the Abyssal road (dark, organic, hot)? */
    public boolean favoursAbyss() { return this == ABYSS || this == SHADOW || this == BLOOD || this == FLAME || this == VERDANT; }

    /**
     * Deterministic theme of a sector: sector 0 is always {@link #GRAVITY} (the baseline look is the one the player meets first), later ones
     * are drawn from a splitmix64 of {@code dungeonSeed} and the sector id, never equal to the previous sector's theme (two identical regions
     * in a row would read as a bug, not as a theme).
     */
    public static RegionTheme pick(long dungeonSeed, int sectorId, RegionTheme previous) {
        if (sectorId <= 0) return GRAVITY;
        List<RegionTheme> pool = new ArrayList<>(List.of(values()));
        if (previous != null && pool.size() > 1) pool.remove(previous);
        long z = dungeonSeed * 0x9E3779B97F4A7C15L + sectorId * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return pool.get((int) Math.floorMod(z, (long) pool.size()));
    }
}
