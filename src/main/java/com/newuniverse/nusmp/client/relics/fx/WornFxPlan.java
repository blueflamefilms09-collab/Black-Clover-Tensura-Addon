package com.newuniverse.nusmp.client.relics.fx;

/**
 * What a worn relic adds to its model, by grade tier (1 NORMAL .. 7 GENESIS), the client quality option and the distance to the camera. Pure, so every rule is a test.
 * <ul>
 *   <li>{@code rim}: alpha of the faint rim glow (RARE and up): the shell pass fades in towards the silhouette.</li>
 *   <li>{@code shimmer}: alpha of the scrolling shimmer over the shell parts (UNIQUE and up).</li>
 *   <li>{@code motes}: orbiting glow motes (LEGEND and up); {@code runeRing}: the turning rune ring; {@code halo}: the flowing halo band (GOD and up).</li>
 *   <li>{@code pulse}: depth of the gentle brightness pulse of the emissive parts.</li>
 *   <li>{@code particleEvery}, {@code particlesPerEvent}: the ambient particles (every N client ticks per wearer, at most this many at once); 0 = none.</li>
 * </ul>
 * Past {@link #EXTRAS_RANGE} blocks only the model itself is drawn, and the layer draws nothing beyond {@link #CULL_RANGE}.
 */
public record WornFxPlan(float rim, float shimmer, int motes, boolean runeRing, boolean halo, float pulse, int particleEvery, int particlesPerEvent) {
    public static final double EXTRAS_RANGE = 16.0, CULL_RANGE = 32.0;
    public static final WornFxPlan NONE = new WornFxPlan(0f, 0f, 0, false, false, 0f, 0, 0);

    public static WornFxPlan of(int tier, FxQuality q, double distSq) {
        if (tier <= 2 || q == null || distSq > EXTRAS_RANGE * EXTRAS_RANGE) return particlesOnly(tier, q);
        float rim, shimmer, pulse;
        int motes;
        boolean ring, halo;
        switch (Math.min(tier, 7)) {
            case 3 -> { rim = 0.22f; shimmer = 0f; motes = 0; ring = false; halo = false; pulse = 0f; }
            case 4 -> { rim = 0.30f; shimmer = 0.16f; motes = 0; ring = false; halo = false; pulse = 0.05f; }
            case 5 -> { rim = 0.36f; shimmer = 0.22f; motes = 3; ring = true; halo = false; pulse = 0.12f; }
            case 6 -> { rim = 0.42f; shimmer = 0.28f; motes = 5; ring = true; halo = true; pulse = 0.18f; }
            default -> { rim = 0.50f; shimmer = 0.34f; motes = 7; ring = true; halo = true; pulse = 0.25f; }
        }
        if (q == FxQuality.LOW) {                                            // the cheapest look: a rim, two motes, nothing that scrolls or turns
            shimmer = 0f;
            motes = Math.min(motes, 2);
            ring = false;
            halo = false;
            rim *= 0.8f;
        } else if (q == FxQuality.MEDIUM) {
            motes = Math.min(motes, 4);
            halo = halo && tier >= 7;
        }
        WornFxPlan p = particlesOnly(tier, q);
        return new WornFxPlan(rim, shimmer, motes, ring, halo, pulse, p.particleEvery, p.particlesPerEvent);
    }

    /** The ambient particles do not depend on the distance rule above (the caller culls the wearers it does not look at). */
    private static WornFxPlan particlesOnly(int tier, FxQuality q) {
        if (tier <= 2 || q == null) return NONE;
        int every, per;
        switch (Math.min(tier, 7)) {
            case 3 -> { every = 20; per = 1; }
            case 4 -> { every = 14; per = 1; }
            case 5 -> { every = 8; per = 1; }
            case 6 -> { every = 5; per = 2; }
            default -> { every = 3; per = 2; }
        }
        every = Math.max(1, Math.round(every / q.particleRate));
        if (q == FxQuality.LOW) per = 1;
        return new WornFxPlan(0f, 0f, 0, false, false, 0f, every, per);
    }

    /** Vertices the extra passes of this plan cost on top of the model: the shell pass (the caller adds it) and the procedural motes, ring and halo. */
    public int fxVertices() {
        return motes * 32 + (runeRing ? 128 : 0) + (halo ? 128 : 0);
    }

    public boolean usesShell() { return rim > 0f || shimmer > 0f; }
}
