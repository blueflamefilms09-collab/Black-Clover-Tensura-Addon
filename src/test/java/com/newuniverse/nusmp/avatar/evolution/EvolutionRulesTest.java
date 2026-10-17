package com.newuniverse.nusmp.avatar.evolution;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The evolution numbers: the EP a race costs, when the check is due, the chain of races, the EP a kill adds, the festival clock. */
class EvolutionRulesTest {
    @Test
    void theThresholdIsAShareOfTheTargetRacesBaseEp() {
        assertEquals(1440.0, EvolutionRules.threshold(2400, 0.6), 1e-9, "elf to high elf at the default share");
        assertEquals(2400.0, EvolutionRules.threshold(2400, 1.0), 1e-9);
        assertEquals(0.0, EvolutionRules.threshold(-5, 0.6), 1e-9);
        assertEquals(EvolutionRules.threshold(1000, 0.05), EvolutionRules.threshold(1000, 0.0), 1e-9, "the share is kept above 5%");
        assertEquals(EvolutionRules.threshold(1000, 5.0), EvolutionRules.threshold(1000, 50.0), 1e-9, "and below 500%");
        assertEquals(EvolutionRules.threshold(1000, EvolutionRules.DEFAULT_FACTOR), EvolutionRules.threshold(1000, Double.NaN), 1e-9);
    }

    @Test
    void theEvolutionIsDueOnlyWithEnoughEpIdleAndRested() {
        assertTrue(EvolutionRules.due(1500, 1440, false, 10_000, 0, 6000));
        assertFalse(EvolutionRules.due(1439, 1440, false, 10_000, 0, 6000), "not enough EP");
        assertFalse(EvolutionRules.due(1500, 1440, true, 10_000, 0, 6000), "a festival is running");
        assertFalse(EvolutionRules.due(1500, 1440, false, 10_000, 8_000, 6000), "evolved two thousand ticks ago");
        assertTrue(EvolutionRules.due(1500, 1440, false, 14_000, 8_000, 6000), "and rested after six thousand");
        assertFalse(EvolutionRules.due(1500, 0, false, 10_000, 0, 6000), "a race that costs nothing is not an evolution");
    }

    @Test
    void theChainStopsAtTheEndAtARepeatAndAtTheLimit() {
        Map<String, String> next = Map.of("goblin", "hobgoblin", "hobgoblin", "hobgoblin_saint", "ogre", "kijin", "kijin", "ogre", "a", "b", "b", "a", "x", "y", "y", "z", "z", "y");
        assertEquals(List.of("hobgoblin", "hobgoblin_saint"), EvolutionRules.chain(next::get, "goblin", 5));
        assertEquals(List.of("hobgoblin"), EvolutionRules.chain(next::get, "goblin", 1));
        assertEquals(List.of("kijin"), EvolutionRules.chain(next::get, "ogre", 5), "a loop back to the start ends the chain");
        assertEquals(List.of("b"), EvolutionRules.chain(next::get, "a", 5), "so does a loop between two races");
        assertEquals(List.of("y", "z"), EvolutionRules.chain(next::get, "x", 9), "and a loop that does not include the start");
        assertEquals(List.of(), EvolutionRules.chain(next::get, "human", 5));
        assertEquals(List.of(), EvolutionRules.chain(next::get, null, 5));
        assertEquals(List.of(), EvolutionRules.chain(next::get, "goblin", 0));
    }

    @Test
    void aKillAddsASmallShareAndNeverMoreThanTwoPercent() {
        assertEquals(0.0, EvolutionRules.killGain(5, 1000, 0.04), 1e-9, "a rabbit is worth nothing");
        assertEquals(4.0, EvolutionRules.killGain(100, 1000, 0.04), 1e-9);
        assertEquals(20.0, EvolutionRules.killGain(100_000, 1000, 0.04), 1e-9, "capped at two percent of the Avatar's EP");
        assertEquals(0.0, EvolutionRules.killGain(100, 1000, 0.0), 1e-9);
        assertEquals(0.0, EvolutionRules.killGain(-1, 1000, 0.04), 1e-9);
        assertEquals(0.0, EvolutionRules.killGain(100, 0, 0.04), 1e-9);
    }

    @Test
    void growthIsSlowEnoughToNeedManyKills() {
        double ep = 1000;
        int kills = 0;
        while (ep < 1440 && kills < 100_000) { ep += EvolutionRules.killGain(ep, ep, 0.04); kills++; }
        assertTrue(kills >= 15, "from 1000 to 1440 EP took " + kills + " equal-sized kills");
        assertTrue(kills < 100_000);
    }

    @Test
    void theFestivalClockCountsDownAndEndsOnce() {
        EvolutionRules.Step s = EvolutionRules.festivalTick(3);
        assertEquals(2, s.left());
        assertFalse(s.ended());
        s = EvolutionRules.festivalTick(s.left());
        s = EvolutionRules.festivalTick(s.left());
        assertEquals(0, s.left());
        assertTrue(s.ended());
        assertFalse(EvolutionRules.festivalTick(0).ended(), "a festival that is not running does not end again");
        assertEquals(0, EvolutionRules.festivalTick(-4).left());
    }

    @Test
    void anEvolutionNeverLeavesTheAvatarWeakerThanItWas() {
        assertEquals(2.0, EvolutionRules.keepFactor(2000, 1000), 1e-9);
        assertEquals(1.0, EvolutionRules.keepFactor(1000, 2000), 1e-9);
        assertEquals(1.0, EvolutionRules.keepFactor(1000, 1000), 1e-9);
        assertEquals(1.0, EvolutionRules.keepFactor(0, 1000), 1e-9);
        assertEquals(1.0, EvolutionRules.keepFactor(1000, 0), 1e-9);
        assertEquals(1.0, EvolutionRules.keepFactor(Double.NaN, 1000), 1e-9);
    }
}
