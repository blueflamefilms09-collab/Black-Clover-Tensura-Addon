package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: every placement decision of the assembler is a box question (does the new piece fit inside the region, touch another piece, poke into a
 * neighbouring sector?), so the box semantics are written down once. Coordinates are INCLUSIVE cell corners, exactly like the game's
 * {@code BoundingBox} ({@code getXSpan() = maxX - minX + 1}), which makes two pieces that stand face to face have boxes that are adjacent
 * (max of one + 1 = min of the other) and therefore NOT overlapping: vanilla Jigsaw tests the same thing with {@code deflate(0.25)}. Pure integer maths,
 * immutable, with value equality.
 */
public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public Box {
        if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("empty box " + minX + "," + minY + "," + minZ + ".." + maxX + "," + maxY + "," + maxZ);
    }

    /** The box through two corner cells in any order. */
    public static Box of(Vec a, Vec b) {
        return new Box(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z()), Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()));
    }

    public int sizeX() { return maxX - minX + 1; }

    public int sizeY() { return maxY - minY + 1; }

    public int sizeZ() { return maxZ - minZ + 1; }

    /** Do the two boxes share at least one cell? (Face to face with nothing shared is NOT an overlap.) */
    public boolean overlaps(Box o) {
        return minX <= o.maxX && o.minX <= maxX && minY <= o.maxY && o.minY <= maxY && minZ <= o.maxZ && o.minZ <= maxZ;
    }

    /** Does this box lie inside {@code outer}, allowed to protrude by {@code marginXZ} cells on each horizontal side (never vertically)? */
    public boolean inside(Box outer, int marginXZ) {
        return minX >= outer.minX - marginXZ && maxX <= outer.maxX + marginXZ
                && minZ >= outer.minZ - marginXZ && maxZ <= outer.maxZ + marginXZ
                && minY >= outer.minY && maxY <= outer.maxY;
    }

    public boolean contains(Vec p) {
        return p.x() >= minX && p.x() <= maxX && p.y() >= minY && p.y() <= maxY && p.z() >= minZ && p.z() <= maxZ;
    }

    /** This box grown by {@code n} cells on every horizontal side (y unchanged): the "keep this far away" zone around a region. */
    public Box inflateXZ(int n) { return new Box(minX - n, minY, minZ - n, maxX + n, maxY, maxZ + n); }

    /** The smallest box holding both. */
    public Box union(Box o) {
        return new Box(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ), Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
    }

    /** The horizontal centre as doubles (x then z), for distances measured in modules. */
    public double centerX() { return (minX + maxX) / 2.0; }

    public double centerZ() { return (minZ + maxZ) / 2.0; }

    /** The farthest coordinate of the box in the direction {@code f} (for east: maxX, for west: -minX ...), so "how far out does this reach". */
    public int far(Facing f) {
        return switch (f) {
            case EAST -> maxX;
            case WEST -> -minX;
            case SOUTH -> maxZ;
            case NORTH -> -minZ;
        };
    }

    /** The nearest coordinate of the box in the direction {@code f}: the plane where it starts when walking {@code f}. */
    public int near(Facing f) {
        return switch (f) {
            case EAST -> minX;
            case WEST -> -maxX;
            case SOUTH -> minZ;
            case NORTH -> -maxZ;
        };
    }

    @Override
    public String toString() { return "[" + minX + "," + minY + "," + minZ + " .. " + maxX + "," + maxY + "," + maxZ + "]"; }
}
