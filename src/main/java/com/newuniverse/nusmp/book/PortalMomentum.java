package com.newuniverse.nusmp.book;

/**
 * Why: a gate pair must hand an entity over the way a door does (walk in at one, walk out of the other, still heading the same way
 * relative to the door), and that is plain flat geometry. Keeping it in one pure class with no Minecraft types means the rotation
 * and the exit point can be unit-tested, and {@link SpatialBook#hop} only has to apply the numbers.
 * <p>
 * Angles are radians in the XZ plane measured as {@code atan2(z, x)}. A gate's "facing" is the angle of its front normal, the
 * direction from the gate towards the caster at cast time (the side the visual shows its face to). Entering a gate through its front
 * means moving along the negative normal and must leave the other gate along its positive normal, so the velocity is turned by
 * {@code faceB - faceA - PI}. Entering from the back is the mirror case and the same turn applies.
 */
public final class PortalMomentum {
    private PortalMomentum() {}

    /** How far in front of the exit gate an entity is put, so it clears the trigger box and the 30 tick guard does the rest. */
    public static final double EXIT_OFFSET = 0.85;

    /** Facing angle of a gate placed at (gx, gz) that looks at (tx, tz); 0 when both points coincide. */
    public static double facing(double gx, double gz, double tx, double tz) {
        double dx = tx - gx, dz = tz - gz;
        if (dx * dx + dz * dz < 1.0E-8) return 0;
        return Math.atan2(dz, dx);
    }

    /** The turn (radians, in (-PI, PI]) that carries a velocity through gate A into gate B. */
    public static double turn(double faceA, double faceB) {
        return wrap(faceB - faceA - Math.PI);
    }

    /** Wraps an angle into (-PI, PI]. */
    public static double wrap(double a) {
        double r = a % (2 * Math.PI);
        if (r > Math.PI) r -= 2 * Math.PI;
        if (r <= -Math.PI) r += 2 * Math.PI;
        return r;
    }

    /** Rotates the horizontal part (vx, vz) by {@code turn}; returns {x, z}. The vertical part is the caller's and is not touched. */
    public static double[] rotate(double vx, double vz, double turn) {
        double c = Math.cos(turn), s = Math.sin(turn);
        return new double[]{vx * c - vz * s, vx * s + vz * c};
    }

    /** +1 when a point (px, pz) is on the front of gate (gx, gz) with the given facing, -1 when behind it, 0 when level with it. */
    public static int side(double gx, double gz, double face, double px, double pz) {
        double d = (px - gx) * Math.cos(face) + (pz - gz) * Math.sin(face);
        return d > 0.02 ? 1 : d < -0.02 ? -1 : 0;
    }

    /**
     * Where an entity standing at (px, pz) beside gate A comes out beside gate B: it keeps its sideways offset across the door
     * (mirrored, as in a real door pair) and is put {@link #EXIT_OFFSET} in front of B when it entered from A's front, behind B
     * when it entered from A's back. {@code sideOfA} is the result of {@link #side}; 0 counts as the front.
     *
     * @return {x, z}
     */
    public static double[] exitPoint(double ax, double az, double faceA, double bx, double bz, double faceB, double px, double pz, int sideOfA) {
        double tAx = -Math.sin(faceA), tAz = Math.cos(faceA);                       // A's tangent (the normal turned a quarter)
        double lateral = (px - ax) * tAx + (pz - az) * tAz;                         // sideways offset across A's door
        double tBx = -Math.sin(faceB), tBz = Math.cos(faceB);
        double out = sideOfA < 0 ? -EXIT_OFFSET : EXIT_OFFSET;
        return new double[]{bx + Math.cos(faceB) * out - tBx * lateral, bz + Math.sin(faceB) * out - tBz * lateral};
    }
}
