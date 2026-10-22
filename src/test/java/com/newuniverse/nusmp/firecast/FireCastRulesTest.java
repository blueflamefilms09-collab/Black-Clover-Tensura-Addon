package com.newuniverse.nusmp.firecast;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the fire casting styles: style by canon id, the swing hitbox, the punch combo, the charge levels and their caps, the ledgers, the emitter and the anchors of the Salamander. */
class FireCastRulesTest {
    private static void same(double[] want, double[] got, String why) {
        assertEquals(want.length, got.length, why);
        for (int i = 0; i < want.length; i++) assertEquals(want[i], got[i], 1e-9, why);
    }

    // ------------------------------------------------------------------ styles
    @Test
    void onlyTheFourCanonBooksHaveAStyle() {
        assertEquals(FireStyle.MAGNA, FireStyle.ofCanon("magna"));
        assertEquals(FireStyle.MEREOLEONA, FireStyle.ofCanon("MEREOLEONA"));
        assertEquals(FireStyle.FUEGOLEON, FireStyle.ofCanon("fuegoleon"));
        assertEquals(FireStyle.LEOPOLD, FireStyle.ofCanon("Leopold"));
        for (String other : new String[]{"yuno", "asta", "kirsch", "mimosa", "floga", "", "nobody"}) assertEquals(FireStyle.NONE, FireStyle.ofCanon(other), other);
        assertEquals(FireStyle.NONE, FireStyle.ofCanon(null));
    }

    // ------------------------------------------------------------------ Magna: the swing
    @Test
    void theBallHangsInFrontOfTheEyeAndTheSwingCatchesIt() {
        double[] b = FireCastRules.ballPosition(0, 1.6, 0, 0, 0, 1);
        assertEquals(0.0, b[0], 1e-9);
        assertEquals(FireCastRules.BALL_DISTANCE, b[2], 1e-9);
        assertTrue(b[1] < 1.6, "a little below the eye");
        assertTrue(FireCastRules.swingHits(0, 1.6, 0, 0, 0, 1, b[0], b[1], b[2]), "a swing at the ball it just tossed hits it");
        // the player turns away before the contact frame: a miss
        assertFalse(FireCastRules.swingHits(0, 1.6, 0, 1, 0, 0, b[0], b[1], b[2]));
        assertFalse(FireCastRules.swingHits(0, 1.6, 0, 0, 0, -1, b[0], b[1], b[2]));
        // within the cone but too far away: a miss; walking forward 1 block still hits
        assertFalse(FireCastRules.swingHits(0, 1.6, 0, 0, 0, 1, 0, 1.6, FireCastRules.SWING_REACH + 0.5));
        assertTrue(FireCastRules.swingHits(0, 1.6, 1.0, 0, 0, 1, b[0], b[1], b[2]));
        // the cone edge
        double edge = FireCastRules.SWING_HALF_ANGLE;
        double in = Math.toRadians(edge - 5), out = Math.toRadians(edge + 5);
        assertTrue(FireCastRules.swingHits(0, 0, 0, 0, 0, 1, 2 * Math.sin(in), 0, 2 * Math.cos(in)));
        assertFalse(FireCastRules.swingHits(0, 0, 0, 0, 0, 1, 2 * Math.sin(out), 0, 2 * Math.cos(out)));
    }

    @Test
    void aZeroLookStillGivesABall() {
        double[] b = FireCastRules.ballPosition(1, 2, 3, 0, 0, 0);
        assertEquals(3 + FireCastRules.BALL_DISTANCE, b[2], 1e-9);
    }

    @Test
    void theSwingClipPutsItsContactWhereTheServerTestsTheHit() {
        assertEquals(8, FireCastRules.SWING_IMPACT_TICK, "tools/gen_cast_animations.py keys the contact at 0.40 s");
        assertTrue(FireCastRules.BALL_LIFE > FireCastRules.SWING_IMPACT_TICK);
        assertTrue(FireCastRules.SWING_CLIP_TICKS > FireCastRules.SWING_IMPACT_TICK);
        assertEquals(1, FireCastRules.BALLS_PER_PLAYER);
        assertTrue(FireCastRules.MIN_SWING_GAP >= FireCastRules.SWING_CLIP_TICKS, "a new swing waits for the old clip");
    }

    @Test
    void homingFollowsOnlyWhatIsInTheSmallCone() {
        assertTrue(FireCastRules.inHomingCone(0, 0, 1, 0, 0, 10));
        assertTrue(FireCastRules.inHomingCone(0, 0, 1, 1, 0, 10));
        assertFalse(FireCastRules.inHomingCone(0, 0, 1, 5, 0, 10));
        assertFalse(FireCastRules.inHomingCone(0, 0, 1, 0, 0, FireCastRules.HOMING_RANGE + 1));
        assertFalse(FireCastRules.inHomingCone(0, 0, 1, 0, 0, -5));
    }

    // ------------------------------------------------------------------ Mereoleona: the combo
    @Test
    void theFinisherNumbersFollowThePageTierAndTheManaZone() {
        assertEquals(3, FireCastRules.finisherHits(8, false));
        assertEquals(3, FireCastRules.finisherHits(12, false));
        assertEquals(5, FireCastRules.finisherHits(20, false));
        assertEquals(5, FireCastRules.finisherHits(35, false));
        assertEquals(2, FireCastRules.finisherHits(8, true));
        assertEquals(4, FireCastRules.finisherHits(35, true));
        for (double cost : new double[]{0, 5, 8, 12, 20, 35, 80}) for (boolean zone : new boolean[]{false, true}) assertTrue(FireCastRules.finisherHits(cost, zone) >= FireCastRules.MIN_FINISHER);
    }

    @Test
    void threePunchesInARowReleaseAReadiedStarterPage() {
        PunchCombo c = new PunchCombo();
        assertEquals(PunchCombo.Result.HIT, c.land(100, false, 8, true));
        assertEquals(PunchCombo.Result.HIT, c.land(110, false, 8, true));
        assertEquals(PunchCombo.Result.FINISHER, c.land(120, false, 8, true));
        assertEquals(0, c.count(121, false), "the combo is spent by the finisher");
        assertEquals(PunchCombo.Result.HIT, c.land(125, false, 8, true), "and starts again at one");
    }

    @Test
    void aSignaturePageNeedsFivePunchesAndTheZoneTakesOneOff() {
        PunchCombo c = new PunchCombo();
        for (int i = 0; i < 4; i++) assertEquals(PunchCombo.Result.HIT, c.land(10 * i, false, 35, true), "punch " + (i + 1));
        assertEquals(PunchCombo.Result.FINISHER, c.land(40, false, 35, true));
        PunchCombo z = new PunchCombo();
        for (int i = 0; i < 3; i++) assertEquals(PunchCombo.Result.HIT, z.land(10 * i, true, 35, true));
        assertEquals(PunchCombo.Result.FINISHER, z.land(30, true, 35, true));
    }

    @Test
    void aPauseBreaksTheComboAndWithoutAReadiedPageThereIsNoFinisher() {
        PunchCombo c = new PunchCombo();
        c.land(0, false, 8, true);
        c.land(10, false, 8, true);
        assertEquals(PunchCombo.Result.HIT, c.land(10 + FireCastRules.comboTimeout(false) + 1, false, 8, true), "too late: the count starts again");
        assertEquals(1, c.count(10 + FireCastRules.comboTimeout(false) + 1, false));
        PunchCombo u = new PunchCombo();
        for (int i = 0; i < 30; i++) assertEquals(PunchCombo.Result.HIT, u.land(i * 5, false, 8, false));
        assertTrue(u.count(150, false) <= FireCastRules.COMBO_CAP, "an unarmed combo is capped");
        assertTrue(FireCastRules.comboTimeout(true) > FireCastRules.comboTimeout(false));
    }

    @Test
    void theFistIsInFrontOfAndBelowTheEyeOnTheRight() {
        double[] f = FireCastRules.fistPosition(0, 1.6, 0, 0, 0, 1);
        assertTrue(f[2] > 0.5, "in front");
        assertTrue(f[1] < 1.6, "below the eye");
        assertTrue(f[0] < 0, "the right of a player facing +z is -x");
        double[] up = FireCastRules.fistPosition(0, 1.6, 0, 0, 1, 0);
        assertTrue(Double.isFinite(up[0]) && Double.isFinite(up[2]), "looking straight up stays finite");
    }

    // ------------------------------------------------------------------ Leopold: the charge
    @Test
    void theChargeLevelsAreBoundedAndOrdered() {
        assertEquals(0, FireCastRules.chargeLevel(0));
        assertEquals(0, FireCastRules.chargeLevel(19));
        assertEquals(1, FireCastRules.chargeLevel(20));
        assertEquals(2, FireCastRules.chargeLevel(40));
        assertEquals(3, FireCastRules.chargeLevel(65));
        assertEquals(4, FireCastRules.chargeLevel(90));
        assertEquals(FireCastRules.MAX_LEVEL, FireCastRules.chargeLevel(100000), "a hard cap");
        double d = 0, s = 0;
        for (int l = 0; l <= FireCastRules.MAX_LEVEL; l++) {
            assertTrue(FireCastRules.chargeDamage(l) >= d && FireCastRules.chargeSize(l) >= s, "never shrinks as the level grows");
            d = FireCastRules.chargeDamage(l);
            s = FireCastRules.chargeSize(l);
        }
        assertEquals(1.0, FireCastRules.chargeDamage(0), 0.0);
        assertEquals(1.0, FireCastRules.chargeDamage(1), 0.0, "the plain full charge is the plain cast");
        assertTrue(FireCastRules.chargeDamage(FireCastRules.MAX_LEVEL) <= 1.5 && FireCastRules.chargeSize(FireCastRules.MAX_LEVEL) <= 1.4);
        assertEquals(FireCastRules.chargeDamage(FireCastRules.MAX_LEVEL), FireCastRules.chargeDamage(99), "out-of-range levels are clamped");
        assertEquals(FireCastRules.chargeDamage(0), FireCastRules.chargeDamage(-3));
    }

    @Test
    void theManaStepsFallOnTheLevelThresholdsAndStopAtTheCap() {
        int steps = 0;
        Set<Integer> at = new HashSet<>();
        for (int held = 0; held < 400; held++) if (FireCastRules.chargeStepDue(held)) { steps++; at.add(held); }
        assertEquals((FireCastRules.MAX_HOLD - FireCastRules.CHARGE_FREE_TICKS) / FireCastRules.CHARGE_STEP, steps);
        for (int t : FireCastRules.LEVEL_AT) if (t > FireCastRules.CHARGE_FREE_TICKS) assertTrue(at.contains(t), "a level is reached on a paid step: " + t);
        assertFalse(FireCastRules.chargeStepDue(FireCastRules.CHARGE_FREE_TICKS), "the plain cast time is free");
        assertFalse(FireCastRules.chargeStepDue(FireCastRules.MAX_HOLD + FireCastRules.CHARGE_STEP), "nothing is drawn past the cap");
        double total = steps * FireCastRules.CHARGE_STEP_COST;
        assertTrue(total <= 1.0, "the whole charge costs at most one more cast: " + total);
    }

    // ------------------------------------------------------------------ the ledgers
    @Test
    void theLedgerCapsPerPlayerAndAcrossTheServer() {
        CapLedger l = new CapLedger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(l.acquire(a, 1, 2));
        assertFalse(l.acquire(a, 1, 2), "one per player");
        assertTrue(l.acquire(b, 1, 2));
        assertFalse(l.acquire(c, 1, 2), "two on the server");
        assertEquals(2, l.total());
        l.release(a);
        assertTrue(l.acquire(c, 1, 2), "a released slot is free again");
        l.release(a);
        l.release(a);
        assertEquals(2, l.total(), "a double release never goes below what is held");
        l.forget(b);
        l.forget(c);
        assertEquals(0, l.total());
        assertFalse(l.acquire(null, 1, 1));
        assertFalse(l.acquire(a, 0, 5));
    }

    // ------------------------------------------------------------------ Fuegoleon: the Salamander
    @Test
    void theParamPacksVariantFlagsAndExtra() {
        for (int v : new int[]{FireCastRules.V_BALL, FireCastRules.V_SPIRAL, FireCastRules.V_SALAMANDER, FireCastRules.V_DRAGON}) {
            int p = FireCastRules.param(v, 3);
            assertEquals(v, FireCastRules.variant(p));
            assertEquals(3, FireCastRules.extra(p));
            assertFalse(FireCastRules.hidden(p));
            assertTrue(FireCastRules.hidden(p | FireCastRules.HIDDEN));
            assertEquals(v, FireCastRules.variant(p | FireCastRules.HIDDEN));
            assertEquals(3, FireCastRules.extra(p | FireCastRules.HIDDEN), "the hidden bit never touches the extra");
        }
        int stamp = 0xFFFFF;
        assertEquals(stamp, FireCastRules.extra(FireCastRules.param(FireCastRules.V_SALAMANDER, stamp)), "a 20 bit cast stamp survives");
    }

    @Test
    void theSalamanderSitsOnTheRightShoulderAndItsMouthIsInFront() {
        double[] s = FireCastRules.shoulder(10, 64, 20, 0);
        assertTrue(s[0] < 10, "a player facing +z has the right shoulder at -x");
        assertEquals(64 + 1.46, s[1], 1e-9);
        double[] m = FireCastRules.shoulderMouth(10, 64, 20, 0);
        assertTrue(m[2] > s[2], "the mouth is ahead of the perch");
        double[] turned = FireCastRules.shoulder(0, 0, 0, 90);
        assertTrue(turned[2] < -0.3, "turned 90 degrees (facing west, -x) the right shoulder is to the north, -z");
        double[] d = FireCastRules.dragonPosition(0, 0, 0, 0);
        assertTrue(d[1] > 3 && d[2] < 0, "the dragon hangs above and behind");
        assertTrue(FireCastRules.dragonMouth(0, 0, 0, 0)[2] > d[2] + 2, "its mouth is well ahead of its body");
    }

    @Test
    void theEmitterOnlyTakesTheCastersOwnStartAndStaysNearTheEye() {
        double[] eye = {0, 1.6, 0}, feet = {0, 0, 0};
        double[] own = FireCastRules.emit(0, 1.6, 0.6, eye[0], eye[1], eye[2], feet[0], feet[1], feet[2], 0.4, 1.7, 0.4);
        same(new double[]{0.4, 1.7, 0.4}, own, "the caster's own start moves to the emitter");
        double[] far = FireCastRules.emit(30, 5, 30, eye[0], eye[1], eye[2], feet[0], feet[1], feet[2], 0.4, 1.7, 0.4);
        same(new double[]{30, 5, 30}, far, "a start somewhere else (a placed zone) stays where it is");
        double[] clamped = FireCastRules.emit(0, 1.6, 0.6, eye[0], eye[1], eye[2], feet[0], feet[1], feet[2], 0, 11.6, 40);
        double dx = clamped[0] - eye[0], dy = clamped[1] - eye[1], dz = clamped[2] - eye[2];
        assertEquals(FireCastRules.EMITTER_MAX_SHIFT, Math.sqrt(dx * dx + dy * dy + dz * dz), 1e-6, "an emitter far away is pulled back near the eye");
    }

    @Test
    void theBigFormIsRareAndShortLived() {
        assertTrue(FireCastRules.DRAGON_TICKS <= 100, "a few seconds");
        assertTrue(FireCastRules.DRAGON_COOLDOWN >= 2 * FireCastRules.DRAGON_TICKS);
        assertEquals(1, FireCastRules.DRAGONS_PER_PLAYER);
        assertEquals(1, FireCastRules.SPIRITS_PER_PLAYER);
        assertTrue(FireCastRules.DRAGON_BREATH_TICK < FireCastRules.DRAGON_TICKS);
    }

    @Test
    void packetsAreRateLimited() {
        assertTrue(FireCastRules.gapOk(100, 80, FireCastRules.PACKET_GAP));
        assertFalse(FireCastRules.gapOk(100, 95, FireCastRules.PACKET_GAP));
        assertTrue(FireCastRules.gapOk(0, Long.MIN_VALUE / 4, FireCastRules.PACKET_GAP), "the first ask is always answered");
    }
}
