package com.newuniverse.nusmp.labyrinth.mega;

/**
 * Why: Floor 0 (the Sanctuary Hub, dimension {@code nusmp:sanctuary}) is built by THREE parties that never import each other: the 3D-noise chunk generator
 * draws the plains, the Holy Spire and the Abyssal Chasm and puts the two polarity gates at the end of their stairs; the hub package places the plaza
 * (the way home and the signposts), protects it and routes the gates; the alignment lock decides who may use which gate. They agree on WHERE things are
 * through these constants and nothing else, so a landmark can be redrawn without anyone else changing a line, and a unit test can pin that the gates sit
 * inside the generator's volumes. All numbers are block coordinates in the Sanctuary dimension, whose type is {@code min_y -64, height 384} (so the highest
 * block is y 319 and the lowest y -64); the Hub is a plains world with its surface near {@link #SURFACE_Y}. The Holy Spire rises to the EAST of the plaza,
 * the Abyssal Chasm sinks to the WEST, because "up" and "down" are the two roads of the brief and east/west keeps them on opposite sides of one square.
 * Pure data: no Minecraft import.
 */
public final class HubLayout {
    private HubLayout() {}

    /** Ground level of the plains around the plaza (the plaza is flattened to exactly this). */
    public static final int SURFACE_Y = 68;

    // ---- the plaza: the safe, buildable town square with the way home
    public static final int PLAZA_X = 0, PLAZA_Z = 0;
    /** Radius of the flattened disc; the terrain blends back to its natural height over the next 24 blocks. */
    public static final int PLAZA_RADIUS = 40;
    /** Where a player arrives in the Hub (standing cell, one above the ground): a few steps from the way home. */
    public static final int SPAWN_X = 0, SPAWN_Y = SURFACE_Y + 1, SPAWN_Z = 8;
    /** The way home (the Overworld): the centre of the gate plane, on the north side of the plaza facing south. */
    public static final int HOME_GATE_X = 0, HOME_GATE_Y = SURFACE_Y + 2, HOME_GATE_Z = -14;

    // ---- the Holy Spire (Ascension): a hollow tower of white stone rising from the plains to the east
    public static final int SPIRE_X = 176, SPIRE_Z = 0;
    /** Outer radius of the base and of the crown; the shell tapers between them. */
    public static final int SPIRE_BASE_RADIUS = 34, SPIRE_TOP_RADIUS = 20;
    /** Height of the tower above {@link #SURFACE_Y}. */
    public static final int SPIRE_HEIGHT = 150;
    /** The ground-level doorway on the plaza side (the stairs start behind it). */
    public static final int SPIRE_DOOR_X = SPIRE_X - SPIRE_BASE_RADIUS, SPIRE_DOOR_Z = SPIRE_Z;
    /** The centre of the Holy gate plane at the top of the stairs. */
    public static final int HOLY_GATE_X = SPIRE_X, HOLY_GATE_Y = SURFACE_Y + SPIRE_HEIGHT - 6, HOLY_GATE_Z = SPIRE_Z;

    // ---- the Abyssal Chasm (Descent): a pit in the plains to the west
    public static final int CHASM_X = -176, CHASM_Z = 0;
    /** Radius at the rim and at the bottom; the walls narrow between them. */
    public static final int CHASM_RIM_RADIUS = 46, CHASM_FLOOR_RADIUS = 30;
    /** Depth below {@link #SURFACE_Y}. */
    public static final int CHASM_DEPTH = 116;
    /** The ground-level rim path on the plaza side (the spiral ramp starts here). */
    public static final int CHASM_DOOR_X = CHASM_X + CHASM_RIM_RADIUS, CHASM_DOOR_Z = CHASM_Z;
    /** The centre of the Abyssal gate plane at the bottom of the descent. */
    public static final int ABYSS_GATE_X = CHASM_X, ABYSS_GATE_Y = SURFACE_Y - CHASM_DEPTH + 4, ABYSS_GATE_Z = CHASM_Z;

    /** How far from a gate's recorded centre a gate block still counts as that gate (the plane is 3 wide and 4 high, and players lean). */
    public static final int GATE_REACH = 10;

    /** Which gate a position belongs to. */
    public enum GateKind { HOME, HOLY, ABYSSAL }

    /**
     * The gate whose recorded centre is nearest to the position and within {@link #GATE_REACH} blocks (spherical), or null. This is how the gate block
     * (a plain {@code nusmp:labyrinth_gate} in the Hub) learns which road it is without a block property: the position decides.
     */
    public static GateKind gateNear(int x, int y, int z) {
        GateKind best = null;
        double bestD = (double) GATE_REACH * GATE_REACH + 1;
        double d = dist2(x, y, z, HOME_GATE_X, HOME_GATE_Y, HOME_GATE_Z);
        if (d < bestD) { bestD = d; best = GateKind.HOME; }
        d = dist2(x, y, z, HOLY_GATE_X, HOLY_GATE_Y, HOLY_GATE_Z);
        if (d < bestD) { bestD = d; best = GateKind.HOLY; }
        d = dist2(x, y, z, ABYSS_GATE_X, ABYSS_GATE_Y, ABYSS_GATE_Z);
        if (d < bestD) { best = GateKind.ABYSSAL; }
        return best;
    }

    private static double dist2(int x, int y, int z, int gx, int gy, int gz) {
        double dx = x - gx, dy = y - gy, dz = z - gz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** The polarity a gate kind leads to, or null for the way home. */
    public static Polarity polarityOf(GateKind k) { return k == GateKind.HOLY ? Polarity.HOLY : k == GateKind.ABYSSAL ? Polarity.ABYSSAL : null; }
}
