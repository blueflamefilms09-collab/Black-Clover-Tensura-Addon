package com.newuniverse.nusmp.dungeon.wild;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Living Dungeon's wildlife rules (docs/living_dungeon.md section 1): hunger, who hunts whom, sleep, carcasses, births. */
class WildRulesTest {
    @Test
    void hungerScaleIsTheOneOfTheOldCreatures() {
        assertEquals(100, WildRules.HUNGER_MAX);
        assertEquals(40, WildRules.HUNGRY);
        assertEquals(80, WildRules.FULL);
        assertEquals(70, WildRules.START);
        assertTrue(WildRules.hungry(40));
        assertFalse(WildRules.hungry(41));
        assertTrue(WildRules.full(80));
        assertFalse(WildRules.full(79));
        assertEquals(0, WildRules.clampHunger(-5));
        assertEquals(100, WildRules.clampHunger(500));
    }

    @Test
    void carcassKindsByEntityId() {
        assertEquals(0, WildRules.carcassKind("tensura:slime"));
        assertEquals(0, WildRules.carcassKind("tensura:metal_slime"));
        assertEquals(0, WildRules.carcassKind("tensura:supermassive_slime"));
        assertEquals(1, WildRules.carcassKind("tensura:armorsaurus"));
        assertEquals(2, WildRules.carcassKind("tensura:basilisk"));
        assertEquals(3, WildRules.carcassKind("tensura:direwolf"));
        assertEquals(4, WildRules.carcassKind("tensura:goblin"));
        assertEquals(4, WildRules.carcassKind("iceandfire:fire_dragon"));
        assertEquals(4, WildRules.carcassKind("minecraft:zombie"));
        assertEquals(4, WildRules.carcassKind(null));
    }

    @Test
    void weightsFollowHealth() {
        assertEquals(1f, WildRules.massOf(0f));
        assertEquals(5f, WildRules.massOf(20f));
        assertEquals(60f, WildRules.massOf(900f));
        assertEquals(1f, WildRules.starveDamage(20f));
        assertEquals(8f, WildRules.starveDamage(400f), 1e-6f);
        assertEquals(28f, WildRules.carcassWeight(28f));
        assertEquals(0f, WildRules.carcassWeight(-3f));
    }

    @Test
    void onlyHuntersOfTier2UpTakeLowerTiersTheyCanBeat() {
        assertFalse(WildRules.mayPrey(1, false, 1, false, 100f, 1f), "a scavenger hunts nothing");
        assertTrue(WildRules.mayPrey(2, false, 1, false, 30f, 10f));
        assertFalse(WildRules.mayPrey(2, false, 2, false, 30f, 10f), "not its own tier");
        assertFalse(WildRules.mayPrey(2, false, 3, false, 30f, 10f), "not a higher tier");
        assertTrue(WildRules.mayPrey(3, false, 2, false, 60f, 40f));
        assertFalse(WildRules.mayPrey(3, false, 2, false, 60f, 150f), "never a fight it cannot win");
        assertFalse(WildRules.mayPrey(3, false, 0, false, 60f, 1f), "not a member of the roster");
        assertFalse(WildRules.mayPrey(3, true, 3, true, 500f, 10f), "nothing hunts a dragon");
        assertTrue(WildRules.mayPrey(3, true, 3, false, 500f, 60f), "a dragon takes an ordinary apex predator");
        assertFalse(WildRules.mayPrey(3, false, 3, false, 500f, 60f), "an ordinary apex predator does not");
    }

    @Test
    void sleepAndWake() {
        assertTrue(WildRules.mayFallAsleep(true, false, 90, false, false, true, false, false));
        assertFalse(WildRules.mayFallAsleep(false, false, 90, false, false, true, false, false), "only apex predators rest");
        assertFalse(WildRules.mayFallAsleep(true, true, 90, false, false, true, false, false), "a dragon never goes NoAI");
        assertFalse(WildRules.mayFallAsleep(true, false, 79, false, false, true, false, false), "not full");
        assertFalse(WildRules.mayFallAsleep(true, false, 90, true, false, true, false, false), "has a target");
        assertFalse(WildRules.mayFallAsleep(true, false, 90, false, true, true, false, false), "busy");
        assertFalse(WildRules.mayFallAsleep(true, false, 90, false, false, false, false, false), "in the air");
        assertFalse(WildRules.mayFallAsleep(true, false, 90, false, false, true, true, false), "a player is near");
        assertFalse(WildRules.mayFallAsleep(true, false, 90, false, false, true, false, true), "just woken");
        assertTrue(WildRules.mustWake(40, false, false), "hungry again");
        assertTrue(WildRules.mustWake(90, true, false), "hurt");
        assertTrue(WildRules.mustWake(90, false, true), "a player in the den");
        assertFalse(WildRules.mustWake(90, false, false));
        assertTrue(WildRules.hurtSince(30f, 29f));
        assertFalse(WildRules.hurtSince(30f, 30f));
        assertFalse(WildRules.justWoken(100L, Long.MIN_VALUE), "never woken");
        assertTrue(WildRules.justWoken(100L, 50L));
        assertFalse(WildRules.justWoken(50L + WildRules.WAKE_SPAN, 50L));
    }

    @Test
    void sleepCannotFlipBackAndForth() {
        for (int h = 0; h <= 100; h++) {
            boolean falls = WildRules.mayFallAsleep(true, false, h, false, false, true, false, false);
            boolean wakes = WildRules.mustWake(h, false, false);
            assertFalse(falls && wakes, "at hunger " + h + " it would fall asleep and wake in the same breath");
        }
    }

    @Test
    void birthGatesAreThoseOf1180() {
        assertTrue(WildRules.slimeBirth(150f, 0, 0.49f));
        assertFalse(WildRules.slimeBirth(149f, 0, 0.1f), "air not dense enough");
        assertFalse(WildRules.slimeBirth(150f, 3, 0.1f), "magicule 150 feeds 3");
        assertTrue(WildRules.slimeBirth(400f, 5, 0.1f));
        assertFalse(WildRules.slimeBirth(1000f, 10, 0.1f), "never more than 10");
        assertFalse(WildRules.slimeBirth(400f, 0, 0.5f));
        assertTrue(WildRules.midBirth(4, 2, 50f, 0.14f));
        assertFalse(WildRules.midBirth(3, 0, 100f, 0.0f));
        assertFalse(WildRules.midBirth(4, 3, 100f, 0.0f));
        assertFalse(WildRules.midBirth(4, 0, 49f, 0.0f));
        assertFalse(WildRules.midBirth(4, 0, 100f, 0.15f));
        assertTrue(WildRules.apexBirth(2, 1, 50f, 0.07f));
        assertFalse(WildRules.apexBirth(1, 0, 100f, 0.0f));
        assertFalse(WildRules.apexBirth(2, 2, 100f, 0.0f));
        assertFalse(WildRules.apexBirth(2, 0, 49f, 0.0f));
        assertFalse(WildRules.apexBirth(2, 0, 100f, 0.08f));
    }

    @Test
    void dragonsAreBornRarelyAndOnlyInThickLife() {
        assertTrue(WildRules.legendBirth(100f, 1, 50f, 0.003f));
        assertFalse(WildRules.legendBirth(99f, 1, 100f, 0.0f), "biomass not high");
        assertFalse(WildRules.legendBirth(300f, 0, 100f, 0.0f), "no apex predator lives here yet");
        assertFalse(WildRules.legendBirth(300f, 2, 49f, 0.0f), "thin air");
        assertFalse(WildRules.legendBirth(300f, 2, 100f, 0.004f), "very rare");
        assertTrue(WildRules.LEGEND_CHANCE < 0.08f, "rarer than the ordinary apex predators");
    }

    @Test
    void grazing() {
        assertTrue(WildRules.grazeGain(true) > WildRules.grazeGain(false));
        assertTrue(WildRules.grazeCost(true) > WildRules.grazeCost(false));
    }
}
