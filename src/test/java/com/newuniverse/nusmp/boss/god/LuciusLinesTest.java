package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The voice lines: at least four per event, deterministic per seed, no repeat before every line was heard. */
class LuciusLinesTest {
    @Test
    void everyEventHasAtLeastFourDistinctNonEmptyLines() {
        for (LuciusLines.Event e : LuciusLines.Event.values()) {
            assertTrue(LuciusLines.count(e) >= 4, e.name());
            Set<String> seen = new HashSet<>();
            for (String l : LuciusLines.all(e)) {
                assertFalse(l.isBlank(), e.name());
                assertTrue(seen.add(l), "duplicate line in " + e.name());
            }
        }
    }

    @Test
    void thePickIsDeterministic() {
        for (LuciusLines.Event e : LuciusLines.Event.values())
            for (int n = 0; n < 12; n++) assertEquals(LuciusLines.pick(e, 12345L, n), LuciusLines.pick(e, 12345L, n));
    }

    @Test
    void noLineRepeatsBeforeAllWereHeard() {
        for (LuciusLines.Event e : LuciusLines.Event.values()) {
            Set<String> heard = new HashSet<>();
            int n = LuciusLines.count(e);
            for (int i = 0; i < n; i++) assertTrue(heard.add(LuciusLines.pick(e, 777L, i)), e.name() + " #" + i);
            assertEquals(LuciusLines.pick(e, 777L, 0), LuciusLines.pick(e, 777L, n), "then it starts over");
        }
    }

    @Test
    void differentSeedsStartAtDifferentLinesSometimes() {
        Set<String> firsts = new HashSet<>();
        for (long seed = 0; seed < 64; seed++) firsts.add(LuciusLines.pick(LuciusLines.Event.FIGHT_START, seed, 0));
        assertTrue(firsts.size() > 1, "the seed matters");
    }

    @Test
    void aNegativeOccurrenceDoesNotThrow() {
        assertNotNull(LuciusLines.pick(LuciusLines.Event.DEFEAT, -9L, -3));
    }

    @Test
    void stageTitles() {
        assertEquals("ACT I", LuciusLines.stageTitle(1));
        assertEquals("ACT III", LuciusLines.stageTitle(3));
        assertEquals("ACT III", LuciusLines.stageTitle(8));
        assertEquals("Gravity and Blood", LuciusLines.stageSubtitle(2));
    }
}
