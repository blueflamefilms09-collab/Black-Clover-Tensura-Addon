package com.newuniverse.nusmp.client.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Why (docs/wings_on_back.md): the ONE convention for a wing worn on a player, and the one fix for the wings that broke it. The owner's report: the wings of the Devil Union (and of the other forms) stood at the
 * sides of the body and swept FORWARD past it, instead of sitting on the back and sweeping backward like the Djinn, Corundum and mode-armour wings.
 *
 * <p><b>The convention</b> (what every correct wing of the mod does, see the doc): in the humanoid frame of a geo file (pixels, y up, the front on the -z side, so the BACK is +z, the torso's back face at
 * z = {@link #BACK_PLANE_Z}) a wing is hinged at the shoulder blade ({@code |x|} about 2 to 3, z at or behind {@link #MIN_PIVOT_Z}, in practice 2.6 to 3.4) and its mass lies BEHIND the hinge: the centroid of the wing is
 * further back than the hinge ({@code dz >= 0}), the tip never ends in front of the hinge, and a spread wing's tip is further out than the hinge. In vanilla's rotation order that means the LEFT wing (the +x side)
 * turns about the vertical by a NEGATIVE y angle and the right wing by a positive one (the Corundum wings: {@code wL ry = -24}, {@code wR ry = +24}; the Djinn layer: {@code YP.rotationDegrees(-side * sweep)}).
 *
 * <p><b>What was wrong</b>: the generator of the Black Clover and devil looks ({@code tools/bc_playermodels/bcparts3.py}, {@code wing()}) and the generators of the Dark, Flame, Lightning and Water armours, of the
 * Spirit Dive fire and saint wings and of the Noelle Valkyrie second pair wrote the sweep with the wrong sign (+y for the left wing), so every one of those wings leans toward the face. {@link GeoDraw} draws exactly what
 * the file says, so the files were right about nothing but the shape.
 *
 * <p><b>The fix</b>: when a model is drawn on a player ({@link GeoDraw.Space#PLAYER}) and the client option {@code visual.wingsOnBack} is on (the default), {@link GeoModelData#onBack()} turns every wing group that
 * is hinged on the back but sweeps to the front by reflecting it in the plane z = its hinge: the same wing, the same size, now behind. Wings that already follow the convention are not touched (the model is returned
 * as it is, no copy). Off = the files exactly as drawn before. Pure (no game classes): the unit test {@code WingsOnBackTest} runs the same forward kinematics over every worn wing of the mod.
 *
 * <p>Which bones are wings: a bone whose name contains "wing", or, in a file that is a wing piece ({@code ..._wing_l}, {@code ..._wings2}, {@code ..._wingbud_r}), every root bone of the file. A bone that is not at
 * the back (hinge z below {@link #MIN_PIVOT_Z}: a chest emblem, an arm blade) is never turned.
 */
public final class WingFrame {
    private WingFrame() {}

    /** The back face of the torso in a geo file (pixels): the body box spans z = -2 to +2, the front is -z. */
    public static final float BACK_PLANE_Z = 2f;
    /** A wing is hinged at or behind this z (pixels); a hinge in front of it is not a back piece and is left alone. */
    public static final float MIN_PIVOT_Z = 1f;
    /** A wing group whose centroid is more than this many pixels IN FRONT of its hinge sweeps forward (the wings of the report: 2.2 to 9 px; the wings that are right: never below -0.4). */
    public static final float FORWARD_DZ = -1.5f;

    /** ... or whose tip ends more than this many pixels in front of its hinge (the tall Valkyrie, Dragon and Beast wings lean 2.5 to 6 px forward with the centroid still within the first limit). */
    public static final float FORWARD_TIP_DZ = -2f;

    private static volatile boolean onBack = true;

    /** The client option {@code visual.wingsOnBack}, mirrored here by {@code WingsOnBackConfig} (render thread reads it, the config thread writes it). */
    public static void setOnBack(boolean on) { onBack = on; }

    public static boolean isOnBack() { return onBack; }

    private static final Pattern WING_BONE = Pattern.compile("wing", Pattern.CASE_INSENSITIVE);
    private static final Pattern WING_FILE = Pattern.compile(".*_wings?\\d*(?:bud)?(?:_[lr])?");

    /** True for a bone that is the root of a wing by its name. */
    public static boolean isWingBone(String name) { return WING_BONE.matcher(name).find(); }

    /** True for a geo file that is a wing piece of an armour or form ({@code dark_arm_wing_l}, {@code nvl_arm_wings2}, {@code spiritdive_arm_fire_wingbud_r}); {@code id} is a resource location or a file name. */
    public static boolean isWingFile(String id) {
        int s = id.lastIndexOf('/');
        String n = s >= 0 ? id.substring(s + 1) : id;
        if (n.endsWith(".geo.json")) n = n.substring(0, n.length() - ".geo.json".length());
        return WING_FILE.matcher(n).matches();
    }

    /** One wing (a wing bone and everything under it) in the rest pose: where it is hinged, where its mass is, and its farthest corner, in the geo frame (x to the model's left, y UP, z to the back). */
    public record Group(String root, double[] pivot, double[] centroid, double[] tip, int cubes) {
        /** How far behind its hinge the wing's centroid lies (negative = in front of it). */
        public double dz() { return centroid[2] - pivot[2]; }

        /** How far behind its hinge the farthest corner lies. */
        public double tipDz() { return tip[2] - pivot[2]; }

        /** True when the wing is hinged on the back but its mass sits in front of the hinge: the bug of the report. */
        public boolean sweepsForward() { return pivot[2] >= MIN_PIVOT_Z && (dz() < FORWARD_DZ || tipDz() < FORWARD_TIP_DZ); }
    }

    /** Every wing of the model (the roots the model draws, so a view of one anchor lists only its own wings), in rest pose. Allocates; for loading and tests. */
    public static List<Group> groups(GeoModelData m) {
        List<Group> out = new ArrayList<>();
        boolean wingFile = isWingFile(m.id);
        double[] unit = {1, 0, 0, 0, 1, 0, 0, 0, 1};
        for (GeoModelData.Bone r : m.roots) visit(r, null, unit, new double[3], wingFile, true, null, out);
        return out;
    }

    /** A bare anchor bone of a wing piece (no cubes, no turn, pivot at the origin: the {@code root} of {@code dark_arm_wing_l}); the hinge is the first bone below it. */
    private static boolean isAnchor(GeoModelData.Bone b) {
        return b.cubes.isEmpty() && b.px == 0f && b.py == 0f && b.pz == 0f && b.rx == 0f && b.ry == 0f && b.rz == 0f && !isWingBone(b.name);
    }

    /**
     * The model with its forward wings turned to the back (a deep copy, the original untouched), or null when no wing of it sweeps forward. Reflection in z about the hinge plane z = {@code pz} of the wing's root
     * bone: every bone and cube of the wing moves to {@code z' = 2 pz - z}, and its x and y turns change sign (a z turn is unchanged by a mirror in z).
     */
    public static GeoModelData fix(GeoModelData m) {
        List<String> bad = null;
        for (Group g : groups(m)) if (g.sweepsForward()) (bad == null ? bad = new ArrayList<>() : bad).add(g.root());
        if (bad == null) return null;
        GeoModelData c = m.copy();
        for (String name : bad) {
            GeoModelData.Bone rb = c.bones.get(name);
            if (rb != null) reflect(rb, rb.pz);
        }
        c.markBackChecked();
        return c;
    }

    private static void reflect(GeoModelData.Bone b, float pz) {
        b.pz = 2f * pz - b.pz;
        b.rx = -b.rx;
        b.ry = -b.ry;
        b.zMirrored = true;
        for (GeoModelData.Cube c : b.cubes) {
            c.oz = 2f * pz - c.oz - c.sz;
            if (c.hasRot) {
                c.pz = 2f * pz - c.pz;
                c.rx = -c.rx;
                c.ry = -c.ry;
            }
        }
        for (GeoModelData.Bone ch : b.children) reflect(ch, pz);
    }

    // ================================================================ forward kinematics (the arithmetic of GeoDraw, in doubles)
    private static final class Acc {
        final String root;
        final double[] pivot;
        final double[] sum = new double[3];
        int n, cubes;
        final List<double[]> corners = new ArrayList<>();

        Acc(String root, double[] pivot) { this.root = root; this.pivot = pivot; }

        void add(double[] p) {
            sum[0] += p[0]; sum[1] += p[1]; sum[2] += p[2];
            n++;
        }

        Group finish() {
            double[] cen = n == 0 ? pivot.clone() : new double[]{sum[0] / n, -sum[1] / n, sum[2] / n};
            double[] pv = {pivot[0], -pivot[1], pivot[2]};
            double[] t = pv;
            double best = -1;
            for (double[] c : corners) {
                double d = (c[0] - pv[0]) * (c[0] - pv[0]) + (c[1] - pv[1]) * (c[1] - pv[1]) + (c[2] - pv[2]) * (c[2] - pv[2]);
                if (d > best) { best = d; t = c; }
            }
            return new Group(root, pv, cen, t.clone(), cubes);
        }

    }

    /** Vanilla's rotation of a file bone: Rz(-rz) * Ry(ry) * Rx(-rx), degrees, as GeoDraw.rotate builds it (row major). */
    private static double[] rot(double rxDeg, double ryDeg, double rzDeg) {
        double a = -rxDeg * Math.PI / 180.0, b = ryDeg * Math.PI / 180.0, g = -rzDeg * Math.PI / 180.0;
        double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b), cg = Math.cos(g), sg = Math.sin(g);
        return new double[]{
                cg * cb, cg * sb * sa - sg * ca, cg * sb * ca + sg * sa,
                sg * cb, sg * sb * sa + cg * ca, sg * sb * ca - cg * sa,
                -sb, cb * sa, cb * ca};
    }

    private static double[] mul(double[] a, double[] b) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
        return r;
    }

    private static double[] apply(double[] m, double[] t, double x, double y, double z) {
        return new double[]{t[0] + m[0] * x + m[1] * y + m[2] * z, t[1] + m[3] * x + m[4] * y + m[5] * z, t[2] + m[6] * x + m[7] * y + m[8] * z};
    }

    /**
     * Works in vanilla's y-down frame (x, -y, z) like the drawing does; {@code pm}/{@code pt} are the parent bone's rotation and pivot position there. The group accumulates the corners of every cube under its root;
     * {@link Acc#finish} turns the y back up.
     */
    private static void visit(GeoModelData.Bone b, GeoModelData.Bone parent, double[] pm, double[] pt, boolean wingFile, boolean anchored, Acc grp, List<Group> out) {
        double[] r = (b.rx != 0f || b.ry != 0f || b.rz != 0f) ? rot(b.rx, b.ry, b.rz) : new double[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
        double[] t, m;
        if (parent == null) {
            t = new double[]{b.px, -b.py, b.pz};
            m = r;
        } else {
            t = apply(pm, pt, b.px - parent.px, -b.py + parent.py, b.pz - parent.pz);
            m = mul(pm, r);
        }
        Acc g = grp;
        boolean mine = false;
        if (g == null && (isWingBone(b.name) || (wingFile && anchored && !isAnchor(b) && !b.name.startsWith("x_")))) {
            g = new Acc(b.name, new double[]{t[0], t[1], t[2]});
            mine = true;
        }
        if (g != null) {
            for (GeoModelData.Cube c : b.cubes) {
                double e = c.inflate;
                double minX = c.ox - b.px - e, maxX = c.ox - b.px + c.sx + e;
                double minY = -(c.oy + c.sy) + b.py - e, maxY = -c.oy + b.py + e;
                double minZ = c.oz - b.pz - e, maxZ = c.oz - b.pz + c.sz + e;
                double[] cm = null;
                double cx = 0, cy = 0, cz = 0;
                if (c.hasRot && (c.rx != 0f || c.ry != 0f || c.rz != 0f)) {
                    cm = rot(c.rx, c.ry, c.rz);
                    cx = c.px - b.px; cy = -c.py + b.py; cz = c.pz - b.pz;
                }
                for (int i = 0; i < 8; i++) {
                    double x = (i & 1) == 0 ? minX : maxX, y = (i & 2) == 0 ? minY : maxY, z = (i & 4) == 0 ? minZ : maxZ;
                    if (cm != null) {
                        double dx = x - cx, dy = y - cy, dz = z - cz;
                        x = cx + cm[0] * dx + cm[1] * dy + cm[2] * dz;
                        y = cy + cm[3] * dx + cm[4] * dy + cm[5] * dz;
                        z = cz + cm[6] * dx + cm[7] * dy + cm[8] * dz;
                    }
                    double[] w = apply(m, t, x, y, z);
                    g.add(w);
                    g.corners.add(new double[]{w[0], -w[1], w[2]});
                }
                g.cubes++;
            }
        }
        boolean childAnchored = anchored && isAnchor(b);
        for (GeoModelData.Bone ch : b.children) visit(ch, b, m, t, wingFile, childAnchored, g, out);
        if (mine) out.add(g.finish());
    }
}
