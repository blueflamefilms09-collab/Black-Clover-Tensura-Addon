package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.relics.djinn.Djinn.Look;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The silhouette of a Djinn as the medallion shows it: a bust with the Djinn's horns, ears and, in four beats of a wing, its wings, rasterised ONCE
 * from the Djinn's {@link Look} into horizontal runs of 1 pixel cells (a 60 x 60 grid, clipped to the disc of the medallion). The tail and the halo
 * move, so the painter draws them live; everything else is replayed as runs.
 *
 * <p>Pure geometry (polygon fills of polylines), nothing here knows a Djinn by name: any combination of horns, wings and ears yields a shape
 * (tested for the whole product of the Look enums). A run is packed into an int, see {@link #x}, {@link #y}, {@link #len}, {@link #part}, {@link #edge}.
 */
public final class Emblem {
    public static final int GW = 60, GH = 60, FRAMES = 4;
    public static final int P_BODY = 1, P_HORN = 2, P_EAR = 3, P_WING = 4, P_WING2 = 5;
    /** The head's centre in the grid, and the root of the tail. */
    public static final int HEAD_X = 30, HEAD_Y = 25;
    public static final float DISC_R = 29.0f;

    /** The static part (ears, horns, body: drawn after the wings). */
    public final int[] body;
    public final int bodyRuns;
    /** One run list per beat of the wing (0 = raised, FRAMES-1 = lowered). */
    public final int[][] wings;
    public final int[] wingRuns;

    private Emblem(int[] body, int bodyRuns, int[][] wings, int[] wingRuns) {
        this.body = body;
        this.bodyRuns = bodyRuns;
        this.wings = wings;
        this.wingRuns = wingRuns;
    }

    private record Key(Look.Horns horns, Look.Wings wings, boolean ears) {}

    private static final Map<Key, Emblem> CACHE = new ConcurrentHashMap<>();

    /** The emblem of a look (built on first use, then shared; nothing in it is ever modified). */
    public static Emblem of(Look look) {
        Key k = look == null ? new Key(Look.Horns.NONE, Look.Wings.NONE, false) : new Key(look.horns(), look.wings(), look.ears());
        return CACHE.computeIfAbsent(k, Emblem::build);
    }

    // ---------------------------------------------------------------- run packing
    public static int x(int r) { return r & 63; }
    public static int y(int r) { return (r >> 6) & 63; }
    public static int len(int r) { return (r >> 12) & 63; }
    public static int part(int r) { return (r >> 18) & 15; }
    public static int edge(int r) { return (r >> 22) & 1; }

    private static int pack(int x, int y, int len, int part, int edge) { return x | (y << 6) | (len << 12) | (part << 18) | (edge << 22); }

    // ---------------------------------------------------------------- build
    private static Emblem build(Key key) {
        byte[] g = new byte[GW * GH];
        int[][] wings = new int[FRAMES][];
        int[] wn = new int[FRAMES];
        for (int f = 0; f < FRAMES; f++) {
            Arrays.fill(g, (byte) 0);
            float lift = (float) Math.toRadians(62.0 - 70.0 * f / (FRAMES - 1));
            drawWings(g, key.wings(), lift, f);
            int[] runs = encode(g);
            wings[f] = runs;
            wn[f] = runs.length;
        }
        Arrays.fill(g, (byte) 0);
        if (key.ears()) {
            rel(g, new float[]{6, -1, 14, -7, 6.5f, 3}, P_EAR, true);
        }
        drawHorns(g, key.horns());
        drawBody(g);
        int[] body = encode(g);
        return new Emblem(body, body.length, wings, wn);
    }

    private static void drawBody(byte[] g) {
        float[] head = new float[32];
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            head[i * 2] = HEAD_X + (float) (Math.cos(a) * 6.5);
            head[i * 2 + 1] = HEAD_Y + (float) (Math.sin(a) * 7.5);
        }
        abs(g, head, P_BODY, false);
        abs(g, new float[]{HEAD_X - 3, HEAD_Y + 5, HEAD_X + 3, HEAD_Y + 5, HEAD_X + 4, 38, HEAD_X - 4, 38}, P_BODY, false);
        abs(g, new float[]{HEAD_X - 19, 60, HEAD_X - 16, 46, HEAD_X - 9, 39.5f, HEAD_X - 3, 37.5f, HEAD_X, 37, HEAD_X + 3, 37.5f, HEAD_X + 9, 39.5f,
                HEAD_X + 16, 46, HEAD_X + 19, 60}, P_BODY, false);
    }

    /** Horns as tapered strips along smooth lines, relative to the head centre, mirrored to the other side (the crown is one symmetric piece). */
    private static void drawHorns(byte[] g, Look.Horns h) {
        switch (h) {
            case CRESCENT -> horn(g, new float[]{4, -6, 9, -9, 13, -14, 13, -20, 10, -24}, 3.4f, 0.4f);
            case RAM -> horn(g, new float[]{3, -6, 8, -8.5f, 12.5f, -5.5f, 13.5f, -0.5f, 10.5f, 3, 7, 1.5f, 7, -2.5f}, 3.6f, 0.9f);
            case STAG -> {
                horn(g, new float[]{3, -7, 7, -12, 9, -19, 8, -24}, 2.6f, 0.5f);
                horn(g, new float[]{6.2f, -12.5f, 11, -14, 14, -19}, 1.8f, 0.4f);
                horn(g, new float[]{8.4f, -18.5f, 12, -22.5f}, 1.5f, 0.4f);
                horn(g, new float[]{4.6f, -9, 9.5f, -9.5f, 12.5f, -13}, 1.7f, 0.4f);
            }
            case SPIKE -> horn(g, new float[]{3, -7, 8, -13, 13, -22}, 3.2f, 0.2f);
            case CROWN -> {
                float[] xs = {-9, -4.5f, 0, 4.5f, 9}, hs = {7, 10, 13, 10, 7};
                for (int i = 0; i < 5; i++)
                    rel(g, new float[]{xs[i] - 2.2f, -6, xs[i], -6 - hs[i], xs[i] + 2.2f, -6}, P_HORN, false);
                rel(g, new float[]{-10.5f, -9, 10.5f, -9, 10.5f, -5.5f, -10.5f, -5.5f}, P_HORN, false);
            }
            default -> { }
        }
    }

    private static void horn(byte[] g, float[] line, float w0, float w1) {
        float[] sm = smooth(line, 5);
        rel(g, strip(sm, w0, w1), P_HORN, true);
    }

    private static void drawWings(byte[] g, Look.Wings kind, float lift, int frame) {
        float ax = HEAD_X + 8, ay = 42;
        switch (kind) {
            case FEATHER -> {
                for (int j = 0; j < 6; j++) {
                    float a = lift + (float) Math.toRadians((j - 2.5) * 11.0), len = 19 + 2.2f * j;
                    leaf(g, ax, ay, a, len, 4.8f, j % 2 == 0 ? P_WING : P_WING2);
                }
            }
            case BLADE -> {
                for (int j = 0; j < 4; j++) {
                    float a = lift + (float) Math.toRadians((j - 1.5) * 17.0), len = 33 - 3.2f * j;
                    blade(g, ax, ay, a, len, 3.6f, j % 2 == 0 ? P_WING : P_WING2);
                }
            }
            case FIRE -> {
                for (int j = 0; j < 5; j++) {
                    float a = lift + (float) Math.toRadians((j - 2) * 16.0), len = 27 - Math.abs(j - 2) * 4f;
                    float bend = (float) Math.sin(frame * 1.9 + j * 1.3) * 2.6f;
                    flame(g, ax, ay, a, len, 6.5f, bend, P_WING);
                    flame(g, ax, ay, a, len * 0.62f, 3.2f, bend * 0.5f, P_WING2);
                }
            }
            case MEMBRANE -> membrane(g, ax, ay, lift, true);
            case VEIL -> membrane(g, ax, ay, lift, false);
            default -> { }
        }
    }

    private static void leaf(byte[] g, float ax, float ay, float a, float len, float w, int part) {
        float dx = (float) Math.cos(a), dy = -(float) Math.sin(a), px = -dy, py = dx;
        float mx = ax + dx * len * 0.55f, my = ay + dy * len * 0.55f, tx = ax + dx * len, ty = ay + dy * len;
        abs(g, new float[]{ax + px * 0.7f, ay + py * 0.7f, mx + px * w / 2, my + py * w / 2, tx, ty, mx - px * w / 2, my - py * w / 2, ax - px * 0.7f, ay - py * 0.7f}, part, true);
    }

    private static void blade(byte[] g, float ax, float ay, float a, float len, float w, int part) {
        float dx = (float) Math.cos(a), dy = -(float) Math.sin(a), px = -dy, py = dx;
        float mx = ax + dx * len * 0.72f, my = ay + dy * len * 0.72f, tx = ax + dx * len, ty = ay + dy * len;
        abs(g, new float[]{ax + px * w / 2, ay + py * w / 2, mx + px * w * 0.42f, my + py * w * 0.42f, tx, ty, mx - px * w * 0.5f, my - py * w * 0.5f,
                ax - px * w / 2, ay - py * w / 2}, part, true);
    }

    private static void flame(byte[] g, float ax, float ay, float a, float len, float w, float bend, int part) {
        float dx = (float) Math.cos(a), dy = -(float) Math.sin(a), px = -dy, py = dx;
        float mx = ax + dx * len * 0.5f, my = ay + dy * len * 0.5f, tx = ax + dx * len + px * bend, ty = ay + dy * len + py * bend;
        abs(g, new float[]{ax + px * w * 0.45f, ay + py * w * 0.45f, mx + px * (w / 2 + bend * 0.4f), my + py * (w / 2 + bend * 0.4f), tx, ty,
                mx - px * (w / 2 - bend * 0.4f), my - py * (w / 2 - bend * 0.4f), ax - px * w * 0.45f, ay - py * w * 0.45f}, part, true);
    }

    /** A bat wing (arm, three fingers, scalloped skin) or, without bones, a wide soft veil with five trailing tips. */
    private static void membrane(byte[] g, float ax, float ay, float lift, boolean bones) {
        int n = bones ? 3 : 5;
        float[] tipX = new float[n], tipY = new float[n];
        float wx = ax, wy = ay;
        if (bones) {
            float a = lift + (float) Math.toRadians(22);
            wx = ax + (float) Math.cos(a) * 18;
            wy = ay - (float) Math.sin(a) * 18;
            for (int k = 0; k < n; k++) {
                float fa = lift + (float) Math.toRadians(22 + (k - 1) * 34.0), fl = 21 - k * 2f;
                tipX[k] = wx + (float) Math.cos(fa) * fl;
                tipY[k] = wy - (float) Math.sin(fa) * fl;
            }
        } else {
            for (int k = 0; k < n; k++) {
                float fa = lift + (float) Math.toRadians((k - 2) * 15.0), fl = 31 - Math.abs(k - 2) * 3f;
                tipX[k] = ax + (float) Math.cos(fa) * fl;
                tipY[k] = ay - (float) Math.sin(fa) * fl;
            }
        }
        float pull = bones ? 0.38f : 0.2f;
        float[] pts = new float[(2 + n + (n - 1) + 1) * 2];
        int i = 0;
        pts[i++] = ax; pts[i++] = ay;
        if (bones) { pts[i++] = wx; pts[i++] = wy; } else { pts[i++] = ax + 1; pts[i++] = ay - 3; }
        for (int k = 0; k < n; k++) {
            pts[i++] = tipX[k]; pts[i++] = tipY[k];
            if (k < n - 1) {
                float mx = (tipX[k] + tipX[k + 1]) / 2, my = (tipY[k] + tipY[k + 1]) / 2;
                pts[i++] = mx + (ax - mx) * pull + (bones ? (wx - mx) * 0.2f : 0f);
                pts[i++] = my + (ay - my) * pull + (bones ? (wy - my) * 0.2f : 0f);
            }
        }
        pts[i++] = ax - 2; pts[i++] = ay + 10;
        abs(g, pts, P_WING, true);
        if (bones) {
            abs(g, strip(new float[]{ax, ay, wx, wy}, 1.8f, 1.4f), P_WING2, true);
            for (int k = 0; k < n; k++) abs(g, strip(new float[]{wx, wy, tipX[k], tipY[k]}, 1.4f, 0.6f), P_WING2, true);
        } else {
            for (int k = 1; k < n - 1; k++) abs(g, strip(new float[]{ax, ay, ax + (tipX[k] - ax) * 0.8f, ay + (tipY[k] - ay) * 0.8f}, 1.2f, 0.5f), P_WING2, true);
        }
    }

    // ---------------------------------------------------------------- geometry
    /** Catmull-Rom through the points, {@code sub} steps a segment. */
    static float[] smooth(float[] p, int sub) {
        int n = p.length / 2;
        if (n < 3) return p;
        float[] out = new float[((n - 1) * sub + 1) * 2];
        int o = 0;
        for (int i = 0; i < n - 1; i++) {
            float x0 = p[Math.max(0, i - 1) * 2], y0 = p[Math.max(0, i - 1) * 2 + 1], x1 = p[i * 2], y1 = p[i * 2 + 1];
            float x2 = p[(i + 1) * 2], y2 = p[(i + 1) * 2 + 1], x3 = p[Math.min(n - 1, i + 2) * 2], y3 = p[Math.min(n - 1, i + 2) * 2 + 1];
            for (int s = 0; s < sub; s++) {
                float t = s / (float) sub, t2 = t * t, t3 = t2 * t;
                out[o++] = 0.5f * ((2 * x1) + (-x0 + x2) * t + (2 * x0 - 5 * x1 + 4 * x2 - x3) * t2 + (-x0 + 3 * x1 - 3 * x2 + x3) * t3);
                out[o++] = 0.5f * ((2 * y1) + (-y0 + y2) * t + (2 * y0 - 5 * y1 + 4 * y2 - y3) * t2 + (-y0 + 3 * y1 - 3 * y2 + y3) * t3);
            }
        }
        out[o++] = p[(n - 1) * 2];
        out[o] = p[(n - 1) * 2 + 1];
        return out;
    }

    /** A closed outline round a polyline, wide w0 at the start and w1 at the end. */
    static float[] strip(float[] line, float w0, float w1) {
        int n = line.length / 2;
        float[] out = new float[n * 4];
        for (int i = 0; i < n; i++) {
            float tx = line[Math.min(n - 1, i + 1) * 2] - line[Math.max(0, i - 1) * 2], ty = line[Math.min(n - 1, i + 1) * 2 + 1] - line[Math.max(0, i - 1) * 2 + 1];
            float l = (float) Math.sqrt(tx * tx + ty * ty);
            if (l < 1e-4f) { tx = 1; ty = 0; l = 1; }
            float nx = -ty / l, ny = tx / l, w = (w0 + (w1 - w0) * i / Math.max(1, n - 1)) / 2;
            out[i * 2] = line[i * 2] + nx * w;
            out[i * 2 + 1] = line[i * 2 + 1] + ny * w;
            out[(2 * n - 1 - i) * 2] = line[i * 2] - nx * w;
            out[(2 * n - 1 - i) * 2 + 1] = line[i * 2 + 1] - ny * w;
        }
        return out;
    }

    /** Fills a polygon whose points are relative to the head centre, and (when {@code mirror}) its reflection on the other side. */
    private static void rel(byte[] g, float[] pts, int part, boolean mirror) {
        float[] p = pts.clone();
        for (int i = 0; i < p.length; i += 2) { p[i] += HEAD_X; p[i + 1] += HEAD_Y; }
        abs(g, p, part, mirror);
    }

    /** Fills a polygon in grid coordinates, and (when {@code mirror}) its reflection on the other side of the head's axis. */
    private static void abs(byte[] g, float[] p, int part, boolean mirror) {
        fill(g, p, part);
        if (mirror) {
            float[] m = new float[p.length];
            for (int i = 0; i < p.length; i += 2) { m[i] = 2 * HEAD_X - p[i]; m[i + 1] = p[i + 1]; }
            fill(g, m, part);
        }
    }

    private static void fill(byte[] g, float[] p, int part) {
        int n = p.length / 2;
        float[] xs = new float[n];
        for (int y = 0; y < GH; y++) {
            float yc = y + 0.5f;
            int c = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float y1 = p[i * 2 + 1], y2 = p[j * 2 + 1];
                if ((y1 <= yc && y2 > yc) || (y2 <= yc && y1 > yc)) xs[c++] = p[i * 2] + (yc - y1) / (y2 - y1) * (p[j * 2] - p[i * 2]);
            }
            Arrays.sort(xs, 0, c);
            for (int k = 0; k + 1 < c; k += 2) {
                int xa = Math.max(0, (int) Math.ceil(xs[k] - 0.5f)), xb = Math.min(GW - 1, (int) Math.ceil(xs[k + 1] - 0.5f) - 1);
                for (int x = xa; x <= xb; x++) g[y * GW + x] = (byte) part;
            }
        }
    }

    private static boolean inDisc(int x, int y) {
        float dx = x + 0.5f - GW / 2f, dy = y + 0.5f - GH / 2f;
        return dx * dx + dy * dy <= DISC_R * DISC_R;
    }

    private static int[] encode(byte[] g) {
        int[] tmp = new int[GW * GH / 2];
        int n = 0;
        for (int y = 0; y < GH; y++) {
            int x = 0;
            while (x < GW) {
                int part = inDisc(x, y) ? g[y * GW + x] : 0;
                if (part == 0) { x++; continue; }
                int e = isEdge(g, x, y);
                int x2 = x + 1;
                while (x2 < GW && inDisc(x2, y) && g[y * GW + x2] == part && isEdge(g, x2, y) == e) x2++;
                tmp[n++] = pack(x, y, x2 - x, part, e);
                x = x2;
            }
        }
        return Arrays.copyOf(tmp, n);
    }

    private static int isEdge(byte[] g, int x, int y) {
        return (empty(g, x - 1, y) || empty(g, x + 1, y) || empty(g, x, y - 1) || empty(g, x, y + 1)) ? 1 : 0;
    }

    private static boolean empty(byte[] g, int x, int y) {
        return x < 0 || y < 0 || x >= GW || y >= GH || g[y * GW + x] == 0 || !inDisc(x, y);
    }
}
