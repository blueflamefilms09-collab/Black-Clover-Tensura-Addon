package com.newuniverse.nusmp.labyrinth.trap;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.labyrinth.trap.TrapMath.PitAction.*;
import static org.junit.jupiter.api.Assertions.*;

/** The pitfall's machine and the snuffer glyph's distance, refresh and refusal rules. */
class TrapMathTest {
    @Test
    void onlyASurvivalPlayerSpringsAPitfall() {
        assertTrue(TrapMath.springs(true, false, false, true));
        assertFalse(TrapMath.springs(false, false, false, true), "a mob walking over the floor never springs it");
        assertFalse(TrapMath.springs(true, true, false, true), "creative");
        assertFalse(TrapMath.springs(true, false, true, true), "spectator");
        assertFalse(TrapMath.springs(true, false, false, false), "traps switched off");
    }

    @Test
    void aStepArmsOnlyAClosedUnsafeTile() {
        assertTrue(TrapMath.stepArms(false, false, true));
        assertFalse(TrapMath.stepArms(true, false, true), "the safe route never gives way");
        assertFalse(TrapMath.stepArms(false, true, true), "an open tile is already gone");
        assertFalse(TrapMath.stepArms(false, false, false), "nobody who may spring it");
    }

    @Test
    void aTickOpensACrackedTileAndClosesAnEmptyHole() {
        assertEquals(OPEN, TrapMath.onTick(false, false, false), "the crack delay ran out: the floor opens");
        assertEquals(WAIT, TrapMath.onTick(false, true, true), "somebody is still in the hole: wait");
        assertEquals(CLOSE, TrapMath.onTick(false, true, false), "empty: the floor comes back");
    }

    @Test
    void aStaleTickOnASafeTileDoesNothingAndSchedulesNothing() {
        assertEquals(NONE, TrapMath.onTick(true, false, false));
        assertEquals(0, TrapMath.nextDelay(NONE));
    }

    @Test
    void theDelaysAreTheNamedConstants() {
        assertEquals(TrapMath.OPEN_TICKS, TrapMath.nextDelay(OPEN));
        assertEquals(TrapMath.RETRY_TICKS, TrapMath.nextDelay(WAIT));
        assertEquals(0, TrapMath.nextDelay(CLOSE));
        assertTrue(TrapMath.CRACK_TICKS > 0 && TrapMath.CRACK_TICKS < TrapMath.RETRY_TICKS);
    }

    @Test
    void aFullPitCycleOpensWaitsAndCloses() {
        boolean open = false;
        assertEquals(OPEN, TrapMath.onTick(false, open, false));
        open = true;
        assertEquals(WAIT, TrapMath.onTick(false, open, true));
        assertEquals(WAIT, TrapMath.onTick(false, open, true));
        assertEquals(CLOSE, TrapMath.onTick(false, open, false));
    }

    @Test
    void theDarknessSphereIsSquaredAndInclusive() {
        assertTrue(TrapMath.within(6, 0, 0, TrapMath.DARKNESS_RADIUS));
        assertTrue(TrapMath.within(3, 3, 3, TrapMath.DARKNESS_RADIUS), "5.196 away");
        assertFalse(TrapMath.within(4, 4, 4, TrapMath.DARKNESS_RADIUS), "6.93 away");
        assertFalse(TrapMath.within(0, 6.01, 0, TrapMath.DARKNESS_RADIUS));
        assertTrue(TrapMath.within(0, 0, 0, 0));
    }

    @Test
    void darknessIsOnlyToppedUpWhenItIsAboutToFade() {
        assertTrue(TrapMath.needsRefresh(0), "none at all");
        assertTrue(TrapMath.needsRefresh(-1));
        assertTrue(TrapMath.needsRefresh(TrapMath.DARKNESS_TICKS / 2 - 1));
        assertFalse(TrapMath.needsRefresh(TrapMath.DARKNESS_TICKS / 2), "still fresh: re-applying it would make the screen pulse");
        assertFalse(TrapMath.needsRefresh(TrapMath.DARKNESS_TICKS));
    }

    @Test
    void aGlyphRefusesALightFromASurvivalPlayerOnly() {
        assertTrue(TrapMath.refusesLight(14, false, false, true), "a torch");
        assertTrue(TrapMath.refusesLight(1, false, false, true), "any light at all");
        assertFalse(TrapMath.refusesLight(0, false, false, true), "a plain block is fine");
        assertFalse(TrapMath.refusesLight(15, true, false, true), "a creative builder keeps building");
        assertFalse(TrapMath.refusesLight(15, false, true, true));
        assertFalse(TrapMath.refusesLight(15, false, false, false), "traps switched off");
    }

    @Test
    void theNoLightZoneIsEightBlocks() {
        assertTrue(TrapMath.inNoLightZone(8, 0, 0));
        assertFalse(TrapMath.inNoLightZone(9, 0, 0));
        assertFalse(TrapMath.inNoLightZone(6, 6, 0), "8.49 away");
        assertTrue(TrapMath.SNUFF_RADIUS < TrapMath.PLACE_RADIUS && TrapMath.DARKNESS_RADIUS < TrapMath.PLACE_RADIUS);
    }
}
