package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Who cut the roots of which Maw: remembered in memory, with an end time, and forgotten when the Maw closes. */
class MawCreditTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a"), B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void everyoneWhoCutARootIsCreditedOnce() {
        MawCredit c = new MawCredit();
        c.record("overworld@1", A, 100, 5000);
        c.record("overworld@1", B, 110, 5000);
        c.record("overworld@1", A, 120, 5000);
        assertEquals(Set.of(A, B), c.take("overworld@1", 200));
    }

    @Test
    void takingAMawForgetsIt() {
        MawCredit c = new MawCredit();
        c.record("overworld@1", A, 100, 5000);
        assertEquals(1, c.mawCount());
        assertEquals(Set.of(A), c.take("overworld@1", 200));
        assertEquals(0, c.mawCount());
        assertTrue(c.take("overworld@1", 200).isEmpty());
    }

    @Test
    void mawsAreKeptApart() {
        MawCredit c = new MawCredit();
        c.record("overworld@1", A, 100, 5000);
        c.record("overworld@2", B, 100, 5000);
        assertEquals(Set.of(B), c.take("overworld@2", 200));
        assertEquals(Set.of(A), c.take("overworld@1", 200));
        assertTrue(c.take("overworld@3", 200).isEmpty());
    }

    @Test
    void aCreditThatHasRunOutCountsForNothing() {
        MawCredit c = new MawCredit();
        c.record("overworld@1", A, 100, 1000);
        c.record("overworld@1", B, 100, 3000);
        assertEquals(Set.of(B), c.take("overworld@1", 2000));
    }

    @Test
    void aLaterCutKeepsTheLongerCredit() {
        MawCredit c = new MawCredit();
        c.record("overworld@1", A, 100, 3000);
        c.record("overworld@1", A, 200, 1000);
        assertEquals(Set.of(A), c.take("overworld@1", 2000));
    }

    @Test
    void mawsThatClosedSomeOtherWayAreSweptOutByNewCredits() {
        MawCredit c = new MawCredit();
        c.record("overworld@old", A, 100, 1000);
        assertEquals(1, c.mawCount());
        c.record("overworld@new", B, 5000, 9000);
        assertEquals(1, c.mawCount(), "the old Maw's credit ran out and was swept");
        assertEquals(Set.of(B), c.take("overworld@new", 6000));
    }

    @Test
    void clearForgetsEverything() {
        MawCredit c = new MawCredit();
        c.record("overworld@1", A, 100, 5000);
        c.clear();
        assertEquals(0, c.mawCount());
        assertTrue(c.take("overworld@1", 200).isEmpty());
    }
}
