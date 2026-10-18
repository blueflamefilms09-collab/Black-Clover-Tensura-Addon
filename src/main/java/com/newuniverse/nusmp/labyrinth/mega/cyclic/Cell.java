package com.newuniverse.nusmp.labyrinth.mega.cyclic;

/**
 * Why: every piece of the Mega-Dungeon stands on the 9-block module grid and every doorway sits at the centre of a module along a wall
 * ({@code docs/labyrinth/mega.md} section 4), so the question "do these two doorways line up" is a question about integer module cells, not about
 * blocks. A cell is one module (x east, z south) counted from any fixed origin of the plan; a record gives value equality and a stable hash for the
 * occupancy maps. All arithmetic is integer, so the planner gives the same answer on every machine.
 */
public record Cell(int x, int z) {
    public Cell step(Dir d) { return new Cell(x + d.dx, z + d.dz); }

    public Cell step(Dir d, int n) { return new Cell(x + d.dx * n, z + d.dz * n); }

    /** Manhattan distance in modules. */
    public int manhattan(Cell o) { return Math.abs(x - o.x) + Math.abs(z - o.z); }

    @Override
    public String toString() { return "[" + x + "," + z + "]"; }
}
