package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Morris's Structural Tuning: what he learns from, how the learning fades, and the trade a tuning makes. */
class StructuralTuningTest {
    @Test
    void aBlowCountsHalfAfterTheHalfLife() {
        StructuralTuning t = new StructuralTuning();
        t.record(0, DamageKind.MELEE, 100);
        assertEquals(100.0, t.recent(0), 1e-6);
        assertEquals(50.0, t.recent(StructuralTuning.HALF_LIFE), 1e-6);
        assertEquals(25.0, t.recent(2 * StructuralTuning.HALF_LIFE), 1e-6);
    }

    @Test
    void theKindThatHurtMostLeadsAndLittleDataTeachesNothing() {
        StructuralTuning t = new StructuralTuning();
        assertNull(t.dominant(0, 1), "nothing recorded yet");
        t.record(0, DamageKind.MELEE, 30);
        t.record(10, DamageKind.RANGED, 50);
        t.record(20, DamageKind.MAGIC, 20);
        assertEquals(DamageKind.RANGED, t.dominant(30, 10));
        assertNull(t.dominant(30, 500), "under the minimum total he learns nothing");
        // melee pours in later and overtakes the older ranged damage
        t.record(200, DamageKind.MELEE, 80);
        assertEquals(DamageKind.MELEE, t.dominant(200, 10));
    }

    @Test
    void kindsThatCannotBeTunedAreNeverRecorded() {
        StructuralTuning t = new StructuralTuning();
        t.record(0, DamageKind.ANTI_MAGIC, 999);
        t.record(0, DamageKind.OTHER, 999);
        t.record(0, null, 999);
        t.record(0, DamageKind.MELEE, -5);
        t.record(0, DamageKind.MELEE, 0);
        assertEquals(0.0, t.recent(0), 1e-9);
        assertNull(t.last());
        assertFalse(t.tune(DamageKind.ANTI_MAGIC, 0, 0.3));
        assertFalse(t.tune(DamageKind.OTHER, 0, 0.3));
        assertFalse(t.tune(null, 0, 0.3));
        assertFalse(t.active(0));
    }

    @Test
    void aTuningTradesACutForAHole() {
        StructuralTuning t = new StructuralTuning();
        assertTrue(t.tune(DamageKind.MELEE, 100, 0.30));
        assertTrue(t.active(100) && t.active(100 + StructuralTuning.TUNE_TICKS - 1));
        assertFalse(t.active(100 + StructuralTuning.TUNE_TICKS), "twelve seconds and it is over");
        assertEquals(0.70f, t.factor(DamageKind.MELEE, 150), 1e-6f);
        assertEquals(StructuralTuning.HOLE, t.factor(DamageKind.RANGED, 150), 1e-6f);
        assertEquals(StructuralTuning.HOLE, t.factor(DamageKind.MAGIC, 150), 1e-6f);
        assertEquals(1f, t.factor(DamageKind.ANTI_MAGIC, 150), 1e-6f, "modification cannot touch anti-magic, for or against");
        assertEquals(1f, t.factor(DamageKind.OTHER, 150), 1e-6f);
        assertEquals(1f, t.factor(DamageKind.MELEE, 100 + StructuralTuning.TUNE_TICKS), 1e-6f, "no tuning, no factor");
        assertEquals(DamageKind.MELEE, t.tuned(150));
        assertNull(t.tuned(100 + StructuralTuning.TUNE_TICKS));
    }

    @Test
    void theCutCanNeverPassTheCeiling() {
        StructuralTuning t = new StructuralTuning();
        t.tune(DamageKind.MAGIC, 0, 5.0);
        assertEquals(StructuralTuning.MAX_REDUCTION, t.reduction(), 1e-9);
        assertEquals(1.0f - (float) StructuralTuning.MAX_REDUCTION, t.factor(DamageKind.MAGIC, 1), 1e-6f);
        t.tune(DamageKind.MAGIC, 0, -2.0);
        assertEquals(0.0, t.reduction(), 1e-9, "a negative cut is nothing");
        for (int stage = 0; stage <= 6; stage++) {
            double cut = StructuralTuning.cutFor(0.9, stage);
            assertTrue(cut <= StructuralTuning.MAX_REDUCTION + 1e-9 && cut >= 0, "stage " + stage + " cut " + cut);
        }
    }

    @Test
    void theCutGrowsWithThePowerStageAndCentresOnTheBaseAtSixtyPercent() {
        double base = 0.30;
        double s1 = StructuralTuning.cutFor(base, 1), s2 = StructuralTuning.cutFor(base, 2), s3 = StructuralTuning.cutFor(base, 3), s4 = StructuralTuning.cutFor(base, 4);
        assertTrue(s1 < s2 && s2 < s3 && s3 < s4, "stronger with every step: " + s1 + " " + s2 + " " + s3 + " " + s4);
        assertEquals(base, s2, 1e-9, "at 60% the base");
        assertEquals(0.0, StructuralTuning.cutFor(0.0, 4), 1e-9);
    }

    @Test
    void aPartyThatSwitchesKindBeatsOneThatDoesNot() {
        // twelve seconds, one blow a second of 10 damage: the party that keeps to the tuned kind versus one that rotates the three kinds
        StructuralTuning stay = new StructuralTuning(), rotate = new StructuralTuning();
        stay.tune(DamageKind.MELEE, 0, 0.30);
        rotate.tune(DamageKind.MELEE, 0, 0.30);
        DamageKind[] kinds = {DamageKind.MELEE, DamageKind.RANGED, DamageKind.MAGIC};
        double kept = 0, rotated = 0, switched = 0;
        for (int s = 0; s < 12; s++) {
            long now = s * 20L;
            kept += 10 * stay.factor(DamageKind.MELEE, now);
            rotated += 10 * rotate.factor(kinds[s % 3], now);
            switched += 10 * rotate.factor(DamageKind.RANGED, now);
        }
        assertTrue(rotated > kept, "rotating is better than staying: " + rotated + " vs " + kept);
        assertTrue(switched > rotated, "switching entirely away is best: " + switched);
        assertTrue(switched > 120.0 && kept < 120.0, "switching away beats an untuned boss, staying loses to it");
    }

    @Test
    void dropForgetsTheTuningButNotWhatWasLearnedAndResetForgetsAll() {
        StructuralTuning t = new StructuralTuning();
        t.record(0, DamageKind.RANGED, 40);
        t.tune(DamageKind.RANGED, 0, 0.3);
        t.drop();
        assertFalse(t.active(1));
        assertEquals(DamageKind.RANGED, t.dominant(1, 5), "the ledger stays");
        t.reset();
        assertNull(t.dominant(1, 0.001));
        assertNull(t.last());
    }
}
