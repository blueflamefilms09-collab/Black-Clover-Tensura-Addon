package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveCues.Call;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.State;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The signs the hive gives of what it is about to do. */
class HiveCuesTest {
    @Test
    void everyChangeOfStateHasACallAndALine() {
        Set<Call> calls = new HashSet<>();
        for (State from : State.values()) {
            for (State to : State.values()) {
                Call c = HiveCues.call(from, to);
                String line = HiveCues.line(from, to);
                if (from == to) {
                    assertSame(Call.NONE, c);
                    assertEquals("", line);
                    continue;
                }
                assertNotSame(Call.NONE, c, from + " -> " + to);
                calls.add(c);
                if (to != State.SEARCH) assertFalse(line.isBlank(), from + " -> " + to);
                assertFalse(line.contains("null"));
            }
        }
        assertEquals(6, calls.size(), "six different calls, one for each state it can enter");
    }

    @Test
    void theColoursDifferAndAreValid() {
        Set<String> seen = new HashSet<>();
        for (State s : State.values()) {
            int[] c = HiveCues.colour(s);
            assertEquals(3, c.length);
            for (int v : c) assertTrue(v >= 0 && v <= 255);
            assertTrue(seen.add(c[0] + "," + c[1] + "," + c[2]), "a colour for each state: " + s);
        }
        assertTrue(HiveCues.colour(State.ENGAGE)[0] > 200 && HiveCues.colour(State.ENGAGE)[1] < 80, "red for the fight");
    }

    @Test
    void theWordsAreThereAndCareful() {
        for (String s : new String[]{HiveCues.FAKE_WARNING, HiveCues.FAKE_DISSOLVES, HiveCues.MARKED, HiveCues.UNRAVELS}) assertFalse(s.isBlank());
        assertTrue(HiveCues.line(State.ENGAGE, State.SEARCH).contains("trail"));
        assertEquals("", HiveCues.line(State.SEARCH, State.SEARCH));
    }
}
