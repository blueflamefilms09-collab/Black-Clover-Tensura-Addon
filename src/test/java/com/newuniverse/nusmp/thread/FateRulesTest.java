package com.newuniverse.nusmp.thread;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Rouge's decision: what triggers, what never does, repeats, boss cooldown and the price. */
class FateRulesTest {
    private static final FateRules.Params P = FateRules.Params.defaults();

    /** A player with 20 health, no armour, hit by 'amount'. */
    private static FateRules.Hit hit(float amount) {
        return new FateRules.Hit(amount, true, 0, 0, 20f, 0f, 20f, false, false, false, 1000, 0);
    }

    private static FateRules.Hit with(FateRules.Hit h, boolean unsavable, boolean boss, boolean repeat, long now, long bossCd) {
        return new FateRules.Hit(h.amount(), h.ignoresArmor(), h.armor(), h.toughness(), h.health(), h.absorption(), h.maxHealth(), unsavable, boss, repeat, now, bossCd);
    }

    @Test
    void aLethalBlowIsRewrittenAndASmallOneIsNot() {
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(hit(20f), P), "exactly lethal");
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(hit(500f), P));
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(hit(3f), P), "a scratch");
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(hit(0f), P), "no damage");
    }

    @Test
    void aBigHitTriggersEvenIfItDoesNotKill() {
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(hit(6.9f), P), "just under 35% of 20");
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(hit(7.0f), P), "35% of 20 is 7");
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(hit(12f), P));
    }

    @Test
    void lethalCountsAbsorptionAndWoundedHealth() {
        FateRules.Hit wounded = new FateRules.Hit(5f, true, 0, 0, 4f, 0f, 20f, false, false, false, 0, 0);
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(wounded, P), "5 damage on 4 health");
        FateRules.Hit shielded = new FateRules.Hit(5f, true, 0, 0, 4f, 4f, 20f, false, false, false, 0, 0);
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(shielded, P), "4 health + 4 absorption survives 5");
        assertTrue(FateRules.lethal(8f, 4f, 4f));
        assertFalse(FateRules.lethal(7.99f, 4f, 4f));
        assertFalse(FateRules.lethal(0f, 0f, 0f), "no damage never kills");
    }

    @Test
    void armourLowersTheEstimateButIgnoringArmourDoesNot() {
        float reduced = FateRules.afterArmour(20f, 20.0, 8.0);
        assertTrue(reduced < 12f && reduced > 0f, "full diamond takes a good part off: " + reduced);
        FateRules.Hit armoured = new FateRules.Hit(20f, false, 20, 8, 20f, 0f, 20f, false, false, false, 0, 0);
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(armoured, P), "still a big hit even after armour");
        FateRules.Hit light = new FateRules.Hit(12f, false, 20, 8, 20f, 0f, 20f, false, false, false, 0, 0);
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(light, P), "12 against full diamond is under 35% of 20");
        FateRules.Hit pierce = new FateRules.Hit(12f, true, 20, 8, 20f, 0f, 20f, false, false, false, 0, 0);
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(pierce, P), "the same blow that bypasses armour");
        assertEquals(0f, FateRules.afterArmour(0f, 20, 8));
        assertEquals(10f, FateRules.afterArmour(10f, 0, 0), 1e-4);
    }

    @Test
    void theVoidAndKillAndStoppedTimeAreNeverRewritten() {
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(with(hit(9999f), true, false, false, 1000, 0), P));
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(with(hit(9999f), true, true, true, 1000, 0), P), "unsavable beats everything");
    }

    @Test
    void theSameAttackerIsSkippedForTenTicksAtNoCost() {
        FateRules.Hit repeated = with(hit(2f), false, false, true, 1000, 0);
        assertEquals(FateRules.Verdict.REPEAT_FREE, FateRules.judge(repeated, P), "even a small second swing is cancelled while the mark holds");
        assertTrue(FateRules.sameAttacker("a", 1010, "a", 1005));
        assertFalse(FateRules.sameAttacker("a", 1010, "a", 1010), "the mark has run out");
        assertFalse(FateRules.sameAttacker("a", 1010, "b", 1005), "another attacker");
        assertFalse(FateRules.sameAttacker("", 0, null, 5));
    }

    @Test
    void aBossBlowCostsDoubleAndStartsACooldownForThatVictim() {
        FateRules.Hit boss = with(hit(30f), false, true, false, 1000, 0);
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(boss, P));
        long end = FateRules.bossCooldownEnd(1000, P);
        assertEquals(1000 + 160, end, "8 seconds");
        assertEquals(FateRules.Verdict.BOSS_COOLDOWN, FateRules.judge(with(hit(30f), false, true, false, 1100, end), P), "the next boss blow lands");
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(with(hit(30f), false, true, false, end, end), P), "cooldown over");
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(with(hit(30f), false, false, false, 1100, end), P), "the cooldown is only against bosses");
    }

    @Test
    void thePriceIsFlatPlusPercentAndDoubleForBosses() {
        assertEquals(150 + 200, FateRules.cost(1000, P, false), 1e-9);
        assertEquals((150 + 200) * 2, FateRules.cost(1000, P, true), 1e-9);
        assertEquals(150, FateRules.cost(0, P, false), 1e-9, "no mana pool still costs the flat part");
        assertEquals(150, FateRules.cost(-50, P, false), 1e-9, "never negative percent part");
        FateRules.Params free = new FateRules.Params(0.35, -5, -5, 160, 10);
        assertEquals(0.0, FateRules.cost(1000, free, false), 1e-9);
    }

    @Test
    void sheSavesOnlyIfTheWholePriceCanBePaid() {
        assertTrue(FateRules.canPay(350, 350));
        assertFalse(FateRules.canPay(349.9, 350));
        assertFalse(FateRules.canPay(0, 150));
        assertTrue(FateRules.canPay(0, 0));
    }

    @Test
    void bigHitFractionFollowsTheConfig() {
        FateRules.Params half = new FateRules.Params(0.5, 150, 20, 160, 10);
        assertEquals(FateRules.Verdict.IGNORE, FateRules.judge(hit(9f), half));
        assertEquals(FateRules.Verdict.SAVE, FateRules.judge(hit(10f), half));
        assertFalse(FateRules.big(5f, 0f, 0.35), "a zero-health victim never counts as a big hit");
    }
}
