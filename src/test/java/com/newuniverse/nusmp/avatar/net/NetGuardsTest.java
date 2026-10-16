package com.newuniverse.nusmp.avatar.net;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** What the server does to untrusted input before reading it: the roll rate limit and the string cleaning. */
class NetGuardsTest {
    @Test
    void oneRollPerGapPerPlayer() {
        RollLimiter l = new RollLimiter();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(l.allow(a, 1000, 500));
        assertFalse(l.allow(a, 1200, 500), "too soon");
        assertTrue(l.allow(b, 1200, 500), "another player is not limited by the first");
        assertTrue(l.allow(a, 1500, 500));
        assertFalse(l.allow(a, 1999, 500));
        assertTrue(l.allow(a, 2000, 500));
    }

    @Test
    void aZeroGapNeverLimits() {
        RollLimiter l = new RollLimiter();
        UUID a = UUID.randomUUID();
        for (int i = 0; i < 10; i++) assertTrue(l.allow(a, 5, 0));
    }

    @Test
    void forgettingAPlayerResetsTheirLimit() {
        RollLimiter l = new RollLimiter();
        UUID a = UUID.randomUUID();
        assertTrue(l.allow(a, 10, 500));
        l.forget(a);
        assertTrue(l.allow(a, 11, 500));
    }

    @Test
    void textIsCutAndStrippedOfControlCharacters() {
        assertEquals("", NetText.clean(null, 10));
        assertEquals("abc", NetText.clean("a\u0000b\nc", 10));
        assertEquals("abcde", NetText.clean("abcdefgh", 5));
        assertEquals("ab", NetText.clean("a§b", 10), "the section sign (formatting) is dropped");
        assertEquals(NetText.NAME_MAX, NetText.clean("x".repeat(1000), NetText.NAME_MAX).length());
    }
}
