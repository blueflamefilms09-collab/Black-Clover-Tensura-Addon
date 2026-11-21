package com.newuniverse.nusmp.blackclover;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.newuniverse.nusmp.blackclover.SlotClickRules.Action.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Grimoire Slot rework (docs/grimoire_slot_rework.md) moves the slot's clicks, its change signal, its scan stagger and its click rate limit into pure rules, so they can be pinned without a game: the
 * full table of what a click does, that the scan runs once per 20 ticks and never in the same tick for every player, that a bump is seen exactly once, and that a flood of clicks is cut.
 */
class SlotClickRulesTest {
    @Test
    void theClickTableIsComplete() {
        // decide(cursorOwned, cursorEmpty, slotEmpty, shift, button)
        assertEquals(NONE, SlotClickRules.decide(false, true, true, false, 0));
        assertEquals(TAKE, SlotClickRules.decide(false, true, false, false, 0));
        assertEquals(TAKE, SlotClickRules.decide(false, true, false, false, 1));
        assertEquals(PLACE, SlotClickRules.decide(true, false, true, false, 0));
        assertEquals(SWAP, SlotClickRules.decide(true, false, false, false, 0));
        assertEquals(NONE, SlotClickRules.decide(false, false, true, false, 0), "somebody else's grimoire or any item is never placed");
        assertEquals(NONE, SlotClickRules.decide(false, false, false, false, 0), "no swap with a foreign item");
        assertEquals(TO_INVENTORY, SlotClickRules.decide(false, true, false, true, 0));
        assertEquals(FROM_INVENTORY, SlotClickRules.decide(false, true, true, true, 0));
        assertEquals(NONE, SlotClickRules.decide(true, false, true, true, 0), "shift with a cursor stack does nothing");
        for (int b : new int[]{-1, 2, 3, 99}) assertEquals(NONE, SlotClickRules.decide(true, false, true, false, b), "button " + b);
    }

    @Test
    void theScanRunsOncePerSecondAndIsSpreadByUuid() {
        int phase = 13;
        int due = 0;
        for (long t = 0; t < 2000; t++) if (SlotClickRules.scanDue(t, phase)) due++;
        assertEquals(100, due, "once per 20 ticks");
        assertTrue(SlotClickRules.scanDue(7, 13) && !SlotClickRules.scanDue(8, 13));
        Set<Integer> phases = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            UUID id = new UUID(i * 0x9E3779B97F4A7C15L, i * 31L);
            int ph = SlotClickRules.phase(id);
            assertTrue(ph >= 0 && ph < SlotClickRules.SCAN_EVERY);
            phases.add(ph);
        }
        assertEquals(SlotClickRules.SCAN_EVERY, phases.size(), "all twenty phases are used, so players are not all scanned in one tick");
        // with 1000 players the busiest tick scans about 1/20 of them, never all
        int[] perTick = new int[SlotClickRules.SCAN_EVERY];
        for (int i = 0; i < 1000; i++) perTick[SlotClickRules.phase(new UUID(i * 0x9E3779B97F4A7C15L, i * 31L))]++;
        int max = 0;
        for (int n : perTick) max = Math.max(max, n);
        assertTrue(max < 120, "busiest tick scans " + max + " of 1000 players");
    }

    @Test
    void theChangeSignalSeesEachChangeOnce() {
        SlotClickRules.Signal s = new SlotClickRules.Signal();
        assertTrue(s.dirty(), "a new player is sent once");
        s.markSent();
        assertFalse(s.dirty());
        s.bump(); s.bump();
        assertTrue(s.dirty());
        s.markSent();
        assertFalse(s.dirty(), "two bumps before a send are one send");
        s.reset();
        assertTrue(s.dirty(), "respawn / dimension change sends again");
    }

    @Test
    void theRateLimitCutsAFloodAndRecovers() {
        SlotClickRules.Limiter l = new SlotClickRules.Limiter();
        int ok = 0;
        for (int i = 0; i < 100; i++) if (l.allow(1000)) ok++;
        assertEquals(SlotClickRules.CLICKS_PER_WINDOW, ok);
        assertFalse(l.allow(1000 + SlotClickRules.WINDOW_TICKS - 1));
        assertTrue(l.allow(1000 + SlotClickRules.WINDOW_TICKS), "a new window opens");
        SlotClickRules.Limiter fresh = new SlotClickRules.Limiter();
        assertTrue(fresh.allow(0), "the first click at tick 0 is allowed");
        for (int i = 0; i < 20; i++) assertTrue(new SlotClickRules.Limiter().allow(i));
        SlotClickRules.Limiter back = new SlotClickRules.Limiter();
        for (int i = 0; i < SlotClickRules.CLICKS_PER_WINDOW; i++) back.allow(500);
        assertTrue(back.allow(10), "a clock that went backwards (respawn) starts a new window");
    }
}
