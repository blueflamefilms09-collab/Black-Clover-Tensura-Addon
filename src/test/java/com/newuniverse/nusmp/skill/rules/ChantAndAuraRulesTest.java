package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the chants (hold, finish every line, release) and the toggle curves are the feel of the DanMachi and Hell Mode skills. */
class ChantAndAuraRulesTest {
    @Test
    void theChantsHaveTheLinesTheLoreGives() {
        assertEquals(3, ChantRules.Spell.WYNN_FIMBULVETR.lines);
        assertEquals(3, ChantRules.Spell.REA_LAEVATEINN.lines);
        assertEquals(1, ChantRules.Spell.ARCS_RAY.lines);
        assertEquals(2, ChantRules.Spell.FUTSUNOMITAMA.lines);
        assertEquals(3, ChantRules.Spell.UCHIDE_NO_KOZUCHI.lines);
    }

    @Test
    void aChantNeedsEveryLineAndMasteryIsFaster() {
        ChantRules.Spell s = ChantRules.Spell.WYNN_FIMBULVETR;
        int tpl = ChantRules.ticksPerLine(Tier.BASE);
        assertEquals(0, ChantRules.linesDone(0, s, Tier.BASE));
        assertEquals(0, ChantRules.linesDone(tpl - 1, s, Tier.BASE));
        assertEquals(1, ChantRules.linesDone(tpl, s, Tier.BASE));
        assertEquals(3, ChantRules.linesDone(tpl * 10, s, Tier.BASE), "never more than the spell has");
        assertFalse(ChantRules.complete(tpl * 2, s, Tier.BASE));
        assertTrue(ChantRules.complete(tpl * 3, s, Tier.BASE));
        assertTrue(ChantRules.totalTicks(s, Tier.MASTERED) < ChantRules.totalTicks(s, Tier.BASE));
        assertTrue(ChantRules.maxHeldTicks(s, Tier.BASE) > ChantRules.totalTicks(s, Tier.BASE));
        assertEquals(0, ChantRules.linesDone(-3, s, Tier.BASE));
    }

    @Test
    void theShortChantIsDoneInOneLine() {
        ChantRules.Spell s = ChantRules.Spell.ARCS_RAY;
        assertTrue(ChantRules.complete(ChantRules.ticksPerLine(Tier.BASE), s, Tier.BASE));
    }

    @Test
    void everySpellGetsStrongerWhenMastered() {
        assertTrue(ChantRules.blizzardDamage(Tier.MASTERED) > ChantRules.blizzardDamage(Tier.BASE));
        assertTrue(ChantRules.blizzardRadius(Tier.MASTERED) > ChantRules.blizzardRadius(Tier.BASE));
        assertTrue(ChantRules.pillarWaves(Tier.MASTERED) > ChantRules.pillarWaves(Tier.BASE));
        assertTrue(ChantRules.pillarsPerWave(Tier.MASTERED) > ChantRules.pillarsPerWave(Tier.BASE));
        assertTrue(ChantRules.rayDamage(Tier.MASTERED) > ChantRules.rayDamage(Tier.BASE));
        assertTrue(ChantRules.rayRange(Tier.MASTERED) > ChantRules.rayRange(Tier.BASE));
        assertTrue(ChantRules.gravityRadius(Tier.MASTERED) > ChantRules.gravityRadius(Tier.BASE));
        assertTrue(ChantRules.gravityTicks(Tier.MASTERED) > ChantRules.gravityTicks(Tier.BASE));
        assertTrue(ChantRules.boostTicks(Tier.MASTERED) > ChantRules.boostTicks(Tier.BASE));
        assertTrue(ChantRules.rayUndeadFactor() > 1.0);
    }

    @Test
    void zonesPulseAtTheirInterval() {
        assertEquals(3, ChantRules.pulses(60, 20));
        assertEquals(0, ChantRules.pulses(10, 20));
        assertEquals(0, ChantRules.pulses(60, 0));
        assertEquals(5, ChantRules.pulses(ChantRules.blizzardTicks(Tier.MASTERED), ChantRules.blizzardInterval()));
    }

    @Test
    void theMostExpensiveChantIsTheLongestCooldown() {
        double maxCost = 0;
        for (ChantRules.Spell s : ChantRules.Spell.values()) {
            assertTrue(s.cost > 0 && s.cooldown > 0 && s.lines >= 1);
            maxCost = Math.max(maxCost, s.cost);
        }
        assertTrue(ChantRules.Spell.UCHIDE_NO_KOZUCHI.cost >= maxCost - 1000);
        assertTrue(ChantRules.Spell.UCHIDE_NO_KOZUCHI.cooldown > ChantRules.Spell.ARCS_RAY.cooldown);
    }

    @Test
    void hellMultiplierRisesAsHealthFalls() {
        double full = AuraRules.hellMultiplier(20, 20, Tier.BASE);
        double low = AuraRules.hellMultiplier(2, 20, Tier.BASE);
        assertEquals(1.0, full, 1e-9);
        assertTrue(low > 1.9 && low < 2.2, "roughly double at 10% health: " + low);
        assertTrue(AuraRules.hellMultiplier(2, 20, Tier.MASTERED) > low);
        assertTrue(AuraRules.hellMultiplier(2, 20, Tier.ULTIMATE) > AuraRules.hellMultiplier(2, 20, Tier.MASTERED));
        assertEquals(1.0, AuraRules.hellMultiplier(5, 0, Tier.ULTIMATE), 1e-9, "no division by zero");
    }

    @Test
    void theConquerorHealsAndBurnsOnlyWhenHurt() {
        assertEquals(0.0, AuraRules.hellLifesteal(15, 20, Tier.ULTIMATE));
        assertTrue(AuraRules.hellLifesteal(5, 20, Tier.ULTIMATE) > 0);
        assertEquals(0.0, AuraRules.hellLifesteal(5, 20, Tier.MASTERED), "the Unique never steals life");
        assertFalse(AuraRules.hellfire(15, 20, Tier.ULTIMATE));
        assertTrue(AuraRules.hellfire(5, 20, Tier.ULTIMATE));
        assertFalse(AuraRules.hellfire(5, 20, Tier.MASTERED));
        assertTrue(AuraRules.berserk(5, 20, Tier.ULTIMATE));
        assertFalse(AuraRules.berserk(10, 20, Tier.ULTIMATE));
    }

    @Test
    void hatiTurnsAbsorbedDamageIntoStackingStrengthUpToACap() {
        assertEquals(0, AuraRules.hatiNextAmp(-1, Tier.BASE));
        assertEquals(1, AuraRules.hatiNextAmp(0, Tier.BASE));
        assertEquals(1, AuraRules.hatiNextAmp(1, Tier.BASE), "capped");
        assertEquals(3, AuraRules.hatiNextAmp(2, Tier.MASTERED));
        assertEquals(3, AuraRules.hatiNextAmp(3, Tier.MASTERED));
        assertEquals(4.0, AuraRules.hatiAbsorbed(10, Tier.BASE), 1e-9);
        assertEquals(6.0, AuraRules.hatiAbsorbed(10, Tier.MASTERED), 1e-9);
        assertEquals(0.0, AuraRules.hatiHeal(8, Tier.BASE));
        assertEquals(2.0, AuraRules.hatiHeal(8, Tier.MASTERED), 1e-9);
        assertEquals(0.0, AuraRules.hatiAbsorbed(-4, Tier.BASE));
    }

    @Test
    void auraRangesAndHealingGrowWithMastery() {
        assertTrue(AuraRules.hawkRange(Tier.MASTERED) > AuraRules.hawkRange(Tier.BASE));
        assertTrue(AuraRules.spiritRadius(Tier.MASTERED) > AuraRules.spiritRadius(Tier.BASE));
        assertTrue(AuraRules.spiritHeal(Tier.MASTERED) > AuraRules.spiritHeal(Tier.BASE));
        assertTrue(AuraRules.sixRange(Tier.MASTERED) > AuraRules.sixRange(Tier.BASE));
        assertTrue(AuraRules.sixDrain(Tier.MASTERED) < AuraRules.sixDrain(Tier.BASE));
        assertFalse(AuraRules.spiritCleanses(Tier.BASE));
        assertTrue(AuraRules.spiritCleanses(Tier.MASTERED));
    }
}
