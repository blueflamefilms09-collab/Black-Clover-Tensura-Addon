package com.newuniverse.nusmp.mana.method;

/**
 * The real blocks True Magic leaves behind on impact (pure, tested). True Fire sets a few real fires, True Water real Water Source blocks, True Earth a small mound of stone and
 * True Plant moss; True Wind and True Lightning are not blocks and place nothing. Everything is bounded: a handful of blocks per cast, per caster and in the whole world, and
 * every block is taken back after a while ({@link #seconds}); the placement itself (protected areas, loaded chunks, free cells) is {@code TrueBlocks}.
 */
public final class TrueBlockRules {
    private TrueBlockRules() {}

    public enum Kind { NONE, FIRE, WATER, STONE, MOSS }

    /** The most blocks one cast places, one caster has standing, and the whole server has standing. */
    public static final int PER_CAST = 7, PER_CASTER = 21, GLOBAL = 160;
    public static final int DEFAULT_SECONDS = 30, MIN_SECONDS = 5, MAX_SECONDS = 300;

    public static Kind kindOf(RuneElement e) {
        if (e == null) return Kind.NONE;
        return switch (e) {
            case FIRE -> Kind.FIRE;
            case WATER -> Kind.WATER;
            case EARTH -> Kind.STONE;
            case PLANT -> Kind.MOSS;
            default -> Kind.NONE;
        };
    }

    /** The registry id of the block (a vanilla block). */
    public static String blockId(Kind k) {
        return switch (k) {
            case FIRE -> "minecraft:fire";
            case WATER -> "minecraft:water";
            case STONE -> "minecraft:stone";
            case MOSS -> "minecraft:moss_carpet";
            default -> "";
        };
    }

    /** How many blocks a cast of this kind wants (at most {@link #PER_CAST}). */
    public static int blocksWanted(Kind k) {
        return switch (k) {
            case FIRE, WATER -> 5;
            case STONE, MOSS -> 7;
            default -> 0;
        };
    }

    /** The cells round the impact, as (dx, dz) pairs: the impact cell first, then its four neighbours, then two diagonals. */
    private static final int[][] CELLS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}};

    public static int[] cell(int i) { return CELLS[Math.floorMod(i, CELLS.length)]; }

    public static int cellCount() { return CELLS.length; }

    /** Does the kind need a solid block under the cell (fire, stone and moss sit on the ground; water may be set anywhere free)? */
    public static boolean needsGround(Kind k) { return k == Kind.FIRE || k == Kind.STONE || k == Kind.MOSS; }

    /** How many of {@code wanted} may be placed given what this caster and the world already have standing. Never negative. */
    public static int allowed(int wanted, int casterLive, int globalLive, int casterCap, int globalCap) {
        int room = Math.min(casterCap - casterLive, globalCap - globalLive);
        return Math.max(0, Math.min(Math.min(wanted, PER_CAST), room));
    }

    /** The lifetime of a placed block in seconds, from the config, kept in range. */
    public static int seconds(int configured) { return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, configured)); }
}
