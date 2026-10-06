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

    /**
     * One quad: 4 corners (x,y,z in 0..16, outward winding), UVs (0..16 of the texture), texture name, tint layer (-1 none), the
     * nearest axis it faces and its exact outward normal (differs from the axis only on the open book's turned halves).
     */
    public record Quad(float[] pos, float[] uv, String texture, int tint, boolean emissive, Face face, float[] normal) {
        public Quad(float[] pos, float[] uv, String texture, int tint, boolean emissive, Face face) {
            this(pos, uv, texture, tint, emissive, face, axis(face));
        }
    }

    /** Unit normal of an axis face. */
    public static float[] axis(Face f) {
        return switch (f) {
            case DOWN -> new float[]{0, -1, 0}; case UP -> new float[]{0, 1, 0}; case NORTH -> new float[]{0, 0, -1};
            case SOUTH -> new float[]{0, 0, 1}; case WEST -> new float[]{-1, 0, 0}; case EAST -> new float[]{1, 0, 0};
        };
    }

    /** The axis face closest to a normal. */
    public static Face nearest(float nx, float ny, float nz) {
        float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        if (ay >= ax && ay >= az) return ny > 0 ? Face.UP : Face.DOWN;
        if (ax >= az) return nx > 0 ? Face.EAST : Face.WEST;
        return nz > 0 ? Face.SOUTH : Face.NORTH;
    }

    // layout (model units)
    static final float BX0 = 2.5f, BX1 = 13.5f, BY0 = 1.3f, BY1 = 14.7f;            // boards
    static final float BACK0 = 6.6f, BACK1 = 7.1f, FRONT0 = 8.9f, FRONT1 = 9.4f;           // 0.36: ultra-thin (was 5.5 / 6.3 / 9.7 / 10.5)
    static final float PX0 = 2.7f, PX1 = 13.2f, PY0 = 1.6f, PY1 = 14.4f;            // pages
    static final float SX0 = 1.9f, SX1 = 2.7f, SY0 = 1.1f, SY1 = 14.9f, SZ0 = 6.5f, SZ1 = 9.5f;    // spine (0.36: slim)
    static final float BAR = 0.45f, BAR_INSET = 0.4f, BAR_H = 0.3f;                 // raised frame bars
    static final float MED = 2.0f, MED_H = 0.35f, EMB = 1.6f;                       // medallion half-size, height; emblem half-size

    private GrimoireBookPlan() {}

    public static List<Quad> build(BookLook.Key key) {
        if (key.open() > 0) return buildOpen(key);
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
        design(out, Face.SOUTH, BX0, BY0, BX1, BY1, FRONT1, 1, key);
        box(out, 8 - MED, 8 - MED, FRONT1, 8 + MED, 8 + MED, FRONT1 + MED_H, "trim_metal", trim, false, false, new Face[]{Face.UP, Face.DOWN, Face.WEST, Face.EAST});
        quad(out, Face.SOUTH, 8 - MED, 8 - MED, 8 + MED, 8 + MED, FRONT1 + MED_H, "medallion", trim, false);
        quad(out, Face.SOUTH, 8 - EMB, 8 - EMB, 8 + EMB, 8 + EMB, FRONT1 + MED_H + 0.02f, "emblem_" + key.emblem().toLowerCase(), BookLook.TINT_EMBLEM, held);

        // back cover: the same frame and ornament, a small rosette
        if (!key.tattered()) frame(out, BACK0, -BAR_H, trim);
        design(out, Face.NORTH, BX0, BY0, BX1, BY1, BACK0, -1, key);
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

    /**
     * The cover design over a whole cover face at depth {@code z} ({@code dir} +1 = outwards along +Z, -1 along -Z). An art-pack
     * design is two layers: its background shading tinted to the cover colour, then its ornament in the art's own colours, which
     * glows while the book is held. The canon specials (tattered, straps) are one trim-tinted overlay as before.
     */
    private static void design(List<Quad> out, Face face, float x0, float y0, float x1, float y1, float z, int dir, BookLook.Key key) {
        BookMotif m = key.motif();
        if (m.isArt()) {
            quad(out, face, x0, y0, x1, y1, z + dir * 0.015f, m.base(), BookLook.TINT_COVER, false);
            quad(out, face, x0, y0, x1, y1, z + dir * 0.03f, m.texture(), -1, key.held());
        } else {
            quad(out, face, x0, y0, x1, y1, z + dir * 0.02f, m.texture(), BookLook.TINT_TRIM, false);
        }
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

    // ---------------------------------------------------------------- the summoned book, open in a V
    /** Opening steps (Key.open 1..3): degrees each half is turned back from lying flat; 34 is the fully open V of the reference renders. */
    public static final int OPEN_STEPS = 3;
    public static final float[] OPEN_DEGREES = {90f, 55f, 30f, 14f};       // 0.36: opens almost flat (was 62 / 46 / 34)
    /** Spine axis (vertical) at (OPEN_CX, OPEN_CZ); each half is OPEN_W wide; boards OPEN_T thick; page blocks OPEN_P thick. */
    public static final float OPEN_CX = 8f, OPEN_CZ = 11f, OPEN_W = 7.6f, OPEN_T = 0.45f, OPEN_P = 0.9f;   // 0.36: thin boards and page blocks
    /** Where loose pages turn (page flip): on the spine axis, at the page surface. */
    public static final float OPEN_HINGE_Z = OPEN_CZ - OPEN_T - OPEN_P - 0.6f;

    /**
     * The open book: two halves hinged on a vertical spine at the front, each turned back by {@link #OPEN_DEGREES}, outer covers
     * facing out (+Z, to onlookers) and the glowing page blocks facing the reader behind it (-Z): the right half carries the front
     * cover (frame, ornament, medallion, emblem), the left half the back cover (frame, ornament, rosette).
     */
    static List<Quad> buildOpen(BookLook.Key key) {
        List<Quad> out = new ArrayList<>(140);
        boolean held = key.held();
        String leather = key.tattered() ? "cover_tattered" : "cover_leather";
        int cover = BookLook.TINT_COVER, trim = BookLook.TINT_TRIM;
        float th = (float) Math.toRadians(OPEN_DEGREES[Math.max(1, Math.min(OPEN_STEPS, key.open()))]);
        float cx = OPEN_CX, cz = OPEN_CZ, w = OPEN_W, t = OPEN_T, p = OPEN_P;
        // page blocks start far enough from the spine that the two halves' pages do not cross
        float u0 = Math.max(0.6f, Math.min(3.0f, (0.05f + (t + p) * (float) Math.sin(th)) / (float) Math.cos(th)));
        for (int side : new int[]{1, -1}) {
            List<Quad> half = new ArrayList<>(70);
            float xa = side > 0 ? cx + 0.3f : cx - w, xb = side > 0 ? cx + w : cx - 0.3f;
            box(half, xa, BY0, cz - t, xb, BY1, cz, leather, cover, false, true, ALL);
            float pa = side > 0 ? cx + u0 : cx - w + 0.3f, pb = side > 0 ? cx + w - 0.3f : cx - u0;
            box(half, pa, PY0, cz - t - p, pb, PY1, cz - t, "pages", -1, held, true,
                    new Face[]{Face.UP, Face.DOWN, Face.NORTH, side > 0 ? Face.EAST : Face.WEST});
            // 0.36: glowing spell runes on the open pages while the book is held (cast state)
            if (held) quad(half, Face.NORTH, pa + 0.5f, PY0 + 0.6f, pb - 0.5f, PY1 - 0.6f, cz - t - p - 0.03f, "page_runes", BookLook.TINT_TRIM, true);
            if (!key.tattered()) {
                float x0 = xa + BAR_INSET, x1 = xb - BAR_INSET, y0 = BY0 + BAR_INSET, y1 = BY1 - BAR_INSET, z0 = cz, z1 = cz + BAR_H;
                box(half, x0, y0, z0, x0 + BAR, y1, z1, "trim_metal", trim, false, true, BAR_FACES_FRONT);
                box(half, x1 - BAR, y0, z0, x1, y1, z1, "trim_metal", trim, false, true, BAR_FACES_FRONT);
                box(half, x0 + BAR, y1 - BAR, z0, x1 - BAR, y1, z1, "trim_metal", trim, false, true, BAR_FACES_FRONT);
                box(half, x0 + BAR, y0, z0, x1 - BAR, y0 + BAR, z1, "trim_metal", trim, false, true, BAR_FACES_FRONT);
            }
            design(half, Face.SOUTH, xa, BY0, xb, BY1, cz, 1, key);
            float mx = (xa + xb) / 2, my = 8f, med = side > 0 ? MED * 0.8f : MED * 0.45f;
            box(half, mx - med, my - med, cz, mx + med, my + med, cz + MED_H, "trim_metal", trim, false, false, new Face[]{Face.UP, Face.DOWN, Face.WEST, Face.EAST});
            quad(half, Face.SOUTH, mx - med, my - med, mx + med, my + med, cz + MED_H, "medallion", trim, false);
            if (side > 0) {
                float e = EMB * 0.8f;
                quad(half, Face.SOUTH, mx - e, my - e, mx + e, my + e, cz + MED_H + 0.02f, "emblem_" + key.emblem().toLowerCase(), BookLook.TINT_EMBLEM, held);
            }
            for (Quad q : half) {
                Quad turned = turn(q, cx, cz, side * th);
                out.add(turned);
                out.add(backFace(turned));          // double-sided: no renderer or viewing angle can cull a half away (0.27)
            }
        }
        // the spine, bulging towards the onlookers, with three metal bands
        box(out, cx - 1.0f, SY0, cz - t - 0.3f, cx + 1.0f, SY1, cz + 0.5f, "spine", cover, false, true, ALL);
        for (float y : new float[]{3.95f, 8.0f, 12.05f})
            box(out, cx - 0.8f, y - 0.25f, cz + 0.5f, cx + 0.8f, y + 0.25f, cz + 0.75f, "trim_metal", trim, false, true, ALL);
        return Collections.unmodifiableList(out);
    }

    /** The same quad seen from behind: corners in reverse order, normal flipped. */
    static Quad backFace(Quad q) {
        float[] p = q.pos(), uv = q.uv();
        float[] rp = new float[12], ruv = new float[8];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(p, (3 - i) * 3, rp, i * 3, 3);
            System.arraycopy(uv, (3 - i) * 2, ruv, i * 2, 2);
        }
        float[] n = q.normal();
        return new Quad(rp, ruv, q.texture(), q.tint(), q.emissive(), nearest(-n[0], -n[1], -n[2]), new float[]{-n[0], -n[1], -n[2]});
    }

    /** Turns a quad about the vertical axis through (cx, cz) by {@code a} radians (positive swings +X towards -Z). */
    static Quad turn(Quad q, float cx, float cz, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float[] p = q.pos().clone();
        for (int i = 0; i < 4; i++) {
            float dx = p[i * 3] - cx, dz = p[i * 3 + 2] - cz;
            p[i * 3] = cx + dx * c + dz * s;
            p[i * 3 + 2] = cz - dx * s + dz * c;
        }
        float[] n = q.normal();
        float nx = n[0] * c + n[2] * s, nz = -n[0] * s + n[2] * c;
        return new Quad(p, q.uv(), q.texture(), q.tint(), q.emissive(), nearest(nx, n[1], nz), new float[]{nx, n[1], nz});
    }

    /** Every texture the plan can use (for the model loader to resolve). */
    public static List<String> textures() {
        List<String> t = new ArrayList<>(List.of("cover_leather", "cover_tattered", "pages", "spine", "trim_metal", "medallion", "page_runes"));
        for (BookMotif m : BookMotif.values()) {
            t.add(m.texture());
            if (m.base() != null) t.add(m.base());
        }
        for (String e : new String[]{"three_leaf", "four_leaf", "five_leaf", "spade", "double_spade", "triple_spade",
                "heart", "two_heart", "cracked_heart", "diamond", "five_sided", "cracked_diamond", "black_magic", "god_tier"}) t.add("emblem_" + e);
        return t;
    }
}
