package com.newuniverse.nusmp.labyrinth.mega;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: in the floor model (1.58.0) a sector is ONE floor, and the same numbers (floors per sector, boss every tenth floor, a theme per decade) are read by the
 * planner, the draft, the generator, the travel, the lock, the System overlay and the flow module. The one regression that would hurt is the old rule read the new
 * way: with one floor per sector "a decennial floor is every {@code perRegion}-th" would make EVERY floor a boss floor. Each rule is written out longhand here, next
 * to the old rule it must not break, so a drift fails the build instead of putting a Guardian on every floor.
 */
public class FloorModelTest {

    @Test
    public void aSectorIsOneFloorByDefaultAndTheOldLengthsStayValid() {
        assertEquals(1, FloorMap.DEFAULT_PER_SECTOR);
        assertEquals(10, FloorMap.DEFAULT_PER_REGION, "the old length stays the default of a sector saved before 1.58.0");
        assertTrue(FloorMap.isFloorModel(1));
        assertTrue(FloorMap.isFloorModel(0), "a damaged value reads as the floor model, never as a sector of no floors");
        assertFalse(FloorMap.isFloorModel(2));
        assertFalse(FloorMap.isFloorModel(10));
        assertEquals(1, FloorMap.floorsPerSector(0));
        assertEquals(1, FloorMap.floorsPerSector(-5));
        assertEquals(1, FloorMap.floorsPerSector(1));
        assertEquals(10, FloorMap.floorsPerSector(10));
        assertEquals(20, FloorMap.floorsPerSector(20));
        assertEquals(20, FloorMap.floorsPerSector(99), "the most the banding supports");
    }

    @Test
    public void theFloorOfASectorIsTheSignOfTheRoadTimesItsPlace() {
        for (int k = 0; k < 60; k++) {
            assertEquals(k + 1, FloorMap.floorOfSector(Polarity.HOLY, k), "holy sector " + k);
            assertEquals(-(k + 1), FloorMap.floorOfSector(Polarity.ABYSSAL, k), "abyssal sector " + k);
            for (Polarity p : Polarity.values()) {
                int f = FloorMap.floorOfSector(p, k);
                assertEquals(f, FloorMap.firstFloor(p, k, 1), "one floor: first");
                assertEquals(f, FloorMap.lastFloor(p, k, 1), "one floor: last");
                assertEquals(k, FloorMap.chainIndex(f, 1), "the place round-trips");
                assertEquals(1, FloorMap.band(f, 1), "a one-floor sector has only band 1");
                assertEquals(p, Polarity.ofFloor(f));
            }
        }
        assertEquals(1, FloorMap.floorOfSector(Polarity.HOLY, -4), "a negative place reads as the first");
    }

    @Test
    public void everyTenthFloorIsABossFloorWhateverTheLengthOfASector() {
        for (int f : new int[] {10, 20, 30, 100, -10, -20, -30, -100}) {
            assertTrue(FloorMap.isBossFloor(f), f + " is a boss floor");
            assertTrue(FloorMap.isDecennial(f, 1), f + " is decennial in the floor model");
            assertTrue(FloorMap.isDecennial(f, 10), f + " is decennial in the banded model too");
        }
        for (int f : new int[] {0, 1, 2, 5, 9, 11, 19, 21, 99, -1, -5, -9, -11, -19}) {
            assertFalse(FloorMap.isBossFloor(f), f + " is no boss floor");
            assertFalse(FloorMap.isDecennial(f, 1), f + ": with one floor per sector a floor is NOT decennial just because it is its sector's last");
        }
        assertFalse(FloorMap.isBossFloor(0), "Floor 0 is the Hub");
        assertFalse(FloorMap.isDecennial(0, 1));
    }

    @Test
    public void theOldDecennialRuleIsUnchangedForTheBandedModel() {
        for (int f : new int[] {10, 20, -30}) assertTrue(FloorMap.isDecennial(f), f + " is decennial");
        for (int f : new int[] {0, 1, 9, 11, 19, -1, -9}) assertFalse(FloorMap.isDecennial(f), f + " is not decennial");
        assertTrue(FloorMap.isDecennial(5, 5), "a server may use shorter regions");
        assertTrue(FloorMap.isDecennial(15, 5));
        assertFalse(FloorMap.isDecennial(7, 5));
        assertTrue(FloorMap.isDecennial(20, 20), "twenty floors per sector: the boss ends the sector");
        assertFalse(FloorMap.isDecennial(10, 20));
    }

    @Test
    public void aBossFloorIsTheLastOfItsDecadeAndTheNextFloorOpensTheNext() {
        for (int k = 0; k < 120; k++) {
            boolean boss = FloorMap.isBossFloor(FloorMap.floorOfSector(Polarity.HOLY, k));
            assertEquals(k % 10 == 9, boss, "sector " + k);
            assertEquals(boss, FloorMap.startsDecade(k + 1, 1), "the floor after a boss floor opens a decade: sector " + (k + 1));
            assertEquals(boss, FloorMap.isBossFloor(FloorMap.floorOfSector(Polarity.ABYSSAL, k)), "both roads count the same");
        }
    }

    @Test
    public void aThemeBelongsToADecadeOfFloors() {
        for (int k = 0; k < 10; k++) assertEquals(0, FloorMap.decadeOf(k, 1), "floors +1..+10 are decade 0");
        for (int k = 10; k < 20; k++) assertEquals(1, FloorMap.decadeOf(k, 1), "floors +11..+20 are decade 1");
        assertEquals(12, FloorMap.decadeOf(123, 1));
        assertTrue(FloorMap.startsDecade(0, 1));
        assertTrue(FloorMap.startsDecade(10, 1));
        assertTrue(FloorMap.startsDecade(20, 1));
        for (int k : new int[] {1, 2, 5, 9, 11, 19, 21}) assertFalse(FloorMap.startsDecade(k, 1), "sector " + k + " does not open a decade");
        for (int k = 0; k < 30; k++) {
            assertEquals(k, FloorMap.decadeOf(k, 10), "ten floors per sector: each sector is its own decade (the 1.55.0 rule)");
            assertTrue(FloorMap.startsDecade(k, 10), "and every sector opens one");
        }
        assertEquals(0, FloorMap.decadeOf(1, 5));
        assertEquals(1, FloorMap.decadeOf(2, 5), "five floors per sector: two sectors a decade");
        assertTrue(FloorMap.startsDecade(2, 5));
        assertFalse(FloorMap.startsDecade(3, 5));
        assertEquals(0, FloorMap.decadeOf(-7, 1), "a negative place reads as the first");
        assertTrue(FloorMap.startsDecade(-7, 1));
        assertEquals(0, FloorMap.decadeOf(3, 0), "a damaged length reads as one floor");
    }

    @Test
    public void aDecadeNeverSplitsAcrossTheBossFloorOfAnother() {
        // the three questions the generator asks of a sector must agree: which decade, does it open one, is it the boss floor that closes one
        int open = 0;
        for (int k = 0; k < 200; k++) {
            if (FloorMap.startsDecade(k, 1)) open++;
            int decade = FloorMap.decadeOf(k, 1);
            assertEquals(open - 1, decade, "the decade counts the openers so far: sector " + k);
            if (FloorMap.isBossFloor(FloorMap.floorOfSector(Polarity.HOLY, k))) assertEquals(decade, FloorMap.decadeOf(k + 1, 1) - 1, "the next sector is in the next decade after a boss floor");
            else assertEquals(decade, FloorMap.decadeOf(k + 1, 1), "inside a decade the decade does not change: sector " + k);
        }
    }
}
