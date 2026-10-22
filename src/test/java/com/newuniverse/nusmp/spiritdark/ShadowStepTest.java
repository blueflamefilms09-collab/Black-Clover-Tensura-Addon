package com.newuniverse.nusmp.spiritdark;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Shadow Step destination search on a fake grid: a flat stone floor at y = 63 (solid), air above, and cells the tests make solid, hazardous, unloaded or dark. */
class ShadowStepTest {
    /** A fake world: floor at y 63 everywhere in a 100 x 100 area, everything above is air. */
    static class Grid implements ShadowStep.Probe {
        final Set<String> solid = new HashSet<>(), hazard = new HashSet<>(), unloaded = new HashSet<>(), dark = new HashSet<>(), holes = new HashSet<>();
        boolean noLine;
        int lineChecks;
        int baseLight = 15;

        static String k(int x, int y, int z) { return x + "," + y + "," + z; }

        private boolean isSolid(int x, int y, int z) {
            if (solid.contains(k(x, y, z))) return true;
            return y <= 63 && !holes.contains(x + "," + z) && Math.abs(x) < 50 && Math.abs(z) < 50;
        }

        @Override public boolean loaded(int x, int y, int z) { return !unloaded.contains((x >> 4) + "," + (z >> 4)) && Math.abs(x) < 50 && Math.abs(z) < 50; }
        @Override public boolean passable(int x, int y, int z) { return !isSolid(x, y, z); }
        @Override public boolean hazard(int x, int y, int z) { return hazard.contains(k(x, y, z)); }
        @Override public boolean floor(int x, int y, int z) { return isSolid(x, y, z); }
        @Override public int light(int x, int y, int z) { return dark.contains(x + "," + z) ? 2 : baseLight; }
        @Override public boolean lineClear(double x0, double y0, double z0, double x1, double y1, double z1) { lineChecks++; return !noLine; }
        @Override public int minY() { return -64; }
        @Override public int maxY() { return 320; }
    }

    private static ShadowStep.Spot pick(Grid g, double yaw) { return ShadowStep.pick(g, 0.5, 64, 0.5, 1.62, yaw, 0.0, SpiritDarkRules.STEP_RANGE_MAX); }

    @Test
    void onOpenGroundItGoesTheFullRangeStraightAhead() {
        ShadowStep.Spot s = pick(new Grid(), 0.0);
        assertNotNull(s);
        assertEquals(64.0, s.y());
        assertTrue(s.distance() <= SpiritDarkRules.STEP_RANGE_MAX + 0.75 && s.distance() >= SpiritDarkRules.STEP_RANGE_MAX - 1.5, "the farthest: " + s.distance());
        assertTrue(s.z() > 10, "toward where it looks (yaw 0 faces +z)");
        ShadowStep.Spot e = pick(new Grid(), 270.0);
        assertNotNull(e);
        assertTrue(e.x() > 10, "yaw 270 faces +x: " + e.x());
    }

    @Test
    void theRangeIsNeverExceededAndNeverBelowTheMinimum() {
        for (double yaw = 0; yaw < 360; yaw += 15) {
            ShadowStep.Spot s = pick(new Grid(), yaw);
            assertNotNull(s);
            assertTrue(s.distance() <= SpiritDarkRules.STEP_RANGE_MAX + 0.75, "range at yaw " + yaw);
            assertTrue(s.distance() >= SpiritDarkRules.STEP_RANGE_MIN - 0.75);
        }
        assertNull(ShadowStep.pick(new Grid(), 0.5, 64, 0.5, 1.62, 0, 0, 0.0), "no range, no step");
        ShadowStep.Spot s = ShadowStep.pick(new Grid(), 0.5, 64, 0.5, 1.62, 0, 0, 99.0);
        assertTrue(s.distance() <= SpiritDarkRules.STEP_RANGE_MAX + 0.75, "a larger ask is clamped to the rule");
    }

    @Test
    void itPrefersADarkSpotOverALitOneAtTheSameDistance() {
        Grid g = new Grid();
        for (int x = -3; x <= 3; x++) for (int z = 9; z <= 12; z++) g.dark.add(x + "," + z);                   // a dark patch a little short of the full range
        ShadowStep.Spot s = pick(g, 0.0);
        assertNotNull(s);
        assertTrue(s.light() <= SpiritDarkRules.DARK_LIGHT_MAX, "it chose the dark patch: light " + s.light() + " at " + s.x() + "," + s.z());
        Grid lit = new Grid();
        ShadowStep.Spot far = pick(lit, 0.0);
        assertTrue(far.distance() >= s.distance(), "without darkness it would have gone at least as far");
    }

    @Test
    void itNeverLandsInsideABlockOrWithoutHeadroom() {
        Grid g = new Grid();
        for (int x = -40; x <= 40; x++) for (int z = 4; z <= 40; z++) g.solid.add(Grid.k(x, 65, z));            // a ceiling at head height ahead: nothing there fits two cells
        ShadowStep.Spot s = pick(g, 0.0);
        if (s != null) assertTrue(s.z() < 4 || s.y() >= 66, "not under the low ceiling (on top of it is fine): " + s.y() + " at " + s.z());
        Grid wall = new Grid();
        for (int y = 60; y < 70; y++) for (int x = -40; x <= 40; x++) for (int z = 5; z <= 40; z++) wall.solid.add(Grid.k(x, y, z));      // solid rock beyond z = 5
        ShadowStep.Spot w = pick(wall, 0.0);
        assertTrue(w == null || w.z() < 5 || w.y() > 70, "never inside rock");
    }

    @Test
    void itNeverStandsOverTheVoidOrInHazards() {
        Grid g = new Grid();
        for (int x = -40; x <= 40; x++) for (int z = 4; z <= 40; z++) g.holes.add(x + "," + z);                 // no floor at all ahead (a void)
        ShadowStep.Spot s = pick(g, 0.0);
        if (s != null) assertTrue(s.z() < 4, "not over the void: " + s.z());
        Grid h = new Grid();
        for (int x = -40; x <= 40; x++) for (int z = 4; z <= 40; z++) { h.hazard.add(Grid.k(x, 63, z)); h.hazard.add(Grid.k(x, 64, z)); }   // a lava lake: hazard floor and cells
        ShadowStep.Spot l = pick(h, 0.0);
        if (l != null) assertTrue(l.z() < 4, "not on the lava: " + l.z());
    }

    @Test
    void itNeverGoesIntoAnUnloadedChunkOrThroughAWall() {
        Grid g = new Grid();
        for (int cx = -4; cx <= 4; cx++) for (int cz = 1; cz <= 4; cz++) g.unloaded.add(cx + "," + cz);          // every chunk with z >= 16 is unloaded
        ShadowStep.Spot s = pick(g, 0.0);
        if (s != null) assertTrue(s.z() < 16, "a loaded chunk only: " + s.z());
        Grid blocked = new Grid();
        blocked.noLine = true;
        assertNull(pick(blocked, 0.0), "a sight line that is blocked everywhere gives no step");
        assertTrue(blocked.lineChecks <= ShadowStep.MAX_LINE_CHECKS, "bounded clips: " + blocked.lineChecks);
    }

    @Test
    void theHeightIsFollowedUpAndDownSlopesButOnlyByAFewBlocks() {
        Grid g = new Grid();
        for (int x = -40; x <= 40; x++) for (int z = 4; z <= 40; z++) g.solid.add(Grid.k(x, 64, z));            // the ground steps up one block at z = 4
        ShadowStep.Spot s = pick(g, 0.0);
        assertNotNull(s);
        assertEquals(65.0, s.y(), "stands on the raised ground");
        Grid cliff = new Grid();
        for (int x = -40; x <= 40; x++) for (int z = 4; z <= 40; z++) cliff.holes.add(x + "," + z);
        for (int x = -40; x <= 40; x++) for (int z = 4; z <= 40; z++) for (int y = 40; y <= 58; y++) cliff.solid.add(Grid.k(x, y, z));      // the floor drops six blocks to y 58
        ShadowStep.Spot c = pick(cliff, 0.0);
        if (c != null) assertTrue(c.z() < 4 || c.y() >= 59, "it does not follow a deep drop beyond the scan: y " + c.y());
    }

    @Test
    void theBestFewCandidatesAreKeptInOrderAndStillValidChecksAnAnswer() {
        Grid g = new Grid();
        ShadowStep.Spot s = pick(g, 45.0);
        assertNotNull(s);
        assertTrue(ShadowStep.stillValid(g, s, 0.5, 64, 0.5));
        g.solid.add(Grid.k((int) Math.floor(s.x()), (int) s.y(), (int) Math.floor(s.z())));
        assertFalse(ShadowStep.stillValid(g, s, 0.5, 64, 0.5), "a block appeared in the spot");
        assertFalse(ShadowStep.stillValid(new Grid(), new ShadowStep.Spot(0, 64, 100, 100, 15), 0.5, 64, 0.5), "too far");
        assertTrue(ShadowStep.FAN.length * ShadowStep.DISTANCES.length <= 64, "a bounded number of candidate columns");
    }
}
