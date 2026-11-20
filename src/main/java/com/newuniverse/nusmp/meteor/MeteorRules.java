package com.newuniverse.nusmp.meteor;

/**
 * The numbers and the maths of the shared meteor (docs/meteor_fall.md), with no game class in it so the unit tests and the dedicated server read the same code.
 * The fall is a pure function of (start, impact, fall ticks, tick): the server moves the entity with it and every client moves its copy with the same function from the
 * same synced numbers, so the entity needs no movement packets after the one that spawns it.
 */
public final class MeteorRules {
    private MeteorRules() {}

    /** A meteor falls for at least this many ticks (a faster one would be a flash, not a fall) and at most this many (the cap of the request). */
    public static final int MIN_FALL = 8, MAX_FALL = 60;
    /** How high above the impact point a meteor starts, in blocks (28 to 40 for the long falls; a short fall starts lower so it is never faster than {@link #MAX_SPEED}). */
    public static final double MIN_HEIGHT = 12.0, MAX_HEIGHT = 40.0, LONG_MIN_HEIGHT = 28.0;
    /** The sideways share of the start: the meteor comes in at about 30 degrees from the vertical. */
    public static final double SIDE_FACTOR = 0.55;
    /** The meteor's average speed never passes this many blocks a tick. */
    public static final double MAX_SPEED = 3.2;
    /** A meteor that is still there this many ticks after it should have landed is discarded (the chunk was unloaded, the server stalled). */
    public static final int GRACE = 10;
    /** The most things one landing hurts, the largest radius, knockback, size and damage a caller can ask for. */
    public static final int MAX_TARGETS = 24;
    public static final double MIN_RADIUS = 0.5, MAX_RADIUS = 16.0, MAX_KNOCKBACK = 3.0;
    public static final float MIN_SIZE = 0.4f, MAX_SIZE = 6.0f, MAX_DAMAGE = 5000f;
    /** Caps of the setting (server config) and their defaults: meteors in flight per caster and in the whole server. */
    public static final int DEFAULT_PER_CASTER = 4, DEFAULT_GLOBAL = 24, ABS_MAX_PER_CASTER = 8, ABS_MAX_GLOBAL = 64;
    /** Beyond this many blocks a client neither draws a meteor nor makes its trail particles. */
    public static final double DRAW_DISTANCE = 112.0, TRAIL_DISTANCE = 64.0;

    public static int clampFall(int ticks) { return Math.max(MIN_FALL, Math.min(MAX_FALL, ticks)); }

    public static float clampSize(float s) { return Float.isNaN(s) ? 1f : Math.max(MIN_SIZE, Math.min(MAX_SIZE, s)); }

    public static double clampRadius(double r) { return Double.isNaN(r) ? MIN_RADIUS : Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, r)); }

    public static float clampDamage(float d) { return Float.isNaN(d) ? 0f : Math.max(0f, Math.min(MAX_DAMAGE, d)); }

    public static double clampKnockback(double k) { return Double.isNaN(k) ? 0.0 : Math.max(0.0, Math.min(MAX_KNOCKBACK, k)); }

    /**
     * How high the meteor starts: 1.5 blocks per tick of the fall, between 12 and 40, and at least 28 for a fall of 19 ticks or more. The average speed
     * (height over fall ticks) is therefore never above {@link #MAX_SPEED}.
     */
    public static double height(int fallTicks) {
        int t = clampFall(fallTicks);
        double h = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, t * 1.5));
        if (t >= 19) h = Math.max(LONG_MIN_HEIGHT, h);
        return Math.min(h, MAX_SPEED * t);
    }

    /** The sideways offset of the start (blocks) for a start of {@code height}. */
    public static double sideOffset(double height) { return height * SIDE_FACTOR; }

    /** How far along the fall the meteor is after {@code t} of {@code total} ticks (0..1): slow at first, fast at the end, always rising. */
    public static double curve(int t, int total) {
        if (total <= 0) return 1.0;
        double u = Math.max(0.0, Math.min(1.0, t / (double) total));
        return 0.25 * u + 0.75 * u * u;
    }

    /** The meteor's centre after {@code t} of {@code total} ticks, from {@code start} to {@code end} (both {x, y, z}). */
    public static double[] position(double[] start, double[] end, int t, int total) {
        double c = curve(t, total);
        return new double[]{start[0] + (end[0] - start[0]) * c, start[1] + (end[1] - start[1]) * c, start[2] + (end[2] - start[2]) * c};
    }

    /** The start of a fall: {@code height} above the impact and {@link #sideOffset} to the side, in the horizontal direction {@code angle} (radians). */
    public static double[] start(double[] impact, int fallTicks, double angle) {
        double h = height(fallTicks), s = sideOffset(h);
        return new double[]{impact[0] + Math.cos(angle) * s, impact[1] + h, impact[2] + Math.sin(angle) * s};
    }

    /** May one more meteor be launched? {@code owned} is how many this caster has in flight, {@code all} how many the server has; the caps are clamped to their absolute maxima. */
    public static boolean canLaunch(int owned, int all, int perCaster, int global) {
        return owned < cap(perCaster, 1, ABS_MAX_PER_CASTER) && all < cap(global, 1, ABS_MAX_GLOBAL);
    }

    public static int cap(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    /** True once a meteor of {@code fallTicks} has outlived its fall by more than {@link #GRACE} ticks. */
    public static boolean expired(int age, int fallTicks) { return age > clampFall(fallTicks) + GRACE; }

    /** The share of the damage a target at {@code distance} from the centre takes: all of it inside 40 percent of the radius, down to half at the edge, none outside. */
    public static double falloff(double distance, double radius) {
        if (radius <= 0 || distance > radius) return 0.0;
        double inner = radius * 0.4;
        if (distance <= inner) return 1.0;
        return 1.0 - 0.5 * (distance - inner) / (radius - inner);
    }

    /** The rock's spin in degrees per tick: a smaller meteor tumbles faster. */
    public static float spinPerTick(float size) { return 14f / Math.max(0.6f, size); }
}
