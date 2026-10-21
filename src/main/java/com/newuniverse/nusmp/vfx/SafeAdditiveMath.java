package com.newuniverse.nusmp.vfx;

/**
 * 1.110.0 (docs/visual_upgrade/safe_additive.md): the blend maths of the central additive helper, as plain functions so they can be unit-tested. A channel is 0..1.
 * <ul>
 *   <li>{@link #alphaAdd}: {@code dst + src * srcAlpha * vertexAlpha} (GL {@code SRC_ALPHA, ONE}): a texel of alpha 0 adds nothing whatever colour it carries, so a transparent corner can never be a box.</li>
 *   <li>{@link #plainAdd}: {@code dst + src} (GL {@code ONE, ONE}, vanilla's eyes type): right only for a texture premultiplied by alpha; it lights a colour-under-alpha texel fully.</li>
 *   <li>{@link #eyesKeeps}: the eyes shader drops a texel whose final alpha is below 0.1.</li>
 *   <li>{@link #cardEdgeOk} / {@link #flareOk}: the size caps of a halo card and a flare relative to the weapon (a light shaft scaled far past its weapon was the second half of the Elsdocia screenshot).</li>
 * </ul>
 */
public final class SafeAdditiveMath {
    private SafeAdditiveMath() {}

    /** A halo card's longest edge may be at most this share of the weapon's body height. */
    public static final float MAX_CARD_RATIO = 0.9f;
    /** A flare's half size may be at most this share of the weapon's body height. */
    public static final float MAX_FLARE_RATIO = 0.2f;
    /** The eyes shader's alpha cut. */
    public static final float EYES_DISCARD_BELOW = 0.1f;

    public static float alphaAdd(float dst, float src, float srcAlpha, float vertexAlpha) {
        return clamp01(dst + src * clamp01(srcAlpha) * clamp01(vertexAlpha));
    }

    public static float plainAdd(float dst, float src) { return clamp01(dst + src); }

    public static boolean eyesKeeps(float alpha) { return alpha >= EYES_DISCARD_BELOW; }

    /** The result of adding a premultiplied texel (rgb already times alpha) with the alpha-weighted blend: it is weighed twice, so edges come out a little thinner but never lit. */
    public static float alphaAddPremultiplied(float dst, float premultSrc, float srcAlpha, float vertexAlpha) { return alphaAdd(dst, premultSrc, srcAlpha, vertexAlpha); }

    /** True when drawing this texel adds nothing (the no-box guarantee): alpha 0 with the alpha-weighted blend. */
    public static boolean addsNothing(float srcAlpha, float vertexAlpha) { return alphaAdd(0f, 1f, srcAlpha, vertexAlpha) <= 0f; }

    public static boolean cardEdgeOk(float edge, float bodyHeight) { return bodyHeight <= 0f || edge <= MAX_CARD_RATIO * bodyHeight + 1e-3f; }

    public static boolean flareOk(float halfSize, float bodyHeight) { return bodyHeight <= 0f || halfSize <= MAX_FLARE_RATIO * bodyHeight + 1e-3f; }

    /** The factor by which a card of this longest edge is scaled about its centre to fit the cap (1 = unchanged). */
    public static float cardScale(float edge, float bodyHeight) {
        if (bodyHeight <= 0f || edge <= 0f) return 1f;
        float cap = MAX_CARD_RATIO * bodyHeight;
        return edge <= cap ? 1f : cap / edge;
    }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
}
