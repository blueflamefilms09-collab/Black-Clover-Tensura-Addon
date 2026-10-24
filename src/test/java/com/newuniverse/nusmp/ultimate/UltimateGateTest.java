package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.ultimate.UltimateGate.Progress;
import com.newuniverse.nusmp.ultimate.UltimateGate.Req;
import com.newuniverse.nusmp.ultimate.UltimateGate.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the late-game gate of the Ultimate pages (full mastery bar, Senior 1st Class, 1,000,000 EP) is pure code, pinned here: the numbers, the floors under any strictness, the Wizard King's right of the crown,
 * and the text the player reads for what is missing. Pure.
 */
class UltimateGateTest {
    private static final Progress TOP = new Progress(1.0, 14, 1_000_000, false);

    @Test
    void aPlayerAtTheTopOfTheLadderPasses() {
        Verdict v = UltimateGate.evaluate(1.0, TOP);
        assertTrue(v.open());
        assertTrue(v.unmet().isEmpty());
        assertEquals("", v.summary());
        assertEquals(3, v.checks().size());
    }

    @Test
    void eachRequirementAloneKeepsThePageClosed() {
        assertFalse(UltimateGate.passes(1.0, new Progress(0.99, 14, 1_000_000, false)), "mastery must be the whole bar");
        assertFalse(UltimateGate.passes(1.0, new Progress(1.0, 13, 1_000_000, false)), "rank 13 is Senior 2nd Class");
        assertFalse(UltimateGate.passes(1.0, new Progress(1.0, 14, 999_999, false)));
        assertFalse(UltimateGate.passes(1.0, Progress.NONE));
        Verdict v = UltimateGate.evaluate(1.0, new Progress(0.5, 3, 10, false));
        assertEquals(3, v.unmet().size());
        assertTrue(v.summary().contains("mastery 50% of 100%"), v.summary());
        assertTrue(v.summary().contains("rank Junior 2nd Class of Senior 1st Class"), v.summary());
        assertTrue(v.summary().contains("EP 10 of 1,000,000"), v.summary());
    }

    @Test
    void theWizardKingIsOpenOnRankAndEpButStillNeedsTheMastery() {
        assertTrue(UltimateGate.passes(1.0, new Progress(1.0, 16, 0, true)));
        assertTrue(UltimateGate.passes(1.0, new Progress(1.0, 0, 0, true)));
        assertFalse(UltimateGate.passes(1.0, new Progress(0.9, 16, 0, true)), "the crown does not give the book's last page");
        Verdict v = UltimateGate.evaluate(1.0, new Progress(0.9, 16, 0, true));
        assertEquals(1, v.unmet().size());
        assertEquals(Req.MASTERY, v.unmet().get(0).req());
    }

    @Test
    void grandMagicKnightsPassTheRank() {
        assertTrue(UltimateGate.passes(1.0, new Progress(1.0, 15, 1_000_000, false)));
    }

    @Test
    void strictnessMovesTheEpButNeverUnderTheFloors() {
        assertEquals(1_000_000.0, UltimateGate.needEp(1.0), 1e-6);
        assertEquals(500_000.0, UltimateGate.needEp(0.5), 1e-6);
        assertEquals(500_000.0, UltimateGate.needEp(0.0001), 1e-6, "clamped to 0.5, which is exactly the floor");
        assertEquals(500_000.0, UltimateGate.needEp(-9), 1e-6);
        assertEquals(4_000_000.0, UltimateGate.needEp(4.0), 1e-6);
        assertEquals(4_000_000.0, UltimateGate.needEp(400.0), 1e-6);
        assertEquals(1_000_000.0, UltimateGate.needEp(Double.NaN), 1e-6);
        assertTrue(UltimateGate.needEp(0.5) >= UltimateGate.FLOOR_EP);
        assertTrue(UltimateGate.passes(0.5, new Progress(1.0, 14, 500_000, false)));
        assertFalse(UltimateGate.passes(2.0, new Progress(1.0, 14, 1_999_999, false)));
    }

    @Test
    void theRankIsALadderNotAQuantityAndSitsAtSenior1st() {
        assertEquals(14, UltimateGate.needStep());
        assertTrue(UltimateGate.needStep() >= UltimateGate.FLOOR_STEP);
        assertEquals("Senior 1st Class", UltimateGate.rank(14));
        assertEquals("Junior 5th Class", UltimateGate.rank(0));
        assertEquals("Intermediate 3rd Class", UltimateGate.rank(7));
        assertEquals("Grand Magic Knight", UltimateGate.rank(15));
        assertEquals("Wizard King", UltimateGate.rank(16));
        assertEquals("Wizard King", UltimateGate.rank(99));
        assertEquals("Junior 5th Class", UltimateGate.rank(-4));
    }

    @Test
    void theMasteryThresholdToleratesFloatNoiseOnly() {
        assertTrue(UltimateGate.passes(1.0, new Progress(1.0 - 1e-12, 14, 1_000_000, false)));
        assertFalse(UltimateGate.passes(1.0, new Progress(0.9999, 14, 1_000_000, false)));
    }
}
