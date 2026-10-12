package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.BlockProps;

import java.util.Set;

/**
 * The slow cycle that keeps the Golden City's garden alive (docs/living_dungeon.md, "The Golden City"): flowers bloom and wilt, grass is
 * grazed bare and comes back, moss spreads and recedes, vines grow down and are drawn up again, berries ripen, the shores of the pools
 * rise and fall, and mini-groves sprout, grow into small trees and are pruned. Every answer is a pure function of the place and the game
 * time, so nothing has to be saved, the whole garden shifts in slow waves, and anything that is gone or picked comes back the next time
 * it is looked at. {@link Biosphere} is the glue that looks at a few places near the players on every pass; this class only says what
 * belongs in a place at a time. It never touches a path, a pool, a tree or the city: {@link #mayWrite} says which kinds of ground it may
 * change. Pure.
 */
final class BiosphereCycle {
    private BiosphereCycle() {}

    /** Ticks between two passes over one garden, how far from a player a pass looks, how many ground places and vines it tries for each player, and the most players it serves. */
    static final int PASS_TICKS = 60, RADIUS = 26, SAMPLES = 40, VINE_TRIES = 5, MAX_PLAYERS = 4;
    /** The most blocks one pass writes in all: a handful of plants and vines, or one grove changing stage (its 3 x 3 x 5 cells). */
    static final int MAX_WRITES = 48, GROVE_CELLS = 45;

    /** May a grove be rebuilt in this pass, given how many blocks the pass has written so far? Its cells count in full, so the budget never overshoots. */
    static boolean mayRebuildGrove(int written) { return written + GROVE_CELLS <= MAX_WRITES; }

    /** Game ticks one stage of a flower, a vine or a grove lasts. */
    static final long STAGE_TICKS = 900L, VINE_TICKS = 1200L, GROVE_TICKS = 1800L, TIDE_TICKS = 3600L, MOSS_TICKS = 7200L;
    static final int STAGES = 6, GROVE_STAGES = 4;

    /** A block to put in a place: its registry id and its properties ("" for none). */
    record Cover(String block, String props) {
        static final Cover AIR = new Cover("minecraft:air", "");

        boolean air() { return block.equals("minecraft:air"); }
    }

    private static final String[] FLOWERS = {"minecraft:poppy", "minecraft:dandelion", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet",
            "minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower",
            "minecraft:lily_of_the_valley", "minecraft:torchflower"};

    /** What the cycle may replace in a column's cover cell: the plants it puts there and nothing else (a block of any other kind is left alone). */
    static final Set<String> FLORA = Set.of("minecraft:air", "minecraft:short_grass", "minecraft:fern", "minecraft:moss_carpet", "minecraft:pink_petals",
            "minecraft:poppy", "minecraft:dandelion", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip",
            "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower",
            "minecraft:lily_of_the_valley", "minecraft:torchflower");

    /** What may stand in the cells of a grove, in any of its stages. */
    static final Set<String> GROVE_BLOCKS = Set.of("minecraft:air", "minecraft:azalea", "minecraft:flowering_azalea", "minecraft:azalea_leaves",
            "minecraft:flowering_azalea_leaves", GoldenFlora.LOG, GoldenFlora.GOLD_LEAVES);

    /** May the cycle change the cover of this kind of column (see {@link GoldenGrid#NONE} and its siblings)? */
    static boolean mayWrite(byte kind) {
        return kind == GoldenGrid.FLOWER || kind == GoldenGrid.GRASS || kind == GoldenGrid.FERN || kind == GoldenGrid.MOSS;
    }

    // ---------------------------------------------------------------- time and place
    /** A triangle wave of a number of whole periods: 0 .. 1 .. 0. */
    static double tri(long now, long period, int offset) {
        long phase = Math.floorMod(now + offset * (period / 8), period);
        double t = phase / (double) period;
        return t < 0.5 ? t * 2.0 : 2.0 - t * 2.0;
    }

    /** The stage (0 .. STAGES-1) a flower, a tuft of grass or a fern is in at a place and time. */
    static int stage(long seed, int u, int v, long now) {
        int off = GoldenRng.hash(seed, u, v, 61) % STAGES;
        return (int) Math.floorMod(now / STAGE_TICKS + off, (long) STAGES);
    }

    // ---------------------------------------------------------------- ground cover
    /** What belongs in the cover cell of a column of this kind at this time (Cover.AIR if the cell should be empty). */
    static Cover cover(long seed, byte kind, int u, int v, long now) {
        switch (kind) {
            case GoldenGrid.FLOWER: {
                int s = stage(seed, u, v, now);
                if (s == 0) return Cover.AIR;
                if (s == 1 || s == 5) return new Cover("minecraft:short_grass", "");
                int h = GoldenRng.hash(seed, u, v, 62);
                if (h % 11 == 0) return new Cover("minecraft:pink_petals", "flower_amount=" + (1 + (h >> 4) % 4) + ",facing=" + BlockProps.FACINGS[(h >> 8) % 4]);
                return new Cover(FLOWERS[(h >> 4) % FLOWERS.length], "");
            }
            case GoldenGrid.GRASS:
                return stage(seed, u, v, now) == 4 ? Cover.AIR : new Cover("minecraft:short_grass", "");
            case GoldenGrid.FERN: {
                int s = stage(seed, u, v, now);
                return s == 5 ? Cover.AIR : s == 4 ? new Cover("minecraft:short_grass", "") : new Cover("minecraft:fern", "");
            }
            case GoldenGrid.MOSS:
                return mossAt(seed, u, v, now) ? new Cover("minecraft:moss_carpet", "") : Cover.AIR;
            case GoldenGrid.LICHEN:
                return new Cover("minecraft:glow_lichen", "down=true");
            default:
                return Cover.AIR;
        }
    }

    /** Moss carpet lies where the noise of the place is above a mark that rises and falls over a few minutes: it spreads, then recedes. */
    static boolean mossAt(long seed, int u, int v, long now) {
        double level = GoldenRng.noise(seed, u, v, 9, 63);
        return level >= 0.34 + 0.32 * tri(now, MOSS_TICKS, GoldenRng.hash(seed, 0, 0, 64) % 8);
    }

    // ---------------------------------------------------------------- the shores of the pools
    /** Is the shore cell under water at this time? The shore rises and falls over an hour, a little differently along its length. */
    static boolean shoreWet(long seed, int u, int v, long now) {
        double level = GoldenRng.noise(seed, u, v, 5, 71);
        return level < 0.22 + 0.55 * tri(now, TIDE_TICKS, GoldenRng.hash(seed, 1, 1, 72) % 8);
    }

    // ---------------------------------------------------------------- vines
    /** How many cells of its length a vine has out at this time: it grows down and is drawn up again, never shorter than 2 (or its whole length if that is less). */
    static int vineLength(long seed, int u, int v, int max, long now) {
        if (max <= 2) return Math.max(0, max);
        int off = GoldenRng.hash(seed, u, v, 65) % 8;
        return 2 + (int) Math.round((max - 2) * tri(now, VINE_TICKS * 8, off));
    }

    /** Are the glow berries of a vine ripe at this time? Four fifths of the time they are. */
    static boolean ripe(long seed, int u, int v, long now) {
        int off = GoldenRng.hash(seed, u, v, 66) % 5;
        return Math.floorMod(now / VINE_TICKS + off, 5L) != 0;
    }

    // ---------------------------------------------------------------- groves
    /** The stage of a grove: bare, a sapling, a bush, then a small tree for half of the cycle, and pruned back to bare. */
    static int groveStage(int phase, long now) {
        int[] seq = {0, 1, 2, 3, 3, 3};
        return seq[(int) Math.floorMod(now / GROVE_TICKS + phase, 6L)];
    }

    // ---------------------------------------------------------------- where to look
    /**
     * The k-th place to look at in one pass, as an offset (dx, dz) from a player within `radius` blocks of them: a stable pick from the pass
     * number, so a test can replay it and the pass costs the same every time.
     */
    static int[] sample(long salt, long pass, int k, int radius) {
        for (int t = 0; t < 8; t++) {
            int dx = GoldenRng.hash(salt ^ pass * 0x9E3779B97F4A7C15L, k, t, 81) % (2 * radius + 1) - radius;
            int dz = GoldenRng.hash(salt ^ pass * 0xC2B2AE3D27D4EB4FL, k, t, 82) % (2 * radius + 1) - radius;
            if (dx * dx + dz * dz <= radius * radius) return new int[]{dx, dz};
        }
        return new int[]{0, 0};
    }
}
