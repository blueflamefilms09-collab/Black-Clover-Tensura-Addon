package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

/**
 * Why: the Djinn Full Equip is the standard the owner wants every temporary armour to be built to: the pieces of the set do not pop in, they ASSEMBLE. Each piece, anchored on the model part it was
 * built for, flies in from its own side while it fades up and grows from 85 percent to full size over 8 ticks, the pieces staggered by a per-piece delay, and when the armour ends they shatter out
 * (they drift away along the same direction, fade and swell 25 percent over 12 ticks). That motion lived inside {@code DjinnArmorLayer} as private arithmetic keyed to the Djinn's synced phase; this
 * class is the same arithmetic keyed to an {@link AuraContext} (ticks since the aura began, ticks until it ends), so every magic's armour painter can use the IDENTICAL equip animation with two calls:
 * {@code a = EquipAnimation.assembled(c, delay)} then, with the piece's anchor already applied, {@code float alpha = EquipAnimation.begin(c.pose(), a, dx, dy, dz)} and draw the piece with that alpha
 * (and {@code EquipAnimation.end} is not needed: wrap the piece in pushPose / popPose). The numbers are pure and unit-tested ({@code EquipAnimationTest}) against the Djinn's own.
 * Rules of use: pick delays between 0 and 12 ticks and give the aura at least 40 ticks, so the last piece is fully on before the shatter window (the last 18 ticks) opens.
 */
public final class EquipAnimation {
    private EquipAnimation() {}

    /** Ticks a piece takes to forge in (the Djinn's value). */
    public static final float FORGE_TICKS = 8f;
    /** Ticks a piece takes to shatter out (the Djinn's value). */
    public static final float SHATTER_TICKS = 12f;
    /** The window before the aura's end in which pieces start to shatter: the shatter time plus the largest stagger (12 ticks, halved). */
    public static final float SHATTER_WINDOW = SHATTER_TICKS + 6f;

    /**
     * How far a piece is assembled, the Djinn's convention: 1 fully on, 0..1 forging in, negative shattering out ({@code -progress}, -1 is gone), 0 not there yet.
     *
     * @param t     ticks since the aura began (partial tick included)
     * @param left  ticks until it ends (partial tick included)
     * @param delay the piece's stagger in ticks
     */
    public static float assembled(float t, float left, float delay) {
        if (left < SHATTER_WINDOW) {
            float p = (SHATTER_WINDOW - left - delay * 0.5f) / SHATTER_TICKS;
            if (p >= 1f) return 0f;
            if (p > 0f) return -Mth.clamp(p, 0.001f, 1f);
            // the piece's shatter has not started yet: it is still fully on
        }
        return Mth.clamp((t - delay) / FORGE_TICKS, 0f, 1f);
    }

    public static float assembled(AuraContext c, float delay) { return assembled(c.t(), c.left(), delay); }

    /** The offset factor along the piece's fly-in direction: 1 far away, 0 in place; shattering drifts out to 1.4 (squared progress). */
    public static float offset(float assembled) {
        if (assembled >= 1f || assembled == 0f) return 0f;
        if (assembled > 0f) return (1f - assembled) * (1f - assembled);
        float p = -assembled;
        return p * p * 1.4f;
    }

    /** The opacity of a piece: forging in = the progress, fully on = 1, shattering = 1 - progress. */
    public static float alpha(float assembled) { return assembled >= 1f ? 1f : assembled > 0f ? assembled : assembled < 0f ? 1f + assembled : 0f; }

    /** The size factor: 0.85 growing to 1 while forging in, 1 growing to 1.25 while shattering. */
    public static float scale(float assembled) {
        if (assembled >= 1f || assembled == 0f) return 1f;
        return assembled > 0f ? 0.85f + 0.15f * assembled : 1f + 0.25f * -assembled;
    }

    /**
     * Applies the animation to a pose that is already at the piece's anchor and in model pixels (scaled 1/16): translates by {@code (dx, dy, dz) * offset} (the direction the piece flies in
     * from, in model pixels, the Djinn's own numbers are about 2 to 14) and scales. Returns the alpha to draw the piece with. The caller pushes and pops the pose around the piece.
     */
    public static float begin(PoseStack pose, float assembled, float dx, float dy, float dz) {
        if (assembled < 1f) {
            float off = offset(assembled), sc = scale(assembled);
            pose.translate(dx * off, dy * off, dz * off);
            pose.scale(sc, sc, sc);
        }
        return alpha(assembled);
    }
}
