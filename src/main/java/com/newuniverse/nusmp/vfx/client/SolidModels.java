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
}
