package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.MawShapes.Mat;
import com.newuniverse.nusmp.dungeon.MawShapes.Plan;
import com.newuniverse.nusmp.dungeon.MawShapes.Put;

import java.util.ArrayList;
import java.util.List;

/**
 * Does a {@link MawShapes} plan fit the ground it was asked to stand on, and what exactly has to be written? Pure: the ground is a
 * {@link Terrain} (MawManifest reads the world through one), so every refusal is tested without the game. Nothing is written here; a plan
 * that does not fit gives null, and DungeonMaw then tries another shape or makes the plain mouth.
 */
public final class MawFit {
    private MawFit() {}

    /** The most blocks one manifestation writes, footings included. */
    public static final int MAX_WRITES = MawShapes.MAX_PUTS + 200;

    /** What a cell is, to the scan: free (air), soft (grass, flowers, leaves, snow: it may be built over), natural ground, a fluid, or anything else (built by someone, or alive). */
    public enum Cell { AIR, SOFT, GROUND, FLUID, OTHER }

    /** What the scan may ask of the world. Nothing in an unloaded chunk may be read. */
    public interface Terrain {
        /** Is the chunk of this column loaded? */
        boolean loaded(int x, int z);

        /** The y of the first free cell above the ground of this column (leaves do not count as ground). Only asked of loaded columns. */
        int height(int x, int z);

        /** What is in this cell. Only asked of loaded columns. */
        Cell cell(int x, int y, int z);

        /** Is anything alive inside this box (all six bounds inclusive)? */
        boolean occupied(int x1, int y1, int z1, int x2, int y2, int z2);

        /** The lowest y that can be built on. */
        int minY();

        /** One above the highest y that can be built on. */
        int maxY();
    }

    /** One block to write, in world coordinates. */
    public record Place(int x, int y, int z, Mat mat, int variant) {}

    /** The floor level (the y of the first free cell above the highest column) and everything to write, footings first, then the plan in its order. */
    public record Fit(int base, List<Place> places) {}

    /** The stone a footing is made of: mostly cobble, some mossy. A hash of the place, so it is the same every time. */
    static Mat footing(int x, int y, int z) { return ((x * 31 + y * 17 + z) & 3) == 0 ? Mat.MOSSY_COBBLE : Mat.COBBLE; }

    /**
     * Checks the plan against the ground with its centre column at (cx, cz) and returns what to write, or null if it does not fit:
     * a chunk it touches is not loaded; the surface is too uneven or the doorway would hang over a cliff; it would leave the world's height;
     * anything lives in the patch; something other than air, grass, leaves and the like is in the way above the ground, or anything other
     * than natural ground below it; there is water in it; or a column would hang over a drop deeper than a footing can fill.
     */
    public static Fit scan(Plan plan, Terrain t, int cx, int cz) {
        int r = plan.reach();
        if (!t.loaded(cx - r, cz - r) || !t.loaded(cx + r, cz - r) || !t.loaded(cx - r, cz + r) || !t.loaded(cx + r, cz + r)) return null;
        int[] cols = plan.columns();
        int base = Integer.MIN_VALUE, low = Integer.MAX_VALUE;                                  // the highest column sets the floor, lower ones are footed up to it
        for (int i = 0; i < cols.length; i += 2) {
            int h = t.height(cx + cols[i], cz + cols[i + 1]);
            base = Math.max(base, h);
            low = Math.min(low, h);
        }
        if (base - low > plan.maxSlope()) return null;
        int[] door = plan.approach();
        if (door != null && base - t.height(cx + door[0], cz + door[1]) > MawShapes.MAX_STEP) return null;   // the doorway must not hang over a cliff
        if (base + plan.maxY() + 2 >= t.maxY() || base + plan.minY() - MawShapes.FOOT_MAX <= t.minY() + 1) return null;
        if (t.occupied(cx - r, base + plan.minY(), cz - r, cx + r, base + plan.maxY(), cz + r)) return null;   // the walls must not close on anyone
        List<Place> places = new ArrayList<>();
        int[] feet = plan.footings();
        for (int i = 0; i < feet.length; i += 3) {                                               // under every column of stone, down to the ground
            int x = cx + feet[i], z = cz + feet[i + 1];
            for (int k = 0; ; k++) {
                int y = base + feet[i + 2] - 1 - k;
                Cell c = t.cell(x, y, z);
                if (c == Cell.FLUID) return null;
                if (c != Cell.AIR && c != Cell.SOFT) break;
                if (k == MawShapes.FOOT_MAX) return null;                                        // it hangs over a drop
                places.add(new Place(x, y, z, footing(x, y, z), 0));
            }
        }
        for (Put p : plan.puts()) {
            int x = cx + p.x(), y = base + p.y(), z = cz + p.z();
            Cell c = t.cell(x, y, z);
            if (c == Cell.FLUID) return null;
            boolean free = c == Cell.AIR || c == Cell.SOFT;
            if (!free && !(p.y() < 0 && c == Cell.GROUND)) return null;                          // only soft things above the ground; natural ground below it
            if (p.mat() == Mat.AIR && c == Cell.AIR) continue;                                   // nothing to clear
            places.add(new Place(x, y, z, p.mat(), p.variant()));
        }
        return places.size() > MAX_WRITES ? null : new Fit(base, places);
    }
}
