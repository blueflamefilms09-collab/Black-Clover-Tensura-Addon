package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Dungeon Master and the wandering bosses must agree on which rooms are left alone, and the rule is one line that is easy to break by adding a room
 * kind; these tests pin every kind of the contract to its answer.
 */
class SanctuaryTest {
    @Test
    void restRoomsAndTheBossChainAreOffLimits() {
        assertTrue(Sanctuary.is(RoomKind.LANDING));
        assertTrue(Sanctuary.is(RoomKind.SAFE));
        assertTrue(Sanctuary.is(RoomKind.ARENA));
        assertTrue(Sanctuary.is(RoomKind.ANTECHAMBER));
        assertTrue(Sanctuary.is(RoomKind.VAULT));
    }

    @Test
    void everyOtherKindIsOrdinaryDungeon() {
        for (RoomKind k : RoomKind.values()) {
            boolean off = k == RoomKind.LANDING || k == RoomKind.SAFE || k == RoomKind.ARENA || k == RoomKind.ANTECHAMBER || k == RoomKind.VAULT;
            assertEquals(off, Sanctuary.is(k), k.name());
        }
        assertFalse(Sanctuary.is(null));
    }
}
