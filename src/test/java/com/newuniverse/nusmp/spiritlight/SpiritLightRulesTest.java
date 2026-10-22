package com.newuniverse.nusmp.spiritlight;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Spirit of Light: caps, the piercing beam, the Glowing pulse, Swiftness III, the prop words, the anchor points and the ledgers. No Minecraft class. */
class SpiritLightRulesTest {
    // ------------------------------------------------------------------ the caps (a config can only turn things down, never past the Light Magic numbers)
    @Test
    void theBeamNumbersAreCappedAndTheSeraphIsStrongerThanTheCubButNeverPastTheCaps() {
        for (boolean pw : new boolean[]{false, true}) {
            assertTrue(SpiritLightRules.beamRaw(pw, 1.0) <= SpiritLightRules.BEAM_RAW_CAP);
            assertTrue(SpiritLightRules.beamRaw(pw, 99.0) <= SpiritLightRules.BEAM_RAW_CAP, "a silly damage scale is clamped to 1");
            assertEquals(0f, SpiritLightRules.beamRaw(pw, 0.0), 1e-6, "0 = harmless");
            assertEquals(0f, SpiritLightRules.beamRaw(pw, -3.0), 1e-6);
            assertTrue(SpiritLightRules.beamPierce(pw) >= 1 && SpiritLightRules.beamPierce(pw) <= SpiritLightRules.MAX_PIERCE);
            assertTrue(SpiritLightRules.beamRange(pw) <= SpiritLightRules.MAX_RANGE);
            assertTrue(SpiritLightRules.beamCooldown(pw) >= SpiritLightRules.MIN_BEAM_COOLDOWN);
            assertTrue(SpiritLightRules.pulseRadius(pw) <= SpiritLightRules.MAX_PULSE_RADIUS);
            assertTrue(SpiritLightRules.pulseTargets(pw) <= SpiritLightRules.MAX_PULSE_TARGETS);
            assertTrue(SpiritLightRules.pulseTicks(pw) <= SpiritLightRules.MAX_PULSE_TICKS && SpiritLightRules.pulseTicks(pw) <= 200, "the glow is short: at most ten seconds");
            assertTrue(SpiritLightRules.pulseCooldown(pw) >= SpiritLightRules.MIN_PULSE_COOLDOWN);
        }
        assertTrue(SpiritLightRules.beamRaw(true, 1.0) > SpiritLightRules.beamRaw(false, 1.0));
        assertTrue(SpiritLightRules.beamPierce(true) > SpiritLightRules.beamPierce(false));
        assertTrue(SpiritLightRules.BEAM_RAW_CAP <= 5.0f, "no more than the Light Sword of Judgment's raw number (LightBoneJa.SWORD_RAW)");
        assertTrue(SpiritLightRules.POWER_TICKS <= 1200 && SpiritLightRules.POWER_COOLDOWN >= 600, "the seraph is short and cooldown bound");
    }

    // ------------------------------------------------------------------ the piercing beam
    @Test
    void theBeamPiercesTheNearestFewAndNothingElse() {
        double[] along = {9, 3, -1, 5, 1, 7};
        int[] hit = SpiritLightRules.pierce(along, along.length, 3);
        assertArrayEquals(new int[]{4, 1, 3}, hit, "the three nearest, nearest first, the miss (-1) is never hit");
        assertEquals(0, SpiritLightRules.pierce(new double[]{-1, -1}, 2, 3).length);
        assertEquals(SpiritLightRules.MAX_PIERCE, SpiritLightRules.pierce(new double[]{1, 2, 3, 4, 5, 6, 7, 8}, 8, 99).length, "even a silly ask is clamped to the pierce cap");
        assertEquals(0, SpiritLightRules.pierce(along, 0, 3).length);
        assertEquals(2, SpiritLightRules.pierce(new double[]{1, 2, 3}, 3, 2).length);
    }

    @Test
    void aFoeIsOnTheLineWhenWithinTheWidthAndTheDistanceAlongIsRight() {
        // a segment from the origin 10 blocks along +x
        assertEquals(5.0, SpiritLightRules.alongHit(0, 0, 0, 10, 0, 0, 5, 0.4, 0, 0.55), 1e-9);
        assertEquals(-1.0, SpiritLightRules.alongHit(0, 0, 0, 10, 0, 0, 5, 0.6, 0, 0.55), 1e-9, "just outside the width");
        assertEquals(10.0, SpiritLightRules.alongHit(0, 0, 0, 10, 0, 0, 10.3, 0, 0, 0.55), 1e-9, "past the end counts at the end when within the width of the end point");
        assertEquals(-1.0, SpiritLightRules.alongHit(0, 0, 0, 10, 0, 0, 12, 0, 0, 0.55), 1e-9);
        assertEquals(-1.0, SpiritLightRules.alongHit(0, 0, 0, 0, 0, 0, 1, 0, 0, 5.0), 1e-9, "a segment of no length hits nothing");
    }

    @Test
    void theBeamDirectionGoesFromTheEmitterToTheAimAndFallsBackToTheLook() {
        double[] d = SpiritLightRules.beamDirection(0, 0, 0, 0, 0, 10, 1, 0, 0);
        assertArrayEquals(new double[]{0, 0, 1}, d, 1e-9);
        double[] near = SpiritLightRules.beamDirection(0, 0, 0, 0.1, 0, 0, 0, 1, 0);
        assertArrayEquals(new double[]{0, 1, 0}, near, 1e-9, "an aim point on top of the emitter: the way the owner looks");
        assertEquals(0.0, SpiritLightRules.yawOf(0, 1), 1e-9, "a yaw of 0 faces +z");
        assertEquals(-90.0, SpiritLightRules.yawOf(1, 0), 1e-9);
        assertEquals(-90.0, SpiritLightRules.pitchOf(0, 1, 0), 1e-9, "up is -90 in Minecraft");
        assertEquals(45.0, SpiritLightRules.pitchOf(0, -1, 1), 1e-9);
    }

    // ------------------------------------------------------------------ the Glowing pulse
    @Test
    void thePulseMarksTheNearestFoesAndTheHiddenOnesFirst() {
        double[] d = {4, 100, 9, 36, 1, 25};
        int[] sl = SpiritLightRules.pulseShortlist(d, 6, 8.0);              // radius 8: squared 64, so 100 is out
        assertArrayEquals(new int[]{4, 0, 2, 5, 3}, sl, "the shortlist is the nearest foes inside the radius, nearest first");
        double[] sd = new double[sl.length];
        boolean[] hidden = new boolean[sl.length];
        for (int i = 0; i < sl.length; i++) sd[i] = d[sl[i]];
        hidden[3] = true;                                                    // the foe at 25 is behind a wall
        int[] pick = SpiritLightRules.pulsePick(sd, hidden, sl.length, 2, 8.0);
        assertArrayEquals(new int[]{3, 0}, pick, "the hidden foe comes first, then the nearest");
        assertTrue(SpiritLightRules.pulsePick(sd, null, sl.length, 99, 8.0).length <= SpiritLightRules.MAX_PULSE_TARGETS);
    }

    @Test
    void aCrowdIsReadOnlyUpToTheCapsSoTheCostIsBounded() {
        double[] many = new double[5000];
        for (int i = 0; i < many.length; i++) many[i] = i % 50;
        assertTrue(SpiritLightRules.pulseShortlist(many, many.length, 20.0).length <= SpiritLightRules.MAX_PULSE_CANDIDATES, "at most 32 line of sight tests per pulse");
        double[] along = new double[5000];
        java.util.Arrays.fill(along, 3.0);
        assertTrue(SpiritLightRules.pierce(along, along.length, 5).length <= 5);
        assertEquals(32, SpiritLightRules.MAX_PULSE_CANDIDATES);
    }

    // ------------------------------------------------------------------ Swiftness III
    @Test
    void swiftnessIsAShortEffectRefreshedWhenLowAndOnlyTheSpiritsOwnIsRemoved() {
        assertEquals(2, SpiritLightRules.SWIFT_AMPLIFIER, "Swiftness III is amplifier 2");
        assertTrue(SpiritLightRules.SWIFT_TICKS <= 100, "short: gone within seconds of the link ending even if a cleanup hook were missed");
        assertTrue(SpiritLightRules.swiftnessDue(false, 0));
        assertTrue(SpiritLightRules.swiftnessDue(true, SpiritLightRules.SWIFT_REFRESH_BELOW - 1));
        assertFalse(SpiritLightRules.swiftnessDue(true, SpiritLightRules.SWIFT_TICKS));
        assertTrue(SpiritLightRules.ownSwiftness(true, false, 2, 60));
        assertFalse(SpiritLightRules.ownSwiftness(false, false, 2, 60), "a potion is not ambient");
        assertFalse(SpiritLightRules.ownSwiftness(true, true, 2, 60), "a beacon shows its particles");
        assertFalse(SpiritLightRules.ownSwiftness(true, false, 1, 60), "another level");
        assertFalse(SpiritLightRules.ownSwiftness(true, false, 2, 6000), "a long effect is somebody else's");
    }

    // ------------------------------------------------------------------ the prop words
    @Test
    void theFormParamKeepsTheVariantThePoseTheHiddenBitAndTheStamp() {
        long tick = 123456;
        int p = SpiritLightRules.formParam(SpiritLightRules.V_SERAPH, false, 6, tick);
        assertEquals(SpiritLightRules.V_SERAPH, SpiritLightRules.variant(p));
        assertEquals(6, SpiritLightRules.pose(p));
        assertFalse(SpiritLightRules.hidden(p));
        assertEquals(0, SpiritLightRules.sinceCast(p, tick));
        assertEquals(17, SpiritLightRules.sinceCast(p, tick + 17));
        int q = SpiritLightRules.formParam(SpiritLightRules.V_CUB, true, 9, 0);
        assertTrue(SpiritLightRules.hidden(q));
        assertEquals(9, SpiritLightRules.pose(q));
        assertEquals(Integer.MAX_VALUE, SpiritLightRules.sinceCast(q, 99), "never played");
        assertEquals(1, SpiritLightRules.stamp(SpiritLightRules.formParam(0, false, 0, 1L << 20)), "a stamp that would wrap to 0 stays 1: 0 means never");
    }

    @Test
    void theBeamAndPulseParamsRoundTrip() {
        int b = SpiritLightRules.beamParam(true, 3, -20.0, 17.5);
        assertEquals(SpiritLightRules.V_BEAM, SpiritLightRules.variant(b));
        assertTrue(SpiritLightRules.powered(b));
        assertEquals(3, SpiritLightRules.beamHits(b));
        assertEquals(-20.0, SpiritLightRules.beamPitch(b), 1e-9);
        assertEquals(17.5, SpiritLightRules.beamLength(b), 1e-9);
        int c = SpiritLightRules.beamParam(false, 99, 400.0, 999.0);
        assertEquals(7, SpiritLightRules.beamHits(c));
        assertEquals(90.0, SpiritLightRules.beamPitch(c), 1e-9);
        assertEquals(127.0, SpiritLightRules.beamLength(c), 1e-9);
        int pl = SpiritLightRules.pulseParam(true, 18);
        assertEquals(SpiritLightRules.V_PULSE, SpiritLightRules.variant(pl));
        assertEquals(18.0, SpiritLightRules.pulseRadius(pl), 1e-9);
        assertEquals(63.0, SpiritLightRules.pulseRadius(SpiritLightRules.pulseParam(false, 500)), 1e-9);
    }

    @Test
    void onlyTheFourKeyActionsAreValidAndTheGapIsExact() {
        for (int a = 0; a < 4; a++) assertTrue(SpiritLightRules.validAction(a));
        for (int a : new int[]{-1, 4, 5, 255, Integer.MAX_VALUE}) assertFalse(SpiritLightRules.validAction(a));
        assertTrue(SpiritLightRules.gapOk(10, 4, 6));
        assertFalse(SpiritLightRules.gapOk(9, 4, 6));
        assertEquals(SpiritLightRules.SOUND_KINDS, SpiritLightRules.SOUND_IDS.length);
        for (int k = 0; k < SpiritLightRules.SOUND_KINDS; k++) assertTrue(SpiritLightRules.soundGap(k) > 0);
        assertEquals(Integer.MAX_VALUE, SpiritLightRules.soundGap(99));
    }

    // ------------------------------------------------------------------ anchors
    @Test
    void theCubFloatsBesideTheRightShoulderAndTheSeraphStandsBehind() {
        double[] c = SpiritLightRules.companion(0, 0, 0, 0);            // an owner facing +z: the right is -x
        assertTrue(c[0] < -0.4 && c[1] > 1.0 && c[1] < 1.7 && Math.abs(c[2]) < 0.5, "to the right, at shoulder height, close");
        double[] s = SpiritLightRules.seraph(0, 0, 0, 0);
        assertTrue(s[2] < -1.0, "behind (-z) an owner facing +z");
        double[] turned = SpiritLightRules.seraph(0, 0, 0, 180);
        assertTrue(turned[2] > 1.0, "turns with the owner");
        double[] core = SpiritLightRules.seraphCore(0, 0, 0, 0);
        assertTrue(core[1] > s[1] + 1.2, "the core (the head) is high above the seraph's feet");
        double[] mouth = SpiritLightRules.companionMouth(0, 0, 0, 0);
        assertTrue(mouth[2] > c[2], "the mouth is in front of the cub");
        double[] into = new double[3];
        SpiritLightRules.companionTo(into, 5, 6, 7, 90);
        assertArrayEquals(SpiritLightRules.companion(5, 6, 7, 90), into, 1e-12);
        SpiritLightRules.seraphTo(into, 5, 6, 7, 33);
        assertArrayEquals(SpiritLightRules.seraph(5, 6, 7, 33), into, 1e-12);
    }

    @Test
    void theEmitterIsKeptNearTheEye() {
        double[] same = SpiritLightRules.clampEmitter(0, 0, 0, 1, 1, 1);
        assertArrayEquals(new double[]{1, 1, 1}, same, 1e-12);
        double[] far = SpiritLightRules.clampEmitter(0, 0, 0, 100, 0, 0);
        assertEquals(SpiritLightRules.EMITTER_MAX_SHIFT, far[0], 1e-9);
    }

    // ------------------------------------------------------------------ the ledgers
    @Test
    void theCountedLedgerHoldsThePerPlayerAndTheServerCap() {
        SpiritLightLedger.Counted l = new SpiritLightLedger.Counted();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(l.acquire(a, 1, 2));
        assertFalse(l.acquire(a, 1, 2), "one per player");
        assertTrue(l.acquire(b, 1, 2));
        assertFalse(l.acquire(c, 1, 2), "two on the whole server");
        assertEquals(2, l.total());
        l.release(a);
        assertTrue(l.acquire(c, 1, 2));
        l.release(a);
        l.release(a);
        assertEquals(2, l.total(), "releasing nothing changes nothing");
        l.forget(b);
        l.forget(c);
        assertEquals(0, l.total());
        assertFalse(l.acquire(null, 1, 1));
        assertFalse(l.acquire(a, 0, 1));
    }

    @Test
    void theTimedLedgerExpiresByItselfSoALostPropNeverLeaksASlot() {
        SpiritLightLedger.Timed t = new SpiritLightLedger.Timed();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        for (int i = 0; i < SpiritLightRules.EFFECT_PROPS_PER_PLAYER; i++) assertTrue(t.acquire(a, 100, 6, SpiritLightRules.EFFECT_PROPS_PER_PLAYER, 10));
        assertFalse(t.acquire(a, 100, 6, SpiritLightRules.EFFECT_PROPS_PER_PLAYER, 10), "the per player cap");
        assertEquals(3, t.count(a, 105));
        assertEquals(0, t.count(a, 106), "all gone after their life");
        assertTrue(t.acquire(a, 106, 6, 3, 10));
        assertTrue(t.acquire(b, 106, 6, 3, 1) == false, "the server cap of 1 is already taken by a");
        t.forget(a);
        assertEquals(0, t.total(106));
        assertFalse(t.acquire(a, 0, 0, 3, 3), "a slot of no life is refused");
    }
}
