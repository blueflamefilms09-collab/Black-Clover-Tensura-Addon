package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a wandering boss must hear a loud thing exactly once, a moment after it happened, and only if it was loud enough and near enough; these tests pin
 * the ring's rules (what is heard, what is never heard twice, which sound wins, what happens when the ring is full) so a refactor cannot make a boss deaf or
 * make it answer its own roar forever.
 */
class BurstRingTest {
    private static final WandererRules.Tuning T = WandererRules.of(24, 40, 160);

    @Test
    void anEmptyRingHearsNothing() {
        BurstRing r = new BurstRing(8);
        assertNull(r.loudest(0, 65, 0, 0, T));
        assertEquals(0, r.size());
    }

    @Test
    void theLoudestCloseSoundWins() {
        BurstRing r = new BurstRing(8);
        r.add(10, 65, 0, 2.0, 100);
        r.add(30, 65, 0, 6.0, 100);
        r.add(5, 65, 0, 1.0, 100);
        WandererRules.Heard h = r.loudest(0, 65, 0, 0, T);
        assertNotNull(h);
        assertEquals(30.0, h.x(), 0.0, "scores are 2/2 = 1.0, 6/4 = 1.5 and 1/1.5 = 0.67: the blast wins even from farther away");
    }

    @Test
    void theBestScoreNotTheLoudestAlone() {
        BurstRing r = new BurstRing(8);
        r.add(38, 65, 0, 6.0, 100);      // a blast at the edge of hearing: 6 / 4.8 = 1.25
        r.add(4, 65, 0, 1.0, 100);       // a block broken next door: 1 / 1.4 = 0.71
        r.add(8, 65, 0, 3.0, 100);       // a fight nearby: 3 / 1.8 = 1.67
        WandererRules.Heard h = r.loudest(0, 65, 0, 0, T);
        assertEquals(8.0, h.x(), 0.0);
        assertEquals(3.0, h.loudness(), 0.0);
        assertEquals(8.0, h.distance(), 1e-9);
    }

    @Test
    void soundsFromBeforeTheLastPassAreNotHeardAgain() {
        BurstRing r = new BurstRing(8);
        r.add(10, 65, 0, 6.0, 100);
        assertNotNull(r.loudest(0, 65, 0, 99, T));
        assertNull(r.loudest(0, 65, 0, 100, T), "a pass at tick 100 has already seen everything of tick 100, and the listener's own roar after it");
        assertNull(r.loudest(0, 65, 0, 101, T));
    }

    @Test
    void tooFarOrTooQuietIsNotHeard() {
        BurstRing r = new BurstRing(8);
        r.add(100, 65, 0, 6.0, 5);
        r.add(30, 65, 0, 0.5, 5);
        r.add(15, 65, 0, 1.0, 5);        // a quiet sound carries only a quarter of the range: 10 blocks
        assertNull(r.loudest(0, 65, 0, 0, T));
    }

    @Test
    void theOldestIsOverwrittenWhenTheRingIsFullAndItNeverGrows() {
        BurstRing r = new BurstRing(4);
        for (int i = 0; i < 10; i++) r.add(i, 65, 0, 6.0, i);
        assertEquals(4, r.size());
        assertEquals(4, r.capacity());
        WandererRules.Heard h = r.loudest(0, 65, 0, 0, T);
        assertNotNull(h);
        assertTrue(h.x() >= 6.0, "only the four newest events are left: " + h.x());
    }

    @Test
    void clearForgetsEverything() {
        BurstRing r = new BurstRing(8);
        r.add(1, 65, 0, 6.0, 5);
        r.clear();
        assertEquals(0, r.size());
        assertNull(r.loudest(0, 65, 0, 0, T));
        r.add(2, 65, 0, 6.0, 6);
        assertEquals(1, r.size());
    }

    @Test
    void aTinyCapacityIsRaisedToAUsableOne() {
        assertTrue(new BurstRing(0).capacity() >= 4);
        assertTrue(new BurstRing(-3).capacity() >= 4);
    }
}
