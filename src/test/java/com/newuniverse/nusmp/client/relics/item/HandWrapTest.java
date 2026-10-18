package com.newuniverse.nusmp.client.relics.item;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a wrapped weapon (the Vassal Claw and Gauntlet worn on the hand) is only right if the mesh's forearm axis lies along the arm and the turn is a proper rotation (no mirror,
 * which would cull every face). Both are checked here, plus that the bracer ends up on the arm and not beyond it.
 */
class HandWrapTest {
    @Test
    void theForearmAxisRunsTowardTheFingers() {
        for (String id : new String[]{"vassal_claw", "vassal_gauntlet"}) {
            HandWrap.Wrap w = HandWrap.of(id);
            assertNotNull(w, id);
            float[] a = HandWrap.forearmAxis(w);
            assertEquals(0f, a[0], 1e-5, id);
            assertEquals(0f, a[1], 1e-5, id);
            assertEquals(-1f, a[2], 1e-5, id + ": mesh y must run along item -z, toward the fingers");
        }
    }

    @Test
    void theTurnIsARotationNotAMirror() {
        for (String id : new String[]{"vassal_claw", "vassal_gauntlet"}) {
            assertEquals(1f, HandWrap.of(id).rot().determinant(), 1e-5, id);
            Vector3f x = HandWrap.of(id).rot().transform(new Vector3f(1, 0, 0));
            Vector3f y = HandWrap.of(id).rot().transform(new Vector3f(0, 1, 0));
            assertEquals(0f, x.dot(y), 1e-5, id);
        }
    }

    @Test
    void theBracerStartsOnTheArmAndTheFistComesOutAtItsEnd() {
        // mesh heights of the cuff mouth, the end of the bracer and the end of the hand plate (tools/heroweapon/art_hart2.py)
        HandWrap.Wrap g = HandWrap.of("vassal_gauntlet"), c = HandWrap.of("vassal_claw");
        assertTrue(g.armPixelOf(0) >= 0 && g.armPixelOf(0) <= 4, "gauntlet mouth on the upper arm");
        assertTrue(g.armPixelOf(70) > 12 && g.armPixelOf(70) < 20, "gauntlet fist ends a little beyond the 12 pixel arm");
        assertTrue(c.armPixelOf(0) >= 0 && c.armPixelOf(0) <= 4, "claw mouth on the upper arm");
        assertTrue(c.armPixelOf(74) > 12 && c.armPixelOf(74) < 20, "claw hand plate ends a little beyond the arm");
    }
}
