package com.newuniverse.nusmp.vfx.client;

import org.joml.Vector3f;

/**
 * Why (docs/visual/flat_props.md): the solid replacements of the flat sword / axe / shield / paper-figure sprites, built from {@link SolidParts}. Each is a handful of low-poly shaded parts, in
 * local blocks (about 1.1 high, centred), and costs at most the quads below once the faces turned away from the camera are dropped (4 vertices a quad, drawn through one texture):
 * <pre>
 *   sword    blade box + tip pyramid + guard + grip + pommel      about 14 quads
 *   axe      shaft + two wedge heads + two caps                   about 13 quads
 *   shield   bevelled pentagon + boss + bar                       about 19 quads
 *   figure   head, torso, two arms, legs (a glass mannequin)      about 15 quads
 * </pre>
 * Each returns false when the instance's vertex budget cannot hold the model (nothing is drawn, never half a model) or when it is too far away ({@link SolidParts#MAX_DISTANCE}). Nothing is allocated.
 */
public final class SolidModels {
    private SolidModels() {}

    private static final Vector3f TMP = new Vector3f();                // scratch (render thread only)
    private static final float[] SHIELD = {-0.28f, 0.32f, 0.28f, 0.32f, 0.28f, -0.04f, 0f, -0.42f, -0.28f, -0.04f};      // counter-clockwise, convex: the classic pentagon shield

    /** 0xFFFFFFFF mixed towards {@code accent} by f (a colour that carries a little of the spell's colour into the metal). */
    public static int tint(int accent, float f) {
        int r = 255 - (int) ((255 - ((accent >> 16) & 255)) * f), g = 255 - (int) ((255 - ((accent >> 8) & 255)) * f), b = 255 - (int) ((255 - (accent & 255)) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static boolean sword(VfxVertexBuffer buf, Vector3f c, float yaw, float tilt, float scale, float alpha, int accent) {
        SolidParts.Model m = SolidParts.begin(c, yaw, tilt, scale, alpha);
        if (m.culled()) return false;
        int steel = tint(accent, 0.22f), trim = tint(accent, 0.55f);
        m.box(-0.05f, -0.23f, -0.022f, 0.05f, 0.34f, 0.022f, SolidParts.STEEL, steel);
        m.pyramid(0f, 0.34f, 0f, 0.05f, 0.022f, 0f, 0.52f, 0f, SolidParts.STEEL, steel);
        m.box(-0.15f, -0.27f, -0.035f, 0.15f, -0.23f, 0.035f, SolidParts.GOLD, trim);
        m.box(-0.022f, -0.43f, -0.022f, 0.022f, -0.27f, 0.022f, SolidParts.LEATHER, 0xFFFFFFFF);
        m.box(-0.04f, -0.5f, -0.04f, 0.04f, -0.43f, 0.04f, SolidParts.GOLD, trim);
        return m.draw(buf);
    }

    public static boolean axe(VfxVertexBuffer buf, Vector3f c, float yaw, float tilt, float scale, float alpha, int accent) {
        SolidParts.Model m = SolidParts.begin(c, yaw, tilt, scale, alpha);
        if (m.culled()) return false;
        int steel = tint(accent, 0.22f), trim = tint(accent, 0.55f);
        m.box(-0.02f, -0.5f, -0.02f, 0.02f, 0.5f, 0.02f, SolidParts.OAK, 0xFFFFFFFF);
        m.wedge(0.02f, 0.18f, 0.42f, 0.035f, 0.30f, 0.02f, 0.58f, SolidParts.STEEL, steel);
        m.wedge(-0.02f, 0.18f, 0.42f, 0.035f, -0.30f, 0.02f, 0.58f, SolidParts.STEEL, steel);
        m.pyramid(0f, 0.5f, 0f, 0.02f, 0.02f, 0f, 0.6f, 0f, SolidParts.STEEL, steel);
        m.box(-0.035f, -0.5f, -0.035f, 0.035f, -0.44f, 0.035f, SolidParts.GOLD, trim);
        return m.draw(buf);
    }

    /** Faces +z (the front of the shield) when yaw is 0. */
    public static boolean shield(VfxVertexBuffer buf, Vector3f c, float yaw, float tilt, float scale, float alpha, int accent) {
        SolidParts.Model m = SolidParts.begin(c, yaw, tilt, scale, alpha);
        if (m.culled()) return false;
        int steel = tint(accent, 0.22f), trim = tint(accent, 0.55f);
        m.plate(SHIELD, 0f, 0.06f, 0.35f, SolidParts.STEEL, steel);
        m.box(-0.025f, -0.36f, 0.03f, 0.025f, 0.30f, 0.055f, SolidParts.GOLD, trim);
        m.box(-0.07f, 0.02f, 0.03f, 0.07f, 0.16f, 0.075f, SolidParts.GOLD, trim);
        return m.draw(buf);
    }

    /** A glass mannequin about 1.8 high (scale 1) standing on the point c (feet), facing +z when yaw is 0. Used by the Mirror doubles. */
    public static boolean figure(VfxVertexBuffer buf, Vector3f feet, float yaw, float scale, float alpha, int accent) {
        SolidParts.Model m = SolidParts.begin(TMP.set(feet.x, feet.y + 0.9f * scale, feet.z), yaw, 0f, scale, alpha);      // the model's feet are at local y = -0.9
        if (m.culled()) return false;
        int glass = tint(accent, 0.30f), silver = tint(accent, 0.15f);
        m.box(-0.11f, 0.58f, -0.11f, 0.11f, 0.80f, 0.11f, SolidParts.GLASS, glass);                  // head
        m.box(-0.19f, 0.00f, -0.10f, 0.19f, 0.56f, 0.10f, SolidParts.GLASS, glass);                  // torso
        m.box(-0.31f, 0.02f, -0.06f, -0.20f, 0.54f, 0.06f, SolidParts.SILVER, silver);               // arms
        m.box(0.20f, 0.02f, -0.06f, 0.31f, 0.54f, 0.06f, SolidParts.SILVER, silver);
        m.box(-0.18f, -0.90f, -0.08f, 0.18f, -0.02f, 0.08f, SolidParts.GLASS, glass);                // legs
        return m.draw(buf);
    }

    // ---- the hero weapon projectiles (docs/heroweapon/flat_parts.md): local +y is the way it flies; about 30 quads each once the faces turned away are dropped (HeroSolid.MAX_QUADS_EACH)
    private static final float[] ARROW_LEAF = {0f, 0.50f, -0.034f, 0.355f, -0.026f, 0.29f, 0.026f, 0.29f, 0.034f, 0.355f};                              // a broadhead, counter-clockwise
    private static final float[] SPEAR_LEAF = {0f, 0.56f, -0.072f, 0.34f, -0.088f, 0.17f, -0.040f, 0.0f, 0.040f, 0.0f, 0.088f, 0.17f, 0.072f, 0.34f};     // a leaf blade, convex

    /** An arrow about 1 block long at scale 1, flying along {@code dir} (its point leads): a shaft, a nock, a broadhead with a ridge (a bevelled plate) and two crossed vanes. */
    public static boolean arrow(VfxVertexBuffer buf, Vector3f c, Vector3f dir, float scale, float alpha, int accent) {
        SolidParts.Model m = SolidParts.beginAimed(c, dir, scale, alpha);
        if (m.culled()) return false;
        int shaft = tint(accent, 0.18f), steel = tint(accent, 0.12f), fletch = tint(accent, 0.6f);
        m.box(-0.016f, -0.46f, -0.016f, 0.016f, 0.31f, 0.016f, SolidParts.BONE, shaft);
        m.box(-0.021f, -0.50f, -0.021f, 0.021f, -0.46f, 0.021f, SolidParts.DARK, 0xFFFFFFFF);
        m.plate(ARROW_LEAF, 0f, 0.034f, 0.55f, SolidParts.STEEL, steel);
        m.box(-0.036f, -0.47f, -0.0035f, 0.036f, -0.31f, 0.0035f, SolidParts.SILVER, fletch);                // the vanes cross in 3D
        m.box(-0.0035f, -0.47f, -0.036f, 0.0035f, -0.31f, 0.036f, SolidParts.SILVER, fletch);
        return m.draw(buf);
    }

    /**
     * A spear tip about 0.85 block long at scale 1, flying along {@code dir} (the point leads, the local origin is the collar): a socket, a collar with two lugs and a leaf blade with a sharp ridge (a bevelled plate).
     * {@code barbed} adds the two backward wedges of a harpoon head.
     */
    public static boolean spearTip(VfxVertexBuffer buf, Vector3f c, Vector3f dir, float scale, float alpha, int accent, boolean barbed) {
        SolidParts.Model m = SolidParts.beginAimed(c, dir, scale, alpha);
        if (m.culled()) return false;
        int steel = tint(accent, 0.22f), trim = tint(accent, 0.55f);
        m.box(-0.030f, -0.30f, -0.030f, 0.030f, -0.04f, 0.030f, SolidParts.GOLD, trim);
        m.box(-0.048f, -0.06f, -0.048f, 0.048f, 0.0f, 0.048f, SolidParts.GOLD, trim);
        m.box(-0.088f, -0.05f, -0.011f, -0.048f, 0.03f, 0.011f, SolidParts.GOLD, trim);
        m.box(0.048f, -0.05f, -0.011f, 0.088f, 0.03f, 0.011f, SolidParts.GOLD, trim);
        m.plate(SPEAR_LEAF, 0f, 0.075f, 0.85f, SolidParts.STEEL, steel);
        if (barbed) {
            m.wedge(0.05f, 0.14f, 0.32f, 0.018f, 0.115f, 0.04f, 0.17f, SolidParts.STEEL, steel);
            m.wedge(-0.05f, 0.14f, 0.32f, 0.018f, -0.115f, 0.04f, 0.17f, SolidParts.STEEL, steel);
        }
        return m.draw(buf);
    }
}
