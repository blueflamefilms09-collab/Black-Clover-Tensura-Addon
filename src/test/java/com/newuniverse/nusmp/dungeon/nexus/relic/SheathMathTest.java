package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Devil-Bound Weapon Sheath's charge and its Mana Siphon curse (docs/living_dungeon.md section 7). */
class SheathMathTest {
    @Test
    void parriedMagicChargesItAndAParryEarnsMore() {
        assertEquals(0, SheathMath.gain(0f, true));
        assertEquals(5, SheathMath.gain(1f, false), "a trickle still counts for 5");
        assertEquals(20, SheathMath.gain(10f, false));
        assertEquals(30, SheathMath.gain(10f, true), "a true parry is half again");
        assertEquals(40, SheathMath.gain(500f, false), "one blow can only add 40");
        assertEquals(60, SheathMath.gain(500f, true));
    }

    @Test
    void chargeStaysBetweenZeroAndAHundred() {
        assertEquals(100, SheathMath.add(90, 60));
        assertEquals(40, SheathMath.add(40, -5), "a negative gain adds nothing");
        assertEquals(0, SheathMath.add(-3, 0));
        assertFalse(SheathMath.full(99));
        assertTrue(SheathMath.full(100));
    }

    @Test
    void fourBigParriesFillIt() {
        int c = 0;
        int n = 0;
        while (!SheathMath.full(c)) {
            c = SheathMath.add(c, SheathMath.gain(20f, true));
            n++;
        }
        assertTrue(n <= 3, "three parried hits of 20 fill it, took " + n);
    }

    @Test
    void theSiphonCountsOnlyWhileFullAndTellsBeforeItStrikes() {
        int t = 0;
        t = SheathMath.fullTicks(t, 100, 20);
        assertEquals(20, t);
        assertEquals(0, SheathMath.fullTicks(t, 99, 20), "not full any more: the clock is back to zero");
        assertEquals(SheathMath.Siphon.QUIET, SheathMath.siphon(799));
        assertEquals(SheathMath.Siphon.WARN, SheathMath.siphon(800));
        assertEquals(SheathMath.Siphon.CRACKLE, SheathMath.siphon(1050));
        assertEquals(SheathMath.Siphon.CRACKLE, SheathMath.siphon(1200), "exactly a minute is not yet more than a minute");
        assertEquals(SheathMath.Siphon.DISCHARGE, SheathMath.siphon(1201));
    }

    @Test
    void aMinuteOfSecondsReachesTheDischargeInOrder() {
        int t = 0;
        SheathMath.Siphon last = SheathMath.Siphon.QUIET;
        int seconds = 0;
        while (last != SheathMath.Siphon.DISCHARGE) {
            t = SheathMath.fullTicks(t, 100, 20);
            SheathMath.Siphon now = SheathMath.siphon(t);
            assertTrue(now.ordinal() >= last.ordinal(), "the warnings only escalate");
            last = now;
            seconds++;
            assertTrue(seconds < 100);
        }
        assertEquals(61, seconds, "the discharge comes with the first sweep after a full minute");
    }

    @Test
    void theDischargeNeverKills() {
        assertEquals(6f, SheathMath.dischargeDamage(20f, 20f), 1e-6);
        assertEquals(2f, SheathMath.dischargeDamage(3f, 20f), 1e-6, "leaves a point");
        assertEquals(0f, SheathMath.dischargeDamage(1f, 20f), 1e-6);
        assertEquals(0f, SheathMath.dischargeDamage(0.5f, 20f), 1e-6);
    }

    @Test
    void chargeFeedsMagiculesBack() {
        assertEquals(0.05, SheathMath.magiculeShare(100), 1e-9);
        assertEquals(0.0, SheathMath.magiculeShare(-4), 1e-9);
    }
}
