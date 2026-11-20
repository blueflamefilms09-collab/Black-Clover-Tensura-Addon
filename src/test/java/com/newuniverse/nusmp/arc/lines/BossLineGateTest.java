package com.newuniverse.nusmp.arc.lines;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BossLineGateTest {
    @Test
    void aGroupRestsAndTheGapHoldsBackTwoLines() {
        BossLineGate g = new BossLineGate();
        UUID b = UUID.randomUUID();
        assertTrue(g.allow(b, "intro", 0));
        assertTrue(g.allow(b, "intro", 1), "intro has no rest");
        assertFalse(g.allow(b, "hurt_big", 10), "a normal line right after another is held back by the gap");
        assertTrue(g.allow(b, "hurt_big", 50));
        assertFalse(g.allow(b, "dodge", 60), "within the gap of the last line");
        assertTrue(g.allow(b, "dodge", 50 + BossLineGate.MIN_GAP));
        assertFalse(g.allow(b, "dodge", 50 + BossLineGate.MIN_GAP + 5), "the group rests");
        assertTrue(g.allow(b, "form_ssj", 50 + BossLineGate.MIN_GAP + 6), "a form line is never held back by the gap");
    }

    @Test
    void rotationCountsPerGroup() {
        BossLineGate g = new BossLineGate();
        UUID b = UUID.randomUUID();
        assertEquals(0, g.next(b, "taunt"));
        assertEquals(1, g.next(b, "taunt"));
        assertEquals(0, g.next(b, "combo"));
    }

    @Test
    void memoryIsBounded() {
        BossLineGate g = new BossLineGate();
        for (int i = 0; i < 100; i++) g.allow(UUID.randomUUID(), "taunt", i);
        assertEquals(BossLineGate.MAX_BOSSES, g.tracked());
        UUID b = UUID.randomUUID();
        g.allow(b, "taunt", 0);
        g.forget(b);
        assertTrue(g.allow(b, "taunt", 1), "forgotten boss starts fresh");
        g.clear();
        assertEquals(0, g.tracked());
    }
}
