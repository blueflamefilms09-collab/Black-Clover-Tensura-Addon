package com.newuniverse.nusmp.arc.kakashi;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Kakashi boss (docs/bosses/kakashi.md): phases, the Kamui window, the Susanoo shield, the copy ledger, the layouts and the bounds. */
class KakashiMathTest {
    @Test
    void phasesFollowTheThresholdsAndNeverGoBack() {
        assertEquals(KakashiMath.COPY, KakashiMath.phaseOf(1.0f, 0));
        assertEquals(KakashiMath.COPY, KakashiMath.phaseOf(0.76f, 0));
        assertEquals(KakashiMath.PACK, KakashiMath.phaseOf(0.75f, 0));
        assertEquals(KakashiMath.MANGEKYO, KakashiMath.phaseOf(0.50f, 0));
        assertEquals(KakashiMath.SUSANOO, KakashiMath.phaseOf(0.25f, 0));
        assertEquals(KakashiMath.SUSANOO, KakashiMath.phaseOf(0.9f, KakashiMath.SUSANOO), "a heal does not take a phase back");
        assertEquals(KakashiMath.PHASES, KakashiMath.PHASE_NAMES.length);
        assertEquals(KakashiMath.PHASES - 1, KakashiMath.THRESHOLDS.length);
    }

    @Test
    void everyMoveOfTheBookOpensInAKnownPhaseAndHasAVoiceGroup() {
        assertEquals(15, KakashiMath.BOOK.size());
        assertEquals(KakashiMath.BOOK.size(), List.copyOf(new java.util.LinkedHashSet<>(KakashiMath.BOOK)).size(), "no duplicate move id");
        for (String m : KakashiMath.BOOK) {
            int p = KakashiMath.openingPhase(m);
            assertTrue(p >= 0 && p < KakashiMath.PHASES, m);
            assertNotNull(KakashiMoves.voiceGroup(m), m + " has a voice group");
        }
        assertEquals(99, KakashiMath.openingPhase("not_a_move"));
    }

    @Test
    void oneHitIsCappedByTheVictimsMaximum() {
        assertEquals(30f, KakashiMath.capHit(500f, 100f, 0.30), 1e-4);
        assertEquals(10f, KakashiMath.capHit(10f, 100f, 0.30), 1e-4);
        assertEquals(1f, KakashiMath.capHit(50f, 2f, 0.10), 1e-4, "never below 1");
        assertEquals(1.35f, KakashiMath.lightningFactor(true), 1e-4);
        assertEquals(1.0f, KakashiMath.lightningFactor(false), 1e-4);
    }

    @Test
    void kamuiIsOnlyForTheMangekyoPhaseAndNeverWhileHeIsWindingUp() {
        KakashiMath.Kamui k = new KakashiMath.Kamui();
        assertFalse(k.tryPhase(100, KakashiMath.PACK, true), "not before the Mangekyo phase");
        assertFalse(k.tryPhase(100, KakashiMath.MANGEKYO, false), "the setting can switch it off");
        k.moveStarted(100, 30);
        assertFalse(k.tryPhase(120, KakashiMath.MANGEKYO, true), "solid while winding up");
        assertFalse(k.tryPhase(100 + 30 + KakashiMath.Kamui.SOLID_AFTER_MOVE - 1, KakashiMath.MANGEKYO, true), "solid while recovering");
        long t = 100 + 30 + KakashiMath.Kamui.SOLID_AFTER_MOVE;
        assertTrue(k.tryPhase(t, KakashiMath.MANGEKYO, true));
        assertTrue(k.intangible(t + KakashiMath.Kamui.WINDOW - 1));
        assertFalse(k.intangible(t + KakashiMath.Kamui.WINDOW), "the window ends");
        assertFalse(k.tryPhase(t + KakashiMath.Kamui.WINDOW + 1, KakashiMath.MANGEKYO, true), "cooldown");
        assertTrue(k.tryPhase(t + KakashiMath.Kamui.COOLDOWN, KakashiMath.MANGEKYO, true), "ready again");
        k.reset();
        assertEquals(0L, k.readyAt());
    }

    @Test
    void theSusanooShieldAbsorbsThenLetsTheRestThrough() {
        float[] r = KakashiMath.absorb(100f, 30f);
        assertEquals(0f, r[0], 1e-4);
        assertEquals(70f, r[1], 1e-4);
        r = KakashiMath.absorb(20f, 50f);
        assertEquals(30f, r[0], 1e-4);
        assertEquals(0f, r[1], 1e-4);
        assertTrue(KakashiMath.kawarimi(60f, 1000f, 500, 400));
        assertFalse(KakashiMath.kawarimi(59f, 1000f, 500, 400), "too small a hit");
        assertFalse(KakashiMath.kawarimi(600f, 1000f, 300, 400), "on cooldown");
    }

    @Test
    void theLedgerIsBoundedExpiresAndPicksTheStrongest() {
        KakashiMath.Ledger l = new KakashiMath.Ledger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertNull(l.strongest(0));
        l.record(a, 40f, 10);
        l.record(b, 90f, 20);
        l.record(null, 500f, 20);
        l.record(a, -5f, 20);
        assertEquals(2, l.size());
        assertEquals(b, l.strongest(30));
        assertNull(l.strongest(20 + KakashiMath.Ledger.KEEP + 10), "stale hits are not copied");
        for (int i = 0; i < 40; i++) l.record(UUID.randomUUID(), 1f + i, 100 + i);
        assertEquals(KakashiMath.Ledger.SIZE, l.size(), "at most eight players are remembered");
        l.forget(a);
        l.clear();
        assertEquals(0, l.size());
        assertEquals(20f, KakashiMath.copied(1000f, 1000f), 1e-4, "a copy never exceeds two percent of his own health");
        assertEquals(9f, KakashiMath.copied(10f, 1000f), 1e-4);
    }

    @Test
    void theLayoutsKeepTheirPromises() {
        double[][] marks = KakashiMath.cloneMarks(0.3, 6.0);
        assertEquals(3, marks.length);
        for (double[] m : marks) assertEquals(6.0, Math.hypot(m[0], m[1]), 1e-9);
        double[][] cross = KakashiMath.shurikenCross(8.0);
        assertEquals(4, cross.length);
        for (double[] s : cross) assertTrue(KakashiMath.inShurikenLane(0, 0, s, 1.0), "the middle is in every lane");
        double d = 8.0 * Math.sqrt(0.5);
        for (double[] s : cross) assertFalse(KakashiMath.inShurikenLane(d, d, s, 1.5), "the diagonals are the safe lanes");
        double[][] path = KakashiMath.dragonPath(20.0, 9, 3.0, 0.0);
        assertEquals(9, path.length);
        assertEquals(0.0, path[0][0], 1e-9);
        assertEquals(20.0, path[8][0], 1e-9);
        assertEquals(0.0, path[0][1], 1e-9);
        assertEquals(0.0, path[8][1], 1e-6);
        List<Integer> t = KakashiMath.purpleTargets(new double[]{12, 3, 8, 20, 5}, 3);
        assertEquals(List.of(1, 4, 2), t);
        assertEquals(1, KakashiMath.purpleTargets(new double[]{7}, 3).size());
    }

    @Test
    void theFangSteersWithALimitedTurn() {
        double h = KakashiMath.steer(0.0, Math.PI, KakashiMath.FANG_TURN);
        assertEquals(KakashiMath.FANG_TURN, h, 1e-9);
        assertEquals(0.05, KakashiMath.steer(0.0, 0.05, KakashiMath.FANG_TURN), 1e-9, "a small error is closed at once");
        assertTrue(KakashiMath.steer(3.0, -3.0, KakashiMath.FANG_TURN) > 3.0, "it turns the short way round");
    }

    @Test
    void theHoundsAreEightAndBounded() {
        assertEquals(8, KakashiMath.PACK_ROSTER.size());
        assertTrue(KakashiMath.packTotal() < 4.0f, "one pursuit stays below four times the base hit on one foe");
        int last = -1;
        for (KakashiMath.Hound h : KakashiMath.PACK_ROSTER) {
            assertTrue(h.delay() > last, "launched in order");
            last = h.delay();
        }
        assertTrue(KakashiMath.MAX_VFX_PER_MOVE <= 6 && KakashiMath.MAX_POINTS <= 24 && KakashiMath.MAX_FOES <= 12);
    }
}
