package com.newuniverse.nusmp.visual.grip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.111.0 (docs/weapon_grip.md): the maths that puts a mesh's grip point into the hand. The reference here is vanilla's own order of operations written out step by step
 * ({@link #applyEntry}: translate, then turn about z, y, x in that order for a vector, then scale, about the item's centre), not the closed forms of {@link GripMath}.
 */
class GripMathTest {
    static final GripMath.Display HANDHELD_TP = new GripMath.Display(0, -90, 55, 0, 4, 0.5f, 0.85f);
    static final GripMath.Display HANDHELD_TP_LEFT = new GripMath.Display(0, 90, -55, 0, 4, 0.5f, 0.85f);
    static final GripMath.Display HANDHELD_FP = new GripMath.Display(0, -90, 25, 1.13f, 3.2f, 1.13f, 0.68f);
    static final GripMath.Display HANDHELD_FP_LEFT = new GripMath.Display(0, 90, -25, -1.13f, 3.2f, 1.13f, 0.68f);

    /** Where the point p of the item's unit cube lands relative to the hand under the entry, applied the way ItemTransform.apply does (mirrored for the left hand). */
    static double[] applyEntry(GripMath.Display d, boolean left, double px, double py, double pz) {
        double rx = d.rx(), ry = left ? -d.ry() : d.ry(), rz = left ? -d.rz() : d.rz();
        double tx = (left ? -d.tx() : d.tx()) / 16.0, ty = d.ty() / 16.0, tz = d.tz() / 16.0;
        double x = (px - 0.5) * d.scale(), y = (py - 0.5) * d.scale(), z = (pz - 0.5) * d.scale();     // T(-0.5), then S
        double c = Math.cos(Math.toRadians(rz)), s = Math.sin(Math.toRadians(rz));                        // Rz first
        double x1 = x * c - y * s, y1 = x * s + y * c;
        x = x1;
        y = y1;
        c = Math.cos(Math.toRadians(ry));
        s = Math.sin(Math.toRadians(ry));
        double x2 = x * c + z * s, z2 = -x * s + z * c;                                                   // then Ry
        x = x2;
        z = z2;
        c = Math.cos(Math.toRadians(rx));
        s = Math.sin(Math.toRadians(rx));
        double y3 = y * c - z * s, z3 = y * s + z * c;                                                    // then Rx
        return new double[]{x + tx, y3 + ty, z3 + tz};
    }

    @Test
    void theHandPointOfVanillaHandheldIsWhereTheSpriteHiltIs() {
        float[] h = GripMath.handPoint(HANDHELD_TP, false);
        assertEquals(0.238f, h[0], 0.002f);
        assertEquals(0.361f, h[1], 0.002f);
        assertEquals(0.5f, h[2], 0.002f);
    }

    @Test
    void theHandPointLandsAtTheHandOriginUnderTheVanillaChain() {
        for (GripMath.Display d : new GripMath.Display[]{HANDHELD_TP, new GripMath.Display(-80, 260, -40, -1, -2, 2.5f, 0.9f), new GripMath.Display(12, 33, -70, 3, -4, 5, 1.7f)}) {
            for (boolean left : new boolean[]{false, true}) {
                float[] h = GripMath.handPoint(d, left);
                double[] w = applyEntry(d, left, h[0], h[1], h[2]);
                assertEquals(0.0, w[0], 1e-5, "x for " + d + " left=" + left);
                assertEquals(0.0, w[1], 1e-5, "y for " + d + " left=" + left);
                assertEquals(0.0, w[2], 1e-5, "z for " + d + " left=" + left);
            }
        }
    }

    @Test
    void theLeftHandEntryOfVanillaIsTheSamePictureAsTheRightOne() {
        float[] r = GripMath.handPoint(HANDHELD_TP, false), l = GripMath.handPoint(HANDHELD_TP_LEFT, true);
        for (int i = 0; i < 3; i++) assertEquals(r[i], l[i], 1e-5f, "vanilla mirrors the left json back to the right values");
    }

    @Test
    void aMissingEntryHoldsTheCentreOfTheCube() {
        float[] h = GripMath.handPoint(GripMath.Display.NONE, false);
        assertEquals(0.5f, h[0], 1e-6f);
        assertEquals(0.5f, h[1], 1e-6f);
        assertEquals(0.5f, h[2], 1e-6f);
    }

    @Test
    void firstPersonTargetOfTheVanillaSwordIsItsHilt() {
        float[] t = GripMath.firstPersonTarget(HANDHELD_FP, false);
        assertEquals(0.25f, t[0], 1e-4f);
        assertEquals(0.25f, t[1], 1e-4f);
        assertEquals(0.5f, t[2], 1e-4f);
        float[] l = GripMath.firstPersonTarget(HANDHELD_FP_LEFT, true);
        assertEquals(0.25f, l[0], 1e-4f, "the left hand's sword has its hilt in the same place of the cube");
        assertEquals(0.25f, l[1], 1e-4f);
    }

    @Test
    void theGripLandsOnTheHandWhateverTheSizeOfTheMesh() {
        // a mesh placed on the old sprite diagonal at three different sizes about its butt: the grip point lands on the hand every time (scale around the grip, not the centre)
        float[] grip = {0.5f, 14f, 0.1f};
        float[] h = GripMath.handPoint(HANDHELD_TP, false);
        for (float scale : new float[]{0.01f, 0.0125f, 0.02f, 0.03f}) {
            float[] place = {0.1f, 0.08f, 45f, scale, 0f};
            float[] s = GripMath.shift(place, grip, h);
            float[] g = GripMath.cubePoint(place[0], place[1], place[2], place[3], place[4], grip[0], grip[1], grip[2]);
            double[] w = applyEntry(HANDHELD_TP, false, g[0] + s[0], g[1] + s[1], g[2] + s[2]);
            assertEquals(0.0, Math.hypot(Math.hypot(w[0], w[1]), w[2]), 1e-5, "grip on the hand at scale " + scale);
        }
    }

    @Test
    void theShiftIsZeroWhenTheOldPlacementAlreadyHeldTheGrip() {
        float[] place = {0.1f, 0.08f, 45f, 0.02f, 0f};
        float[] h = GripMath.handPoint(HANDHELD_TP, false);
        float[] grip = new float[3];
        double a = Math.toRadians(45), d = ((h[0] - 0.1) * Math.cos(a) + (h[1] - 0.08) * Math.sin(a)) / 0.02;
        grip[1] = (float) d;                                                          // the point of the mesh axis nearest the hand
        grip[0] = (float) (((h[0] - 0.1) * Math.sin(a) - (h[1] - 0.08) * Math.cos(a)) / 0.02);
        float[] s = GripMath.shift(place, grip, h);
        for (float v : s) assertEquals(0f, v, 1e-4f);
    }

    @Test
    void firstPersonKeepsTheWeaponInViewOrLeavesItWhereItWas() {
        // a tall mesh whose tip would leave the view if the grip were moved to the hilt spot: the share stays below 1, and the result is in view
        GripMath.Display d = new GripMath.Display(0, -90, 25, 1.13f, 0.87f, 0.28f, 0.318f);
        float[] place = {-0.23849f, -0.23849f, 45f, 0.024847f, 0f};
        float[] box = {-27f, 27f, -0.6f, 125f, -16f, 16f};
        float[] grip = {0f, 17f, 0f};
        float[] target = GripMath.firstPersonTarget(d, false);
        float[] full = GripMath.shift(place, grip, target);
        float k = GripMath.firstPersonShare(place, box, full, d, false);
        assertTrue(k >= 0f && k <= 1f);
        float[] applied = {full[0] * k, full[1] * k, full[2] * k};
        boolean oldOk = GripMath.inView(place, box, new float[3], d, false);
        assertTrue(!oldOk || GripMath.inView(place, box, applied, d, false), "the applied shift never takes a weapon out of view that was in view");
        // a weapon that fits comfortably gets the whole shift
        assertEquals(1f, GripMath.firstPersonShare(new float[]{0.1f, 0.1f, 45f, 0.0125f, 0f}, new float[]{-3, 3, 0, 40, -1, 1}, new float[]{0.02f, 0.01f, 0f}, HANDHELD_FP, false), 1e-6f);
    }

    @Test
    void theOldHandSatAQuarterToAThirdUpTheWeapon() {
        // what 1.110.0 did: the hand of every handheld display lies at 0.245 to 0.32 of the mesh length (tools/weapon_grip/grip_lib.py measures it for every weapon)
        float[] h = GripMath.handPoint(HANDHELD_TP, false);
        float[] place = {-0.23849f, -0.23849f, 45f, 0.024847f, 0f};
        float f = GripMath.oldHandFraction(place, 3.11f, h);
        assertEquals(0.245f, f, 0.01f);
    }
}
