package com.newuniverse.nusmp.grimoire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The grimoire's 3D geometry as plain data (item model units 0..16, front cover facing +Z / SOUTH, spine on the -X side), built
 * after the reference art: thick boards, a cream page block, a rounded banded spine, a raised double frame on both covers, the
 * motif ornament stamped on each cover, a raised medallion carrying the emblem on the front and a rosette on the back.
 * While the book is held its pages and emblem glow (fullbright). Pure: the client turns each quad into a BakedQuad.
 */
public final class GrimoireBookPlan {
    public enum Face { DOWN, UP, NORTH, SOUTH, WEST, EAST }

    /** One quad: 4 corners (x,y,z in 0..16, outward winding), UVs (0..16 of the texture), texture name, tint layer (-1 none). */
    public record Quad(float[] pos, float[] uv, String texture, int tint, boolean emissive, Face face) {}

    // layout (model units)
    static final float BX0 = 2.5f, BX1 = 13.5f, BY0 = 1.3f, BY1 = 14.7f;            // boards
    static final float BACK0 = 5.5f, BACK1 = 6.3f, FRONT0 = 9.7f, FRONT1 = 10.5f;
    static final float PX0 = 2.7f, PX1 = 13.2f, PY0 = 1.6f, PY1 = 14.4f;            // pages
    static final float SX0 = 1.9f, SX1 = 2.7f, SY0 = 1.1f, SY1 = 14.9f, SZ0 = 5.4f, SZ1 = 10.6f;   // spine
    static final float BAR = 0.45f, BAR_INSET = 0.4f, BAR_H = 0.3f;                 // raised frame bars
    static final float MED = 2.0f, MED_H = 0.35f, EMB = 1.6f;                       // medallion half-size, height; emblem half-size

    private GrimoireBookPlan() {}

    public static List<Quad> build(BookLook.Key key) {
        List<Quad> out = new ArrayList<>(110);
        boolean held = key.held();
        String leather = key.tattered() ? "cover_tattered" : "cover_leather";
        int cover = BookLook.TINT_COVER, trim = BookLook.TINT_TRIM;

        // boards and spine (leather), pages (cream, glowing while held)
        box(out, BX0, BY0, FRONT0, BX1, BY1, FRONT1, leather, cover, false, true, ALL);
        box(out, BX0, BY0, BACK0, BX1, BY1, BACK1, leather, cover, false, true, ALL);
        box(out, PX0, PY0, BACK1, PX1, PY1, FRONT0, "pages", -1, held, true, new Face[]{Face.UP, Face.DOWN, Face.EAST});
        box(out, SX0, SY0, SZ0, SX1, SY1, SZ1, "spine", cover, false, true, ALL);
        for (float y : new float[]{3.95f, 8.0f, 12.05f}) box(out, SX0 - 0.2f, y - 0.25f, SZ0 + 0.2f, SX0 + 0.05f, y + 0.25f, SZ1 - 0.2f, "trim_metal", trim, false, true, ALL);

        // front cover: raised double frame (outer bars; the inner line is part of the ornament texture), ornament, medallion, emblem
        if (!key.tattered()) frame(out, FRONT1, BAR_H, trim);
        overlay(out, Face.SOUTH, FRONT1 + 0.02f, key.motif().texture(), trim, false);
        box(out, 8 - MED, 8 - MED, FRONT1, 8 + MED, 8 + MED, FRONT1 + MED_H, "trim_metal", trim, false, false, new Face[]{Face.UP, Face.DOWN, Face.WEST, Face.EAST});
        quad(out, Face.SOUTH, 8 - MED, 8 - MED, 8 + MED, 8 + MED, FRONT1 + MED_H, "medallion", trim, false);
        quad(out, Face.SOUTH, 8 - EMB, 8 - EMB, 8 + EMB, 8 + EMB, FRONT1 + MED_H + 0.02f, "emblem_" + key.emblem().toLowerCase(), BookLook.TINT_EMBLEM, held);

        // back cover: the same frame and ornament, a small rosette
        if (!key.tattered()) frame(out, BACK0, -BAR_H, trim);
        overlay(out, Face.NORTH, BACK0 - 0.02f, key.motif().texture(), trim, false);
        float r = MED * 0.55f;
        box(out, 8 - r, 8 - r, BACK0 - MED_H * 0.8f, 8 + r, 8 + r, BACK0, "trim_metal", trim, false, false, new Face[]{Face.UP, Face.DOWN, Face.WEST, Face.EAST});
        quad(out, Face.NORTH, 8 - r, 8 - r, 8 + r, 8 + r, BACK0 - MED_H * 0.8f, "medallion", trim, false);
        return Collections.unmodifiableList(out);
    }

    private static final Face[] ALL = Face.values();
    private static final Face[] BAR_FACES_FRONT = {Face.UP, Face.DOWN, Face.WEST, Face.EAST, Face.SOUTH};
    private static final Face[] BAR_FACES_BACK = {Face.UP, Face.DOWN, Face.WEST, Face.EAST, Face.NORTH};

    /** Four raised bars just inside the cover edge. h > 0 rises from the front face (z), h < 0 from the back face. */
    private static void frame(List<Quad> out, float z, float h, int tint) {
        float z0 = Math.min(z, z + h), z1 = Math.max(z, z + h);
        Face[] faces = h > 0 ? BAR_FACES_FRONT : BAR_FACES_BACK;
        float x0 = BX0 + BAR_INSET, x1 = BX1 - BAR_INSET, y0 = BY0 + BAR_INSET, y1 = BY1 - BAR_INSET;
        box(out, x0, y0, z0, x0 + BAR, y1, z1, "trim_metal", tint, false, true, faces);                 // spine side
        box(out, x1 - BAR, y0, z0, x1, y1, z1, "trim_metal", tint, false, true, faces);                 // fore edge
        box(out, x0 + BAR, y1 - BAR, z0, x1 - BAR, y1, z1, "trim_metal", tint, false, true, faces);     // top
        box(out, x0 + BAR, y0, z0, x1 - BAR, y0 + BAR, z1, "trim_metal", tint, false, true, faces);     // bottom
    }

    /** The motif texture stamped over a whole cover face. */
    private static void overlay(List<Quad> out, Face face, float z, String texture, int tint, boolean emissive) {
        quad(out, face, BX0, BY0, BX1, BY1, z, texture, tint, emissive);
    }

    /** A single quad facing SOUTH or NORTH at depth z over [x0,x1] x [y0,y1], full texture. */
    private static void quad(List<Quad> out, Face face, float x0, float y0, float x1, float y1, float z, String texture, int tint, boolean emissive) {
        float[] pos = corners(face, x0, y0, z, x1, y1, z);
        out.add(new Quad(pos, new float[]{0, 0, 0, 16, 16, 16, 16, 0}, texture, tint, emissive, face));
    }

    /** A box; each listed face is textured over its own size (leather and metal tile naturally). */
    private static void box(List<Quad> out, float x0, float y0, float z0, float x1, float y1, float z1, String texture, int tint,
                            boolean emissive, boolean scaleUv, Face[] faces) {
        for (Face f : faces) {
            float[] pos = corners(f, x0, y0, z0, x1, y1, z1);
            float w, h;
            switch (f) {
                case NORTH, SOUTH -> { w = x1 - x0; h = y1 - y0; }
                case EAST, WEST -> { w = z1 - z0; h = y1 - y0; }
                default -> { w = x1 - x0; h = z1 - z0; }
            }
            float u1 = scaleUv ? Math.min(16, w) : 16, v1 = scaleUv ? Math.min(16, h) : 16;
            out.add(new Quad(pos, new float[]{0, 0, 0, v1, u1, v1, u1, 0}, texture, tint, emissive, f));
        }
    }

    /** Corner order per face as vanilla's FaceInfo: counter-clockwise seen from outside, starting top-left. */
    static float[] corners(Face f, float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (f) {
            case DOWN -> new float[]{x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1};
            case UP -> new float[]{x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0};
            case NORTH -> new float[]{x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0};
            case SOUTH -> new float[]{x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1};
            case WEST -> new float[]{x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1};
            case EAST -> new float[]{x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0};
        };
    }

    /** Every texture the plan can use (for the model loader to resolve). */
    public static List<String> textures() {
        List<String> t = new ArrayList<>(List.of("cover_leather", "cover_tattered", "pages", "spine", "trim_metal", "medallion"));
        for (BookMotif m : BookMotif.values()) t.add(m.texture());
        for (String e : new String[]{"three_leaf", "four_leaf", "five_leaf", "spade", "double_spade", "triple_spade",
                "heart", "two_heart", "cracked_heart", "diamond", "five_sided", "cracked_diamond"}) t.add("emblem_" + e);
        return t;
    }
}
