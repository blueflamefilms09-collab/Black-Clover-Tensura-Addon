package com.newuniverse.nusmp.shishou;

/**
 * Why: the geometry of the original abilities' effects (Akashic Index, Blades of the Record, Akashic Sanctuary, Telekinetic Grasp) in one pure place (no game classes): a rune circle that turns, a column of
 * falling blades, a lift spiral. The glue ({@link ShishouAbilities}) turns these points into particles through the perf budget; the caps here ARE the particle budget of an ability, tested. Nothing allocates:
 * a point is read by index.
 */
public final class ShishouVfx {
    private ShishouVfx() {}

    /** Most points of one ring, and the most particles one step of any effect may ask for. */
    public static final int RING_MAX = 40, STEP_MAX = 60;

    public static int ringPoints(double radius) { return Math.max(8, Math.min(RING_MAX, (int) Math.round(radius * 6.0))); }

    /** The x offset of point {@code i} of {@code n} on a ring of {@code radius}, turned by {@code phase} radians. */
    public static double ringX(int i, int n, double radius, double phase) { return Math.cos(phase + 2.0 * Math.PI * i / Math.max(1, n)) * radius; }

    public static double ringZ(int i, int n, double radius, double phase) { return Math.sin(phase + 2.0 * Math.PI * i / Math.max(1, n)) * radius; }

    /** The turn of a rune circle at game tick {@code t}: the outer ring one way, the inner the other (the two rings of the record). */
    public static double phase(long t, boolean inner) { return (inner ? -1.0 : 1.0) * t * 0.11; }

    /** A column of {@code steps} points from {@code top} down to the ground, point {@code i} of {@code steps}: the height above the ground. */
    public static double columnY(int i, int steps, double top) { return top * (1.0 - i / (double) Math.max(1, steps)); }

    /** A spiral lifting a creature: the angle and the height of point {@code i} of {@code n} for a rise of {@code rise} blocks and two turns. */
    public static double spiralAngle(int i, int n) { return 4.0 * Math.PI * i / Math.max(1, n); }

    public static double spiralY(int i, int n, double rise) { return rise * i / Math.max(1, n); }

    /** The particles one wave of the blades asks for: a ring plus a column, capped at {@link #STEP_MAX}. */
    public static int bladeWaveParticles(double radius) { return Math.min(STEP_MAX, ringPoints(radius) + 8); }

    /** The particles one pulse of the Sanctuary asks for: two rings of the field, capped at {@link #STEP_MAX}. */
    public static int sanctuaryPulseParticles(double radius) { return Math.min(STEP_MAX, 2 * ringPoints(radius) / 2 + 6); }
}
