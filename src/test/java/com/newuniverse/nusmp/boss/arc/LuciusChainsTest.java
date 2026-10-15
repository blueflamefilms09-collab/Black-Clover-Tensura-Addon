package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The chains as data: fairness rule, the Checkmate order, the pick per act, and the runner (events, abort, save/resume). */
class LuciusChainsTest {
    @Test
    void everyChainIsFair() {
        for (LuciusChains.Chain c : LuciusChains.all()) assertTrue(LuciusChains.valid(c), c.id());
    }

    @Test
    void aHurtingStepWithAShortWarningIsRefused() {
        LuciusChains.Chain bad = new LuciusChains.Chain("bad", 0b10, 100, 1, List.of(new LuciusChains.Step(LuciusChains.Kind.SOUL_TOUCH, 11, 0, true)));
        assertFalse(LuciusChains.valid(bad));
        LuciusChains.Chain ok = new LuciusChains.Chain("ok", 0b10, 100, 1, List.of(new LuciusChains.Step(LuciusChains.Kind.SOUL_TOUCH, 12, 0, true)));
        assertTrue(LuciusChains.valid(ok));
        assertFalse(LuciusChains.valid(new LuciusChains.Chain("empty", 0b10, 1, 1, List.of())));
    }

    @Test
    void checkmateIsGravityThenSpaceThenTime() {
        LuciusChains.Chain c = LuciusChains.byId(LuciusChains.CHECKMATE);
        assertEquals(List.of(LuciusChains.Kind.GRAVITY_PIN, LuciusChains.Kind.SPATIAL_BLINK, LuciusChains.Kind.CHRONO_STASIS),
                c.steps().stream().map(LuciusChains.Step::kind).toList());
        assertFalse(c.usable(1), "the Untouchable Messiah does not come close enough for it");
        assertTrue(c.usable(2) && c.usable(3));
    }

    @Test
    void theSoulTouchChainsEndWithTheTouch() {
        for (String id : List.of(LuciusChains.CHECKMATE_SOUL, LuciusChains.SOUL_TOUCH_FOLLOWUP, LuciusChains.CRUSH_BLINK_TOUCH)) {
            LuciusChains.Chain c = LuciusChains.byId(id);
            assertEquals(LuciusChains.Kind.SOUL_TOUCH, c.steps().get(c.steps().size() - 1).kind(), id);
            assertFalse(c.usable(1), id + " is never an act 1 chain");
        }
    }

    @Test
    void actOneNeverMeleesAndActTwoNeverClones() {
        for (LuciusChains.Chain c : LuciusChains.all()) {
            if (c.usable(1)) for (LuciusChains.Step s : c.steps()) assertTrue(s.kind() != LuciusChains.Kind.SOUL_TOUCH, c.id());
            if (c.usable(2)) for (LuciusChains.Step s : c.steps()) assertTrue(s.kind() != LuciusChains.Kind.CLONE_WAVE, c.id());
        }
        assertTrue(LuciusChains.byId(LuciusChains.CLONE_WAVE).usable(3));
        assertTrue(LuciusChains.byId(LuciusChains.RETREAT).usable(1));
    }

    @Test
    void pickRespectsActAndCooldownAndNeverDrawsTheRetreat() {
        Random r = new Random(7);
        Map<String, Long> ready = new HashMap<>();
        for (int i = 0; i < 300; i++) {
            LuciusChains.Chain c = LuciusChains.pick(1, 0, ready, r, 20).orElseThrow();
            assertTrue(c.usable(1));
            assertTrue(!LuciusChains.RETREAT.equals(c.id()));
        }
        for (LuciusChains.Chain c : LuciusChains.all()) ready.put(c.id(), 1000L);
        assertTrue(LuciusChains.pick(2, 10, ready, r, 3).isEmpty(), "all on cooldown");
        assertTrue(LuciusChains.pick(2, 1000, ready, r, 3).isPresent(), "ready at the tick");
    }

    @Test
    void closeTargetsDrawTheTouchMoreOften() {
        Map<String, Long> ready = new HashMap<>();
        int closeTouch = 0, farTouch = 0;
        Random r1 = new Random(1), r2 = new Random(1);
        for (int i = 0; i < 4000; i++) {
            if (touches(LuciusChains.pick(2, 0, ready, r1, 4).orElseThrow())) closeTouch++;
            if (touches(LuciusChains.pick(2, 0, ready, r2, 30).orElseThrow())) farTouch++;
        }
        assertTrue(closeTouch > farTouch * 1.3, closeTouch + " vs " + farTouch);
    }

    private static boolean touches(LuciusChains.Chain c) { return c.steps().stream().anyMatch(s -> s.kind() == LuciusChains.Kind.SOUL_TOUCH); }

    @Test
    void runnerWalksBeginFireDoneWithTheTelegraphInBetween() {
        LuciusChains.Runner run = new LuciusChains.Runner();
        LuciusChains.Chain c = LuciusChains.byId(LuciusChains.CHECKMATE);
        assertTrue(run.start(c, 100));
        assertTrue(run.active());
        assertFalse(run.start(c, 100), "one chain at a time");
        List<String> log = new ArrayList<>();
        int tick = 0, beginAt = -1;
        int[] fireDelay = new int[3];
        while (run.active() && tick < 1000) {
            LuciusChains.Event e = run.tick();
            tick++;
            if (e.type() == LuciusChains.EventType.BEGIN) { beginAt = tick; log.add("B" + e.step()); }
            if (e.type() == LuciusChains.EventType.FIRE) { fireDelay[e.step()] = tick - beginAt; log.add("F" + e.step()); }
            if (e.type() == LuciusChains.EventType.DONE) log.add("D");
        }
        assertEquals(List.of("B0", "F0", "B1", "F1", "B2", "F2", "D"), log);
        assertEquals(20, fireDelay[0]);
        assertEquals(12, fireDelay[1]);
        assertEquals(16, fireDelay[2]);
        assertFalse(run.active());
        assertEquals(100L + c.cooldown(), (long) run.readyAt().get(LuciusChains.CHECKMATE));
    }

    @Test
    void abortStopsEverythingAndKeepsTheCooldown() {
        LuciusChains.Runner run = new LuciusChains.Runner();
        run.start(LuciusChains.byId(LuciusChains.CHECKMATE), 5);
        run.tick();
        run.abort();
        assertFalse(run.active());
        assertEquals(LuciusChains.EventType.NONE, run.tick().type());
        assertTrue(run.readyAt().containsKey(LuciusChains.CHECKMATE));
    }

    @Test
    void savedProgressResumesAtTheStepWithAFreshWarning() {
        LuciusChains.Runner run = new LuciusChains.Runner();
        run.start(LuciusChains.byId(LuciusChains.CHECKMATE), 0);
        for (int i = 0; i < 500 && run.stepIndex() < 1; i++) run.tick();
        run.tick();
        LuciusChains.Saved s = run.save();
        assertEquals(LuciusChains.CHECKMATE, s.chainId());
        assertEquals(1, s.step());
        LuciusChains.Runner back = new LuciusChains.Runner();
        assertTrue(back.restore(s, 0));
        assertEquals(1, back.stepIndex());
        LuciusChains.Event first = back.tick();
        assertEquals(LuciusChains.EventType.BEGIN, first.type());
        assertEquals(LuciusChains.Kind.SPATIAL_BLINK, first.kind());
        int ticks = 0;
        while (back.tick().type() != LuciusChains.EventType.FIRE) ticks++;
        assertEquals(11, ticks, "twelve ticks of warning from the begin");
        assertNull(new LuciusChains.Runner().save());
    }

    @Test
    void restoreOfAnUnknownChainFails() {
        LuciusChains.Runner run = new LuciusChains.Runner();
        assertFalse(run.restore(new LuciusChains.Saved("gone", 0, 3), 0));
        assertFalse(run.restore(new LuciusChains.Saved(LuciusChains.CHECKMATE, 9, 3), 0));
        assertFalse(run.active());
    }
}
