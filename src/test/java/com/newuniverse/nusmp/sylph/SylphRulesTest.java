package com.newuniverse.nusmp.sylph;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Sylph (docs/magic/sylph.md): the prop word, the caps, the forms, the emitter and the clip choice. */
class SylphRulesTest {
    private static void near(double[] e, double[] a, String m) {
        assertEquals(e.length, a.length, m);
        for (int i = 0; i < e.length; i++) assertEquals(e[i], a[i], 1e-9, m + " [" + i + "]");
    }

    @Test
    void thePropWordRoundTripsAndStampsWrapOver20Bits() {
        for (int variant : new int[]{SylphRules.V_SMALL, SylphRules.V_POWERED}) {
            for (int extra : new int[]{0, 1, 77, 0xFFFFF}) {
                int p = SylphRules.param(variant, extra);
                assertEquals(variant, SylphRules.variant(p));
                assertEquals(extra, SylphRules.extra(p));
                assertFalse(SylphRules.hidden(p));
                assertTrue(SylphRules.hidden(p | SylphRules.HIDDEN));
                assertEquals(variant, SylphRules.variant(p | SylphRules.HIDDEN), "the hidden bit does not change the variant");
            }
        }
        assertTrue(SylphRules.stamp(0) != 0, "a stamp is never 0 (0 means no cast yet)");
        assertTrue(SylphRules.stamp(0x100000) != 0);
        int st = SylphRules.stamp(1_000_000L);
        assertEquals(5, SylphRules.sinceStamp(st, 1_000_005L));
        assertEquals(Integer.MAX_VALUE, SylphRules.sinceStamp(0, 77));
        int near = SylphRules.stamp(0xFFFFE);
        assertEquals(4, SylphRules.sinceStamp(near, 0x100002L), "the 20 bit counter wraps without a jump");
    }

    @Test
    void onlyTheBigPagesCallThePoweredFormAndItLastsLongerForZephyr() {
        assertFalse(SylphRules.callsPowered(8.0));
        assertFalse(SylphRules.callsPowered(19.9));
        assertTrue(SylphRules.callsPowered(20.0));
        assertTrue(SylphRules.callsPowered(35.0));
        assertTrue(SylphRules.poweredTicks(true) > SylphRules.poweredTicks(false));
        assertTrue(SylphRules.POWERED_COOLDOWN > SylphRules.POWERED_TICKS, "she comes back only after she has been gone a while");
        assertEquals(1, SylphRules.POWERED_PER_PLAYER);
        assertEquals(1, SylphRules.SPIRITS_PER_PLAYER);
        assertEquals(48, SylphRules.DEFAULT_MAX_SPIRITS);
        assertEquals(6, SylphRules.DEFAULT_MAX_POWERED);
    }

    @Test
    void theSmallFormIsAboutAHalfBlockAndThePoweredFormAFewTimesThat() {
        assertTrue(SylphRules.SMALL_H >= 0.5f && SylphRules.SMALL_H <= 0.6f, "about 0.5 to 0.6 blocks tall");
        assertTrue(SylphRules.POWERED_H >= 1.4f && SylphRules.POWERED_H <= 1.8f, "about 1.4 to 1.8 blocks tall");
        assertTrue(SylphRules.SMALL_W < 0.5f && SylphRules.POWERED_W < 1.0f);
    }

    @Test
    void theSmallSylphFloatsBesideTheHeadAndThePoweredOneAboveAndBehind() {
        // a caster at the origin facing +z (yaw 0): the player's right is -x
        double[] s = SylphRules.small(0, 0, 0, 0);
        assertTrue(s[0] < -0.5 && s[0] > -1.0, "on the right side, within arm's reach");
        assertTrue(s[1] > 1.7 && s[1] < 2.2, "level with the head");
        assertTrue(s[2] > 0.0, "a little forward");
        double[] p = SylphRules.powered(0, 0, 0, 0);
        assertTrue(p[2] < -1.0, "behind");
        assertTrue(p[1] > 2.5, "above");
        // turning the body turns both (yaw 90 faces -x: forward is -x, right is +z... the offsets rotate with the body)
        double[] s90 = SylphRules.small(0, 0, 0, 90);
        assertEquals(Math.hypot(s[0], s[2]), Math.hypot(s90[0], s90[2]), 1e-9, "the same distance from the caster at any yaw");
        // the into-array forms give the same numbers
        double[] t = new double[3];
        SylphRules.smallTo(t, 1, 2, 3, 40);
        double[] u = SylphRules.small(1, 2, 3, 40);
        near(u, t, "smallTo");
        SylphRules.poweredTo(t, 1, 2, 3, 40);
        near(SylphRules.powered(1, 2, 3, 40), t, "poweredTo");
        // the hand is in front of her
        double[] hand = SylphRules.smallHand(0, 0, 0, 0);
        assertTrue(hand[2] > s[2], "her hand is forward of her");
        double[] ph = SylphRules.poweredHand(0, 0, 0, 0);
        assertTrue(ph[2] > p[2]);
    }

    @Test
    void theEmitterOnlyMovesTheCastersOwnStartAndStaysNearTheEye() {
        double[] own = SylphRules.emit(0, 1.62, 0, 0, 1.62, 0, 0, 0, 0, 0.7, 2.0, 0.3);
        near(new double[]{0.7, 2.0, 0.3}, own, "the caster's own start goes to the emitter");
        double[] feet = SylphRules.emit(0, 1.0, 0, 0, 1.62, 0, 0, 0, 0, 0.7, 2.0, 0.3);
        near(new double[]{0.7, 2.0, 0.3}, feet, "a start near the feet is the caster's own too");
        double[] far = SylphRules.emit(10, 5, 10, 0, 1.62, 0, 0, 0, 0, 0.7, 2.0, 0.3);
        near(new double[]{10, 5, 10}, far, "a start far from the caster (a placed spell) is left alone");
        double[] clamp = SylphRules.emit(0, 1.62, 0, 0, 1.62, 0, 0, 0, 0, 0, 40, 0);
        assertEquals(SylphRules.EMITTER_MAX_SHIFT, Math.abs(clamp[1] - 1.62), 1e-9, "the emitter is kept within reach of the eye");
    }

    @Test
    void theSmallClipFollowsTheCastTheMovementAndTheClock() {
        assertEquals("cast", SylphRules.smallClip(0, true, 0, 0));
        assertEquals("cast", SylphRules.smallClip(SylphRules.CAST_CLIP_TICKS - 1, false, 0, 0));
        assertEquals("fly", SylphRules.smallClip(SylphRules.CAST_CLIP_TICKS, true, 0, 0));
        assertEquals("laugh", SylphRules.smallClip(500, false, 0, 0), "the cycle starts with the laugh");
        assertEquals("idle", SylphRules.smallClip(500, false, 150, 0));
        assertEquals("point", SylphRules.smallClip(500, false, 320, 0));
        assertEquals("idle", SylphRules.smallClip(500, false, 450, 0));
        // two Sylphs do not laugh together
        assertNotEquals(SylphRules.smallClip(500, false, 10, 0), SylphRules.smallClip(500, false, 10, 200));
        for (String c : new String[]{"cast", "fly", "laugh", "point", "idle"}) assertTrue(java.util.Arrays.asList(SylphRules.SMALL_CLIPS).contains(c), c + " is a clip of the file");
        assertEquals("powered_cast", SylphRules.poweredClip(5, 100));
        assertEquals("shout", SylphRules.poweredClip(500, 4));
        assertEquals("hover", SylphRules.poweredClip(500, 100));
        for (String c : new String[]{"powered_cast", "shout", "hover"}) assertTrue(java.util.Arrays.asList(SylphRules.POWERED_CLIPS).contains(c));
    }

    @Test
    void theLedgerHoldsItsCapsAndForgets() {
        SylphLedger l = new SylphLedger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(l.acquire(a, 1, 2));
        assertFalse(l.acquire(a, 1, 2), "one per player");
        assertTrue(l.acquire(b, 1, 2));
        assertFalse(l.acquire(c, 1, 2), "two on the whole server");
        assertEquals(2, l.total());
        l.release(a);
        assertEquals(1, l.total());
        assertTrue(l.acquire(c, 1, 2));
        l.forget(b);
        l.forget(c);
        assertEquals(0, l.total());
        assertEquals(0, l.count(b));
        l.release(b);                                                       // releasing what was never taken does nothing
        assertEquals(0, l.total());
        assertFalse(l.acquire(null, 1, 2));
        assertFalse(l.acquire(a, 0, 2));
        assertFalse(l.acquire(a, 1, 0));
        l.acquire(a, 1, 5);
        l.clear();
        assertEquals(0, l.total());
    }

    @Test
    void thePacketGapIsAtLeastHalfASecond() {
        assertTrue(SylphRules.gapOk(100, 90, SylphRules.PACKET_GAP));
        assertFalse(SylphRules.gapOk(100, 95, SylphRules.PACKET_GAP));
        assertTrue(SylphRules.PACKET_GAP >= 10);
    }
}
