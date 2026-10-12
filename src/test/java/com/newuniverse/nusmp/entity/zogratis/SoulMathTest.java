package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of Lucius's Soul Magic: Paladins, clones, the drain and the purge, and the resurrection budget. */
class SoulMathTest {
    @Test
    void theCapAndTheReviveTimerFollowThePower() {
        assertEquals(1, SoulMath.paladinCap(20));
        assertEquals(2, SoulMath.paladinCap(40));
        assertEquals(3, SoulMath.paladinCap(60));
        assertEquals(4, SoulMath.paladinCap(80));
        assertEquals(4, SoulMath.paladinCap(100));
        assertEquals(240, SoulMath.reviveTicks(20), "twelve seconds");
        assertEquals(160, SoulMath.reviveTicks(100), "eight seconds");
        assertTrue(SoulMath.reviveTicks(60) < SoulMath.reviveTicks(40));
        assertTrue(SoulMath.reviveTicks(100) >= 160, "never shorter than eight seconds: the window to cut the soul");
        assertEquals(240, SoulMath.reviveTicks(0), "below the opening power is the opening power");
    }

    @Test
    void aPaladinIsStrongerThanItWasButBounded() {
        assertEquals(120, SoulMath.paladinHealth(20, 20), 1e-9, "a floor");
        assertEquals(160, SoulMath.paladinHealth(100, 20), 1e-9, "1.6 x at the opening");
        assertEquals(320, SoulMath.paladinHealth(100, 100), 1e-9, "3.2 x at the Gates");
        assertTrue(SoulMath.paladinHealth(100, 100) > SoulMath.paladinHealth(100, 20));
        assertEquals(600, SoulMath.paladinHealth(500, 100), 1e-9, "a ceiling");
        assertEquals(2000, SoulMath.paladinHealth(2000, 100), 1e-9, "never made weaker");
        assertEquals(7, SoulMath.paladinAttack(3, 20), 1e-9);
        assertEquals(3 + 4 + 1.5 * 4, SoulMath.paladinAttack(3, 100), 1e-9);
        assertEquals(22, SoulMath.paladinAttack(20, 100), 1e-9, "a hard hitter gets a little");
        assertTrue(SoulMath.worthy(30) && !SoulMath.worthy(29));
        assertTrue(SoulMath.isBossScale(300) && !SoulMath.isBossScale(299));
    }

    @Test
    void clonesAreCappedFragileAndShortLived() {
        assertEquals(2, SoulMath.cloneCap(20));
        assertEquals(6, SoulMath.cloneCap(100));
        for (int p = 20; p <= 100; p += 20) {
            assertTrue(SoulMath.cloneCap(p) <= 6);
            assertTrue(SoulMath.cloneHealth(p) <= 140, "they shatter under pressure");
            assertTrue(SoulMath.cloneLife(p) <= 800);
        }
        assertTrue(SoulMath.cloneSpellGap(100) < SoulMath.cloneSpellGap(20));
        assertTrue(SoulMath.cloneSpellGap(100) >= 40, "no spam");
        assertTrue(SoulMath.CLONE_DAMAGE < 0.5);
    }

    @Test
    void drainAndPurgeHealingAreBounded() {
        assertEquals(3.5f, SoulMath.drainHeal(7f, 3600f), 1e-6, "half of what the beam deals");
        assertEquals(3600 * 0.006f, SoulMath.drainHeal(1000f, 3600f), 1e-3, "capped per tick");
        assertEquals(0f, SoulMath.drainHeal(-4f, 3600f));
        assertEquals(108f, SoulMath.drainCap(3600f), 1e-3);
        assertEquals(54f, SoulMath.purgeHeal(1, 3600f), 1e-3);
        assertEquals(216f, SoulMath.purgeHeal(50, 3600f), 1e-3, "at most 6%");
        assertEquals(0f, SoulMath.purgeHeal(-1, 3600f));
        assertEquals(1.5f, SoulMath.purgeFactor(true));
        assertEquals(1f, SoulMath.purgeFactor(false));
        assertEquals(160, SoulMath.siblingCutTicks(40));
        assertEquals(240, SoulMath.siblingCutTicks(100));
    }

    @Test
    void theResurrectionBudgetIsSpentAndRefused() {
        SoulMath.Budget b = new SoulMath.Budget();
        assertEquals(3, SoulMath.resurrectBudget(20));
        assertEquals(2, b.take(2, 20));
        assertEquals(1, b.take(2, 20), "only one left");
        assertEquals(0, b.take(1, 20), "used up");
        assertEquals(3, b.used());
        assertEquals(1, b.take(2, 40), "more power, one more soul to raise (budget 4, 3 used)");
        b.reset();
        assertEquals(0, b.used());
        assertEquals(0, b.take(-3, 20));
        b.restore(99);
        assertEquals(0, b.take(1, 100), "a restored budget over the cap raises nothing");
        assertEquals(3, SoulMath.resurrectPerCast(100));
        assertEquals(2, SoulMath.resurrectPerCast(60));
    }
}
