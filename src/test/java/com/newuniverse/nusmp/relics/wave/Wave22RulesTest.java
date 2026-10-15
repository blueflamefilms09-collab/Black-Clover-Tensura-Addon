package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, wave 22: the pure rules of the twelve staves and wands (charges at dawn, the sky's double and half, stored spells, blood, forms). */
class Wave22RulesTest {
    private static final IntUnaryOperator MAX = n -> n, MIN = n -> 1;

    @Test
    void aNewWandIsFullAndSpendingNeverGoesBelowZero() {
        Wave22Rules.Store s = Wave22Rules.fresh(Wave22Rules.Kind.SERPENT, 5);
        assertEquals(10, s.charges());
        assertTrue(Wave22Rules.canSpend(s, 3));
        Wave22Rules.Store a = Wave22Rules.spend(s, 3);
        assertEquals(7, a.charges());
        assertFalse(Wave22Rules.canSpend(a, 8));
        assertEquals(7, Wave22Rules.spend(a, 8).charges(), "an unaffordable spend changes nothing");
        assertFalse(Wave22Rules.canSpend(Wave22Rules.lock(a), 1), "a dormant wand spends nothing");
    }

    @Test
    void theSerpentStaffGainsOneD6PlusFourAtDawnAndCaps() {
        Wave22Rules.Store s = new Wave22Rules.Store(0, 10, false);
        assertEquals(5, Wave22Rules.dawn(Wave22Rules.Kind.SERPENT, s, 11, MIN).store().charges());
        assertEquals(10, Wave22Rules.dawn(Wave22Rules.Kind.SERPENT, s, 11, MAX).store().charges());
        assertEquals(10, Wave22Rules.dawn(Wave22Rules.Kind.SERPENT, new Wave22Rules.Store(8, 10, false), 11, MAX).store().charges(), "capped at ten");
        Wave22Rules.Dawn same = Wave22Rules.dawn(Wave22Rules.Kind.SERPENT, s, 10, MAX);
        assertEquals(0, same.store().charges(), "same day: nothing");
    }

    @Test
    void theOtherWandsRefillByTheirOwnRule() {
        assertEquals(3, Wave22Rules.dawn(Wave22Rules.Kind.EMBER, new Wave22Rules.Store(0, 1, false), 2, MIN).store().charges());
        assertEquals(1, Wave22Rules.dawn(Wave22Rules.Kind.APPRENTICE, new Wave22Rules.Store(0, 1, false), 2, MIN).store().charges());
        assertEquals(4, Wave22Rules.dawn(Wave22Rules.Kind.APPRENTICE, new Wave22Rules.Store(0, 1, false), 2, MAX).store().charges());
        assertEquals(2, Wave22Rules.dawn(Wave22Rules.Kind.SPIDER, new Wave22Rules.Store(0, 1, false), 2, MIN).store().charges());
        assertEquals(7, Wave22Rules.dawn(Wave22Rules.Kind.SPIDER, new Wave22Rules.Store(0, 1, false), 2, MAX).store().charges());
    }

    @Test
    void severalDawnsAddUpButOnlySevenAreCounted() {
        Wave22Rules.Dawn d = Wave22Rules.dawn(Wave22Rules.Kind.APPRENTICE, new Wave22Rules.Store(0, 0, false), 3, MIN);
        assertEquals(3, d.store().charges());
        assertEquals(5, Wave22Rules.dawn(Wave22Rules.Kind.APPRENTICE, new Wave22Rules.Store(0, 0, false), 1000, MAX).store().charges());
    }

    @Test
    void theWispWandRefillsOnlyWhenEmptyAndAZeroMakesItSulk() {
        assertEquals(3, Wave22Rules.dawn(Wave22Rules.Kind.WISP, new Wave22Rules.Store(3, 1, false), 2, MAX).store().charges(), "not empty: no refill");
        Wave22Rules.Dawn zero = Wave22Rules.dawn(Wave22Rules.Kind.WISP, new Wave22Rules.Store(0, 1, false), 2, MIN);   // d8 = 1 -> d8-1 = 0
        assertTrue(zero.sulked());
        assertEquals(0, zero.store().charges());
        Wave22Rules.Dawn full = Wave22Rules.dawn(Wave22Rules.Kind.WISP, new Wave22Rules.Store(0, 1, false), 2, MAX);   // d8 = 8 -> 7
        assertFalse(full.sulked());
        assertEquals(7, full.store().charges());
    }

    @Test
    void aDormantWandWakesAtDawn() {
        Wave22Rules.Store locked = Wave22Rules.lock(new Wave22Rules.Store(0, 4, false));
        assertTrue(locked.locked());
        assertFalse(Wave22Rules.dawn(Wave22Rules.Kind.SPIDER, locked, 5, MIN).store().locked());
        assertTrue(Wave22Rules.lastChargeLocks(1));
        assertFalse(Wave22Rules.lastChargeLocks(2));
        assertFalse(Wave22Rules.lastChargeLocks(20));
    }

    @Test
    void elementsAreReadFromDamageTypeIds() {
        assertEquals(Wave22Rules.Element.FIRE, Wave22Rules.element("minecraft:in_fire"));
        assertEquals(Wave22Rules.Element.COLD, Wave22Rules.element("minecraft:freeze"));
        assertEquals(Wave22Rules.Element.LIGHTNING, Wave22Rules.element("minecraft:lightning_bolt"), "lightning is not light");
        assertEquals(Wave22Rules.Element.THUNDER, Wave22Rules.element("minecraft:sonic_boom"));
        assertEquals(Wave22Rules.Element.RADIANT, Wave22Rules.element("tensura:holy_damage"));
        assertEquals(Wave22Rules.Element.NECROTIC, Wave22Rules.element("minecraft:wither"));
        assertEquals(Wave22Rules.Element.FORCE, Wave22Rules.element("minecraft:magic"));
        assertEquals(Wave22Rules.Element.OTHER, Wave22Rules.element("minecraft:player_attack"));
        assertEquals(Wave22Rules.Element.OTHER, Wave22Rules.element(null));
    }

    @Test
    void theSkyIsDayNightOrTwilight() {
        assertEquals(Wave22Rules.Sky.DAY, Wave22Rules.sky(false, 1000));
        assertEquals(Wave22Rules.Sky.DAY, Wave22Rules.sky(false, 23500));
        assertEquals(Wave22Rules.Sky.NIGHT, Wave22Rules.sky(false, 15000));
        assertEquals(Wave22Rules.Sky.NIGHT, Wave22Rules.sky(false, 24000L * 5 + 18000));
        assertEquals(Wave22Rules.Sky.TWILIGHT, Wave22Rules.sky(true, 1000));
    }

    @Test
    void theMoonTableMatchesTheCard() {
        assertEquals(0.5, Wave22Rules.moonScale(Wave22Rules.Sky.DAY, Wave22Rules.Element.COLD));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.DAY, Wave22Rules.Element.FIRE));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.DAY, Wave22Rules.Element.RADIANT));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.DAY, Wave22Rules.Element.THUNDER));
        assertEquals(1.0, Wave22Rules.moonScale(Wave22Rules.Sky.DAY, Wave22Rules.Element.NECROTIC));
        assertEquals(0.5, Wave22Rules.moonScale(Wave22Rules.Sky.NIGHT, Wave22Rules.Element.FIRE));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.NIGHT, Wave22Rules.Element.COLD));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.NIGHT, Wave22Rules.Element.NECROTIC));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.NIGHT, Wave22Rules.Element.LIGHTNING));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.TWILIGHT, Wave22Rules.Element.COLD));
        assertEquals(2.0, Wave22Rules.moonScale(Wave22Rules.Sky.TWILIGHT, Wave22Rules.Element.RADIANT));
        assertEquals(0.5, Wave22Rules.moonScale(Wave22Rules.Sky.TWILIGHT, Wave22Rules.Element.FORCE));
        assertEquals(0.5, Wave22Rules.moonScale(Wave22Rules.Sky.TWILIGHT, Wave22Rules.Element.LIGHTNING));
        assertEquals(1.0, Wave22Rules.moonScale(Wave22Rules.Sky.TWILIGHT, Wave22Rules.Element.OTHER));
    }

    @Test
    void doubledDamageRoundsUpAndIsCappedAgainstPlayers() {
        assertEquals(10f, Wave22Rules.moonScaled(5f, 2.0));
        assertEquals(3f, Wave22Rules.moonScaled(5f, 0.5), "2.5 rounds up to 3");
        assertEquals(9f, Wave22Rules.capVsPlayer(30f, 20f), 0.001f, "45% of 20 health");
        assertEquals(4f, Wave22Rules.capVsPlayer(4f, 20f));
    }

    @Test
    void theLichWandStoresThreeDifferentSpellsPerDayAndKeepsThemStable() {
        for (long seed = 0; seed < 60; seed++) {
            int[] a = Wave22Rules.dailySpells(seed * 7919L, 12);
            assertEquals(3, a.length);
            Set<Integer> seen = new HashSet<>();
            for (int i : a) {
                assertTrue(i >= 0 && i < Wave22Rules.LICH_POOL);
                assertTrue(seen.add(i), "distinct");
            }
            assertEquals(a[0], Wave22Rules.dailySpells(seed * 7919L, 12)[0], "same day, same spells");
        }
        assertFalse(java.util.Arrays.equals(Wave22Rules.dailySpells(77, 1), Wave22Rules.dailySpells(77, 2)) && java.util.Arrays.equals(Wave22Rules.dailySpells(77, 2), Wave22Rules.dailySpells(77, 3)), "the days differ");
    }

    @Test
    void theSlotMaskWalksThroughTheThreeSpells() {
        assertEquals(0, Wave22Rules.nextSlot(0));
        assertEquals(1, Wave22Rules.nextSlot(1));
        assertEquals(2, Wave22Rules.nextSlot(3));
        assertEquals(-1, Wave22Rules.nextSlot(7));
        assertTrue(Wave22Rules.slotUsed(5, 2));
        assertFalse(Wave22Rules.slotUsed(5, 1));
    }

    @Test
    void bloodCostScalesWithLevelAndNeverKills() {
        assertEquals(2.5f, Wave22Rules.bloodCost(1, 20f, 20f, MAX), 0.0001f);
        assertEquals(0.25f, Wave22Rules.bloodCost(1, 20f, 20f, MIN), 0.0001f);
        assertEquals(12f, Wave22Rules.bloodCost(5, 20f, 20f, MAX), 0.0001f, "capped at 60% of maximum health");
        assertEquals(5.0f, Wave22Rules.bloodCost(5, 20f, 7f, MAX), 0.0001f, "never below 2 health");
        assertEquals(0f, Wave22Rules.bloodCost(5, 20f, 2f, MAX), 0.0001f);
    }

    @Test
    void theFlameReshapesHumanoidBeastBirdAndRound() {
        assertEquals(Wave22Rules.Form.BEAST, Wave22Rules.Form.HUMANOID.next());
        assertEquals(Wave22Rules.Form.BIRD, Wave22Rules.Form.BEAST.next());
        assertEquals(Wave22Rules.Form.HUMANOID, Wave22Rules.Form.BIRD.next());
        assertEquals(Wave22Rules.Form.BIRD, Wave22Rules.Form.of(-1));
        assertEquals(0f, Wave22Rules.Form.BIRD.damage(), "the hummingbird lights foes, it does not burn them");
        assertTrue(Wave22Rules.Form.BEAST.radius() > Wave22Rules.Form.HUMANOID.radius());
    }

    @Test
    void theApprenticeSchoolsAndWeeklyStudy() {
        assertEquals(8, Wave22Rules.SCHOOLS.length);
        assertEquals(8, Wave22Rules.SPELLS.length);
        for (String[] two : Wave22Rules.SPELLS) assertEquals(2, two.length);
        assertEquals(0, Wave22Rules.school(1));
        assertEquals(7, Wave22Rules.school(8));
        assertEquals(0, Wave22Rules.school(9), "wraps");
        assertTrue(Wave22Rules.weekReady(-1, 0));
        assertFalse(Wave22Rules.weekReady(3, 9));
        assertTrue(Wave22Rules.weekReady(3, 10));
    }

    @Test
    void wispsAreCappedAtTwo() {
        assertEquals(1, Wave22Rules.wispCount(0));
        assertEquals(2, Wave22Rules.wispCount(1));
        assertEquals(2, Wave22Rules.wispCount(2));
        assertEquals(2, Wave22Rules.wispCount(40));
        assertEquals(1, Wave22Rules.wispCount(-3));
    }

    @Test
    void theBroodsCostWhatTheCardSays() {
        assertEquals(1, Wave22Rules.SPIDER.cost());
        assertEquals(2, Wave22Rules.SWARM.cost());
        assertEquals(3, Wave22Rules.LARGE.cost());
        assertTrue(Wave22Rules.SWARM.count() > 1);
        assertTrue(Wave22Rules.SPIDER.cost() + Wave22Rules.SWARM.cost() + Wave22Rules.LARGE.cost() <= Wave22Rules.Kind.SPIDER.max());
        assertEquals(3, Wave22Rules.WISP_COST);
        assertTrue(Wave22Rules.WISP_COST <= Wave22Rules.Kind.WISP.max());
    }

    @Test
    void dreamsAreReadFromTheEffects() {
        assertEquals(Wave22Rules.Dream.NEUTRAL, Wave22Rules.dreamOf(0, 0));
        assertEquals(Wave22Rules.Dream.NIGHTMARE, Wave22Rules.dreamOf(2, 0));
        assertEquals(Wave22Rules.Dream.SWEET, Wave22Rules.dreamOf(0, 1));
        assertEquals(Wave22Rules.Dream.NIGHTMARE, Wave22Rules.dreamOf(3, 1));
        assertEquals(Wave22Rules.Dream.SWEET, Wave22Rules.dreamOf(1, 3));
        assertEquals(Wave22Rules.Dream.NEUTRAL, Wave22Rules.dreamOf(2, 2));
    }

    @Test
    void blindnessLastsHalfAgainAsLongAndIsCapped() {
        assertEquals(120, Wave22Rules.blindTicks(80));
        assertEquals(400, Wave22Rules.blindTicks(1000));
    }

    @Test
    void theCompassAndTheGate() {
        assertEquals("east", Wave22Rules.bearing(10, 0));
        assertEquals("south", Wave22Rules.bearing(0, 10));
        assertEquals("west", Wave22Rules.bearing(-10, 0));
        assertEquals("north", Wave22Rules.bearing(0, -10));
        assertEquals("north-east", Wave22Rules.bearing(5, -5));
        assertTrue(Wave22Rules.gateReady(-1, 0));
        assertFalse(Wave22Rules.gateReady(2, 8));
        assertTrue(Wave22Rules.gateReady(2, 9));
        assertEquals(3, Wave22Rules.gateDaysLeft(2, 6));
        assertEquals(0, Wave22Rules.gateDaysLeft(-1, 6));
    }

    @Test
    void theWindwheelStoresTheSpellLevelAndDischargesOneD6PerCharge() {
        assertEquals(0, Wave22Rules.windStored(0.004), "too small to be a spell");
        assertEquals(1, Wave22Rules.windStored(0.012));
        assertEquals(1, Wave22Rules.windStored(0.02));
        assertEquals(2, Wave22Rules.windStored(0.021));
        assertEquals(5, Wave22Rules.windStored(0.9), "level capped at 5");
        assertEquals(10, Wave22Rules.windTotal(8, 5), "at most ten stored");
        assertEquals(3, Wave22Rules.windTotal(-4, 3));
        assertEquals(10.5f, Wave22Rules.windDamage(3, false), 0.001f);
        assertEquals(5.25f, Wave22Rules.windDamage(3, true), 0.001f);
        assertEquals(35f, Wave22Rules.windDamage(10, false), 0.001f, "ten charges is the most");
        assertTrue(Wave22Rules.windAlive(100, 101));
        assertFalse(Wave22Rules.windAlive(100, 100));
    }
}
