package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.ExtraRules;
import com.newuniverse.nusmp.elite.skills.rules.IntrinsicRules;
import com.newuniverse.nusmp.elite.skills.rules.MagicRules;
import com.newuniverse.nusmp.elite.skills.rules.SnapshotRing;
import com.newuniverse.nusmp.elite.skills.rules.UltimateRules;
import com.newuniverse.nusmp.elite.skills.rules.UniqueRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the numbers of the 21 elite skills were designed without the original addon, so the RELATIONS between them are what must hold: a longer charge is stronger and dearer, a higher
 * Kaioken rung is stronger and burns more, the Unique is never stronger than its mastered self, a rewind from near death never returns you to near death. These checks pin those
 * relations without Minecraft.
 */
class EliteSkillRulesTest {
    // ---------------------------------------------------------------- Kamehameha
    @Test
    void kamehamehaLevelsFollowTheHoldAndMasteryChargesFaster() {
        assertEquals(0, MagicRules.Kamehameha.level(0, false));
        assertEquals(0, MagicRules.Kamehameha.level(19, false));
        assertEquals(1, MagicRules.Kamehameha.level(20, false));
        assertEquals(5, MagicRules.Kamehameha.level(100, false));
        assertEquals(5, MagicRules.Kamehameha.level(5000, false), "capped at the top level");
        assertEquals(5, MagicRules.Kamehameha.level(70, true));
        assertTrue(MagicRules.Kamehameha.level(45, true) > MagicRules.Kamehameha.level(45, false));
        assertEquals(0, MagicRules.Kamehameha.level(-5, false));
    }

    @Test
    void kamehamehaGrowsWithEveryLevelAndAFizzleIsFree() {
        assertFalse(MagicRules.Kamehameha.fires(0));
        assertEquals(0.0, MagicRules.Kamehameha.cost(0));
        assertEquals(0.0, MagicRules.Kamehameha.damage(0, false));
        for (int lv = 1; lv < MagicRules.Kamehameha.MAX_LEVEL; lv++) {
            assertTrue(MagicRules.Kamehameha.cost(lv + 1) > MagicRules.Kamehameha.cost(lv), "cost " + lv);
            assertTrue(MagicRules.Kamehameha.damage(lv + 1, false) > MagicRules.Kamehameha.damage(lv, false), "damage " + lv);
            assertTrue(MagicRules.Kamehameha.range(lv + 1) > MagicRules.Kamehameha.range(lv), "range " + lv);
            assertTrue(MagicRules.Kamehameha.width(lv + 1) > MagicRules.Kamehameha.width(lv), "width " + lv);
            assertTrue(MagicRules.Kamehameha.cooldownTicks(lv + 1) > MagicRules.Kamehameha.cooldownTicks(lv), "cooldown " + lv);
        }
        assertTrue(MagicRules.Kamehameha.damage(3, true) > MagicRules.Kamehameha.damage(3, false));
        assertEquals(0, MagicRules.Kamehameha.recoilTicks(1));
        assertTrue(MagicRules.Kamehameha.recoilTicks(5) > 0);
        assertTrue(MagicRules.Kamehameha.finale(5) && !MagicRules.Kamehameha.finale(4));
        assertEquals(0, MagicRules.Kamehameha.ticksToNext(100, false));
        assertEquals(5, MagicRules.Kamehameha.ticksToNext(35, false));
    }

    // ---------------------------------------------------------------- Gravity Well
    @Test
    void gravityWellPullsHardestAtTheCentreAndNotAtAll_outside() {
        double r = MagicRules.GravityWell.radius(false);
        assertEquals(1.0, MagicRules.GravityWell.falloff(0, r), 1e-9);
        assertEquals(0.4, MagicRules.GravityWell.falloff(r, r), 1e-9);
        assertEquals(0.0, MagicRules.GravityWell.falloff(r + 0.01, r));
        assertTrue(MagicRules.GravityWell.pull(1, r, false) > MagicRules.GravityWell.pull(4, r, false));
        assertEquals(0.0, MagicRules.GravityWell.pull(r + 1, r, false));
        assertTrue(MagicRules.GravityWell.pull(0, r, false) <= MagicRules.GravityWell.MAX_PULL);
        assertTrue(MagicRules.GravityWell.pull(1, r, true) < MagicRules.GravityWell.pull(1, r, false), "a boss is pulled less");
        assertTrue(MagicRules.GravityWell.radius(true) > r);
        assertEquals(12, MagicRules.GravityWell.pulses(false));
        assertEquals(16, MagicRules.GravityWell.pulses(true));
        assertTrue(MagicRules.GravityWell.collapseDamage(0, r, false) > 4 * MagicRules.GravityWell.pulseDamage(0, r, false));
        assertTrue(MagicRules.GravityWell.collapseCost() < MagicRules.GravityWell.COST);
    }

    // ---------------------------------------------------------------- Static Chain
    @Test
    void staticChainVisitsTheNearestUnvisitedWithinRangeAndStops() {
        double[][] p = {{0, 0, 0}, {5, 0, 0}, {10, 0, 0}, {30, 0, 0}, {6, 0, 1}};
        int[] o = MagicRules.StaticChain.order(p, 0, 4, 7.0);
        assertEquals(4, o.length, "the far one at x=30 is out of every jump");
        assertEquals(0, o[0]);
        assertEquals(1, o[1], "nearest first");
        assertEquals(4, o[2]);
        assertEquals(2, o[3]);
        int[] shortChain = MagicRules.StaticChain.order(p, 0, 1, 7.0);
        assertEquals(2, shortChain.length, "one jump means two victims");
        assertEquals(0, MagicRules.StaticChain.order(p, 9, 4, 7.0).length, "a bad start gives an empty chain");
        assertEquals(1, MagicRules.StaticChain.order(new double[][]{{0, 0, 0}}, 0, 4, 7.0).length);
    }

    @Test
    void staticChainDamageFallsOffAndMasteryAddsJumps() {
        assertEquals(12.0, MagicRules.StaticChain.linkDamage(0, false), 1e-9);
        assertTrue(MagicRules.StaticChain.linkDamage(1, false) < MagicRules.StaticChain.linkDamage(0, false));
        assertTrue(MagicRules.StaticChain.maxJumps(true) > MagicRules.StaticChain.maxJumps(false));
        assertTrue(MagicRules.StaticChain.yank(MagicRules.StaticChain.TETHER_RADIUS + 0.1));
        assertFalse(MagicRules.StaticChain.yank(1.0));
    }

    // ---------------------------------------------------------------- Extras
    @Test
    void ancientSoulFragmentsAreCappedAndSpentInLots() {
        assertEquals(100, ExtraRules.AncientSoul.add(99, 5));
        assertEquals(0, ExtraRules.AncientSoul.add(0, -3));
        assertFalse(ExtraRules.AncientSoul.canSpend(19));
        assertTrue(ExtraRules.AncientSoul.canSpend(20));
        assertEquals(5, ExtraRules.AncientSoul.spend(25));
        assertEquals(19, ExtraRules.AncientSoul.spend(19), "an unaffordable spend changes nothing");
        assertTrue(ExtraRules.AncientSoul.gainForKill(true) > ExtraRules.AncientSoul.gainForKill(false));
        assertTrue(ExtraRules.AncientSoul.healShare(true) > ExtraRules.AncientSoul.healShare(false));
    }

    @Test
    void ironResolveOnlyStacksWhenWoundedAndIsCapped() {
        assertEquals(2, ExtraRules.IronResolve.stackAfterHit(2, 0.9, false), "healthy: no new stack, none lost");
        assertEquals(3, ExtraRules.IronResolve.stackAfterHit(2, 0.5, false));
        assertEquals(5, ExtraRules.IronResolve.stackAfterHit(5, 0.1, false));
        assertEquals(7, ExtraRules.IronResolve.stackAfterHit(7, 0.1, true));
        assertEquals(0.0, ExtraRules.IronResolve.reduction(0, false));
        assertEquals(ExtraRules.IronResolve.MAX_REDUCTION, ExtraRules.IronResolve.reduction(50, false));
        assertTrue(ExtraRules.IronResolve.reduction(0, true) >= ExtraRules.IronResolve.BRACE_REDUCTION);
        assertFalse(ExtraRules.IronResolve.knockbackImmune(2, false));
        assertTrue(ExtraRules.IronResolve.knockbackImmune(3, false));
        assertTrue(ExtraRules.IronResolve.knockbackImmune(0, true));
        assertTrue(ExtraRules.IronResolve.expired(1000, 800));
        assertFalse(ExtraRules.IronResolve.expired(1000, 900));
    }

    @Test
    void meditationFocusBuildsWithStillnessAndNeverExceedsTheCap() {
        assertEquals(0, ExtraRules.Meditation.focus(5));
        assertEquals(1, ExtraRules.Meditation.focus(6));
        assertEquals(5, ExtraRules.Meditation.focus(1000));
        assertEquals(0.0, ExtraRules.Meditation.regenShare(0, true));
        assertTrue(ExtraRules.Meditation.regenShare(5, false) > ExtraRules.Meditation.regenShare(1, false));
        assertTrue(ExtraRules.Meditation.regenShare(5, true) > ExtraRules.Meditation.regenShare(5, false));
        assertTrue(ExtraRules.Meditation.regenShare(5, true) < 0.05, "never more than 5% of the pool per second");
        assertEquals(0.0, ExtraRules.Meditation.healPerSecond(2));
        assertTrue(ExtraRules.Meditation.healPerSecond(5) > 0);
        assertTrue(ExtraRules.Meditation.still(0.0));
        assertFalse(ExtraRules.Meditation.still(1.0));
        assertTrue(ExtraRules.Meditation.inCombat(100, 50));
        assertFalse(ExtraRules.Meditation.inCombat(300, 50));
    }

    @Test
    void runesmithModesMapToRunesAndModeZeroIsTheReading() {
        assertNull(ExtraRules.Runesmith.runeForMode(0));
        assertEquals(ExtraRules.Runesmith.Rune.FIRE, ExtraRules.Runesmith.runeForMode(1));
        assertEquals(ExtraRules.Runesmith.Rune.VITALITY, ExtraRules.Runesmith.runeForMode(4));
        assertNull(ExtraRules.Runesmith.runeForMode(5));
        assertEquals(0.0, ExtraRules.Runesmith.READ_COST);
        assertTrue(ExtraRules.Runesmith.durationTicks(true) > ExtraRules.Runesmith.durationTicks(false));
        assertTrue(ExtraRules.Runesmith.thunderBonus(10, false) > 0);
        assertTrue(ExtraRules.Runesmith.vitalityHeal(10, true) > ExtraRules.Runesmith.vitalityHeal(10, false));
        assertTrue(ExtraRules.Runesmith.active(10, 11));
        assertFalse(ExtraRules.Runesmith.active(11, 11));
    }

    @Test
    void scavengerAddsAtMostOneStackAndOnlyWhenThereIsLoot() {
        assertTrue(ExtraRules.Scavenger.duplicates(0.0, false));
        assertFalse(ExtraRules.Scavenger.duplicates(0.99, true));
        assertTrue(ExtraRules.Scavenger.duplicates(0.3, true));
        assertFalse(ExtraRules.Scavenger.duplicates(0.3, false));
        assertEquals(1, ExtraRules.Scavenger.extraStacks(true, 9));
        assertEquals(0, ExtraRules.Scavenger.extraStacks(true, 0));
        assertEquals(0, ExtraRules.Scavenger.extraStacks(false, 3));
        assertTrue(ExtraRules.Scavenger.sniffRadius(true) > ExtraRules.Scavenger.sniffRadius(false));
    }

    // ---------------------------------------------------------------- Intrinsics
    @Test
    void kaiokenLadderIsMonotonicAndNeverKillsItsWearer() {
        for (int lv = 0; lv + 1 < IntrinsicRules.Kaioken.levels(); lv++) {
            assertTrue(IntrinsicRules.Kaioken.multiplier(lv + 1) > IntrinsicRules.Kaioken.multiplier(lv));
            assertTrue(IntrinsicRules.Kaioken.damageBonus(lv + 1) > IntrinsicRules.Kaioken.damageBonus(lv));
            assertTrue(IntrinsicRules.Kaioken.speedBonus(lv + 1) > IntrinsicRules.Kaioken.speedBonus(lv));
            assertTrue(IntrinsicRules.Kaioken.healthDrain(lv + 1, 20) > IntrinsicRules.Kaioken.healthDrain(lv, 20), "drain " + lv);
            assertTrue(IntrinsicRules.Kaioken.magiculeDrain(lv + 1) > IntrinsicRules.Kaioken.magiculeDrain(lv));
        }
        assertEquals(2, IntrinsicRules.Kaioken.multiplier(0));
        assertEquals(20, IntrinsicRules.Kaioken.multiplier(4));
        assertEquals(3, IntrinsicRules.Kaioken.unlocked(false));
        assertEquals(5, IntrinsicRules.Kaioken.unlocked(true));
        assertEquals(2, IntrinsicRules.Kaioken.clampLevel(99, false));
        assertEquals(0, IntrinsicRules.Kaioken.clampLevel(-4, true));
        assertEquals(0, IntrinsicRules.Kaioken.next(2, false, false), "wraps round");
        assertEquals(2, IntrinsicRules.Kaioken.next(0, false, true));
        assertFalse(IntrinsicRules.Kaioken.canSustain(4.0, 20.0), "4 hit points is the floor");
        assertFalse(IntrinsicRules.Kaioken.canSustain(10.0, 100.0), "15% of a big pool is the floor");
        assertTrue(IntrinsicRules.Kaioken.canSustain(10.0, 20.0));
        assertTrue(IntrinsicRules.Kaioken.aftershockTicks(4, 30) > IntrinsicRules.Kaioken.aftershockTicks(0, 1));
        assertTrue(IntrinsicRules.Kaioken.aftershockTicks(4, 1000) <= 1200);
    }

    @Test
    void mechanicalEyeAndHandsHaveSaneNumbers() {
        assertTrue(IntrinsicRules.MechanicalEye.scanRange(true) > IntrinsicRules.MechanicalEye.scanRange(false));
        assertEquals(1.0, IntrinsicRules.MechanicalEye.markedMultiplier(false));
        assertEquals(1.2, IntrinsicRules.MechanicalEye.markedMultiplier(true), 1e-9);
        assertTrue(IntrinsicRules.MechanicalEye.markActive(5, 6));
        assertFalse(IntrinsicRules.MechanicalEye.markActive(6, 6));
        assertTrue(IntrinsicRules.MechanicalHands.blockReach(true) > IntrinsicRules.MechanicalHands.blockReach(false));
        assertTrue(IntrinsicRules.MechanicalHands.pullSpeed(0) >= 0.15 && IntrinsicRules.MechanicalHands.pullSpeed(100) <= 0.6);
        assertEquals(0, IntrinsicRules.MechanicalHands.mendPoints(100, 0, false), "nothing damaged, nothing mended");
        assertEquals(15, IntrinsicRules.MechanicalHands.mendPoints(100, 80, false));
        assertEquals(5, IntrinsicRules.MechanicalHands.mendPoints(100, 5, false), "never more than the damage");
        assertEquals(0, IntrinsicRules.MechanicalHands.mendPoints(0, 5, false));
        assertEquals(1, IntrinsicRules.MechanicalHands.mendPoints(2, 2, false), "at least one point");
    }

    // ---------------------------------------------------------------- Uniques
    @Test
    void crocodileHideThickensInWaterAndWithMastery() {
        assertTrue(UniqueRules.Crocodile.reduction(false, true) > UniqueRules.Crocodile.reduction(false, false));
        assertTrue(UniqueRules.Crocodile.reduction(true, false) > UniqueRules.Crocodile.reduction(false, false));
        assertTrue(UniqueRules.Crocodile.reduction(true, true) < 0.8, "never close to immunity");
        assertEquals(1.0, UniqueRules.Crocodile.waterDamage(false, true));
        assertTrue(UniqueRules.Crocodile.waterDamage(true, false) > 1.0);
        assertTrue(UniqueRules.Crocodile.bindTicks(true) > UniqueRules.Crocodile.bindTicks(false));
    }

    @Test
    void hecateFormsPeakAtTheirOwnMoonAndTheDayWeakensThem() {
        assertEquals(UniqueRules.Hecate.Form.MAIDEN, UniqueRules.Hecate.form(0));
        assertEquals(UniqueRules.Hecate.Form.CRONE, UniqueRules.Hecate.form(2));
        assertEquals(UniqueRules.Hecate.Form.CRONE, UniqueRules.Hecate.form(99), "out of range clamps");
        UniqueRules.Hecate.Form m = UniqueRules.Hecate.Form.MOTHER, c = UniqueRules.Hecate.Form.CRONE, a = UniqueRules.Hecate.Form.MAIDEN;
        assertEquals(1.4, UniqueRules.Hecate.moonPower(m, 0, true), 1e-9, "the Mother is full at the full moon");
        assertEquals(1.4, UniqueRules.Hecate.moonPower(c, 4, true), 1e-9, "the Crone is strongest at the new moon");
        assertEquals(1.4, UniqueRules.Hecate.moonPower(a, 6, true), 1e-9, "the Maiden waxes");
        assertTrue(UniqueRules.Hecate.moonPower(m, 4, true) < UniqueRules.Hecate.moonPower(m, 0, true));
        assertTrue(UniqueRules.Hecate.moonPower(m, 0, false) < UniqueRules.Hecate.moonPower(m, 0, true));
        for (int phase = -8; phase < 16; phase++) {
            for (UniqueRules.Hecate.Form f : UniqueRules.Hecate.Form.values()) {
                double v = UniqueRules.Hecate.moonPower(f, phase, true);
                assertTrue(v >= 0.85 && v <= 1.4, "phase " + phase + " " + f);
            }
        }
        assertTrue(UniqueRules.Hecate.cost(m) > 0 && UniqueRules.Hecate.cooldown(a) > 0);
    }

    @Test
    void voidEdgeScalesWithArmourButIsCapped() {
        assertEquals(0.15, UniqueRules.VoidEdge.voidShare(0, false), 1e-9);
        assertTrue(UniqueRules.VoidEdge.voidShare(20, false) > UniqueRules.VoidEdge.voidShare(0, false));
        assertEquals(0.60, UniqueRules.VoidEdge.voidShare(500, true), 1e-9);
        assertEquals(0.15, UniqueRules.VoidEdge.voidShare(-5, false), 1e-9, "negative armour counts as none");
        assertTrue(UniqueRules.VoidEdge.imbueTicks(true) > UniqueRules.VoidEdge.imbueTicks(false));
    }

    // ---------------------------------------------------------------- Ultimates
    @Test
    void ultimateTablesAreSane() {
        assertTrue(UltimateRules.Basilisk.reduction(true) > UltimateRules.Basilisk.reduction(false));
        assertTrue(UltimateRules.Basilisk.reduction(true) < 0.6);
        assertTrue(UltimateRules.Basilisk.stoneTicks(true) < UltimateRules.Basilisk.stoneTicks(false), "players are held for less");
        assertEquals(1.0, UltimateRules.Beelzebuth.hungerBonus(0));
        assertEquals(1.4, UltimateRules.Beelzebuth.hungerBonus(10), 1e-9);
        assertEquals(1.4, UltimateRules.Beelzebuth.hungerBonus(99), 1e-9, "capped");
        assertTrue(UltimateRules.Beelzebuth.stacksForKill(true) > UltimateRules.Beelzebuth.stacksForKill(false));
        assertTrue(UltimateRules.Beelzebuth.killHeal(true) > UltimateRules.Beelzebuth.killHeal(false));
        assertEquals(0, UltimateRules.Hephaestus.reforgePoints(-3));
        assertEquals(40, UltimateRules.Hephaestus.reforgePoints(40));
        assertTrue(UltimateRules.Hephaestus.tempered(5, 6) && !UltimateRules.Hephaestus.tempered(6, 6));
        assertTrue(UltimateRules.Rapheal.predictChance(true, true) > UltimateRules.Rapheal.predictChance(false, false));
        assertTrue(UltimateRules.Rapheal.predictChance(true, true) < 0.6, "never close to dodging everything");
        assertTrue(UltimateRules.Rapheal.predicts(0.1, 0.2, 100, 0));
        assertFalse(UltimateRules.Rapheal.predicts(0.1, 0.2, 100, 95), "at most one per second");
        assertFalse(UltimateRules.Rapheal.predicts(0.5, 0.2, 100, 0));
        assertTrue(UltimateRules.VoidSovereign.domainRadius(true) > UltimateRules.VoidSovereign.domainRadius(false));
        assertEquals(20.0, UltimateRules.VoidSovereign.collapseDamage(10, false), "the floor");
        assertEquals(240.0, UltimateRules.VoidSovereign.collapseDamage(2000, false), 1e-9);
        assertEquals(48.0, UltimateRules.VoidSovereign.collapseDamage(2000, true), 1e-9, "a boss loses a fifth of it");
        assertTrue(UltimateRules.VoidSovereign.inside(1, 1, 1, 2));
        assertFalse(UltimateRules.VoidSovereign.inside(2, 2, 2, 2));
    }

    @Test
    void ouroborosNeverRewindsToNearDeath() {
        assertEquals(10.0, UltimateRules.Ouroboros.restoredHealth(2.0, 20.0), 1e-9, "at least half");
        assertEquals(15.0, UltimateRules.Ouroboros.restoredHealth(15.0, 20.0), 1e-9);
        assertTrue(UltimateRules.Ouroboros.rewindTicks(true) > UltimateRules.Ouroboros.rewindTicks(false));
        assertTrue(UltimateRules.Ouroboros.cycleMagiculeShare(true) > UltimateRules.Ouroboros.cycleMagiculeShare(false));
    }

    @Test
    void snapshotRingKeepsTheNewestAndAnswersByAge() {
        SnapshotRing ring = new SnapshotRing(4);
        assertNull(ring.at(100, 50), "empty ring");
        assertNull(ring.newest());
        for (int i = 0; i < 6; i++) assertTrue(ring.add(new SnapshotRing.Sample(i * 20L, 20 - i, i, 64, 0, "overworld"), 20));
        assertEquals(4, ring.size(), "the ring never grows past its capacity");
        assertFalse(ring.add(new SnapshotRing.Sample(101, 1, 0, 0, 0, "x"), 20), "a sample too soon after the newest is ignored");
        assertEquals(100L, ring.newest().tick());
        assertEquals(40L, ring.at(100, 60).tick(), "newest sample that is at least 60 ticks old");
        assertEquals(40L, ring.at(100, 1000).tick(), "if none is old enough, the oldest remembered");
        assertEquals(100L, ring.at(100, 0).tick());
        ring.clear();
        assertEquals(0, ring.size());
        assertNull(ring.newest());
    }
}
