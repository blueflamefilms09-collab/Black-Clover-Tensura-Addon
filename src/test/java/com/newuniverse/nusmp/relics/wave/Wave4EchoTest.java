package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 4: the guard the Pendant of Echoed Souls raises around its wearer. */
class Wave4EchoTest {
    private static Set<UUID> ids(int n) {
        Set<UUID> s = new HashSet<>();
        for (int i = 0; i < n; i++) s.add(UUID.randomUUID());
        return s;
    }

    @Test
    void theWarriorCountIsOneDFour() {
        for (int d = -3; d <= 9; d++) {
            int n = Wave4Echo.count(d);
            assertTrue(n >= 1 && n <= 4, "1d4 gave " + n);
        }
        assertEquals(1, Wave4Echo.count(1));
        assertEquals(4, Wave4Echo.count(4));
    }

    @Test
    void oncePerDayAndNeverOnAnEmptyStamp() {
        assertTrue(Wave4Echo.ready(-1, 5));
        assertFalse(Wave4Echo.ready(1000, 1000 + Wave4Echo.COOLDOWN - 1));
        assertTrue(Wave4Echo.ready(1000, 1000 + Wave4Echo.COOLDOWN));
    }

    @Test
    void guardHoldsWhileAWarriorLivesAndNoLongerThanAMinute() {
        Wave4Echo.clear();
        UUID me = UUID.randomUUID();
        Set<UUID> w = ids(3);
        Wave4Echo.start(me, 1000, w);
        assertTrue(Wave4Echo.has(me));
        Set<UUID> alive = new HashSet<>(w);
        assertTrue(Wave4Echo.guarded(me, 1010, alive::contains));
        UUID first = w.iterator().next();
        alive.remove(first);
        assertTrue(Wave4Echo.guarded(me, 1200, alive::contains), "two warriors still stand");
        alive.clear();
        assertTrue(Wave4Echo.guarded(me, 1050, alive::contains), "the five-second floor holds even with nobody alive");
        assertFalse(Wave4Echo.guarded(me, 1000 + Wave4Echo.FLOOR + 1, alive::contains), "all warriors fallen after the floor: the guard ends");
        assertFalse(Wave4Echo.has(me));
        assertFalse(Wave4Echo.isWarrior(first), "the warriors are forgotten with the guard");
    }

    @Test
    void theGuardEndsAfterAMinuteEvenIfWarriorsLive() {
        Wave4Echo.clear();
        UUID me = UUID.randomUUID();
        Set<UUID> w = ids(2);
        Wave4Echo.start(me, 0, w);
        assertTrue(Wave4Echo.guarded(me, Wave4Echo.LIFE - 1, w::contains));
        assertFalse(Wave4Echo.guarded(me, Wave4Echo.LIFE, w::contains));
        assertEquals(0, Wave4Echo.size());
    }

    @Test
    void aGuardWithNoWarriorsStillHoldsTheFloor() {
        Wave4Echo.clear();
        UUID me = UUID.randomUUID();
        Wave4Echo.start(me, 500, Set.of());
        assertTrue(Wave4Echo.guarded(me, 500 + Wave4Echo.FLOOR - 1, x -> false));
        assertFalse(Wave4Echo.guarded(me, 500 + Wave4Echo.FLOOR, x -> false));
    }

    @Test
    void warriorsAreRememberedUntilTheGuardEndsAndStayBounded() {
        Wave4Echo.clear();
        UUID me = UUID.randomUUID();
        Set<UUID> w = ids(4);
        Wave4Echo.start(me, 0, w);
        for (UUID u : w) assertTrue(Wave4Echo.isWarrior(u));
        assertEquals(w, Wave4Echo.warriors(me));
        assertFalse(Wave4Echo.isWarrior(UUID.randomUUID()));
        Wave4Echo.end(me);
        for (UUID u : w) assertFalse(Wave4Echo.isWarrior(u));
        for (int i = 0; i < 500; i++) Wave4Echo.start(UUID.randomUUID(), i, ids(1));
        assertTrue(Wave4Echo.size() <= 64, "the table is bounded, got " + Wave4Echo.size());
        Wave4Echo.clear();
        assertEquals(0, Wave4Echo.size());
    }

    @Test
    void startingAgainReplacesTheOldGuard() {
        Wave4Echo.clear();
        UUID me = UUID.randomUUID();
        Set<UUID> a = ids(2), b = ids(1);
        Wave4Echo.start(me, 0, a);
        Wave4Echo.start(me, 10, b);
        for (UUID u : a) assertFalse(Wave4Echo.isWarrior(u));
        assertEquals(b, Wave4Echo.warriors(me));
        Wave4Echo.clear();
    }
}
