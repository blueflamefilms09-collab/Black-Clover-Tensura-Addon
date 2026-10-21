package com.newuniverse.nusmp.franchise;

import com.newuniverse.nusmp.franchise.FranchiseRules.Facts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the franchise engravings: every effect has a cap, a cooldown or a window, and no rule touches the food bar. */
class FranchiseRulesTest {
    private static final double EPS = 1e-9;

    private static Facts facts(boolean day, double tFrac, double sFrac, boolean undead, double armour, double maxHp, boolean sprint, boolean fall, boolean sneak, boolean wet) {
        return new Facts(day, tFrac, sFrac, undead, armour, maxHp, sprint, fall, sneak, wet);
    }

    @Test
    void levelsScaleSlowlyAndAreClamped() {
        assertEquals(1.0, FranchiseRules.levelMult(1), EPS);
        assertEquals(1.25, FranchiseRules.levelMult(2), EPS);
        assertEquals(1.5, FranchiseRules.levelMult(3), EPS);
        assertEquals(1.0, FranchiseRules.levelMult(0), EPS);
        assertEquals(1.5, FranchiseRules.levelMult(9), EPS);
        assertEquals(1, FranchiseRules.levelForGrade(0));
        assertEquals(1, FranchiseRules.levelForGrade(2));
        assertEquals(2, FranchiseRules.levelForGrade(3));
        assertEquals(2, FranchiseRules.levelForGrade(4));
        assertEquals(3, FranchiseRules.levelForGrade(5));
        assertEquals(3, FranchiseRules.levelForGrade(6));
    }

    @Test
    void attackBonusIsCappedAndHalvedAgainstPlayers() {
        assertEquals(0.08, FranchiseRules.cappedAttack(0.08, false, FranchiseRules.ATTACK_CAP), EPS);
        assertEquals(0.15, FranchiseRules.cappedAttack(0.80, false, FranchiseRules.ATTACK_CAP), EPS, "15% is the ceiling of all bonuses together");
        assertEquals(0.04, FranchiseRules.cappedAttack(0.08, true, FranchiseRules.ATTACK_CAP), EPS, "half against a player");
        assertEquals(0.15, FranchiseRules.cappedAttack(1.0, true, FranchiseRules.ATTACK_CAP), EPS, "still capped against a player");
        assertEquals(0.0, FranchiseRules.cappedAttack(-1.0, false, FranchiseRules.ATTACK_CAP), EPS, "a negative sum is nothing");
        assertEquals(0.30, FranchiseRules.cappedAttack(5.0, false, 9.0), EPS, "a config can raise the cap only to 30%");
        assertEquals(0.0, FranchiseRules.cappedAttack(5.0, false, -1.0), EPS);
        assertEquals(115f, FranchiseRules.applyAttack(100f, 3.0, false, FranchiseRules.ATTACK_CAP), 1e-3f);
        assertEquals(0f, FranchiseRules.applyAttack(0f, 3.0, false, FranchiseRules.ATTACK_CAP));
    }

    @Test
    void bonusIsAShareOfTheHitSoItScalesWithTheGradeTables() {
        // a genesis greatsword hit (~350) and a normal dagger hit (~20): the same engraving adds the same share, never a flat number
        float a = FranchiseRules.applyAttack(350f, 0.08, false, FranchiseRules.ATTACK_CAP);
        float b = FranchiseRules.applyAttack(20f, 0.08, false, FranchiseRules.ATTACK_CAP);
        assertEquals(350f * 1.08f, a, 1e-2f);
        assertEquals(20f * 1.08f, b, 1e-3f);
    }

    @Test
    void conditionsAreExact() {
        Facts base = facts(true, 1.0, 1.0, false, 0, 20, false, false, false, false);
        assertTrue(FranchiseRules.condMet("always", base));
        assertTrue(FranchiseRules.condMet("day", base));
        assertFalse(FranchiseRules.condMet("night", base));
        assertTrue(FranchiseRules.condMet("night", facts(false, 1, 1, false, 0, 20, false, false, false, false)));
        assertFalse(FranchiseRules.condMet("target_big", base));
        assertTrue(FranchiseRules.condMet("target_big", facts(true, 1, 1, false, 0, 60, false, false, false, false)));
        assertFalse(FranchiseRules.condMet("target_big", facts(true, 1, 1, false, 0, 59.9, false, false, false, false)));
        assertTrue(FranchiseRules.condMet("self_wounded", facts(true, 1, 0.5, false, 0, 20, false, false, false, false)));
        assertFalse(FranchiseRules.condMet("self_wounded", facts(true, 1, 0.51, false, 0, 20, false, false, false, false)));
        assertTrue(FranchiseRules.condMet("target_wounded", facts(true, 0.5, 1, false, 0, 20, false, false, false, false)));
        assertTrue(FranchiseRules.condMet("undead", facts(true, 1, 1, true, 0, 20, false, false, false, false)));
        assertTrue(FranchiseRules.condMet("sneaking", facts(true, 1, 1, false, 0, 20, false, false, true, false)));
        assertTrue(FranchiseRules.condMet("target_armoured", facts(true, 1, 1, false, 10, 20, false, false, false, false)));
        assertFalse(FranchiseRules.condMet("target_armoured", facts(true, 1, 1, false, 9, 20, false, false, false, false)));
        assertFalse(FranchiseRules.condMet("nonsense", base), "an unknown condition never holds");
        assertFalse(FranchiseRules.condMet(null, base));
    }

    @Test
    void guardsAreCapped() {
        assertEquals(0.06, FranchiseRules.cappedGuard(0.06), EPS);
        assertEquals(0.20, FranchiseRules.cappedGuard(0.90), EPS);
        assertEquals(0.0, FranchiseRules.cappedGuard(-0.4), EPS);
        assertEquals(80f, FranchiseRules.applyGuard(100f, 5.0), 1e-3f);
        assertTrue(FranchiseRules.wardApplies("magic", true, false, false, false, false, false));
        assertFalse(FranchiseRules.wardApplies("magic", false, true, false, false, false, false));
        assertTrue(FranchiseRules.wardApplies("projectile", false, true, false, false, false, false));
        assertTrue(FranchiseRules.wardApplies("fall", false, false, true, false, false, false));
        assertTrue(FranchiseRules.wardApplies("melee", false, false, false, true, false, false));
        assertFalse(FranchiseRules.wardApplies("other", true, true, true, true, true, true));
        assertFalse(FranchiseRules.wardApplies(null, true, true, true, true, true, true));
    }

    @Test
    void chancesAndTimersAreBounded() {
        assertEquals(0.05, FranchiseRules.chance(0.05, 1), EPS);
        assertEquals(0.0625, FranchiseRules.chance(0.05, 2), EPS);
        assertEquals(0.25, FranchiseRules.chance(0.9, 3), EPS, "no chance goes past 25%");
        assertTrue(FranchiseRules.roll(0.04, 0.05));
        assertFalse(FranchiseRules.roll(0.05, 0.05));
        assertTrue(FranchiseRules.ready(100, 100));
        assertFalse(FranchiseRules.ready(99, 100));
        assertEquals(0, FranchiseRules.pick(0.0, 4));
        assertEquals(3, FranchiseRules.pick(0.999, 4));
        assertEquals(3, FranchiseRules.pick(5.0, 4));
        assertEquals(0, FranchiseRules.pick(0.7, 1));
    }

    @Test
    void killStacksGrowToTheirMaxAndFade() {
        assertEquals(1, FranchiseRules.addStack(0, 5));
        assertEquals(5, FranchiseRules.addStack(5, 5));
        assertEquals(5, FranchiseRules.addStack(4, 5));
        assertEquals(3, FranchiseRules.stacksLeft(3, 1000, 1000 + 1200, 1200));
        assertEquals(0, FranchiseRules.stacksLeft(3, 1000, 1000 + 1201, 1200));
        assertEquals(0.075, FranchiseRules.stackBonus(5, 5, 0.015), EPS);
        assertEquals(0.075, FranchiseRules.stackBonus(9, 5, 0.015), EPS, "never more than max stacks");
        assertEquals(0.0, FranchiseRules.stackBonus(-2, 5, 0.015), EPS);
    }

    @Test
    void combosResetOutsideTheirWindow() {
        assertEquals(1, FranchiseRules.comboNext(0, -1_000_000L, 100, 60, 5));
        assertEquals(2, FranchiseRules.comboNext(1, 100, 150, 60, 5));
        assertEquals(1, FranchiseRules.comboNext(4, 100, 161, 60, 5), "a gap longer than the window starts over");
        assertEquals(5, FranchiseRules.comboNext(5, 100, 120, 60, 5), "capped at max");
    }

    @Test
    void chargeFiresOnTheNthHitAndResets() {
        int charge = 0;
        int fired = 0;
        for (int i = 1; i <= 16; i++) {
            if (FranchiseRules.chargeFires(charge, 8)) fired++;
            charge = FranchiseRules.chargeNext(charge, 8);
        }
        assertEquals(2, fired, "16 hits with a charge of 8 fire twice");
        assertEquals(0, charge);
        assertEquals(0, FranchiseRules.chargeNext(7, 8));
        assertEquals(8, FranchiseRules.chargeNext(7, 20));
        assertTrue(FranchiseRules.chargeFires(0, 1), "a charge of one fires every hit but is still capped by the attack cap");
        assertTrue(FranchiseRules.chargeStale(0, 201));
        assertFalse(FranchiseRules.chargeStale(0, 200));
    }

    @Test
    void thePriceOfBloodNeverKills() {
        assertTrue(FranchiseRules.priceOk(20f, 1.0, 8.0));
        assertTrue(FranchiseRules.priceOk(9f, 1.0, 8.0));
        assertFalse(FranchiseRules.priceOk(8.5f, 1.0, 8.0), "paying would leave less than the floor");
        assertFalse(FranchiseRules.priceOk(1f, 1.0, 8.0));
        assertFalse(FranchiseRules.priceOk(20f, 0.0, 8.0), "a free price is not a price");
        assertFalse(FranchiseRules.priceOk(20f, -3.0, 8.0));
    }

    @Test
    void aSecondWindNeedsALivingBearerUnderTheThreshold() {
        assertTrue(FranchiseRules.windFires(4f, 20f, 0.25));
        assertTrue(FranchiseRules.windFires(5f, 20f, 0.25));
        assertFalse(FranchiseRules.windFires(5.1f, 20f, 0.25));
        assertFalse(FranchiseRules.windFires(0f, 20f, 0.25), "a dead bearer is not saved");
        assertFalse(FranchiseRules.windFires(-1f, 20f, 0.25));
        assertFalse(FranchiseRules.windFires(3f, 0f, 0.25));
    }

    @Test
    void healsAreCapped() {
        assertEquals(1.5f, FranchiseRules.healCap(1.5, 1), 1e-6f);
        assertEquals(2.0f, FranchiseRules.healCap(1.5, 3), 1e-6f, "1.5 x 1.5 = 2.25 is cut to 2");
        assertEquals(2.0f, FranchiseRules.healCap(30.0, 1), 1e-6f);
        assertEquals(0.0f, FranchiseRules.healCap(-3.0, 1), 1e-6f);
        assertEquals(1.5f, FranchiseRules.steal(1000.0, 0.04, 1.5, 1), 1e-6f, "a lifesteal of a huge hit is cut to its own cap");
        assertEquals(0.8f, FranchiseRules.steal(20.0, 0.04, 1.5, 1), 1e-6f);
        assertEquals(2.0f, FranchiseRules.steal(1000.0, 0.5, 9.0, 3), 1e-6f, "and never past the global heal cap");
        assertEquals(0.0f, FranchiseRules.steal(-5.0, 0.04, 1.5, 1), 1e-6f);
    }

    @Test
    void spellsOpenAWindow() {
        assertTrue(FranchiseRules.spellWindow(160, 100, 60));
        assertFalse(FranchiseRules.spellWindow(161, 100, 60));
        assertFalse(FranchiseRules.spellWindow(100, -1, 60), "no cast yet, no window");
    }

    @Test
    void manaReturnsRespectEveryBudget() {
        // 10 000 maximum magicule: a 1.2% refund is 120, but a minute may return at most 5% = 500
        assertEquals(120.0, FranchiseRules.refund(10000, 0.012, 1, 0.0), 1e-6);
        assertEquals(180.0, FranchiseRules.refund(10000, 0.012, 3, 0.0), 1e-6, "level 3 is +50%");
        assertEquals(100.0, FranchiseRules.refund(10000, 0.012, 1, 400.0), 1e-6, "only the rest of the minute's budget is left");
        assertEquals(0.0, FranchiseRules.refund(10000, 0.012, 1, 500.0), 1e-6);
        assertEquals(0.0, FranchiseRules.refund(10000, 0.012, 1, 900.0), 1e-6);
        assertEquals(0.0, FranchiseRules.refund(0, 0.012, 1, 0.0), 1e-6);
        assertEquals(0.0, FranchiseRules.refund(10000, -1.0, 1, 0.0), 1e-6);
        assertEquals(40.0, FranchiseRules.trickle(10000, 0.004, 1), 1e-6);
        assertEquals(60.0, FranchiseRules.trickle(10000, 0.02, 1), 1e-6, "a pulse never returns more than 0.6%");
        assertEquals(0.0, FranchiseRules.trickle(-5, 0.004, 1), 1e-6);
    }

    @Test
    void theHungerBanHolds() {
        assertFalse(FranchiseRules.effectAllowed("minecraft:hunger"));
        assertFalse(FranchiseRules.effectAllowed("minecraft:saturation"));
        assertFalse(FranchiseRules.effectAllowed("tensura:food_drain"));
        assertFalse(FranchiseRules.effectAllowed(null));
        assertTrue(FranchiseRules.effectAllowed("minecraft:slowness"));
        assertTrue(FranchiseRules.effectAllowed("minecraft:weakness"));
        assertEquals(100, FranchiseRules.regenDuration(5000));
        assertEquals(20, FranchiseRules.regenDuration(1));
    }
}
