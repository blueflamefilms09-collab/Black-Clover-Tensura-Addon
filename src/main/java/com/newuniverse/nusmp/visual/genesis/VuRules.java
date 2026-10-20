package com.newuniverse.nusmp.visual.genesis;

import com.newuniverse.nusmp.perf.VfxQuality;

/**
 * The decisions of the upgraded Genesis weapon renderer, as pure functions (no Minecraft classes), so the performance guard of the brief is pinned by unit tests:
 * which mesh at which distance, which passes at which distance and quality, how the draw animation eases, how the vein light flows, how a flicker is derived from a
 * tick count without a random generator.
 */
public final class VuRules {
    private VuRules() {}

    /** The most welded vertices a Genesis weapon mesh may have in the hand at full detail (brief: the performance guard). */
    public static final int MAX_VERTICES_GENESIS = 6000;
    /** The level-of-detail mesh may have at most this share of the vertices of the full one. */
    public static final float LOD_RATIO_MAX = 0.15f;
    /** Beyond this many blocks (third person, other players, frames, drops) the LOD mesh is drawn. */
    public static final float LOD_NEAR = 12f;
    /** Beyond this many blocks the additive passes (void, glow veins, halo cards, flares) are skipped. */
    public static final float ADDITIVE_FAR = 24f;
    /** Ambient particles of one holder per client tick, at most (the shared client budget {@code PerfBudgets.clientParticles} limits all holders together). */
    public static final int MAX_PARTICLES_PER_HOLDER = 6;

    // ---------------------------------------------------------------- mesh and pass selection
    /** Which mesh: the full one up close, the LOD one beyond {@code near} blocks (squared distances: no square root per draw). */
    public static boolean useLod(double distSq, boolean firstPerson, float near) {
        return !firstPerson && distSq > (double) near * near;
    }

    public static boolean additiveCulled(double distSq, boolean firstPerson, float far) {
        return !firstPerson && distSq > (double) far * far;
    }

    /** The halo cards and flares are drawn at medium and high quality, the vein wave at every quality (it is the item's emissive core), animation from medium up. */
    public static boolean haloAllowed(VfxQuality q) { return q != VfxQuality.LOW; }

    public static boolean animationAllowed(VfxQuality q) { return q != VfxQuality.LOW; }

    public static boolean flaresAllowed(VfxQuality q) { return q == VfxQuality.HIGH; }

    /** Particle emission per holder per tick, scaled by the quality (0 at low). */
    public static int particleBudget(VfxQuality q) {
        return switch (q) {
            case LOW -> 0;
            case MEDIUM -> MAX_PARTICLES_PER_HOLDER / 2;
            case HIGH -> MAX_PARTICLES_PER_HOLDER;
        };
    }

    /** The upgraded mesh is used only when the client flag is on and the weapon ships one; otherwise the old mesh (the fallback of the brief). */
    public static boolean useUpgrade(boolean flag, boolean hasUpgradedMesh) { return flag && hasUpgradedMesh; }

    // ---------------------------------------------------------------- maths
    /** sin(2 pi x) for a phase in turns. */
    public static float sin01(float x) { return (float) Math.sin(x * (2.0 * Math.PI)); }

    /** 0 before the draw starts, 1 when it is over, linear between ({@code age} in ticks since the draw began). */
    public static float drawProgress(float age, int ticks) {
        if (age <= 0f) return 0f;
        if (age >= ticks) return 1f;
        return age / ticks;
    }

    /** A small overshoot on the way to 1 (the parts snap open a little past and settle back). */
    public static float easeOutBack(float p) {
        if (p <= 0f) return 0f;
        if (p >= 1f) return 1f;
        float c1 = 1.2f, c3 = c1 + 1f, q = p - 1f;
        return 1f + c3 * q * q * q + c1 * q * q;
    }

    /** True while the draw animation of a weapon first seen at {@code start} is still running at {@code now}; a gap of more than {@code regap} ticks between two sightings starts a new draw. */
    public static boolean newDraw(long lastSeen, long now, int regap) { return lastSeen < 0 || now - lastSeen > regap || now < lastSeen; }

    /** The flow of light up a blade: 0.3 to 1 as a travelling wave of `freq` per cm moving at `speed` cm per tick. */
    public static float veinWave(float y, float ticks, float freq, float speed) {
        float s = 0.5f + 0.5f * (float) Math.sin((y * freq - ticks * speed) * (2.0 * Math.PI));
        return 0.3f + 0.7f * s * s;
    }

    /** A deterministic flicker in [min, max]: a hash of the time bucket, smoothed between buckets. */
    public static float flicker(float ticks, float ratePerSecond, float min, float max, int seed) {
        float x = ticks * ratePerSecond / 20f;
        int i = (int) Math.floor(x);
        float f = x - i;
        f = f * f * (3f - 2f * f);
        float a = hash01(i * 31 + seed), b = hash01((i + 1) * 31 + seed);
        float v = a + (b - a) * f;
        return min + (max - min) * (v > 0.55f ? 1f : v * 1.4f);
    }

    public static float hash01(int n) {
        n = (n << 13) ^ n;
        int h = n * (n * n * 15731 + 789221) + 1376312589;
        return (h & 0x7fffffff) / (float) 0x7fffffff;
    }

    /** Where the tip of a weapon of `reach` blocks is at a point `progress` (0..1) of a swing, relative to the eye, as (side, up, forward): an arc from the shoulder across the front. */
    public static void swingTip(float progress, float reach, float[] out) {
        float theta = (float) Math.toRadians(70.0 - 140.0 * progress);
        out[0] = (float) Math.sin(theta) * reach * 0.92f;
        out[1] = (0.55f - progress) * 0.5f * reach;
        out[2] = (float) Math.cos(theta) * reach * 0.92f;
    }

    /** Packs 0..255 channels and an alpha 0..1 into 0xAARRGGBB. */
    public static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return a << 24 | (rgb & 0xFFFFFF);
    }

    /** The share of a colour channel after a brightness factor, clamped. */
    public static int channel(int c, float k) { return Math.max(0, Math.min(255, (int) (c * k))); }
}
