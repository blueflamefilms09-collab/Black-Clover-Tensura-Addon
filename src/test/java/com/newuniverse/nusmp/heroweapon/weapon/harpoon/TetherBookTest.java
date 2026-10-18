package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Why: every follow-up technique of the Harpoon acts on the creature the line is tied to, so the line's rules are proved exactly: one tie at a time, a fixed life, a capped extension from melee blows, and a clean cut. */
class TetherBookTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes());

    @Test
    void aFreshTieHoldsForItsLifeThenLetsGo() {
        TetherBook t = new TetherBook();
        assertNull(t.target(0));
        t.tie(A, 100);
        assertEquals(A, t.target(100));
        assertEquals(A, t.target(259));
        assertNull(t.target(260));
        assertEquals(60L, t.left(200));
        assertEquals(0L, t.left(260));
    }

    @Test
    void aNewThrowReplacesTheOldTie() {
        TetherBook t = new TetherBook();
        t.tie(A, 0);
        t.tie(B, 50);
        assertEquals(B, t.target(60));
        assertNull(t.target(300));
    }

    @Test
    void aMeleeBlowOnTheTiedCreatureAddsFortyTicks() {
        TetherBook t = new TetherBook();
        t.tie(A, 0);
        assertTrue(t.extend(100, A));
        assertEquals(100L, t.left(100), "160 + 40 minus the 100 spent");
    }

    @Test
    void aBlowOnSomeoneElseOrAfterTheTieEndedChangesNothing() {
        TetherBook t = new TetherBook();
        t.tie(A, 0);
        assertFalse(t.extend(10, B));
        assertFalse(t.extend(200, A), "the tie ran out at 160");
        assertNull(t.target(200));
    }

    @Test
    void theExtensionNeverPassesTheCeilingMeasuredFromTheThrow() {
        TetherBook t = new TetherBook();
        t.tie(A, 0);
        for (int now = 10; now < 240; now += 10) t.extend(now, A);
        assertEquals(A, t.target(239));
        assertEquals(1L, t.left(239));
        assertNull(t.target(240), "240 ticks after the throw is the hard end");
        assertFalse(t.extend(239, A), "nothing more can be added at the ceiling");
    }

    @Test
    void cuttingTheLineEndsItAtOnce() {
        TetherBook t = new TetherBook();
        t.tie(A, 0);
        t.cut();
        assertNull(t.target(1));
        assertFalse(t.extend(1, A));
    }

    @Test
    void theDrawingBeatFiresOnceEveryTwenty() {
        TetherBook t = new TetherBook();
        assertTrue(t.drawDue(500, 20));
        assertFalse(t.drawDue(515, 20));
        assertTrue(t.drawDue(520, 20));
    }
}
