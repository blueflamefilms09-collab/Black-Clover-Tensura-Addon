package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the room assignment must be the same every time a sector is rebuilt and must follow the rules of the design: no native faction in a safe
 * room, a Landing, a corridor, a junction, a link or the Way Onward; the template's own marker wins when it names a real faction; otherwise a seeded
 * theme-weighted draw; an unknown marker falls back to the draw; nothing is assigned when no faction is loaded.
 */
class NativeFactionsTest {
    private static final FactionGraph G = FactionGraph.of(List.of(Fx.def("undead", List.of(), List.of()), Fx.def("goblin", List.of(), List.of()), Fx.def("slime", List.of(), List.of())));

    @Test void safeRoomsAndCirculationHaveNoNativeFaction() {
        for (RoomKind k : new RoomKind[] {RoomKind.LANDING, RoomKind.SAFE, RoomKind.CORRIDOR, RoomKind.JUNCTION, RoomKind.LINK, RoomKind.ONWARD, RoomKind.CAP})
            assertFalse(NativeFactions.eligible(k), k + " must not get a native faction");
        assertFalse(NativeFactions.eligible(null));
    }

    @Test void everyOtherKindIsEligible() {
        for (RoomKind k : new RoomKind[] {RoomKind.HALL, RoomKind.SET_PIECE, RoomKind.CAVERN, RoomKind.TRIAL, RoomKind.TRAP, RoomKind.PUZZLE, RoomKind.TREASURE,
                RoomKind.ARENA, RoomKind.ANTECHAMBER, RoomKind.VAULT, RoomKind.OTHER})
            assertTrue(NativeFactions.eligible(k), k + " should get a native faction");
    }

    @Test void theTemplatesMarkerWinsWhenItNamesARealFaction() {
        for (long seed = 0; seed < 100; seed++) assertEquals("slime", NativeFactions.choose(G, seed, (int) seed, "gravity", "slime"));
        assertEquals("goblin", NativeFactions.choose(G, 5L, 3, "blood", "  Goblin "), "a marker is trimmed and case-insensitive");
    }

    @Test void anUnknownMarkerFallsBackToTheDraw() {
        for (int room = 0; room < 50; room++) {
            String f = NativeFactions.choose(G, 99L, room, "gravity", "dragon");
            assertTrue(G.knows(f));
            assertEquals(G.pickNative(99L ^ room, "gravity"), f, "the draw uses sectorSeed ^ roomId");
        }
        assertEquals(G.pickNative(7L ^ 4, "frost"), NativeFactions.choose(G, 7L, 4, "frost", null));
    }

    @Test void theSameSectorGivesTheSameRoomsEveryTime() {
        for (int room = 0; room < 80; room++) assertEquals(NativeFactions.choose(G, 1234567L, room, "shadow", null), NativeFactions.choose(G, 1234567L, room, "shadow", null));
    }

    @Test void aSectorMixesFactionsAcrossItsRooms() {
        Set<String> seen = new HashSet<>();
        for (int room = 0; room < 40; room++) seen.add(NativeFactions.choose(G, 424242L, room, "gravity", null));
        assertEquals(3, seen.size(), "forty rooms of three equal factions use all three: " + seen);
    }

    @Test void withNoFactionsNothingIsAssigned() {
        assertNull(NativeFactions.choose(FactionGraph.EMPTY, 1L, 1, "gravity", "undead"));
        assertNull(NativeFactions.choose(null, 1L, 1, "gravity", null));
    }
}
