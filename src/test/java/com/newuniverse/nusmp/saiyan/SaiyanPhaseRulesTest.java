package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the pure rules of the Dragon Ball rework (SaiyanPhaseRules, SaiyanRaceRules, SaiyanRitualRules, SaiyanNet2): every hard bound holds whatever the setting says, the Kaio-ken stages 1 to 3 are the
 * old table, the race stages only rise, and the ask range does not overlap the older one.
 */
class SaiyanPhaseRulesTest {
    @Test
    void theAskRangeIsAppendedAfterTheOlderOnesAndTheCycleIsInsideIt() {
        assertTrue(SaiyanNet2.FIRST > SaiyanNet.OP_ENERGY);
        assertFalse(SaiyanNet2.valid(SaiyanNet2.FIRST - 1));
        assertTrue(SaiyanNet2.valid(SaiyanNet2.FIRST) && SaiyanNet2.valid(SaiyanNet2.LAST));
        assertFalse(SaiyanNet2.valid(SaiyanNet2.LAST + 1));
        for (int op : SaiyanNet2.CYCLE) assertTrue(SaiyanNet2.valid(op));
        assertEquals("anime_multiverse.saiyan.tech2.22", SaiyanNet2.nameKey(22));
    }

    @Test
    void everyHardBoundHoldsWhateverTheSettingSays() {
        assertEquals(SaiyanPhaseRules.MAX_VANISH, SaiyanPhaseRules.vanishDistance(1.0e9), 1e-9);
        assertEquals(2.0, SaiyanPhaseRules.vanishDistance(Double.NaN), 1e-9);
        assertTrue(SaiyanPhaseRules.slamRadius(1.0e9, 1.0e9) <= SaiyanPhaseRules.MAX_SLAM_RADIUS);
        assertEquals(2.0, SaiyanPhaseRules.slamFactor(1.0e9), 1e-9);
        assertEquals(1.0, SaiyanPhaseRules.slamFactor(-5.0), 1e-9);
        assertTrue(SaiyanPhaseRules.counterReflect(1.0e9, 10.0) <= SaiyanPhaseRules.MAX_COUNTER_REFLECT);
        assertEquals(0.0, SaiyanPhaseRules.counterReflect(-4.0, 2.0), 1e-9);
        assertEquals(0, SaiyanPhaseRules.waveTicks(3, 10, 60));
        assertTrue(SaiyanPhaseRules.waveTicks(100000, 0, 100000) <= SaiyanPhaseRules.MAX_WAVE_TICKS);
        assertEquals(30, SaiyanPhaseRules.waveTicks(40, 10, 60));
        assertTrue(SaiyanPhaseRules.waveBeat(104, 100) && !SaiyanPhaseRules.waveBeat(103, 100) && !SaiyanPhaseRules.waveBeat(100, 100));
        assertEquals(SaiyanPhaseRules.MAX_SPHERE_TRAVEL, SaiyanPhaseRules.sphereTravel(1.0e9, 0.0));
        assertEquals(6, SaiyanPhaseRules.sphereTravel(0.0, 1.0));
        assertEquals(SaiyanPhaseRules.MAX_GRAVITY, SaiyanPhaseRules.adaptedLevel(Integer.MAX_VALUE, 1));
        assertEquals(0, SaiyanPhaseRules.adaptedLevel(-9, 300));
        assertEquals(1, SaiyanPhaseRules.trainingLevel(0));
        assertEquals(SaiyanPhaseRules.MAX_GRAVITY, SaiyanPhaseRules.trainingLevel(99));
        assertTrue(SaiyanPhaseRules.gravityPenalty(10, 0) <= 0.35 + 1e-9);
        assertTrue(SaiyanPhaseRules.gravityPenalty(5, 5) < SaiyanPhaseRules.gravityPenalty(5, 0), "an adapted body is slowed less");
        assertTrue(SaiyanPhaseRules.gravityCounts(true, 1000, -5000) && SaiyanPhaseRules.gravityCounts(false, 1000, 900) && !SaiyanPhaseRules.gravityCounts(false, 1000, 100));
        assertEquals(SaiyanPhaseRules.MAX_AVATAR_TICKS, SaiyanPhaseRules.avatarTicks(1.0e9));
        assertEquals(20, SaiyanPhaseRules.avatarTicks(-3.0));
        assertEquals(900, SaiyanPhaseRules.avatarTicks(45.0));
        assertEquals(SaiyanPhaseRules.MAX_AVATAR_SCALE, SaiyanPhaseRules.avatarScale(1.0e9), 1e-9);
        assertEquals(3.0, SaiyanPhaseRules.avatarScale(-1.0), 1e-9);
        assertTrue(SaiyanPhaseRules.avatarSwing(120, 100) && !SaiyanPhaseRules.avatarSwing(100, 100) && !SaiyanPhaseRules.avatarSwing(119, 100));
        assertTrue(SaiyanPhaseRules.avatarStands(9, 10) && !SaiyanPhaseRules.avatarStands(10, 10));
        assertTrue(SaiyanPhaseRules.avatarForm("true_ultra_instinct", true) && !SaiyanPhaseRules.avatarForm("super_saiyan_god", true) && SaiyanPhaseRules.avatarForm("super_saiyan_god", false));
    }

    @Test
    void kaiokenStagesOneToThreeAreTheOldTableAndTheExtraOnesAreOptIn() {
        for (int stage = 1; stage <= 3; stage++) {
            assertEquals(SaiyanForms.kaioDamageMul(stage), SaiyanPhaseRules.kaioDamage(stage), 1e-9);
            assertEquals(SaiyanForms.kaioDrainMul(stage), SaiyanPhaseRules.kaioDrain(stage), 1e-9);
            assertEquals(SaiyanForms.kaioStrainRate(stage), SaiyanPhaseRules.kaioStrain(stage), 1e-9);
        }
        for (double m : new double[] {0.0, 0.34, 0.67, 0.9, 1.0}) assertEquals(SaiyanForms.kaioMaxStage(m), SaiyanPhaseRules.kaioMaxStage(m, false), "extended off = the old stages at " + m);
        assertEquals(3, SaiyanPhaseRules.kaioMaxStage(0.8, true));
        assertEquals(4, SaiyanPhaseRules.kaioMaxStage(0.9, true));
        assertEquals(5, SaiyanPhaseRules.kaioMaxStage(1.0, true));
        assertTrue(SaiyanForms.kaioDamageMul(5) > SaiyanForms.kaioDamageMul(3), "the table holds stages 4 and 5");
        assertEquals(1.0, SaiyanForms.kaioDamageMul(-4), 1e-9);
        assertEquals(0.0, SaiyanPhaseRules.drainFactor("mastered_super_saiyan", 1.0), 1e-9);
        assertEquals(SaiyanForms.drainFactor(0.5), SaiyanPhaseRules.drainFactor("super_saiyan", 0.5), 1e-9);
    }

    @Test
    void theFormRulesAndTheMoonAreAsTheSpecSays() {
        assertTrue(SaiyanPhaseRules.berserkChance(0.0) > SaiyanPhaseRules.berserkChance(1.0));
        assertTrue(SaiyanPhaseRules.isBerserk("super_saiyan_2_berserk") && !SaiyanPhaseRules.isBerserk("super_saiyan_2"));
        assertTrue(SaiyanPhaseRules.isApe("oozaru") && SaiyanPhaseRules.isApe("golden_great_ape") && !SaiyanPhaseRules.isApe("super_saiyan"));
        assertTrue(SaiyanPhaseRules.moonOk(0, 15000, false));
        assertFalse(SaiyanPhaseRules.moonOk(0, 6000, false));
        assertFalse(SaiyanPhaseRules.moonOk(4, 15000, false));
        assertTrue(SaiyanPhaseRules.moonOk(4, 6000, true));
        assertTrue(SaiyanPhaseRules.reinforceOut(0.2) > 1.0 && SaiyanPhaseRules.reinforceTaken(0.15) < 1.0 && SaiyanPhaseRules.reinforceOut(99.0) <= 2.5 + 1e-9);
    }

    @Test
    void theRaceStagesOnlyRiseAndChainWithEveryRequirement() {
        SaiyanRaceRules.Need need = SaiyanRaceRules.Need.DEFAULT;
        SaiyanRaceRules.Progress nothing = new SaiyanRaceRules.Progress(1.0e12, 99, Set.of(), Set.of());
        assertEquals(SaiyanRaceRules.SAIYAN, SaiyanRaceRules.next(0, need, nothing), "a Saiyan with nothing earned stays at the first stage");
        assertEquals(SaiyanRaceRules.SAIYAN, SaiyanRaceRules.next(SaiyanRaceRules.SAIYAN, need, new SaiyanRaceRules.Progress(10, 0, Set.of("super_saiyan_3"), Set.of())));
        assertEquals(SaiyanRaceRules.PRIMAL, SaiyanRaceRules.next(SaiyanRaceRules.SAIYAN, need, new SaiyanRaceRules.Progress(400_000, 0, Set.of("super_saiyan_3"), Set.of())));
        assertEquals(SaiyanRaceRules.DIVINE, SaiyanRaceRules.next(SaiyanRaceRules.PRIMAL, need, new SaiyanRaceRules.Progress(1, 0, Set.of(), Set.of(SaiyanRaceRules.RITUAL_FLAG))));
        SaiyanRaceRules.Progress all = new SaiyanRaceRules.Progress(1.0e12, 99, Set.of("super_saiyan_3", "perfected_ultra_instinct"), Set.of(SaiyanRaceRules.RITUAL_FLAG));
        assertEquals(SaiyanRaceRules.AUTONOMOUS, SaiyanRaceRules.next(SaiyanRaceRules.SAIYAN, need, all), "every requirement met chains to the last stage");
        assertEquals(SaiyanRaceRules.AUTONOMOUS, SaiyanRaceRules.next(SaiyanRaceRules.AUTONOMOUS, need, nothing), "a stage is never lost");
        assertTrue(SaiyanRaceRules.missing(SaiyanRaceRules.AUTONOMOUS, need, nothing).isEmpty());
        assertFalse(SaiyanRaceRules.missing(SaiyanRaceRules.SAIYAN, need, new SaiyanRaceRules.Progress(0, 0, Set.of(), Set.of())).isEmpty());
        assertEquals("anime_multiverse.saiyan.racestage.primal_saiyan", SaiyanRaceRules.nameKey(SaiyanRaceRules.PRIMAL));
        assertEquals(SaiyanRaceRules.NONE, SaiyanRaceRules.clampStage(-9));
        assertEquals(SaiyanRaceRules.AUTONOMOUS, SaiyanRaceRules.clampStage(99));
    }

    @Test
    void theStageBuffsAreModestRisingAndScaled() {
        for (int s = SaiyanRaceRules.SAIYAN; s < SaiyanRaceRules.AUTONOMOUS; s++) assertTrue(SaiyanRaceRules.bonus(s + 1, 1.0).attack() > SaiyanRaceRules.bonus(s, 1.0).attack());
        assertEquals(0.0, SaiyanRaceRules.bonus(SaiyanRaceRules.AUTONOMOUS, 0.0).attack(), 1e-9);
        assertEquals(0.0, SaiyanRaceRules.bonus(SaiyanRaceRules.NONE, 2.0).health(), 1e-9);
        assertTrue(SaiyanRaceRules.bonus(SaiyanRaceRules.AUTONOMOUS, 99.0).knockback() <= 0.8 + 1e-9);
        assertEquals(0.0, SaiyanRaceRules.bonus(SaiyanRaceRules.AUTONOMOUS, Double.NaN).attack(), 1e-9);
    }

    @Test
    void theRitualRingAndItsBoundsAreFixed() {
        assertFalse(SaiyanRitualRules.inRing(1.0, 1.0));
        assertTrue(SaiyanRitualRules.inRing(3.0, 4.0) && !SaiyanRitualRules.inRing(6.0, 6.0));
        assertEquals(SaiyanRitualRules.MAX_HELPERS, SaiyanRitualRules.helpers(500));
        assertEquals(1, SaiyanRitualRules.helpers(-5));
        assertEquals(SaiyanRitualRules.MAX_TICKS, SaiyanRitualRules.ticks(1_000_000));
        assertEquals(40, SaiyanRitualRules.ticks(1));
        assertTrue(SaiyanRitualRules.casterReady(false, 2_000_000, 1_000_000, 0, 1.0e8, true, false));
        assertFalse(SaiyanRitualRules.casterReady(false, 2_000_000, 1_000_000, 0, 1.0e8, false, false), "the Super Saiyan must be mastered");
        assertFalse(SaiyanRitualRules.casterReady(false, 2_000_000, 1_000_000, 0, 1.0e8, true, true), "once is enough");
        assertTrue(SaiyanRitualRules.casterReady(true, 1, 1_000_000, 2.0e8, 1.0e8, true, false) && !SaiyanRitualRules.casterReady(true, 9.0e9, 1_000_000, 5.0e7, 1.0e8, true, false));
        assertTrue(SaiyanRitualRules.circleHolds(5, 5) && !SaiyanRitualRules.circleHolds(4, 5));
        assertTrue(SaiyanRitualRules.done(600, 0, 600) && !SaiyanRitualRules.done(599, 0, 600));
        assertEquals(50, SaiyanRitualRules.percent(300, 0, 600));
        assertEquals(100, SaiyanRitualRules.percent(9999, 0, 600));
        assertTrue(SaiyanRitualRules.checkTick(110, 100) && !SaiyanRitualRules.checkTick(105, 100));
    }
}
