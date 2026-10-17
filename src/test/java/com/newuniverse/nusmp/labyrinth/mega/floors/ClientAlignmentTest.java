package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the client holder is read by whatever draws a gate hint or a window line, so it must answer "free" until the server has spoken, must never show a
 * half-updated picture, and must treat a damaged packet the way a damaged save is treated. The tests pin those three promises and the logout reset, and leave
 * the holder cleared (it is static state shared by every test of the run).
 */
class ClientAlignmentTest {

    @Test void beforeTheFirstPacketEveryRoadIsFree() {
        ClientAlignment.clear();
        assertFalse(ClientAlignment.synced());
        assertNull(ClientAlignment.chosen());
        assertFalse(ClientAlignment.locked());
        assertTrue(ClientAlignment.mayEnter(Polarity.HOLY));
        assertTrue(ClientAlignment.mayEnter(Polarity.ABYSSAL));
        assertEquals(0, ClientAlignment.deepest(Polarity.HOLY));
        assertSame(ClientAlignment.Snapshot.EMPTY, ClientAlignment.snapshot());
    }

    @Test void aPacketReplacesThePictureInOneStep() {
        ClientAlignment.receive("holy", true, 12, 3);
        ClientAlignment.Snapshot s = ClientAlignment.snapshot();
        assertTrue(s.synced());
        assertEquals(Polarity.HOLY, s.chosen());
        assertTrue(s.locked());
        assertEquals(12, s.deepestHoly());
        assertEquals(3, s.deepestAbyssal());
        assertEquals(12, ClientAlignment.deepest(Polarity.HOLY));
        assertEquals(3, ClientAlignment.deepest(Polarity.ABYSSAL));
        assertEquals(0, ClientAlignment.deepest(null));
        assertTrue(ClientAlignment.mayEnter(Polarity.HOLY), "their own road");
        assertFalse(ClientAlignment.mayEnter(Polarity.ABYSSAL), "the other one is shut");
        assertTrue(ClientAlignment.mayEnter(null), "Floor 0 is not a road");
        ClientAlignment.receive("", false, 0, 0);
        assertTrue(ClientAlignment.synced(), "an empty picture is still a picture");
        assertTrue(ClientAlignment.mayEnter(Polarity.ABYSSAL));
        ClientAlignment.clear();
    }

    @Test void aDamagedPacketIsRepairedLikeADamagedSave() {
        ClientAlignment.receive("sideways", true, -4, -9);
        ClientAlignment.Snapshot s = ClientAlignment.snapshot();
        assertNull(s.chosen());
        assertFalse(s.locked(), "a lock without a road is no lock");
        assertEquals(0, s.deepestHoly());
        assertEquals(0, s.deepestAbyssal());
        assertTrue(ClientAlignment.mayEnter(Polarity.HOLY));
        ClientAlignment.clear();
    }

    @Test void logoutForgetsTheServer() {
        ClientAlignment.receive("abyssal", true, 1, 5);
        ClientAlignment.clear();
        assertFalse(ClientAlignment.synced());
        assertNull(ClientAlignment.chosen());
        assertTrue(ClientAlignment.mayEnter(Polarity.HOLY));
    }
}
