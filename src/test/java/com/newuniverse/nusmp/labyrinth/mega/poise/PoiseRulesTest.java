package com.newuniverse.nusmp.labyrinth.mega.poise;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the poise bar, super armor, the stagger, the regeneration and the desperation phase are rules the party feels in every fight and nobody can play-test them here, so the pure parts are pinned:
 * the capacity grows with health and tier and is clamped, a blow always drains at least one point (an arrow less than a sword), super armor holds until the bar empties and not a blow longer,
 * the blow that empties it staggers and the bar cannot be broken again during the immune window, the bar refills only after its delay, desperation starts under 30 percent and ends only over its
 * exit share (no flicker), its cooldowns shrink but never to zero and its wind-up never under the readable minimum.
 */
public class PoiseRulesTest {
    private static final PoiseTuning T = PoiseTuning.DEFAULT;

    @Test
    public void capacityGrowsWithHealthAndTierAndIsClamped() {
        double a = PoiseMath.maxPoise(100, 1, false, T), b = PoiseMath.maxPoise(400, 1, false, T), c = PoiseMath.maxPoise(400, 5, false, T);
        assertTrue(b > a && c > b);
        assertTrue(PoiseMath.maxPoise(400, 3, true, T) > PoiseMath.maxPoise(400, 3, false, T), "a captain's bar is bigger");
        assertEquals(T.minMax(), PoiseMath.maxPoise(0, 1, false, T), 1e-9);
        assertEquals(T.maxMax(), PoiseMath.maxPoise(1.0e9, 5, true, T), 1e-9);
        assertEquals(T.minMax(), PoiseMath.maxPoise(Double.NaN, 1, false, T), 1e-9);
        assertEquals(T.minMax(), PoiseMath.maxPoise(Double.POSITIVE_INFINITY, 1, false, T), 1e-9, "a non-finite health never gives a huge bar");
    }

    @Test
    public void aBlowAlwaysCountsAndAnArrowCountsLess() {
        assertEquals(1.0, PoiseMath.poiseDamage(0.0, false, T), 1e-9);
        assertEquals(1.0, PoiseMath.poiseDamage(Double.NaN, false, T), 1e-9);
        assertTrue(PoiseMath.poiseDamage(20, true, T) < PoiseMath.poiseDamage(20, false, T));
        assertEquals(20.0, PoiseMath.poiseDamage(20, false, T), 1e-9, "poise damage is the damage dealt, never a share of anybody's health");
    }

    @Test
    public void armorTakesItsShareOffAndNothingWhenDown() {
        assertEquals(10.0 * (1.0 - T.armorReduction()), PoiseMath.damageThrough(10.0, true, T), 1e-9);
        assertEquals(10.0, PoiseMath.damageThrough(10.0, false, T), 1e-9);
        assertEquals(0.0, PoiseMath.damageThrough(-5.0, true, T), 1e-9);
    }

    @Test
    public void withoutArmorABlowIsOpenAndOnlyStartsTheDelay() {
        PoiseState s = new PoiseState(100);
        assertEquals(PoiseState.Hit.OPEN, s.hit(10, 500, T));
        assertEquals(100.0, s.poise(), 1e-9, "no armor: the bar is untouched");
        assertFalse(s.staggered(11));
    }

    @Test
    public void armorHoldsUntilTheBarBreaksThenStaggersThenCannotBeBrokenAgainAtOnce() {
        PoiseState s = new PoiseState(100);
        s.raiseArmor(0, 200);
        assertTrue(s.armored(5));
        assertEquals(PoiseState.Hit.ABSORBED, s.hit(5, 40, T));
        assertEquals(PoiseState.Hit.ABSORBED, s.hit(6, 40, T));
        assertEquals(20.0, s.poise(), 1e-9);
        assertEquals(PoiseState.Hit.BROKEN, s.hit(7, 40, T));
        assertEquals(0.0, s.poise(), 1e-9);
        assertEquals(1, s.breaks());
        assertFalse(s.armored(8), "the armor drops with the bar");
        assertTrue(s.staggered(8));
        assertTrue(s.staggered(7 + T.staggerTicks() - 1));
        assertFalse(s.staggered(7 + T.staggerTicks()));
        s.raiseArmor(8, 100);
        assertFalse(s.armored(9), "no armor can be raised during the stagger");
        long after = 7 + T.staggerTicks();
        s.tick(after, 1, T);
        assertEquals(T.recoverFrac() * 100.0, s.poise(), 1e-9, "the bar is back at its recover share when the stagger ends");
        s.raiseArmor(after, 100);
        assertEquals(PoiseState.Hit.ABSORBED, s.hit(after + 1, 1000, T), "inside the immune window a huge blow drains nothing");
        assertEquals(T.recoverFrac() * 100.0, s.poise(), 1e-9);
        long later = after + T.immuneTicks() + 1;
        s.raiseArmor(later, 100);
        assertEquals(PoiseState.Hit.BROKEN, s.hit(later + 1, 1000, T), "after the window it can break again");
        assertEquals(2, s.breaks());
    }

    @Test
    public void theBarRefillsOnlyAfterItsDelay() {
        PoiseState s = new PoiseState(100);
        s.raiseArmor(0, 100);
        s.hit(1, 30, T);
        s.tick(1 + T.regenDelay() - 1, 1, T);
        assertEquals(70.0, s.poise(), 1e-9, "inside the delay nothing comes back");
        s.tick(1 + T.regenDelay(), 20, T);
        assertEquals(70.0 + 100.0 * T.regenFrac(), s.poise(), 1e-9, "after it, one second brings back regenFrac of the bar");
        for (int i = 0; i < 100; i++) s.tick(1000 + i * 20L, 20, T);
        assertEquals(100.0, s.poise(), 1e-9, "never over the capacity");
    }

    @Test
    public void armorNeverShortensAndExpires() {
        PoiseState s = new PoiseState(50);
        s.raiseArmor(0, 100);
        s.raiseArmor(10, 20);
        assertTrue(s.armored(99));
        assertFalse(s.armored(100));
        s.dropArmor();
        assertFalse(s.armored(50));
    }

    @Test
    public void aSavedBarComesBackStandingAndWithoutArmor() {
        PoiseState s = new PoiseState(10);
        s.restore(80, 33, 2);
        assertEquals(80.0, s.max(), 1e-9);
        assertEquals(33.0, s.poise(), 1e-9);
        assertEquals(2, s.breaks());
        assertFalse(s.armored(0));
        assertFalse(s.staggered(0));
        s.restore(80, 9999, 0);
        assertEquals(80.0, s.poise(), 1e-9, "a corrupt saved fill is clamped");
    }

    @Test
    public void desperationStartsUnderThirtyPercentAndEndsOnlyOverItsExit() {
        assertFalse(Desperation.next(false, 0.30, T), "exactly 30 percent is not under it");
        assertTrue(Desperation.next(false, 0.299, T));
        assertTrue(Desperation.next(true, 0.40, T), "a healer's tick to 40 percent does not end it");
        assertFalse(Desperation.next(true, 0.46, T));
        assertFalse(Desperation.next(false, 0.0, T), "a dead bearer is not desperate");
        assertFalse(Desperation.next(true, Double.NaN, T));
    }

    @Test
    public void desperationQuickensCooldownsButNeverToZeroAndKeepsTheCueReadable() {
        assertEquals(100, Desperation.cooldown(100, false, T));
        assertTrue(Desperation.cooldown(100, true, T) < 100);
        assertEquals(71, Desperation.cooldown(100, true, T));
        assertEquals(1, Desperation.cooldown(1, true, T));
        assertEquals(0, Desperation.cooldown(0, true, T), "a passive's zero stays zero");
        assertTrue(Desperation.wind(20, true, T) < 20);
        assertTrue(Desperation.wind(20, true, T) >= Desperation.MIN_WIND);
        assertEquals(Desperation.MIN_WIND, Desperation.wind(10, true, T), "never under the readable minimum");
        assertEquals(5, Desperation.wind(5, true, T), "a wind-up already under the minimum is not lengthened");
        assertEquals(20, Desperation.wind(20, false, T));
        assertEquals(Desperation.wind(30, true, T), PoiseMath.wind(14, 30, true, T), "the wind-up is the longer of the ability's and the telegraph's, then shortened");
    }

    @Test
    public void theConfigKnobsAreClamped() {
        PoiseTuning t = T.with(5.0, 99, 5.0, -4);
        assertEquals(0.6, t.desperateBelow(), 1e-9);
        assertTrue(t.desperateExit() > t.desperateBelow());
        assertEquals(6, t.eliteTier());
        assertEquals(0.9, t.armorReduction(), 1e-9);
        assertEquals(10, t.staggerTicks());
        PoiseTuning u = T.with(Double.NaN, 0, Double.NaN, 9999);
        assertEquals(0.05, u.desperateBelow(), 1e-9);
        assertEquals(1, u.eliteTier());
        assertEquals(200, u.staggerTicks());
    }

    @Test
    public void whoCarriesABarIsOneDecision() {
        assertEquals(PoiseBearers.Kind.NONE, PoiseBearers.classify(false, false, false, false, true, true, 5, 4), "never outside the dungeon");
        assertEquals(PoiseBearers.Kind.NONE, PoiseBearers.classify(true, true, false, false, true, true, 5, 4), "never a boss");
        assertEquals(PoiseBearers.Kind.NONE, PoiseBearers.classify(true, false, true, false, true, true, 5, 4), "never a servant");
        assertEquals(PoiseBearers.Kind.NONE, PoiseBearers.classify(true, false, false, true, true, true, 5, 4), "never a minion");
        assertEquals(PoiseBearers.Kind.CAPTAIN, PoiseBearers.classify(true, false, false, false, true, true, 5, 4), "a captain that is also an elite is a captain");
        assertEquals(PoiseBearers.Kind.ELITE, PoiseBearers.classify(true, false, false, false, false, true, 0, 4));
        assertEquals(PoiseBearers.Kind.ELITE, PoiseBearers.classify(true, false, false, false, false, false, 4, 4));
        assertEquals(PoiseBearers.Kind.NONE, PoiseBearers.classify(true, false, false, false, false, false, 3, 4));
        assertEquals(PoiseBearers.Kind.NONE, PoiseBearers.classify(true, false, false, false, false, false, 0, 4), "a mob with no tier and no tag is plain");
    }

    @Test
    public void dungeonMonstersAreOneSideAndTheMasterSwitchBeatsAStaleQuarrelSwitch() {
        assertTrue(PassivityRule.blocks(true, true, true));
        // never touches a player, a servant or a pet (they are not dungeon monsters), and the master switch off keeps the old game
        assertFalse(PassivityRule.blocks(true, true, false));
        assertFalse(PassivityRule.blocks(true, false, true));
        assertFalse(PassivityRule.blocks(false, true, true));
        // the old quarrel systems (turf wars, rival goal) run only with the master switch off AND their own switch on: a config file written by an older version that still says true does not revive them
        assertFalse(PassivityRule.quarrelsRun(true, true));
        assertFalse(PassivityRule.quarrelsRun(true, false));
        assertTrue(PassivityRule.quarrelsRun(false, true));
        assertFalse(PassivityRule.quarrelsRun(false, false));
    }
}
