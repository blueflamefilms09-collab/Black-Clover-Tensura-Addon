package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Mantle of the Scavenger: the Pheromone Mark and Prey's Weakness (docs/living_dungeon.md section 7). */
class PheromoneTest {
    @Test
    void theMarkLastsThirtySecondsAndRunsOut() {
        long now = 4000;
        long until = Pheromone.until(now);
        assertEquals(now + 600, until);
        assertTrue(Pheromone.active(now, until));
        assertTrue(Pheromone.active(now + 599, until));
        assertFalse(Pheromone.active(now + 600, until));
        assertEquals(30, Pheromone.secondsLeft(now, until));
        assertEquals(1, Pheromone.secondsLeft(now + 599, until));
        assertEquals(0, Pheromone.secondsLeft(now + 600, until));
    }

    @Test
    void aStampFromAClockThatRanBackIsNotAMark() {
        assertFalse(Pheromone.active(100, 100 + 10_000));
        assertFalse(Pheromone.active(100, 0));
    }

    @Test
    void onlyPredatorsOfTierTwoAndUpComeForTheMarked() {
        assertFalse(Pheromone.aggroes(0));
        assertFalse(Pheromone.aggroes(1));
        assertTrue(Pheromone.aggroes(2));
        assertTrue(Pheromone.aggroes(3));
        assertEquals(48, Pheromone.RADIUS);
        assertTrue(Pheromone.MAX_PER_SWEEP > 0 && Pheromone.MAX_PER_SWEEP <= 64, "a sweep is bounded");
    }

    @Test
    void apexPredatorsIgnoreTheWearerUntilMarked() {
        assertFalse(MantleRules.ignores(0, false), "not of the roster");
        assertFalse(MantleRules.ignores(1, false));
        assertFalse(MantleRules.ignores(2, false), "tier 2 still hunts the Scavenger");
        assertTrue(MantleRules.ignores(3, false));
        assertFalse(MantleRules.ignores(3, true), "the mark switches the ignoring off");
    }

    @Test
    void preysWeaknessTurnsPhysicalBlowsAsideFromTierTwoUp() {
        assertFalse(MantleRules.turnsAside(true, 0));
        assertFalse(MantleRules.turnsAside(true, 1));
        assertTrue(MantleRules.turnsAside(true, 2));
        assertTrue(MantleRules.turnsAside(true, 3));
        assertFalse(MantleRules.turnsAside(false, 3), "spells still work");
    }
}
