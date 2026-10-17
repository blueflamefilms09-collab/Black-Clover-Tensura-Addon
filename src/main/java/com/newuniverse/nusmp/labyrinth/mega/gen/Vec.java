package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: a cell position for the pure assembler. {@code net.minecraft.core.BlockPos} cannot be used here (the assembler must compile and run with no
 * game on the class path), and a plain record gives value equality and a stable hash for free, which the spatial index and the loop detector
 * rely on. All maths on it is integer-only, so a plan is bit-for-bit the same on every machine.
 */
public record Vec(int x, int y, int z) {
    public static final Vec ZERO = new Vec(0, 0, 0);

    public Vec add(Vec o) { return new Vec(x + o.x, y + o.y, z + o.z); }

    public Vec add(int dx, int dy, int dz) { return new Vec(x + dx, y + dy, z + dz); }

    public Vec sub(Vec o) { return new Vec(x - o.x, y - o.y, z - o.z); }

    /** The cell {@code n} steps away in a horizontal direction. */
    public Vec step(Facing f, int n) { return new Vec(x + f.dx * n, y, z + f.dz * n); }

    /** The cell one step away in a horizontal direction. */
    public Vec step(Facing f) { return step(f, 1); }

    /** This offset turned by {@code quarterTurns} clockwise quarter turns about the origin, the way the game turns a template (see {@link Rot#cell}). */
    public Vec rotate(int quarterTurns) { return Rot.cell(quarterTurns, this); }

    /** The coordinate along a horizontal direction: how far "forward" this cell is when walking {@code f}. */
    public int along(Facing f) { return x * f.dx + z * f.dz; }

    @Override
    public String toString() { return "(" + x + "," + y + "," + z + ")"; }
}
