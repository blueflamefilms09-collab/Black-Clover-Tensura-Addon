package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The break counter of a long cast: armed, fed, says once that it broke. */
class ChannelGuardTest {
    @Test
    void itBreaksExactlyOnceWhenTheThresholdIsCrossed() {
        ChannelGuard g = new ChannelGuard();
        g.arm(100);
        assertFalse(g.hit(40));
        assertEquals(0.4, g.progress(), 1e-9);
        assertFalse(g.hit(59.9));
        assertTrue(g.hit(0.2), "the blow that crosses the line");
        assertTrue(g.broken());
        assertFalse(g.hit(500), "it breaks once, not once per blow");
        assertEquals(1.0, g.progress(), 1e-9);
    }

    @Test
    void itDoesNothingUntilArmedAndForgetsWhenDisarmed() {
        ChannelGuard g = new ChannelGuard();
        assertFalse(g.hit(1000), "not armed: the blows of an ordinary fight do not count");
        g.arm(10);
        g.hit(6);
        g.disarm();
        assertFalse(g.armed());
        assertFalse(g.hit(1000));
        g.arm(10);
        assertFalse(g.hit(6), "arming again starts from nothing");
        assertFalse(g.hit(-50), "a blow of no size is ignored");
        assertFalse(g.hit(0));
        assertTrue(g.hit(4));
    }

    @Test
    void theThresholdIsAShareOfHealth() {
        assertEquals(156.0, ChannelGuard.threshold(1300, 0.12), 1e-9);
        assertEquals(1.0, ChannelGuard.threshold(0, 0.12), 1e-9, "never zero");
    }
}
