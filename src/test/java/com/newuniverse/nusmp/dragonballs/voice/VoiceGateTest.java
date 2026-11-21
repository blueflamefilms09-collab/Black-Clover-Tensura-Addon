package com.newuniverse.nusmp.dragonballs.voice;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VoiceGateTest {
    private static UUID id(int n) { return new UUID(0, n); }

    @Test
    void aCompleteUnlockedSetOrASummonerIsListenedTo() {
        assertTrue(VoiceGate.eligible(false, 7, 7, false));
        assertFalse(VoiceGate.eligible(false, 6, 7, false), "one ball missing");
        assertFalse(VoiceGate.eligible(false, 7, 7, true), "stone");
        assertTrue(VoiceGate.eligible(true, 0, 7, true), "the summoner speaks the wish");
        assertFalse(VoiceGate.eligible(false, 8, 0, false));
    }

    @Test
    void whisperFollowsTheSetting() {
        assertTrue(VoiceGate.whisperOk(false, false));
        assertTrue(VoiceGate.whisperOk(true, true));
        assertFalse(VoiceGate.whisperOk(true, false));
    }

    @Test
    void theListenerCapHolds() {
        List<UUID> many = List.of(id(1), id(2), id(3), id(4), id(5), id(6));
        assertEquals(4, VoiceGate.select(List.of(), many, Set.of(), 4).size());
        assertTrue(VoiceGate.select(List.of(), many, Set.of(), 0).isEmpty());
        assertTrue(VoiceGate.select(List.of(), many, Set.of(), -3).isEmpty());
    }

    @Test
    void summonersFirstThenThoseAlreadyListenedTo() {
        List<UUID> eligible = List.of(id(1), id(2), id(3), id(4));
        List<UUID> pick = VoiceGate.select(List.of(id(9)), eligible, Set.of(id(4), id(3)), 3);
        assertEquals(id(9), pick.get(0));
        assertTrue(pick.contains(id(3)) && pick.contains(id(4)), "nobody who is already listened to is bumped by a newcomer");
        assertEquals(3, pick.size());
    }

    @Test
    void noDuplicatesWhenASummonerIsAlsoEligible() {
        List<UUID> pick = VoiceGate.select(List.of(id(1)), List.of(id(1), id(2)), Set.of(), 4);
        assertEquals(2, pick.size());
    }

    @Test
    void everyPlayerHasOneTurnPerSecond() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            int b = VoiceGate.bucket(UUID.randomUUID());
            assertTrue(b >= 0 && b < VoiceGate.BUCKETS);
            seen.add(b);
        }
        assertEquals(VoiceGate.BUCKETS, seen.size(), "the players are spread over all 20 ticks");
    }
}
