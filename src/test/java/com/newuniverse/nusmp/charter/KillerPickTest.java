package com.newuniverse.nusmp.charter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Who gets the grimoire. */
class KillerPickTest {
    private static final UUID A = new UUID(1, 1), B = new UUID(2, 2);

    @Test
    void theKillingBlowWins() {
        assertEquals(A, KillerPick.choose(new KillerPick.Candidate(A, true, 0), new KillerPick.Candidate(B, true, 10)));
        assertEquals(A, KillerPick.choose(new KillerPick.Candidate(A, true, 0), null));
    }

    @Test
    void aCreativeKillerGetsNothingAndNobodyTakesTheirPlace() {
        assertNull(KillerPick.choose(new KillerPick.Candidate(A, false, 0), new KillerPick.Candidate(B, true, 10)));
        assertNull(KillerPick.choose(new KillerPick.Candidate(A, false, 0), null));
    }

    @Test
    void withoutAPlayerBlowTheLastAttackerWithinThirtySecondsGetsIt() {
        assertEquals(B, KillerPick.choose(null, new KillerPick.Candidate(B, true, 0)));
        assertEquals(B, KillerPick.choose(null, new KillerPick.Candidate(B, true, 600)));
        assertNull(KillerPick.choose(null, new KillerPick.Candidate(B, true, 601)));
        assertNull(KillerPick.choose(null, new KillerPick.Candidate(B, false, 5)));
        assertNull(KillerPick.choose(null, new KillerPick.Candidate(B, true, -1)));
        assertNull(KillerPick.choose(null, null));
        assertNull(KillerPick.choose(null, new KillerPick.Candidate(null, true, 1)));
    }
}
