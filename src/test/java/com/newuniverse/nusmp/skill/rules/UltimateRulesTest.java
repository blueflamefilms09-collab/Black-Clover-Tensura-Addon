package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: every Ultimate must be provably stronger or different from its base, and the god-tier knobs must stay inside safe bounds. */
class UltimateRulesTest {
    @Test
    void tierPicksAndMasteryScale() {
        assertEquals(1, Tier.BASE.pick(1, 2, 3));
        assertEquals(2, Tier.MASTERED.pick(1, 2, 3));
        assertEquals(3, Tier.ULTIMATE.pick(1, 2, 3));
        assertEquals(Tier.BASE, Tier.of(false, false));
        assertEquals(Tier.MASTERED, Tier.of(true, false));
        assertEquals(Tier.ULTIMATE, Tier.of(false, true));
        assertEquals(Tier.ULTIMATE, Tier.of(true, true));
        assertEquals(1.0, Tier.MASTERED.masteryScale(true));
        assertEquals(1.0, Tier.ULTIMATE.masteryScale(false));
        assertTrue(Tier.ULTIMATE.masteryScale(true) > 1.0);
        assertTrue(Tier.ULTIMATE.atLeast(Tier.MASTERED));
        assertFalse(Tier.BASE.atLeast(Tier.MASTERED));
    }

    @Test
    void immortalWillRisesFullerAndSoonerThanUnbreakableWill() {
        assertEquals(0.3, WillRules.restoredShare(Tier.BASE), 1e-9);
        assertEquals(0.5, WillRules.restoredShare(Tier.MASTERED), 1e-9);
        assertEquals(1.0, WillRules.restoredShare(Tier.ULTIMATE), 1e-9);
        assertTrue(WillRules.cooldownTicks(Tier.ULTIMATE) < WillRules.cooldownTicks(Tier.MASTERED));
        assertTrue(WillRules.cooldownTicks(Tier.MASTERED) < WillRules.cooldownTicks(Tier.BASE));
        assertTrue(WillRules.shockRadius(Tier.ULTIMATE) > 0);
        assertEquals(0.0, WillRules.shockRadius(Tier.MASTERED));
        assertEquals(2, WillRules.modes(Tier.ULTIMATE));
        assertEquals(1, WillRules.modes(Tier.BASE));
    }

    @Test
    void theWillRefusesDeathOnlyWhenReadyAndNotVoided() {
        assertTrue(WillRules.mayRefuse(false, false));
        assertFalse(WillRules.mayRefuse(true, false));
        assertFalse(WillRules.mayRefuse(false, true), "/kill and the void are not refused");
    }

    @Test
    void adollaBurstHasAThirdModeAndOutdamagesTheBase() {
        assertEquals(3, FireForceRules.modes(Tier.ULTIMATE));
        assertEquals(2, FireForceRules.modes(Tier.MASTERED));
        assertTrue(FireForceRules.kickDamage(Tier.ULTIMATE) > FireForceRules.kickDamage(Tier.MASTERED));
        assertTrue(FireForceRules.kickDamage(Tier.MASTERED) > FireForceRules.kickDamage(Tier.BASE));
        assertTrue(FireForceRules.kickRange(Tier.ULTIMATE) > FireForceRules.kickRange(Tier.BASE));
        assertTrue(FireForceRules.lightSpeedRange(Tier.ULTIMATE) > FireForceRules.lightSpeedRange(Tier.MASTERED));
        assertTrue(FireForceRules.dashPower(Tier.MASTERED) > FireForceRules.dashPower(Tier.BASE));
        assertTrue(FireForceRules.burstDamage() > FireForceRules.kickDamage(Tier.ULTIMATE));
        assertEquals(2, FireForceRules.BURST);
        assertTrue(FireForceRules.fireImmune(true));
    }

    @Test
    void bookMakerBarrageIsMasteredOnly() {
        assertFalse(FictionRules.barrageUnlocked(Tier.BASE));
        assertTrue(FictionRules.barrageUnlocked(Tier.MASTERED));
        assertEquals(2, FictionRules.modes());
        assertTrue(FictionRules.pinDamage(Tier.MASTERED) > FictionRules.pinDamage(Tier.BASE));
    }

    @Test
    void undoRestoresOnlyWhatWasLostAndNeverOverheals() {
        assertEquals(20f, FictionRules.undoHealth(20f, 5f, 20f));
        assertEquals(20f, FictionRules.undoHealth(30f, 5f, 20f), "never above max");
        assertEquals(15f, FictionRules.undoHealth(10f, 15f, 20f), "never lowers health");
    }

    @Test
    void theHealthHistoryShiftsNewestFirst() {
        float[] h = new float[FictionRules.HISTORY_SECONDS];
        for (int i = 1; i <= 7; i++) FictionRules.push(h, i);
        assertEquals(7f, h[0]);
        assertEquals(3f, h[FictionRules.oldestIndex()]);
        FictionRules.push(new float[0], 4f); // an empty history is harmless
    }

    @Test
    void limitlessHasFourModesAndPurpleIsTheMostExpensive() {
        assertEquals(4, GojoRules.MODES);
        assertEquals("Infinity", GojoRules.INFINITY_KEY);
        double purple = GojoRules.cost(GojoRules.PURPLE, false);
        assertTrue(purple > GojoRules.cost(GojoRules.RED, false));
        assertTrue(purple > GojoRules.cost(GojoRules.BLUE, false));
        assertTrue(GojoRules.cost(GojoRules.INFINITY, true) < GojoRules.cost(GojoRules.INFINITY, false), "mastered Infinity is cheaper to hold");
        assertTrue(GojoRules.purpleDamage(true) > GojoRules.purpleDamage(false));
        assertTrue(GojoRules.cooldown(GojoRules.PURPLE) > GojoRules.cooldown(GojoRules.RED));
        assertEquals(0, GojoRules.cooldown(GojoRules.INFINITY));
        assertEquals(1.0, GojoRules.infinityBlock());
    }

    @Test
    void requiemReturnsHarmMoreThanGoldExperienceEverHeals() {
        assertTrue(JoJoRules.returns(true, 0.99, false), "an open window returns everything");
        assertTrue(JoJoRules.returns(false, 0.1, false));
        assertFalse(JoJoRules.returns(false, 0.5, false));
        assertTrue(JoJoRules.returns(false, 0.3, true));
        assertTrue(JoJoRules.zeroTicks(true) > JoJoRules.zeroTicks(false));
        assertTrue(JoJoRules.lifeHeal(Tier.MASTERED) > JoJoRules.lifeHeal(Tier.BASE));
        assertEquals(2, JoJoRules.requiemModes());
        assertTrue(JoJoRules.burstRadius(true) > JoJoRules.burstRadius(false));
    }

    @Test
    void blackWingsReflectEverythingAndGlideOnlyInTheAir() {
        assertEquals(0.5, VectorRules.reflectShare(Tier.BASE), 1e-9);
        assertEquals(0.75, VectorRules.reflectShare(Tier.MASTERED), 1e-9);
        assertEquals(1.0, VectorRules.reflectShare(Tier.ULTIMATE), 1e-9);
        assertEquals(10.0, VectorRules.reflected(10, Tier.ULTIMATE) + VectorRules.taken(10, Tier.ULTIMATE), 1e-9);
        assertEquals(0.0, VectorRules.taken(10, Tier.ULTIMATE), 1e-9);
        assertEquals(10.0, VectorRules.reflected(10, Tier.BASE) + VectorRules.taken(10, Tier.BASE), 1e-9);
        assertEquals(0.0, VectorRules.reflected(-5, Tier.BASE));
        assertTrue(VectorRules.projectileBoost(Tier.ULTIMATE) > VectorRules.projectileBoost(Tier.MASTERED));
        assertTrue(VectorRules.canGlide(Tier.ULTIMATE, true, false, false));
        assertFalse(VectorRules.canGlide(Tier.MASTERED, true, false, false));
        assertFalse(VectorRules.canGlide(Tier.ULTIMATE, false, false, false));
        assertFalse(VectorRules.canGlide(Tier.ULTIMATE, true, true, false));
        assertFalse(VectorRules.canGlide(Tier.ULTIMATE, true, false, true));
        assertTrue(VectorRules.fallImmune(Tier.ULTIMATE));
        assertTrue(VectorRules.drain(Tier.ULTIMATE) > VectorRules.drain(Tier.BASE));
    }

    @Test
    void godTierKnobsStayInsideSafeBounds() {
        assertTrue(GodTierRules.kills(true));
        assertFalse(GodTierRules.kills(false), "bosses and protected players are only hurt");
        assertEquals(0.2, GodTierRules.dodgeChance(0.2, false), 1e-9);
        assertEquals(0.3, GodTierRules.dodgeChance(0.2, true), 1e-9);
        assertEquals(0.95, GodTierRules.dodgeChance(1.0, true), 1e-9);
        assertEquals(0.0, GodTierRules.dodgeChance(-1.0, false), 1e-9);
        assertTrue(GodTierRules.massCost() > GodTierRules.dieCost());
        assertTrue(GodTierRules.dieCooldown(true) < GodTierRules.dieCooldown(false));
        assertEquals(3, GodTierRules.MODES);
        assertTrue(GodTierRules.meteorDamage(true) > GodTierRules.meteorDamage(false));
        assertTrue(GodTierRules.rewriteTicks(true) > GodTierRules.rewriteTicks(false));
    }

    @Test
    void skillMathIsSafe() {
        assertEquals(0, SkillMath.cooldown(0, 2.0));
        assertEquals(40, SkillMath.cooldown(20, 2.0));
        assertEquals(0, SkillMath.cooldown(20, -1.0));
        assertEquals(0.0, SkillMath.cost(-5, 1.0));
        assertEquals(1, SkillMath.ticksToSeconds(1));
        assertEquals(2, SkillMath.ticksToSeconds(21));
        assertEquals(0, SkillMath.ticksToSeconds(0));
        assertEquals(0.0, SkillMath.missing(20, 20), 1e-9);
        assertEquals(1.0, SkillMath.missing(-4, 20), 1e-9);
        assertEquals(0.0, SkillMath.missing(5, 0), 1e-9);
        assertEquals(5.0, SkillMath.lerp(0, 10, 0.5), 1e-9);
        assertEquals(10.0, SkillMath.lerp(0, 10, 9), 1e-9);
    }
}
