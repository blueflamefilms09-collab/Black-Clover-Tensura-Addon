package com.newuniverse.nusmp.compat.maps;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The waypoint chat strings: exact layout, colon escaping, dimension keys, round trip. The layout itself is from memory (see WaypointShare), the tests pin what this class promises. */
class WaypointShareTest {
    @Test
    void theXaeroStringHasTheDocumentedLayout() {
        String s = WaypointShare.xaero("Hub Gate", 12, 70, -30, 14, "nusmp:sanctuary");
        assertEquals("xaero-waypoint:Hub Gate:H:12:70:-30:14:false:0:Internal-dim%nusmp$sanctuary-waypoints", s);
        assertEquals(10, s.split(":", -1).length, "prefix, name, initials, x, y, z, colour, disabled, type, set");
    }

    @Test
    void vanillaDimensionsUseTheirShortNames() {
        assertEquals("overworld", WaypointShare.xaeroDimensionKey("minecraft:overworld"));
        assertEquals("the-nether", WaypointShare.xaeroDimensionKey("minecraft:the_nether"));
        assertEquals("the-end", WaypointShare.xaeroDimensionKey("minecraft:the_end"));
        assertEquals("Internal-overworld-waypoints", WaypointShare.xaeroSet("minecraft:overworld"));
    }

    @Test
    void customDimensionsUseDimPercentNamespaceDollarPath() {
        assertEquals("dim%nusmp$mega_dungeon", WaypointShare.xaeroDimensionKey("nusmp:mega_dungeon"));
        assertEquals("Internal-dim%nusmp$mega_dungeon-waypoints", WaypointShare.xaeroSet("nusmp:mega_dungeon"));
        assertEquals("dim%mod$a%b", WaypointShare.xaeroDimensionKey("mod:a/b"));
        assertEquals("dim%minecraft$something", WaypointShare.xaeroDimensionKey("minecraft:something"));
        assertEquals("overworld", WaypointShare.xaeroDimensionKey(null));
        assertEquals("overworld", WaypointShare.xaeroDimensionKey("garbage"));
    }

    @Test
    void colonsInANameAreEscapedAndTheStringStaysColonSeparated() {
        String s = WaypointShare.xaero("Floor 3: Gate", 1, 2, 3, 11, "nusmp:mega_dungeon");
        assertTrue(s.contains("Floor 3^col^ Gate"), s);
        assertEquals(10, s.split(":", -1).length, s);
        Optional<WaypointShare.Waypoint> back = WaypointShare.parse(s);
        assertTrue(back.isPresent());
        assertEquals("Floor 3: Gate", back.get().name());
    }

    @Test
    void namesAreCleanedLimitedAndNeverEmpty() {
        assertEquals("Waypoint", WaypointShare.escapeName(""));
        assertEquals("Waypoint", WaypointShare.escapeName(null));
        assertEquals("Red", WaypointShare.escapeName("§cRed"));
        assertEquals("ab", WaypointShare.escapeName("a\u0007b"));
        assertEquals(WaypointShare.MAX_NAME, WaypointShare.escapeName("x".repeat(100)).length());
        assertEquals("W", WaypointShare.initials("!!!"));
        assertEquals("G", WaypointShare.initials("gate"));
        assertEquals("3", WaypointShare.initials(" 3rd"));
    }

    @Test
    void colourIsClampedToTheSixteenXaeroColours() {
        assertTrue(WaypointShare.xaero("a", 0, 0, 0, 99, "minecraft:overworld").contains(":15:false:0:"));
        assertTrue(WaypointShare.xaero("a", 0, 0, 0, -4, "minecraft:overworld").contains(":0:false:0:"));
        for (WaypointShare.Kind k : WaypointShare.Kind.values()) assertTrue(k.colour >= 0 && k.colour <= 15, k.name());
    }

    @Test
    void roundTrip() {
        String s = WaypointShare.xaero("Return Waypoint", -5, 64, 4000100, WaypointShare.Kind.RETURN, "nusmp:mega_dungeon");
        WaypointShare.Waypoint w = WaypointShare.parse(s).orElseThrow();
        assertEquals("Return Waypoint", w.name());
        assertEquals("R", w.initials());
        assertEquals(-5, w.x());
        assertEquals(64, w.y());
        assertEquals(4000100, w.z());
        assertEquals(WaypointShare.Kind.RETURN.colour, w.colour());
        assertFalse(w.disabled());
        assertEquals(0, w.type());
        assertTrue(WaypointShare.setMatches(w.set(), "nusmp:mega_dungeon"));
        assertFalse(WaypointShare.setMatches(w.set(), "nusmp:sanctuary"));
    }

    @Test
    void parseAcceptsTheOldPrefixTildeAndExtraFields() {
        assertTrue(WaypointShare.parse("xaero_waypoint:A:A:1:2:3:4:false:0:Internal-overworld-waypoints").isPresent());
        WaypointShare.Waypoint t = WaypointShare.parse("xaero-waypoint:A:A:1:~:3:4:true:1:Internal-the-end-waypoints").orElseThrow();
        assertEquals(WaypointShare.UNKNOWN_Y, t.y());
        assertTrue(t.disabled());
        WaypointShare.Waypoint extra = WaypointShare.parse("xaero-waypoint:A:A:1:2:3:4:false:0:false:90:Internal-the-end-waypoints").orElseThrow();
        assertEquals("Internal-the-end-waypoints", extra.set());
        assertEquals("", WaypointShare.parse("xaero-waypoint:A:A:1:2:3:4:false:0").orElseThrow().set());
    }

    @Test
    void parseRejectsGarbage() {
        assertTrue(WaypointShare.parse(null).isEmpty());
        assertTrue(WaypointShare.parse("hello").isEmpty());
        assertTrue(WaypointShare.parse("xaero-waypoint:A:A:x:2:3:4:false:0:Internal-overworld-waypoints").isEmpty());
        assertTrue(WaypointShare.parse("xaero-waypoint:A:A:1:2:3").isEmpty());
        assertTrue(WaypointShare.parse("xaero-waypointer:A:A:1:2:3:4:false:0:S").isEmpty());
    }

    @Test
    void journeyMapForm() {
        assertEquals("[name:\"Gate\", x:12, y:64, z:-30, dim:minecraft:overworld]", WaypointShare.journeyMap("Gate", 12, 64, -30, "minecraft:overworld"));
        assertEquals("[name:\"Say 'hi'\", x:0, y:0, z:0, dim:nusmp:sanctuary]", WaypointShare.journeyMap("Say \"hi\"", 0, 0, 0, "nusmp:sanctuary"));
        assertTrue(WaypointShare.journeyMap(null, 0, 0, 0, null).contains("name:\"Waypoint\""));
    }
}
