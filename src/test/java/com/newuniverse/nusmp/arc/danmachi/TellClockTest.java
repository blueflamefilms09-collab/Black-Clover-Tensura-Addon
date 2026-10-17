package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the aim of a move is locked on the first call of its telegraph and must never be locked again during the same wind-up, for every wind-up length the four tables
 * use (and the shorter and longer ones the power tuning makes of them); this walks the framework's calling rule (every third tick, while ticks are left) and proves it.
 */
class TellClockTest {
    /** The framework's rule, written the way {@code ZogratisBoss.tickCast} does it: one tick after another, call when the ticks left are a positive multiple of three. */
    private static int[] walk(int total) {
        int[] seen = new int[total];
        int n = 0;
        for (int tick = 1; tick <= total; tick++) {
            int left = total - tick;
            if (left > 0 && left % 3 == 0) seen[n++] = left;
        }
        return java.util.Arrays.copyOf(seen, n);
    }

    @Test
    void theCallsAreTheMultiplesOfThreeBelowTheTotal() {
        for (int total = 4; total <= 200; total++) {
            assertEquals(java.util.Arrays.toString(walk(total)), java.util.Arrays.toString(TellClock.calls(total)), "wind-up " + total);
        }
        assertEquals(0, TellClock.calls(3).length, "a wind-up of three ticks is never telegraphed: left never is a positive multiple of three");
    }

    @Test
    void exactlyOneCallIsTheFirstAndItComesInTheFirstThreeTicks() {
        for (int total = 8; total <= 200; total++) {
            int firsts = 0;
            int[] calls = walk(total);
            assertTrue(calls.length >= 1, "wind-up " + total + " is telegraphed at least once");
            for (int left : calls) if (TellClock.first(left, total)) firsts++;
            assertEquals(1, firsts, "wind-up " + total);
            assertTrue(TellClock.first(calls[0], total), "the first call in time is the one that locks, wind-up " + total);
            assertTrue(total - calls[0] <= TellClock.STEP, "within the first three ticks, wind-up " + total);
        }
    }

    @Test
    void everyWindUpOfTheTablesAndTheirTunedLengthsIsCovered() {
        for (java.util.List<DmSpec> table : java.util.List.of(MinotaurTable.specs(), BlackGoliathTable.specs(), AmphisbaenaTable.specs(), UdaeusTable.specs())) {
            for (DmSpec s : table) {
                for (double mult : new double[]{1.0, 0.95, 0.85, 0.75}) {
                    int wind = Math.max(s.ultimate() ? 30 : 8, (int) Math.round(s.windUpTicks() * mult));
                    int firsts = 0;
                    for (int left : walk(wind)) if (TellClock.first(left, wind)) firsts++;
                    assertEquals(1, firsts, s.id() + " at x" + mult + " (" + wind + " ticks)");
                }
            }
        }
    }
}
