package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: this is the single place where "a piece turned by r quarter turns" becomes numbers, and it must be IDENTICAL to what the game does when it
 * places the same piece, because the assembler plans in pure Java and the game then places the plan with {@code PoolElementStructurePiece}. The
 * game's rule ({@code StructureTemplate.transform} with the default pivot at the origin) is: a template cell (x, z) turned by
 * {@code CLOCKWISE_90} lands on (-z, x), by {@code CLOCKWISE_180} on (-x, -z), by {@code COUNTERCLOCKWISE_90} on (z, -x); y never changes; the piece
 * is then moved by its origin. {@code r} counts clockwise quarter turns, so r = 1 is CLOCKWISE_90, 2 is CLOCKWISE_180 and 3 is COUNTERCLOCKWISE_90
 * (the order of the vanilla {@code Rotation} enum). A piece's bounding box is the box through the turned corners (0,0,0) and (sx-1, sy-1, sz-1)
 * moved by the origin, which is exactly what {@code StructurePoolElement.getBoundingBox} returns. {@code RotationTest} pins all of this with
 * hand-computed cases, and {@code /multiverse mega verify} compares it with the game on a generated sector.
 */
public final class Rot {
    private Rot() {}

    /** The offset of template cell {@code c} after {@code r} clockwise quarter turns about the template origin (r is reduced modulo 4). */
    public static Vec cell(int r, Vec c) {
        switch (r & 3) {
            case 1: return new Vec(-c.z(), c.y(), c.x());
            case 2: return new Vec(-c.x(), c.y(), -c.z());
            case 3: return new Vec(c.z(), c.y(), -c.x());
            default: return c;
        }
    }

    /** The world cell of template cell {@code c} for a piece placed at {@code origin} turned {@code r} quarter turns. */
    public static Vec world(Vec origin, int r, Vec c) { return origin.add(cell(r, c)); }

    /** The direction {@code f} (a facing in the template) after the piece is turned {@code r} quarter turns. */
    public static Facing facing(int r, Facing f) { return f.turn(r); }

    /** How many quarter turns bring a template facing {@code from} to the world facing {@code to}. */
    public static int turnsFor(Facing from, Facing to) { return from.turnsTo(to); }

    /** The size (x, z swapped for odd turns) of a template of {@code sx} by {@code sz} after {@code r} quarter turns: {@code {x, z}}. */
    public static int[] size(int r, int sx, int sz) { return (r & 1) == 0 ? new int[] {sx, sz} : new int[] {sz, sx}; }

    /** The bounding box of a template of the given size placed at {@code origin} and turned {@code r} quarter turns (inclusive cell corners). */
    public static Box box(Vec origin, int r, int sx, int sy, int sz) {
        Vec far = cell(r, new Vec(sx - 1, sy - 1, sz - 1));
        return new Box(origin.x() + Math.min(0, far.x()), origin.y(), origin.z() + Math.min(0, far.z()),
                origin.x() + Math.max(0, far.x()), origin.y() + sy - 1, origin.z() + Math.max(0, far.z()));
    }
}
