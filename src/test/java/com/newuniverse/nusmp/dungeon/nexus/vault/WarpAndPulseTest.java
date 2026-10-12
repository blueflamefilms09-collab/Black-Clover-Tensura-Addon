package com.newuniverse.nusmp.dungeon.nexus.vault;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Warp Trap's throw, the traps' placement and the gravity pulses of a pocket's heart. */
class WarpAndPulseTest {
    // ---------------------------------------------------------------- warp
    @Test
    void everyOffsetIsEightToTwentyFourBlocksAway() {
        for (long seed = 0; seed < 3000; seed++) {
            for (WarpPlan.Offset o : WarpPlan.offsets(seed * 7919 + 13, 24)) {
                double d = Math.hypot(o.dx(), o.dz());
                assertTrue(d >= WarpPlan.MIN_DISTANCE - 1e-9 && d <= WarpPlan.MAX_DISTANCE + 1e-9, "distance " + d + " for " + o);
                assertTrue(o.dy() >= -6 && o.dy() <= 6);
            }
        }
        assertEquals(WarpPlan.offsets(5, 24), WarpPlan.offsets(5, 24), "stable");
        assertNotEquals(WarpPlan.offsets(5, 24), WarpPlan.offsets(6, 24));
    }

    @Test
    void offsetsSpreadInAllDirections() {
        Set<Integer> quadrants = new HashSet<>();
        double nearest = 99, farthest = 0;
        for (WarpPlan.Offset o : WarpPlan.offsets(99, 400)) {
            quadrants.add((o.dx() >= 0 ? 1 : 0) + (o.dz() >= 0 ? 2 : 0));
            double d = Math.hypot(o.dx(), o.dz());
            nearest = Math.min(nearest, d);
            farthest = Math.max(farthest, d);
        }
        assertEquals(4, quadrants.size());
        assertTrue(nearest < 10 && farthest > 22, "the whole range is used: " + nearest + " .. " + farthest);
    }

    private static VaultTerrain floorAt(int floorY) {
        return (x, y, z) -> y < floorY ? VaultTerrain.STONE : y == floorY || y == floorY + 1 ? VaultTerrain.AIR : y <= floorY + 5 ? VaultTerrain.AIR : VaultTerrain.STONE;
    }

    @Test
    void aDrySpotIsTwoAirCellsOverSolidRock() {
        VaultTerrain t = floorAt(40);
        assertEquals(40, WarpPlan.spotInColumn(t, 3, 40, 3, false));
        assertEquals(40, WarpPlan.spotInColumn(t, 3, 43, 3, false), "searches down to the floor");
        assertEquals(40, WarpPlan.spotInColumn(t, 3, 37, 3, false), "and up to it");
        assertEquals(Integer.MIN_VALUE, WarpPlan.spotInColumn(t, 3, 60, 3, false), "but only within reach");
    }

    @Test
    void lavaOrUnloadedBlocksNearASpotRefuseIt() {
        VaultTerrain t = floorAt(40);
        VaultTerrain lava = (x, y, z) -> x == 4 && y == 40 && z == 3 ? VaultTerrain.LAVA : t.at(x, y, z);
        assertEquals(Integer.MIN_VALUE, WarpPlan.spotInColumn(lava, 3, 40, 3, false), "lava beside the spot");
        assertEquals(40, WarpPlan.spotInColumn(lava, 10, 40, 3, false), "far from it is fine");
        VaultTerrain unloaded = (x, y, z) -> x == 3 && y == 41 && z == 4 ? VaultTerrain.UNKNOWN : t.at(x, y, z);
        assertEquals(Integer.MIN_VALUE, WarpPlan.spotInColumn(unloaded, 3, 40, 3, false));
    }

    @Test
    void waterIsAPlaceOnlyForSomeoneWhoWasInWater() {
        VaultTerrain flooded = (x, y, z) -> y < 30 ? VaultTerrain.STONE : y < 60 ? VaultTerrain.WATER : VaultTerrain.STONE;
        assertEquals(Integer.MIN_VALUE, WarpPlan.spotInColumn(flooded, 0, 40, 0, false), "no one is dropped into water from a dry floor");
        assertEquals(40, WarpPlan.spotInColumn(flooded, 0, 40, 0, true));
        VaultTerrain solid = (x, y, z) -> VaultTerrain.STONE;
        assertEquals(Integer.MIN_VALUE, WarpPlan.spotInColumn(solid, 0, 40, 0, true), "never inside rock");
    }

    // ---------------------------------------------------------------- traps
    @Test
    void eachChunkLooksAtEightDistinctColumnsOfItsOwn() {
        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                List<TrapPlan.Column> cols = TrapPlan.columns(cx, cz);
                assertEquals(TrapPlan.COLUMNS, cols.size());
                Set<Long> seen = new HashSet<>();
                for (TrapPlan.Column c : cols) {
                    assertEquals(cx, Math.floorDiv(c.x(), 16));
                    assertEquals(cz, Math.floorDiv(c.z(), 16));
                    assertTrue(seen.add((long) c.x() << 32 | (c.z() & 0xFFFFFFFFL)), "distinct");
                }
                assertEquals(cols, TrapPlan.columns(cx, cz), "stable");
            }
        }
    }

    @Test
    void aboutOneColumnInFourIsWantedAndTheFloorPickIsInRange() {
        int wanted = 0, total = 0;
        for (int cx = -30; cx < 30; cx++) {
            for (int cz = -30; cz < 30; cz++) {
                for (TrapPlan.Column c : TrapPlan.columns(cx, cz)) {
                    total++;
                    if (TrapPlan.wanted(c)) wanted++;
                    for (int n = 1; n < 6; n++) {
                        int pick = TrapPlan.pickFloor(c, n);
                        assertTrue(pick >= 0 && pick < n);
                    }
                    assertEquals(-1, TrapPlan.pickFloor(c, 0));
                }
            }
        }
        double share = wanted / (double) total;
        assertTrue(share > 0.20 && share < 0.30, "share " + share);
    }

    // ---------------------------------------------------------------- pulses
    @Test
    void aPulseIsAlwaysWarnedOfFirst() {
        GravityPulse g = new GravityPulse();
        long now = 0;
        boolean telegraphed = false, fired = false;
        for (int i = 0; i < 2000 && !fired; i++) {
            GravityPulse.Act a = g.step(now, 0.9, false, true, 50, 10);
            if (a == GravityPulse.Act.TELEGRAPH) {
                assertFalse(fired);
                telegraphed = true;
            }
            if (a == GravityPulse.Act.LIFT || a == GravityPulse.Act.CRUSH) {
                assertTrue(telegraphed, "no pulse without a warning");
                fired = true;
            }
            now += 4;
        }
        assertTrue(fired);
    }

    @Test
    void theWarningLastsAtLeastAGoodSecondAndThenTheLiftOrCrushComes() {
        GravityPulse g = new GravityPulse();
        long now = 0, warnedAt = -1;
        for (int i = 0; i < 2000; i++, now += 4) {
            GravityPulse.Act a = g.step(now, 1.0, false, true, 0, 0);
            if (a == GravityPulse.Act.TELEGRAPH) warnedAt = now;
            if (a == GravityPulse.Act.LIFT) {
                assertTrue(now - warnedAt >= GravityPulse.WARNING, "warned for " + (now - warnedAt) + " ticks");
                assertTrue(now - warnedAt <= GravityPulse.WARNING + 4);
                return;
            }
            if (a == GravityPulse.Act.SPIRAL) assertTrue(g.armed());
        }
        fail("no lift came");
    }

    @Test
    void noHeadroomMeansACrushNeverALift() {
        for (int roll = 0; roll < 100; roll += 9) {
            GravityPulse g = new GravityPulse();
            long now = 0;
            for (int i = 0; i < 2000; i++, now += 4) {
                GravityPulse.Act a = g.step(now, 0.8, false, false, roll, 0);
                assertNotEquals(GravityPulse.Act.LIFT, a);
            }
        }
    }

    @Test
    void leavingThePocketOrBeingAnchoredCancelsAWarning() {
        GravityPulse g = new GravityPulse();
        long now = 0;
        while (g.step(now, 0.9, false, true, 0, 0) != GravityPulse.Act.TELEGRAPH) now += 4;
        assertTrue(g.armed());
        assertEquals(GravityPulse.Act.NONE, g.step(now + 4, 0.3, false, true, 0, 0), "weaker than 0.6: nothing");
        assertFalse(g.armed(), "and the warning is gone");
        for (long t = now + 8; t < now + 1000; t += 4) assertNotEquals(GravityPulse.Act.LIFT, g.step(t, 0.3, false, true, 0, 0));
        // anchored
        GravityPulse h = new GravityPulse();
        long t2 = 0;
        for (int i = 0; i < 3000; i++, t2 += 4) assertEquals(GravityPulse.Act.NONE, h.step(t2, 1.0, true, true, 0, 0));
    }

    @Test
    void pulsesAreSpacedByAtLeastTheGap() {
        GravityPulse g = new GravityPulse();
        long last = -1_000_000;
        int fired = 0;
        for (long now = 0; now < 20_000; now += 4) {
            GravityPulse.Act a = g.step(now, 0.95, false, true, (int) (now / 4 % 100), 80);
            if (a == GravityPulse.Act.LIFT || a == GravityPulse.Act.CRUSH) {
                assertTrue(now - last >= GravityPulse.MIN_GAP, "gap " + (now - last));
                last = now;
                fired++;
            }
        }
        assertTrue(fired > 20, "pulses keep coming: " + fired);
    }

    @Test
    void impulseGrowsWithStrengthAndIsBounded() {
        assertEquals(0.75, GravityPulse.impulse(0.6), 1e-9);
        assertEquals(1.1, GravityPulse.impulse(1.0), 1e-9);
        assertEquals(0.75, GravityPulse.impulse(0.0), 1e-9);
        assertEquals(1.1, GravityPulse.impulse(5.0), 1e-9);
        assertTrue(GravityPulse.impulse(0.8) > GravityPulse.impulse(0.7));
    }
}
