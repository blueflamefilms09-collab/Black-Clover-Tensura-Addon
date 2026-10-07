package com.newuniverse.nusmp.client;

/**
 * 0.48: the Genesis Demon-Slayer's 3D model as plain geometry, with no Minecraft classes, so tools/item_preview can draw the
 * exact same mesh outside the game. {@link DemonSlayerRenderer} feeds it a PoseStack-backed {@link Sink}.
 * <p>
 * Units are model pixels of 1/64 block (scaled into the item's [0, 1] block space by {@link #draw}). Blade x runs across
 * (-7..7), y along it (1 at the guard .. 49 at the tip), z through (-1..1). The constants marked "shared" must match
 * tools/gen_demon_slayer_textures.py, which paints textures/entity/demon_slayer_sword.png in this layout.
 */
public final class DemonSlayerMesh {
    private DemonSlayerMesh() {}

    /** Layers: the body (texture pixels as uv), the void (position only), the pommel clover (0..1 uv), and two glows. */
    public static final int BODY = 0, VOID = 1, CLOVER = 2, BLOOM = 3, CRACK = 4;

    /** Where the geometry goes. Corners are {x, y, z, u, v}, counter-clockwise seen from outside. */
    public interface Sink {
        void push();
        void pop();
        void translate(float x, float y, float z);
        void rotateZ(float degrees);
        void rotateX(float degrees);
        void scale(float s);
        void quad(int layer, float[] a, float[] b, float[] c, float[] d, float nx, float ny, float nz, int argb);
    }

    // ---- shared with tools/gen_demon_slayer_textures.py
    static final float BLADE_Y0 = 1, BLADE_Y1 = 49, TIP_RIGHT_Y = 46, HALF_BASE = 6.5f, HALF_TOP = 5.5f, T = 1f;
    static final float SLOT_X = 1, SLOT_Y0 = 3, SLOT_Y1 = 19;
    static final float[][] SPOTS_FRONT = {{-5, 24, -2, 27}, {-4, 23, -3, 24}, {-5.5f, 25, -5, 26.5f}, {2, 29, 4, 32}, {1, 30, 2, 31.5f}, {2.5f, 32, 3.5f, 33},
            {-3, 37, -1, 39}, {-2.5f, 39, -1.5f, 40}, {3, 13, 5, 15}, {3.5f, 15, 4.5f, 16}, {-5, 5, -3, 7}, {0, 42, 2, 44}, {-1, 42.5f, 0, 43.5f}};
    static final float[][] SPOTS_BACK = {{-4, 30, -2, 33}, {-2, 31, -1, 32}, {2, 23, 4.5f, 26}, {-5, 14, -3, 16}, {1, 39, 3, 41}, {3, 6, 5, 8}};
    static final int EDGE_U = 28, SLOT_U = 30, CAP_U = 32;
    // ---- not shared
    /** Where the blade breaks during Black Meteorite: four shards. */
    static final float[] CUTS = {BLADE_Y0, 11, 22, 33, BLADE_Y1};
    public static final int FRACTURE_TICKS = 30;
    static final int WHITE = 0xFFFFFFFF, CRIMSON = 0xDC122E, CLOVER_TINT = 0xFF1E161A;

    /** How far apart the blade is (0..1) 'left' ticks before Black Meteorite's fracture ends: breaks fast, holds, re-forms. */
    public static float fracture(float left) {
        if (left <= 0 || left > FRACTURE_TICKS) return 0;
        float q = 1 - left / FRACTURE_TICKS;
        return smooth(q / 0.15f) * (1 - smooth((q - 0.7f) / 0.3f));
    }

    /** The whole sword in item space: on the old 32x32 sprite's diagonal (pommel bottom-left, tip top-right). */
    public static void draw(Sink s, int resonance, float fracture, float time) {
        s.push();
        s.translate(0.516f, 0.523f, 0.5f);
        s.rotateZ(-45f);
        s.scale(0.98f / 64f);
        s.translate(0f, -11.75f, 0f);
        hilt(s);
        for (int i = 0; i < CUTS.length - 1; i++) {
            s.push();
            if (fracture > 0) shardPose(s, i, fracture, time);
            segment(s, i, fracture > 0);
            s.pop();
        }
        if (fracture > 0) crackLight(s, fracture, time);
        bloom(s, Math.max(0, Math.min(4, resonance)), time);
        s.pop();
    }

    static float smooth(float x) { x = Math.max(0, Math.min(1, x)); return x * x * (3 - 2 * x); }

    static float sin(float a) { return (float) Math.sin(a); }

    static float halfW(float y) { return HALF_BASE + (HALF_TOP - HALF_BASE) * (y - BLADE_Y0) / (BLADE_Y1 - BLADE_Y0); }

    static float hash(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** A shard drifts out along the blade and sideways, tilts, and shivers while the meteor runs (the base shard stays put). */
    static void shardPose(Sink s, int i, float e, float time) {
        float cy = (CUTS[i] + CUTS[i + 1]) / 2;
        float shiver = sin(time * 2.3f + i * 1.7f) * 0.18f * e;
        s.translate((hash(i, 1) - 0.5f) * 3.2f * e * Math.min(1, i) + shiver, i * 2.6f * e, (hash(i, 2) - 0.5f) * 2.4f * e * Math.min(1, i));
        s.translate(0, cy, 0);
        s.rotateZ((hash(i, 3) - 0.5f) * 18f * e * Math.min(1, i));
        s.rotateX((hash(i, 4) - 0.5f) * 14f * e * Math.min(1, i));
        s.translate(0, -cy, 0);
    }

    // ================================================================ geometry
    static float[] c(float x, float y, float z, float u, float v) { return new float[]{x, y, z, u, v}; }

    /** An axis-aligned box with the vanilla cube UV layout at (u, v) (top / bottom row of depth d, then the four sides). */
    static void box(Sink s, float x0, float y0, float z0, float x1, float y1, float z1, int u, int v) {
        float w = x1 - x0, h = y1 - y0, d = z1 - z0;
        s.quad(BODY, c(x0, y1, z1, u + d, v + d), c(x1, y1, z1, u + d + w, v + d), c(x1, y1, z0, u + d + w, v), c(x0, y1, z0, u + d, v), 0, 1, 0, WHITE);
        s.quad(BODY, c(x0, y0, z0, u + d + w, v), c(x1, y0, z0, u + d + 2 * w, v), c(x1, y0, z1, u + d + 2 * w, v + d), c(x0, y0, z1, u + d + w, v + d), 0, -1, 0, WHITE);
        s.quad(BODY, c(x0, y0, z0, u, v + d + h), c(x0, y0, z1, u + d, v + d + h), c(x0, y1, z1, u + d, v + d), c(x0, y1, z0, u, v + d), -1, 0, 0, WHITE);
        s.quad(BODY, c(x1, y0, z0, u + d, v + d + h), c(x0, y0, z0, u + d + w, v + d + h), c(x0, y1, z0, u + d + w, v + d), c(x1, y1, z0, u + d, v + d), 0, 0, -1, WHITE);
        s.quad(BODY, c(x1, y0, z1, u + d + w, v + d + h), c(x1, y0, z0, u + 2 * d + w, v + d + h), c(x1, y1, z0, u + 2 * d + w, v + d), c(x1, y1, z1, u + d + w, v + d), 1, 0, 0, WHITE);
        s.quad(BODY, c(x0, y0, z1, u + 2 * d + w, v + d + h), c(x1, y0, z1, u + 2 * d + 2 * w, v + d + h), c(x1, y1, z1, u + 2 * d + 2 * w, v + d), c(x0, y1, z1, u + 2 * d + w, v + d), 0, 0, 1, WHITE);
    }

    /** Crossguard (with chunky ends), cloth-wrapped grip and pommel. */
    static void hilt(Sink s) {
        box(s, -11, -2, -2, 11, 1, 2, 0, 50);
        box(s, -12, -3, -2.5f, -9, 2, 2.5f, 36, 0);
        box(s, 9, -3, -2.5f, 12, 2, 2.5f, 36, 0);
        box(s, -1.5f, -22, -1.5f, 1.5f, -2, 1.5f, 36, 10);
        box(s, -2.5f, -26, -2.5f, 2.5f, -22, 2.5f, 36, 33);
    }

    /** One shard of the blade: both faces (minus the split), its edges, the split's walls and void, the spots' void. */
    static void segment(Sink s, int i, boolean broken) {
        float ya = CUTS[i], yb = CUTS[i + 1];
        boolean last = i == CUTS.length - 2;
        float s0 = Math.max(ya, SLOT_Y0), s1 = Math.min(yb, SLOT_Y1);
        for (int side = 0; side < 2; side++) {
            boolean front = side == 0;
            if (s0 >= s1) facePiece(s, ya, yb, -1, front, last);
            else {
                if (ya < s0) facePiece(s, ya, s0, -1, front, false);
                facePiece(s, s0, s1, 0, front, false);                                // left of the split
                facePiece(s, s0, s1, 1, front, false);                                // right of the split
                if (s1 < yb) facePiece(s, s1, yb, -1, front, last);
            }
        }
        float topR = last ? TIP_RIGHT_Y : yb;
        side(s, halfW(ya), ya, halfW(topR), topR, EDGE_U);                             // right edge, going up
        if (last) side(s, halfW(TIP_RIGHT_Y), TIP_RIGHT_Y, -halfW(BLADE_Y1), BLADE_Y1, EDGE_U);   // the slanted tip
        side(s, -halfW(yb), yb, -halfW(ya), ya, EDGE_U);                               // left edge, going down
        if (broken) {                                                                   // the break faces (the base sits on the guard)
            if (i > 0) cap(s, ya, false);
            if (!last) cap(s, yb, true);
        }
        if (s0 < s1) {                                                                  // the central split: walls, void inside
            side(s, -SLOT_X, s0, -SLOT_X, s1, SLOT_U);
            side(s, SLOT_X, s1, SLOT_X, s0, SLOT_U);
            if (s0 == SLOT_Y0) side(s, SLOT_X, SLOT_Y0, -SLOT_X, SLOT_Y0, SLOT_U);
            if (s1 == SLOT_Y1) side(s, -SLOT_X, SLOT_Y1, SLOT_X, SLOT_Y1, SLOT_U);
            voidRect(s, -SLOT_X, s0, SLOT_X, s1, 0, true);
            voidRect(s, -SLOT_X, s0, SLOT_X, s1, 0, false);
        }
        for (float[] r : SPOTS_FRONT) if (r[1] >= ya && r[3] <= yb) voidRect(s, r[0], r[1], r[2], r[3], T + 0.02f, true);
        for (float[] r : SPOTS_BACK) if (r[1] >= ya && r[3] <= yb) voidRect(s, r[0], r[1], r[2], r[3], -T - 0.02f, false);
    }

    /**
     * A piece of a flat face over [a, b]: part -1 = full width, 0 = left of the split, 1 = right of it; 'tip' slants the top
     * (the last piece). Front faces sit at z = +T (u from 0), back faces at z = -T (u from 14).
     */
    static void facePiece(Sink s, float a, float b, int part, boolean front, boolean tip) {
        float bl = part == 1 ? SLOT_X : -halfW(a), br = part == 0 ? -SLOT_X : halfW(a);
        float tl = part == 1 ? SLOT_X : -halfW(b), tr = part == 0 ? -SLOT_X : halfW(tip ? TIP_RIGHT_Y : b);
        float trY = tip && part != 0 ? TIP_RIGHT_Y : b;
        float z = front ? T : -T, u0 = front ? 7 : 21;
        float[] BL = c(bl, a, z, u0 + bl, 49 - a), BR = c(br, a, z, u0 + br, 49 - a), TR = c(tr, trY, z, u0 + tr, 49 - trY), TL = c(tl, b, z, u0 + tl, 49 - b);
        if (front) s.quad(BODY, BL, BR, TR, TL, 0, 0, 1, WHITE);
        else s.quad(BODY, BL, TL, TR, BR, 0, 0, -1, WHITE);
    }

    /**
     * A wall along the outline edge A -> B through the blade's thickness (the outline runs counter-clockwise seen from the front,
     * so the outside is on its right). 'strip' is the 2-px texture column it samples.
     */
    static void side(Sink s, float ax, float ay, float bx, float by, int strip) {
        float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        boolean vertical = Math.abs(dy) >= Math.abs(dx);
        float va = vertical ? 49 - ay : 24 + ax, vb = vertical ? 49 - by : 24 + bx;
        s.quad(BODY, c(ax, ay, T, strip + 2, va), c(ax, ay, -T, strip, va), c(bx, by, -T, strip, vb), c(bx, by, T, strip + 2, vb), dy / len, -dx / len, 0, WHITE);
    }

    /** A break face across the blade at y (top = facing +y), split around the slot when it cuts through it. */
    static void cap(Sink s, float y, boolean top) {
        float hw = halfW(y);
        boolean slot = y > SLOT_Y0 && y < SLOT_Y1;
        if (top) {
            if (slot) { side(s, hw, y, SLOT_X, y, CAP_U); side(s, -SLOT_X, y, -hw, y, CAP_U); }
            else side(s, hw, y, -hw, y, CAP_U);
        } else {
            if (slot) { side(s, -hw, y, -SLOT_X, y, CAP_U); side(s, SLOT_X, y, hw, y, CAP_U); }
            else side(s, -hw, y, hw, y, CAP_U);
        }
    }

    /** A rectangle in the plane z facing +z (front) or -z, on the given layer with 0..1 uv. */
    static void rect(Sink s, int layer, float x0, float y0, float x1, float y1, float z, boolean front, int argb) {
        if (front) s.quad(layer, c(x0, y0, z, 0, 1), c(x1, y0, z, 1, 1), c(x1, y1, z, 1, 0), c(x0, y1, z, 0, 0), 0, 0, 1, argb);
        else s.quad(layer, c(x0, y0, z, 0, 1), c(x0, y1, z, 0, 0), c(x1, y1, z, 1, 0), c(x1, y0, z, 1, 1), 0, 0, -1, argb);
    }

    static void voidRect(Sink s, float x0, float y0, float x1, float y1, float z, boolean front) { rect(s, VOID, x0, y0, x1, y1, z, front, WHITE); }

    // ================================================================ light
    /** Crimson light glaring out of the gaps between the shards. */
    static void crackLight(Sink s, float e, float time) {
        float flick = 0.8f + 0.2f * sin(time * 1.9f);
        for (int k = 1; k < CUTS.length - 1; k++) {
            float y = CUTS[k] + (k - 0.5f) * 2.6f * e, hw = halfW(y) + 1.5f, half = 1 + 3.2f * e;
            rect(s, CRACK, -hw, y - half, hw, y + half, 0.2f, true, strength(CRIMSON, e * flick));
            rect(s, CRACK, -hw, y - half, hw, y + half, -0.2f, false, strength(CRIMSON, e * flick));
        }
    }

    /** An additive tint: the RGB scaled by k (0..1), full alpha. */
    static int strength(int rgb, float k) {
        k = Math.max(0, Math.min(1, k));
        int r = (int) ((rgb >> 16 & 255) * k), g = (int) ((rgb >> 8 & 255) * k), b = (int) ((rgb & 255) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * The pommel's five-leaf clover on both faces: a near-black clover that grows with resonance (0..4), and a crimson glow
     * behind it pulsing between crimson and black, faster and brighter the stronger the being nearby.
     */
    static void bloom(Sink s, int res, float time) {
        float cy = -24, pulse = 0.5f + 0.5f * sin(time * (0.25f + 0.12f * res));
        float leaf = 2.3f + 0.55f * res;
        rect(s, CLOVER, -leaf, cy - leaf, leaf, cy + leaf, 2.56f, true, CLOVER_TINT);
        rect(s, CLOVER, -leaf, cy - leaf, leaf, cy + leaf, -2.56f, false, CLOVER_TINT);
        float k = res == 0 ? 0.16f + 0.08f * pulse : (0.3f + 0.17f * res) * (0.2f + 0.8f * pulse);   // crimson <-> black
        float glow = (3.2f + 1.6f * res) * (0.92f + 0.16f * pulse);
        rect(s, BLOOM, -glow, cy - glow, glow, cy + glow, 2.62f, true, strength(CRIMSON, k));
        rect(s, BLOOM, -glow, cy - glow, glow, cy + glow, -2.62f, false, strength(CRIMSON, k));
    }
}
