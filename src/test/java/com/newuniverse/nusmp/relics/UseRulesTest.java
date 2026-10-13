package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cooldown clocks, per-day counters, the cost arithmetic and the attunement cap. */
class UseRulesTest {
    @Test
    void cooldownCountsDown() {
        long at = UseRules.readyAt(1000, 200);
        assertEquals(1200, at);
        assertFalse(UseRules.ready(1100, at));
        assertEquals(100, UseRules.remaining(1100, at));
        assertTrue(UseRules.ready(1200, at));
        assertEquals(0, UseRules.remaining(5000, at));
        assertEquals(1000, UseRules.readyAt(1000, -5));
    }

    @Test
    void usesResetEachMinecraftDay() {
        assertEquals(0, UseRules.day(0));
        assertEquals(0, UseRules.day(23999));
        assertEquals(1, UseRules.day(24000));
        assertEquals(2, UseRules.usesLeft(5, 0, 5, 2));
        assertEquals(1, UseRules.usesLeft(5, 1, 5, 2));
        assertEquals(0, UseRules.usesLeft(5, 2, 5, 2));
        assertEquals(2, UseRules.usesLeft(5, 2, 6, 2), "a new day gives the uses back");
        assertEquals(Integer.MAX_VALUE, UseRules.usesLeft(5, 99, 5, 0), "zero means unlimited");
        assertEquals(1, UseRules.usedAfter(4, 7, 5));
        assertEquals(3, UseRules.usedAfter(5, 2, 5));
    }

    @Test
    void spanReadsWell() {
        assertEquals("1 s", UseRules.span(1));
        assertEquals("12 s", UseRules.span(240));
        assertEquals("1 m", UseRules.span(1200));
        assertEquals("1 m 5 s", UseRules.span(1300));
    }

    @Test
    void manaMaySpendDownToNothingButNotBelow() {
        CostMath.Spend ok = CostMath.spend(100, 100, 40, 0);
        assertTrue(ok.ok());
        assertEquals(60, ok.after(), 1e-9);
        assertTrue(CostMath.spend(40, 100, 40, 0).ok());
        assertFalse(CostMath.spend(39, 100, 40, 0).ok());
        assertEquals(39, CostMath.spend(39, 100, 40, 0).after(), 1e-9);
    }

    @Test
    void existenceCostsKeepTenPercent() {
        assertTrue(CostMath.spend(100, 100, 90, CostMath.EP_FLOOR).ok());
        assertFalse(CostMath.spend(100, 100, 91, CostMath.EP_FLOOR).ok());
        assertFalse(CostMath.spend(50, 100, 45, CostMath.EP_FLOOR).ok());
        assertTrue(CostMath.spend(50, 100, 40, CostMath.EP_FLOOR).ok());
    }

    @Test
    void aPoolWithNoMaximumIsFree() {
        assertTrue(CostMath.spend(0, 0, 50, CostMath.EP_FLOOR).ok());
        assertTrue(CostMath.spend(10, 100, 0, 0.1).ok());
    }

    @Test
    void onlyThreeAttunedRelicsWork() {
        boolean[] needs = {true, true, true, true, false, true};
        boolean[] mine = {true, true, true, true, false, false};
        boolean[] w = AttuneRules.working(needs, mine, AttuneRules.CAP);
        assertArrayEquals(new boolean[]{true, true, true, false, true, false}, w);
        assertTrue(AttuneRules.canAttune(2));
        assertFalse(AttuneRules.canAttune(3));
        assertEquals(0, AttuneRules.percent(0));
        assertEquals(50, AttuneRules.percent(50));
        assertEquals(100, AttuneRules.percent(500));
    }

    @Test
    void relicsThatNeedNoAttunementAlwaysWork() {
        boolean[] w = AttuneRules.working(new boolean[]{false, false}, new boolean[]{false, false}, 3);
        assertTrue(w[0] && w[1]);
    }

    @Test
    void manaIsPaidInMagiculesThenAura() {
        CostMath.Pooled a = CostMath.spendPooled(100, 50, 30);
        assertTrue(a.ok());
        assertEquals(70, a.first(), 1e-9);
        assertEquals(50, a.second(), 1e-9);
        CostMath.Pooled b = CostMath.spendPooled(20, 50, 30);
        assertTrue(b.ok());
        assertEquals(0, b.first(), 1e-9);
        assertEquals(40, b.second(), 1e-9);
        CostMath.Pooled c = CostMath.spendPooled(20, 5, 30);
        assertFalse(c.ok());
        assertEquals(20, c.first(), 1e-9);
        assertEquals(5, c.second(), 1e-9);
        assertTrue(CostMath.spendPooled(0, 0, 0).ok());
    }
}
