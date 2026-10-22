package com.newuniverse.nusmp.undine;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Undine: the prop word, the timings, where she stands, the emitter, the clips, the caps and the sound budget. */
class UndineRulesTest {
    @Test
    void thePropWordCarriesVariantHiddenFacePoseAndStampWithoutOverlap() {
        int w = UndineRules.withPose(UndineRules.param(UndineRules.V_FULL) | UndineRules.HIDDEN, UndinePose.POWER.id(), UndineFace.ANGRY.id(), 12345);
        assertEquals(UndineRules.V_FULL, UndineRules.variant(w));
        assertTrue(UndineRules.hidden(w));
        assertEquals(UndinePose.POWER.id(), UndineRules.pose(w));
        assertEquals(UndineFace.ANGRY.id(), UndineRules.face(w));
        assertEquals(12345, UndineRules.stamp(w));
        assertFalse(UndineRules.hidden(UndineRules.param(UndineRules.V_COMPANION)));
        // every pose and face fits its bits, whatever the others are
        for (UndinePose pose : UndinePose.values()) for (UndineFace face : UndineFace.values()) {
            int x = UndineRules.withPose(UndineRules.V_COMPANION, pose.id(), face.id(), 0xFFFF);
            assertEquals(pose.id(), UndineRules.pose(x));
            assertEquals(face.id(), UndineRules.face(x));
            assertEquals(UndineRules.V_COMPANION, UndineRules.variant(x));
            assertFalse(UndineRules.hidden(x));
        }
        assertTrue(UndinePose.maxId() <= UndineRules.POSE_MASK && UndineFace.maxId() <= UndineRules.FACE_MASK, "the ids fit their bits");
        int cleared = UndineRules.withoutPose(w);
        assertEquals(0, UndineRules.pose(cleared));
        assertEquals(0, UndineRules.face(cleared));
        assertEquals(UndineRules.V_FULL, UndineRules.variant(cleared));
        assertTrue(UndineRules.hidden(cleared));
    }

    @Test
    void theStampNeverIsZeroAndWrapsSafely() {
        assertEquals(1, UndineRules.stampOf(0));
        assertEquals(1, UndineRules.stampOf(UndineRules.STAMP_MASK + 1L));
        int s = UndineRules.stampOf(1000);
        assertEquals(5, UndineRules.sinceStamp(s, 1005));
        assertEquals(Integer.MAX_VALUE, UndineRules.sinceStamp(0, 1005));
        assertEquals(10, UndineRules.sinceStamp(UndineRules.stampOf(UndineRules.STAMP_MASK - 4L), UndineRules.STAMP_MASK + 6L), "a stamp taken just before the counter wraps still counts right");
        assertEquals(0xFFFF, UndineRules.STAMP_MASK, "a 16 bit stamp: 54 minutes before it wraps, far longer than any pose");
    }

    @Test
    void onlyTheZoneAndSignaturePagesCallTheFullForm() {
        assertFalse(UndineRules.callsFull(8));
        assertFalse(UndineRules.callsFull(19.9));
        assertTrue(UndineRules.callsFull(20));
        assertTrue(UndineRules.callsFull(35));
    }

    @Test
    void theCompanionFloatsOnTheCastersRightAndTheFullFormStandsBehind() {
        // yaw 0 faces +z, the caster's right is -x
        double[] c = UndineRules.companion(0, 64, 0, 0);
        assertTrue(c[0] < -1.0, "to the right of a caster facing +z");
        assertTrue(c[2] < 0, "a little behind");
        assertEquals(64.25, c[1], 1e-9);
        double[] f = UndineRules.full(0, 64, 0, 0);
        assertEquals(0.0, f[0], 1e-9);
        assertEquals(-2.7, f[2], 1e-9);
        assertEquals(64.0, f[1], 1e-9);
        // turned 90 degrees (facing -x): the right is -z
        double[] c90 = UndineRules.companion(0, 0, 0, 90);
        assertTrue(c90[2] < -1.0);
        // the array version gives the same numbers
        double[] t = new double[3];
        UndineRules.companionTo(t, 3, 4, 5, 33);
        double[] u = UndineRules.companion(3, 4, 5, 33);
        assertArrayEquals(u, t, 1e-12);
        UndineRules.fullTo(t, 3, 4, 5, 33);
        assertArrayEquals(UndineRules.full(3, 4, 5, 33), t, 1e-12);
    }

    @Test
    void thePalmsAreInFrontOfTheirFormAtHandHeight() {
        double[] f = UndineRules.forward(0);
        assertEquals(0.0, f[0], 1e-9);
        assertEquals(1.0, f[1], 1e-9);
        double[] p = UndineRules.fullPalms(0, 0, 0, 0), full = UndineRules.full(0, 0, 0, 0);
        assertTrue(p[2] > full[2] && p[1] > full[1] + 1.5, "between her raised hands, in front of her");
        double[] cp = UndineRules.companionPalm(0, 0, 0, 0), comp = UndineRules.companion(0, 0, 0, 0);
        assertTrue(cp[2] > comp[2] && cp[1] > comp[1] + 0.8);
    }

    @Test
    void theEmitterOnlyMovesTheCastersOwnStartAndNeverFarFromTheEye() {
        double[] palm = UndineRules.fullPalms(0, 0, 0, 0);
        double[] own = UndineRules.emit(0, 1.6, 0.3, 0, 1.62, 0, 0, 0, 0, palm[0], palm[1], palm[2]);
        assertTrue(Math.sqrt(Math.pow(own[0], 2) + Math.pow(own[1] - 1.62, 2) + Math.pow(own[2], 2)) <= UndineRules.EMITTER_MAX_SHIFT + 1e-9);
        double[] other = UndineRules.emit(30, 70, 30, 0, 1.62, 0, 0, 0, 0, palm[0], palm[1], palm[2]);
        assertArrayEquals(new double[]{30, 70, 30}, other, 1e-12, "a start that is not the caster's own is left alone");
        double[] near = UndineRules.emit(0, 0.5, 0, 0, 1.62, 0, 0, 0, 0, 0.5, 1.5, 0.5);
        assertArrayEquals(new double[]{0.5, 1.5, 0.5}, near, 1e-12, "an emitter within reach is used as it is");
    }

    @Test
    void theClipIsThePoseWhileItIsHeldThenTheFormsIdle() {
        assertEquals("cast", UndineRules.clip(false, UndinePose.CAST.id(), 0f));
        assertEquals("cast_f", UndineRules.clip(true, UndinePose.CAST.id(), UndinePose.CAST.ticks() - 1f));
        assertEquals("welcome", UndineRules.clip(true, UndinePose.WELCOME.id(), 5f), "a looping pose is the same for both forms");
        assertEquals("idle", UndineRules.clip(false, UndinePose.CAST.id(), UndinePose.CAST.ticks() + UndineRules.POSE_TAIL + 1f), "the pose is over");
        assertEquals("full_idle", UndineRules.clip(true, UndinePose.CAST.id(), UndinePose.CAST.ticks() + UndineRules.POSE_TAIL + 1f));
        assertEquals("idle", UndineRules.clip(false, 0, 3f));
        assertEquals("idle", UndineRules.clip(false, UndinePose.CAST.id(), -1f), "no stamp yet");
        assertEquals(1.0f, UndineRules.clipSeconds(UndinePose.CAST.id(), 20f, 999f), 1e-6, "a pose counts from its stamp");
        assertEquals(2.0f, UndineRules.clipSeconds(0, 3f, 40f), 1e-6, "an idle counts from the age");
        assertEquals(UndineFace.SAD.id(), UndineRules.shownFace(UndineRules.withPose(0, UndinePose.SORROW.id(), UndineFace.SAD.id(), 5), 10f));
        assertEquals(UndineFace.CALM.id(), UndineRules.shownFace(UndineRules.withPose(0, UndinePose.SORROW.id(), UndineFace.SAD.id(), 5), UndinePose.SORROW.ticks() + UndineRules.POSE_TAIL + 1f), "the face goes back with the pose");
    }

    @Test
    void theFadeFormsAndFadesAndStaysInRange() {
        assertEquals(0f, UndineRules.fade(0f, 100f, 10f, 10f), 1e-6);
        assertEquals(0.5f, UndineRules.fade(5f, 100f, 10f, 10f), 1e-6);
        assertEquals(1f, UndineRules.fade(50f, 100f, 10f, 10f), 1e-6);
        assertEquals(0.5f, UndineRules.fade(95f, 100f, 10f, 10f), 1e-6);
        assertEquals(0f, UndineRules.fade(120f, 100f, 10f, 10f), 1e-6);
    }

    @Test
    void theFullFormIsShortAndRareAndTheCapsAreSmall() {
        assertTrue(UndineRules.FULL_TICKS <= 120, "a few seconds");
        assertTrue(UndineRules.FULL_COOLDOWN >= 4 * UndineRules.FULL_TICKS, "it comes at most once per cooldown, which is much longer than its stay");
        assertEquals(1, UndineRules.FULL_PER_PLAYER);
        assertEquals(1, UndineRules.SPIRITS_PER_PLAYER);
        assertEquals(48, UndineRules.DEFAULT_MAX_SPIRITS);
        assertEquals(6, UndineRules.DEFAULT_MAX_FULL);
        assertTrue(UndineRules.PACKET_GAP >= 5, "a key packet is answered at most a few times a second");
    }

    @Test
    void theLedgerKeepsBothCapsAndLetsGo() {
        UndineLedger l = new UndineLedger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(l.acquire(a, 1, 2));
        assertFalse(l.acquire(a, 1, 2), "one per player");
        assertTrue(l.acquire(b, 1, 2));
        assertFalse(l.acquire(c, 1, 2), "two on the whole server");
        l.release(a);
        assertTrue(l.acquire(c, 1, 2));
        assertEquals(2, l.total());
        l.release(a);
        l.release(a);
        assertEquals(2, l.total(), "releasing what a player does not hold changes nothing");
        l.forget(b);
        assertEquals(1, l.total());
        l.clear();
        assertEquals(0, l.total());
        assertFalse(l.acquire(null, 1, 1));
        assertFalse(l.acquire(a, 0, 5));
        assertFalse(l.acquire(a, 5, 0));
    }

    @Test
    void theSoundBudgetHasACueCooldownAnAnyGapAndAServerWideSecondCap() {
        UndineVoiceBudget v = new UndineVoiceBudget();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(v.tryAcquire(a, "cast", 100, 8, 12));
        assertFalse(v.tryAcquire(a, "cast", 104, 8, 12), "the same cue is on cooldown");
        assertFalse(v.tryAcquire(a, "link", 101, 0, 12), "never two sounds of one owner within the any-gap");
        assertTrue(v.tryAcquire(a, "link", 104, 0, 12));
        assertTrue(v.tryAcquire(a, "cast", 108, 8, 12));
        // the server cap per second
        UndineVoiceBudget w = new UndineVoiceBudget();
        int ok = 0;
        for (int i = 0; i < 40; i++) if (w.tryAcquire(UUID.randomUUID(), "ambient", 1000 + i / 4, 0, 5)) ok++;
        assertEquals(5, ok, "only five sounds in that second, however many owners ask");
        assertTrue(w.tryAcquire(UUID.randomUUID(), "ambient", 1040, 0, 5), "the next second starts again");
        assertFalse(v.tryAcquire(b, "cast", 100, 8, 0), "a cap of zero is silence");
        assertFalse(v.tryAcquire(null, "cast", 100, 8, 12));
        v.forget(a);
        v.prune(100000);
        assertEquals(0, v.tracked(), "forgotten and quiet owners leave the map");
    }
}
