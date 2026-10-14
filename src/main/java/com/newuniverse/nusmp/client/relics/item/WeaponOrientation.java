package com.newuniverse.nusmp.client.relics.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WeaponOrientation: the ONE orientation convention of every weapon mesh and the detector that flags a mesh that breaks it. Pure: no Minecraft
 * imports. The Python reference is tools/relics/weapon_orientation.py (same algorithm, same constants); docs/weapon_scale_orientation.md explains it.
 * <p>
 * THE CONVENTION. A mesh lives in mesh space: y along the weapon from the butt (grip end, low y) to the tip or head (high y), x across it in the item
 * sprite's plane, z through it. WeaponRenderer lays mesh y on the sprite's up-right diagonal, so
 * <ul>
 *   <li>the tip or head points up-forward, away from the hand, never down and never back at the wielder;</li>
 *   <li>mesh -x is FORWARD (the sprite's up-left side; in first person it points away from the camera, in third person down): the cutting edge,
 *       the convex side of a crescent, the blade of an axe, the hook of a hook sword face -x; the flat, the back, the poll, the spine face +x;</li>
 *   <li>a curved blade bends like its class does: a katana's tip toward its edge (-x), a sabre's, scimitar's and dadao's toward its spine (+x);</li>
 *   <li>a symmetric weapon (a straight sword, a staff, a double-bladed axe) has no handedness: only "tip up" applies.</li>
 * </ul>
 * Measures are taken on the body layer: every quad is clipped into {@link #BINS} slices along y, per slice the area, the area-weighted x and the x / z
 * extents are kept, so a blade made of a few long quads still has a profile.
 */
public final class WeaponOrientation {
    private WeaponOrientation() {}

    public static final int BINS = 32;
    public static final int GRIP_BINS = 5;
    public static final int TIP_BINS = 2;
    /** Slices above this share of the length count as the head / blade region for the imbalance. */
    public static final double HEAD_FROM = 0.30;
    /** Blocks: below this a mesh has no side (its mass is balanced about the haft). */
    public static final double IMBALANCE_MIN = 0.012;
    /** Blocks: below this a blade is straight. */
    public static final double CURVE_MIN = 0.05;
    /** A head-heavy weapon whose area centroid lies below this share of its length has its head at the butt. */
    public static final double HEAD_HEAVY_MAX = 0.30;
    /** A pointed weapon whose widest part lies above this share of its length is suspicious (informational). */
    public static final double POINTED_WIDEST_MAX = 0.72;

    /** What the detector reports. TIP_DOWN and HEAD_TOWARD_PLAYER are acted on; the other two are informational. */
    public enum Flag { TIP_DOWN, HEAD_TOWARD_PLAYER, MIRRORED, TIP_SUSPECT }

    /** Class traits (class name -> traits): handed = the edge / head is on one side (must be -x); pointed; head_heavy; curve_edge; curve_spine. */
    private static final Map<String, Set<String>> TRAITS = new HashMap<>();
    static {
        t("dagger", "pointed"); t("shortsword", "pointed"); t("longsword", "pointed"); t("greatsword", "pointed"); t("rapier", "pointed");
        t("katana", "pointed", "curve_edge"); t("sabre", "pointed", "curve_spine"); t("dadao", "pointed", "curve_spine");
        t("handaxe", "handed", "head_heavy"); t("battleaxe", "handed", "head_heavy"); t("greataxe", "handed", "head_heavy");
        t("crescent", "handed", "head_heavy"); t("scythe", "handed", "head_heavy"); t("cleaver", "handed"); t("hook", "handed");
        t("spear", "head_heavy"); t("glaive", "handed", "head_heavy"); t("trident", "head_heavy");
        t("staff", "head_heavy"); t("mace", "handed", "head_heavy"); t("hammer", "handed", "head_heavy"); t("club", "head_heavy");
    }

    private static void t(String cls, String... traits) {
        Set<String> s = new java.util.HashSet<>();
        Collections.addAll(s, traits);
        TRAITS.put(cls, s);
    }

    public static boolean hasTrait(String cls, String trait) {
        Set<String> s = cls == null ? null : TRAITS.get(cls);
        return s != null && s.contains(trait);
    }

    /** The measurements of a mesh (lengths in blocks unless named *Mu = mesh units). */
    public static final class Measures {
        public boolean empty = true;
        public double y0, y1, lengthMu, length;
        /** The haft's x: area-weighted mean x of the lowest {@link #GRIP_BINS} slices. */
        public double axisX;
        /** Area-weighted y as a share of the length (0 = butt, 1 = tip). */
        public double centroid;
        /** Area-weighted mean (x - axisX) over the head region, blocks: negative = mass toward -x (forward). */
        public double imbalance;
        public double maxWidth;
        /** Where the widest slice lies (0 = butt, 1 = tip). */
        public double widestAt;
        /** x of the top slices' centre minus axisX, blocks: the sideways displacement of the tip. */
        public double curve;
        public double shaftWidth, headHeight, reachNeg, reachPos;
        public double[] width = new double[BINS];
    }

    // ================================================================ geometry helpers
    private static double polyArea(List<double[]> p) {
        double cx = 0, cy = 0, cz = 0;
        int n = p.size();
        for (int k = 0; k < n; k++) {
            double[] a = p.get(k), b = p.get((k + 1) % n);
            cx += a[1] * b[2] - a[2] * b[1];
            cy += a[2] * b[0] - a[0] * b[2];
            cz += a[0] * b[1] - a[1] * b[0];
        }
        return 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    /** The polygon clipped to lo <= y <= hi (Sutherland-Hodgman, linear in 3D). */
    private static List<double[]> clipY(List<double[]> poly, double lo, double hi) {
        List<double[]> cur = poly;
        for (int pass = 0; pass < 2; pass++) {
            boolean keepGe = pass == 0;
            double bound = keepGe ? lo : hi;
            List<double[]> out = new ArrayList<>();
            int n = cur.size();
            for (int k = 0; k < n; k++) {
                double[] a = cur.get(k), b = cur.get((k + 1) % n);
                boolean ina = keepGe ? a[1] >= bound : a[1] <= bound;
                boolean inb = keepGe ? b[1] >= bound : b[1] <= bound;
                if (ina) out.add(a);
                if (ina != inb && Math.abs(b[1] - a[1]) > 1e-12) {
                    double t = (bound - a[1]) / (b[1] - a[1]);
                    out.add(new double[]{a[0] + t * (b[0] - a[0]), bound, a[2] + t * (b[2] - a[2])});
                }
            }
            cur = out;
            if (cur.isEmpty()) break;
        }
        return cur;
    }

    /**
     * Measures a body-layer vertex list: {@code verts} holds the vertices of consecutive quads (four per quad), each an array whose first three entries
     * are x, y, z in mesh units; {@code scale} is the place scale (blocks per mesh unit).
     */
    public static Measures measure(List<float[]> verts, float scale) {
        Measures m = new Measures();
        int nq = verts.size() / 4;
        if (nq == 0) return m;
        double y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (float[] v : verts) {
            y0 = Math.min(y0, v[1]);
            y1 = Math.max(y1, v[1]);
        }
        double len = Math.max(y1 - y0, 1e-9);
        m.empty = false;
        m.y0 = y0;
        m.y1 = y1;
        m.lengthMu = len;
        m.length = len * scale;
        double[] area = new double[BINS], ax = new double[BINS], ay = new double[BINS];
        double[] xmin = new double[BINS], xmax = new double[BINS];
        java.util.Arrays.fill(xmin, 1e30);
        java.util.Arrays.fill(xmax, -1e30);
        double bh = len / BINS;
        for (int q = 0; q < nq; q++) {
            List<double[]> p = new ArrayList<>(4);
            double qy0 = Double.MAX_VALUE, qy1 = -Double.MAX_VALUE;
            for (int k = 0; k < 4; k++) {
                float[] v = verts.get(q * 4 + k);
                p.add(new double[]{v[0], v[1], v[2]});
                qy0 = Math.min(qy0, v[1]);
                qy1 = Math.max(qy1, v[1]);
            }
            int b0 = Math.min(BINS - 1, Math.max(0, (int) ((qy0 - y0) / bh)));
            int b1 = Math.min(BINS - 1, Math.max(0, (int) ((qy1 - y0) / bh)));
            for (int b = b0; b <= b1; b++) {
                List<double[]> poly = b0 == b1 ? p : clipY(p, y0 + b * bh, y0 + (b + 1) * bh);
                if (poly.size() < 3) continue;
                double a = polyArea(poly);
                double sx = 0, sy = 0;
                for (double[] pt : poly) {
                    sx += pt[0];
                    sy += pt[1];
                    xmin[b] = Math.min(xmin[b], pt[0]);
                    xmax[b] = Math.max(xmax[b], pt[0]);
                }
                area[b] += a;
                ax[b] += a * sx / poly.size();
                ay[b] += a * (sy / poly.size() - y0);
            }
        }
        double total = 0, sumAy = 0;
        for (int b = 0; b < BINS; b++) {
            total += area[b];
            sumAy += ay[b];
        }
        double gx = 0, ga = 0;
        for (int b = 0; b < GRIP_BINS; b++) {
            gx += ax[b];
            ga += area[b];
        }
        if (ga <= 0) {
            gx = 0;
            ga = 0;
            for (int b = 0; b < BINS; b++) {
                gx += ax[b];
                ga += area[b];
            }
        }
        m.axisX = ga > 0 ? gx / ga : 0.0;
        m.centroid = total > 0 ? sumAy / total / len : 0.5;
        int hb = (int) (HEAD_FROM * BINS);
        double ha = 0, hx = 0;
        for (int b = hb; b < BINS; b++) {
            ha += area[b];
            hx += ax[b];
        }
        hx -= m.axisX * ha;
        m.imbalance = ha > 0 ? hx / ha * scale : 0.0;
        double maxW = 0;
        for (int b = 0; b < BINS; b++) {
            m.width[b] = (xmax[b] >= xmin[b] ? xmax[b] - xmin[b] : 0.0) * scale;
            maxW = Math.max(maxW, m.width[b]);
        }
        m.maxWidth = maxW;
        double sumIdx = 0;
        int cnt = 0;
        if (maxW > 0) {
            for (int b = 0; b < BINS; b++) {
                if (m.width[b] >= 0.9 * maxW) {
                    sumIdx += b;
                    cnt++;
                }
            }
        }
        m.widestAt = cnt > 0 ? (sumIdx / cnt + 0.5) / BINS : 0.5;
        double cs = 0;
        int cc = 0;
        for (int b = BINS - 3; b < BINS; b++) {
            if (xmax[b] >= xmin[b]) {
                cs += (xmin[b] + xmax[b]) / 2.0;
                cc++;
            }
        }
        m.curve = cc > 0 ? (cs / cc - m.axisX) * scale : 0.0;
        List<Double> pos = new ArrayList<>();
        for (double w : m.width) if (w > 0) pos.add(w);
        Collections.sort(pos);
        m.shaftWidth = pos.isEmpty() ? 0.0 : pos.get(pos.size() / 2);
        int headBins = 0;
        double limit = Math.max(2.0 * m.shaftWidth, 0.08);
        for (double w : m.width) if (w > limit) headBins++;
        m.headHeight = headBins * len / BINS * scale;
        double rn = 0, rp = 0;
        for (int b = 0; b < BINS; b++) {
            if (xmax[b] >= xmin[b]) {
                rn = Math.max(rn, m.axisX - xmin[b]);
                rp = Math.max(rp, xmax[b] - m.axisX);
            }
        }
        m.reachNeg = rn * scale;
        m.reachPos = rp * scale;
        return m;
    }

    // ================================================================ the detector
    /**
     * The flags of a mesh for a weapon class ({@code cls} null or unknown: only the class-free checks) placed at {@code placeAngleDeg} (the
     * {@code place} line's angle; 45 = the sprite diagonal).
     */
    public static Set<Flag> detect(Measures m, String cls, double placeAngleDeg) {
        Set<Flag> f = EnumSet.noneOf(Flag.class);
        if (m.empty) return f;
        boolean known = cls != null && !"item".equals(cls) && !"whip".equals(cls) && !"coilwhip".equals(cls);
        if (known && Math.sin(Math.toRadians(placeAngleDeg)) < -0.1) f.add(Flag.TIP_DOWN);
        else if (hasTrait(cls, "head_heavy") && m.centroid < HEAD_HEAVY_MAX) f.add(Flag.TIP_DOWN);
        if (hasTrait(cls, "pointed") && m.widestAt > POINTED_WIDEST_MAX) f.add(Flag.TIP_SUSPECT);
        if (hasTrait(cls, "handed") && m.imbalance > IMBALANCE_MIN) f.add(Flag.HEAD_TOWARD_PLAYER);
        if (hasTrait(cls, "curve_edge") && m.curve > CURVE_MIN) f.add(Flag.MIRRORED);
        if (hasTrait(cls, "curve_spine") && m.curve < -CURVE_MIN) f.add(Flag.MIRRORED);
        return f;
    }

    // ================================================================ operations on vertex lists (8 floats each: x y z u v nx ny nz)
    /**
     * Mirrors the mesh across the plane x = px: x -> 2 px - x, the normals' x negated and the winding of every quad reversed (v0 v3 v2 v1) so the
     * front faces stay front faces (the glow layer is culled).
     */
    public static void mirrorX(List<float[]> verts, double px) {
        for (float[] v : verts) {
            v[0] = (float) (2 * px - v[0]);
            if (v.length >= 8) v[5] = -v[5];
        }
        for (int q = 0; q + 3 < verts.size(); q += 4) {
            float[] b = verts.get(q + 1), d = verts.get(q + 3);
            verts.set(q + 1, d);
            verts.set(q + 3, b);
        }
    }

    /** Rotates the mesh by {@code deg} about the z axis through (0, pivotY): 180 turns a tip-down mesh tip-up. Normals turn with it. */
    public static void rotateZ(List<float[]> verts, double deg, double pivotY) {
        double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
        for (float[] v : verts) {
            double x = v[0], y = v[1] - pivotY;
            v[0] = (float) (x * c - y * s);
            v[1] = (float) (x * s + y * c + pivotY);
            if (v.length >= 8) {
                double nx = v[5], ny = v[6];
                v[5] = (float) (nx * c - ny * s);
                v[6] = (float) (nx * s + ny * c);
            }
        }
    }
}
