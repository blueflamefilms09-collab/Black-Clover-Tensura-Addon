package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner report 3.7.0, docs/magic/rades_cast_fix.md): a refused numbered cast and a body that vanished early must each leave a line with the reason. The words and the three small rules
 * behind them are pure, so they are tested here.
 */
class RadesCastDiagnosisTest {
    @Test
    void everyRefusalHasAReasonAndAPlayerLine() {
        for (RadesCastDiagnosis.Refusal r : RadesCastDiagnosis.Refusal.values()) {
            assertFalse(r.why().isBlank(), r + " has no reason");
            String line = r.playerLine("No.4 - Jimmy");
            assertTrue(line.length() > 10, r + " has no player line");
        }
        assertTrue(RadesCastDiagnosis.Refusal.SWITCHED_OFF.playerLine("x").contains("switched off"));
        assertTrue(RadesCastDiagnosis.Refusal.ELITE_STANDS.playerLine("No.0 - Michael Caesal").contains("No.0 - Michael Caesal"));
    }

    @Test
    void refusedLineNamesThePlayerTheKindAndTheReason() {
        String s = RadesCastDiagnosis.refusedLine("Steve", "jimmy", RadesCastDiagnosis.Refusal.WORLD_REFUSED);
        assertTrue(s.contains("Steve") && s.contains("jimmy") && s.contains("addFreshEntity"), s);
        assertTrue(s.startsWith("[nusmp]"));
    }

    @Test
    void vanishedLineCarriesTheAgeInTicksAndSeconds() {
        String s = RadesCastDiagnosis.vanishedLine("carl", "uuid-1", 60, "it was killed");
        assertTrue(s.contains("carl") && s.contains("uuid-1") && s.contains("60 ticks") && s.contains("3.0 s") && s.contains("it was killed"), s);
    }

    @Test
    void earlyMeansUnderFiveSecondsAndNotANormalEnd() {
        assertTrue(RadesCastDiagnosis.vanishedEarly(0, false));
        assertTrue(RadesCastDiagnosis.vanishedEarly(RadesCastDiagnosis.EARLY_TICKS - 1, false));
        assertFalse(RadesCastDiagnosis.vanishedEarly(RadesCastDiagnosis.EARLY_TICKS, false));
        assertFalse(RadesCastDiagnosis.vanishedEarly(10, true), "a chunk unload is not a loss");
        assertFalse(RadesCastDiagnosis.vanishedEarly(-1, false));
    }

    @Test
    void normalEndsAreTheMovesNotTheLosses() {
        assertTrue(RadesCastDiagnosis.normalEnd("UNLOADED_TO_CHUNK"));
        assertTrue(RadesCastDiagnosis.normalEnd("UNLOADED_WITH_PLAYER"));
        assertTrue(RadesCastDiagnosis.normalEnd("CHANGED_DIMENSION"));
        assertFalse(RadesCastDiagnosis.normalEnd("KILLED"));
        assertFalse(RadesCastDiagnosis.normalEnd("DISCARDED"));
        assertFalse(RadesCastDiagnosis.normalEnd(null));
    }

    @Test
    void removalReasonPrefersLifeThenOwnerThenKind() {
        assertTrue(RadesCastDiagnosis.removalReason("KILLED", true, true).contains("life"));
        assertTrue(RadesCastDiagnosis.removalReason("KILLED", true, false).contains("owner"));
        assertEquals("it was killed", RadesCastDiagnosis.removalReason("KILLED", false, false));
        assertTrue(RadesCastDiagnosis.removalReason("DISCARDED", false, false).contains("discarded"));
        assertTrue(RadesCastDiagnosis.removalReason("WEIRD", false, false).contains("WEIRD"));
        assertEquals("unknown", RadesCastDiagnosis.removalReason(null, false, false));
    }

    @Test
    void rateLimitAllowsOneLinePerSecondPerPlayer() {
        assertTrue(RadesCastDiagnosis.mayLog(Long.MIN_VALUE, 5), "the first line is always written");
        assertFalse(RadesCastDiagnosis.mayLog(100, 100));
        assertFalse(RadesCastDiagnosis.mayLog(100, 100 + RadesCastDiagnosis.LOG_GAP_TICKS - 1));
        assertTrue(RadesCastDiagnosis.mayLog(100, 100 + RadesCastDiagnosis.LOG_GAP_TICKS));
        assertTrue(RadesCastDiagnosis.mayLog(100, 50), "the clock went backwards (new world): write");
    }
}
