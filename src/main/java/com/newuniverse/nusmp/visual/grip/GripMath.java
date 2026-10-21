package com.newuniverse.nusmp.visual.grip;

/**
 * 1.111.0 (docs/weapon_grip.md): the pure maths of "the hand holds the grip". No Minecraft imports; tools/weapon_grip/grip_lib.py is the Python twin.
 * <p>
 * An item model json's {@code display} entry (rotation r in degrees, translation t in sixteenths, scale s) is applied by vanilla as
 * {@code M = T(t/16) * R * S(s) * T(-0.5)} after the holder's own transform, with R = Rx * Ry * Rz (JOML {@code rotationXYZ}); a left-hand entry is mirrored first (y and z
 * rotation and the x translation change sign). The model's point {@code p} therefore lies at {@code hand + t/16 + R * s * (p - 0.5)}, so the point of the item's unit cube
 * that sits exactly at the hand is {@link #handPoint}: {@code H = 0.5 - (1/s) * R^-1 * (t/16)}. A weapon mesh is held right when its grip point is H.
 * <p>
 * Before 1.111.0 nothing looked at H: the mesh was put on the old sprite's diagonal (butt at the sprite's low corner), so the hand closed on whatever part of the weapon lay
 * at H, a quarter to a third of the way up its length.
 */
public final class GripMath {
    private GripMath() {}

    /** An item-model display entry: rotation (degrees), translation (sixteenths of a block), uniform scale. {@link #NONE} = vanilla's NO_TRANSFORM. */
    public record Display(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
        public static final Display NONE = new Display(0, 0, 0, 0, 0, 0, 1);
    }

    /** vanilla item/handheld, first person, the right hand (display_presets.py BASE_*): the transform of the ordinary sprite sword whose hilt the oversize rule keeps. */
    public static final float BASE_SCALE = 0.68f;
    public static final float[] BASE_TRANSLATION = {1.13f, 3.2f, 1.13f};
    /** Where the first-person hand is (camera space) for the right hand: ItemInHandRenderer.applyItemArmTransform. */
    public static final float[] FP_HAND = {0.56f, -0.52f, -0.72f};
    /** The hilt of an ordinary sprite sword in the unit cube. */
    public static final float[] SPRITE_HILT = {0.25f, 0.25f, 0.5f};

    /** Row-major 3x3 of R = Rx * Ry * Rz for degrees. */
    public static float[] rotation(float rx, float ry, float rz) {
        double ax = Math.toRadians(rx), ay = Math.toRadians(ry), az = Math.toRadians(rz);
        double cx = Math.cos(ax), sx = Math.sin(ax), cy = Math.cos(ay), sy = Math.sin(ay), cz = Math.cos(az), sz = Math.sin(az);
        double[][] a = {{1, 0, 0}, {0, cx, -sx}, {0, sx, cx}}, b = {{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}}, c = {{cz, -sz, 0}, {sz, cz, 0}, {0, 0, 1}};
        double[][] ab = mul(a, b), abc = mul(ab, c);
        float[] r = new float[9];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) r[i * 3 + j] = (float) abc[i][j];
        return r;
    }

    private static double[][] mul(double[][] a, double[][] b) {
        double[][] o = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) o[i][j] += a[i][k] * b[k][j];
        return o;
    }

    /** The entry as vanilla applies it: a left-hand entry is mirrored. */
    public static Display mirrored(Display d, boolean left) {
        return left ? new Display(d.rx, -d.ry, -d.rz, -d.tx, d.ty, d.tz, d.scale) : d;
    }

    /** R^-1 * v (R is a rotation, so R^-1 = R^T). */
    private static float[] unrotate(float[] r, float x, float y, float z) {
        return new float[]{r[0] * x + r[3] * y + r[6] * z, r[1] * x + r[4] * y + r[7] * z, r[2] * x + r[5] * y + r[8] * z};
    }

    /** The point of the item's unit cube that lies at the hand origin under the entry (see the class comment). */
    public static float[] handPoint(Display d, boolean left) {
        Display m = mirrored(d, left);
        if (!(m.scale > 0f)) return new float[]{0.5f, 0.5f, 0.5f};
        float[] v = unrotate(rotation(m.rx, m.ry, m.rz), m.tx / 16f, m.ty / 16f, m.tz / 16f);
        return new float[]{0.5f - v[0] / m.scale, 0.5f - v[1] / m.scale, 0.5f - v[2] / m.scale};
    }

    /**
     * First person: the point of the unit cube that lies where an ordinary sprite sword's hilt lies (display_presets.anchor), i.e. where the grip of any weapon is put so that the
     * weapon is held like a sword. {@code ANCHOR = HAND + T0 + R * (s0 * (hilt - 0.5))} in camera space, and the point that is at ANCHOR under the entry {@code d} is
     * {@code 0.5 + (1/s) * R^-1 * (ANCHOR - HAND - t/16)}. The left hand is mirrored in x like everything else.
     */
    public static float[] firstPersonTarget(Display d, boolean left) {
        Display m = mirrored(d, left);
        if (!(m.scale > 0f)) return new float[]{0.5f, 0.5f, 0.5f};
        float sx = left ? -1f : 1f;
        float[] base = rotation(m.rx, m.ry, m.rz);                       // the rotation of the entry is the same one the vanilla sword uses (display_presets never changes it)
        float ax = sx * BASE_TRANSLATION[0] / 16f + base[0] * BASE_SCALE * (SPRITE_HILT[0] - 0.5f) + base[1] * BASE_SCALE * (SPRITE_HILT[1] - 0.5f) + base[2] * BASE_SCALE * (SPRITE_HILT[2] - 0.5f);
        float ay = BASE_TRANSLATION[1] / 16f + base[3] * BASE_SCALE * (SPRITE_HILT[0] - 0.5f) + base[4] * BASE_SCALE * (SPRITE_HILT[1] - 0.5f) + base[5] * BASE_SCALE * (SPRITE_HILT[2] - 0.5f);
        float az = BASE_TRANSLATION[2] / 16f + base[6] * BASE_SCALE * (SPRITE_HILT[0] - 0.5f) + base[7] * BASE_SCALE * (SPRITE_HILT[1] - 0.5f) + base[8] * BASE_SCALE * (SPRITE_HILT[2] - 0.5f);
        // ANCHOR - HAND = T0 + R s0 (hilt - P); minus this entry's own t gives the offset of the cube point from the hand in camera space
        float[] v = unrotate(base, ax - m.tx / 16f, ay - m.ty / 16f, az - m.tz / 16f);
        return new float[]{0.5f + v[0] / m.scale, 0.5f + v[1] / m.scale, 0.5f + v[2] / m.scale};
    }

    /** A mesh point (mesh units) in the item's unit cube under WeaponRenderer's placement: translate(ox, oy, 0.5), rotateZ(angle - 90), scale, translate(0, -ymin, 0). */
    public static float[] cubePoint(float ox, float oy, float angleDeg, float scale, float ymin, float mx, float my, float mz) {
        double a = Math.toRadians(angleDeg);
        double along = (my - ymin) * scale, across = mx * scale;
        return new float[]{(float) (ox + along * Math.cos(a) + across * Math.sin(a)), (float) (oy + along * Math.sin(a) - across * Math.cos(a)), 0.5f + mz * scale};
    }

    /** The translation (in the unit cube, applied before the placement) that puts the grip point on {@code target}. */
    public static float[] shift(float[] place, float[] grip, float[] target) {
        float[] g = cubePoint(place[0], place[1], place[2], place[3], place[4], grip[0], grip[1], grip[2]);
        return new float[]{target[0] - g[0], target[1] - g[1], target[2] - g[2]};
    }

    /** The camera bounds of tools/relics/display_presets.py (the first-person rule): every corner of the mesh box at least Z_MARGIN in front, the tip inside the view cone. */
    public static final float Z_MARGIN = 0.16f, Y_VIEW = (float) (0.92 * Math.tan(Math.toRadians(35.0))), X_VIEW = 1.32f;

    /** Vanilla's resting first-person hand (camera space) for the right and the left hand. */
    public static float[] firstPersonHand(boolean left) { return new float[]{left ? -FP_HAND[0] : FP_HAND[0], FP_HAND[1], FP_HAND[2]}; }

    /** A point of the unit cube in first-person camera space under the entry: HAND + t/16 + R * s * (p - 0.5). */
    public static float[] camera(Display d, boolean left, float px, float py, float pz) {
        Display m = mirrored(d, left);
        float[] r = rotation(m.rx, m.ry, m.rz), h = firstPersonHand(left);
        float x = m.scale * (px - 0.5f), y = m.scale * (py - 0.5f), z = m.scale * (pz - 0.5f);
        return new float[]{h[0] + m.tx / 16f + r[0] * x + r[1] * y + r[2] * z, h[1] + m.ty / 16f + r[3] * x + r[4] * y + r[5] * z, h[2] + m.tz / 16f + r[6] * x + r[7] * y + r[8] * z};
    }

    /** Is the mesh (box {x0, x1, y0, y1, z0, z1} in mesh space, placed by {@code place}, moved by {@code delta} in the cube) in view in first person under the entry? */
    public static boolean inView(float[] place, float[] box, float[] delta, Display d, boolean left) {
        for (int i = 0; i < 8; i++) {
            float[] c = cubePoint(place[0], place[1], place[2], place[3], place[4], box[i & 1], box[2 + (i >> 1 & 1)], box[4 + (i >> 2 & 1)]);
            if (camera(d, left, c[0] + delta[0], c[1] + delta[1], c[2] + delta[2])[2] > -Z_MARGIN) return false;
        }
        float[] tip = cubePoint(place[0], place[1], place[2], place[3], place[4], (box[0] + box[1]) / 2f, box[3], (box[4] + box[5]) / 2f);
        float[] c = camera(d, left, tip[0] + delta[0], tip[1] + delta[1], tip[2] + delta[2]);
        float depth = -c[2];
        return depth >= Z_MARGIN - 1e-6f && Math.abs(c[1]) <= Y_VIEW * depth + 1e-6f && Math.abs(c[0]) <= X_VIEW * depth + 1e-6f;
    }

    /**
     * How much of the full first-person shift ({@code delta}) to apply: 1 when the weapon is still in view after it, else the largest share (bisection; the feasible set is
     * an interval that contains 0, every bound being linear) that keeps it in view, 0 when even the old placement is not in view (the shift never makes that worse).
     */
    public static float firstPersonShare(float[] place, float[] box, float[] delta, Display d, boolean left) {
        if (box == null || !(d.scale > 0f)) return 1f;
        if (inView(place, box, delta, d, left)) return 1f;
        float[] zero = {0f, 0f, 0f};
        if (!inView(place, box, zero, d, left)) return 0f;
        float lo = 0f, hi = 1f;
        for (int i = 0; i < 24; i++) {
            float mid = (lo + hi) / 2f;
            if (inView(place, box, new float[]{delta[0] * mid, delta[1] * mid, delta[2] * mid}, d, left)) lo = mid;
            else hi = mid;
        }
        return lo;
    }

    /** Where the hand ends up on the mesh (as a share of the mesh length from the butt) before any shift: the old behaviour, for the audit. */
    public static float oldHandFraction(float[] place, float length, float[] hand) {
        double a = Math.toRadians(place[2]);
        double along = (hand[0] - place[0]) * Math.cos(a) + (hand[1] - place[1]) * Math.sin(a);
        return length > 0f ? (float) (along / length) : 0f;
    }
}
