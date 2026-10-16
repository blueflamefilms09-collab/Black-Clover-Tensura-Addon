package com.newuniverse.nusmp.quests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Why: the counters decide "cast 50 spells" style advancements, so what one event bumps and what a respawn keeps is pinned down here. */
class EventCountersTest {
    @Test
    void plainEventBumpsOnlyTheKey() {
        Map<String, Integer> c = new HashMap<>();
        EventCounters.increment(c, "grimoire.received", null);
        EventCounters.increment(c, "grimoire.received", "");
        assertEquals(2, EventCounters.read(c, "grimoire.received", null));
        assertEquals(1, c.size());
    }

    @Test
    void eventWithArgBumpsKeyAndKeyArg() {
        Map<String, Integer> c = new HashMap<>();
        EventCounters.increment(c, "spell.cast", "thread");
        EventCounters.increment(c, "spell.cast", "thread");
        EventCounters.increment(c, "spell.cast", "fire");
        assertEquals(3, EventCounters.read(c, "spell.cast", null));
        assertEquals(2, EventCounters.read(c, "spell.cast", "thread"));
        assertEquals(1, EventCounters.read(c, "spell.cast", "fire"));
        assertEquals(0, EventCounters.read(c, "spell.cast", "water"));
    }

    @Test
    void namesUseThePipe() {
        assertEquals("boss.defeated|lucius_zogratis", EventCounters.name("boss.defeated", "lucius_zogratis"));
        assertEquals("squad.created", EventCounters.name("squad.created", null));
    }

    @Test
    void counterStopsAtMax() {
        Map<String, Integer> c = new HashMap<>();
        c.put("x", Integer.MAX_VALUE);
        EventCounters.increment(c, "x", null);
        assertEquals(Integer.MAX_VALUE, EventCounters.read(c, "x", null));
    }

    @Test
    void copyIsIndependent() {
        Map<String, Integer> c = new HashMap<>();
        EventCounters.increment(c, "spell.cast", "thread");
        Map<String, Integer> copy = EventCounters.copy(c);
        assertFalse(c == copy);
        EventCounters.increment(c, "spell.cast", "thread");
        assertEquals(1, EventCounters.read(copy, "spell.cast", "thread"));
        assertEquals(2, EventCounters.read(c, "spell.cast", "thread"));
    }
}
