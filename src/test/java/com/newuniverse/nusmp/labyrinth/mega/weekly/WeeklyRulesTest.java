package com.newuniverse.nusmp.labyrinth.mega.weekly;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyRulesTest {
    @Test
    void floorsEarnOneTwoThreeTwists() {
        assertEquals(0, WeeklyRules.countFor(29, 30, 70, 100));
        assertEquals(1, WeeklyRules.countFor(30, 30, 70, 100));
        assertEquals(2, WeeklyRules.countFor(70, 30, 70, 100));
        assertEquals(3, WeeklyRules.countFor(100, 30, 70, 100));
        assertEquals(3, WeeklyRules.countFor(5000, 30, 70, 100));
    }

    @Test
    void thresholdsOutOfOrderAreReadSorted() {
        assertEquals(WeeklyRules.countFor(75, 30, 70, 100), WeeklyRules.countFor(75, 100, 30, 70));
        assertEquals(WeeklyRules.countFor(10, 30, 70, 100), WeeklyRules.countFor(10, 70, 100, 30));
    }

    @Test
    void aWeekIsTheSameEverywhereAndHasNoRepeats() {
        for (long w = 0; w < 200; w++) {
            List<WeeklyRules.Mod> a = WeeklyRules.pick(w, 3), b = WeeklyRules.pick(w, 3);
            assertEquals(a, b);
            assertEquals(3, new HashSet<>(a).size(), "no twist twice in a week");
            assertEquals(a.get(0), WeeklyRules.pick(w, 1).get(0), "the first pick is the same whatever the floor");
        }
    }

    @Test
    void theWeeksVary() {
        Set<WeeklyRules.Mod> firsts = new HashSet<>();
        for (long w = 0; w < 100; w++) firsts.add(WeeklyRules.pick(w, 1).get(0));
        assertTrue(firsts.size() >= 4, "the first twist moves through the list: " + firsts);
    }

    @Test
    void countsAreClamped() {
        assertTrue(WeeklyRules.pick(1, -4).isEmpty());
        assertEquals(WeeklyRules.Mod.values().length, WeeklyRules.pick(1, 99).size());
    }

    @Test
    void weekNumberChangesEverySevenDaysAndHandlesNegativeDays() {
        assertEquals(WeeklyRules.weekOf(7), WeeklyRules.weekOf(13));
        assertEquals(WeeklyRules.weekOf(6) + 1, WeeklyRules.weekOf(7));
        assertEquals(-1, WeeklyRules.weekOf(-1));
    }

    @Test
    void strengthScaleWorks() {
        assertEquals(25, WeeklyRules.scaledPercent(WeeklyRules.Mod.THICK_HIDE, 100));
        assertEquals(50, WeeklyRules.scaledPercent(WeeklyRules.Mod.THICK_HIDE, 200));
        assertEquals(0, WeeklyRules.scaledPercent(WeeklyRules.Mod.THICK_HIDE, 0));
        assertEquals(1, WeeklyRules.scaledPercent(WeeklyRules.Mod.SWIFT_FOES, 1));
    }

    @Test
    void firstClearPaysOncePerSectorAndKeepsTheRecordSmall() {
        int[] r = FirstClearRules.claim(null, 7);
        assertArrayEquals(new int[]{7}, r);
        assertNull(FirstClearRules.claim(r, 7), "a second claim pays nothing");
        int[] r2 = FirstClearRules.claim(r, 3);
        assertArrayEquals(new int[]{3, 7}, r2);
        assertArrayEquals(new int[]{7}, r, "the old record is not changed");
        int[] full = new int[FirstClearRules.RECORD_CAP];
        for (int i = 0; i < full.length; i++) full[i] = i;
        assertNull(FirstClearRules.claim(full, 9999), "a full record pays nothing more");
    }

    @Test
    void firstClearPrizeGrowsWithTheFloorAndIsCapped() {
        assertEquals(1, FirstClearRules.levels(0));
        assertEquals(2, FirstClearRules.levels(10));
        assertEquals(30, FirstClearRules.levels(100000));
        assertEquals(1, FirstClearRules.items(5));
        assertEquals(16, FirstClearRules.items(100000));
    }

    @Test
    void parTimeBonus() {
        assertTrue(FirstClearRules.withinPar(100, 1000, 900));
        assertFalse(FirstClearRules.withinPar(100, 1001, 900));
        assertFalse(FirstClearRules.withinPar(-1, 10, 900), "unknown entry time never earns the bonus");
        assertFalse(FirstClearRules.withinPar(100, 200, 0), "par 0 = off");
        assertFalse(FirstClearRules.withinPar(500, 100, 900), "a clock that went backwards never earns it");
        assertEquals(2, FirstClearRules.parLevels(0));
        assertEquals(30, FirstClearRules.parLevels(100000));
    }
}
