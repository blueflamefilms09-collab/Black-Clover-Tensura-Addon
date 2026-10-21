package com.newuniverse.nusmp.audio.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the order of the rules that choose a layer is written down once, in {@link MusicRules#decide}; this pins it (hub, boss, rift, safe, fight, explore), so a change of the order is
 * a test failure and not a surprise in the game.
 */
class MusicRulesTest {
    @Test
    void theHubIsAlwaysTheHub() {
        assertEquals(MusicLayer.HUB, MusicRules.decide(true, false, false, false, 0));
        assertEquals(MusicLayer.HUB, MusicRules.decide(true, true, true, true, 5));
    }

    @Test
    void aBossBeatsEverythingInTheDungeon() {
        assertEquals(MusicLayer.BOSS, MusicRules.decide(false, true, true, true, 5));
        assertEquals(MusicLayer.BOSS, MusicRules.decide(false, true, false, false, 0));
    }

    @Test
    void aRiftKeepsItsSoundUnlessABossIsThere() {
        assertEquals(MusicLayer.RIFT, MusicRules.decide(false, false, true, false, 4));
        assertEquals(MusicLayer.RIFT, MusicRules.decide(false, false, true, true, 0));
    }

    @Test
    void aSafeRoomIsSafeEvenWithAHunterOutside() {
        assertEquals(MusicLayer.SAFE, MusicRules.decide(false, false, false, true, 3));
    }

    @Test
    void huntersMeanAFightAndNothingMeansExploring() {
        assertEquals(MusicLayer.BATTLE, MusicRules.decide(false, false, false, false, MusicRules.BATTLE_HUNTERS));
        assertEquals(MusicLayer.EXPLORE, MusicRules.decide(false, false, false, false, 0));
    }
}
