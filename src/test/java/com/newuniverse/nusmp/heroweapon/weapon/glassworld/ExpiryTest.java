package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: every timed effect of the four weapons (a veil, a floating scythe, a parry window, a mark that must not be repeated) rests on this table, so it is shown to
 * start, run out exactly on the tick, extend without shortening, forget, prune what has passed, and never hold more than its cap.
 */
class ExpiryTest {
    @Test
    void anEntryIsActiveUntilItsTickAndNotAfter() {
        Expiry<String> e = new Expiry<>(8);
        assertFalse(e.active("a", 0));
        e.set("a", 100);
        assertTrue(e.active("a", 0));
        assertTrue(e.active("a", 99));
        assertFalse(e.active("a", 100), "exclusive end");
        assertFalse(e.active("a", 500));
        assertEquals(40L, e.remaining("a", 60));
        assertEquals(0L, e.remaining("a", 100));
        assertEquals(0L, e.remaining("b", 0));
    }

    @Test
    void setReplacesAndExtendNeverShortens() {
        Expiry<String> e = new Expiry<>(8);
        e.set("a", 100);
        e.set("a", 50);
        assertEquals(50L, e.remaining("a", 0), "set replaces");
        e.extend("a", 30);
        assertEquals(50L, e.remaining("a", 0), "extend keeps the later time");
        e.extend("a", 90);
        assertEquals(90L, e.remaining("a", 0));
        e.extend("new", 10);
        assertTrue(e.active("new", 5));
    }

    @Test
    void clearPruneAndTheCap() {
        Expiry<UUID> e = new Expiry<>(3);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        e.set(a, 10);
        e.set(b, 20);
        e.set(c, 30);
        assertEquals(3, e.size());
        e.set(d, 40);
        assertEquals(3, e.size(), "the cap holds");
        assertFalse(e.active(a, 0), "the entry that expires first was dropped");
        assertTrue(e.active(b, 0) && e.active(c, 0) && e.active(d, 0));
        e.set(b, 25);
        assertEquals(3, e.size(), "setting a held key does not evict");
        assertEquals(1, e.prune(26), "one entry has passed");
        assertFalse(e.active(b, 0));
        e.clear(c);
        assertFalse(e.active(c, 0));
        e.clearAll();
        assertEquals(0, e.size());
        e.set(null, 5);
        assertEquals(0, e.size(), "a null key is ignored");
    }
}
