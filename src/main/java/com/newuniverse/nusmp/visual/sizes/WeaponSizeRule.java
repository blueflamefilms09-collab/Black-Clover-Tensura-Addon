package com.newuniverse.nusmp.visual.sizes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The in-hand size rule of daggers and katanas (1.110.0, docs/visual_upgrade/weapon_sizes.md), as plain data and arithmetic: no Minecraft class, so the renderer, the JUnit tests
 * and the CI self-test {@code visual.weapon_sizes} all use the same code.
 * <p>
 * Why: the owner saw a tiny dagger and katanas that looked like a thin line. Measured, a katana blade was 1.1 pixels wide and 0.2 pixels thick in the hand, and four daggers had a
 * first-person scale of 0.383. tools/relics/weapon_sizes.py writes thickened copies of the meshes into {@code weapon_meshes_sz}, {@code weapon_meshes_sz/up} and
 * {@code weapon_meshes_vu_sz} (the old files stay); the renderer reads the copy when it exists ({@link #szPath}); this class measures a mesh text the same way the tool does and checks
 * it against the limits of {@code assets/nusmp/weapon_sizes.json}.
 */
public final class WeaponSizeRule {
    private WeaponSizeRule() {}

    public static final int BINS = 32;
    /** The stretch of the length the blade section is measured over (a guard never reaches it, a taper has not begun): 45 to 70 percent. */
    public static final double BLADE_FROM = 0.45, BLADE_TO = 0.70;
    /** A mesh corner must stay this far in front of the camera in first person (tools/relics/display_presets.py Z_MARGIN). */
    public static final double Z_MARGIN = 0.16;

    // ---------------------------------------------------------------- paths
    /**
     * The path of the thickened copy of a mesh file (relative to assets/nusmp), or null for a path that has none: {@code weapon_meshes/x.txt} to {@code weapon_meshes_sz/x.txt},
     * {@code weapon_meshes/up/x.txt} to {@code weapon_meshes_sz/up/x.txt}, {@code weapon_meshes_vu/x.txt} to {@code weapon_meshes_vu_sz/x.txt}.
     */
    public static String szPath(String file) {
        if (file == null) return null;
        if (file.startsWith("weapon_meshes_vu/")) return "weapon_meshes_vu_sz/" + file.substring("weapon_meshes_vu/".length());
        if (file.startsWith("weapon_meshes/")) return "weapon_meshes_sz/" + file.substring("weapon_meshes/".length());
        return null;
    }

    // ---------------------------------------------------------------- the rule file
    /** One class row of the rule file (tests section). */
    public record Limits(double len3pMin, double len3pMax, double len1pMin, double len1pMax, double bw1pMin, double bw3pMin, double bdMin, double bw1pMax, double bdMax) {}

    /** The parsed rule file: the limits of the tests per class, the excluded ids. */
    public static final class Rules {
        public final java.util.Map<String, Limits> limits = new java.util.LinkedHashMap<>();
        public final List<String> excluded = new ArrayList<>();
    }

    public static Rules parseRules(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Rules r = new Rules();
        JsonObject t = root.getAsJsonObject("tests");
        for (var e : t.entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            r.limits.put(e.getKey(), new Limits(o.get("len3p_min").getAsDouble(), o.get("len3p_max").getAsDouble(), o.get("len1p_min").getAsDouble(), o.get("len1p_max").getAsDouble(),
                    o.get("bw_1p_min").getAsDouble(), o.get("bw_3p_min").getAsDouble(), o.get("bd_min").getAsDouble(), o.get("bw_1p_max").getAsDouble(), o.get("bd_max").getAsDouble()));
        }
        JsonArray ex = root.getAsJsonArray("excluded");
        if (ex != null) for (JsonElement e : ex) r.excluded.add(e.getAsString());
        return r;
    }

    // ---------------------------------------------------------------- measuring a mesh text
    /** What the rule reads of a mesh file. Lengths and widths in blocks at the file's own place scale. */
    public static final class Measure {
        public double[] place = new double[5];               // ox oy angle scale ymin
        public double length;                                // (ymax over the static body, void and glow layers - ymin) * scale
        public double bladeWidth, bladeDepth;                // median of the slices at BLADE_FROM..BLADE_TO of the length
        public double maxHandleWidth;                        // widest slice of the lowest quarter (grip and guard): it must fit a hand
        public double[] boxMin = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, boxMax = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        public int quads;
        public boolean valid() { return quads > 0 && place[3] > 0; }
    }

    /** Measures the lines of a weapon mesh file (the dialects of the old meshes, the upgrade meshes and the Genesis meshes). */
    public static Measure measure(List<String> lines) {
        Measure m = new Measure();
        boolean vu = false;
        for (String l : lines) if (l.startsWith("vu ")) { vu = true; break; }
        List<double[][]> body = new ArrayList<>();
        String group = "-";
        double ymaxAll = -Double.MAX_VALUE;
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty() || t.charAt(0) == '#') continue;
            String[] v = t.split("\\s+");
            switch (v[0]) {
                case "place" -> { for (int i = 0; i < 5; i++) m.place[i] = Double.parseDouble(v[i + 1]); }
                case "g" -> group = v.length > 1 ? v[1] : "-";
                case "q", "qn" -> {
                    int stride = v[0].equals("q") ? 5 : 8;
                    int layer = Integer.parseInt(v[1]);
                    double[][] p = new double[4][3];
                    for (int k = 0; k < 4; k++) {
                        for (int j = 0; j < 3; j++) p[k][j] = Double.parseDouble(v[2 + k * stride + j]);
                        for (int j = 0; j < 3; j++) {
                            m.boxMin[j] = Math.min(m.boxMin[j], p[k][j]);
                            m.boxMax[j] = Math.max(m.boxMax[j], p[k][j]);
                        }
                    }
                    m.quads++;
                    boolean staticGroup = !vu ? layer <= 2 : group.equals("-") && layer <= 3;
                    if (layer <= 2 && (staticGroup || layer == 0)) for (double[] pt : p) ymaxAll = Math.max(ymaxAll, pt[1]);
                    if (layer == 0 && (vu ? group.equals("-") : true)) body.add(p);
                }
                default -> { }
            }
        }
        if (body.isEmpty() || !(m.place[3] > 0)) return m;
        double y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (double[][] q : body) for (double[] p : q) { y0 = Math.min(y0, p[1]); y1 = Math.max(y1, p[1]); }
        double len = Math.max(y1 - y0, 1e-9), bh = len / BINS, sc = m.place[3];
        double[] xmin = new double[BINS], xmax = new double[BINS], zmin = new double[BINS], zmax = new double[BINS];
        Arrays.fill(xmin, 1e30);
        Arrays.fill(xmax, -1e30);
        Arrays.fill(zmin, 1e30);
        Arrays.fill(zmax, -1e30);
        for (double[][] q : body) {
            double qy0 = Math.min(Math.min(q[0][1], q[1][1]), Math.min(q[2][1], q[3][1])), qy1 = Math.max(Math.max(q[0][1], q[1][1]), Math.max(q[2][1], q[3][1]));
            int b0 = Math.min(BINS - 1, Math.max(0, (int) ((qy0 - y0) / bh))), b1 = Math.min(BINS - 1, Math.max(0, (int) ((qy1 - y0) / bh)));
            for (int b = b0; b <= b1; b++) {
                List<double[]> poly = new ArrayList<>(Arrays.asList(q));
                if (b0 != b1) poly = clip(clip(poly, y0 + b * bh, true), y0 + (b + 1) * bh, false);
                if (poly.size() < 3) continue;
                for (double[] p : poly) {
                    xmin[b] = Math.min(xmin[b], p[0]);
                    xmax[b] = Math.max(xmax[b], p[0]);
                    zmin[b] = Math.min(zmin[b], p[2]);
                    zmax[b] = Math.max(zmax[b], p[2]);
                }
            }
        }
        int lo = (int) (BLADE_FROM * BINS), hi = (int) Math.ceil(BLADE_TO * BINS);
        double[] ws = new double[hi - lo], ds = new double[hi - lo];
        for (int b = lo; b < hi; b++) {
            ws[b - lo] = (xmax[b] >= xmin[b] ? xmax[b] - xmin[b] : 0.0) * sc;
            ds[b - lo] = (zmax[b] >= zmin[b] ? zmax[b] - zmin[b] : 0.0) * sc;
        }
        m.bladeWidth = median(ws);
        m.bladeDepth = median(ds);
        for (int b = 1; b < 8; b++) if (xmax[b] >= xmin[b]) m.maxHandleWidth = Math.max(m.maxHandleWidth, Math.max(xmax[b] - xmin[b], zmax[b] - zmin[b]) * sc);
        m.length = (ymaxAll - m.place[4]) * sc;
        return m;
    }

    private static double median(double[] a) {
        double[] s = a.clone();
        Arrays.sort(s);
        return s.length % 2 == 1 ? s[s.length / 2] : 0.5 * (s[s.length / 2 - 1] + s[s.length / 2]);
    }

    private static List<double[]> clip(List<double[]> poly, double y, boolean keepGe) {
        List<double[]> out = new ArrayList<>();
        int n = poly.size();
        for (int k = 0; k < n; k++) {
            double[] a = poly.get(k), b = poly.get((k + 1) % n);
            boolean ina = keepGe ? a[1] >= y : a[1] <= y, inb = keepGe ? b[1] >= y : b[1] <= y;
            if (ina) out.add(a);
            if (ina != inb && Math.abs(b[1] - a[1]) > 1e-12) {
                double t = (y - a[1]) / (b[1] - a[1]);
                out.add(new double[]{a[0] + t * (b[0] - a[0]), y, a[2] + t * (b[2] - a[2])});
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- first-person camera
    /**
     * The depth (blocks in front of the camera) of the nearest corner of the mesh's box under a first-person display of the right hand: rotation (degrees), translation (pixels),
     * scale. The chain of tools/relics/display_presets.py: c = H + T + R * (s * (p - P)) with H the resting hand and R = Rx Ry Rz.
     */
    public static double nearestCornerDepth(Measure m, double[] rotation, double[] translationPx, double scale) {
        double[][] r = rot(rotation[0], rotation[1], rotation[2]);
        double a = Math.toRadians(m.place[2]), dx = Math.cos(a), dy = Math.sin(a), ux = Math.sin(a), uy = -Math.cos(a);
        double nearest = Double.MAX_VALUE;
        for (int cx = 0; cx < 2; cx++) for (int cy = 0; cy < 2; cy++) for (int cz = 0; cz < 2; cz++) {
            double mx = cx == 0 ? m.boxMin[0] : m.boxMax[0], my = cy == 0 ? m.boxMin[1] : m.boxMax[1], mz = cz == 0 ? m.boxMin[2] : m.boxMax[2];
            double along = (my - m.place[4]) * m.place[3], across = mx * m.place[3];
            double[] p = {m.place[0] + along * dx + across * ux, m.place[1] + along * dy + across * uy, 0.5 + mz * m.place[3]};
            double[] q = {scale * (p[0] - 0.5), scale * (p[1] - 0.5), scale * (p[2] - 0.5)};
            double z = -0.72 + translationPx[2] / 16.0 + r[2][0] * q[0] + r[2][1] * q[1] + r[2][2] * q[2];
            nearest = Math.min(nearest, -z);
        }
        return nearest;
    }

    static double[][] rot(double rx, double ry, double rz) {
        double ax = Math.toRadians(rx), ay = Math.toRadians(ry), az = Math.toRadians(rz);
        double cx = Math.cos(ax), sx = Math.sin(ax), cy = Math.cos(ay), sy = Math.sin(ay), cz = Math.cos(az), sz = Math.sin(az);
        double[][] mx = {{1, 0, 0}, {0, cx, -sx}, {0, sx, cx}}, my = {{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}}, mz = {{cz, -sz, 0}, {sz, cz, 0}, {0, 0, 1}};
        return mul(mx, mul(my, mz));
    }

    private static double[][] mul(double[][] a, double[][] b) {
        double[][] c = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) c[i][j] += a[i][k] * b[k][j];
        return c;
    }

    // ---------------------------------------------------------------- the verdict
    /**
     * The problems of one mesh against the limits of its class; empty = fine. {@code s3} and {@code s1} are the third- and first-person display scales of the model json,
     * {@code fpRotation}/{@code fpTranslationPx} its first-person right-hand entry (null: no camera check).
     */
    public static List<String> problems(String id, Measure m, Limits lim, double s3, double s1, double[] fpRotation, double[] fpTranslationPx) {
        List<String> out = new ArrayList<>();
        if (!m.valid()) {
            out.add(id + ": the mesh has no quads or no place line");
            return out;
        }
        double l3 = m.length * s3, l1 = m.length * s1, w1 = m.bladeWidth * s1, w3 = m.bladeWidth * s3, d3 = m.bladeDepth * s3;
        if (l3 < lim.len3pMin() || l3 > lim.len3pMax()) out.add(String.format("%s: third-person length %.2f outside %.2f - %.2f", id, l3, lim.len3pMin(), lim.len3pMax()));
        if (l1 < lim.len1pMin() || l1 > lim.len1pMax()) out.add(String.format("%s: first-person length %.2f outside %.2f - %.2f", id, l1, lim.len1pMin(), lim.len1pMax()));
        if (w1 < lim.bw1pMin()) out.add(String.format("%s: first-person blade width %.3f below %.3f", id, w1, lim.bw1pMin()));
        if (w3 < lim.bw3pMin()) out.add(String.format("%s: third-person blade width %.3f below %.3f", id, w3, lim.bw3pMin()));
        if (d3 < lim.bdMin()) out.add(String.format("%s: blade thickness %.3f below %.3f", id, d3, lim.bdMin()));
        if (w1 > lim.bw1pMax()) out.add(String.format("%s: first-person blade width %.3f above %.3f", id, w1, lim.bw1pMax()));
        if (d3 > lim.bdMax()) out.add(String.format("%s: blade thickness %.3f above %.3f", id, d3, lim.bdMax()));
        if (m.maxHandleWidth * s1 > 0.30) out.add(String.format("%s: the grip is %.3f blocks wide in first person (a hand is 0.25)", id, m.maxHandleWidth * s1));
        if (fpRotation != null && fpTranslationPx != null) {
            double depth = nearestCornerDepth(m, fpRotation, fpTranslationPx, s1);
            if (depth < Z_MARGIN) out.add(String.format("%s: a corner of the mesh is only %.2f blocks in front of the first-person camera (needs %.2f)", id, depth, Z_MARGIN));
        }
        return out;
    }
}
