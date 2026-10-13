package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 0 (the sixteen remade items): the pure rules, no Minecraft. */
class Wave0RulesTest {
    private static Wave0Rules.Dice dice(long seed) {
        Random r = new Random(seed);
        return r::nextInt;
    }

    @Test
    void diceStayInRangeAndSumTheRolls() {
        Wave0Rules.Dice d = dice(1);
        for (int i = 0; i < 2000; i++) {
            int x = Wave0Rules.roll(d, 3, 6);
            assertTrue(x >= 3 && x <= 18, "3d6 is 3..18 but was " + x);
        }
        assertEquals(0, Wave0Rules.roll(d, 0, 6));
        assertEquals(5, Wave0Rules.roll(b -> 0, 5, 20), "five dice that all roll low");
    }

    @Test
    void aSaveIsAD20AgainstTheDcWithTheTargetsToughness() {
        assertEquals(0.25, Wave0Rules.saveChance(16, 0), 1e-9, "DC 16 with no bonus: 25%");
        assertEquals(0.05, Wave0Rules.saveChance(30, 0), 1e-9, "never below a natural 20");
        assertEquals(0.95, Wave0Rules.saveChance(5, 10), 1e-9, "never above 95%");
        assertEquals(0.55, Wave0Rules.saveChance(16, 6), 1e-9);
        double p = Wave0Rules.saveChance(16, 0);
        assertTrue(Wave0Rules.advantage(p) > p && Wave0Rules.disadvantage(p) < p);
        assertEquals(0.4375, Wave0Rules.advantage(p), 1e-9);
        assertEquals(0.0625, Wave0Rules.disadvantage(p), 1e-9);
        // a sturdy boss saves far more often than a zombie
        int zombie = Wave0Rules.saveMod(0, 20, 0, false), boss = Wave0Rules.saveMod(0.8, 400, 6, true);
        assertEquals(0, zombie);
        assertTrue(boss > zombie + 8, "boss " + boss);
    }

    @Test
    void theSaveRollFollowsItsChance() {
        Wave0Rules.Dice d = dice(7);
        int pass = 0;
        for (int i = 0; i < 20000; i++) if (Wave0Rules.passes(d, 0.25)) pass++;
        assertEquals(0.25, pass / 20000.0, 0.02);
        assertFalse(Wave0Rules.passes(b -> 999, 0.0));
        assertTrue(Wave0Rules.passes(b -> 0, 1.0));
    }

    @Test
    void conditionsLastWholeRounds() {
        assertEquals(1, Wave0Rules.rounds(0));
        assertEquals(1, Wave0Rules.rounds(20));
        assertEquals(2, Wave0Rules.rounds(21));
        assertEquals(5, Wave0Rules.rounds(100));
    }

    @Test
    void aLongRestIsOneMinecraftDay() {
        long day0 = Wave0Rules.day(5000), day1 = Wave0Rules.day(24000 + 1);
        assertEquals(0, day0);
        assertEquals(1, day1);
        Wave0Rules.Uses u = Wave0Rules.takeUse(null, day0, 2);
        assertNotNull(u);
        u = Wave0Rules.takeUse(u, day0, 2);
        assertNotNull(u);
        assertEquals(2, u.used());
        assertNull(Wave0Rules.takeUse(u, day0, 2), "two a day, no third");
        assertEquals(0, Wave0Rules.usesLeft(u, day0, 2));
        assertEquals(2, Wave0Rules.usesLeft(u, day1, 2), "dawn gives them back");
        Wave0Rules.Uses next = Wave0Rules.takeUse(u, day1, 2);
        assertEquals(1, next.used());
        assertEquals(day1, next.day());
        assertNotNull(Wave0Rules.takeUse(null, day0, 1));
        assertNull(Wave0Rules.takeUse(new Wave0Rules.Uses(day0, 1), day0, 1));
    }

    @Test
    void thundercleaveHoldsTwoChargesAndTheDawnAndTheStormRefillThem() {
        assertEquals(2, Wave0Rules.THUNDER_MAX);
        assertEquals(2, Wave0Rules.thunderAtDawn(2, 5, 5));
        assertEquals(0, Wave0Rules.thunderAtDawn(0, 5, 5), "the same day: still spent");
        assertEquals(2, Wave0Rules.thunderAtDawn(0, 5, 6), "a new day: full");
        assertEquals(2, Wave0Rules.thunderAtDawn(7, 5, 5), "never above the maximum");
        assertEquals(1, Wave0Rules.thunderStorm(0, Wave0Rules.STORM_REGEN_TICKS));
        assertEquals(0, Wave0Rules.thunderStorm(0, Wave0Rules.STORM_REGEN_TICKS - 1));
        assertEquals(2, Wave0Rules.thunderStorm(2, 10000), "full is full");
    }

    @Test
    void theLayOnHandsPoolIsTheItemsOwnPool() {
        assertEquals(Wave0Rules.LAY_MAX, Wave0Rules.layPool(null, 0, 0), "a blade never used is full");
        assertEquals(10, Wave0Rules.layPool(10, 100, 100));
        assertEquals(12, Wave0Rules.layPool(10, 100, 100 + 2 * Wave0Rules.LAY_REFILL_TICKS), "a point every 24 ticks");
        assertEquals(Wave0Rules.LAY_MAX, Wave0Rules.layPool(39, 0, 100000));
        assertEquals(0, Wave0Rules.layPool(-5, 0, 0));
        assertEquals(20f, Wave0Rules.layHeal(10), 1e-6);
    }

    @Test
    void theKatanaDrinksBloodButNeverTheWielder() {
        Wave0Rules.Dice d = dice(3);
        for (int i = 0; i < 500; i++) {
            int c = Wave0Rules.sipCost(d);
            assertTrue(c >= 1 && c <= 4, "1d4");
        }
        assertTrue(Wave0Rules.canSip(20f, 4));
        assertTrue(Wave0Rules.canSip(8f, 4));
        assertFalse(Wave0Rules.canSip(7f, 4), "a sip never leaves less than two hearts");
        assertEquals(0, Wave0Rules.flares(2));
        assertEquals(1, Wave0Rules.flares(3));
        assertEquals(1, Wave0Rules.flares(5));
        assertEquals(2, Wave0Rules.flares(6));
        assertEquals(2, Wave0Rules.flares(10), "two a hit at most");
        assertTrue(Wave0Rules.overflowing(10));
        assertFalse(Wave0Rules.overflowing(9));
    }

    @Test
    void damageTypesAreSortedByElement() {
        assertEquals(Wave0Rules.Element.FIRE, Wave0Rules.elementOf("minecraft:in_fire"));
        assertEquals(Wave0Rules.Element.FIRE, Wave0Rules.elementOf("on_fire"));
        assertEquals(Wave0Rules.Element.FIRE, Wave0Rules.elementOf("lava"));
        assertEquals(Wave0Rules.Element.FIRE, Wave0Rules.elementOf("unattributed_fireball"));
        assertEquals(Wave0Rules.Element.FIRE, Wave0Rules.elementOf("tensura:fire_elemental"));
        assertEquals(Wave0Rules.Element.COLD, Wave0Rules.elementOf("freeze"));
        assertEquals(Wave0Rules.Element.COLD, Wave0Rules.elementOf("tensura:ice_elemental"));
        assertEquals(Wave0Rules.Element.LIGHTNING, Wave0Rules.elementOf("lightning_bolt"));
        assertEquals(Wave0Rules.Element.LIGHTNING, Wave0Rules.elementOf("tensura:lightning_elemental"));
        assertEquals(Wave0Rules.Element.NONE, Wave0Rules.elementOf("player_attack"));
        assertEquals(Wave0Rules.Element.NONE, Wave0Rules.elementOf("magic"));
        assertEquals(Wave0Rules.Element.NONE, Wave0Rules.elementOf("fall"));
        assertEquals(Wave0Rules.Element.NONE, Wave0Rules.elementOf(null));
        assertEquals("minecraft:freeze", Wave0Rules.elementDamageType(Wave0Rules.Element.COLD));
        assertEquals("minecraft:magic", Wave0Rules.elementDamageType(Wave0Rules.Element.NONE));
        assertEquals("tensura:cold_resistance", Wave0Rules.elementResistSkill(Wave0Rules.Element.COLD));
        assertEquals("tensura:flame_attack_resistance", Wave0Rules.elementResistSkill(Wave0Rules.Element.FIRE));
        assertEquals("tensura:electricity_resistance", Wave0Rules.elementResistSkill(Wave0Rules.Element.LIGHTNING));
        assertEquals("", Wave0Rules.elementResistSkill(Wave0Rules.Element.NONE));
    }

    @Test
    void constructsAreGolemsAndTheirKin() {
        assertTrue(Wave0Rules.isConstructPath("iron_golem"));
        assertTrue(Wave0Rules.isConstructPath("elemental_colossus"));
        assertTrue(Wave0Rules.isConstructPath("armorsaurus"));
        assertFalse(Wave0Rules.isConstructPath("zombie"));
        assertFalse(Wave0Rules.isConstructPath("goblin"));
        assertFalse(Wave0Rules.isConstructPath(null));
    }

    @Test
    void aCreatureIsSurprisedWhenAsleepFromBehindOrHuntingSomeoneElse() {
        assertTrue(Wave0Rules.surprised(true, false, true, true, true));
        assertTrue(Wave0Rules.surprised(false, true, false, false, false));
        assertTrue(Wave0Rules.surprised(false, false, true, false, false), "a mob that has no target");
        assertTrue(Wave0Rules.surprised(false, false, true, true, false), "a mob hunting someone else");
        assertFalse(Wave0Rules.surprised(false, false, true, true, true), "a mob that is hunting you is ready");
        assertFalse(Wave0Rules.surprised(false, false, false, false, false), "a player facing you is not surprised");
    }

    @Test
    void theWhirlwindHoldsTwoMediumCreaturesOrOneLargeOne() {
        assertEquals(1, Wave0Rules.sizeWeight(0.6, 1.8));
        assertEquals(2, Wave0Rules.sizeWeight(1.9, 1.6));
        assertEquals(2, Wave0Rules.sizeWeight(0.9, 3.0));
        assertTrue(Wave0Rules.fits(0, 2));
        assertTrue(Wave0Rules.fits(1, 1));
        assertFalse(Wave0Rules.fits(1, 2));
        assertFalse(Wave0Rules.fits(2, 1));
    }

    @Test
    void theWhirlwindMovesAtMostNineBlocksAndNeverOvershoots() {
        double[] g = Wave0Rules.clampMove(0, 0, 100, 0);
        assertEquals(Wave0Rules.WHIRL_MAX_MOVE, g[0], 1e-9);
        assertEquals(0, g[1], 1e-9);
        double[] near = Wave0Rules.clampMove(1, 1, 3, 1);
        assertEquals(3, near[0], 1e-9);
        double x = 0, z = 0;
        int steps = 0;
        while (steps < 1000) {
            double[] s = Wave0Rules.stepToward(x, z, 6, 8, 0.8);
            x = s[0];
            z = s[1];
            steps++;
            if (s[2] == 0) break;
        }
        assertEquals(6, x, 1e-9);
        assertEquals(8, z, 1e-9);
        assertEquals(13, steps, "ten blocks at 0.8 a step");
    }
}
