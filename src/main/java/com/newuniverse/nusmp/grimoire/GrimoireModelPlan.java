package com.newuniverse.nusmp.grimoire;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Assembles one grimoire out of boxes and flat quads, as plain data ({@link PlannedQuad}); no rendering classes involved,
 * so the whole thing is unit-testable. The client turns the plan into BakedQuads once per {@link GrimoireRenderKey} and caches it.
 * <p>
 * Space: model pixels 0..16 (divided by 16 on output). The book stands upright, front cover facing +Z (south, towards the
 * camera in the GUI), spine on the west side, centred on z = 8.
 * <p>
 * Parts, in draw order (opaque first, blended / emissive last):
 * <ol>
 *   <li>spine, front + back cover boards, page block (coverless "Boundless" books drop the boards and show the pages)</li>
 *   <li>trim: corner brackets and spine bands (tint 1)</li>
 *   <li>clasp: strap + buckle, dual chains, or strap + padlock + key (thickness-aware)</li>
 *   <li>insignia on the front cover (tint 2)</li>
 *   <li>aura: rim halo front/back, drifting sparks (rare+), magic circle over the crest (forbidden/special) - all tint 3, full-bright</li>
 * </ol>
 */
public final class GrimoireModelPlan {
    private GrimoireModelPlan() {}

    /** One axis-aligned quad. pos = 4 corners x (x,y,z) in 0..1 model units, counter-clockwise seen from outside; uv = 4 x (u,v) in 0..16. */
    public record PlannedQuad(Direction direction, GrimoireSprite sprite, int tint, boolean shade, boolean emissive, float[] pos, float[] uv) {}

    // book frame (px)
    static final float X0 = 3.6f, X1 = 13.0f, Y0 = 1f, Y1 = 15f, SPINE_X = 2.6f;
    // crest vertical centre; horizontally it sits in the middle of the cover (nudged left to leave room when a clasp is fitted)
    static final float INS_CY = 8.0f;
    // clasps live to the right of the crest
    static final float CLASP_X0 = 11.0f, CLASP_X1 = 13.45f;

    // face masks
    private static final int N = 1, S = 2, W = 4, E = 8, D = 16, U = 32, ALL = 63;

    public static List<PlannedQuad> build(GrimoireRenderKey key) {
        Builder b = new Builder();
        Thickness t = key.thickness();
        float hz = t.halfDepth, board = t.board;
        float zF1 = 8 + hz, zF0 = zF1 - board, zB0 = 8 - hz, zB1 = zB0 + board;
        boolean coverless = key.insignia() == InsigniaType.BOUNDLESS;
        GrimoireSprite cover = key.cover().sprite;
        TrimMetal.Style style = key.trimStyle();

        // ---- 1. body
        b.box(cover, 0, true, SPINE_X, Y0, zB0, X0, Y1, zF1, W | U | D, 0, 0, 16, 16);
        b.face(Direction.SOUTH, cover, 0, true, false, SPINE_X, Y0, zF1, X0, Y1, zF1, 0, 0, 1.7f, 16);
        b.face(Direction.NORTH, cover, 0, true, false, SPINE_X, Y0, zB0, X0, Y1, zB0, 0, 0, 1.7f, 16);
        if (coverless) {
            b.box(GrimoireSprite.PAGES, -1, true, X0, Y0, zB0, X1, Y1, zF1, S | N | E | U | D, 0, 0, 16, 16);
        } else {
            b.face(Direction.SOUTH, cover, 0, true, false, X0, Y0, zF1, X1, Y1, zF1, 0, 0, 16, 16);          // front cover
            b.box(cover, 0, true, X0, Y0, zF0, X1, Y1, zF1, U | D | E, 0, 0, 16, 0.8f);
            b.face(Direction.NORTH, cover, 0, true, false, X0, Y0, zB0, X1, Y1, zB0, 0, 0, 16, 16);          // back cover
            b.box(cover, 0, true, X0, Y0, zB0, X1, Y1, zB1, U | D | E, 0, 0, 16, 0.8f);
            b.box(GrimoireSprite.PAGES, -1, true, X0, Y0 + 0.5f, zB1, X1 - 0.4f, Y1 - 0.5f, zF0, E | U | D, 0, 0, 16, 16);
        }

        // ---- 2. trim
        // canon: ordinary grimoires are bare leather; a rare ("ornate") cover carries gilded ornaments around its border instead of
        // hardware. Corner caps and spine bands only exist for the non-canon metal styles (generator ids, showcase).
        boolean ornate = key.cover() == CoverMaterial.METALLIC_TRIM && !coverless;
        if (ornate) {
            b.face(Direction.SOUTH, GrimoireSprite.TRIM_BORDER, 1, true, false, X0 + 0.25f, Y0 + 0.25f, zF1 + 0.06f, X1 - 0.25f, Y1 - 0.25f, zF1 + 0.06f, 0, 0, 16, 16);
        }
        if (style != TrimMetal.Style.NONE && !ornate) {
            float s = style.bracket;
            GrimoireSprite trim = style.sprite;
            float[] bx0 = {X0, X1 + 0.25f - s}, by0 = {Y0 - 0.25f, Y1 + 0.25f - s};
            for (float x : bx0) {
                for (float y : by0) {
                    float xa = x, xb = x + s;
                    b.box(trim, 1, true, xa, y, zF0, xb, y + s, zF1 + 0.35f, S | E | W | U | D, 0, 0, 16, 16);
                    b.box(trim, 1, true, xa, y, zB0 - 0.35f, xb, y + s, zB1, N | E | W | U | D, 0, 0, 16, 16);
                }
            }
            float[] bands = {2.3f, 7.35f, 12.4f};       // raised bands on the spine, symmetric about the middle
            for (float y : bands) b.box(trim, 1, true, SPINE_X - 0.25f, y, zB0 - 0.25f, X0 + 0.3f, y + 1.3f, zF1 + 0.25f, ALL, 0, 0, 16, 16);
            if (style == TrimMetal.Style.RUNED) b.box(trim, 1, true, SPINE_X - 0.35f, 6.2f, zB0 - 0.3f, X0 + 0.1f, 9.8f, zF1 + 0.3f, ALL, 0, 0, 16, 16);
        }

        // ---- 3. clasp
        switch (key.clasp()) {
            case OPEN -> { }
            case SINGLE_BUCKLE -> {
                strap(b, GrimoireSprite.CLASP_STRAP, 0, 7.0f, 9.0f, 0.3f, zB0, zF1);
                b.box(GrimoireSprite.CLASP_BUCKLE, 1, true, 10.7f, 6.6f, zF1 + 0.3f, 12.0f, 9.4f, zF1 + 0.75f, ALL & ~N, 0, 0, 16, 16);
            }
            case DUAL_CHAINS -> {
                strap(b, GrimoireSprite.CLASP_CHAIN, 1, 4.9f, 6.0f, 0.45f, zB0, zF1);
                strap(b, GrimoireSprite.CLASP_CHAIN, 1, 10.0f, 11.1f, 0.45f, zB0, zF1);
            }
            case LOCK_AND_KEY -> {
                strap(b, GrimoireSprite.CLASP_STRAP, 0, 7.4f, 8.8f, 0.3f, zB0, zF1);
                GrimoireSprite lock = GrimoireSprite.CLASP_LOCK;
                b.box(lock, 1, true, 11.1f, 5.0f, zF1 + 0.3f, 13.0f, 7.4f, zF1 + 1.3f, ALL & ~N, 0, 0, 16, 16);      // padlock body
                b.box(lock, 1, true, 11.5f, 7.4f, zF1 + 0.5f, 11.8f, 8.4f, zF1 + 0.8f, ALL & ~N, 0, 0, 4, 4);        // shackle posts + bar
                b.box(lock, 1, true, 12.3f, 7.4f, zF1 + 0.5f, 12.6f, 8.4f, zF1 + 0.8f, ALL & ~N, 0, 0, 4, 4);
                b.box(lock, 1, true, 11.5f, 8.1f, zF1 + 0.5f, 12.6f, 8.4f, zF1 + 0.8f, ALL & ~N, 0, 0, 4, 4);
                float kz0 = zF1 + 0.12f, kz1 = zF1 + 0.45f;                                                              // key lying on the cover
                b.box(lock, 1, true, 7.2f, 2.4f, kz0, 8.4f, 3.6f, kz1, ALL & ~N, 0, 0, 5, 5);
                b.box(lock, 1, true, 8.4f, 2.8f, kz0, 10.1f, 3.2f, kz1, ALL & ~N, 0, 0, 5, 3);
                b.box(lock, 1, true, 9.5f, 2.2f, kz0, 9.8f, 2.8f, kz1, ALL & ~N, 0, 0, 3, 3);
            }
        }

        // ---- 4. insignia (a quad hovering 0.1px above the front cover)
        boolean clasped = key.clasp() != ClaspType.OPEN;
        float cx = clasped ? 7.7f : (X0 + X1) / 2f, half = clasped ? 3.3f : 3.7f;
        b.face(Direction.SOUTH, GrimoireSprite.insignia(key.insignia()), 2, true, false,
                cx - half, INS_CY - half, zF1 + 0.10f, cx + half, INS_CY + half, zF1 + 0.10f, 0, 0, 16, 16);

        // ---- 5. aura (blended + emissive, so it goes last)
        // only while held: a grimoire glows slightly when it is out and in use. Rim glow = the cover's own footprint (+0.3px),
        // just above the brackets, so it hugs the book instead of floating off it
        if (key.held()) {
            b.face(Direction.SOUTH, GrimoireSprite.AURA_HALO, 3, false, true, X0 - 0.3f, Y0 - 0.3f, zF1 + 0.42f, X1 + 0.3f, Y1 + 0.3f, zF1 + 0.42f, 0, 0, 16, 16);
            b.face(Direction.NORTH, GrimoireSprite.AURA_HALO, 3, false, true, X0 - 0.3f, Y0 - 0.3f, zB0 - 0.42f, X1 + 0.3f, Y1 + 0.3f, zB0 - 0.42f, 0, 0, 16, 16);
        }
        if (key.held() && key.auraTier() >= 3) {
            b.face(Direction.SOUTH, GrimoireSprite.AURA_SPARKS, 3, false, true, 0.8f, 0.2f, 8f, 15.2f, 15.8f, 8f, 0, 0, 16, 16);
            b.face(Direction.NORTH, GrimoireSprite.AURA_SPARKS, 3, false, true, 0.8f, 0.2f, 8f, 15.2f, 15.8f, 8f, 0, 0, 16, 16);
        }
        if (key.held() && key.auraTier() >= 3) {
            b.face(Direction.SOUTH, GrimoireSprite.AURA_SIGIL, 3, false, true, cx - 5.0f, INS_CY - 5.0f, zF1 + 0.5f, cx + 5.0f, INS_CY + 5.0f, zF1 + 0.5f, 0, 0, 16, 16);
        }
        return Collections.unmodifiableList(b.out);
    }

    /** A strap / chain that wraps front cover -> fore-edge -> back cover at height [ya, yb]. */
    private static void strap(Builder b, GrimoireSprite sprite, int tint, float ya, float yb, float th, float zB0, float zF1) {
        b.box(sprite, tint, true, CLASP_X0, ya, zF1, CLASP_X1, yb, zF1 + th, S | U | D | W, 0, 0, 16, 16);              // front run
        b.box(sprite, tint, true, X1, ya, zB0 - th, CLASP_X1, yb, zF1 + th, E | U | D, 0, 0, 16, 16);                    // over the fore-edge
        b.box(sprite, tint, true, CLASP_X0, ya, zB0 - th, CLASP_X1, yb, zB0, N | U | D | W, 0, 0, 16, 16);              // back run
    }

    // ------------------------------------------------------------------------------------------------------------------

    private static final class Builder {
        final List<PlannedQuad> out = new ArrayList<>(128);

        /** A box with the faces in {@code mask}; every face samples the same UV rectangle. Coordinates in px. */
        void box(GrimoireSprite sprite, int tint, boolean shade, float x0, float y0, float z0, float x1, float y1, float z1,
                 int mask, float u0, float v0, float u1, float v1) {
            if ((mask & S) != 0) face(Direction.SOUTH, sprite, tint, shade, false, x0, y0, z1, x1, y1, z1, u0, v0, u1, v1);
            if ((mask & N) != 0) face(Direction.NORTH, sprite, tint, shade, false, x0, y0, z0, x1, y1, z0, u0, v0, u1, v1);
            if ((mask & E) != 0) face(Direction.EAST, sprite, tint, shade, false, x1, y0, z0, x1, y1, z1, u0, v0, u1, v1);
            if ((mask & W) != 0) face(Direction.WEST, sprite, tint, shade, false, x0, y0, z0, x0, y1, z1, u0, v0, u1, v1);
            if ((mask & U) != 0) face(Direction.UP, sprite, tint, shade, false, x0, y1, z0, x1, y1, z1, u0, v0, u1, v1);
            if ((mask & D) != 0) face(Direction.DOWN, sprite, tint, shade, false, x0, y0, z0, x1, y0, z1, u0, v0, u1, v1);
        }

        /**
         * One face of the cuboid spanned by (x0,y0,z0)-(x1,y1,z1) (the face's own axis must be flat: both coordinates equal).
         * Corner order is bottom-left, bottom-right, top-right, top-left as seen from outside, which makes the winding
         * counter-clockwise and the normal point out.
         */
        void face(Direction dir, GrimoireSprite sprite, int tint, boolean shade, boolean emissive,
                  float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1, float v1) {
            float[] p = switch (dir) {
                case SOUTH -> new float[]{x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1};
                case NORTH -> new float[]{x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0};
                case EAST -> new float[]{x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1};
                case WEST -> new float[]{x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0};
                case UP -> new float[]{x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0};
                case DOWN -> new float[]{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1};
            };
            for (int i = 0; i < 12; i++) p[i] /= 16f;
            float[] uv = {u0, v1, u1, v1, u1, v0, u0, v0};
            out.add(new PlannedQuad(dir, sprite, tint, shade, emissive, p, uv));
        }
    }
}
