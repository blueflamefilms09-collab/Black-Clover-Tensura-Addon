package com.newuniverse.nusmp.labyrinth.mega;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the contract classes of the Mega-Dungeon (grid numbers, room kinds by folder, the marker grammar, themes) are what nine builders
 * wrote their code and their templates against; if one of these rules drifts (a module edge, a folder name, a marker key) every template and
 * every system breaks silently and only in game. They are pure (no Minecraft types), so they are pinned here, and the same numbers are
 * pinned on the Python side by tools/labyrinth/mega_check.py.
 */
public class MegaContractTest {

    @Test
    public void gridNumbersAreTheOnesTheTemplatesUse() {
        assertEquals(9, MegaGrid.MODULE);
        assertEquals(4, MegaGrid.CENTER);
        assertEquals(8, MegaGrid.FLOOR_ROW);
        assertEquals(48, MegaGrid.HEIGHT);
        assertEquals(9, MegaGrid.DOOR_ROW);
        assertEquals(56, MegaGrid.ORIGIN_Y);
        assertEquals(64, MegaGrid.FLOOR_Y);
        assertTrue(MegaGrid.REGION_GAP > MegaGrid.CAP_MARGIN, "a cap of one sector must never reach a piece of another");
    }

    @Test
    public void socketOffsetsAreModuleCentres() {
        int[] o = MegaGrid.socketOffsets(27);
        assertEquals(3, o.length);
        assertEquals(4, o[0]);
        assertEquals(13, o[1]);
        assertEquals(22, o[2]);
        assertTrue(MegaGrid.isSocketOffset(13, 27));
        assertFalse(MegaGrid.isSocketOffset(12, 27));
        assertFalse(MegaGrid.isSocketOffset(4, 10), "10 is not a whole number of modules");
        assertTrue(MegaGrid.onGrid(45));
        assertFalse(MegaGrid.onGrid(46));
    }

    @Test
    public void aSocketOffsetSurvivesAHalfTurn() {
        // The reason the module is odd: turning a piece by 180 degrees maps offset k to edge-1-k, which must be a legal offset again.
        for (int modules = 1; modules <= 6; modules++) {
            int edge = modules * MegaGrid.MODULE;
            for (int off : MegaGrid.socketOffsets(edge)) assertTrue(MegaGrid.isSocketOffset(edge - 1 - off, edge), "edge " + edge + " offset " + off);
        }
    }

    @Test
    public void roomKindComesFromTheFolder() {
        assertEquals(RoomKind.SAFE, RoomKind.of("nusmp:mega/safe/hearth_hall_a"));
        assertEquals(RoomKind.LANDING, RoomKind.of("mega/landing/landing_a"));
        assertEquals(RoomKind.CAP, RoomKind.of("nusmp:mega/cap/plain_wall"));
        assertEquals(RoomKind.SET_PIECE, RoomKind.of("nusmp:mega/set/cathedral_rotunda"));
        assertEquals(RoomKind.OTHER, RoomKind.of("nusmp:labyrinth/base"));
        assertEquals(RoomKind.OTHER, RoomKind.of(null));
        assertEquals(RoomKind.OTHER, RoomKind.of("nusmp:mega/unknown_folder/x"));
        assertTrue(RoomKind.SAFE.isSafe() && RoomKind.LANDING.isSafe() && !RoomKind.HALL.isSafe());
        assertTrue(RoomKind.CORRIDOR.isCirculation() && RoomKind.LINK.isCirculation() && !RoomKind.VAULT.isCirculation());
    }

    @Test
    public void everyRoomKindFolderIsUnique() {
        Set<String> seen = new HashSet<>();
        for (RoomKind k : RoomKind.values()) if (!k.folder().isEmpty()) assertTrue(seen.add(k.folder()), "duplicate folder " + k.folder());
    }

    @Test
    public void markerGrammar() {
        MarkerSpec s = MarkerSpec.parse("nusmp:seat;as=chair;facing=north");
        assertNotNull(s);
        assertEquals(MarkerKind.SEAT, s.kind());
        assertEquals("chair", s.arg("as", ""));
        assertEquals("north", s.arg("facing", ""));
        assertEquals("nusmp:seat;as=chair;facing=north", s.toMetadata());
        MarkerSpec e = MarkerSpec.parse("nusmp:expansion_socket;facing=west;w=3;h=4;d=2");
        assertNotNull(e);
        assertEquals(MarkerKind.EXPANSION_SOCKET, e.kind());
        assertEquals(4, e.intArg("h", 0));
        assertEquals(7, e.intArg("missing", 7));
        MarkerSpec flag = MarkerSpec.parse("nusmp:spawn;boss");
        assertNotNull(flag);
        assertEquals("true", flag.arg("boss", ""));
        assertNull(MarkerSpec.parse("minecraft:seat"), "another namespace is not ours");
        assertNull(MarkerSpec.parse("nusmp:not_a_kind"), "an unknown kind is skipped, not an error");
        assertNull(MarkerSpec.parse(null));
        assertNull(MarkerSpec.parse(""));
    }

    @Test
    public void markerKindIdsRoundTrip() {
        for (MarkerKind k : MarkerKind.values()) {
            assertEquals(k, MarkerKind.byId(k.id()));
            assertEquals(k, MarkerKind.byId("nusmp:" + k.id()));
            assertEquals(k, MarkerKind.byId(k.id().toUpperCase()));
        }
        assertNull(MarkerKind.byId("zzz"));
    }

    @Test
    public void themesAreDeterministicAndNeverRepeat() {
        assertEquals(RegionTheme.GRAVITY, RegionTheme.pick(99L, 0, null), "the first sector is the baseline look");
        RegionTheme prev = RegionTheme.GRAVITY;
        Set<RegionTheme> seen = new HashSet<>();
        for (int id = 1; id < 400; id++) {
            RegionTheme t = RegionTheme.pick(99L, id, prev);
            assertTrue(t != prev, "two identical regions in a row at sector " + id);
            assertEquals(t, RegionTheme.pick(99L, id, prev), "the draw is deterministic");
            seen.add(t);
            prev = t;
        }
        assertEquals(RegionTheme.values().length, seen.size(), "every theme turns up in 400 sectors");
    }

    @Test
    public void relicCountStaysInTheThemeRange() {
        for (RegionTheme t : RegionTheme.values()) {
            assertTrue(t.minRelics() >= 1 && t.maxRelics() <= 3 && t.minRelics() <= t.maxRelics(), t + " range");
            for (int id = 0; id < 300; id++) {
                int n = t.relicCount(7L, id);
                assertTrue(n >= t.minRelics() && n <= t.maxRelics(), t + " sector " + id + " gave " + n);
                assertEquals(n, t.relicCount(7L, id));
            }
        }
    }

    @Test
    public void themeIdsParseLeniently() {
        assertEquals(RegionTheme.BLOOD, RegionTheme.fromId("blood"));
        assertEquals(RegionTheme.BLOOD, RegionTheme.fromId("BLOOD"));
        assertEquals(RegionTheme.GRAVITY, RegionTheme.fromId("nonsense"), "an old world with an unknown theme must still load");
        assertEquals(RegionTheme.GRAVITY, RegionTheme.fromId(null));
        for (RegionTheme t : RegionTheme.values()) assertEquals(t, RegionTheme.fromId(t.id()));
    }
}
