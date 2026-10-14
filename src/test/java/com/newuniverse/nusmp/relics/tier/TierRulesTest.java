package com.newuniverse.nusmp.relics.tier;

import com.newuniverse.nusmp.relics.tier.TierRules.Catalyst;
import com.newuniverse.nusmp.relics.tier.TierRules.Stat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The tier curve, its caps and the ritual's tables. */
class TierRulesTest {
    @Test
    void anUnraisedItemIsExactlyItself() {
        for (Stat s : Stat.values()) assertEquals(1.0, TierRules.multiplier(s, 0), 0.0, s.name());
        assertEquals(5.0, TierRules.scaledDamageModifier(5.0, 0), 0.0);
        assertEquals(-2.4, TierRules.scaledSpeedModifier(-2.4, 0), 0.0);
        assertEquals(7.0, TierRules.scaledArmor(7.0, 0), 0.0);
        assertEquals(3.0, TierRules.scaledToughness(3.0, 0), 0.0);
    }

    @Test
    void theEffectiveGradeIsTheHigherOfBaseAndTier() {
        assertEquals(3, TierRules.effective(3, 0));
        assertEquals(3, TierRules.effective(3, 2));
        assertEquals(5, TierRules.effective(3, 5));
        assertEquals(6, TierRules.effective(3, 99), "a tier past the top is clamped");
        assertEquals(0, TierRules.effective(0, -4));
        assertEquals(0, TierRules.steps(3, 2));
        assertEquals(2, TierRules.steps(3, 5));
        assertEquals(6, TierRules.steps(0, 6));
    }

    @Test
    void everyBoostGrowsAndEveryReductionFalls() {
        for (Stat s : new Stat[]{Stat.DAMAGE, Stat.ATTACK_SPEED, Stat.ARMOR, Stat.TOUGHNESS, Stat.ACT_POWER})
            for (int i = 1; i <= 6; i++) assertTrue(TierRules.multiplier(s, i) > TierRules.multiplier(s, i - 1), s + " at " + i);
        for (Stat s : new Stat[]{Stat.COST, Stat.COOLDOWN})
            for (int i = 1; i <= 6; i++) assertTrue(TierRules.multiplier(s, i) <= TierRules.multiplier(s, i - 1), s + " at " + i);
    }

    @Test
    void reductionsKeepTheirFloors() {
        for (int i = 0; i <= 12; i++) {
            assertTrue(TierRules.multiplier(Stat.COST, i) >= TierRules.MIN_COST_FACTOR);
            assertTrue(TierRules.multiplier(Stat.COOLDOWN, i) >= TierRules.MIN_COOLDOWN_FACTOR);
        }
        assertEquals(TierRules.multiplier(Stat.DAMAGE, 6), TierRules.multiplier(Stat.DAMAGE, 40), 0.0, "past the table it stays at the top");
    }

    @Test
    void damageIsCappedButNeverLowered() {
        assertEquals(6.0 * 1.65, TierRules.scaledDamageModifier(6.0, 6), 1e-9);
        assertEquals(39.0, TierRules.scaledDamageModifier(35.0, 6), 1e-9, "total damage 40 at most");
        assertEquals(50.0, TierRules.scaledDamageModifier(50.0, 6), 0.0, "an item already above the cap is left as it is");
    }

    @Test
    void speedNeverPassesTheBaseAttackSpeed() {
        assertEquals(0.0, TierRules.scaledSpeedModifier(-0.2, 6), 1e-9, "3.8 x 1.18 is capped at 4.0 attacks per second");
        double sword = TierRules.scaledSpeedModifier(-2.4, 6);
        assertEquals(1.6 * 1.18 - 4.0 + 4.0, sword + 4.0, 1e-9);
        assertTrue(sword > -2.4 && sword <= 0.0);
        assertEquals(-2.4, TierRules.scaledSpeedModifier(-2.4, 0), 0.0);
    }

    @Test
    void armourAndToughnessAreCappedPerPiece() {
        assertEquals(8.0 * 1.40 > 10 ? 10.0 : 8.0 * 1.40, TierRules.scaledArmor(8.0, 6), 1e-9);
        assertEquals(10.0, TierRules.scaledArmor(10.0, 6), 0.0);
        assertEquals(12.0, TierRules.scaledArmor(12.0, 6), 0.0, "already above the cap: unchanged");
        assertEquals(5.0, TierRules.scaledToughness(4.0, 6), 1e-9);
        assertEquals(1.5 * 1.2, TierRules.scaledToughness(1.5, 2), 1e-9);
    }

    @Test
    void everyJumpHasACatalystACostAndARisk() {
        for (int g = 1; g <= 6; g++) {
            assertTrue(TierRules.catalystsTo(g).length >= 1, "catalyst for " + g);
            for (Catalyst c : TierRules.catalystsTo(g)) {
                assertTrue(c.count() >= 1 && c.count() <= 16);
                assertTrue(c.itemId().contains(":"));
            }
            assertTrue(TierRules.successChance(g) > 0.0 && TierRules.successChance(g) <= 1.0);
            assertTrue(TierRules.costPercent(g) > 0.0 && TierRules.costPercent(g) < 50.0, "never more than a pool's half");
            if (g > 1) {
                assertTrue(TierRules.successChance(g) < TierRules.successChance(g - 1), "harder as it climbs");
                assertTrue(TierRules.costPercent(g) > TierRules.costPercent(g - 1));
                assertTrue(TierRules.failCooldownTicks(g) > TierRules.failCooldownTicks(g - 1));
            }
        }
        assertEquals(0, TierRules.catalystsTo(0).length);
        assertEquals(0, TierRules.catalystsTo(7).length);
        assertEquals(0.0, TierRules.successChance(0));
        assertEquals(0.0, TierRules.costPercent(7));
    }

    @Test
    void theTopCatalystIsTheGenesisSeedAndGodTakesHihiirokaneOrADevilsCore() {
        assertEquals("nusmp:genesis_seed", TierRules.catalystsTo(6)[0].itemId());
        boolean hihi = false, core = false;
        for (Catalyst c : TierRules.catalystsTo(5)) {
            hihi |= c.itemId().equals("tensura:hihiirokane_ingot");
            core |= c.itemId().equals("nusmp:devils_core");
        }
        assertTrue(hihi && core);
    }

    @Test
    void theRollDecidesAndTheFirstJumpsAreSafe() {
        assertTrue(TierRules.succeeds(1, 0.999999), "the first jump cannot fail");
        assertTrue(TierRules.succeeds(6, 0.39));
        assertFalse(TierRules.succeeds(6, 0.40));
        assertFalse(TierRules.succeeds(0, 0.0));
        assertFalse(TierRules.losesCatalystOnFail(1));
        assertFalse(TierRules.losesCatalystOnFail(2));
        assertTrue(TierRules.losesCatalystOnFail(3));
        assertTrue(TierRules.losesCatalystOnFail(6));
    }

    @Test
    void theNextTargetClimbsOneGradeAndStopsAtTheTop() {
        assertEquals(1, TierRules.nextTarget(0));
        assertEquals(6, TierRules.nextTarget(5));
        assertEquals(-1, TierRules.nextTarget(6));
        assertEquals(-1, TierRules.nextTarget(9));
    }

    @Test
    void theCatalystListIsACopy() {
        Catalyst[] a = TierRules.catalystsTo(1);
        a[0] = null;
        assertNotNull(TierRules.catalystsTo(1)[0]);
    }
}
