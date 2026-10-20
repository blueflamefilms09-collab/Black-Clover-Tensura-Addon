package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

/**
 * Why: a plan is drawn in its own frame (x east, z south, the Gatekeeper to the north, y 0 the walking surface) but a hall stands in the world, and in a floor of the Mega-Dungeon the hall's own template
 * may have been turned a quarter, a half or three quarters of a turn by the jigsaw. This record is the one place that converts between the two: the world column of the hall's middle, the world height of
 * the walking surface, the size, and the quarter turns. It is pure, so the conversion is tested both ways, and every part of the runtime (the placer, the element events, the seal, the sync to clients)
 * asks the same object instead of repeating the arithmetic.
 *
 * @param cx   the world x of the middle of the hall
 * @param y0   the world y of the walking surface (local y 0)
 * @param cz   the world z of the middle of the hall
 * @param size the footprint (odd)
 * @param rot  quarter turns clockwise (seen from above) the plan is turned by
 */
public record ArenaFrame(int cx, int y0, int cz, int size, int rot) {
    public ArenaFrame {
        rot = ((rot % 4) + 4) % 4;
    }

    private int c() { return size / 2; }

    /** The world position of a local cell: {@code out} receives x, y, z. */
    public void world(int x, int y, int z, int[] out) {
        int dx = x - c(), dz = z - c(), tx, tz;
        switch (rot) {
            case 1 -> { tx = -dz; tz = dx; }
            case 2 -> { tx = -dx; tz = -dz; }
            case 3 -> { tx = dz; tz = -dx; }
            default -> { tx = dx; tz = dz; }
        }
        out[0] = cx + tx;
        out[1] = y0 + y;
        out[2] = cz + tz;
    }

    /** The local cell of a world position (out receives x, y, z); the cell may lie outside the footprint (see {@link #contains}). */
    public void local(int wx, int wy, int wz, int[] out) {
        int tx = wx - cx, tz = wz - cz, dx, dz;
        switch (rot) {
            case 1 -> { dx = tz; dz = -tx; }
            case 2 -> { dx = -tx; dz = -tz; }
            case 3 -> { dx = -tz; dz = tx; }
            default -> { dx = tx; dz = tz; }
        }
        out[0] = c() + dx;
        out[1] = wy - y0;
        out[2] = c() + dz;
    }

    /** Is the world column inside the footprint (all heights)? */
    public boolean containsColumn(int wx, int wz) {
        int h = c();
        return Math.abs(wx - cx) <= h && Math.abs(wz - cz) <= h;
    }

    /** Is the world position inside the hall's volume ({@code height} rows above the surface, the foundation below it)? */
    public boolean contains(int wx, int wy, int wz, int height) {
        return containsColumn(wx, wz) && wy >= y0 - BossArenaStructure.FOUNDATION && wy < y0 + height;
    }

    /** The world horizontal distance from the middle of the hall. */
    public double distance(double wx, double wz) { return Math.hypot(wx - cx, wz - cz); }

    /** The quarter turns that carry the plan's north (where its Gatekeeper is) to the direction of the world offset {@code (dx, dz)} (the larger axis wins); 0 for a zero offset. */
    public static int rotationFacing(int dx, int dz) {
        if (dx == 0 && dz == 0) return 0;
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? 1 : 3;
        return dz > 0 ? 2 : 0;
    }
}
