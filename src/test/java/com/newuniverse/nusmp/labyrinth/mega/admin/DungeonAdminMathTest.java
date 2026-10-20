package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the arithmetic behind {@code /dungeon tp}, {@code skip} and {@code boss} decides which floor an admin lands on, so it is pinned over the whole range of floors the owner
 * named (-1000 to +1000, Floor 0 the Hub between the two roads): the clamp, the sign convention shared with {@link FloorMap} and {@link Polarity}, the level (a decade of ten
 * floors) of every floor and the boss floor of every level, and above all {@link DungeonAdminMath#skip}: forward along the player's own road, from the Hub along the road the
 * caller names, never past +-1000, backwards only as far as the Hub and never across it.
 */
public class DungeonAdminMathTest {

    @Test
    public void theRangeIsTheOwners() {
        assertEquals(1000, DungeonAdminMath.MAX_FLOOR);
        assertEquals(100, DungeonAdminMath.MAX_LEVEL);
        assertEquals(10, DungeonAdminMath.FLOORS_PER_LEVEL);
        assertEquals(DungeonAdminMath.MAX_FLOOR, DungeonAdminMath.MAX_LEVEL * DungeonAdminMath.FLOORS_PER_LEVEL);
    }

    @Test
    public void floorsAreValidFromMinusAThousandToAThousandAndClampBeyond() {
        assertTrue(DungeonAdminMath.validFloor(0));
        assertTrue(DungeonAdminMath.validFloor(1000));
        assertTrue(DungeonAdminMath.validFloor(-1000));
        assertFalse(DungeonAdminMath.validFloor(1001));
        assertFalse(DungeonAdminMath.validFloor(-1001));
        assertFalse(DungeonAdminMath.validFloor(Long.MAX_VALUE));
        assertFalse(DungeonAdminMath.validFloor(Long.MIN_VALUE));
        assertEquals(1000, DungeonAdminMath.clampFloor(1001));
        assertEquals(1000, DungeonAdminMath.clampFloor(Long.MAX_VALUE));
        assertEquals(-1000, DungeonAdminMath.clampFloor(-5000));
        assertEquals(-1000, DungeonAdminMath.clampFloor(Long.MIN_VALUE));
        assertEquals(37, DungeonAdminMath.clampFloor(37));
        assertEquals(0, DungeonAdminMath.clampFloor(0));
    }

    @Test
    public void theSideAndTheRoadFollowTheSignOfTheFloor() {
        assertEquals(1, DungeonAdminMath.side(5));
        assertEquals(-1, DungeonAdminMath.side(-5));
        assertEquals(0, DungeonAdminMath.side(0));
        assertEquals(Polarity.HOLY, DungeonAdminMath.roadOf(1));
        assertEquals(Polarity.ABYSSAL, DungeonAdminMath.roadOf(-1));
        assertNull(DungeonAdminMath.roadOf(0), "the Hub belongs to neither road");
        assertEquals(37, DungeonAdminMath.depth(-37));
        assertEquals(0, DungeonAdminMath.depth(0));
    }

    @Test
    public void everyFloorHasTheLevelOfItsDecade() {
        assertEquals(0, DungeonAdminMath.levelOfFloor(0), "the Hub has no level");
        assertEquals(1, DungeonAdminMath.levelOfFloor(1));
        assertEquals(1, DungeonAdminMath.levelOfFloor(10));
        assertEquals(2, DungeonAdminMath.levelOfFloor(11));
        assertEquals(2, DungeonAdminMath.levelOfFloor(20));
        assertEquals(100, DungeonAdminMath.levelOfFloor(991));
        assertEquals(100, DungeonAdminMath.levelOfFloor(1000));
        assertEquals(-1, DungeonAdminMath.levelOfFloor(-1));
        assertEquals(-1, DungeonAdminMath.levelOfFloor(-10));
        assertEquals(-2, DungeonAdminMath.levelOfFloor(-11));
        assertEquals(-100, DungeonAdminMath.levelOfFloor(-1000));
        assertEquals(100, DungeonAdminMath.levelOfFloor(5000), "a floor beyond the range reads as the last level");
        assertEquals(-100, DungeonAdminMath.levelOfFloor(-5000));
        for (int f = -1000; f <= 1000; f++) {
            int level = DungeonAdminMath.levelOfFloor(f);
            assertEquals(Integer.signum(f), Integer.signum(level), "the level has the sign of the floor " + f);
            assertTrue(Math.abs(level) <= 100, "level of " + f);
            if (f != 0) {
                assertEquals((Math.abs(f) - 1) / 10 + 1, Math.abs(level), "the decade of floor " + f + " (the same rule the waypoint's level uses)");
                int first = DungeonAdminMath.firstFloorOfLevel(level), last = DungeonAdminMath.bossFloorOfLevel(level);
                assertTrue(Math.abs(f) >= Math.abs(first) && Math.abs(f) <= Math.abs(last), "floor " + f + " lies inside its level " + level + " (" + first + ".." + last + ")");
            }
        }
    }

    @Test
    public void everyLevelHasABossFloorAtItsEnd() {
        assertEquals(10, DungeonAdminMath.bossFloorOfLevel(1));
        assertEquals(40, DungeonAdminMath.bossFloorOfLevel(4));
        assertEquals(1000, DungeonAdminMath.bossFloorOfLevel(100));
        assertEquals(-10, DungeonAdminMath.bossFloorOfLevel(-1));
        assertEquals(-40, DungeonAdminMath.bossFloorOfLevel(-4));
        assertEquals(-1000, DungeonAdminMath.bossFloorOfLevel(-100));
        assertEquals(0, DungeonAdminMath.bossFloorOfLevel(0));
        assertEquals(0, DungeonAdminMath.bossFloorOfLevel(101));
        assertEquals(0, DungeonAdminMath.bossFloorOfLevel(-101));
        assertEquals(31, DungeonAdminMath.firstFloorOfLevel(4));
        assertEquals(-31, DungeonAdminMath.firstFloorOfLevel(-4));
        assertEquals(1, DungeonAdminMath.firstFloorOfLevel(1));
        assertEquals(0, DungeonAdminMath.firstFloorOfLevel(0));
        for (int level = -100; level <= 100; level++) {
            assertEquals(level != 0, DungeonAdminMath.validLevel(level), "level " + level);
            if (level == 0) continue;
            int boss = DungeonAdminMath.bossFloorOfLevel(level);
            assertTrue(DungeonAdminMath.isBossFloor(boss), "the boss floor of level " + level + " is a boss floor");
            assertEquals(level, DungeonAdminMath.levelOfFloor(boss), "round trip for level " + level);
            assertEquals(Integer.signum(level), Integer.signum(boss));
        }
        assertFalse(DungeonAdminMath.validLevel(101));
        assertFalse(DungeonAdminMath.validLevel(-101));
        assertFalse(DungeonAdminMath.validLevel(Integer.MIN_VALUE), "no overflow on the most negative int");
    }

    @Test
    public void bossFloorsAreEveryTenthFloorNeverTheHubNeverBeyondTheRange() {
        int n = 0;
        for (int f = -1000; f <= 1000; f++) {
            boolean expected = f != 0 && f % 10 == 0;
            assertEquals(expected, DungeonAdminMath.isBossFloor(f), "floor " + f);
            assertEquals(FloorMap.isBossFloor(f), DungeonAdminMath.isBossFloor(f), "agrees with FloorMap for floor " + f);
            if (expected) n++;
        }
        assertEquals(200, n, "200 boss floors: 100 Holy, 100 Evil");
        assertFalse(DungeonAdminMath.isBossFloor(1010), "beyond the range");
        assertFalse(DungeonAdminMath.isBossFloor(-1010));
        assertFalse(DungeonAdminMath.isBossFloor(0));
    }

    // ---------------------------------------------------------------- skip
    @Test
    public void skipGoesForwardAlongTheOwnRoad() {
        assertEquals(new DungeonAdminMath.Skip(6, false, 1), DungeonAdminMath.skip(5, 1, 1));
        assertEquals(new DungeonAdminMath.Skip(-6, false, -1), DungeonAdminMath.skip(-5, 1, 1), "the hub side is ignored on a road");
        assertEquals(new DungeonAdminMath.Skip(15, false, 1), DungeonAdminMath.skip(5, 10, -1));
        assertEquals(new DungeonAdminMath.Skip(-25, false, -1), DungeonAdminMath.skip(-5, 20, 1));
        assertEquals(new DungeonAdminMath.Skip(1000, false, 1), DungeonAdminMath.skip(1, 999, 1));
        assertEquals(new DungeonAdminMath.Skip(-1000, false, -1), DungeonAdminMath.skip(-1, 999, 1));
    }

    @Test
    public void skipFromTheHubUsesTheSideTheCallerNames() {
        assertEquals(new DungeonAdminMath.Skip(1, false, 1), DungeonAdminMath.skip(0, 1, 1));
        assertEquals(new DungeonAdminMath.Skip(-1, false, -1), DungeonAdminMath.skip(0, 1, -1));
        assertEquals(new DungeonAdminMath.Skip(7, false, 1), DungeonAdminMath.skip(0, 7, 0), "no side named reads as the Holy road");
        assertEquals(new DungeonAdminMath.Skip(-7, false, -1), DungeonAdminMath.skip(0, 7, -3), "any negative side is the Abyssal road");
        assertEquals(new DungeonAdminMath.Skip(1, false, 1), DungeonAdminMath.skip(0, 1, 99), "any positive side is the Holy road");
    }

    @Test
    public void skipStopsAtTheEndOfTheRoadAndSaysSo() {
        assertEquals(new DungeonAdminMath.Skip(1000, true, 1), DungeonAdminMath.skip(995, 20, 1));
        assertEquals(new DungeonAdminMath.Skip(-1000, true, -1), DungeonAdminMath.skip(-995, 20, 1));
        assertEquals(new DungeonAdminMath.Skip(1000, true, 1), DungeonAdminMath.skip(1000, 1, 1), "already at the end");
        assertEquals(new DungeonAdminMath.Skip(1000, true, 1), DungeonAdminMath.skip(0, 5000, 1));
        assertEquals(new DungeonAdminMath.Skip(-1000, true, -1), DungeonAdminMath.skip(0, 5000, -1));
        assertEquals(new DungeonAdminMath.Skip(1000, true, 1), DungeonAdminMath.skip(1, Integer.MAX_VALUE, 1), "no int overflow");
        assertEquals(new DungeonAdminMath.Skip(-1000, true, -1), DungeonAdminMath.skip(-1, Integer.MAX_VALUE, 1));
    }

    @Test
    public void skippingBackStopsAtTheHubAndNeverCrossesIt() {
        assertEquals(new DungeonAdminMath.Skip(2, false, 1), DungeonAdminMath.skip(5, -3, 1));
        assertEquals(new DungeonAdminMath.Skip(-2, false, -1), DungeonAdminMath.skip(-5, -3, 1));
        assertEquals(new DungeonAdminMath.Skip(0, false, 1), DungeonAdminMath.skip(5, -5, 1), "exactly to the Hub is not a cap");
        assertEquals(new DungeonAdminMath.Skip(0, false, -1), DungeonAdminMath.skip(-5, -5, 1));
        assertEquals(new DungeonAdminMath.Skip(0, true, 1), DungeonAdminMath.skip(5, -6, 1), "past the Hub it stops on it");
        assertEquals(new DungeonAdminMath.Skip(0, true, -1), DungeonAdminMath.skip(-5, -500, 1));
        assertEquals(new DungeonAdminMath.Skip(0, true, 1), DungeonAdminMath.skip(0, -1, 1), "nothing behind the Hub");
        assertEquals(new DungeonAdminMath.Skip(0, true, -1), DungeonAdminMath.skip(0, -1, -1));
    }

    @Test
    public void skippingZeroFloorsStaysWhereItIs() {
        assertEquals(new DungeonAdminMath.Skip(5, false, 1), DungeonAdminMath.skip(5, 0, -1));
        assertEquals(new DungeonAdminMath.Skip(-5, false, -1), DungeonAdminMath.skip(-5, 0, 1));
        assertEquals(new DungeonAdminMath.Skip(0, false, 1), DungeonAdminMath.skip(0, 0, 1));
        assertEquals(new DungeonAdminMath.Skip(0, false, -1), DungeonAdminMath.skip(0, 0, -1));
    }

    @Test
    public void skipAlwaysLandsInsideTheRangeAndOnTheSameSideOrTheHub() {
        int[] currents = {-1000, -999, -500, -11, -10, -1, 0, 1, 10, 11, 500, 999, 1000};
        int[] counts = {-2000, -1000, -999, -50, -11, -1, 0, 1, 2, 9, 10, 11, 100, 999, 1000, 5000, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int cur : currents) {
            for (int n : counts) {
                for (int hub : new int[] {-1, 0, 1}) {
                    DungeonAdminMath.Skip s = DungeonAdminMath.skip(cur, n, hub);
                    String what = "skip(" + cur + ", " + n + ", " + hub + ") = " + s;
                    assertTrue(DungeonAdminMath.validFloor(s.target()), what);
                    if (cur != 0 && s.target() != 0) assertEquals(Integer.signum(cur), Integer.signum(s.target()), "never changes road: " + what);
                    if (n > 0) assertTrue(Math.abs(s.target()) >= Math.abs(cur), "forward never goes nearer the Hub: " + what);
                    if (n < 0) assertTrue(Math.abs(s.target()) <= Math.abs(cur), "back never goes farther from the Hub: " + what);
                    if (n == 0) assertEquals(cur, s.target(), what);
                }
            }
        }
    }

    // ---------------------------------------------------------------- growing the dungeon
    @Test
    public void theChainIndexOfAFloorFollowsTheFloorModel() {
        assertEquals(0, DungeonAdminMath.chainIndexOf(1, 1));
        assertEquals(0, DungeonAdminMath.chainIndexOf(-1, 1));
        assertEquals(41, DungeonAdminMath.chainIndexOf(42, 1));
        assertEquals(41, DungeonAdminMath.chainIndexOf(-42, 1));
        assertEquals(999, DungeonAdminMath.chainIndexOf(1000, 1));
        assertEquals(1, DungeonAdminMath.chainIndexOf(15, 10), "the old ten-floor sectors");
        assertEquals(-1, DungeonAdminMath.chainIndexOf(0, 1), "the Hub is in no chain");
        assertEquals(41, DungeonAdminMath.chainIndexOf(42, 0), "a nonsense sector length reads as 1");
    }

    @Test
    public void theClimbIsCountedInSectorsAndSeconds() {
        assertEquals(0, DungeonAdminMath.sectorsToBuild(5, 5));
        assertEquals(0, DungeonAdminMath.sectorsToBuild(9, 5));
        assertEquals(3, DungeonAdminMath.sectorsToBuild(2, 5));
        assertEquals(6, DungeonAdminMath.sectorsToBuild(-1, 5), "an empty road needs the first sector too");
        assertEquals(0, DungeonAdminMath.etaSeconds(0, 2.0));
        assertEquals(0, DungeonAdminMath.etaSeconds(-3, 2.0));
        assertEquals(0, DungeonAdminMath.etaSeconds(5, 0.0));
        assertEquals(0, DungeonAdminMath.etaSeconds(5, Double.NaN));
        assertEquals(10, DungeonAdminMath.etaSeconds(5, 2.0));
        assertEquals(3, DungeonAdminMath.etaSeconds(1, 2.5), "rounded up");
        assertEquals(100, DungeonAdminMath.percent(0, 0));
        assertEquals(0, DungeonAdminMath.percent(0, 10));
        assertEquals(50, DungeonAdminMath.percent(5, 10));
        assertEquals(100, DungeonAdminMath.percent(10, 10));
        assertEquals(100, DungeonAdminMath.percent(99, 10));
        assertEquals(0, DungeonAdminMath.percent(-4, 10));
    }
}
