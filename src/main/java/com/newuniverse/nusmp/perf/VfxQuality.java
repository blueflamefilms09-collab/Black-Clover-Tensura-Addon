package com.newuniverse.nusmp.perf;

import java.util.Locale;

/**
 * Why: one switch for how much the big effects (the VFX layers, the spec-driven FX pieces, the aura painters) may cost a frame. Pure data, no game classes, so the numbers are unit-tested.
 * {@link #HIGH} is the look the mod always had: nothing is thinned out near the camera; only things far past the distance anybody can see are dropped. {@link #MEDIUM} and {@link #LOW}
 * are for weaker machines: fewer effects at once, a lower level of detail from further away, a hard cap on the vertices one frame may draw.
 *
 * <ul>
 *   <li>{@code detail}: multiplied into the segment counts of an effect (the layers read it as {@code ctx.detail}); 1 = as designed.</li>
 *   <li>{@code maxActive}: how many effects may run at the same time (the oldest is dropped first, as before).</li>
 *   <li>{@code frameVertices}: the vertices all effects together may add to one frame (the nearest effects are drawn first, so the cap thins out the far ones).</li>
 *   <li>{@code lodNear} / {@code lodFar}: an effect nearer than {@code lodNear} blocks is drawn at full detail, up to the middle of the two at 3/4 detail, up to {@code lodFar} at half detail, beyond it not at all.</li>
 * </ul>
 */
public enum VfxQuality {
    LOW(0.5f, 24, 6000, 40.0, 96.0),
    MEDIUM(0.75f, 40, 14000, 64.0, 160.0),
    HIGH(1.0f, 64, 40000, 96.0, 256.0);

    public final float detail;
    public final int maxActive;
    public final int frameVertices;
    public final double lodNear;
    public final double lodFar;

    VfxQuality(float detail, int maxActive, int frameVertices, double lodNear, double lodFar) {
        this.detail = detail;
        this.maxActive = maxActive;
        this.frameVertices = frameVertices;
        this.lodNear = lodNear;
        this.lodFar = lodFar;
    }

    /** The quality named by a config string ("low", "medium", "high", any case); {@code fallback} for anything else (null included). */
    public static VfxQuality byName(String name, VfxQuality fallback) {
        if (name == null) return fallback;
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (VfxQuality q : values()) if (q.name().toLowerCase(Locale.ROOT).equals(n)) return q;
        return fallback;
    }

    /** The detail multiplier for an effect {@code distance} blocks from the camera: 1, 0.75, 0.5 by distance, and 0 beyond {@link #lodFar} (cull). Never above {@link #detail}. */
    public float detailAt(double distance) {
        return Math.min(detail, PerfMath.lodDetail(distance, lodNear, lodFar));
    }

    /** Is an effect {@code distance} blocks away past what is drawn at all? */
    public boolean culled(double distance) { return distance > lodFar; }

    /** {@link #detailAt} on a squared distance (no square root per effect per frame). */
    public float detailAtSq(double distanceSq) {
        return Math.min(detail, PerfMath.lodDetailSq(distanceSq, lodNear, lodFar));
    }

    /** {@link #culled} on a squared distance. */
    public boolean culledSq(double distanceSq) { return distanceSq > lodFar * lodFar; }
}
