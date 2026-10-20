package com.newuniverse.nusmp.client.relics.djinn.tiny;

/**
 * The pure numbers of the Tiny Djinn (the Lesser Summon's figure, {@link TinyDjinnLayer}): where it hovers, how it follows, how far it is drawn, how many are
 * drawn, how its dash goes. No Minecraft types, so TinyDjinnMathTest checks every rule. Lengths are blocks, angles degrees (Minecraft's yaw: 0 faces south, +z),
 * times are game ticks (20 a second) unless a name says seconds.
 */
public final class TinyDjinnMath {
    private TinyDjinnMath() {}

    // ---------------------------------------------------------------- size
    /** The model is 33 px tall (the tail tip at -18 to the head top at +15): the figure is drawn 0.45 block tall, the chibi of the brief (0.35 to 0.5). */
    public static final float MODEL_HEIGHT_PX = 33f;
    public static final float HEIGHT_BLOCKS = 0.45f;
    /** In first person the figure is drawn smaller so it never blocks the view. */
    public static final float FIRST_PERSON_SCALE = 0.78f;

    /** The scale applied to the model (whose unit is a sixteenth of a block, which {@code GeoDraw} already applies). */
    public static float modelScale(boolean firstPerson) {
        float k = HEIGHT_BLOCKS / (MODEL_HEIGHT_PX / 16f);
        return firstPerson ? k * FIRST_PERSON_SCALE : k;
    }

    // ---------------------------------------------------------------- level of detail
    public static final double FAR = 48.0, FAR2 = FAR * FAR;
    public static final double GLOW = 24.0, GLOW2 = GLOW * GLOW;
    public static final double NAME = 16.0, NAME2 = NAME * NAME;
    public static final int DEFAULT_MAX = 16, MAX_LIMIT = 64;

    /** Drawn at all: not beyond 48 blocks from the camera. */
    public static boolean inRange(double dist2) { return dist2 <= FAR2; }

    /** The additive glow pass: dropped beyond 24 blocks. */
    public static boolean withGlow(double dist2) { return dist2 <= GLOW2; }

    /** The floating name: only within 16 blocks. */
    public static boolean withName(double dist2) { return dist2 <= NAME2; }

    /** The configured cap, clamped to 0..64 (0 draws none). */
    public static int clampMax(int configured) { return Math.max(0, Math.min(MAX_LIMIT, configured)); }

    /**
     * Keeps the {@code max} nearest of {@code n} figures: {@code keep[i]} is true for the kept ones (ties go to the lower index). Returns how many are kept. No
     * allocation (the arrays belong to the caller); O(n squared) over a handful of figures.
     */
    public static int cap(double[] dist2, int n, int max, boolean[] keep) {
        int kept = 0;
        for (int i = 0; i < n; i++) {
            int rank = 0;
            for (int j = 0; j < n; j++) if (dist2[j] < dist2[i] || (dist2[j] == dist2[i] && j < i)) rank++;
            boolean k = rank < max;
            keep[i] = k;
            if (k) kept++;
        }
        return kept;
    }

    // ---------------------------------------------------------------- where it hovers
    /**
     * The shoulder anchor of a figure above its owner, written to {@code out[0..2]} (absolute coordinates): to the owner's right, a little behind, at shoulder height
     * and above; a sneaking owner lowers it and draws it in. First person: in front of the owner, low and out to the right, so the little one sits in the lower right of the view, clear of the crosshair.
     */
    public static void anchor(float[] out, double ox, double oy, double oz, float yawDeg, float bbHeight, boolean sneaking, boolean firstPerson) {
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);              // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);             // the owner's right
        double right, fwd, up;
        if (firstPerson) {
            right = 0.62;                                           // inside the view's right edge (a camera at the eye sees it about 33 degrees out and 25 down)
            fwd = 0.95;
            up = bbHeight * 0.665 - (sneaking ? 0.2 : 0.0);
        } else {
            right = 0.50;
            fwd = -0.30 + (sneaking ? 0.14 : 0.0);
            up = bbHeight * 0.97 + 0.10 - (sneaking ? 0.22 : 0.0);
        }
        out[0] = (float) (ox + rx * right + fx * fwd);
        out[1] = (float) (oy + up);
        out[2] = (float) (oz + rz * right + fz * fwd);
    }

    /** The slow float of an idle figure: up and down by 4 cm, a second out of step for every figure ({@code phase} 0..1). */
    public static float bob(float now, float phase) {
        return 0.045f * (float) Math.sin(now * 0.13 + phase * 6.2831853);
    }

    /**
     * One step of a spring-damper that pulls {@code pos} to {@code target} (written back to {@code pos} and {@code vel}). {@code omega} is the natural frequency
     * (radians a second), {@code zeta} the damping (1 = critical). {@code dtSeconds} is split into steps of at most 1/90 s and each step is implicit, so a long frame cannot blow it up; a gap
     * over half a second snaps it (the figure was not drawn, it did not fly across the world).
     */
    public static void spring(float[] pos, float[] vel, float[] target, float omega, float zeta, float dtSeconds) {
        if (dtSeconds <= 0f) return;
        if (dtSeconds > 0.5f) {
            for (int k = 0; k < 3; k++) { pos[k] = target[k]; vel[k] = 0f; }
            return;
        }
        int steps = Math.min(12, Math.max(1, (int) Math.ceil(dtSeconds * 90f)));
        float h = dtSeconds / steps;
        for (int s = 0; s < steps; s++) {
            // implicit Euler: stable however large omega * h gets, and exact enough at 1/90 s
            float denom = 1f + 2f * zeta * omega * h + omega * omega * h * h;
            for (int k = 0; k < 3; k++) {
                float d = pos[k] - target[k];
                vel[k] = (vel[k] - h * omega * omega * d) / denom;
                pos[k] += vel[k] * h;
            }
        }
    }

    // ---------------------------------------------------------------- facing
    public static float wrap180(float deg) {
        float d = deg % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    /** The yaw that faces the direction (dx, dz): Minecraft's convention (0 faces south). */
    public static float yawTo(double dx, double dz) { return (float) Math.toDegrees(Math.atan2(-dx, dz)); }

    /** Turns {@code cur} towards {@code target} by at most {@code maxStep} degrees, the short way round. */
    public static float turnToward(float cur, float target, float maxStep) {
        float d = wrap180(target - cur);
        if (Math.abs(d) <= maxStep) return target;
        return cur + Math.copySign(maxStep, d);
    }

    // ---------------------------------------------------------------- the dash
    /** The attack dash is {@code ATTACK_TICKS} long: out for 4, a beat at the foe for 2, back for 6 (the clip "attack" is the same 0.6 s). */
    public static final float DASH_OUT = 4f, DASH_HOLD = 2f, DASH_BACK = 6f, DASH_TOTAL = DASH_OUT + DASH_HOLD + DASH_BACK;

    public static float smooth(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return c * c * (3f - 2f * c);
    }

    /** How far along the way to the foe the figure is, 0 at the shoulder and 1 at the foe, after {@code ticks} of the dash. */
    public static float dash(float ticks) {
        if (ticks <= 0f || ticks >= DASH_TOTAL) return 0f;
        if (ticks < DASH_OUT) return smooth(ticks / DASH_OUT);
        if (ticks < DASH_OUT + DASH_HOLD) return 1f;
        return 1f - smooth((ticks - DASH_OUT - DASH_HOLD) / DASH_BACK);
    }

    /** A point {@code stopShort} blocks before the foe on the line from {@code from} to {@code foe} (it hovers in front of the foe, it does not sit inside it), written to {@code out}. */
    public static void strikePoint(float[] out, float[] from, float[] foe, float stopShort) {
        float dx = foe[0] - from[0], dy = foe[1] - from[1], dz = foe[2] - from[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float k = len <= 1e-4f ? 0f : Math.max(0f, len - stopShort) / len;
        out[0] = from[0] + dx * k;
        out[1] = from[1] + dy * k;
        out[2] = from[2] + dz * k;
    }

    /** The point of the spiral exit: {@code e} 0..1 on the way from {@code from} to {@code to}, turning twice round the line with a radius that closes to nothing. */
    public static void spiral(float[] out, float[] from, float[] to, float e, float radius) {
        float t = Math.max(0f, Math.min(1f, e));
        float k = t * t * (3f - 2f * t);
        float r = radius * (1f - t);
        double a = t * Math.PI * 4.0;
        out[0] = from[0] + (to[0] - from[0]) * k + (float) (Math.cos(a) * r);
        out[1] = from[1] + (to[1] - from[1]) * k + (float) (Math.sin(a * 0.5) * r * 0.5);
        out[2] = from[2] + (to[2] - from[2]) * k + (float) (Math.sin(a) * r);
    }
}
