package com.newuniverse.nusmp.client.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Why: a rig that the renderer cannot trust must fall back to the rigid mesh, never to a torn one (docs/vassal_forms_v2.md). */
class SwingRigTest {
    static String[] bone(int i, int parent) {
        return ("bone " + i + " " + parent + " 0 " + (60 + i * 10) + " 0  0 0 1  1 0 0  1.0 11.0 0.22 9.0").split("\\s+");
    }

    static String[] skin(int layer, int from, int... codes) {
        StringBuilder b = new StringBuilder("skin " + layer + " " + from + " " + codes.length);
        for (int c : codes) b.append(' ').append(c);
        return b.toString().split("\\s+");
    }

    private static SwingRig two() {
        SwingRig r = new SwingRig();
        r.addBone(bone(0, -1));
        r.addBone(bone(1, 0));
        return r;
    }

    @Test
    void aGoodRigIsValid() {
        SwingRig r = two();
        r.addSkin(skin(0, 8, 0, 0, 0, 0, 64, 64, 64, 64));
        assertEquals(2, r.n);
        assertTrue(r.valid(new int[]{16, 0, 0}));
        assertEquals(8, r.skinnedVertices());
        assertEquals(Math.toRadians(9.0), r.maxRad[0], 1e-6);
    }

    @Test
    void codesDecode() {
        short code = (short) (3 * 64 + 21);
        assertEquals(3, SwingRig.bone(code));
        assertEquals(21 / 63f, SwingRig.weight(code), 1e-6);
    }

    @Test
    void aRigThatDoesNotFitTheMeshIsRejected() {
        SwingRig r = two();
        r.addSkin(skin(0, 8, 0, 0, 0, 0));
        assertFalse(r.valid(new int[]{16, 0, 0}), "the skinned tail must end where the layer ends");
        assertFalse(r.valid(new int[]{12, 0, 0}) && r.valid(new int[]{20, 0, 0}));
        assertFalse(r.valid(null));
    }

    @Test
    void aCodeNamingAMissingBoneIsRejected() {
        SwingRig r = two();
        r.addSkin(skin(0, 0, 0, 0, 0, 2 * 64));
        assertFalse(r.valid(new int[]{4, 0, 0}), "bone 2 does not exist");
        SwingRig b = two();
        b.addSkin(skin(0, 0, 0, 0, 0, 64 + 5));
        assertFalse(b.valid(new int[]{4, 0, 0}), "the last bone cannot blend towards a next one");
    }

    @Test
    void brokenLinesBreakTheRig() {
        SwingRig skipped = new SwingRig();
        skipped.addBone(bone(0, -1));
        skipped.addBone(bone(2, 1));
        assertFalse(skipped.valid(new int[]{0, 0, 0}));
        SwingRig noParent = new SwingRig();
        noParent.addBone(bone(0, -1));
        noParent.addBone(bone(1, 5));
        assertFalse(noParent.valid(new int[]{0, 0, 0}));
        SwingRig garbage = two();
        garbage.addSkin("skin 0 0 4 x y z w".split("\\s+"));
        assertFalse(garbage.valid(new int[]{4, 0, 0}));
        SwingRig noSkin = two();
        assertFalse(noSkin.valid(new int[]{4, 0, 0}), "bones without skin move nothing");
        SwingRig twice = two();
        twice.addSkin(skin(0, 0, 0, 0, 0, 0));
        twice.addSkin(skin(0, 0, 0, 0, 0, 0));
        assertFalse(twice.valid(new int[]{4, 0, 0}), "one skin line per layer");
    }

    @Test
    void aDegenerateBoneIsRejected() {
        SwingRig r = new SwingRig();
        r.addBone("bone 0 -1 0 60 0  0 0 0  1 0 0  1.0 11.0 0.22 9.0".split("\\s+"));
        r.addSkin(skin(0, 0, 0, 0, 0, 0));
        assertFalse(r.valid(new int[]{4, 0, 0}), "a zero axis");
        SwingRig w = new SwingRig();
        w.addBone("bone 0 -1 0 60 0  0 0 1  1 0 0  1.0 0.0 0.22 9.0".split("\\s+"));
        w.addSkin(skin(0, 0, 0, 0, 0, 0));
        assertFalse(w.valid(new int[]{4, 0, 0}), "a zero frequency");
    }

    @Test
    void theBoneCapIsHonoured() {
        SwingRig r = new SwingRig();
        for (int i = 0; i < SwingMath.MAX_BONES; i++) r.addBone(bone(i, i - 1));
        assertEquals(SwingMath.MAX_BONES, r.n);
        r.addBone(bone(SwingMath.MAX_BONES, SwingMath.MAX_BONES - 1));
        r.addSkin(skin(0, 0, 0, 0, 0, 0));
        assertFalse(r.valid(new int[]{4, 0, 0}), "one bone too many breaks the rig");
    }
}
