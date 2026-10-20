package com.newuniverse.nusmp.client.relics.djinn.tiny;

import com.newuniverse.nusmp.client.relics.djinn.tiny.TinyDjinnActor.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Tiny Djinn's state machine: entrance, idle, the cast and the dash, the hop after a kill, the exit, and the events it reports. */
class TinyDjinnActorTest {
    private static final float[] ANCHOR = {1f, 65f, 2f}, VESSEL = {0f, 65f, 0f}, FOE = {7f, 65f, 2f};

    /** Runs the actor from {@code from} to {@code to} in half-tick frames; returns the OR of the events it reported. */
    private static int run(TinyDjinnActor a, float from, float to, float[] foe, float face) {
        int ev = 0;
        for (float t = from; t < to; t += 0.5f) {
            a.update(t, ANCHOR, VESSEL, foe, face);
            ev |= a.takeEvents();
        }
        return ev;
    }

    private static TinyDjinnActor idle() {
        TinyDjinnActor a = new TinyDjinnActor(0.25f);
        a.spawn(0f, ANCHOR, 0f);
        a.takeEvents();
        run(a, 0f, 12f, null, 0f);
        assertEquals(State.IDLE, a.state);
        return a;
    }

    @Test
    void anActorStartsGoneAndAppearsInAPoof() {
        TinyDjinnActor a = new TinyDjinnActor(0f);
        assertEquals(State.GONE, a.state);
        assertFalse(a.alive());
        a.update(5f, ANCHOR, VESSEL, null, 0f);
        assertEquals(State.GONE, a.state, "a gone actor does nothing");
        a.spawn(10f, ANCHOR, 45f);
        assertEquals(State.ENTER, a.state);
        assertTrue(a.alive());
        assertEquals("poof", a.clip());
        assertEquals(0f, a.alpha(10f), 1e-5f, "it fades in from nothing");
        assertEquals(1f, a.alpha(14f), 1e-5f);
        assertEquals(TinyDjinnActor.EV_POOF_IN, a.takeEvents());
        assertEquals(0, a.takeEvents(), "events are taken once");
        assertEquals(ANCHOR[0], a.pos[0], 1e-5f, "it appears at the shoulder");
        assertEquals(45f, a.yaw, 1e-5f);
    }

    @Test
    void theEntranceBecomesTheIdleFloat() {
        TinyDjinnActor a = new TinyDjinnActor(0.5f);
        a.spawn(0f, ANCHOR, 0f);
        a.takeEvents();
        run(a, 0f, 9f, null, 0f);
        assertEquals(State.ENTER, a.state);
        run(a, 9f, 11f, null, 0f);
        assertEquals(State.IDLE, a.state);
        assertEquals("idle", a.clip());
        assertTrue(a.clipSeconds(40f) != new TinyDjinnActor(0f).clipSeconds(40f), "figures idle out of step");
    }

    @Test
    void anIdleFigureFollowsItsAnchorAndTurnsWithItsOwner() {
        TinyDjinnActor a = idle();
        float[] far = {6f, 70f, -3f};
        for (float t = 12f; t < 80f; t += 0.5f) a.update(t, far, VESSEL, null, 120f);
        assertEquals(far[0], a.pos[0], 0.1f);
        assertEquals(far[2], a.pos[2], 0.1f);
        assertTrue(Math.abs(a.pos[1] - far[1]) < 0.1f, "the float is a few centimetres");
        assertEquals(120f, a.yaw, 1f, "it faces where its owner faces");
    }

    @Test
    void aCastIsAcceptedWhenIdleAndRefusedWhileBusy() {
        TinyDjinnActor a = new TinyDjinnActor(0f);
        assertFalse(a.cue(0f, 5), "nobody home");
        a.spawn(0f, ANCHOR, 0f);
        assertFalse(a.cue(2f, 5), "not in the first ticks of the poof");
        run(a, 0f, 8f, null, 0f);
        assertTrue(a.cue(8f, 5), "the end of the entrance is fine");
        assertEquals(State.CAST, a.state);
        assertFalse(a.cue(9f, 6), "already casting");
        assertEquals(5, a.targetId);
        a.exit(9f);
        assertEquals(State.EXIT, a.state);
        assertFalse(a.cue(10f, 7), "leaving");
    }

    @Test
    void aCastPointsBurstsThenDashesAndStrikes() {
        TinyDjinnActor a = idle();
        assertTrue(a.cue(12f, 9));
        assertEquals("cast", a.clip());
        int ev = run(a, 12f, 12f + TinyDjinnActor.CAST_BURST_AT - 0.6f, FOE, 0f);
        assertEquals(0, ev & TinyDjinnActor.EV_CAST, "no burst before the pose");
        ev = run(a, 12f + TinyDjinnActor.CAST_BURST_AT - 0.6f, 12f + TinyDjinnActor.CAST_BURST_AT + 1f, FOE, 0f);
        assertEquals(TinyDjinnActor.EV_CAST, ev & TinyDjinnActor.EV_CAST, "the burst when it points");
        assertEquals(State.CAST, a.state);
        assertEquals(-90f, a.yaw, 25f, "it turns to the foe (east of it is -90)");
        run(a, 12f + TinyDjinnActor.CAST_BURST_AT + 1f, 12f + TinyDjinnActor.CAST_TICKS + 0.5f, FOE, 0f);
        assertEquals(State.ATTACK, a.state);
        assertEquals("attack", a.clip());
        ev = run(a, 12f + TinyDjinnActor.CAST_TICKS + 0.5f, 12f + TinyDjinnActor.CAST_TICKS + TinyDjinnMath.DASH_OUT + 1f, FOE, 0f);
        assertEquals(TinyDjinnActor.EV_STRIKE, ev & TinyDjinnActor.EV_STRIKE, "it reaches the foe");
        float dx = FOE[0] - a.pos[0], dz = FOE[2] - a.pos[2];
        assertTrue(Math.sqrt(dx * dx + dz * dz) < 2.2, "at the foe, short of it: " + dx + "," + dz);
        run(a, 12f + TinyDjinnActor.CAST_TICKS + TinyDjinnMath.DASH_OUT + 1f, 12f + TinyDjinnActor.CAST_TICKS + TinyDjinnActor.ATTACK_TICKS + 20f, FOE, 0f);
        assertEquals(State.IDLE, a.state, "back to floating");
        assertEquals(ANCHOR[0], a.pos[0], 0.15f, "and back at the shoulder");
    }

    @Test
    void theKillCheckComesOnceAfterTheStrikeAndAKillBringsAHop() {
        TinyDjinnActor a = idle();
        a.cue(12f, 3);
        float t = 12f;
        int checks = 0;
        boolean hopped = false;
        for (; t < 80f; t += 0.5f) {
            a.update(t, ANCHOR, VESSEL, FOE, 0f);
            if (a.takeEvents() == TinyDjinnActor.EV_HOP) hopped = true;
            if (a.killCheckDue(t)) {
                checks++;
                a.markKill(t);
            }
        }
        assertEquals(1, checks, "asked once per attack");
        assertTrue(hopped, "a kill is celebrated");
        assertEquals(State.IDLE, a.state, "and then it floats again");
    }

    @Test
    void withoutAKillThereIsNoHop() {
        TinyDjinnActor a = idle();
        a.cue(12f, 3);
        boolean hopped = false;
        for (float t = 12f; t < 80f; t += 0.5f) {
            a.update(t, ANCHOR, VESSEL, FOE, 0f);
            if ((a.takeEvents() & TinyDjinnActor.EV_HOP) != 0) hopped = true;
            a.killCheckDue(t);
        }
        assertFalse(hopped);
    }

    @Test
    void aKillNoticedWhileIdleHopsAtOnce() {
        TinyDjinnActor a = idle();
        a.markKill(20f);
        assertEquals(State.HOP, a.state);
        assertEquals("hop", a.clip());
        assertEquals(TinyDjinnActor.EV_HOP, a.takeEvents());
        run(a, 20f, 20f + TinyDjinnActor.HOP_TICKS + 1f, null, 0f);
        assertEquals(State.IDLE, a.state);
    }

    @Test
    void theExitSpiralsIntoTheVesselAndEndsGone() {
        TinyDjinnActor a = idle();
        a.exit(30f);
        assertTrue(a.leaving());
        assertEquals("exit", a.clip());
        int ev = 0;
        float closest = 99f;
        for (float t = 30f; t < 30f + TinyDjinnActor.EXIT_TICKS + 2f; t += 0.5f) {
            a.update(t, ANCHOR, VESSEL, null, 0f);
            ev |= a.takeEvents();
            if (a.alive()) closest = Math.min(closest, (float) Math.abs(a.pos[0] - VESSEL[0]));
        }
        assertFalse(a.alive());
        assertEquals(State.GONE, a.state);
        assertTrue((ev & TinyDjinnActor.EV_SPIRAL) != 0, "motes on the way");
        assertTrue((ev & TinyDjinnActor.EV_POOF_OUT) != 0, "a last puff at the vessel");
        assertTrue(closest < 0.2f, "it ended in the vessel: " + closest);
        assertEquals(0f, a.alpha(30f + TinyDjinnActor.EXIT_TICKS), 1e-4f, "faded out");
        a.exit(100f);                                                 // again: nothing
        assertEquals(State.GONE, a.state);
    }

    @Test
    void anExitInTheMiddleOfAnAttackStillLeaves() {
        TinyDjinnActor a = idle();
        a.cue(12f, 3);
        run(a, 12f, 30f, FOE, 0f);
        a.exit(30f);
        run(a, 30f, 50f, null, 0f);
        assertEquals(State.GONE, a.state);
    }

    @Test
    void vanishRemovesItAtOnceWithoutEvents() {
        TinyDjinnActor a = idle();
        a.vanish();
        assertFalse(a.alive());
        assertEquals(0, a.takeEvents());
    }

    @Test
    void theClipsAreTheSixOfTheAnimationFile() {
        TinyDjinnActor a = new TinyDjinnActor(0f);
        java.util.Set<String> names = new java.util.HashSet<>(java.util.List.of(TinyDjinnAssets.CLIPS));
        a.spawn(0f, ANCHOR, 0f);
        names.remove(a.clip());
        run(a, 0f, 11f, null, 0f);
        names.remove(a.clip());
        a.cue(11f, 1);
        names.remove(a.clip());
        run(a, 11f, 11f + TinyDjinnActor.CAST_TICKS + 1f, FOE, 0f);
        names.remove(a.clip());
        a.markKill(40f);
        run(a, 11f + TinyDjinnActor.CAST_TICKS + 1f, 60f, FOE, 0f);
        a.markKill(60f);
        names.remove(a.clip());
        a.exit(61f);
        names.remove(a.clip());
        assertTrue(names.isEmpty(), "never asked for: " + names);
    }

    @Test
    void theGlowBreathesAndFlaresInAFight() {
        TinyDjinnActor a = idle();
        assertTrue(a.glow(100f) >= 0.5f && a.glow(100f) <= 1.0f);
        a.cue(12f, 3);
        run(a, 12f, 30f, FOE, 0f);
        assertTrue(a.glow(30f) >= 1.0f, "brighter while it fights");
    }

    @Test
    void theAttackLengthMatchesTheClipAndTheServersLeadTime() {
        // the server cues 16 ticks before the bolt: 12 ticks of cast and 4 of dash out
        assertEquals(16f, TinyDjinnActor.CAST_TICKS + TinyDjinnMath.DASH_OUT, 1e-5f);
        assertEquals(12f, TinyDjinnActor.ATTACK_TICKS, 1e-5f);
        assertEquals(10f, TinyDjinnActor.ENTER_TICKS, 1e-5f, "the poof clip is 0.5 s");
        assertEquals(12f, TinyDjinnActor.EXIT_TICKS, 1e-5f, "the exit clip is 0.6 s");
        assertEquals(16f, TinyDjinnActor.HOP_TICKS, 1e-5f, "the hop clip is 0.8 s");
    }
}
