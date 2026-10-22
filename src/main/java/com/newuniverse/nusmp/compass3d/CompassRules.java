package com.newuniverse.nusmp.compass3d;

/**
 * Why: the 3D Compass Magic (docs/magic/compass_3d.md) puts one floating compass beside the caster and makes every spell come out of it as a 3D model. Every decision that does not need the
 * game lives here so it can be tested: the prop word the server and the painter share, where the compass floats, how long it stays and fades, the caps per player, where the needle points, the
 * marching dashes of a route, the flight of a seal disc. Pure: no Minecraft class, no clock, no static state.
 * <p>
 * Prop words (the int {@code param} of a {@code MagicPropEntity}):
 * <ul>
 *   <li>COMPASS_1 (the caster's compass): bits 0-7 variant (0), bits 8-15 aim yaw (0..255 = 0..360 degrees), bits 16-22 aim pitch (0..127 = -90..90), bits 23-24 mode ({@link #MODE_IDLE} ..
 *       {@link #MODE_CHART}), bits 25-30 fade ticks left + 1 (0 = not fading).</li>
 *   <li>COMPASS_2 (what comes out of it): bits 0-7 variant ({@link #V_NEEDLE}, {@link #V_SEAL}, {@link #V_TURN}, {@link #V_ROUTE}), bits 8-15 index.</li>
 * </ul>
 */
public final class CompassRules {
    private CompassRules() {}

    public static final int V_NEEDLE = 0, V_SEAL = 1, V_TURN = 2, V_ROUTE = 3;
    public static final int MODE_IDLE = 0, MODE_ATTACK = 1, MODE_GUARD = 2, MODE_CHART = 3;

    /** Ticks the compass stays after the last cast or hold (8 seconds), ticks of its fade out, and of its pop in (client). */
    public static final int LINGER = 160, FADE = 14, FADE_IN = 10;
    /** Caps per player: living arrow-needles, seal discs, turn discs and routes. The totals can never pile up however fast the spells are cast. */
    public static final int CAP_NEEDLES = 24, CAP_SEALS = 10, CAP_TURNS = 6, CAP_ROUTES = 2;
    /** Radius of the compass in blocks at prop scale 1 (the mesh has radius 1): a disc a little over one block and a half across. */
    public static final float MAIN_SCALE = 0.72f;
    /** How far a seal disc flies from the compass to its foe's feet, in ticks. */
    public static final int SEAL_FLIGHT = 10;
    /** Where the compass floats: to the caster's right, up and a little behind, and how far it turns outward from the caster's facing, in degrees. */
    public static final double SIDE = 1.25, UP = 1.55, BACK = 0.35, OUT_YAW = 25.0;

    // ---------------------------------------------------------------- the prop word of the compass
    public static int packMain(float aimYawDeg, float aimPitchDeg, int mode, int fadeLeft) {
        int yaw = Math.floorMod(Math.round(aimYawDeg / 360f * 256f), 256);
        int pitch = Math.max(0, Math.min(127, Math.round((aimPitchDeg + 90f) / 180f * 127f)));
        int fade = fadeLeft <= 0 ? 0 : Math.min(63, fadeLeft + 1);
        return (yaw << 8) | (pitch << 16) | ((mode & 3) << 23) | (fade << 25);
    }

    public static int variant(int word) { return word & 0xFF; }
    public static int index(int word) { return (word >>> 8) & 0xFF; }
    public static float aimYaw(int word) { return ((word >>> 8) & 0xFF) / 256f * 360f; }
    public static float aimPitch(int word) { return ((word >>> 16) & 0x7F) / 127f * 180f - 90f; }
    public static int mode(int word) { return (word >>> 23) & 3; }
    /** Ticks of fade left (0 = the compass is not fading). */
    public static int fadeLeft(int word) { int f = (word >>> 25) & 63; return f == 0 ? 0 : f - 1; }

    public static int packProp(int variant, int index) { return (variant & 0xFF) | ((index & 0xFF) << 8); }

    // ---------------------------------------------------------------- where it floats
    /** The compass centre for a caster at (x, y, z) facing 'yawDeg' (Minecraft yaw: 0 faces +z): {x, y, z}. */
    public static double[] mainPosition(double x, double y, double z, float yawDeg) {
        double a = Math.toRadians(yawDeg), fx = -Math.sin(a), fz = Math.cos(a), rx = -Math.cos(a), rz = -Math.sin(a);
        return new double[]{x + rx * SIDE - fx * BACK, y + UP, z + rz * SIDE - fz * BACK};
    }

    /** The yaw the compass faces: the caster's, turned outward. */
    public static float mainYaw(float ownerYawDeg) { return ownerYawDeg + (float) OUT_YAW; }

    /** Moves 'cur' toward 'target' by the fraction k (0..1) along the short way round, in degrees. */
    public static float approachDegrees(float cur, float target, float k) {
        float d = ((target - cur) % 360f + 540f) % 360f - 180f;
        return cur + d * Math.max(0f, Math.min(1f, k));
    }

    // ---------------------------------------------------------------- the needle
    /**
     * The angle (degrees, rotation about the model's z axis) that turns the needle (it points +y at 0) toward the world direction (dx, dy, dz), for a compass whose entity yaw is yawDeg.
     * The model's x axis is (cos yaw, 0, sin yaw) in the world, its y axis is up, its z (the front) is (-sin yaw, 0, cos yaw). A direction straight ahead of the face (no x, no y) gives 0.
     */
    public static float needleAngle(double dx, double dy, double dz, float yawDeg) {
        double a = Math.toRadians(yawDeg);
        double lx = dx * Math.cos(a) + dz * Math.sin(a);
        if (Math.abs(lx) < 1e-9 && Math.abs(dy) < 1e-9) return 0f;
        return (float) Math.toDegrees(Math.atan2(-lx, dy));
    }

    /** A damped swing of the needle toward its goal: 'age' ticks since it was told, 'k' = how quickly it settles; an overshoot that rings down. */
    public static float needleSwing(float fromDeg, float toDeg, float age) {
        float d = ((toDeg - fromDeg) % 360f + 540f) % 360f - 180f;
        float settle = (float) (1.0 - Math.exp(-age * 0.22) * Math.cos(age * 0.55));
        return fromDeg + d * settle;
    }

    // ---------------------------------------------------------------- scale and fades (client and tests)
    /** Pop in: 0 -> 1 with a little overshoot over 'in' ticks. */
    public static float popIn(float ageTicks, float in) {
        float t = Math.max(0f, Math.min(1f, ageTicks / Math.max(1f, in)));
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    /** Opacity 0..1 of the compass: it fades in over FADE_IN ticks and out over the fade ticks the server counts down (partial tick included). */
    public static float opacity(float ageTicks, int word, float partial) {
        float in = Math.max(0f, Math.min(1f, ageTicks / FADE_IN));
        int left = fadeLeft(word);
        float out = left <= 0 ? 1f : Math.max(0f, Math.min(1f, (left - partial) / FADE));
        return in * out;
    }

    // ---------------------------------------------------------------- the route (marching dashes)
    /** Number of dashes along a route of this length (about one per 1.3 blocks, 2..14). */
    public static int dashes(double length) { return Math.max(2, Math.min(14, (int) Math.round(length / 1.3))); }

    /** The start and end of dash k as fractions of the route (0..1), marching toward the target as 'ticks' grows: {start, end}; a dash wraps round to the start. */
    public static float[] dash(int k, int n, float ticks) {
        float slot = 1f / n, phase = (ticks * 0.035f) % 1f;
        float s = ((k * slot + phase * slot) % 1f);
        float e = s + slot * 0.55f;
        return new float[]{s, Math.min(1f, e)};
    }

    // ---------------------------------------------------------------- the flight of a seal disc
    /** Position along a straight flight with an ease: from 'a' to 'b' at tick t of 'flight' (1 once it has landed). */
    public static double ease(double t, double flight) {
        double x = Math.max(0, Math.min(1, t / Math.max(1e-6, flight)));
        return 1 - Math.pow(1 - x, 3);
    }

    // ---------------------------------------------------------------- caps
    /** How many of a kind may still be spawned: never negative. */
    public static int room(int live, int cap) { return Math.max(0, cap - live); }

    /** True when the prop word (COMPASS_2) is a variant this code knows. */
    public static boolean knownVariant(int word) { int v = variant(word); return v >= V_NEEDLE && v <= V_ROUTE; }
}
