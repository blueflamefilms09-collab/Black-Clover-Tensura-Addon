package com.newuniverse.nusmp.heroweapon.weapon.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: three small pure parts of the Shield have rules that a single wrong sign would break quietly: the soul pool of the Soul Eater form (a cap, a hold time, a
 * cold pool), the per-target damage cap of the multi-pulse techniques (the budget is per target, and at most twelve targets), and the counter arithmetic (an answer
 * never worth more than the blow it answers; a price that never kills the wielder).
 */
class ShieldSmallPartsTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes());

    // ------------------------------------------------------------------ souls
    @Test
    void soulsAddUpToTheMaximumAndAreSpentAllAtOnce() {
        SoulPool p = new SoulPool();
        for (int i = 0; i < 9; i++) p.add(100 + i);
        assertEquals(ShieldNumbers.SOULS_MAX, p.count(110));
        assertEquals(ShieldNumbers.SOULS_MAX, p.spendAll(110));
        assertEquals(0, p.count(110));
        assertEquals(0, p.spendAll(111));
    }

    @Test
    void aColdPoolIsEmptyAndTheNextSoulStartsFresh() {
        SoulPool p = new SoulPool();
        p.add(100);
        p.add(110);
        assertEquals(2, p.count(110 + ShieldNumbers.SOUL_HOLD - 1));
        assertEquals(0, p.count(110 + ShieldNumbers.SOUL_HOLD), "the hold ran out");
        assertEquals(1, p.add(110 + ShieldNumbers.SOUL_HOLD + 5), "a soul after the pool went cold starts a new pool, not 3");
    }

    @Test
    void eachSoulKeepsTheWholePoolWarm() {
        SoulPool p = new SoulPool();
        p.add(0);
        p.add(500);
        assertEquals(2, p.count(900), "the second soul at 500 kept the pool alive past 600");
        assertEquals(0, p.count(500 + ShieldNumbers.SOUL_HOLD));
    }

    @Test
    void aClockThatWentFarBackEmptiesThePool() {
        SoulPool p = new SoulPool();
        p.add(1_000_000);
        assertEquals(0, p.count(10));
        p.clear();
        assertEquals(0, p.count(1_000_001));
    }

    // ------------------------------------------------------------------ hit cap
    @Test
    void aTargetTakesNoMoreThanTheCapInTotal() {
        HitCap cap = new HitCap(12);
        assertEquals(5.0, cap.take(A, 5), 1e-9);
        assertEquals(5.0, cap.take(A, 5), 1e-9);
        assertEquals(2.0, cap.take(A, 5), 1e-9, "only what is left of the 12");
        assertEquals(0.0, cap.take(A, 5), 1e-9);
        assertEquals(12.0, cap.total(A), 1e-9);
        assertEquals(5.0, cap.take(B, 5), 1e-9, "another target has its own share");
    }

    @Test
    void aCapHoldsAtMostTwelveTargets() {
        HitCap cap = new HitCap(12);
        for (int i = 0; i < 12; i++) assertEquals(1.0, cap.take(UUID.nameUUIDFromBytes(("t" + i).getBytes()), 1), 1e-9);
        assertEquals(0.0, cap.take(UUID.nameUUIDFromBytes("thirteenth".getBytes()), 1), 1e-9);
        assertEquals(12, cap.targets());
        assertEquals(1.0, cap.take(UUID.nameUUIDFromBytes("t3".getBytes()), 1), 1e-9, "an old target can still be hit");
    }

    @Test
    void nonsenseInputTakesNothing() {
        HitCap cap = new HitCap(12);
        assertEquals(0.0, cap.take(null, 5), 1e-9);
        assertEquals(0.0, cap.take(A, 0), 1e-9);
        assertEquals(0.0, cap.take(A, -4), 1e-9);
        assertEquals(0.0, cap.take(A, Double.NaN), 1e-9);
        assertEquals(0, cap.targets());
    }

    // ------------------------------------------------------------------ counter arithmetic
    @Test
    void aCounterAttackScalesFromItsBaseToItsCeilingWithTheCharge() {
        assertEquals(6.0, CounterMath.scaled(6, 6, 0), 1e-9);
        assertEquals(9.0, CounterMath.scaled(6, 6, 50), 1e-9);
        assertEquals(12.0, CounterMath.scaled(6, 6, 100), 1e-9);
        assertEquals(12.0, CounterMath.scaled(6, 6, 900), 1e-9, "a charge above the maximum is clamped");
        assertEquals(6.0, CounterMath.scaled(6, 6, -30), 1e-9);
    }

    @Test
    void theThreeCounterAttacksNeverPassTheirBudgetEvenWithAFullCharge() {
        assertTrue(CounterMath.scaled(ShieldNumbers.CHANGE_BASE, ShieldNumbers.CHANGE_SPAN, 100) <= ShieldNumbers.CHANGE_ATTACK.raw() + 1e-9);
        assertTrue(CounterMath.scaled(ShieldNumbers.IRON_BASE, ShieldNumbers.IRON_SPAN, 100) <= ShieldNumbers.IRON_MAIDEN.raw() + 1e-9);
        assertTrue(CounterMath.scaled(ShieldNumbers.NEEDLE_BURST_BASE, ShieldNumbers.NEEDLE_BURST_SPAN, 100) <= ShieldNumbers.NEEDLE_BURST.raw() + 1e-9);
    }

    @Test
    void anAnswerNeverDoesMoreFinalDamageThanItsShareOfTheBlow() {
        double limit = CounterMath.limit(10, 0.5);
        assertEquals(5.0, limit, 1e-9);
        assertEquals(4.0, CounterMath.fit(4, 4 * 3.0, limit * 4.0), 1e-9, "fits already");
        // the law would turn raw 4 into 24 final damage; the limit is 5, so raw must fall to 4 * 5 / 24
        assertEquals(4.0 * 5.0 / 24.0, CounterMath.fit(4, 24, 5), 1e-9);
        assertEquals(4.0, CounterMath.fit(4, 5, 5), 1e-9, "exactly at the limit stays");
        assertEquals(0.0, CounterMath.fit(4, 24, 0), 1e-9, "nothing was blocked, nothing is answered");
        assertEquals(0.0, CounterMath.fit(0, 24, 5), 1e-9);
        assertEquals(0.0, CounterMath.limit(0, 0.5), 1e-9);
        assertEquals(0.0, CounterMath.limit(10, 0), 1e-9);
    }

    @Test
    void theFitIsProportionalSoTheFinalDamageLandsExactlyOnTheLimit() {
        double probePerRaw = 6.0;                                         // the law multiplies by 6 for this pair of fighters
        double raw = 5, limit = 9;
        double fitted = CounterMath.fit(raw, raw * probePerRaw, limit);
        assertEquals(limit, fitted * probePerRaw, 1e-9);
    }

    @Test
    void theCounterGainIsAShareOfMaxHealthDoubledByAParry() {
        assertEquals(20.0, CounterMath.gain(4, 20, 1.0, false), 1e-9);
        assertEquals(40.0, CounterMath.gain(4, 20, 1.0, true), 1e-9);
        assertEquals(0.0, CounterMath.gain(0, 20, 1.0, false), 1e-9);
        assertEquals(0.0, CounterMath.gain(-1, 20, 1.0, false), 1e-9);
        assertEquals(400.0, CounterMath.gain(4, 0, 1.0, false), 1e-9, "a maximum health of 0 counts as 1, so the gain is 4 / 1 * 100");
    }

    @Test
    void thePriceOfBloodNeverGoesBelowTheFloor() {
        assertEquals(5.0, CounterMath.price(20, 20, 0.25, 0.30), 1e-9, "25 percent of 20 with room for 14");
        assertEquals(2.0, CounterMath.price(8, 20, 0.25, 0.30), 1e-9, "health 8, floor 6: room for 2");
        assertEquals(0.0, CounterMath.price(6, 20, 0.25, 0.30), 1e-9, "at the floor");
        assertEquals(0.0, CounterMath.price(3, 20, 0.25, 0.30), 1e-9, "under the floor");
        assertFalse(CounterMath.price(20, 20, 5.0, 0.30) > 14.0, "a share above one still leaves the floor");
    }
}
