package com.newuniverse.nusmp.multiverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Magic Knight rank ladder: Junior 5th (start) ... Senior 1st, then Grand and Wizard King. */
class KnightRankTest {
    @Test
    void everyoneStartsAtFifthClassJunior() {
        assertEquals(0, KnightRank.step(KnightRank.JUNIOR, 5));
        assertEquals("5th Class Junior Magic Knight", KnightRank.describe(KnightRank.JUNIOR, 5));
    }

    @Test
    void ladderIsInOrder() {
        int prev = -1;
        for (KnightRank r : new KnightRank[]{KnightRank.JUNIOR, KnightRank.INTERMEDIATE, KnightRank.SENIOR}) {
            for (int cls = 5; cls >= 1; cls--) {
                int s = KnightRank.step(r, cls);
                assertEquals(prev + 1, s);
                assertEquals(r, KnightRank.rankOfStep(s));
                assertEquals(cls, KnightRank.classOfStep(s));
                prev = s;
            }
        }
        assertEquals(15, KnightRank.step(KnightRank.GRAND, 0));
        assertEquals(16, KnightRank.step(KnightRank.WIZARD_KING, 0));
        assertEquals(KnightRank.GRAND, KnightRank.rankOfStep(15));
        assertEquals(KnightRank.WIZARD_KING, KnightRank.rankOfStep(16));
    }

    @Test
    void parsesConfigStrings() {
        assertEquals(KnightRank.step(KnightRank.INTERMEDIATE, 5), KnightRank.parseStep("INTERMEDIATE:5"));
        assertEquals(KnightRank.step(KnightRank.SENIOR, 3), KnightRank.parseStep("senior 3"));
        assertEquals(KnightRank.step(KnightRank.SENIOR, 5), KnightRank.parseStep("SENIOR"));
        assertEquals(15, KnightRank.parseStep("GRAND"));
        assertEquals(-1, KnightRank.parseStep("captain"));
        assertEquals(-1, KnightRank.parseStep(""));
    }

    @Test
    void firstClassSeniorIsJustBelowGrand() {
        assertEquals(14, KnightRank.step(KnightRank.SENIOR, 1));
        assertEquals("1st Class Senior Magic Knight", KnightRank.describe(KnightRank.SENIOR, 1));
        assertEquals("Grand Magic Knight", KnightRank.describe(KnightRank.GRAND, 0));
    }
}
