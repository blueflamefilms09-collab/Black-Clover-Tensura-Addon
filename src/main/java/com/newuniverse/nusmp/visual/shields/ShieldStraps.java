package com.newuniverse.nusmp.visual.shields;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 4.72.0 (docs/visual_upgrade/shield_fix.md, "Straps"): the straps that make a shield hang on the holder's forearm. No Minecraft classes: the renderer ({@code client/ShieldStrapRenderer}),
 * the preview dump and the tests all read this.
 * <p>
 * The cubes (two leather rings that enclose the 4 x 4 px forearm, a grip bar over the hand, two brackets) are written in the ARM frame by {@code tools/blockbench/gen_shield_straps.py} into
 * {@code assets/anime_multiverse/shield_straps.json}. This class puts them on the shield's item frame (the frame of {@link ShieldFrame}) with the same chain the game uses between the arm
 * and a held item ({@code ItemInHandLayer}'s turn to the grip, then the model's display transform), so they land on the arm for the right and the left hand, lowered and raised. Two strap
 * tabs then run straight from the rings to the back of the plate (their length is this shield's). In first person no arm is drawn, so only a grip bar and two mounts are built, low on
 * the plate's back.
 * <p>
 * The straps are NOT part of the mesh: {@code ShieldFrame.fit} never sees them, the shield's size, centre and depth are exactly what they were. Cost per shield: the cubes (90 quads) + two tabs
 * with a rivet each (24) = at most {@link #MAX_QUADS}; built once per mesh and hand state, never per frame.
 */
public final class ShieldStraps {
    private ShieldStraps() {}

    /** The most quads one shield's straps may use (the owner's budget). */
    public static final int MAX_QUADS = 150;
    public static final int LEATHER = 0, TRIM = 1;
    /** The contexts: {@code left * 2 + raised} for the third-person hands (0..3), then first person. */
    public static final int CTX_FIRST_PERSON = 4, CONTEXTS = 5;
    /** The leather colour (tints the grey grain of the texture). */
    public static final int LEATHER_RGB = 0x4A3426;
    /** The metal trim colour of a shield with no entry in the table. */
    public static final int DEFAULT_TRIM_RGB = 0xC8A24A;
    /** Beyond this many blocks (squared) from the camera the straps are not drawn. */
    public static final float CULL_DIST_SQ = 24f * 24f;
    /** Texture pixels per block of a face, and the top-left texel of the leather and the metal half of the 128 x 128 sheet. */
    static final float PX_PER_BLOCK = 32f;
    static final float[] UV0 = {2f, 66f};

    private static volatile boolean enabled = true;

    /** True unless the player turned {@code visual.shieldStraps} off (CLIENT config). Default true. */
    public static boolean enabled() { return enabled; }

    /** Written by the client config only. */
    public static void setEnabled(boolean on) { enabled = on; }

    /** The metal trim colour of each shield mesh: the dominant accent colour of its skin (the glow lines of the meshes are mostly plain white). */
    private static final Map<String, Integer> TRIM_RGB = Map.ofEntries(
            Map.entry("magebane_shield", 0xC89C5B), Map.entry("legendary_shield", 0xDFAE4B), Map.entry("legendary_shield_curse", 0xCE3C2C),
            Map.entry("aegis_of_the_abyss", 0x9237D5), Map.entry("celestial_support", 0xF4DA74), Map.entry("heros_shield", 0x42B4CE),
            Map.entry("thunder_shield", 0x69A9C7), Map.entry("calamity_wave_aegis", 0xEAC157), Map.entry("hylian_hero_shield", 0xE9C259),
            Map.entry("legendary_shield__chimera_viper_shield", 0x7EAD5E), Map.entry("legendary_shield__egg_shield", 0xAE7D3D), Map.entry("legendary_shield__leaf_shield", 0x8B954C),
            Map.entry("legendary_shield__light_metal_shield", 0x61D4E9), Map.entry("legendary_shield__needle_shield", 0x9C7A41), Map.entry("legendary_shield__orange_balloon_shield", 0xDD9051),
            Map.entry("legendary_shield__slime_shield", 0xAC945A), Map.entry("legendary_shield__soul_eater_shield", 0xB664F0), Map.entry("legendary_shield__zombie_shield", 0x96C244));

    /** The trim colour of a mesh id (its own entry, else its item's, else {@link #DEFAULT_TRIM_RGB}). */
    public static int trimRgb(String meshId) {
        Integer c = meshId == null ? null : TRIM_RGB.get(meshId);
        if (c == null && meshId != null) c = TRIM_RGB.get(ShieldSet.baseId(meshId));
        return c == null ? DEFAULT_TRIM_RGB : c;
    }

    // ------------------------------------------------------------------ the data file
    /** One cube in blocks: {@code part} is {@link #LEATHER} or {@link #TRIM}. */
    public record Cube(int part, double[] from, double[] to) {}

    /** The parsed {@code shield_straps.json}. */
    public static final class Data {
        public final List<Cube> cubes = new ArrayList<>(), firstPerson = new ArrayList<>();
        public final List<double[]> attach = new ArrayList<>();
        public double armHalf, tabWidth, tabThick, rivet, rivetThick, overlap;
    }

    public static Data parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Data d = new Data();
        d.armHalf = root.get("arm_half").getAsDouble();
        for (JsonElement e : root.getAsJsonArray("cubes")) d.cubes.add(cube(e.getAsJsonObject()));
        for (JsonElement e : root.getAsJsonArray("firstPerson")) d.firstPerson.add(cube(e.getAsJsonObject()));
        for (JsonElement e : root.getAsJsonArray("attach")) d.attach.add(vec(e.getAsJsonObject().getAsJsonArray("at")));
        JsonObject t = root.getAsJsonObject("tab");
        d.tabWidth = t.get("width").getAsDouble();
        d.tabThick = t.get("thick").getAsDouble();
        d.rivet = t.get("rivet").getAsDouble();
        d.rivetThick = t.get("rivet_thick").getAsDouble();
        d.overlap = t.get("overlap").getAsDouble();
        return d;
    }

    private static Cube cube(JsonObject o) {
        double[] a = vec(o.getAsJsonArray("from")), b = vec(o.getAsJsonArray("to"));
        return new Cube("trim".equals(o.get("part").getAsString()) ? TRIM : LEATHER, a, b);
    }

    private static double[] vec(JsonArray a) { return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()}; }

    // ------------------------------------------------------------------ the arm's place on the shield frame
    private static double[] ident() { return new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }

    private static double[] mul(double[] a, double[] b) {
        double[] r = new double[16];
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) for (int k = 0; k < 4; k++) r[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
        return r;
    }

    private static double[] trans(double x, double y, double z) { double[] m = ident(); m[3] = x; m[7] = y; m[11] = z; return m; }

    private static double[] scale(double x, double y, double z) { double[] m = ident(); m[0] = x; m[5] = y; m[10] = z; return m; }

    private static double[] rx(double deg) { double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg)); double[] m = ident(); m[5] = c; m[6] = -s; m[9] = s; m[10] = c; return m; }

    private static double[] ry(double deg) { double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg)); double[] m = ident(); m[0] = c; m[2] = s; m[8] = -s; m[10] = c; return m; }

    private static double[] rz(double deg) { double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg)); double[] m = ident(); m[0] = c; m[1] = -s; m[4] = s; m[5] = c; return m; }

    /** Inverse of an affine matrix (a rotation with a scale, and a shift). */
    private static double[] inverse(double[] m) {
        double a = m[0], b = m[1], c = m[2], d = m[4], e = m[5], f = m[6], g = m[8], h = m[9], i = m[10];
        double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        double[] r = ident();
        r[0] = (e * i - f * h) / det; r[1] = (c * h - b * i) / det; r[2] = (b * f - c * e) / det;
        r[4] = (f * g - d * i) / det; r[5] = (a * i - c * g) / det; r[6] = (c * d - a * f) / det;
        r[8] = (d * h - e * g) / det; r[9] = (b * g - a * h) / det; r[10] = (a * e - b * d) / det;
        for (int k = 0; k < 3; k++) r[k * 4 + 3] = -(r[k * 4] * m[3] + r[k * 4 + 1] * m[7] + r[k * 4 + 2] * m[11]);
        return r;
    }

    /**
     * Where a point of the ARM (the model part of the arm: y down the arm from the shoulder pivot, in blocks) lies on the shield's item frame, as a row-major 4 x 4 matrix.
     * The game draws a held item at {@code arm * hand * display * translate(-0.5)} ({@code ItemInHandLayer}: turn about x by -90, about y by 180, then {@code (+-1/16, 0.125, -0.625)}; then the
     * model's display of the hand; then the item renderer's {@code translate(-0.5)}), so the arm's own pose cancels and the matrix is {@code inverse(hand * display * translate(-0.5))}.
     */
    public static double[] armFrame(boolean left, boolean raised) {
        double[] d = ShieldPreset.of(left ? "thirdperson_lefthand" : "thirdperson_righthand", raised);
        double i = left ? -1 : 1, ryy = left ? -d[1] : d[1], rzz = left ? -d[2] : d[2];
        double[] display = mul(mul(mul(mul(trans(i * d[3] / 16, d[4] / 16, d[5] / 16), rx(d[0])), ry(ryy)), rz(rzz)), scale(d[6], d[7], d[8]));
        double[] hand = mul(mul(rx(-90), ry(180)), trans((left ? -1 : 1) / 16.0, 0.125, -0.625));
        return inverse(mul(mul(hand, display), trans(-0.5, -0.5, -0.5)));
    }

    /** {@code left * 2 + raised}. */
    public static int ctxOf(boolean left, boolean raised) { return (left ? 2 : 0) + (raised ? 1 : 0); }

    // ------------------------------------------------------------------ geometry
    /** Vertices as {x, y, z, u, v, nx, ny, nz} (u and v in texels of the 128 x 128 sheet), four per quad, in the lists of {@link #build}. */
    private static void box(List<float[]> out, double[][] p, int part, double sx, double sy, double sz) {
        // p[0..7]: the corners, bit 0 = x high, bit 1 = y high, bit 2 = z high
        double[] c = new double[3];
        for (double[] q : p) for (int k = 0; k < 3; k++) c[k] += q[k] / 8;
        int[][] faces = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};          // -x +x -y +y -z +z, any winding: corrected below
        int[][] axes = {{1, 2}, {1, 2}, {0, 2}, {0, 2}, {0, 1}, {0, 1}};
        double[] size = {sx, sy, sz};
        for (int f = 0; f < 6; f++) {
            int[] idx = faces[f];
            double[] n = new double[3], mid = new double[3];
            for (int k = 0; k < 4; k++) {
                double[] a = p[idx[k]], b = p[idx[(k + 1) % 4]];
                n[0] += (a[1] - b[1]) * (a[2] + b[2]);
                n[1] += (a[2] - b[2]) * (a[0] + b[0]);
                n[2] += (a[0] - b[0]) * (a[1] + b[1]);
                for (int j = 0; j < 3; j++) mid[j] += a[j] / 4;
            }
            if (n[0] * (mid[0] - c[0]) + n[1] * (mid[1] - c[1]) + n[2] * (mid[2] - c[2]) < 0) {
                idx = new int[]{idx[3], idx[2], idx[1], idx[0]};
                n[0] = -n[0]; n[1] = -n[1]; n[2] = -n[2];
            }
            double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (len < 1e-12) len = 1;
            float du = (float) (size[axes[f][0]] * PX_PER_BLOCK), dv = (float) (size[axes[f][1]] * PX_PER_BLOCK), u0 = UV0[part], v0 = 2f;
            float[][] uv = {{u0, v0}, {u0 + du, v0}, {u0 + du, v0 + dv}, {u0, v0 + dv}};
            for (int k = 0; k < 4; k++) {
                double[] a = p[idx[k]];
                out.add(new float[]{(float) a[0], (float) a[1], (float) a[2], uv[k][0], uv[k][1], (float) (n[0] / len), (float) (n[1] / len), (float) (n[2] / len)});
            }
        }
    }

    private static double[][] corners(double[] lo, double[] hi) {
        double[][] p = new double[8][];
        for (int i = 0; i < 8; i++) p[i] = new double[]{(i & 1) == 0 ? lo[0] : hi[0], (i & 2) == 0 ? lo[1] : hi[1], (i & 4) == 0 ? lo[2] : hi[2]};
        return p;
    }

    private static double[] apply(double[] m, double x, double y, double z) {
        return new double[]{m[0] * x + m[1] * y + m[2] * z + m[3], m[4] * x + m[5] * y + m[6] * z + m[7], m[8] * x + m[9] * y + m[10] * z + m[11]};
    }

    /**
     * The straps of one shield in one hand state, in the item frame (the frame the renderer is in before it moves the mesh): {@code [leather, trim]} vertex lists.
     * {@code anchorZ} is where the back of the plate lies on that frame (see {@link #plateBackZ}); {@code ctx} is {@link #ctxOf} or {@link #CTX_FIRST_PERSON}.
     */
    @SuppressWarnings("unchecked")
    public static List<float[]>[] build(Data d, int ctx, float anchorZ) {
        List<float[]>[] out = new List[]{new ArrayList<float[]>(), new ArrayList<float[]>()};
        if (ctx == CTX_FIRST_PERSON) {
            for (Cube c : d.firstPerson) {
                double[] lo = {c.from[0], c.from[1], c.from[2] + anchorZ}, hi = {c.to[0], c.to[1], c.to[2] + anchorZ};
                box(out[c.part], corners(lo, hi), c.part, hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]);
            }
            return out;
        }
        double[] f = armFrame(ctx >= 2, (ctx & 1) == 1);
        // the arm's "front": the arm-local x or z axis that points most toward the plate (frame +z)
        boolean useX = Math.abs(f[8]) >= Math.abs(f[10]);
        double sgn = (useX ? f[8] : f[10]) >= 0 ? 1 : -1;
        double[] ef = useX ? new double[]{sgn, 0, 0} : new double[]{0, 0, sgn};
        double[] ew = {ef[2], 0, -ef[0]};                                                                                      // ey x ef, with ey = (0,1,0)
        double[] local = ident();                                                                                              // canonical (cx, cy, cz) -> arm-local: columns ew, ey, ef
        for (int r = 0; r < 3; r++) { local[r * 4] = ew[r]; local[r * 4 + 1] = r == 1 ? 1 : 0; local[r * 4 + 2] = ef[r]; }
        double[] m = mul(f, local);
        for (Cube c : d.cubes) {
            double[][] p = corners(c.from, c.to);
            for (int i = 0; i < 8; i++) p[i] = apply(m, p[i][0], p[i][1], p[i][2]);
            box(out[c.part], p, c.part, c.to[0] - c.from[0], c.to[1] - c.from[1], c.to[2] - c.from[2]);
        }
        boolean wideX = Math.abs(f[1]) >= Math.abs(f[5]);                                                                      // the tab is wide along the arm's axis (as seen on the frame), thin across it
        for (double[] at : d.attach) {
            double[] q = apply(m, at[0], at[1], at[2]);
            double z0 = q[2] - d.overlap, z1 = anchorZ;
            if (z1 - z0 < 0.01) continue;                                                                                      // the ring already reaches the plate's back (a deep mesh): nothing to connect
            double wx = wideX ? d.tabWidth : d.tabThick, wy = wideX ? d.tabThick : d.tabWidth;
            double[] lo = {q[0] - wx / 2, q[1] - wy / 2, z0}, hi = {q[0] + wx / 2, q[1] + wy / 2, z1};
            box(out[LEATHER], corners(lo, hi), LEATHER, hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]);
            double[] rl = {q[0] - d.rivet / 2, q[1] - d.rivet / 2, z1 - d.rivetThick}, rh = {q[0] + d.rivet / 2, q[1] + d.rivet / 2, z1 + d.overlap};
            box(out[TRIM], corners(rl, rh), TRIM, rh[0] - rl[0], rh[1] - rl[1], rh[2] - rl[2]);
        }
        return out;
    }

    /** Where the back of the plate lies on the item frame: the fit's shift, the gap off the arm ({@link ShieldFrame#armGap}) and the mesh's own back z scaled by the fit. */
    public static float anchorZ(float[] fit, float plateBackMeshZ) { return fit[3] + ShieldFrame.armGap() + fit[0] * (Float.isNaN(plateBackMeshZ) ? 0f : plateBackMeshZ); }

    public static int quads(List<float[]>[] set) { return (set[0].size() + set[1].size()) / 4; }

    /**
     * The z (mesh space) of the back of the plate: the area-weighted median of the faces that look toward -z (the plate's back, a rim's back). {@code verts} are the mesh vertices
     * ({x, y, z, ...}, four per quad); NaN when no face looks backward.
     */
    public static float plateBackZ(List<float[]> verts) {
        List<float[]> zw = new ArrayList<>();
        double total = 0;
        for (int i = 0; i + 3 < verts.size(); i += 4) {
            float[] a = verts.get(i), b = verts.get(i + 1), c = verts.get(i + 2), e = verts.get(i + 3);
            double nx = 0, ny = 0, nz = 0, z = 0;
            float[][] q = {a, b, c, e};
            for (int k = 0; k < 4; k++) {
                float[] s = q[k], t = q[(k + 1) % 4];
                nx += (s[1] - t[1]) * (s[2] + t[2]);
                ny += (s[2] - t[2]) * (s[0] + t[0]);
                nz += (s[0] - t[0]) * (s[1] + t[1]);
                z += s[2] / 4.0;
            }
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-9) continue;
            double look = a.length >= 8 ? (a[7] + b[7] + c[7] + e[7]) / 4.0 : nz / len;                                         // the mesh's own normal when it has one, else the winding's
            if (look > -0.8) continue;                                                                                         // only a face that looks toward -z is a back
            zw.add(new float[]{(float) z, (float) (len / 2)});
            total += len / 2;
        }
        if (zw.isEmpty()) return Float.NaN;
        zw.sort((p, q) -> Float.compare(p[0], q[0]));
        double acc = 0;
        for (float[] e : zw) {
            acc += e[1];
            if (acc >= total / 2) return e[0];
        }
        return zw.get(zw.size() - 1)[0];
    }
}
