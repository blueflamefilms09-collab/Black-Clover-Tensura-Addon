package com.newuniverse.nusmp.core.magic.grimoire;

/**
 * Where a carried grimoire is, as pure maths (no Minecraft imports, unit-tested): dormant at the right hip, summoned in front of
 * the right hand, and the smooth trip between the two. Also the page-flip curve played when the owner switches spells.
 *
 * <p>Positions are in the owner's body frame, in blocks from their feet: {@code right}, {@code up}, {@code forward}. Rotations are
 * degrees relative to the body yaw (0 = cover facing forward, -90 = cover facing out to the right).
 */
public final class GrimoireCarry {
    /** Ticks for the hip -> hand (summon) and hand -> hip (stow) trip. */
    public static final float TRAVEL_TICKS = 12f;

    /** One placement of the book. */
    public record Pose(float right, float up, float forward, float yaw, float pitch, float roll, float scale) {}

    /** Dormant: hanging against the right hip / thigh, cover out to the side, slightly tilted, smaller. */
    public static final Pose HIP = new Pose(0.36f, 0.72f, -0.02f, -90f, 0f, -12f, 0.38f);
    /** Dormant while sneaking (the hips drop). */
    public static final Pose HIP_SNEAK = new Pose(0.36f, 0.56f, -0.12f, -90f, 0f, -12f, 0.38f);
    /** Summoned: in front of the right hand, cover facing out, turned a little inwards. */
    public static final Pose HAND = new Pose(0.45f, 1.05f, 0.55f, 15f, -10f, 0f, 0.55f);
    /** Summoned while sneaking. */
    public static final Pose HAND_SNEAK = new Pose(0.45f, 0.85f, 0.55f, 15f, -10f, 0f, 0.55f);

    private GrimoireCarry() {}

    /** Ease-in-out cubic over {@link #TRAVEL_TICKS}: 0 at the start of the trip, 1 on arrival. */
    public static float travel(float age) {
        float t = age / TRAVEL_TICKS;
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    /** Linear blend of two poses ({@code t} 0 = a, 1 = b). A little lift in the middle so the book arcs up out of the hip. */
    public static Pose blend(Pose a, Pose b, float t) {
        float arc = 0.18f * (float) Math.sin(Math.PI * t);
        return new Pose(lerp(a.right, b.right, t), lerp(a.up, b.up, t) + arc, lerp(a.forward, b.forward, t),
                lerp(a.yaw, b.yaw, t), lerp(a.pitch, b.pitch, t), lerp(a.roll, b.roll, t), lerp(a.scale, b.scale, t));
    }

    /** The book's pose {@code age} ticks after a summon ({@code toHand}) or a stow (not {@code toHand}). */
    public static Pose trip(boolean toHand, float age, boolean sneaking) {
        Pose hip = sneaking ? HIP_SNEAK : HIP, hand = sneaking ? HAND_SNEAK : HAND;
        float t = travel(age);
        return toHand ? blend(hip, hand, t) : blend(hand, hip, t);
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    // ---------------------------------------------------------------- page flip
    /** Pages turned per spell switch. */
    public static final int FLIP_PAGES = 3;
    /** Ticks one page takes to turn. */
    public static final float FLIP_TICKS = 6f;
    /** Ticks between one page starting and the next. */
    public static final float FLIP_STAGGER = 2.2f;
    /** Furthest a page swings, in degrees, before it fades out. */
    public static final float FLIP_MAX_DEG = 170f;

    /** How long a whole flip lasts. */
    public static float flipDuration() { return (FLIP_PAGES - 1) * FLIP_STAGGER + FLIP_TICKS; }

    /** Progress 0..1 of page {@code k} at {@code age} ticks into the flip (0 before it starts, 1 once turned). */
    public static float pageProgress(int k, float age) {
        float t = (age - k * FLIP_STAGGER) / FLIP_TICKS;
        return t <= 0f ? 0f : t >= 1f ? 1f : t;
    }

    /** Hinge angle of page {@code k} in degrees: a quick ease-out swing from 0 (lying on the cover) to {@link #FLIP_MAX_DEG}. */
    public static float pageAngle(int k, float age) {
        float t = pageProgress(k, age);
        float u = 1f - t;
        return FLIP_MAX_DEG * (1f - u * u);
    }

    /** Extra bend of the page's outer half, in degrees: the free edge lags behind mid-turn, then catches up. */
    public static float pageBend(int k, float age) {
        return -28f * (float) Math.sin(Math.PI * pageProgress(k, age));
    }

    /** Opacity of page {@code k}: fades in as it lifts and out as it lands, so the flip never pops. */
    public static float pageAlpha(int k, float age) {
        float t = pageProgress(k, age);
        if (t <= 0f || t >= 1f) return 0f;
        return Math.min(1f, Math.min(t / 0.15f, (1f - t) / 0.25f));
    }
}
