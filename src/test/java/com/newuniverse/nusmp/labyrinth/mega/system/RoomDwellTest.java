package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a room counts for a clearing quest only after the player really stood in it for 40 ticks, offered once, re-offered on request, and reset by leaving. A
 * timer that offered a room twice would pay a room twice; one that never reset would count a room the player ran through. Pinned here without a game.
 */
class RoomDwellTest {
    @Test void aRoomIsOfferedOnceAfterFortyTicks() {
        RoomDwell d = new RoomDwell();
        for (int i = 0; i < 7; i++) assertEquals(RoomDwell.NONE, d.step(12, 5), "tick " + i * 5);
        assertEquals(12, d.step(12, 5), "the eighth step reaches 40");
        for (int i = 0; i < 20; i++) assertEquals(RoomDwell.NONE, d.step(12, 5), "never offered twice");
    }

    @Test void leavingTheRoomStartsTheTimerAgain() {
        RoomDwell d = new RoomDwell();
        d.step(12, 30);
        assertEquals(RoomDwell.NONE, d.step(13, 30), "another room: the 30 ticks in room 12 do not carry over");
        assertEquals(13, d.step(13, 10));
        assertEquals(RoomDwell.NONE, d.step(RoomDwell.NONE, 100), "a place that does not count");
        assertEquals(RoomDwell.NONE, d.step(13, 30), "back in 13: starts from zero");
        assertEquals(13, d.step(13, 10));
    }

    @Test void aRoomCanBeReofferedAfterAShortWait() {
        RoomDwell d = new RoomDwell();
        assertEquals(5, d.step(5, 40));
        d.retry(20);
        assertEquals(RoomDwell.NONE, d.step(5, 10));
        assertEquals(5, d.step(5, 10), "20 ticks after the retry");
        d.retry(1);
        assertEquals(5, d.step(5, 1));
    }

    @Test void resetForgetsTheRoom() {
        RoomDwell d = new RoomDwell();
        d.step(9, 25);
        d.reset();
        assertEquals(RoomDwell.NONE, d.room());
        assertEquals(0, d.ticks());
        assertEquals(RoomDwell.NONE, d.step(9, 25));
        assertEquals(9, d.step(9, 15));
    }

    @Test void aNegativeStepNeverRunsTheTimerBackwards() {
        RoomDwell d = new RoomDwell();
        d.step(1, 20);
        d.step(1, -50);
        assertEquals(20, d.ticks());
    }
}
