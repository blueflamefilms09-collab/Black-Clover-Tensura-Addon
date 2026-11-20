package com.newuniverse.nusmp.visual.grip;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 4.45.0 (docs/weapon_hold.md): the hero weapons (Legendary, Seven Star, Vassal; every form) are held so that the grip lands on the hand (docs/weapon_grip.md) AND nothing of the
 * weapon passes through the forearm or the torso beside it. Pure (no Minecraft imports); tools/weapon_grip/hold_audit.py is the Python twin, {@code HoldAuditTest} compares them.
 * <p>
 * HAND FRAME (the pose {@code ItemInHandLayer.renderArmWithItem} leaves before the item's display block; {@code HandWrap} documents the same one): origin at the end of the arm on its front
 * face, +x outward (right hand), +y forward, +z up the arm. The forearm there is the box x +-2px, y -4..0px, z 0..12px; the torso lies beside it at x -10..-2px (mirrored for the left
 * hand). A point {@code c} of the item's unit cube is at {@code t/16 + s R (c - 0.5)} in it (the mirrored entry for the left hand). An armour stand and an item display with a hand
 * context run the same chain, so the same numbers hold for them.
 * <p>
 * The rule ({@link #extra}): after the grip has landed on the hand, (1) a weapon that reaches into the torso is moved sideways, away from the body, by the depth plus half a pixel (at
 * most {@link #MAX_OUT}), as far as the hand still reaches it; (2) a weapon with a part inside the forearm further than {@link #ARM_REACH} from the hand (vanilla's own sword reaches
 * 0.30) is slid along its own axis (the hand toward the butt) or moved forward, whichever clears most and still leaves the hand within {@link #FLOAT_MAX} of the surface. Nothing else moves.
 * It runs once per mesh when the mesh is loaded, over at most {@link #MAX_SAMPLES} surface samples (a few thousand operations each), and the two shifts are cached like the grip's.
 */
public final class HoldFit {
    private HoldFit() {}

    public static final float PX = 1f / 16f;
    /** The hand holds something: the nearest surface point of the weapon is no further than this from the hand origin (blocks). */
    public static final float FLOAT_MAX = 0.10f;
    /** An item display with no transform turns about the cube's centre; a pivot this near the weapon is a hand's reach. */
    public static final float NONE_MAX = 0.15f;
    /** The farthest a weapon part inside the forearm box may be from the hand origin (vanilla's own sprite sword: 0.30). */
    public static final float ARM_REACH = 0.34f;
    /** How far into the torso a point may reach without counting (the arm hangs a hair away from the body). */
    public static final float TORSO_SLACK = 0.5f * PX;
    public static final float MAX_OUT = 0.20f, OUT_MARGIN = 0.5f * PX, MAX_SLIDE = 0.08f, SLIDE_MARGIN = 0.5f * PX;
    /** The most the rule can move a weapon from where the grip landed (blocks): the sideways move plus the larger of the slide and the forward move. */
    public static final float MAX_MOVE = MAX_OUT + MAX_SLIDE + 0.06f;
    /** Surface samples at most this far apart (blocks), and at most this many per mesh (the step grows past it). */
    public static final float STEP = 0.03f;
    public static final int MAX_SAMPLES = 60000;
    /** The lowest point of a first-person weapon on screen must be below this (-1 = the bottom edge of the screen; vanilla's sword: -1.29). */
    public static final float LOW_MAX = -1.0f;

    /** Forearm box {x0, x1, y0, y1, z0, z1} of the player (right hand) and of an armour stand. */
    public static final float[] ARM_BOX = {-2 * PX, 2 * PX, -4 * PX, 0f, 0f, 12 * PX};
    public static final float[] STAND_ARM_BOX = {-2 * PX, 0f, -3 * PX, -1 * PX, 0f, 12 * PX};
    /** The torso's face toward the arm and its far side, measured toward the body; its y and z range is the arm's. */
    public static final float TORSO_NEAR = 2 * PX, TORSO_FAR = 10 * PX, TORSO_Y0 = -4 * PX, TORSO_Y1 = 0f, TORSO_Z0 = 0f, TORSO_Z1 = 12 * PX;

    /** The weapons worn on the hand (client/relics/item/HandWrap), which have their own frame. */
    private static final Set<String> WORN = Set.of("vassal_gauntlet", "vassal_claw");
    private static final Pattern PULL = Pattern.compile("_pull[123]$");

    /**
     * Does the rule apply to this mesh? The hero weapons' held meshes: not a worn one, not a draw state of a bow (the arm is raised while it is drawn, so the idle arm is not the picture).
     * Every other weapon keeps the placement of 4.44.x.
     */
    public static boolean applies(String meshId) {
        if (meshId == null || !(meshId.startsWith("legendary_") || meshId.startsWith("vassal_"))) return false;
        int form = meshId.indexOf("__");
        String item = form > 0 ? meshId.substring(0, form) : meshId;
        return !WORN.contains(item) && !PULL.matcher(meshId).find();
    }

    // ================================================================ samples
    /**
     * Surface samples of a mesh's quads in mesh space (x y z per point): every quad (four consecutive vertices a b c d) is sampled on a grid at most {@link #STEP} blocks apart ({@code unit}
     * = blocks per mesh unit), the grid at most 60 cells a side; the step grows when the total would pass {@link #MAX_SAMPLES}.
     */
    public static float[] surfaceSamples(List<float[]> verts, float unit) {
        float step = STEP;
        for (int attempt = 0; attempt < 6; attempt++) {
            long total = 0;
            for (int i = 0; i + 3 < verts.size(); i += 4) total += (long) (cells(verts, i, 0, unit, step) + 1) * (cells(verts, i, 1, unit, step) + 1);
            if (total <= MAX_SAMPLES) break;
            step *= (float) Math.sqrt((double) total / MAX_SAMPLES) * 1.05f;
        }
        int count = 0;
        for (int i = 0; i + 3 < verts.size(); i += 4) count += (cells(verts, i, 0, unit, step) + 1) * (cells(verts, i, 1, unit, step) + 1);
        float[] out = new float[count * 3];
        int o = 0;
        for (int i = 0; i + 3 < verts.size(); i += 4) {
            float[] a = verts.get(i), b = verts.get(i + 1), c = verts.get(i + 2), d = verts.get(i + 3);
            int nu = cells(verts, i, 0, unit, step), nv = cells(verts, i, 1, unit, step);
            for (int iu = 0; iu <= nu; iu++) {
                float s = (float) iu / nu;
                for (int iv = 0; iv <= nv; iv++) {
                    float t = (float) iv / nv;
                    for (int k = 0; k < 3; k++) out[o++] = (1 - s) * ((1 - t) * a[k] + t * d[k]) + s * ((1 - t) * b[k] + t * c[k]);
                }
            }
        }
        return out;
    }

    /** Cells along a quad's two directions: 0 = a to b (and d to c), 1 = a to d (and b to c); at least 1, at most 60. */
    private static int cells(List<float[]> v, int i, int dir, float unit, float step) {
        float[] a = v.get(i), b = v.get(i + 1), c = v.get(i + 2), d = v.get(i + 3);
        float len = dir == 0 ? Math.max(dist(a, b), dist(d, c)) : Math.max(dist(a, d), dist(b, c));
        return Math.max(1, Math.min(60, (int) Math.ceil(len * unit / step)));
    }

    private static float dist(float[] p, float[] q) {
        float x = p[0] - q[0], y = p[1] - q[1], z = p[2] - q[2];
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    // ================================================================ hand frame
    /** The hand-frame position (blocks) of every unit-cube point {@code cube} (x y z each) under the entry {@code d} (mirrored for the left hand): {@code t/16 + s R (c - 0.5)}. */
    public static float[] toHand(float[] cube, int n, GripMath.Display d, boolean left) {
        GripMath.Display m = GripMath.mirrored(d, left);
        float[] r = GripMath.rotation(m.rx(), m.ry(), m.rz());
        float tx = m.tx() / 16f, ty = m.ty() / 16f, tz = m.tz() / 16f, s = m.scale();
        float[] q = new float[n * 3];
        for (int i = 0; i < n; i++) {
            float x = s * (cube[i * 3] - 0.5f), y = s * (cube[i * 3 + 1] - 0.5f), z = s * (cube[i * 3 + 2] - 0.5f);
            q[i * 3] = tx + r[0] * x + r[1] * y + r[2] * z;
            q[i * 3 + 1] = ty + r[3] * x + r[4] * y + r[5] * z;
            q[i * 3 + 2] = tz + r[6] * x + r[7] * y + r[8] * z;
        }
        return q;
    }

    /** The nearest weapon point to the hand origin after the move {@code m} (hand frame), blocks. */
    public static float floatDistance(float[] q, int n, float[] m) {
        float best = Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float x = q[i * 3] + m[0], y = q[i * 3 + 1] + m[1], z = q[i * 3 + 2] + m[2];
            best = Math.min(best, x * x + y * y + z * z);
        }
        return (float) Math.sqrt(best);
    }

    /** The farthest point of the weapon inside the arm box {x0..z1} (mirrored in x for the left hand) from the hand origin after the move {@code m}; 0 when none is inside. */
    public static float armReach(float[] q, int n, float[] m, boolean left, float[] box) {
        float x0 = left ? -box[1] : box[0], x1 = left ? -box[0] : box[1], best = 0f;
        for (int i = 0; i < n; i++) {
            float x = q[i * 3] + m[0], y = q[i * 3 + 1] + m[1], z = q[i * 3 + 2] + m[2];
            if (x > x0 && x < x1 && y > box[2] && y < box[3] && z > box[4] && z < box[5]) best = Math.max(best, x * x + y * y + z * z);
        }
        return (float) Math.sqrt(best);
    }

    /** How far past the torso's face the deepest weapon point reaches after the move {@code m} (0 = none; the first {@link #TORSO_SLACK} do not count), blocks. */
    public static float torsoDepth(float[] q, int n, float[] m, boolean left) {
        float best = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float sx = (left ? q[i * 3] + m[0] : -(q[i * 3] + m[0]));
            float y = q[i * 3 + 1] + m[1], z = q[i * 3 + 2] + m[2];
            if (sx > TORSO_NEAR + TORSO_SLACK && sx < TORSO_FAR && y > TORSO_Y0 && y < TORSO_Y1 && z > TORSO_Z0 && z < TORSO_Z1) best = Math.max(best, sx);
        }
        return best == -Float.MAX_VALUE ? 0f : best - TORSO_NEAR;
    }

    // ================================================================ the rule
    private static final float[] ZERO = {0f, 0f, 0f};

    /**
     * The extra translation of the unit cube (applied with the grip's shift, before the placement) that clears the torso and the forearm. {@code cube} = the weapon's surface samples
     * in the unit cube as drawn with the grip landed on the hand ({@code n} points), {@code d} the hand's display entry, {@code axX, axY} the weapon's axis (toward the tip) in the cube.
     * Returns {dx, dy, dz} (zero when nothing needs to move).
     */
    public static float[] extra(float[] cube, int n, GripMath.Display d, boolean left, float axX, float axY) {
        GripMath.Display mir = GripMath.mirrored(d, left);
        if (!(mir.scale() > 0f) || n == 0) return new float[]{0f, 0f, 0f};
        float[] q = toHand(cube, n, d, left);
        float[] dq = {0f, 0f, 0f};
        float depth = torsoDepth(q, n, ZERO, left);
        if (depth > 0f) {
            float out = Math.min(MAX_OUT, depth + OUT_MARGIN);
            for (float f : new float[]{1f, 0.75f, 0.5f, 0.25f}) {                       // as far out as the hand can still reach the weapon
                float[] m = {(left ? -1f : 1f) * out * f, 0f, 0f};
                if (floatDistance(q, n, m) <= FLOAT_MAX) {
                    dq = m;
                    break;
                }
            }
        }
        float reach = armReach(q, n, dq, left, ARM_BOX);
        if (reach > ARM_REACH) {
            float slide = Math.min(MAX_SLIDE, reach - ARM_REACH + SLIDE_MARGIN);
            float[] r = GripMath.rotation(mir.rx(), mir.ry(), mir.rz());
            float vx = r[0] * axX + r[1] * axY, vy = r[3] * axX + r[4] * axY, vz = r[6] * axX + r[7] * axY;          // the weapon's axis in the hand frame (cube z has none)
            float vl = Math.max((float) Math.sqrt(vx * vx + vy * vy + vz * vz), 1e-9f);
            float[][] steps = {{slide * vx / vl, slide * vy / vl, slide * vz / vl}, {0.5f * slide * vx / vl, 0.5f * slide * vy / vl, 0.5f * slide * vz / vl},
                    {0.25f * slide * vx / vl, 0.25f * slide * vy / vl, 0.25f * slide * vz / vl}, {0f, 0.02f, 0f}, {0f, 0.04f, 0f}, {0f, 0.06f, 0f}};      // slide along the axis, or move forward out of the arm
            float best = reach;
            float[] pick = null;
            for (float[] st : steps) {
                float[] m = {dq[0] + st[0], dq[1] + st[1], dq[2] + st[2]};
                float rr = armReach(q, n, m, left, ARM_BOX);
                if (rr < best && floatDistance(q, n, m) <= FLOAT_MAX) {
                    best = rr;
                    pick = m;
                }
            }
            if (pick != null) dq = pick;
        }
        // the hand-frame move back to the unit cube: R^T dq / s
        float[] r = GripMath.rotation(mir.rx(), mir.ry(), mir.rz());
        float s = mir.scale();
        return new float[]{(r[0] * dq[0] + r[3] * dq[1] + r[6] * dq[2]) / s, (r[1] * dq[0] + r[4] * dq[1] + r[7] * dq[2]) / s, (r[2] * dq[0] + r[5] * dq[1] + r[8] * dq[2]) / s};
    }
}
