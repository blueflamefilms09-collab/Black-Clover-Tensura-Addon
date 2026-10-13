package com.newuniverse.nusmp.dungeon.nexus.vault;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which columns of a chunk may hold a Warp Trap (docs/living_dungeon.md, "Spatial Warping"). The traps are laid lazily, once a chunk near a
 * player is loaded, in the chunk's distorted pockets only (the glue asks the nexus's SpatialDistortion for the strength at each floor): this
 * class says which eight columns of the chunk are looked at, which of them are wanted at all (one in four), and which of the column's
 * floors is chosen. At most {@link #MAX_PER_CHUNK} traps are laid in a chunk. Pure.
 */
public final class TrapPlan {
    private TrapPlan() {}

    public static final int COLUMNS = 8, MAX_PER_CHUNK = 2, CHANCE_PERCENT = 25;
    /** A floor needs at least this much distortion (0 to 1) to hold a trap. */
    public static final double MIN_STRENGTH = 0.25;

    /** A column of the chunk and a number that decides whether and where in it a trap goes. */
    public record Column(int x, int z, int roll) {}

    /** The eight distinct columns of the chunk that are looked at, in order. */
    public static List<Column> columns(int chunkX, int chunkZ) {
        List<Column> out = new ArrayList<>(COLUMNS);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; out.size() < COLUMNS && i < 256; i++) {
            long h = VaultPlan.mix(chunkX, chunkZ, i, 41);
            int lx = (int) Math.floorMod(h, 16L), lz = (int) Math.floorMod(h >>> 8, 16L);
            if (!seen.add(lx * 16 + lz)) continue;
            out.add(new Column(chunkX * 16 + lx, chunkZ * 16 + lz, (int) Math.floorMod(h >>> 20, 100_000L)));
        }
        return out;
    }

    /** Is a trap wanted in this column at all? */
    public static boolean wanted(Column c) { return c.roll() % 100 < CHANCE_PERCENT; }

    /** Which of `count` floors of the column holds the trap (0 .. count-1), or -1 if there are none. */
    public static int pickFloor(Column c, int count) { return count <= 0 ? -1 : Math.floorMod(c.roll() / 100, count); }
}
