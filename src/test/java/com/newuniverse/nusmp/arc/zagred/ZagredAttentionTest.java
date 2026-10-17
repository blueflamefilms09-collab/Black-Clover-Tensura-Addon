package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Why: the answers Zagred gives (Reverse against a burst, Seal against a caster, a turn on the top of the list) are only as honest as this memory. */
class ZagredAttentionTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3);

    @Test
    void aBurstIsTheShareOfHisHealthTakenInTheLastFiveSeconds() {
        ZagredAttention a = new ZagredAttention();
        a.hit(1000, 100f);
        a.hit(1010, 50f);
        assertEquals(0.15, a.burst(1010, 1000f), 1e-9, "both blows are inside the window");
        a.hit(1050, 30f);
        assertEquals(0.18, a.burst(1050, 1000f), 1e-9, "a third blow adds up");
    }

    @Test
    void theWindowForgetsBucketByBucket() {
        ZagredAttention a = new ZagredAttention();
        a.hit(1000, 100f);
        assertEquals(0.1, a.burst(1000 + 79, 1000f), 1e-9, "still inside the five seconds");
        assertEquals(0.0, a.burst(1000 + 100, 1000f), 1e-9, "after five seconds the blow is forgotten");
    }

    @Test
    void aLongQuietSpellEmptiesItAndAnOddClockDoesNotBreakIt() {
        ZagredAttention a = new ZagredAttention();
        a.hit(5000, 400f);
        assertEquals(0.0, a.burst(900000, 1000f), 1e-9, "a gap of many windows leaves nothing");
        a.hit(900001, 10f);
        assertEquals(0.01, a.burst(900001, 1000f), 1e-9);
        a.hit(100, 10f);                                                   // the clock went backwards (a reloaded world): the window starts again
        assertEquals(0.01, a.burst(100, 1000f), 1e-9);
    }

    @Test
    void nothingAndNegativeBlowsCountForNothing() {
        ZagredAttention a = new ZagredAttention();
        a.hit(10, 0f);
        a.hit(10, -5f);
        assertEquals(0.0, a.burst(10, 1000f), 1e-9);
        assertEquals(0.0, a.burst(10, 0f), 1e-9, "a zero maximum health does not divide by zero");
    }

    @Test
    void castersAreThePlayersWhoUsedMagicLately() {
        ZagredAttention a = new ZagredAttention();
        a.magicHit(A, 1000);
        a.magicHit(B, 1100);
        a.magicHit(A, 1120);                                               // the same player twice is still one caster
        assertEquals(2, a.casters(1150));
        assertEquals(1, a.casters(1100 + 160 + 1), "B fell out of the window, A (struck again at 1120) is still in it");
        assertEquals(0, a.casters(5000));
    }

    @Test
    void oldMagicHitsAreForgottenAndNullIsIgnored() {
        ZagredAttention a = new ZagredAttention();
        a.magicHit(null, 10);
        a.magicHit(A, 10);
        a.prune(10 + ZagredAttention.FORGET_TICKS);
        assertEquals(1, a.casters(10 + 100), "not yet forgotten");
        a.prune(10 + ZagredAttention.FORGET_TICKS + 1);
        assertEquals(0, a.casters(10 + 100), "forgotten");
    }

    @Test
    void theTopOfTheThreatListIsTheHardestHitterWhoIsStillEligible() {
        ZagredAttention a = new ZagredAttention();
        a.threatAdd(A, 50f);
        a.threatAdd(B, 80f);
        a.threatAdd(C, 20f);
        a.threatAdd(A, 40f);                                               // A: 90 now
        assertEquals(A, a.topThreat(id -> true));
        assertEquals(B, a.topThreat(id -> !A.equals(id)), "A left the arena: the next one down");
        assertNull(a.topThreat(id -> false), "nobody eligible");
        assertNull(new ZagredAttention().topThreat(id -> true), "an empty list has no top");
    }

    @Test
    void grudgesHalveAndTheSmallOnesVanish() {
        ZagredAttention a = new ZagredAttention();
        a.threatAdd(A, 100f);
        a.threatAdd(B, 1.5f);
        a.threatAdd(null, 99f);
        a.threatAdd(C, -4f);
        a.decayThreat();
        assertEquals(A, a.topThreat(id -> true));
        assertNull(a.topThreat(id -> B.equals(id)), "1.5 halved is 0.75: forgotten");
        a.decayThreat();
        a.decayThreat();
        a.decayThreat();
        a.decayThreat();
        a.decayThreat();
        a.decayThreat();
        assertNull(a.topThreat(id -> true), "100 halved seven times is under one");
    }

    @Test
    void clearForgetsEverything() {
        ZagredAttention a = new ZagredAttention();
        a.hit(10, 300f);
        a.magicHit(A, 10);
        a.threatAdd(A, 100f);
        a.clear();
        assertEquals(0.0, a.burst(10, 1000f), 1e-9);
        assertEquals(0, a.casters(10));
        assertNull(a.topThreat(id -> true));
    }
}
