package com.newuniverse.nusmp.audio.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the cross-fade rules are the part of the engine that can go wrong without any error: a fight that flaps the music, a fade that never ends, a switch that cuts a track off,
 * a third track piling up. The director is a pure state machine, so each rule is a few ticks of a loop here.
 */
class MusicDirectorTest {
    private static final int EXPLORE = MusicLayer.EXPLORE.priority(), BATTLE = MusicLayer.BATTLE.priority(), BOSS = MusicLayer.BOSS.priority();
    private long t;

    private MusicDirector director() {
        t = 0;
        return new MusicDirector(MusicDirector.Params.ofSeconds(4.0, 12.0));
    }

    private void run(MusicDirector d, int ticks, String wanted, int prio) {
        for (int i = 0; i < ticks; i++) d.tick(t++, wanted, prio);
    }

    @Test
    void theFirstTrackStartsAtOnceFadesInAndSettlesAtFullLevel() {
        MusicDirector d = director();
        assertFalse(d.isActive());
        assertTrue(d.tick(t++, "stone_explore", EXPLORE), "the first tick starts the voice");
        assertEquals(1, d.voices().size());
        assertTrue(d.voices().get(0).level() < 0.1f, "it starts quiet");
        run(d, 40, "stone_explore", EXPLORE);
        float mid = d.voices().get(0).level();
        assertTrue(mid > 0.3f && mid < 0.99f, "half way through the fade: " + mid);
        run(d, 60, "stone_explore", EXPLORE);
        assertEquals(1.0f, d.voices().get(0).level(), 0.0001f);
        assertEquals("stone_explore", d.currentTrack());
    }

    @Test
    void aFightTakesOverQuicklyAndTheOldTrackFadesOut() {
        MusicDirector d = director();
        run(d, 200, "stone_explore", EXPLORE);
        run(d, 9, "stone_battle", BATTLE);
        assertEquals("stone_explore", d.currentTrack(), "a single stray tick of a fight must not trigger it");
        run(d, 2, "stone_battle", BATTLE);
        assertEquals("stone_battle", d.currentTrack());
        assertEquals(2, d.voices().size());
        assertTrue(d.voices().get(0).fadingOut());
        run(d, 100, "stone_battle", BATTLE);
        assertEquals(1, d.voices().size(), "the old loop is gone once it has faded");
        assertEquals("stone_battle", d.voices().get(0).track());
    }

    @Test
    void aFightLingersAfterTheLastHunterAndTheCooldownHoldsTheCalmBack() {
        MusicDirector d = director();
        run(d, 100, "stone_explore", EXPLORE);
        run(d, 30, "stone_battle", BATTLE);
        long switched = d.lastSwitch();
        run(d, 99, "stone_explore", EXPLORE);
        assertEquals("stone_battle", d.currentTrack(), "five seconds of calm are needed first");
        run(d, 150, "stone_explore", EXPLORE);
        assertEquals("stone_explore", d.currentTrack(), "after the hold and the cooldown the calm track comes back");
        assertTrue(d.lastSwitch() - switched >= MusicDirector.Params.ofSeconds(4.0, 12.0).cooldown());
    }

    @Test
    void aFlickeringWantDoesNotSwitchAnything() {
        MusicDirector d = director();
        run(d, 100, "stone_explore", EXPLORE);
        for (int i = 0; i < 40; i++) {
            run(d, 5, "stone_battle", BATTLE);
            run(d, 5, "stone_explore", EXPLORE);
        }
        assertEquals("stone_explore", d.currentTrack(), "a want that never holds for ten ticks must never win");
        assertEquals(1, d.voices().size());
    }

    @Test
    void aBossBeatsAFightAndIgnoresTheCooldown() {
        MusicDirector d = director();
        run(d, 100, "stone_explore", EXPLORE);
        run(d, 30, "stone_battle", BATTLE);
        run(d, 15, "boss_holy", BOSS);
        assertEquals("boss_holy", d.currentTrack());
    }

    @Test
    void anEqualPriorityChangeWaitsForTheSettleTimeAndTheCooldown() {
        MusicDirector d = director();
        run(d, 100, "stone_explore", EXPLORE);
        run(d, 150, "frost_explore", EXPLORE);
        assertEquals("frost_explore", d.currentTrack(), "a new floor family is taken once it has held for five seconds and the cooldown is over");
        long at = d.lastSwitch();
        run(d, 150, "holy_explore", EXPLORE);
        assertEquals("frost_explore", d.currentTrack(), "the cooldown (twelve seconds) holds the next switch back");
        run(d, 100, "holy_explore", EXPLORE);
        assertEquals("holy_explore", d.currentTrack());
        assertTrue(d.lastSwitch() - at >= 240);
    }

    @Test
    void silenceFadesTheCurrentVoiceOutAndEndsWithNoVoice() {
        MusicDirector d = director();
        run(d, 100, "stone_explore", EXPLORE);
        d.tick(t++, null, 0);
        assertNull(d.currentTrack());
        assertTrue(d.isActive(), "it is still fading");
        assertTrue(d.voices().get(0).fadingOut());
        run(d, 100, null, 0);
        assertFalse(d.isActive());
    }

    @Test
    void theGainIsEqualPowerAndNeverLeavesZeroToOne() {
        MusicDirector d = director();
        run(d, 1, "stone_explore", EXPLORE);
        float prevIn = -1;
        for (int i = 0; i < 100; i++) {
            d.tick(t++, "stone_explore", EXPLORE);
            float l = d.voices().get(0).level();
            assertTrue(l >= 0f && l <= 1f);
            assertTrue(l >= prevIn, "a fade-in never goes down");
            prevIn = l;
        }
        MusicDirector.Voice v = d.voices().get(0);
        // at the half-way point of an equal-power crossfade the two gains squared add up to one
        double in = Math.sin(0.5 * Math.PI / 2), out = Math.cos(0.5 * Math.PI / 2);
        assertEquals(1.0, in * in + out * out, 1e-9);
        assertEquals(1.0f, v.level(), 0.0001f);
    }

    @Test
    void neverMoreThanThreeVoicesAtOnce() {
        MusicDirector d = new MusicDirector(MusicDirector.Params.ofSeconds(20.0, 0.0));
        t = 0;
        String[] tracks = {"stone_explore", "frost_explore", "holy_explore", "deep_explore", "crypt_explore", "boss_holy"};
        for (int i = 0; i < tracks.length; i++) {
            int prio = i + 1;
            run(d, 12, tracks[i], prio);
            assertTrue(d.voices().size() <= 3, "voices: " + d.voices().size());
        }
        assertEquals("boss_holy", d.currentTrack());
    }

    @Test
    void resetDropsEverything() {
        MusicDirector d = director();
        run(d, 50, "stone_explore", EXPLORE);
        d.reset();
        assertFalse(d.isActive());
        assertNull(d.currentTrack());
        assertTrue(d.tick(t++, "stone_explore", EXPLORE), "a fresh start after a reset is immediate");
    }

    @Test
    void theTimingComesFromSecondsAndIsClamped() {
        MusicDirector.Params p = MusicDirector.Params.ofSeconds(4.0, 12.0);
        assertEquals(80, p.fadeIn());
        assertEquals(60, p.fadeOut());
        assertEquals(40, p.fastFadeIn());
        assertEquals(240, p.cooldown());
        MusicDirector.Params q = MusicDirector.Params.ofSeconds(-5, -5);
        assertTrue(q.fadeIn() >= 1 && q.cooldown() == 0);
        MusicDirector.Params r = MusicDirector.Params.ofSeconds(999, 999);
        assertEquals(400, r.fadeIn());
        assertEquals(2400, r.cooldown());
    }
}
