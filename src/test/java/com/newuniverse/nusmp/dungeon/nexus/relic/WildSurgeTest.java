package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The Gambler's Grimoire Page: the hidden d20, the cooldown and the spell list (docs/living_dungeon.md section 7). */
class WildSurgeTest {
    @Test
    void theRollIsADieTwenty() {
        assertEquals(1, WildSurge.roll(bound -> 0));
        assertEquals(20, WildSurge.roll(bound -> bound - 1));
        Random r = new Random(1234);
        int[] seen = new int[21];
        for (int i = 0; i < 20000; i++) {
            int v = WildSurge.roll(r::nextInt);
            assertTrue(v >= 1 && v <= 20);
            seen[v]++;
        }
        for (int v = 1; v <= 20; v++) assertTrue(seen[v] > 800 && seen[v] < 1200, "face " + v + " came up " + seen[v] + " times");
    }

    @Test
    void onlyAOneBackfires() {
        assertTrue(WildSurge.backfires(1));
        for (int v = 2; v <= 20; v++) assertFalse(WildSurge.backfires(v), "roll " + v);
        assertEquals(0.05, WildSurge.backfireChance(), 1e-12);
    }

    @Test
    void aboutOneUseInTwentyBackfires() {
        Random r = new Random(77);
        int hits = 0, n = 40000;
        for (int i = 0; i < n; i++) if (WildSurge.backfires(WildSurge.roll(r::nextInt))) hits++;
        double rate = hits / (double) n;
        assertTrue(rate > 0.04 && rate < 0.06, "rate " + rate);
    }

    @Test
    void thePageRestsForAMinute() {
        long now = 5000;
        long at = WildSurge.readyAt(now);
        assertEquals(now + 1200, at);
        assertFalse(WildSurge.ready(now, at));
        assertFalse(WildSurge.ready(now + 1199, at));
        assertTrue(WildSurge.ready(now + 1200, at));
        assertEquals(60, WildSurge.secondsLeft(now, at));
        assertEquals(1, WildSurge.secondsLeft(now + 1199, at));
        assertEquals(0, WildSurge.secondsLeft(now + 1200, at));
        assertTrue(WildSurge.ready(10, 0), "never used");
        assertTrue(WildSurge.ready(100, 999_999), "a stamp from a clock that ran later does not lock the page");
    }

    @Test
    void theBackfireCostsATenthOfThePoolForADay() {
        assertEquals(24000, WildSurge.PENALTY_TICKS, "a whole in-game day");
        assertEquals(0.10, WildSurge.PENALTY, 1e-12);
    }

    @Test
    void everySpellOfTheListCanBePickedAndIsTelegraphed() {
        boolean[] seen = new boolean[PageSpell.values().length];
        for (int i = 0; i < PageSpell.values().length; i++) {
            int fixed = i;
            seen[PageSpell.pick(bound -> fixed).ordinal()] = true;
        }
        for (boolean s : seen) assertTrue(s);
        for (PageSpell s : PageSpell.values()) {
            assertTrue(s.telegraph >= 10, s.label + " must show where it lands before it lands");
            assertTrue(s.raw > 0 && s.size > 0 && s.range > 0, s.label);
        }
        assertEquals(PageSpell.DARK_CLOAKED_DIMENSION_SLASH, PageSpell.values()[0]);
        assertEquals(0, WildSurge.pick(bound -> 99, 1), "a list of one has no choice");
    }
}
