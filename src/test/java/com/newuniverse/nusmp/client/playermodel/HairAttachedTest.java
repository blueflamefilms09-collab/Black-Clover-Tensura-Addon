package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 3.37 hair rework (owner: "the hair is floating, the pieces are not attached to the head"): proves with plain geometry, no game, that every box and every chain segment of every anime_* haircut
 * of {@code hair_styles.json} is attached to the head, and that the spikes and curls are built as solid stacks. The same maths is in {@code tools/playermodel/hair_lib.py} ({@code attach_report}),
 * which the generator runs before it writes anything. A piece is an oriented box (a rotated box turns about its pivot in vanilla's Rz Ry Rx order, a chain segment is taken at rest).
 * <ol>
 * <li>ATTACHED: the exact distance between the piece and the scalp cap, or a piece that is already attached, is at most {@link #GAP} pixels (a chain segment hangs from the one above it);</li>
 * <li>EMBEDDED: a static box overlaps the piece it hangs on by at least {@link #EMBED} pixels (the smallest overlap over the 15 separating axes), so a spike stands in the hair, not on a corner of it;</li>
 * <li>BESIDE THE HEAD: a static box with any part lower than the top of the head ({@link #SIDE_Y}) touches the scalp cap itself, so a side puff cannot hang off a piece that floats above;</li>
 * <li>NO Z-FIGHT: two pieces of one orientation never have a face on the same plane facing the same way over more than {@link #MIN_AREA} square pixels.</li>
 * </ol>
 */
class HairAttachedTest {
    static final double GAP = 0.1, EMBED = 0.4, SIDE_Y = -8.0, COPLANAR_TOL = 0.02, MIN_AREA = 0.5;
    static final int QUAD_BUDGET = 200;
    private static final String HAIR = "src/main/resources/assets/nusmp/playermodel/hair_styles.json";
    private static final String SHEETS = "src/main/resources/assets/nusmp/textures/entity/playermodel/";

    /** The haircuts the owner's hair brief names; every one must exist. */
    static final List<String> CONTRACT = List.of("anime_short", "anime_short_open", "anime_swept", "anime_long", "anime_long_open", "anime_afro", "anime_topknot", "anime_mane", "anime_hood",
            "anime_spiky", "anime_spike_tall", "anime_spike_swept", "anime_curls", "anime_afro_wide", "anime_flattop", "anime_bangs_long");

    // ================================================================ oriented boxes
    static final class Obb {
        final String name;
        final double[] c, h;
        final double[][] r;                 // r[i][k]: the k-th axis of the box is the column k
        final String chain;
        final int index;
        final boolean rotated;
        final double[][] v = new double[8][];

        Obb(String name, double[] c, double[] h, double[][] r, boolean rotated, String chain, int index) {
            this.name = name;
            this.c = c;
            this.h = h;
            this.r = r;
            this.rotated = rotated;
            this.chain = chain;
            this.index = index;
            for (int i = 0; i < 8; i++) {
                double[] s = {(i & 4) != 0 ? 1 : -1, (i & 2) != 0 ? 1 : -1, (i & 1) != 0 ? 1 : -1};
                double[] local = {s[0] * h[0], s[1] * h[1], s[2] * h[2]};
                double[] w = mulV(r, local);
                v[i] = new double[]{c[0] + w[0], c[1] + w[1], c[2] + w[2]};
            }
        }

        double[] axis(int k) { return new double[]{r[0][k], r[1][k], r[2][k]}; }

        double[] min() {
            double[] m = {1e18, 1e18, 1e18};
            for (double[] p : v) for (int i = 0; i < 3; i++) m[i] = Math.min(m[i], p[i]);
            return m;
        }

        double[] max() {
            double[] m = {-1e18, -1e18, -1e18};
            for (double[] p : v) for (int i = 0; i < 3; i++) m[i] = Math.max(m[i], p[i]);
            return m;
        }
    }

    static double[][] mul(double[][] a, double[][] b) {
        double[][] o = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) o[i][j] += a[i][k] * b[k][j];
        return o;
    }

    static double[] mulV(double[][] a, double[] x) {
        return new double[]{a[0][0] * x[0] + a[0][1] * x[1] + a[0][2] * x[2], a[1][0] * x[0] + a[1][1] * x[1] + a[1][2] * x[2], a[2][0] * x[0] + a[2][1] * x[1] + a[2][2] * x[2]};
    }

    static double[] mulTV(double[][] a, double[] x) {
        return new double[]{a[0][0] * x[0] + a[1][0] * x[1] + a[2][0] * x[2], a[0][1] * x[0] + a[1][1] * x[1] + a[2][1] * x[2], a[0][2] * x[0] + a[1][2] * x[1] + a[2][2] * x[2]};
    }

    static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    static double[] sub(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }

    static double[] cross(double[] a, double[] b) { return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]}; }

    static double len(double[] a) { return Math.sqrt(dot(a, a)); }

    /** Vanilla's rotation of a model part: Rz * Ry * Rx, degrees. */
    static double[][] rotZYX(double rxd, double ryd, double rzd) {
        double rx = Math.toRadians(rxd), ry = Math.toRadians(ryd), rz = Math.toRadians(rzd);
        double cx = Math.cos(rx), sx = Math.sin(rx), cy = Math.cos(ry), sy = Math.sin(ry), cz = Math.cos(rz), sz = Math.sin(rz);
        return mul(new double[][]{{cz, -sz, 0}, {sz, cz, 0}, {0, 0, 1}}, mul(new double[][]{{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}}, new double[][]{{1, 0, 0}, {0, cx, -sx}, {0, sx, cx}}));
    }

    static final double[][] IDENTITY = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

    /** The oriented boxes of a style: the static boxes, then every chain segment at rest (a chain hangs from its root, the box centred on x and z, its top at the pivot). */
    static List<Obb> piecesOf(HairStyleData.Style s) {
        List<Obb> out = new ArrayList<>();
        for (HairStyleData.Box b : s.boxes()) {
            double[] f = {b.from()[0], b.from()[1], b.from()[2]}, sz = {b.size()[0], b.size()[1], b.size()[2]};
            double[] c = {f[0] + sz[0] / 2, f[1] + sz[1] / 2, f[2] + sz[2] / 2};
            double[][] r = IDENTITY;
            if (b.rotated()) {
                double[] pv = b.pivot() != null ? new double[]{b.pivot()[0], b.pivot()[1], b.pivot()[2]} : new double[]{f[0] + sz[0] / 2, f[1] + sz[1], f[2] + sz[2] / 2};
                r = rotZYX(b.rot()[0], b.rot()[1], b.rot()[2]);
                double[] w = mulV(r, sub(c, pv));
                c = new double[]{pv[0] + w[0], pv[1] + w[1], pv[2] + w[2]};
            }
            double i = b.inflate();
            out.add(new Obb(b.name(), c, new double[]{sz[0] / 2 + i, sz[1] / 2 + i, sz[2] / 2 + i}, r, b.rotated(), null, 0));
        }
        for (HairStyleData.Chain ch : s.chains()) {
            for (int i = 0; i < ch.segments().size(); i++) {
                HairStyleData.Segment g = ch.segments().get(i);
                float[] p = HairStyleData.pivotOf(ch, i);
                double infl = g.inflate();
                out.add(new Obb(ch.name() + "#" + i, new double[]{p[0], p[1] + g.size()[1] / 2.0, p[2]}, new double[]{g.size()[0] / 2.0 + infl, g.size()[1] / 2.0 + infl, g.size()[2] / 2.0 + infl},
                        IDENTITY, false, ch.name(), i));
            }
        }
        return out;
    }

    // ================================================================ distances
    static double aabbGap(Obb a, Obb b) {
        double[] la = a.min(), ha = a.max(), lb = b.min(), hb = b.max();
        double s = 0;
        for (int i = 0; i < 3; i++) {
            double g = Math.max(0, Math.max(lb[i] - ha[i], la[i] - hb[i]));
            s += g * g;
        }
        return Math.sqrt(s);
    }

    static double pointObb(double[] p, Obb o) {
        double[] q = mulTV(o.r, sub(p, o.c));
        double s = 0;
        for (int i = 0; i < 3; i++) {
            double d = Math.abs(q[i]) - o.h[i];
            if (d > 0) s += d * d;
        }
        return Math.sqrt(s);
    }

    static double segSeg(double[] p1, double[] q1, double[] p2, double[] q2) {
        double[] d1 = sub(q1, p1), d2 = sub(q2, p2), r = sub(p1, p2);
        double a = dot(d1, d1), e = dot(d2, d2), f = dot(d2, r), s = 0, t = 0;
        if (a <= 1e-12 && e <= 1e-12) return len(r);
        if (a <= 1e-12) {
            t = Math.min(1, Math.max(0, f / e));
        } else {
            double c = dot(d1, r);
            if (e <= 1e-12) {
                s = Math.min(1, Math.max(0, -c / a));
            } else {
                double b = dot(d1, d2), den = a * e - b * b;
                s = den > 1e-12 ? Math.min(1, Math.max(0, (b * f - c * e) / den)) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = Math.min(1, Math.max(0, -c / a));
                } else if (t > 1) {
                    t = 1;
                    s = Math.min(1, Math.max(0, (b - c) / a));
                }
            }
        }
        double[] x = {p1[0] + d1[0] * s - (p2[0] + d2[0] * t), p1[1] + d1[1] * s - (p2[1] + d2[1] * t), p1[2] + d1[2] * s - (p2[2] + d2[2] * t)};
        return len(x);
    }

    /** The penetration depth of two oriented boxes (the smallest overlap over the 15 separating axes), or -1 when one axis separates them. */
    static double overlap(Obb a, Obb b) {
        List<double[]> axes = new ArrayList<>();
        for (int i = 0; i < 3; i++) axes.add(a.axis(i));
        for (int i = 0; i < 3; i++) axes.add(b.axis(i));
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
            double[] x = cross(a.axis(i), b.axis(j));
            double n = len(x);
            if (n > 1e-6) axes.add(new double[]{x[0] / n, x[1] / n, x[2] / n});
        }
        double[] dc = sub(b.c, a.c);
        double depth = 1e9;
        for (double[] ax : axes) {
            double ra = 0, rb = 0;
            for (int k = 0; k < 3; k++) {
                ra += a.h[k] * Math.abs(dot(a.axis(k), ax));
                rb += b.h[k] * Math.abs(dot(b.axis(k), ax));
            }
            double ov = ra + rb - Math.abs(dot(dc, ax));
            if (ov < 0) return -1;
            depth = Math.min(depth, ov);
        }
        return depth;
    }

    static double distance(Obb a, Obb b, double limit) {
        if (aabbGap(a, b) > limit) return limit + 1;
        if (overlap(a, b) >= 0) return 0;
        double best = 1e9;
        for (double[] p : a.v) best = Math.min(best, pointObb(p, b));
        for (double[] p : b.v) best = Math.min(best, pointObb(p, a));
        for (int i = 0; i < 8; i++) for (int m : new int[]{1, 2, 4}) {
            int j = i ^ m;
            if (i > j) continue;
            for (int k = 0; k < 8; k++) for (int n : new int[]{1, 2, 4}) {
                int l = k ^ n;
                if (k > l) continue;
                best = Math.min(best, segSeg(a.v[i], a.v[j], b.v[k], b.v[l]));
            }
        }
        return best;
    }

    // ================================================================ the report
    record Report(List<String> floating, List<String> shallow, List<String> beside, List<String> coplanar, int pieces) {
        boolean clean() { return floating.isEmpty() && shallow.isEmpty() && beside.isEmpty() && coplanar.isEmpty(); }

        @Override
        public String toString() { return "floating " + floating + ", shallow " + shallow + ", beside the head but off the cap " + beside + ", coplanar " + coplanar; }
    }

    static Report report(HairStyleData.Style s) {
        List<Obb> ps = piecesOf(s);
        Obb cap = ps.stream().filter(p -> p.name.equals("cap")).findFirst().orElse(null);
        if (cap == null) return new Report(List.of(), List.of(), List.of(), List.of(), ps.size());
        List<Obb> attached = new ArrayList<>(List.of(cap));
        List<Obb> todo = new ArrayList<>(ps);
        todo.remove(cap);
        java.util.Map<String, Double> depthOf = new java.util.HashMap<>();
        boolean progress = true;
        while (!todo.isEmpty() && progress) {
            progress = false;
            for (Obb p : new ArrayList<>(todo)) {
                double best = -1;
                for (Obb q : attached) {
                    if (p.chain != null && p.index > 0 && !(p.chain.equals(q.chain) && q.index == p.index - 1)) continue;      // a chain segment hangs from the segment above it
                    if (distance(p, q, GAP) <= GAP) best = Math.max(best, Math.max(0, overlap(p, q)));
                }
                if (best >= 0) {
                    attached.add(p);
                    todo.remove(p);
                    depthOf.put(p.name, best);
                    progress = true;
                }
            }
        }
        List<String> floating = new ArrayList<>(), shallow = new ArrayList<>(), beside = new ArrayList<>();
        for (Obb p : todo) floating.add(p.name);
        for (Obb p : attached) if (p.chain == null && p != cap && depthOf.getOrDefault(p.name, 0.0) < EMBED) shallow.add(p.name);
        for (Obb p : ps) if (p.chain == null && p != cap && p.max()[1] > SIDE_Y && distance(p, cap, GAP) > GAP) beside.add(p.name);
        return new Report(floating, shallow, beside, coplanar(ps), ps.size());
    }

    static List<String> coplanar(List<Obb> ps) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ps.size(); i++) for (int j = i + 1; j < ps.size(); j++) {
            Obb a = ps.get(i), b = ps.get(j);
            if (a.chain != null && a.chain.equals(b.chain)) continue;
            boolean same = true;
            for (int x = 0; x < 3 && same; x++) for (int y = 0; y < 3; y++) if (Math.abs(a.r[x][y] - b.r[x][y]) > 1e-6) same = false;
            if (!same) continue;
            double[] ca = mulTV(a.r, a.c), cb = mulTV(b.r, b.c);
            for (int ax = 0; ax < 3; ax++) {
                double ov = 1;
                for (int k = 0; k < 3; k++) {
                    if (k == ax) continue;
                    ov *= Math.max(0, Math.min(ca[k] + a.h[k], cb[k] + b.h[k]) - Math.max(ca[k] - a.h[k], cb[k] - b.h[k]));
                }
                if (ov < MIN_AREA) continue;
                if (Math.abs((ca[ax] - a.h[ax]) - (cb[ax] - b.h[ax])) < COPLANAR_TOL || Math.abs((ca[ax] + a.h[ax]) - (cb[ax] + b.h[ax])) < COPLANAR_TOL) out.add(a.name + "|" + b.name);
            }
        }
        return out.stream().distinct().sorted().toList();
    }

    // ================================================================ data
    private static Map<String, HairStyleData.Style> hair() throws Exception {
        try (Reader r = new FileReader(HAIR)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            return HairStyleData.parse(o);
        }
    }

    private static HairStyleData.Box box(String name, float[] from, float[] size, float inflate, float[] rot, float[] pivot) {
        return new HairStyleData.Box(name, HairStyleData.Part.HEAD, from, size, inflate, 0, 0, new int[]{1, 1, 1}, 0, rot, pivot, HairStyleData.Helm.HIDE);
    }

    private static HairStyleData.Style synthetic(HairStyleData.Box... boxes) {
        return new HairStyleData.Style("t", "hair/t", List.of(boxes), List.of());
    }

    private static final HairStyleData.Box CAP = box("cap", new float[]{-4, -8, -4}, new float[]{8, 8, 8}, 0.8f, null, null);

    // ================================================================ tests
    @Test
    void everyContractStyleExistsAndEveryAnimeStyleIsAttachedToTheHead() throws Exception {
        Map<String, HairStyleData.Style> hair = hair();
        for (String id : CONTRACT) assertTrue(hair.containsKey(id), "the hair brief names " + id);
        int checked = 0;
        for (HairStyleData.Style s : hair.values()) {
            if (!s.id().startsWith("anime_")) continue;
            Report r = report(s);
            assertTrue(r.clean(), s.id() + ": " + r);
            checked++;
        }
        assertTrue(checked >= CONTRACT.size(), "checked " + checked);
    }

    @Test
    void theCheckCatchesHairThatFloats() {
        // a lock that starts 1.2 px beside the head (the old Bon Clay side puffs): it touches nothing
        Report far = report(synthetic(CAP, box("puff", new float[]{5.4f, -9f, -3f}, new float[]{2.2f, 5.4f, 6.8f}, 0.2f, null, null)));
        assertEquals(List.of("puff"), far.floating(), far.toString());
        // a puff 0.2 px off the cap that is joined to a mass above it: connected, but beside the head and not on the cap
        HairStyleData.Box shell = box("shell", new float[]{-5.4f, -12.8f, -5.4f}, new float[]{10.8f, 7.4f, 10.8f}, 0.3f, null, null);
        Report off = report(synthetic(CAP, shell, box("puffL", new float[]{5.2f, -11f, -3.4f}, new float[]{2.2f, 5.4f, 6.8f}, 0.2f, null, null)));
        assertTrue(off.floating().isEmpty() && off.beside().contains("puffL"), off.toString());
        // a spike box that only touches its parent with a corner
        Report corner = report(synthetic(CAP, box("base", new float[]{-1.5f, -11.5f, -1.5f}, new float[]{3, 3.4f, 3}, 0.1f, null, null),
                box("tip", new float[]{1.65f, -14.65f, -1f}, new float[]{2, 3, 2}, 0.05f, null, null)));
        assertTrue(corner.shallow().contains("tip"), corner.toString());
        // two equal boxes with a face on one plane z-fight
        Report fight = report(synthetic(CAP, box("a", new float[]{-4, -10, -4}, new float[]{8, 2.4f, 8}, 1.3f, null, null), box("b", new float[]{-4, -10, -4}, new float[]{8, 3.4f, 8}, 1.3f, null, null)));
        assertFalse(fight.coplanar().isEmpty(), fight.toString());
        // a solid stack on the cap passes
        Report ok = report(synthetic(CAP, box("a", new float[]{-2, -10.5f, -2}, new float[]{4, 3, 4}, 0.1f, null, null), box("b", new float[]{-1.5f, -12.5f, -1.5f}, new float[]{3, 3, 3}, 0.15f, null, null)));
        assertTrue(ok.clean(), ok.toString());
    }

    @Test
    void everyAnimeStyleIsWithinTheQuadBudget() throws Exception {
        for (HairStyleData.Style s : hair().values()) {
            if (!s.id().startsWith("anime_")) continue;
            int pieces = s.boxes().size();
            for (HairStyleData.Chain c : s.chains()) pieces += c.segments().size();
            assertTrue(pieces * 6 <= QUAD_BUDGET, s.id() + " draws " + pieces * 6 + " quads, the budget is " + QUAD_BUDGET);
        }
    }

    private static final Pattern STACK = Pattern.compile("^(.*?)([a-d])$");

    /** The spiky cuts are real spikes: at least 8 of them, every one a stack of at least two boxes, the base wider than the tip, the sections of one stack overlapping. */
    @Test
    void theSpikyCutsAreStacksOfOverlappingSectionsWithAWiderBase() throws Exception {
        Map<String, HairStyleData.Style> hair = hair();
        for (String id : List.of("anime_spiky", "anime_spike_tall", "anime_spike_swept", "anime_mane")) {
            HairStyleData.Style s = hair.get(id);
            Map<String, List<HairStyleData.Box>> stacks = new java.util.LinkedHashMap<>();
            for (HairStyleData.Box b : s.boxes()) {
                Matcher m = STACK.matcher(b.name());
                if (m.matches() && b.rotated()) stacks.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(b);
            }
            assertTrue(stacks.size() >= 8, id + " has " + stacks.size() + " spikes");
            List<Obb> ps = piecesOf(s);
            for (Map.Entry<String, List<HairStyleData.Box>> e : stacks.entrySet()) {
                List<HairStyleData.Box> st = e.getValue();
                assertTrue(st.size() >= 2 && st.size() <= 4, id + " " + e.getKey() + ": " + st.size() + " sections");
                assertTrue(st.get(0).size()[0] > st.get(st.size() - 1).size()[0], id + " " + e.getKey() + ": the base must be wider than the tip");
                for (int i = 1; i < st.size(); i++) {
                    String a = e.getKey() + (char) ('a' + i - 1), b = e.getKey() + (char) ('a' + i);
                    Obb oa = ps.stream().filter(p -> p.name.equals(a)).findFirst().orElseThrow(), ob = ps.stream().filter(p -> p.name.equals(b)).findFirst().orElseThrow();
                    assertTrue(overlap(oa, ob) >= EMBED, id + " " + e.getKey() + ": section " + b + " must sink into " + a);
                }
            }
        }
    }

    /** The data's own rules still hold for the new styles: UVs in the sheet, the cap on the hat layer, nothing blank under a UV box (the sheets are read as the game reads them). */
    @Test
    void everyBoxOfEveryAnimeStyleHasPaintUnderItsUv() throws Exception {
        for (HairStyleData.Style s : hair().values()) {
            if (!s.id().startsWith("anime_")) continue;
            File f = new File(SHEETS + s.texture() + ".png");
            assertTrue(f.isFile(), f + " is missing");
            BufferedImage img = ImageIO.read(f);
            assertEquals(HairStyleData.SHEET, img.getWidth(), s.id() + " sheet width");
            assertEquals(HairStyleData.SHEET, img.getHeight(), s.id() + " sheet height");
            for (HairStyleData.Box b : s.boxes()) assertTrue(opaque(img, b.u(), b.v(), b.uvSize()) > 0, s.id() + " box " + b.name() + " has a blank UV region");
            for (HairStyleData.Chain c : s.chains()) for (HairStyleData.Segment g : c.segments()) assertTrue(opaque(img, g.u(), g.v(), g.uvSize()) > 0, s.id() + " chain " + c.name() + " has a blank UV region");
        }
    }

    private static int opaque(BufferedImage img, int u, int v, int[] uv) {
        int n = 0;
        int w = uv[0], h = uv[1], d = uv[2];
        for (int y = v; y < v + d + h && y < img.getHeight(); y++) {
            for (int x = u; x < u + 2 * (w + d) && x < img.getWidth(); x++) {
                if (((img.getRGB(x, y) >>> 24) & 255) > 25) n++;
            }
        }
        return n;
    }
}
