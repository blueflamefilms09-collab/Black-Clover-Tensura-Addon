package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The decision table of the Genesis and five-leaf item layers, the mesh candidate order, and the glow maths. */
class VariantRulesTest {
    private static VariantRules.Decision d(boolean stackGenesis, boolean holderGenesis, int leaves, boolean bc, boolean known) {
        return VariantRules.decide(new VariantRules.Inputs(stackGenesis, holderGenesis, leaves, bc, known));
    }

    @Test
    void genesisComesFromTheStackGradeOrTheHoldersBody() {
        assertTrue(d(true, false, 0, false, false).genesis(), "a Genesis relic needs no holder (frame, ground)");
        assertTrue(d(false, true, 0, false, true).genesis(), "a Genesis body holding anything");
        assertFalse(d(false, true, 0, false, false).genesis(), "the body flag of an unknown holder counts for nothing");
        assertFalse(d(false, false, 4, true, true).genesis());
    }

    @Test
    void fiveLeafNeedsABlackCloverWeaponAFiveLeafHolderAndAKnownHolder() {
        assertTrue(d(false, false, 5, true, true).fiveLeaf());
        assertTrue(d(false, false, 6, true, true).fiveLeaf(), "five and above");
        assertFalse(d(false, false, 4, true, true).fiveLeaf(), "a four-leaf is not enough");
        assertFalse(d(false, false, 5, false, true).fiveLeaf(), "not a Black Clover weapon");
        assertFalse(d(false, false, 5, true, false).fiveLeaf(), "no holder");
    }

    @Test
    void theTwoVariantsAreIndependent() {
        VariantRules.Decision both = d(true, false, 5, true, true);
        assertTrue(both.genesis() && both.fiveLeaf());
        assertFalse(d(false, false, 0, true, true).any());
        assertSame(VariantRules.Decision.NONE.any(), false);
    }

    @Test
    void candidatesPutTheVariantMeshBeforeThePlainOneAtEveryStage() {
        List<VariantRules.Candidate> c = VariantRules.candidates(List.of("x_pull2", "x_pull1", "x"), new VariantRules.Decision(true, true));
        assertEquals(List.of("x_pull2_genesis", "x_pull2_5leaf", "x_pull2", "x_pull1_genesis", "x_pull1_5leaf", "x_pull1", "x_genesis", "x_5leaf", "x"),
                c.stream().map(VariantRules.Candidate::meshId).toList());
        assertEquals(VariantRules.Tag.GENESIS, c.get(0).tag());
        assertEquals(VariantRules.Tag.FIVE_LEAF, c.get(1).tag());
        assertEquals(VariantRules.Tag.NONE, c.get(2).tag());
        assertEquals(List.of("x"), VariantRules.candidates(List.of("x"), VariantRules.Decision.NONE).stream().map(VariantRules.Candidate::meshId).toList());
    }

    @Test
    void aVariantWithoutItsOwnMeshGetsTheGlowTreatment() {
        VariantRules.Decision g = new VariantRules.Decision(true, false), f = new VariantRules.Decision(false, true);
        assertTrue(VariantRules.intensify(g, VariantRules.Tag.NONE));
        assertFalse(VariantRules.intensify(g, VariantRules.Tag.GENESIS), "its own mesh needs no extra glow");
        assertFalse(VariantRules.intensify(f, VariantRules.Tag.NONE), "five-leaf is a tint, not an intensity");
        assertTrue(VariantRules.tint(f, VariantRules.Tag.NONE));
        assertFalse(VariantRules.tint(f, VariantRules.Tag.FIVE_LEAF));
        assertFalse(VariantRules.tint(g, VariantRules.Tag.NONE));
    }

    @Test
    void theIntensifiedPulseIsFasterAndWiderAndBounded() {
        float min = 0.45f, max = 1.0f, speed = 0.14f;
        float mid = 0.5f * (min + max);
        float lo = Float.MAX_VALUE, hi = -1f, loBoost = Float.MAX_VALUE, hiBoost = -1f;
        for (int t = 0; t < 2000; t++) {
            float k = VariantRules.pulse(t, speed, min, max, 1f), kb = VariantRules.pulse(t, speed, min, max, VariantRules.GENESIS_BOOST);
            lo = Math.min(lo, k); hi = Math.max(hi, k); loBoost = Math.min(loBoost, kb); hiBoost = Math.max(hiBoost, kb);
        }
        assertEquals(min, lo, 0.01f);
        assertEquals(max, hi, 0.01f);
        assertEquals(mid - (mid - min) * 1.6f, loBoost, 0.02f, "swing widened 1.6 times about the middle");
        assertEquals(Math.min(1.5f, mid + (max - mid) * 1.6f), hiBoost, 0.02f);
        assertTrue(hiBoost <= 1.5f && loBoost >= 0f);
        // faster: the boosted pulse completes a cycle in 1/1.6 of the time
        assertEquals(VariantRules.pulse(0f, speed, min, max, 1f), VariantRules.pulse(0f, speed, min, max, VariantRules.GENESIS_BOOST), 1e-6, "same phase at t = 0");
        float period = (float) (2 * Math.PI / speed);
        assertEquals(VariantRules.pulse(3f, speed, min, max, VariantRules.GENESIS_BOOST),
                VariantRules.pulse(3f + period / VariantRules.GENESIS_BOOST, speed, min, max, VariantRules.GENESIS_BOOST), 1e-3);
    }

    @Test
    void theFiveLeafTintIsDarkAndViolet() {
        int tinted = VariantRules.tintGlow(0xFFE9A0);
        int r = tinted >> 16 & 255, g = tinted >> 8 & 255, b = tinted & 255;
        assertTrue(b > g, "violet leans blue over green");
        assertTrue(r < 0xFF * VariantRules.VIOLET_DARKEN, "darker than the source");
        int dark = VariantRules.tintGlow(0x000000);
        assertTrue((dark & 255) > (dark >> 16 & 255) && (dark & 255) < 120, "even a black glow becomes a dim violet");
        assertEquals((int) (0x5A * VariantRules.VIOLET_DARKEN), VariantRules.tintGlow(0x5A1E9E) >> 16 & 255, 1, "violet stays violet, darkened");
    }

    @Test
    void channelsClampToAByteAndAlphaBreathes() {
        assertEquals(255, VariantRules.channel(200, 1.5f));
        assertEquals(0, VariantRules.channel(200, -1f));
        assertEquals(100, VariantRules.channel(200, 0.5f));
        for (int t = 0; t < 400; t++) {
            int a = VariantRules.emissiveAlpha(t);
            assertTrue(a >= 60 && a <= 160, "alpha " + a);
        }
    }
}
