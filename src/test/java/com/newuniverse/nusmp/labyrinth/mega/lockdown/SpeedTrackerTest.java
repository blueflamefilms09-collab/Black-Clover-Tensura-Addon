package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "faster than a speed threshold" must not be fooled in either direction. A sprint (5.6 blocks a second) and a sprint jump (7) are not running past; Speed II
 * (about 8) is borderline and a fast mount is over; a teleport is a huge step that is NOT speed, and must not read as a ten-second sprint afterwards; a player who has
 * just appeared has no speed yet. The samples are one every ten ticks, as the director takes them.
 */
class SpeedTrackerTest {
    private static final double TELE = 24.0;

    /** Walks along x at {@code bps} blocks a second for {@code seconds}, sampling every ten ticks, starting at tick 0. Returns the tracker. */
    private static SpeedTracker walk(double bps, int seconds) {
        SpeedTracker t = new SpeedTracker();
        for (int tick = 0; tick <= seconds * 20; tick += 10) t.add(tick, bps * tick / 20.0, 0.0, TELE);
        return t;
    }

    @Test
    void aSprintAndASprintJumpAreNotRunningPast() {
        assertEquals(5.6, walk(5.6, 4).speed(80, 60), 0.05);
        assertTrue(walk(7.0, 4).speed(80, 60) < 8.5);
    }

    @Test
    void aFastMountIsOverTheLimit() {
        assertTrue(walk(12.0, 4).speed(80, 60) > 8.5);
        assertTrue(walk(8.0, 4).speed(80, 60) < 8.5, "Speed II is not enough");
    }

    @Test
    void theSpeedIsHorizontalOnly() {
        SpeedTracker t = new SpeedTracker();
        for (int tick = 0; tick <= 80; tick += 10) t.add(tick, 0.0, 0.0, TELE);              // standing still, however far it falls: the tracker is only given x and z
        assertEquals(0.0, t.speed(80, 60));
    }

    @Test
    void aTeleportIsNotSpeedAndTheWindowStartsAgain() {
        SpeedTracker t = new SpeedTracker();
        for (int tick = 0; tick <= 40; tick += 10) t.add(tick, tick / 4.0, 0.0, TELE);
        assertEquals(SpeedTracker.Move.TELEPORT, t.add(50, 500.0, 0.0, TELE));
        assertTrue(t.lastStep() > 400.0);
        assertEquals(0.0, t.speed(50, 60), "right after a teleport there is no speed yet");
        for (int tick = 60; tick <= 120; tick += 10) t.add(tick, 500.0 + (tick - 50) / 4.0, 0.0, TELE);
        assertTrue(t.speed(120, 60) < 6.0, "a walk after a teleport is a walk: " + t.speed(120, 60));
    }

    @Test
    void aPlayerWhoJustAppearedHasNoSpeed() {
        SpeedTracker t = new SpeedTracker();
        assertEquals(SpeedTracker.Move.FIRST, t.add(0, 0, 0, TELE));
        assertEquals(0.0, t.speed(0, 60));
        t.add(10, 20.0, 0.0, TELE);
        assertEquals(0.0, t.speed(10, 60), "half a second is less than 60 percent of a three-second window");
    }

    @Test
    void oldSamplesLeaveTheWindow() {
        SpeedTracker t = walk(12.0, 4);
        for (int tick = 90; tick <= 300; tick += 10) t.add(tick, 48.0, 0.0, TELE);            // then stands still for ten seconds
        assertEquals(0.0, t.speed(300, 60), 1e-9);
    }

    @Test
    void theTrackerIsBoundedAndCanBeCleared() {
        SpeedTracker t = new SpeedTracker();
        for (int tick = 0; tick < 10_000; tick += 10) t.add(tick, 0.0, 0.0, TELE);
        assertTrue(t.size() <= 64);
        t.clear();
        assertEquals(0, t.size());
        assertEquals(SpeedTracker.Move.FIRST, t.add(5, 1, 1, TELE));
    }

    @Test
    void twoSamplesInTheSameTickAreOne() {
        SpeedTracker t = new SpeedTracker();
        t.add(10, 0, 0, TELE);
        assertEquals(SpeedTracker.Move.WALK, t.add(10, 50, 0, TELE), "a second sample in the same tick tells nothing and is not a teleport");
        assertEquals(1, t.size());
    }
}
