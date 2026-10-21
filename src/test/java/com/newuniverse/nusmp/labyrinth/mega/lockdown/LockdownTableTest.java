package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the room data that flags a challenge room is hand-edited and a game nobody can run here will not tell the owner that a typo turned a lock off, so these tests pin
 * the lookup order (exact template, name pattern, kind, nothing), that every kind of mistake in a file is a line of text and costs only itself, that nothing a file
 * contains can throw, and that the SHIPPED file parses without a single problem and says what the doc says about the real templates of the dungeon.
 */
class LockdownTableTest {
    private static final String FILE = "src/main/resources/data/nusmp/mega/lockdown/rooms.json";

    private static LockdownTable shipped() throws Exception {
        LockdownTable.Parsed p = LockdownTable.parse(FILE, Files.readString(Path.of(FILE), StandardCharsets.UTF_8));
        assertTrue(p.clean(), "the shipped room data has problems: " + p.problems());
        return p.table();
    }

    @Test
    void theShippedFileIsCleanAndFlagsTheKeyChallengeRooms() throws Exception {
        LockdownTable t = shipped();
        assertFalse(t.isEmpty());
        assertTrue(t.ruleFor("nusmp:mega/treasure/holy_church_reliquary", RoomKind.TREASURE).lock(), "a treasure room locks");
        assertTrue(t.ruleFor("nusmp:mega/set/cathedral_rotunda", RoomKind.SET_PIECE).lock(), "a set piece locks");
        assertEquals(Band.HARD, t.ruleFor("nusmp:mega/set/cathedral_rotunda", RoomKind.SET_PIECE).band());
        assertEquals(2, t.ruleFor("nusmp:mega/set/cathedral_rotunda", RoomKind.SET_PIECE).waves());
        assertTrue(t.ruleFor("nusmp:mega/hall/barracks_a", RoomKind.HALL).lock(), "a barracks always locks");
        assertEquals(1.0, t.ruleFor("nusmp:mega/hall/barracks_a", RoomKind.HALL).chance());
        LockdownRule hall = t.ruleFor("nusmp:mega/hall/library_a", RoomKind.HALL);
        assertTrue(hall.lock() && hall.chance() < 1.0, "an ordinary hall locks for a share of its rooms only");
    }

    @Test
    void roomsThatHaveTheirOwnChallengeOrNoWallNeverLock() throws Exception {
        LockdownTable t = shipped();
        assertFalse(t.ruleFor("nusmp:mega/trial/trial_hall_a", RoomKind.TRIAL).lock(), "a trial hall seals itself with its Sealed Archway");
        assertFalse(t.ruleFor("nusmp:mega/puzzle/sigil_hall_a", RoomKind.PUZZLE).lock(), "a puzzle room's mechanic bars its doors");
        assertFalse(t.ruleFor("nusmp:mega/trap/pit_hall_a", RoomKind.TRAP).lock());
        assertFalse(t.ruleFor("nusmp:mega/cavern/crystal_cavern", RoomKind.CAVERN).lock(), "a cavern has no wall to close");
        for (RoomKind k : new RoomKind[] {RoomKind.LANDING, RoomKind.SAFE, RoomKind.STAIRWELL, RoomKind.ARENA, RoomKind.ANTECHAMBER, RoomKind.VAULT, RoomKind.CORRIDOR, RoomKind.JUNCTION, RoomKind.LINK, RoomKind.ONWARD}) {
            assertFalse(t.ruleFor("nusmp:mega/" + k.folder() + "/x", k).lock(), k + " must never lock");
        }
    }

    @Test
    void everyRoomKindIsDecidedByTheShippedFile() throws Exception {
        LockdownTable t = shipped();
        for (RoomKind k : RoomKind.values()) {
            if (k == RoomKind.CAP || k == RoomKind.OTHER) continue;                    // never stored as rooms / no folder
            assertTrue(t.kinds().containsKey(LockdownTable.kindKey(k)), "the room data says nothing about the kind " + k + ": decide it");
        }
    }

    @Test
    void theLookupOrderIsTemplatePatternKindNothing() {
        String json = """
                {"kinds": {"hall": {"lock": true, "band": "minor"}},
                 "patterns": [{"match": "forge", "band": "hard", "waves": 2}],
                 "templates": {"nusmp:mega/hall/forge_a": {"lock": false}}}
                """;
        LockdownTable.Parsed p = LockdownTable.parse("t", json);
        assertTrue(p.clean(), String.valueOf(p.problems()));
        LockdownTable t = p.table();
        assertFalse(t.ruleFor("nusmp:mega/hall/forge_a", RoomKind.HALL).lock(), "the exact template wins, and may opt a room out");
        assertEquals(Band.HARD, t.ruleFor("nusmp:mega/hall/forge_b", RoomKind.HALL).band(), "a name pattern beats the kind");
        assertEquals(Band.MINOR, t.ruleFor("nusmp:mega/hall/library_a", RoomKind.HALL).band(), "the kind answers when nothing more specific does");
        assertFalse(t.ruleFor("nusmp:mega/trap/pit", RoomKind.TRAP).lock(), "a room nothing mentions does not lock");
        assertEquals(t.ruleFor("mega/hall/forge_b", RoomKind.HALL), t.ruleFor("NUSMP:mega/hall/forge_b", RoomKind.HALL), "the namespace and the case do not matter");
    }

    @Test
    void aBadFileIsReportedInWordsAndNeverThrows() {
        assertFalse(LockdownTable.parse("x", "not json").clean());
        assertFalse(LockdownTable.parse("x", "[1,2]").clean());
        assertTrue(LockdownTable.parse("x", "{}").clean());
        assertTrue(LockdownTable.parse("x", null).table().isEmpty());
        LockdownTable.Parsed p = LockdownTable.parse("x", """
                {"surprise": 1,
                 "kinds": {"hrad": {"lock": true}, "hall": {"band": "hrad", "waves": 9, "chance": 4, "unknown": 1}},
                 "templates": {"nusmp:structure/x": {}},
                 "patterns": [{"match": "ab"}, 7]}
                """);
        String all = String.join("\n", p.problems());
        assertTrue(all.contains("unknown key 'surprise'"), all);
        assertTrue(all.contains("kinds.hrad is not a room kind"), all);
        assertTrue(all.contains("band must be"), all);
        assertTrue(all.contains("'waves' 9.0 is outside"), all);
        assertTrue(all.contains("'chance' 4.0 is outside"), all);
        assertTrue(all.contains("unknown key 'unknown'"), all);
        assertTrue(all.contains("not a mega template id"), all);
        assertTrue(all.contains("at least three characters"), all);
        assertTrue(all.contains("patterns[1] is not an object"), all);
        LockdownRule hall = p.table().ruleFor("nusmp:mega/hall/x", RoomKind.HALL);
        assertEquals(LockdownRule.MAX_WAVES, hall.waves(), "a bad value is clamped, the entry is kept");
        assertEquals(1.0, hall.chance());
        assertEquals(Band.STANDARD, hall.band(), "a bad band falls back to the default");
    }

    @Test
    void aRuleCannotHoldAnImpossibleValue() {
        LockdownRule r = new LockdownRule(true, 7.0, Band.NOTHING, 0, -5, -5, null);
        assertEquals(1.0, r.chance());
        assertEquals(Band.STANDARD, r.band(), "NOTHING would make a wave of nothing");
        assertEquals(1, r.waves());
        assertEquals(1, r.minFloor());
        assertEquals(0, r.minArea());
        assertEquals("", r.note());
        assertEquals(LockdownRule.MAX_WAVES, r.withWaves(99).waves());
        assertEquals(Band.HARD, r.withBand(Band.HARD).band());
    }
}
