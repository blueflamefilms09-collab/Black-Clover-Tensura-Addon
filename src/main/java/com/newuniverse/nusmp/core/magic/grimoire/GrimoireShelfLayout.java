package com.newuniverse.nusmp.core.magic.grimoire;

/**
 * Where each book of the floating grimoire shelf sits, as pure maths (no Minecraft imports, unit-tested). Everything is computed
 * fresh from the shelf's age every frame: there is no keyframed or model animation, no entity and nothing to sync.
 *
 * <p>Coordinates are in the shelf's own frame, measured from the opener's eyes at the moment it opened: {@code right}, {@code up}
 * and {@code forward} in blocks. {@link #worldDX} / {@link #worldDZ} turn that into a world offset for the opener's yaw.
 */
public final class GrimoireShelfLayout {
    /** Bob height: 0.04 blocks up and down (about 2.5 cm). */
    public static final float BOB_AMPLITUDE = 0.04f;
    /** Bob speed in radians per tick: one full bob takes 2*PI / 0.12 = about 52 ticks (2.6 s). */
    public static final float BOB_SPEED = 0.12f;
    /** Phase shift per book (radians), so the books bob out of step instead of as one block. */
    public static final float BOB_PHASE_PER_BOOK = 7f;
    /** Books rise from this height (below their rest spot) while opening. */
    public static final float RISE_FROM = -0.6f;
    /** Ticks one book takes to rise into place. */
    public static final float OPEN_TICKS = 10f;
    /** Ticks between one book starting to rise and the next. */
    public static final float STAGGER_TICKS = 2f;

    /** Books per row; more books start a new row underneath. */
    public static final int PER_ROW = 7;
    /** Distance from the eyes to the arc of books, in blocks. */
    public static final float RADIUS = 1.8f;
    /** Angle between neighbouring books on the arc, in radians (18 degrees). */
    public static final float STEP = (float) Math.toRadians(18);
    /** Height between rows, in blocks. */
    public static final float ROW_GAP = 0.5f;

    /** A book's resting spot on the shelf. */
    public record Rest(float right, float up, float forward) {}

    /** A book's spot at a given age: its rest spot after easing in, plus the bob. {@code ease} is 0 (not started) to 1 (arrived). */
    public record Pose(float right, float up, float forward, float ease, float bob) {}

    private GrimoireShelfLayout() {}

    /** Resting spot of book {@code i} of {@code n}: an arc in front of the opener, rows of {@link #PER_ROW}, centred on eye height. */
    public static Rest rest(int i, int n) {
        int rows = Math.max(1, (n + PER_ROW - 1) / PER_ROW);
        int row = i / PER_ROW;
        int inRow = row < rows - 1 ? PER_ROW : n - row * PER_ROW;
        float angle = (i % PER_ROW - (inRow - 1) * 0.5f) * STEP;
        float up = ((rows - 1) * 0.5f - row) * ROW_GAP;
        return new Rest((float) Math.sin(angle) * RADIUS, up, (float) Math.cos(angle) * RADIUS);
    }

    /** Opening ease of book {@code i} at {@code age} ticks: 0 until its turn, then ease-out cubic to 1 over {@link #OPEN_TICKS}. */
    public static float ease(int i, float age) {
        float t = (age - i * STAGGER_TICKS) / OPEN_TICKS;
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Bob offset of book {@code i}: a sine wave, faded in by the opening ease {@code e} so a book only bobs once it has arrived. */
    public static float bob(int i, float age, float e) {
        return BOB_AMPLITUDE * (float) Math.sin(age * BOB_SPEED + i * BOB_PHASE_PER_BOOK) * e;
    }

    /** Pose of book {@code i} of {@code n} at {@code age} ticks since the shelf opened (game time minus open time, plus partial tick). */
    public static Pose open(int i, int n, float age) {
        Rest r = rest(i, n);
        float e = ease(i, age);
        float bob = bob(i, age, e);
        float up = RISE_FROM + (r.up() + -RISE_FROM) * e + bob;
        return new Pose(r.right(), up, r.forward(), e, bob);
    }

    /** World X offset of a shelf-frame point for an opener with Minecraft yaw {@code yawDeg} (0 = facing south, +Z). */
    public static double worldDX(float right, float forward, float yawDeg) {
        double yaw = Math.toRadians(yawDeg);
        return -Math.sin(yaw) * forward - Math.cos(yaw) * right;
    }

    /** World Z offset of a shelf-frame point for an opener with Minecraft yaw {@code yawDeg}. */
    public static double worldDZ(float right, float forward, float yawDeg) {
        double yaw = Math.toRadians(yawDeg);
        return Math.cos(yaw) * forward - Math.sin(yaw) * right;
    }

    /** Y rotation (radians) that turns a book's front (+Z) towards a viewer {@code (dx, dz)} away from it: {@code atan2(dx, dz)}. */
    public static float faceYaw(double dx, double dz) {
        return (float) Math.atan2(dx, dz);
    }

    /**
     * Which book the viewer is looking at: the one whose centre lies closest to the look ray, within {@code radius} blocks of it and
     * in front of the eyes. {@code centers} holds x, y, z per book; {@code look} must be normalised. Returns -1 for none.
     */
    public static int pick(double eyeX, double eyeY, double eyeZ, double lookX, double lookY, double lookZ, double[] centers, double radius) {
        int best = -1;
        double bestDist = radius * radius;
        for (int i = 0; i * 3 + 2 < centers.length; i++) {
            double vx = centers[i * 3] - eyeX, vy = centers[i * 3 + 1] - eyeY, vz = centers[i * 3 + 2] - eyeZ;
            double t = vx * lookX + vy * lookY + vz * lookZ;
            if (t <= 0) continue;
            double d2 = vx * vx + vy * vy + vz * vz - t * t;
            if (d2 < bestDist) { bestDist = d2; best = i; }
        }
        return best;
    }
}
