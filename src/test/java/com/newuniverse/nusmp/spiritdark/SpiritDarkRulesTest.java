package com.newuniverse.nusmp.spiritdark;

import com.newuniverse.nusmp.spiritdark.SpiritDarkSoundRules.Cue;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Spirit of Darkness: the life steal caps, the per target mark, the prop word, the anchors, the sound gate and the caps ledger. */
class SpiritDarkRulesTest {
    @Test
    void aStrikeNeverGivesBackMoreThanItsCapNorANegativeAmount() {
        assertEquals(SpiritDarkRules.STEAL_HIT_CAP, SpiritDarkRules.stealOfHit(1000f, false));
        assertEquals(SpiritDarkRules.STEAL_HIT_CAP_POWERED, SpiritDarkRules.stealOfHit(1000f, true));
        assertEquals(0f, SpiritDarkRules.stealOfHit(-4f, false));
        assertEquals(0f, SpiritDarkRules.stealOfHit(Float.NaN, false));
        assertEquals(0f, SpiritDarkRules.stealOfHit(Float.POSITIVE_INFINITY, false));
        assertEquals(1.2f, SpiritDarkRules.stealOfHit(10f, false), 1e-5f);
        assertTrue(SpiritDarkRules.STEAL_FRACTION <= 0.2f, "a small share of the damage, never all of it");
    }

    @Test
    void theSecondCapHoldsAndRefillsEachSecond() {
        SpiritDarkRules.StealWindow w = new SpiritDarkRules.StealWindow();
        float total = 0f;
        for (int i = 0; i < 40; i++) total += w.grant(1000 + i / 4, 1.5f, false);                  // 40 strikes inside ten ticks
        assertTrue(total <= SpiritDarkRules.STEAL_SECOND_CAP + 1e-4f, "capped per second: " + total);
        assertEquals(SpiritDarkRules.STEAL_SECOND_CAP, total, 1e-4f);
        assertEquals(1.5f, w.grant(1000 + SpiritDarkRules.STEAL_WINDOW, 1.5f, false), 1e-5f, "a new second, a new allowance");
        SpiritDarkRules.StealWindow p = new SpiritDarkRules.StealWindow();
        float tp = 0f;
        for (int i = 0; i < 20; i++) tp += p.grant(50, 2.0f, true);
        assertEquals(SpiritDarkRules.STEAL_SECOND_CAP_POWERED, tp, 1e-4f);
        assertEquals(0f, new SpiritDarkRules.StealWindow().grant(0, 0f, false));
        assertEquals(0f, new SpiritDarkRules.StealWindow().grant(0, -1f, false));
    }

    @Test
    void aTargetIsMarkedOnceEveryFiveSecondsAndTheListStaysSmall() {
        SpiritDarkRules.TargetLedger t = new SpiritDarkRules.TargetLedger();
        UUID a = UUID.randomUUID();
        assertEquals(0, t.tryMark(a, 100));
        assertEquals(-1, t.tryMark(a, 100 + SpiritDarkRules.TARGET_COOLDOWN - 1));
        assertEquals(1, t.tryMark(a, 100 + SpiritDarkRules.TARGET_COOLDOWN));
        assertEquals(-1, t.tryMark(null, 0));
        for (int i = 0; i < 100; i++) t.tryMark(UUID.randomUUID(), 1000);
        assertTrue(t.size() <= SpiritDarkRules.MAX_TRACKED_TARGETS, "bounded: " + t.size());
    }

    @Test
    void theMarksAlternateWitherThenBlindnessAndAreShort() {
        assertEquals(SpiritDarkRules.FX_WITHER, SpiritDarkRules.effectOf(0));
        assertEquals(SpiritDarkRules.FX_BLIND, SpiritDarkRules.effectOf(1));
        assertEquals(SpiritDarkRules.FX_WITHER, SpiritDarkRules.effectOf(2));
        assertTrue(SpiritDarkRules.WITHER_TICKS_POWERED <= 100 && SpiritDarkRules.BLIND_TICKS <= 60, "short durations");
    }

    @Test
    void thePropWordRoundTripsEveryPoseAndFace() {
        for (DarkMoodRules.Pose pose : DarkMoodRules.Pose.values()) {
            for (DarkMoodRules.Face face : DarkMoodRules.Face.values()) {
                for (int variant : new int[]{SpiritDarkRules.V_COMPANION, SpiritDarkRules.V_POWERED}) {
                    int w = SpiritDarkRules.param(variant, variant == 0, pose, face, 987_654L);
                    assertEquals(variant, SpiritDarkRules.variant(w));
                    assertEquals(variant == 0, SpiritDarkRules.hidden(w));
                    assertEquals(pose, SpiritDarkRules.pose(w));
                    assertEquals(face, SpiritDarkRules.face(w));
                    assertEquals(987_654 & SpiritDarkRules.STAMP_MASK, SpiritDarkRules.stamp(w));
                }
            }
        }
        assertEquals(Integer.MAX_VALUE, SpiritDarkRules.since(SpiritDarkRules.plain(1, false), 500), "never set");
        int w = SpiritDarkRules.param(0, false, DarkMoodRules.Pose.BOW, DarkMoodRules.Face.SMUG, 1000L);
        assertEquals(30, SpiritDarkRules.since(w, 1030));
        assertEquals(DarkMoodRules.Face.GRIN, SpiritDarkRules.face(SpiritDarkRules.withFace(w, DarkMoodRules.Face.GRIN)));
        assertEquals(DarkMoodRules.Pose.BOW, SpiritDarkRules.pose(SpiritDarkRules.withFace(w, DarkMoodRules.Face.GRIN)));
        assertTrue(SpiritDarkRules.hidden(SpiritDarkRules.withHidden(w, true)) && !SpiritDarkRules.hidden(SpiritDarkRules.withHidden(SpiritDarkRules.withHidden(w, true), false)));
        assertEquals(1, SpiritDarkRules.stamp(SpiritDarkRules.param(0, false, DarkMoodRules.Pose.BOW, DarkMoodRules.Face.SMUG, 0L)), "0 is reserved for never set");
        int wrapped = SpiritDarkRules.param(0, false, DarkMoodRules.Pose.BOW, DarkMoodRules.Face.SMUG, SpiritDarkRules.STAMP_MASK - 5L);
        assertEquals(10, SpiritDarkRules.since(wrapped, SpiritDarkRules.STAMP_MASK + 5L), "the 20 bit stamp wraps cleanly");
    }

    @Test
    void theFormsHoverNearTheirOwnerAndOnTheRightSides() {
        for (double yaw = -180; yaw <= 180; yaw += 30) {
            double[] c = SpiritDarkRules.companion(10, 64, -3, yaw), p = SpiritDarkRules.powered(10, 64, -3, yaw);
            assertTrue(SpiritDarkRules.anchorDistance(c, 10, 64, -3) < SpiritDarkRules.MAX_ANCHOR_DISTANCE);
            assertTrue(SpiritDarkRules.anchorDistance(p, 10, 64, -3) < SpiritDarkRules.MAX_ANCHOR_DISTANCE);
        }
        double[] c = SpiritDarkRules.companion(0, 0, 0, 0), p = SpiritDarkRules.powered(0, 0, 0, 0);
        assertTrue(c[0] > 0.3 && c[1] > 1.6, "the companion is above the left shoulder (left is +x at yaw 0)");
        assertTrue(p[2] < -1.0, "the powered form is behind the owner (the owner faces +z at yaw 0)");
        double[] t = new double[3];
        SpiritDarkRules.companionTo(t, 0, 0, 0, 0);
        assertEquals(c[0], t[0]);
        assertEquals(c[2], t[2]);
        SpiritDarkRules.poweredTo(t, 0, 0, 0, 90);
        assertTrue(t[0] > 1.0, "turned to face -x the form is behind at +x");
    }

    @Test
    void theKeyPacketOnlyAcceptsItsThreeActionsAndStepsAreCooldownBound() {
        for (int a = -3; a < 12; a++) assertEquals(a >= 0 && a <= 2, SpiritDarkRules.validAction(a), "action " + a);
        assertTrue(SpiritDarkRules.stepCooldown(false) >= 160 && SpiritDarkRules.stepCooldown(false) <= 200, "about 9 s");
        assertTrue(SpiritDarkRules.stepCooldown(true) < SpiritDarkRules.stepCooldown(false) && SpiritDarkRules.stepCooldown(true) >= 120);
        assertTrue(SpiritDarkRules.STEP_RANGE_MAX <= 16 && SpiritDarkRules.STEP_RANGE_MAX >= 12 && SpiritDarkRules.DARK_LIGHT_MAX <= 8);
        assertTrue(SpiritDarkRules.POWER_COOLDOWN > SpiritDarkRules.POWER_TICKS * 3, "the demon cannot be kept out all the time");
        assertTrue(SpiritDarkRules.gapOk(10, 0, 10) && !SpiritDarkRules.gapOk(9, 0, 10));
    }

    @Test
    void theCapLedgerHoldsPerPlayerAndServerWide() {
        DarkCapLedger l = new DarkCapLedger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(l.acquire(a, 1, 2));
        assertFalse(l.acquire(a, 1, 2), "one per player");
        assertTrue(l.acquire(b, 1, 2));
        assertFalse(l.acquire(c, 1, 2), "two on the whole server");
        l.forget(a);
        assertEquals(1, l.total());
        assertTrue(l.acquire(c, 1, 2));
        l.release(c);
        l.release(c);
        assertEquals(1, l.total(), "release clamps at zero for a player");
        assertFalse(l.acquire(null, 1, 2));
        assertFalse(l.acquire(a, 0, 2));
    }

    @Test
    void theSoundGateHoldsTheGapPerSourceAndTheServerWideAllowance() {
        SpiritDarkSoundRules.Gate g = new SpiritDarkSoundRules.Gate();
        UUID a = UUID.randomUUID();
        assertTrue(g.tryPlay(a, Cue.WHISPER, 1000));
        assertFalse(g.tryPlay(a, Cue.WHISPER, 1000 + Cue.WHISPER.gap - 1));
        assertTrue(g.tryPlay(a, Cue.WHISPER, 1000 + Cue.WHISPER.gap));
        assertTrue(g.tryPlay(a, Cue.STEP, 1001), "another cue has its own gap");
        int played = 0;
        for (int i = 0; i < 50; i++) if (g.tryPlay(UUID.randomUUID(), Cue.CHIME, 5000)) played++;
        assertEquals(SpiritDarkSoundRules.PER_TICK, played, "at most " + SpiritDarkSoundRules.PER_TICK + " sounds in one tick for the whole server");
        g.forget(a);
        assertFalse(g.tryPlay(null, Cue.CHIME, 9999));
        for (Cue c : Cue.values()) {
            assertTrue(c.range >= 12 && c.range <= 40, c.id);
            assertTrue(SpiritDarkSoundRules.pitch(c, 0f) >= c.pitchLo && SpiritDarkSoundRules.pitch(c, 1f) <= c.pitchHi + 1e-6f);
            assertEquals("spirit.dark." + c.id, c.eventPath());
        }
    }
}
