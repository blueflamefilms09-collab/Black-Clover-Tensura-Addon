package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the 1.93.0 rebuild of Song, Sound, Soul, Soul Corpse and Memory Magic keeps every decision that can be pure in small classes (SongRules, SoundResonance, SoulRules,
 * SoulCorpseRules, MemoryRewind), so the numbers that make the magics behave are pinned here without Minecraft: the choir ring and verse, the resonance stacks, the spiritual damage
 * and the harvest, the puppet's step and the hand's cycle, the history ring that Recall reads and the Archive replay cap.
 */
class MagicRebuildRulesTest {
    // ---------------------------------------------------------------- Song
    @Test
    void theChoirStandsOnARingAndNeverSingsAllAtOnceForLong() {
        assertEquals(0.0, SongRules.ringAngle(0, 0), 1e-9);
        assertEquals(Math.PI * 2 / 3, SongRules.ringAngle(1, 0), 1e-9);
        assertEquals(Math.PI * 4 / 3 + 50 * SongRules.RING_SPEED, SongRules.ringAngle(2, 50), 1e-9);
        int silentAll = 0, singers = 0;
        for (int t = 0; t < 90; t++) {
            int n = 0;
            for (int role = 0; role < 3; role++) if (SongRules.singingAt(role, t)) n++;
            singers += n;
            if (n == 0) silentAll++;
        }
        assertEquals(108, singers);
        assertEquals(0, silentAll, "the staggered choir has someone singing at every tick");
        assertTrue(SongRules.verseStarts(0, 30) && SongRules.verseStarts(1, 20) && SongRules.verseStarts(2, 10) && !SongRules.verseStarts(0, 31));
        assertTrue(SongRules.beats(1) && SongRules.beats(21) && !SongRules.beats(2));
    }

    // ---------------------------------------------------------------- Sound
    @Test
    void resonanceStacksToFiveForTenSecondsAndRingsPerStack() {
        assertEquals(1, SoundResonance.next(0, 0, 100));
        assertEquals(2, SoundResonance.next(1, 100, 150));
        assertEquals(5, SoundResonance.next(5, 150, 200));
        assertEquals(5, SoundResonance.next(4, 150, 200));
        assertEquals(1, SoundResonance.next(3, 100, 100 + SoundResonance.WINDOW_TICKS + 1), "a mark that ran out starts again at one");
        assertEquals(3, SoundResonance.alive(3, 100, 100 + SoundResonance.WINDOW_TICKS));
        assertEquals(0, SoundResonance.alive(3, 100, 100 + SoundResonance.WINDOW_TICKS + 1));
        assertEquals(0, SoundResonance.alive(0, 100, 101));
        assertEquals(25f, SoundResonance.ring(5, 5f), 1e-6);
        assertEquals(25f, SoundResonance.ring(9, 5f), 1e-6, "never more than five count");
        assertEquals(0f, SoundResonance.ring(-1, 5f), 1e-6);
    }

    // ---------------------------------------------------------------- Soul
    @Test
    void spiritualDamageScalesWithEpIsCappedAndHalvedForBosses() {
        assertEquals(2f, SoulRules.spirit(1.0, 2f, false), 1e-6);
        assertEquals(10f, SoulRules.spirit(5.0, 2f, false), 1e-6);
        assertEquals(SoulRules.SPIRIT_CAP, SoulRules.spirit(12.0, 30f, false), 1e-6);
        assertEquals(SoulRules.SPIRIT_CAP, SoulRules.spirit(99.0, 30f, false), 1e-6, "the EP scale is clamped to 12 first");
        assertEquals(SoulRules.SPIRIT_CAP / 2f, SoulRules.spirit(12.0, 30f, true), 1e-6);
        assertEquals(2f, SoulRules.spirit(0.1, 2f, false), 1e-6, "the scale never goes below 1");
    }

    @Test
    void aHarvestPaysForTheFirstSixSoulsOnly() {
        for (int n = 0; n < SoulRules.HARVEST_MAX_SOULS; n++) assertTrue(SoulRules.pays(n));
        assertFalse(SoulRules.pays(SoulRules.HARVEST_MAX_SOULS));
        assertEquals(60.0, SoulRules.harvestMana(1000.0), 1e-9);
        assertEquals(1.6f, SoulRules.harvestHealth(20f), 1e-6);
    }

    // ---------------------------------------------------------------- Soul Corpse
    @Test
    void aPuppetWalksAtItsSpeedAndStopsAtArmsReach() {
        double[] s = SoulCorpseRules.step(0, 0, 10, 0, 0.2, 1.6);
        assertEquals(0.2, s[0], 1e-9);
        assertEquals(0.0, s[1], 1e-9);
        double[] near = SoulCorpseRules.step(0, 0, 1.7, 0, 0.2, 1.6);
        assertEquals(0.1, near[0], 1e-9, "never overshoots the stop distance");
        double[] there = SoulCorpseRules.step(0, 0, 1.5, 0, 0.2, 1.6);
        assertEquals(0.0, there[0], 1e-9);
        assertEquals(0.0, there[1], 1e-9);
        double[] diag = SoulCorpseRules.step(0, 0, 6, 8, 0.5, 0);
        assertEquals(0.3, diag[0], 1e-9);
        assertEquals(0.4, diag[1], 1e-9);
        assertTrue(SoulCorpseRules.strikes(20, 1.9) && !SoulCorpseRules.strikes(20, 2.1) && !SoulCorpseRules.strikes(21, 1.0));
        assertTrue(SoulCorpseRules.beyondLeash(25) && !SoulCorpseRules.beyondLeash(10));
    }

    @Test
    void aHandClosesEveryCycleAndShowsItsFistFirst() {
        assertTrue(SoulCorpseRules.handCloses(24) && SoulCorpseRules.handCloses(48) && !SoulCorpseRules.handCloses(0) && !SoulCorpseRules.handCloses(25));
        assertTrue(SoulCorpseRules.handClosing(24) && SoulCorpseRules.handClosing(33) && !SoulCorpseRules.handClosing(34));
        assertEquals(4, SoulCorpseRules.PUPPET_CAP);
    }

    // ---------------------------------------------------------------- Memory
    @Test
    void theHistoryRingKeepsEightSamplesAndRecallGoesBackThreeSecondsOrMore() {
        MemoryRewind.Ring ring = new MemoryRewind.Ring();
        assertNull(ring.recall(1000, "overworld"), "a fresh caster remembers nothing");
        for (int k = 0; k < 12; k++) ring.offer(new MemoryRewind.Sample(k * 20L, k, 64, 0, 20f - k, "overworld"));
        assertEquals(MemoryRewind.SAMPLES, ring.size());
        assertEquals(11, ring.latest().x(), 1e-9);
        MemoryRewind.Sample back = ring.recall(11 * 20L, "overworld");
        assertNotNull(back);
        assertEquals(4 * 20L, back.tick(), "the oldest sample is 7 seconds old, and it is the one kept");
        assertTrue(11 * 20L - back.tick() >= MemoryRewind.MIN_AGE);
        assertNull(ring.recall(11 * 20L, "the_nether"), "another dimension has no memory of you");
        MemoryRewind.Ring young = new MemoryRewind.Ring();
        young.offer(new MemoryRewind.Sample(100, 0, 0, 0, 20f, "overworld"));
        young.offer(new MemoryRewind.Sample(120, 1, 0, 0, 20f, "overworld"));
        assertNull(young.recall(140, "overworld"), "nothing is three seconds old yet");
        assertNotNull(young.recall(100 + MemoryRewind.MIN_AGE, "overworld"));
    }

    @Test
    void theRingIgnoresASampleThatComesTooSoonAndClears() {
        MemoryRewind.Ring ring = new MemoryRewind.Ring();
        assertTrue(ring.offer(new MemoryRewind.Sample(0, 0, 0, 0, 20f, "o")));
        assertFalse(ring.offer(new MemoryRewind.Sample(10, 1, 0, 0, 20f, "o")));
        assertTrue(ring.offer(new MemoryRewind.Sample(20, 2, 0, 0, 20f, "o")));
        assertEquals(2, ring.size());
        ring.clear();
        assertEquals(0, ring.size());
        assertNull(ring.latest());
    }

    @Test
    void archiveReplaysSixtyPercentUnderACap() {
        assertEquals(6f, MemoryRewind.replay(10, 100f, false), 1e-6);
        assertEquals(35f, MemoryRewind.replay(1000, 100f, false), 1e-6, "capped at 35 percent of the foe's health");
        assertEquals(17.5f, MemoryRewind.replay(1000, 100f, true), 1e-6, "half of that for a boss");
        assertEquals(0f, MemoryRewind.replay(0, 100f, false), 1e-6);
        assertEquals(0f, MemoryRewind.replay(-5, 100f, false), 1e-6);
    }
}
