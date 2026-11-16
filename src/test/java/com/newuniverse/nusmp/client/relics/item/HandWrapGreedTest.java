package com.newuniverse.nusmp.client.relics.item;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Black Sword Greed's gloves are worn on the hand like the Vassal Gauntlet, one glove per hand (wrist at mesh y = 0, fingers +y, back of the hand +z, the right glove's thumb on -x, the left
 * glove's on +x). Each must be a proper rotation (a mirror culls every face), its fingers must run toward item -z (the fingers of the arm) in third person and toward the screen's far side in first
 * person, its back of the hand must face OUT of the arm (item +x for the right arm, item -x for the left, where +x is the inside), and its thumb must point forward (item +y) in third person and toward the
 * middle of the screen in first person.
 */
class HandWrapGreedTest {
    private static final String RIGHT = "black_sword_greed_gauntlets", LEFT = "black_sword_greed_gauntlets_left";

    private static Vector3f turn(HandWrap.Wrap w, float x, float y, float z) { return w.rot().transform(new Vector3f(x, y, z)); }

    @Test
    void bothGlovesAreKnownAndAreNotInTheVassalTable() {
        assertNotNull(HandWrap.glove(RIGHT));
        assertNotNull(HandWrap.glove(LEFT));
        assertNull(HandWrap.of(RIGHT), "the old held look (classic) must not be treated as worn");
        assertNull(HandWrap.glove("black_sword_greed_dual_blades"));
    }

    @Test
    void theTurnsAreRotationsNotMirrors() {
        for (HandWrap.Wrap w : new HandWrap.Wrap[]{HandWrap.glove(RIGHT), HandWrap.glove(LEFT), HandWrap.GLOVE_FP}) assertEquals(1f, w.rot().determinant(), 1e-5);
    }

    @Test
    void thirdPersonFingersFollowTheArmTheBackFacesOutAndTheThumbPointsForward() {
        Vector3f fingers = turn(HandWrap.glove(RIGHT), 0, 1, 0), fingersL = turn(HandWrap.glove(LEFT), 0, 1, 0);
        assertEquals(-1f, fingers.z, 1e-5);
        assertEquals(-1f, fingersL.z, 1e-5);
        assertEquals(1f, turn(HandWrap.glove(RIGHT), 0, 0, 1).x, 1e-5, "the right hand's back is item +x (away from the body)");
        assertEquals(-1f, turn(HandWrap.glove(LEFT), 0, 0, 1).x, 1e-5, "the left arm's inside is item +x, so its back is -x");
        assertEquals(1f, turn(HandWrap.glove(RIGHT), -1, 0, 0).y, 1e-5, "the right thumb (mesh -x) points forward (item +y)");
        assertEquals(1f, turn(HandWrap.glove(LEFT), 1, 0, 0).y, 1e-5, "the left thumb (mesh +x) points forward (item +y)");
    }

    @Test
    void firstPersonFingersGoAwayFromTheCameraTheBackIsUpAndTheThumbsPointToTheMiddle() {
        assertEquals(-1f, turn(HandWrap.GLOVE_FP, 0, 1, 0).z, 1e-5, "fingers toward -z (away from the camera)");
        assertEquals(1f, turn(HandWrap.GLOVE_FP, 0, 0, 1).y, 1e-5, "the back of the hand is up");
        assertEquals(-1f, turn(HandWrap.GLOVE_FP, -1, 0, 0).x, 1e-5, "the right thumb (mesh -x) points left, to the middle of the screen");
        assertEquals(1f, turn(HandWrap.GLOVE_FP, 1, 0, 0).x, 1e-5, "the left thumb (mesh +x) points right, to the middle of the screen");
    }

    @Test
    void theGloveFitsTheArm() {
        HandWrap.Wrap w = HandWrap.glove(RIGHT);
        float lengthPx = 64.4f * w.sy() * 16f, depthPx = 18.4f * w.sz() * 16f;
        assertTrue(depthPx >= 4f && depthPx <= 5.2f, "wraps the 4 pixel arm without sinking into it or standing off: " + depthPx);
        assertTrue(lengthPx > 12f && lengthPx < 17f, "from the shoulder end of the arm to just past the hand: " + lengthPx);
    }
}
