package com.newuniverse.nusmp.fairytail.storm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The three small books of the storm system: how many storms a player and the server may have, how often one creature may be hit, and the per-player spell cooldowns. */
class StormLimitsTest {
    private static List<StormCaps.Live> live(String... owners) {
        List<StormCaps.Live> l = new ArrayList<>();
        long t = 0;
        for (String o : owners) l.add(new StormCaps.Live(o, t++));
        return l;
    }

    // ------------------------------------------------------------------------------------------------------------------ caps
    @Test
    void aPlayerWithRoomJustCasts() {
        StormCaps.Verdict v = StormCaps.decide(live("a"), "a", 2, 24);
        assertTrue(v.admit());
        assertFalse(v.evicts());
    }

    @Test
    void aPlayerAtTheirCapEndsTheirOldestStorm() {
        StormCaps.Verdict v = StormCaps.decide(live("a", "b", "a"), "a", 2, 24);
        assertTrue(v.admit());
        assertTrue(v.evicts());
        assertEquals(0, v.evictIndex(), "the first (oldest) storm of a, not b's");
        assertEquals(1, StormCaps.decide(live("a", "b", "a"), "b", 1, 24).evictIndex(), "b's only storm");
    }

    @Test
    void aFullSkyRefusesAStrangerButNotAPlayerWhoCanSwap() {
        List<StormCaps.Live> full = live("a", "b", "c");
        assertFalse(StormCaps.decide(full, "d", 2, 3).admit(), "d has no storm to swap and the sky is full");
        StormCaps.Verdict swap = StormCaps.decide(full, "b", 2, 3);
        assertTrue(swap.admit());
        assertEquals(1, swap.evictIndex());
    }

    @Test
    void theLimitsAreClamped() {
        assertTrue(StormCaps.decide(live(), "a", 0, 0).admit(), "a nonsense config still lets one storm be cast");
        List<StormCaps.Live> five = live("a", "a", "a", "a", "a");
        assertTrue(StormCaps.decide(five, "a", 99, 999).evicts(), "per-player is never above 4");
    }

    @Test
    void theNumberOfStormsNeverGrowsPastTheCaps() {
        List<StormCaps.Live> l = new ArrayList<>();
        String[] who = {"a", "b", "c", "d", "e"};
        for (int i = 0; i < 400; i++) {
            String o = who[i % who.length];
            StormCaps.Verdict v = StormCaps.decide(l, o, 2, 7);
            if (!v.admit()) continue;
            if (v.evicts()) l.remove(v.evictIndex());
            l.add(new StormCaps.Live(o, i));
            assertTrue(l.size() <= 7, "global cap");
            for (String p : who) assertTrue(l.stream().filter(x -> x.owner().equals(p)).count() <= 2, "per-player cap");
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ hits
    @Test
    void oneCreatureTakesAtMostThreeBoltsEightStonesAndSixPulsesFromOneStorm() {
        StormHits h = new StormHits();
        int bolts = 0, hail = 0, pulses = 0;
        for (long t = 0; t < 10_000; t++) {
            if (h.tryHit(7, StormHits.Kind.BOLT, t)) bolts++;
            if (h.tryHit(7, StormHits.Kind.HAIL, t)) hail++;
            if (h.tryHit(7, StormHits.Kind.PULSE, t)) pulses++;
        }
        assertEquals(StormMath.MAX_BOLTS_PER_TARGET, bolts);
        assertEquals(StormMath.MAX_HAIL_PER_TARGET, hail);
        assertEquals(StormMath.MAX_PULSES_PER_TARGET, pulses);
    }

    @Test
    void twoHitsOfOneKindNeedTheirGap() {
        StormHits h = new StormHits();
        assertTrue(h.tryHit(1, StormHits.Kind.BOLT, 100));
        assertFalse(h.tryHit(1, StormHits.Kind.BOLT, 100 + StormMath.BOLT_GAP - 1));
        assertTrue(h.tryHit(1, StormHits.Kind.BOLT, 100 + StormMath.BOLT_GAP));
        assertTrue(h.tryHit(1, StormHits.Kind.HAIL, 101), "the kinds have separate gaps");
        assertTrue(h.tryHit(2, StormHits.Kind.BOLT, 101), "and separate creatures");
    }

    @Test
    void theBookIsBounded() {
        StormHits h = new StormHits();
        for (int id = 0; id < 500; id++) assertTrue(h.tryHit(id, StormHits.Kind.HAIL, id));
        assertTrue(h.size() <= StormHits.MAX_ENTRIES);
    }

    // ------------------------------------------------------------------------------------------------------------------ cooldowns
    @Test
    void cooldownsAreKeyedByThePlayerAndTheSpell() {
        StormCooldowns c = new StormCooldowns();
        assertTrue(c.ready("p1", 0, 0));
        c.start("p1", 0, 100, 200);
        assertEquals(200, c.left("p1", 0, 100));
        assertEquals(150, c.left("p1", 0, 150));
        assertEquals(0, c.left("p1", 0, 300));
        assertTrue(c.ready("p1", 1, 100), "another spell of the same player is ready");
        assertTrue(c.ready("p2", 0, 100), "the same spell of another player is ready");
        c.forget("p1");
        assertTrue(c.ready("p1", 0, 100), "a player who left starts clean");
        assertEquals(0, c.players(), "nothing is left");
    }

    @Test
    void aNonsenseSpellNumberNeverBreaksTheBook() {
        StormCooldowns c = new StormCooldowns();
        c.start("p", -1, 0, 100);
        c.start("p", 99, 0, 100);
        assertTrue(c.ready("p", -1, 0));
        assertTrue(c.ready("p", 99, 0));
        assertEquals(0, c.players());
    }
}
