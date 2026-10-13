package com.newuniverse.nusmp.dungeon.nexus.vault;

/**
 * Where a vault is: the interior box and which way its front wall (the one that looks out on the cave) faces. The interior is
 * {@code width} across the front, {@code depth} from the front wall to the back, {@code height} tall; (ix, iy, iz) is its lowest corner in
 * the world. Local coordinates are used by the layouts: u runs across the front (0 .. width-1), w runs from the front wall inwards (w = -1
 * is the wall itself, 0 .. depth-1 the interior, depth the back wall), y is the height above the interior's floor (y = -1 is the floor
 * block). The mapping to the world turns the room to face {@code facing}. Pure.
 */
public record VaultSite(VaultKind kind, int ix, int iy, int iz, int width, int height, int depth, int facing, boolean hidden, int variant) {
    /** The way the front wall looks: out of the room, towards the cave. */
    public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;

    public VaultSite {
        if (kind == null) throw new IllegalArgumentException("a site needs a kind");
        if (width < 1 || height < 1 || depth < 1) throw new IllegalArgumentException("a site needs a size");
        facing = Math.floorMod(facing, 4);
    }

    /** Extent of the interior along the world's x and z axes. */
    public int sizeX() { return (facing & 1) == 0 ? width : depth; }

    public int sizeZ() { return (facing & 1) == 0 ? depth : width; }

    public int maxX() { return ix + sizeX() - 1; }

    public int maxZ() { return iz + sizeZ() - 1; }

    public int maxY() { return iy + height - 1; }

    public int centreX() { return ix + sizeX() / 2; }

    public int centreZ() { return iz + sizeZ() / 2; }

    /** The unit step from the room towards the cave, in x and z. */
    public int outX() { return facing == EAST ? 1 : facing == WEST ? -1 : 0; }

    public int outZ() { return facing == SOUTH ? 1 : facing == NORTH ? -1 : 0; }

    /** World x of the local cell (u, w). */
    public int worldX(int u, int w) {
        return switch (facing) {
            case NORTH -> ix + u;
            case EAST -> ix + (depth - 1 - w);
            case SOUTH -> ix + (width - 1 - u);
            default -> ix + w;
        };
    }

    /** World z of the local cell (u, w). */
    public int worldZ(int u, int w) {
        return switch (facing) {
            case NORTH -> iz + w;
            case EAST -> iz + u;
            case SOUTH -> iz + (depth - 1 - w);
            default -> iz + (width - 1 - u);
        };
    }

    /** Is this block inside the room or its one-block shell, grown by `margin` blocks on every side? */
    public boolean contains(int x, int y, int z, int margin) {
        int m = 1 + Math.max(0, margin);
        return x >= ix - m && x <= maxX() + m && y >= iy - m && y <= maxY() + m && z >= iz - m && z <= maxZ() + m;
    }

    /** Is this block inside the interior itself (the air of the room), grown by `margin`? */
    public boolean insideInterior(int x, int y, int z, int margin) {
        int m = Math.max(0, margin);
        return x >= ix - m && x <= maxX() + m && y >= iy - m && y <= maxY() + m && z >= iz - m && z <= maxZ() + m;
    }

    /** Do two sites (shell and a margin) overlap? */
    public boolean intersects(VaultSite o, int margin) {
        int m = 1 + Math.max(0, margin);
        return ix - m <= o.maxX() + 1 && maxX() + m >= o.ix - 1 && iy - m <= o.maxY() + 1 && maxY() + m >= o.iy - 1
                && iz - m <= o.maxZ() + 1 && maxZ() + m >= o.iz - 1;
    }

    /** The cell just outside the middle of the front wall, at floor level: where a visitor stands before stepping through. */
    public int doorX() { return worldX(width / 2, -2); }

    public int doorZ() { return worldZ(width / 2, -2); }

    /** The chunks (x, z) the shell and the doorway touch: all of them must be loaded before building. */
    public java.util.List<int[]> chunks() {
        int x0 = Math.min(ix - 1, doorX()) - 1, x1 = Math.max(maxX() + 1, doorX()) + 1;
        int z0 = Math.min(iz - 1, doorZ()) - 1, z1 = Math.max(maxZ() + 1, doorZ()) + 1;
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++)
            for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) out.add(new int[]{cx, cz});
        return out;
    }
}
