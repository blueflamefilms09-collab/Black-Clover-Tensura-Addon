package com.newuniverse.nusmp.spiritspace;

import com.newuniverse.nusmp.spiritspace.Waypoints.Waypoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Waypoint validation: names, well-formedness, cleaning a stored book, and the checks of a trip (same dimension, in range, slot set). */
class WaypointsTest {
    private static final String OW = "minecraft:overworld", NETHER = "minecraft:the_nether";

    private static Waypoint wp(String name, String dim, double x, double y, double z) { return new Waypoint(name, dim, x, y, z, 0f); }

    @Test
    void namesAreCleanedTrimmedAndCappedAndABlankOneBecomesTheSlotsDefault() {
        assertEquals("Home Base", Waypoints.cleanName("  Home Base  ", 0));
        assertEquals("Waypoint 3", Waypoints.cleanName("", 2));
        assertEquals("Waypoint 1", Waypoints.cleanName(null, 0));
        assertEquals("Waypoint 2", Waypoints.cleanName("§§§<>", 1), "only letters, digits, space, underscore, hyphen and apostrophe survive");
        assertEquals("Mom's-place_2", Waypoints.cleanName("Mom's-place_2!", 0));
        assertEquals(SpiritSpaceRules.NAME_MAX, Waypoints.cleanName("a".repeat(100), 0).length());
        assertEquals("ab", Waypoints.cleanName("a\u0000\n\tb", 0));
    }

    @Test
    void aWaypointMustBeFiniteInsideTheBuildHeightAndNamed() {
        assertTrue(Waypoints.valid(wp("Home", OW, 10, 64, 10), -64, 320));
        assertFalse(Waypoints.valid(wp("Home", OW, Double.NaN, 64, 10), -64, 320));
        assertFalse(Waypoints.valid(wp("Home", OW, 10, Double.POSITIVE_INFINITY, 10), -64, 320));
        assertFalse(Waypoints.valid(wp("Home", OW, 10, 400, 10), -64, 320));
        assertFalse(Waypoints.valid(wp("Home", OW, 10, -100, 10), -64, 320));
        assertFalse(Waypoints.valid(wp("Home", OW, 5.0E7, 64, 0), -64, 320), "beyond the world border of the game");
        assertFalse(Waypoints.valid(wp("", OW, 0, 64, 0), -64, 320));
        assertFalse(Waypoints.valid(wp("Home", "", 0, 64, 0), -64, 320));
        assertFalse(Waypoints.valid(null, -64, 320));
        assertFalse(Waypoints.valid(new Waypoint("Home", OW, 0, 64, 0, Float.NaN), -64, 320));
        assertFalse(Waypoints.valid(wp("x".repeat(40), OW, 0, 64, 0), -64, 320));
    }

    @Test
    void cleaningDropsBrokenEntriesAndUnknownDimensionsAndFixesNames() {
        Waypoint[] raw = {wp("Good", OW, 1, 64, 1), wp("Bad", OW, Double.NaN, 64, 1), wp("Gone", "mod:removed_dimension", 1, 64, 1), wp("  x<y>  ", OW, 2, 70, 2), wp("Extra", OW, 3, 64, 3)};
        Waypoint[] out = Waypoints.clean(raw, 4, -64, 320, d -> d.equals(OW) || d.equals(NETHER));
        assertEquals(4, out.length, "exactly the configured number of slots");
        assertEquals("Good", out[0].name());
        assertNull(out[1], "a malformed waypoint is cleared");
        assertNull(out[2], "a waypoint of a dimension that no longer exists is cleared");
        assertEquals("xy", out[3].name(), "a name is cleaned");
        assertEquals(1, Waypoints.count(new Waypoint[]{null, out[0], null}));
        Waypoint[] shrunk = Waypoints.clean(raw, 1, -64, 320, d -> true);
        assertEquals(1, shrunk.length, "slots beyond the configured number are dropped");
        assertEquals(3, Waypoints.clean(null, 3, -64, 320, d -> true).length);
        assertEquals(raw[0], raw[0], "the input is not changed");
        assertEquals("  x<y>  ", raw[3].name());
    }

    @Test
    void aTripNeedsASetSlotTheSameDimensionAndTheRange() {
        Waypoint[] book = {wp("Home", OW, 50, 64, 0), null, wp("Hole", NETHER, 0, 64, 0)};
        assertNull(Waypoints.checkTrip(book, 0, OW, 0, 64, 0, 96));
        assertEquals(Waypoints.TOO_FAR, Waypoints.checkTrip(book, 0, OW, 0, 64, 0, 40));
        assertEquals(Waypoints.EMPTY, Waypoints.checkTrip(book, 1, OW, 0, 64, 0, 96));
        assertEquals(Waypoints.OTHER_DIMENSION, Waypoints.checkTrip(book, 2, OW, 0, 64, 0, 96), "a waypoint of another dimension is never used, however near");
        assertEquals(Waypoints.OTHER_DIMENSION, Waypoints.checkTrip(book, 0, NETHER, 0, 64, 0, 96));
        assertEquals(Waypoints.BAD_SLOT, Waypoints.checkTrip(book, 3, OW, 0, 64, 0, 96));
        assertEquals(Waypoints.BAD_SLOT, Waypoints.checkTrip(book, -1, OW, 0, 64, 0, 96));
        assertEquals(Waypoints.BAD_SLOT, Waypoints.checkTrip(null, 0, OW, 0, 64, 0, 96));
        assertEquals(Waypoints.OTHER_DIMENSION, Waypoints.checkTrip(book, 0, null, 0, 64, 0, 96));
    }

    @Test
    void aSpotToMarkMustBeFiniteAndInsideTheBuildHeight() {
        assertNull(Waypoints.checkSpot(0, 64, 0, -64, 320));
        assertEquals(Waypoints.BAD_SPOT, Waypoints.checkSpot(Double.NaN, 64, 0, -64, 320));
        assertEquals(Waypoints.BAD_SPOT, Waypoints.checkSpot(0, 500, 0, -64, 320));
        assertEquals(Waypoints.BAD_SPOT, Waypoints.checkSpot(4.0E7, 64, 0, -64, 320));
    }
}
