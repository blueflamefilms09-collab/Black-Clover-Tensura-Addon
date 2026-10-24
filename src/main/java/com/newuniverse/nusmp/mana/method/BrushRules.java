package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;

/**
 * The Brush Rune page (pure, tested). Canon (Black Clover Wiki, Rill Boismortier and Potrof's training): Rill, a Painting Magic user, learns the Mana Method by WRITING the runes with his paint
 * and brush instead of shaping them from mana. The page lets the caster paint a rune array on the surface they look at (floor, wall or ceiling, within {@value #REACH} blocks). The array stays
 * for a while and acts when a foe steps into it: ONE bolt leaves it toward that foe (the same bolt as the other runes: Pursue / Grow, damage through the balance law, credited to the caster),
 * and the array is spent. It never hurts the caster or an ally.
 *
 * <p>Bounded: at most {@code brushPerCaster} (default {@value #DEFAULT_PER_CASTER}, 1..{@value #MAX_PER_CASTER}) arrays per caster and {@code brushGlobal} (default {@value #DEFAULT_GLOBAL},
 * {@value #MIN_GLOBAL}..{@value #MAX_GLOBAL}) on the server; each lives {@code brushSeconds} (default {@value #DEFAULT_SECONDS}, {@value #MIN_SECONDS}..{@value #MAX_SECONDS}) x ({@value #MIN_TIME_SHARE}
 * at a tap .. 1 at a full hold); one cheap entity query per array every {@value #CHECK_EVERY} ticks, all arrays together at most {@value #QUERIES_PER_TICK} queries per tick. The charge scales the size
 * of the array (the trigger radius {@value #RADIUS_MIN}..{@value #RADIUS_MAX} blocks), its life and the power of the bolt.
 */
public final class BrushRules {
    private BrushRules() {}

    // ---------------------------------------------------------------- the page
    public static final double COST_PERCENT = 9.0, COST_FLOOR = 180.0;
    public static final int COOLDOWN = 240;
    public static final double REACH = 5.0;
    /** A new array must be at least this far from another array of the same caster. */
    public static final double MIN_SPACING = 1.5;
    public static final int DEFAULT_PER_CASTER = 3, MAX_PER_CASTER = 5, DEFAULT_GLOBAL = 24, MIN_GLOBAL = 4, MAX_GLOBAL = 64;
    public static final int DEFAULT_SECONDS = 30, MIN_SECONDS = 5, MAX_SECONDS = 120;
    public static final double MIN_TIME_SHARE = 0.5;
    public static final double RADIUS_MIN = 1.3, RADIUS_MAX = 2.4;
    /** How long a new array only draws itself and does not act (the caster steps back; a wake-up grace), in ticks. */
    public static final int ARM_TICKS = 14;
    /** An array looks at its surroundings every this many ticks (staggered by id) and all arrays together may do {@value #QUERIES_PER_TICK} queries in one tick. */
    public static final int CHECK_EVERY = 4, QUERIES_PER_TICK = 8;
    /** The trigger reaches this far out of the surface (along its normal) and this far into it. */
    public static final double REACH_OUT = 2.0, REACH_IN = 0.3;
    /** At most this many painted runes (brush and body together) are drawn by a client at once. */
    public static final int MAX_PAINTED_DRAWN = 8;
    /** A painted rune farther from the camera than this is not even started. */
    public static final double PAINT_DISTANCE = 56.0;

    public static int clampPerCaster(int configured) { return Math.max(1, Math.min(MAX_PER_CASTER, configured)); }

    public static int clampGlobal(int configured) { return Math.max(MIN_GLOBAL, Math.min(MAX_GLOBAL, configured)); }

    public static int seconds(int configured) { return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, configured)); }

    /** May another array be painted by a caster who has {@code casterLive} of them while the server has {@code globalLive}? */
    public static boolean allowed(int casterLive, int globalLive, int perCaster, int global) {
        return casterLive < clampPerCaster(perCaster) && globalLive < clampGlobal(global);
    }

    /** The trigger radius of a charge: {@link #RADIUS_MIN} (a tap) .. {@link #RADIUS_MAX} (a full hold). */
    public static double radius(Charge c) { return RADIUS_MIN + (RADIUS_MAX - RADIUS_MIN) * RuneCost.level(c); }

    /** The life of an array in ticks: {@code brushSeconds} x 0.5 (a tap) .. 1 (a full hold), at least one second. */
    public static int lifeTicks(Charge c, int configuredSeconds) {
        double share = MIN_TIME_SHARE + (1.0 - MIN_TIME_SHARE) * RuneCost.level(c);
        return Math.max(20, (int) Math.round(seconds(configuredSeconds) * 20 * share));
    }

    /** Is the array at the world tick {@code now} still alive? */
    public static boolean alive(long now, long expires) { return now < expires; }

    /** Is the array armed (past its drawing grace) at {@code now}? */
    public static boolean armed(long now, long born) { return now - born >= ARM_TICKS; }

    /** Is it this array's turn to look around? Staggered by the array's id so the arrays do not all query in the same tick. */
    public static boolean due(long now, int id) { return Math.floorMod(now + id, CHECK_EVERY) == 0; }

    /** Is a new array too close to an older one of the same caster? */
    public static boolean tooClose(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx, dy = ay - by, dz = az - bz;
        return dx * dx + dy * dy + dz * dz < MIN_SPACING * MIN_SPACING;
    }

    // ---------------------------------------------------------------- the trigger
    /**
     * Does a creature trigger the array? {@code (cx, cy, cz)} is the array's centre, {@code (nx, ny, nz)} the unit normal of the surface it is painted on (pointing out of the surface), and
     * {@code (px, py, pz)} the point of the creature's body nearest to the centre (see {@link #nearest}). The creature must be within {@code radius} of the array in the surface's plane and
     * between {@link #REACH_IN} behind and {@link #REACH_OUT} in front of the surface. On the floor that is "standing in the array", on a wall "standing in front of it".
     */
    public static boolean triggered(double cx, double cy, double cz, double nx, double ny, double nz, double px, double py, double pz, double radius) {
        double dx = px - cx, dy = py - cy, dz = pz - cz;
        double along = dx * nx + dy * ny + dz * nz;
        if (along < -REACH_IN || along > REACH_OUT) return false;
        double planar2 = dx * dx + dy * dy + dz * dz - along * along;
        return planar2 <= radius * radius;
    }

    /** The coordinate of the interval [lo, hi] nearest to {@code v}: the nearest point of a creature's box to the array's centre, one axis at a time. */
    public static double nearest(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }

    // ---------------------------------------------------------------- the picture
    /**
     * The look vector a painted rune is drawn with: the rune's plane faces AGAINST the look ({@link RuneGeneratorUtil#faceQuaternion}), so the look is minus the surface normal. A floor or a
     * ceiling has no horizontal direction of its own, so the caster's yaw (degrees, Minecraft's: 0 faces +z) leans the look a little (0.02) to turn the glyphs upright toward the caster.
     * Writes the unit vector to {@code out}.
     */
    public static void lookFor(double nx, double ny, double nz, double casterYawDeg, double[] out) {
        double x = -nx, y = -ny, z = -nz;
        if (Math.abs(ny) > 0.9) {
            double yaw = Math.toRadians(casterYawDeg);
            x += -Math.sin(yaw) * 0.02 * Math.signum(ny);
            z += Math.cos(yaw) * 0.02 * Math.signum(ny);
        }
        double len = Math.sqrt(x * x + y * y + z * z);
        if (!(len > 1e-9)) { out[0] = 0; out[1] = -1; out[2] = 0; return; }
        out[0] = x / len;
        out[1] = y / len;
        out[2] = z / len;
    }

    /** The centre of a painted rune: the point hit on the surface, lifted {@code lift} along the normal so it does not sink into the block. */
    public static double lifted(double hit, double normalComponent, double lift) { return hit + normalComponent * lift; }

    /** The scale to give the picture so the written rune spans about the trigger radius: its own radius ({@link RuneGeneratorUtil#paramsFor}) is 1.15 to 1.8 blocks. */
    public static float pictureScale(double triggerRadius, double ownRadius) {
        if (!(ownRadius > 0.05)) return 1f;
        return (float) Math.max(0.3, Math.min(3.0, triggerRadius * 0.95 / ownRadius));
    }

    /** The unit normals of the six faces in the order of the packet (Minecraft's 3D data value: down, up, north, south, west, east). Writes the normal of face {@code face} (0..5; anything else is up). */
    public static void normal(int face, double[] out) {
        switch (face) {
            case 0 -> { out[0] = 0; out[1] = -1; out[2] = 0; }
            case 2 -> { out[0] = 0; out[1] = 0; out[2] = -1; }
            case 3 -> { out[0] = 0; out[1] = 0; out[2] = 1; }
            case 4 -> { out[0] = -1; out[1] = 0; out[2] = 0; }
            case 5 -> { out[0] = 1; out[1] = 0; out[2] = 0; }
            default -> { out[0] = 0; out[1] = 1; out[2] = 0; }
        }
    }

    /** One byte for the packet: a length in tenths of a block, 0..255. */
    public static int tenths(double v) { return (int) Math.max(0, Math.min(255, Math.round(v * 10.0))); }

    /** The yaw as one byte (0..255 for 0..360 degrees) and back. */
    public static int yawByte(double yawDeg) { return Math.floorMod((int) Math.round(yawDeg * 256.0 / 360.0), 256); }

    public static double yawDegrees(int b) { return (b & 0xFF) * 360.0 / 256.0; }
}
