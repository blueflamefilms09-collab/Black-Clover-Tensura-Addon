package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Clone count, spell assignment, staggered casts, the shared pressure budget. */
class CloneSchedulerTest {
    @Test
    void cloneCountIsThreeAtTheLineAndOneMorePerFivePercent() {
        assertEquals(0, CloneScheduler.cloneCount(0.31f));
        assertEquals(0, CloneScheduler.cloneCount(0.30f), "act 3 begins under 30%");
        assertEquals(3, CloneScheduler.cloneCount(0.299f));
        assertEquals(3, CloneScheduler.cloneCount(0.26f));
        assertEquals(4, CloneScheduler.cloneCount(0.249f));
        assertEquals(4, CloneScheduler.cloneCount(0.21f));
        assertEquals(5, CloneScheduler.cloneCount(0.199f));
        assertEquals(5, CloneScheduler.cloneCount(0.01f));
        assertEquals(5, CloneScheduler.cloneCount(0f));
        assertEquals(0, CloneScheduler.cloneCount(Float.NaN));
    }

    @Test
    void everySpellIsCoveredFromThreeClones() {
        assertEquals(CloneScheduler.Spell.TIME, CloneScheduler.spellOf(0));
        assertEquals(CloneScheduler.Spell.GRAVITY, CloneScheduler.spellOf(1));
        assertEquals(CloneScheduler.Spell.SPATIAL, CloneScheduler.spellOf(2));
        assertEquals(CloneScheduler.Spell.TIME, CloneScheduler.spellOf(3));
    }

    @Test
    void atMostOneCastPerTickAndNoSameSpellInsideTheGap() {
        CloneScheduler s = new CloneScheduler();
        s.resize(5, 0);
        boolean[] alive = {true, true, true, true, true};
        Map<CloneScheduler.Spell, Long> last = new EnumMap<>(CloneScheduler.Spell.class);
        int casts = 0;
        for (long t = 0; t < 4000; t++) {
            List<CloneScheduler.Cast> c = s.tick(t, alive);
            assertTrue(c.size() <= 1, "tick " + t);
            for (CloneScheduler.Cast k : c) {
                casts++;
                Long prev = last.get(k.spell());
                if (prev != null) assertTrue(t - prev >= 40, k.spell() + " at " + t + " after " + prev);
                last.put(k.spell(), t);
            }
        }
        assertTrue(casts > 5 * (4000 / CloneScheduler.CAST_INTERVAL) - 10, "all clones keep casting: " + casts);
    }

    @Test
    void twoCopiesOfTheSameSpellDueTogetherAreSeparatedByFortyTicks() {
        CloneScheduler s = new CloneScheduler();
        s.resize(4, 0);                                              // slots 0 and 3 both cast Time
        boolean[] asleep = {false, false, false, false};
        boolean[] awake = {true, false, false, true};
        long first = -1, second = -1;
        for (long t = 0; t < 500; t++) {
            for (CloneScheduler.Cast c : s.tick(t, t < 300 ? asleep : awake)) {
                if (first < 0) first = t; else if (second < 0) second = t;
            }
        }
        assertEquals(300L, first, "both are due at once when they wake");
        assertTrue(second >= first + 40, "the second Time cast waits: " + first + " then " + second);
        assertEquals(340L, second);
    }

    @Test
    void everyCloneCastsAndOnItsOwnClock() {
        CloneScheduler s = new CloneScheduler();
        s.resize(3, 0);
        boolean[] alive = {true, true, true};
        int[] per = new int[3];
        List<Long> times = new ArrayList<>();
        for (long t = 0; t < 1000; t++) for (CloneScheduler.Cast c : s.tick(t, alive)) { per[c.slot()]++; times.add(t); }
        for (int n : per) assertTrue(n >= 4, "each clone casts about every 8 s");
        assertEquals(times.size(), times.stream().distinct().count(), "never the same tick twice");
    }

    @Test
    void deadClonesDoNotCast() {
        CloneScheduler s = new CloneScheduler();
        s.resize(3, 0);
        boolean[] alive = {false, true, false};
        for (long t = 0; t < 1000; t++) for (CloneScheduler.Cast c : s.tick(t, alive)) assertEquals(1, c.slot());
        assertTrue(s.tick(2000, null).isEmpty());
    }

    @Test
    void resizeKeepsTheOldTimersAndStaggersNewSlots() {
        CloneScheduler s = new CloneScheduler();
        s.resize(3, 0);
        assertEquals(3, s.slots());
        s.resize(5, 100);
        assertEquals(5, s.slots());
        s.resize(9, 100);
        assertEquals(5, s.slots(), "never above five");
        s.resize(0, 100);
        assertEquals(0, s.slots());
    }

    @Test
    void theSharedPressureBudgetCapsAVictimsSecond() {
        CloneScheduler.PressureBudget b = new CloneScheduler.PressureBudget();
        float give = b.allow(1, 100, 20f, 40f);
        assertEquals(12f, give, 1e-4f);                                     // 30% of 40
        assertEquals(0f, b.allow(1, 105, 5f, 40f), 1e-4f, "spent");
        assertEquals(5f, b.allow(2, 105, 5f, 40f), 1e-4f, "another victim has its own second");
        assertEquals(12f, b.allow(1, 120, 99f, 40f), 1e-4f, "a new window");
        assertEquals(0f, b.allow(1, 121, -3f, 40f));
        assertEquals(0f, b.allow(1, 121, 3f, 0f));
    }
}
