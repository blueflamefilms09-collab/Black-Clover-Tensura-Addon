package com.newuniverse.nusmp.gnome;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Gnome: the prop word, the clips and their ticks, the caps, the cooldown, the places, the emitter. */
class GnomeRulesTest {
    private static void same(double[] e, double[] a, String m) {
        assertEquals(e.length, a.length, m);
        for (int i = 0; i < e.length; i++) assertEquals(e[i], a[i], 1e-9, m + " [" + i + "]");
    }

    @Test
    void thePropWordRoundTripsAndTheStampWrapsOverTheClock() {
        for (int variant : new int[]{GnomeRules.V_COMPANION, GnomeRules.V_FULL}) {
            for (int stamp : new int[]{0, 1, 77, GnomeRules.STAMP_MASK}) {
                int w = GnomeRules.param(variant, stamp);
                assertEquals(variant, GnomeRules.variant(w));
                assertEquals(stamp, GnomeRules.stamp(w));
                assertFalse(GnomeRules.hidden(w));
                assertTrue(GnomeRules.hidden(w | GnomeRules.HIDDEN), "the hidden bit is its own");
                assertEquals(stamp, GnomeRules.stamp(w | GnomeRules.HIDDEN), "hiding keeps the stamp");
            }
        }
        assertEquals(1, GnomeRules.stampOf(0), "a stamp is never 0 (0 means no cast)");
        assertEquals(1, GnomeRules.stampOf(1L << 20));
        long now = (1L << 20) + 5;
        assertEquals(10, GnomeRules.sinceStamp(now, GnomeRules.stampOf(now - 10)), "ticks since the cast across the wrap of the 20 bit clock");
        assertEquals(Integer.MAX_VALUE, GnomeRules.sinceStamp(now, 0));
    }

    @Test
    void theFullFormRisesStandsCastsAndSinksOnItsOwnTicks() {
        int max = GnomeRules.FULL_TICKS;
        assertEquals(GnomeRules.CLIP_RISE, GnomeRules.fullClip(0, max, -1));
        assertEquals(GnomeRules.CLIP_RISE, GnomeRules.fullClip(GnomeRules.FULL_RISE_TICKS - 1, max, -1));
        assertEquals(GnomeRules.CLIP_IDLE, GnomeRules.fullClip(GnomeRules.FULL_RISE_TICKS, max, -1));
        assertEquals(GnomeRules.CLIP_CAST, GnomeRules.fullClip(60, max, 3), "a cast while it stands plays the cast clip");
        assertEquals(GnomeRules.CLIP_IDLE, GnomeRules.fullClip(60, max, GnomeRules.CAST_CLIP_TICKS), "the cast clip is 24 ticks long");
        assertEquals(GnomeRules.CLIP_SINK, GnomeRules.fullClip(max - GnomeRules.FULL_SINK_TICKS, max, 3), "the sink wins over a late cast");
        assertEquals(GnomeRules.CLIP_RISE, GnomeRules.fullClip(2, max, 1), "the rise wins over a cast at the start");
        assertEquals(max, GnomeRules.FULL_RISE_TICKS + 80 + GnomeRules.FULL_SINK_TICKS, "rise 20 + hold 80 + sink 20 = 120 ticks (6 seconds)");
        assertTrue(GnomeRules.FULL_SLAM_TICK < GnomeRules.FULL_RISE_TICKS, "the fists land inside the rise clip");
        assertTrue(GnomeRules.CAST_CONTACT_TICK < GnomeRules.CAST_CLIP_TICKS);
    }

    @Test
    void theCompanionWalksWhenItsMasterMovesAndCastsForOneClip() {
        assertEquals(GnomeRules.CLIP_IDLE, GnomeRules.companionClip(false, Integer.MAX_VALUE));
        assertEquals(GnomeRules.CLIP_WALK, GnomeRules.companionClip(true, Integer.MAX_VALUE));
        assertEquals(GnomeRules.CLIP_CAST, GnomeRules.companionClip(true, 5), "the cast wins over the walk");
        assertEquals(GnomeRules.CLIP_WALK, GnomeRules.companionClip(true, GnomeRules.CAST_CLIP_TICKS));
        assertTrue(GnomeRules.slamNow(GnomeRules.CLIP_RISE, GnomeRules.FULL_SLAM_TICK));
        assertTrue(GnomeRules.slamNow(GnomeRules.CLIP_CAST, GnomeRules.CAST_CONTACT_TICK));
        assertFalse(GnomeRules.slamNow(GnomeRules.CLIP_IDLE, GnomeRules.CAST_CONTACT_TICK));
        assertEquals(1f, GnomeRules.fullFade(60, 120, 0f), "solid while it stands");
        assertEquals(0f, GnomeRules.fullFade(120, 120, 0f), "gone when its life ends");
    }

    @Test
    void onlyThePagesOfTheBigTiersCallTheFullFormAndItHasACooldownAndCaps() {
        assertFalse(GnomeRules.callsFull(8), "a starter page");
        assertFalse(GnomeRules.callsFull(12), "a mid page");
        assertTrue(GnomeRules.callsFull(20), "a zone page");
        assertTrue(GnomeRules.callsFull(35), "a signature page");
        assertTrue(GnomeRules.fullAllowed(1000, 1000, 0, 0, 6));
        assertFalse(GnomeRules.fullAllowed(999, 1000, 0, 0, 6), "the cooldown");
        assertFalse(GnomeRules.fullAllowed(1000, 1000, 1, 0, 6), "one per player");
        assertFalse(GnomeRules.fullAllowed(1000, 1000, 0, 6, 6), "six on the server");
        assertEquals(1, GnomeRules.FULL_PER_PLAYER);
        assertEquals(1, GnomeRules.SPIRITS_PER_PLAYER);
        assertEquals(48, GnomeRules.DEFAULT_MAX_SPIRITS, "48 spirits server-wide");
        assertEquals(6, GnomeRules.DEFAULT_MAX_FULL, "6 full forms server-wide");
    }

    @Test
    void theLedgerCountsPerPlayerAndForTheServerAndForgetsAPlayer() {
        GnomeLedger l = new GnomeLedger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(l.acquire(a, 1, 2));
        assertFalse(l.acquire(a, 1, 2), "the per player cap");
        assertTrue(l.acquire(b, 1, 2));
        assertFalse(l.acquire(c, 1, 2), "the server cap");
        assertEquals(2, l.total());
        l.release(a);
        l.release(a);
        assertEquals(1, l.total(), "a release is clamped at zero");
        assertTrue(l.acquire(c, 1, 2));
        l.forget(b);
        assertEquals(0, l.count(b));
        assertEquals(1, l.total());
        assertFalse(l.acquire(null, 1, 2));
        assertFalse(l.acquire(a, 0, 2));
        l.clear();
        assertEquals(0, l.total());
    }

    @Test
    void theCompanionStandsBesideAndTheGiantAheadOnTheGround() {
        double[] c = GnomeRules.companion(10, 64, 20, 0);
        assertEquals(10 - 1.15, c[0], 1e-9, "yaw 0 faces +z, so the right side is -x");
        assertEquals(64.10, c[1], 1e-9, "it hovers a hand above the ground");
        assertEquals(20 - 0.35, c[2], 1e-9, "a little behind");
        double[] d = new double[3];
        GnomeRules.companionTo(d, 10, 64, 20, 0);
        same(c, d, "the allocation-free variant agrees");
        double[] f = GnomeRules.full(0, 64, 0, 90);
        assertEquals(-4.8, f[0], 1e-9, "yaw 90 faces -x");
        assertEquals(0.0, f[2], 1e-9);
        assertEquals(64.0, f[1], 1e-9, "the giant stands on the ground");
        double[] h = GnomeRules.companionHand(0, 0, 0, 0), g = GnomeRules.fullHand(0, 0, 0, 0);
        assertTrue(h[2] > 0 && g[2] > h[2], "the hands are in front");
        assertTrue(g[1] > h[1] - 0.2);
    }

    @Test
    void theEmitterOnlyMovesTheCastersOwnStartAndStaysBounded() {
        double[] stay = GnomeRules.emit(30, 5, 30, 0, 1.6, 0, 0, 0, 0, 1, 1, 1);
        same(new double[]{30, 5, 30}, stay, "a start far from the caster is not the caster's own: unchanged");
        double[] moved = GnomeRules.emit(0, 1.6, 0.6, 0, 1.6, 0, 0, 0, 0, 1.5, 1.2, 2.0);
        same(new double[]{1.5, 1.2, 2.0}, moved, "the caster's own start moves to the fist");
        double[] far = GnomeRules.emit(0, 1.6, 0.6, 0, 1.6, 0, 0, 0, 0, 40, 40, 40);
        double d = Math.sqrt(far[0] * far[0] + (far[1] - 1.6) * (far[1] - 1.6) + far[2] * far[2]);
        assertEquals(GnomeRules.EMITTER_MAX_SHIFT, d, 1e-9, "the emitter is clamped to 7 blocks from the eye");
    }

    @Test
    void theCanonHostIsFeremenWhoseNameTheOwnerSpellsFeramina() {
        assertTrue(GnomeRules.isHostCanon("feremena"));
        assertTrue(GnomeRules.isHostCanon("Feramina"));
        assertFalse(GnomeRules.isHostCanon("mars"));
        assertFalse(GnomeRules.isHostCanon(null));
    }
}
