package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Why: the per-wielder state of the weapons must be made once per wielder, be the same object on the next call, be forgettable, and be bounded, because a
 * map that grows with every player who ever held a weapon on a long-lived server is a leak.
 */
class PerPlayerTest {
    @Test
    void oneStatePerWielderMadeOnFirstUse() {
        AtomicInteger made = new AtomicInteger();
        PerPlayer<int[]> p = new PerPlayer<>(() -> new int[] {made.incrementAndGet()}, 4);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertNull(p.peek(a));
        int[] sa = p.of(a);
        assertSame(sa, p.of(a));
        assertNotNull(p.peek(a));
        p.of(b);
        assertEquals(2, made.get());
        assertEquals(2, p.size());
    }

    @Test
    void forgetClearAndTheCap() {
        PerPlayer<String> p = new PerPlayer<>(() -> "s", 2);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        p.of(a);
        p.of(b);
        p.of(c);
        assertEquals(2, p.size(), "the cap holds");
        assertNull(p.peek(a), "the oldest wielder went");
        assertNotNull(p.peek(c));
        p.forget(c);
        assertNull(p.peek(c));
        p.clear();
        assertEquals(0, p.size());
    }
}
