package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.fx.Piece;
import com.newuniverse.nusmp.vfx.fx.Vec;

/**
 * Builds a piece's white mesh once: x, y, z, u, v per vertex, in the piece's local space.
 * SPRITE: unit quad (billboarded at draw). DECAL: flat quad. CROSS / SHARD: two crossed quads rising along +Y
 * (SHARD tapers to a point). DOME: a hemisphere. RIBBON: crossed strips along the piece's path (effect-local).
 * Vertex counts match Piece.vertices(), so the headless budget check is exact.
 */
public final class FxMeshes {
    private FxMeshes() {}

    public static float[] build(Piece p) {
        float w = (float) p.width(), l = (float) p.length();
        return switch (p.mesh()) {
            case SPRITE -> new float[]{-w / 2, -w / 2, 0, 0, 1, w / 2, -w / 2, 0, 1, 1, w / 2, w / 2, 0, 1, 0, -w / 2, w / 2, 0, 0, 0};
            case DECAL -> new float[]{-l / 2, 0, -w / 2, 0, 1, l / 2, 0, -w / 2, 1, 1, l / 2, 0, w / 2, 1, 0, -l / 2, 0, w / 2, 0, 0};
            case CROSS -> cross(w / 2, w / 2, l);
            case SHARD -> cross(w / 2, w * 0.04f, l);
            case DOME -> dome(p.segments(), w, l);
            case RIBBON -> ribbon(p);
        };
    }

    private static float[] cross(float bottom, float top, float h) {
        return new float[]{
                -bottom, 0, 0, 0, 1, bottom, 0, 0, 1, 1, top, h, 0, 1, 0, -top, h, 0, 0, 0,
                0, 0, -bottom, 0, 1, 0, 0, bottom, 1, 1, 0, h, top, 1, 0, 0, h, -top, 0, 0};
    }

    private static float[] dome(int segments, float radius, float height) {
        int lat = 3, lon = Math.max(1, segments / lat);
        float[] out = new float[lat * lon * 20];
        int k = 0;
        for (int i = 0; i < lat; i++) for (int j = 0; j < lon; j++) {
            float a0 = (float) (Math.PI / 2 * i / lat), a1 = (float) (Math.PI / 2 * (i + 1) / lat);
            float b0 = (float) (Math.PI * 2 * j / lon), b1 = (float) (Math.PI * 2 * (j + 1) / lon);
            float[][] q = {{a0, b0}, {a0, b1}, {a1, b1}, {a1, b0}};
            for (float[] c : q) {
                out[k++] = (float) (Math.cos(c[0]) * Math.cos(c[1]) * radius);
                out[k++] = (float) (Math.sin(c[0]) * height);
                out[k++] = (float) (Math.cos(c[0]) * Math.sin(c[1]) * radius);
                out[k++] = c[1] / (float) (Math.PI * 2);
                out[k++] = 1 - c[0] / (float) (Math.PI / 2);
            }
        }
        return out;
    }

    private static float[] ribbon(Piece p) {
        int n = p.segments();
        float[] out = new float[n * 40];
        int k = 0;
        for (int i = 0; i < n; i++) {
            Vec a = p.path().get(i), b = p.path().get(i + 1), t = b.sub(a).norm();
            Vec ref = Math.abs(t.y()) > 0.9 ? new Vec(1, 0, 0) : Vec.UP;
            Vec s1 = t.cross(ref).norm(), s2 = t.cross(s1).norm();
            double w0 = p.width() * (1 - 0.6 * i / n) / 2, w1 = p.width() * (1 - 0.6 * (i + 1) / n) / 2;
            float u0 = (float) i / n, u1 = (float) (i + 1) / n;
            for (Vec s : new Vec[]{s1, s2}) {
                Vec[] v = {a.sub(s.mul(w0)), a.add(s.mul(w0)), b.add(s.mul(w1)), b.sub(s.mul(w1))};
                float[][] uv = {{u0, 0}, {u0, 1}, {u1, 1}, {u1, 0}};
                for (int j = 0; j < 4; j++) {
                    out[k++] = (float) v[j].x(); out[k++] = (float) v[j].y(); out[k++] = (float) v[j].z();
                    out[k++] = uv[j][0]; out[k++] = uv[j][1];
                }
            }
        }
        return out;
    }
}
