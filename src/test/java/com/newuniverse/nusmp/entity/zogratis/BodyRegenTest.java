package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Dante's Body Magic: the pulse, its cooldown and strain, and every way the party can counter it. */
class BodyRegenTest {
    static final float MAX = 1200f;

    static BodyRegen.Decision mortal(BodyRegen b, long now, float health, float hit, int power, boolean staggered) {
        return b.onHit(now, health, MAX, hit, power, staggered, false);
    }

    @Test
    void tablesGrowWithPowerAndTheGatesDoubleThePulse() {
        double last = 0;
        for (int p : new int[]{20, 40, 60, 80, 100}) {
            assertTrue(BodyRegen.pulseFraction(p) > last, "pulse grows at " + p);
            last = BodyRegen.pulseFraction(p);
        }
        assertTrue(BodyRegen.pulseFraction(100) >= 1.8 * BodyRegen.pulseFraction(80), "Body Magic doubles at the Gates");
        assertTrue(BodyRegen.trickleFraction(100) >= 1.8 * BodyRegen.trickleFraction(80));
        for (int p = 20; p < 100; p += 20) assertTrue(BodyRegen.cooldownTicks(p + 20) <= BodyRegen.cooldownTicks(p));
        for (int p = 0; p <= 100; p++) {                           // rage values between the steps are fine too
            assertTrue(BodyRegen.pulseFraction(p) > 0 && BodyRegen.pulseFraction(p) < 0.4);
            assertTrue(BodyRegen.cooldownTicks(p) >= 400, "never a rapid-fire heal");
        }
    }

    @Test
    void aSmallHitOnAHealthyBossDoesNothing() {
        BodyRegen b = new BodyRegen();
        BodyRegen.Decision d = mortal(b, 100, 1000f, 30f, 80, false);
        assertEquals(BodyRegen.Result.IDLE, d.result());
        assertEquals(0f, d.heal());
        assertTrue(b.ready(100), "nothing was spent");
    }

    @Test
    void aMortalWoundClosesOnceThenNeedsTimeAndEachPulseIsWeaker() {
        BodyRegen b = new BodyRegen();
        BodyRegen.Decision first = mortal(b, 100, 400f, 60f, 80, false);
        assertEquals(BodyRegen.Result.FIRED, first.result());
        assertEquals(MAX * (float) BodyRegen.pulseFraction(80), first.heal(), 0.01f);
        BodyRegen.Decision second = mortal(b, 110, 300f, 60f, 80, false);
        assertEquals(BodyRegen.Result.COOLDOWN, second.result(), "rate limited");
        assertEquals(0f, second.heal());
        assertTrue(b.cooldownLeft(110) > 0);
        long later = 100 + BodyRegen.cooldownTicks(80) + 1;
        BodyRegen.Decision third = mortal(b, later, 300f, 60f, 80, false);
        assertEquals(BodyRegen.Result.FIRED, third.result());
        assertEquals(first.heal() * 0.75f, third.heal(), 0.01f, "each pulse is a quarter weaker than the last");
        assertTrue(third.heal() < first.heal());
        assertEquals(1.0, BodyRegen.strain(0));
        assertEquals(0.5625, BodyRegen.strain(2), 1e-9);
        assertEquals(2, b.used());
    }

    @Test
    void theGatesPulseIsTwiceAsBigAsTheLastStep() {
        BodyRegen a = new BodyRegen(), g = new BodyRegen();
        float atEighty = mortal(a, 100, 400f, 60f, 80, false).heal();
        float atGates = mortal(g, 100, 400f, 60f, 100, false).heal();
        assertTrue(atGates >= 1.8f * atEighty);
    }

    @Test
    void antiMagicCutsTheBodyMagicForEightSecondsAndCostsThePulse() {
        BodyRegen b = new BodyRegen();
        BodyRegen.Decision cut = b.onHit(100, 800f, MAX, 40f, 80, false, true);
        assertEquals(BodyRegen.Result.SUPPRESSED, cut.result());
        assertTrue(b.suppressed(100 + 159) && !b.suppressed(100 + BodyRegen.SUPPRESS_TICKS));
        assertEquals(BodyRegen.Result.SUPPRESSED, mortal(b, 150, 300f, 60f, 80, false).result(), "no pulse while cut");
        assertEquals(0f, b.trickle(150, MAX, 80, false), "no trickle while cut");
        assertFalse(b.ready(100 + 100));
        assertTrue(b.ready(100 + BodyRegen.SUPPRESS_TICKS));
        assertEquals(BodyRegen.Result.FIRED, mortal(b, 100 + BodyRegen.SUPPRESS_TICKS + 5, 300f, 60f, 80, false).result(), "the flesh mends again afterwards");
        assertTrue(b.trickle(100 + BodyRegen.SUPPRESS_TICKS + 5, MAX, 80, false) > 0);
    }

    @Test
    void aBurstHalvesThePulseAndABiggerOneDeniesIt() {
        BodyRegen half = new BodyRegen(), calm = new BodyRegen(), deny = new BodyRegen();
        float full = mortal(calm, 100, 370f, 20f, 80, false).heal();
        mortal(half, 90, 600f, 0.12f * MAX, 80, false);            // a big hit from higher up, no mortal wound yet
        BodyRegen.Decision halved = mortal(half, 100, 370f, 20f, 80, false);
        assertEquals(BodyRegen.Result.FIRED, halved.result());
        assertEquals(full * 0.5f, halved.heal(), 0.5f, "a 10% burst halves the pulse");
        mortal(deny, 90, 600f, 0.25f * MAX, 80, false);
        BodyRegen.Decision denied = mortal(deny, 100, 370f, 20f, 80, false);
        assertEquals(BodyRegen.Result.OVERLOADED, denied.result(), "a 20% burst denies it");
        assertEquals(0f, denied.heal());
        assertTrue(deny.ready(100), "a denied pulse is not spent");
    }

    @Test
    void theBurstFadesWhenTheDamageIsSpreadOut() {
        BodyRegen b = new BodyRegen();
        mortal(b, 10, 900f, 0.25f * MAX, 80, false);
        assertTrue(b.burst(10) > 0.2 * MAX);
        assertTrue(b.burst(10 + 200) < 0.05 * MAX, "after ten seconds it is gone");
        assertEquals(BodyRegen.Result.FIRED, mortal(b, 10 + 200, 370f, 20f, 80, false).result());
    }

    @Test
    void aBlowNoPulseCouldCoverIsSimplyLethal() {
        BodyRegen b = new BodyRegen();
        BodyRegen.Decision d = mortal(b, 100, 1f, 130f, 40, false);
        assertEquals(BodyRegen.Result.OVERWHELMED, d.result());
        assertEquals(0f, d.heal());
    }

    @Test
    void aPulseNeverMakesTheHitSurvivableThatItCouldNot() {
        // whenever a pulse fires, health + heal is more than the hit that triggered it
        for (float health = 1; health < 400; health += 7)
            for (float hit = 1; hit < 500; hit += 11) {
                BodyRegen b = new BodyRegen();
                BodyRegen.Decision d = mortal(b, 100, health, hit, 80, false);
                if (d.result() == BodyRegen.Result.FIRED) assertTrue(health + d.heal() > hit, health + "/" + hit);
            }
    }

    @Test
    void staggeredHalvesThePulseAndTheTrickle() {
        BodyRegen a = new BodyRegen(), b = new BodyRegen();
        float normal = mortal(a, 100, 370f, 20f, 80, false).heal();
        float weak = mortal(b, 100, 370f, 20f, 80, true).heal();
        assertTrue(normal > 0);
        assertEquals(normal * 0.5f, weak, 0.01f, "half strength while exposed");
        assertEquals(a.trickle(100, MAX, 80, false) * 0.5, a.trickle(100, MAX, 80, true), 1e-4);
    }

    @Test
    void healNeverPassesMaxHealth() {
        BodyRegen b = new BodyRegen();
        BodyRegen.Decision d = mortal(b, 100, 1190f, 900f, 100, false);
        if (d.result() == BodyRegen.Result.FIRED) assertTrue(1190f + d.heal() <= MAX + 0.01f);
    }

    @Test
    void resetAndRestore() {
        BodyRegen b = new BodyRegen();
        mortal(b, 100, 370f, 20f, 80, false);
        assertTrue(b.cooldownLeft(100) > 0);
        b.reset();
        assertTrue(b.ready(100) && b.used() == 0);
        b.restore(1000, 300, 50, 2);
        assertEquals(300, b.cooldownLeft(1000));
        assertEquals(50, b.suppressedLeft(1000));
        assertEquals(2, b.used());
        assertEquals(1000 + 300, 1000 + b.cooldownLeft(1000));
    }

    @Test
    void absurdAmountsNeverBreakTheCounter() {
        BodyRegen b = new BodyRegen();
        assertEquals(BodyRegen.Result.IDLE, mortal(b, 100, 1000f, Float.NaN, 80, false).result(), "NaN counts as nothing");
        assertEquals(0.0, b.burst(100), 1e-9);
        assertEquals(BodyRegen.Result.IDLE, mortal(b, 100, 1000f, -50f, 80, false).result());
        assertEquals(0.0, b.burst(100), 1e-9);
        BodyRegen.Decision huge = mortal(b, 100, 500f, Float.POSITIVE_INFINITY, 100, false);
        assertEquals(BodyRegen.Result.OVERLOADED, huge.result(), "an infinite blow is simply lethal");
        assertEquals(0f, huge.heal());
        assertFalse(Double.isNaN(b.burst(100)) || Double.isInfinite(b.burst(100)));
        assertTrue(b.burst(100 + 600) < 1.0, "and it still fades");
    }

    @Test
    void trickleIsSmallEvenAtTheGates() {
        for (int p : new int[]{20, 40, 60, 80, 100}) {
            float t = new BodyRegen().trickle(0, MAX, p, false);
            assertTrue(t > 0 && t < MAX * 0.005f, "trickle at " + p + " is " + t);
        }
    }
}
