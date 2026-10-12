package com.newuniverse.nusmp.dungeon.nexus.vault;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a Warp Trap throws its victim (docs/living_dungeon.md, "Spatial Warping"): a random place 8 to 24 blocks away on the same floor, safe
 * to stand in. The offsets come from a seed in a fixed order, the spot in each column is found through a {@link VaultTerrain}, and the first
 * safe one wins. Pure.
 */
public final class WarpPlan {
    private WarpPlan() {}

    public static final int MIN_DISTANCE = 8, MAX_DISTANCE = 24, REACH = 6, TRIES = 24;

    /** A horizontal offset (dx, dz) from the trap and the height (dy) the search for a spot starts from. */
    public record Offset(int dx, int dy, int dz) {}

    /** `n` offsets, each at a horizontal distance of MIN_DISTANCE to MAX_DISTANCE blocks (to the block), for a seed. */
    public static List<Offset> offsets(long seed, int n) {
        List<Offset> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            long h = VaultPlan.mix(seed, i, 0, 5);
            double dist = MIN_DISTANCE + Math.floorMod(h, (long) (MAX_DISTANCE - MIN_DISTANCE + 1));
            double angle = ((h >>> 16) & 0xFFFFL) / 65536.0 * Math.PI * 2.0;
            int dx = (int) Math.round(StrictMath.cos(angle) * dist), dz = (int) Math.round(StrictMath.sin(angle) * dist);
            while (dx * dx + dz * dz < MIN_DISTANCE * MIN_DISTANCE) {                 // rounding may land a little short or long: nudge the longer axis
                if (Math.abs(dx) >= Math.abs(dz)) dx += dx >= 0 ? 1 : -1; else dz += dz >= 0 ? 1 : -1;
            }
            while (dx * dx + dz * dz > MAX_DISTANCE * MAX_DISTANCE) {
                if (Math.abs(dx) >= Math.abs(dz)) dx -= dx >= 0 ? 1 : -1; else dz -= dz >= 0 ? 1 : -1;
            }
            int dy = (int) Math.floorMod(h >>> 40, 13L) - 6;
            out.add(new Offset(dx, dy, dz));
        }
        return out;
    }

    /**
     * The height at which someone can be set down in the column (x, z), looking outwards from `cy` (the height itself first, then one up, one
     * down, and so on to REACH); Integer.MIN_VALUE if there is none. A dry spot is two open cells of air over solid rock; a wet spot (only
     * when `wet`, for a victim who was in water) is two cells of water, or water under air. A spot is refused if any block of the 3 x 3 x 3
     * round it is lava or not loaded.
     */
    public static int spotInColumn(VaultTerrain t, int x, int cy, int z, boolean wet) {
        for (int step = 0; step <= 2 * REACH; step++) {
            int y = cy + ((step & 1) == 1 ? (step + 1) / 2 : -(step / 2));
            if (spotOk(t, x, y, z, wet)) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean spotOk(VaultTerrain t, int x, int y, int z, boolean wet) {
        int feet = t.at(x, y, z), head = t.at(x, y + 1, z), below = t.at(x, y - 1, z);
        if (feet == VaultTerrain.UNKNOWN || head == VaultTerrain.UNKNOWN || below == VaultTerrain.UNKNOWN) return false;
        boolean dry = feet == VaultTerrain.AIR && head == VaultTerrain.AIR && VaultTerrain.solid(below);
        boolean swim = wet && feet == VaultTerrain.WATER && (head == VaultTerrain.WATER || head == VaultTerrain.AIR);
        if (!dry && !swim) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int c = t.at(x + dx, y + dy, z + dz);
                    if (c == VaultTerrain.LAVA || c == VaultTerrain.UNKNOWN) return false;
                }
            }
        }
        return true;
    }
}
