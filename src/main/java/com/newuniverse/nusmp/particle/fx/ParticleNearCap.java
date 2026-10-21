package com.newuniverse.nusmp.particle.fx;

/**
 * Why: a particle a few steps from the camera fills the screen. In first person the weapon of the player is under the camera, so every mote, flame and spark the blade sheds is born within a
 * block of the eye, and a sprite that is 0.3 blocks wide at 0.4 blocks distance covers a third of the view (the flame in the owner's screenshots). This pure class (no game types) holds the
 * three rules that stop it: a particle nearer than {@link #NEAR_FULL} blocks shrinks smoothly and vanishes at {@link #NEAR_ZERO}; an ambient-sized particle never takes more than
 * {@link #MAX_RATIO} of its distance as half-size (about a fifth of the view height at most, however close it is); and an effect that spawns a particle within {@link #SPAWN_SKIP} blocks of the
 * camera does not spawn it at all. The big shapes of the library (rings, bursts, flashes) are not ambient and keep their size ({@link #applies}).
 */
public final class ParticleNearCap {
    private ParticleNearCap() {}

    /** Closer than this (blocks) a particle is gone; at {@link #NEAR_FULL} it has its full size again. */
    public static final double NEAR_ZERO = 0.35, NEAR_FULL = 1.5;
    /** The largest half-size of an ambient particle as a fraction of its distance from the camera. */
    public static final float MAX_RATIO = 0.14f;
    /** The largest half-size (blocks) of an ambient particle at any distance. */
    public static final float MAX_HALF_SIZE = 0.45f;
    /** An upgrade effect does not spawn a particle nearer than this (blocks) to the camera. */
    public static final double SPAWN_SKIP = 1.2;
    /** Only particles whose profile size (full width, start or end) stays at or under this are capped; the larger ones are shapes (rings, bursts, flashes). */
    public static final float AMBIENT_MAX_WIDTH = 0.7f;

    /** Is a particle of this profile an ambient one (a mote, a flame, a spark), so that the near rules apply to it? */
    public static boolean applies(float sizeStart, float sizeEnd) {
        return Math.max(sizeStart, sizeEnd) <= AMBIENT_MAX_WIDTH;
    }

    /** 0 at {@link #NEAR_ZERO} or nearer, 1 at {@link #NEAR_FULL} or further, smooth in between (squared distance in, no root per particle). */
    public static float nearFactor(double distSq) {
        if (distSq >= NEAR_FULL * NEAR_FULL) return 1f;
        if (distSq <= NEAR_ZERO * NEAR_ZERO) return 0f;
        double d = Math.sqrt(distSq);
        float t = (float) ((d - NEAR_ZERO) / (NEAR_FULL - NEAR_ZERO));
        return t * t * (3f - 2f * t);
    }

    /** The half-size a particle of {@code halfSize} blocks may be drawn with at squared distance {@code distSq} from the camera. */
    public static float cap(float halfSize, double distSq) {
        if (halfSize <= 0f) return 0f;
        float f = nearFactor(distSq);
        if (f <= 0f) return 0f;
        float byRatio = (float) (Math.sqrt(Math.max(0.0, distSq)) * MAX_RATIO);
        return Math.min(Math.min(halfSize * f, byRatio), MAX_HALF_SIZE);
    }

    /** Should an upgrade effect skip a spawn at this squared distance from the camera? */
    public static boolean skipSpawn(double distSq) {
        return distSq < SPAWN_SKIP * SPAWN_SKIP;
    }
}
