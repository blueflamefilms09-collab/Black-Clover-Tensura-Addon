package com.newuniverse.nusmp.relics.djinn.weapon;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActClockTest {
    @Test
    void anActIsReadyUntilStartedAndThenWaits() {
        ActClock c = new ActClock();
        assertTrue(c.ready(0, 100));
        c.start(0, 100, 120);
        assertEquals(120, c.left(0, 100));
        assertEquals(1, c.left(0, 219));
        assertTrue(c.ready(0, 220));
        assertTrue(c.ready(1, 100), "the other act is on its own clock");
    }

    @Test
    void aClockThatWentBackwardsDoesNotLockTheWeapon() {
        ActClock c = new ActClock();
        c.start(1, 1_000_000, 260);
        assertEquals(0, c.left(1, 10), "the level's time was set back by a million ticks");
        c.start(0, 50, 99999);
        assertEquals(ActClock.MAX_COOLDOWN, c.left(0, 50), "a cooldown is clamped");
    }

    @Test
    void marksExpireStackNeverWeakenAndAreCapped() {
        WeaponMarks m = new WeaponMarks();
        UUID a = new UUID(1, 1);
        m.mark(a, 0, 100, 0.25);
        assertEquals(0.25, m.bonus(a, 99), 1e-12);
        assertEquals(0.0, m.bonus(a, 100), 1e-12);
        m.mark(a, 200, 100, 0.25);
        m.mark(a, 210, 100, 0.10);
        assertEquals(0.25, m.bonus(a, 220), 1e-12, "a weaker mark never replaces a stronger running one");
        for (int i = 0; i < 500; i++) m.mark(new UUID(2, i), 300, 100 + i, 0.2);
        assertTrue(m.size() <= WeaponMarks.CAP);
        m.prune(100000);
        assertEquals(0, m.size());
        m.mark(a, 0, 0, 0.5);
        m.mark(a, 0, 10, 0.0);
        assertEquals(0, m.size(), "nothing without time or strength");
    }
}
