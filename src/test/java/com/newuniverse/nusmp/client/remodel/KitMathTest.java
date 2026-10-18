package com.newuniverse.nusmp.client.remodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the curves of the remodel's motion (a tint, Rouge unravelling and re-knitting, a painted construct springing up off the canvas) were the old renderers' and must stay what the owner knew;
 * they are pure, so they are pinned here: white changes nothing, an unravelled cat is nearly nothing, a re-knitted one overshoots and settles, a painted body starts flat and ends at full height.
 */
class KitMathTest {
    @Test
    void whiteIsNoTintAndATintMultipliesPerChannel() {
        assertEquals(0x336699, KitMath.multiply(0xFFFFFF, 0x336699));
        assertEquals(0x000000, KitMath.multiply(0x000000, 0x336699));
        assertEquals(0x800000, KitMath.multiply(0xFF0000, 0x808080), "only the red channel survives a red body, halved by the grey");
    }

    @Test
    void mixGoesFromOneColourToTheOther() {
        assertEquals(0xFFFFFF, KitMath.mix(0xFFFFFF, 0x000000, 0f));
        assertEquals(0x000000, KitMath.mix(0xFFFFFF, 0x000000, 1f));
        assertEquals(0x7f7f7f, KitMath.mix(0xFFFFFF, 0x000000, 0.5f));
        assertEquals(0x000000, KitMath.mix(0xFFFFFF, 0x000000, 5f), "k is clamped");
    }

    @Test
    void anUnravellingCatShrinksToNearlyNothing() {
        assertEquals(1f, KitMath.unravelScale(0f), 1e-6);
        assertTrue(KitMath.unravelScale(0.5f) > KitMath.unravelScale(0.9f));
        assertEquals(0.02f, KitMath.unravelScale(1f), 1e-6, "never exactly zero");
        assertEquals(1f, KitMath.unravelScale(-3f), 1e-6, "clamped");
    }

    @Test
    void aReknittingCatOvershootsAndSettles() {
        assertEquals(0.02f, KitMath.reknitScale(0f), 1e-4, "starts at the floor value");
        float best = 0f;
        for (int i = 0; i <= 100; i++) best = Math.max(best, KitMath.reknitScale(i / 100f));
        assertTrue(best > 1.0f && best <= 1.1f, "overshoots a little: " + best);
        assertEquals(1.1f, KitMath.reknitScale(1f), 1e-4, "ends at the cap of the old curve; the next state (sitting) draws her at 1");
    }

    @Test
    void aPaintedBodyStartsFlatAndEndsAtFullHeight() {
        float[] first = KitMath.paintedRise(0f);
        assertNotNull(first);
        assertEquals(1.3f, first[0], 1e-6, "wide at first");
        assertEquals(0.04f, first[1], 1e-6, "flat at first");
        assertEquals(first[0], first[2], 1e-6);
        float[] mid = KitMath.paintedRise(0.6f);
        assertTrue(mid[1] > 0.5f && mid[0] < 1.3f);
        assertNull(KitMath.paintedRise(1f), "a standing body needs no squash");
        assertNull(KitMath.paintedRise(7f));
    }

    @Test
    void easeOutBackHitsItsEndsAndOvershoots() {
        assertEquals(0f, KitMath.easeOutBack(0f), 1e-5);
        assertEquals(1f, KitMath.easeOutBack(1f), 1e-5);
        float best = 0f;
        for (int i = 0; i <= 100; i++) best = Math.max(best, KitMath.easeOutBack(i / 100f));
        assertTrue(best > 1.05f && best < 1.2f, "about 10 percent: " + best);
    }
}
