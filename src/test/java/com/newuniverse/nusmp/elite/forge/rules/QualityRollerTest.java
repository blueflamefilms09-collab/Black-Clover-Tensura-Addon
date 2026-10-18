package com.newuniverse.nusmp.elite.forge.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the quality roll is the heart of the forge and a pure class, so it is tested to the number: the thresholds, the station ceilings, the effort floor, the caps
 * on the flat bonuses, monotonicity in every input, and that a Mythical item needs a Dragonforge station.
 */
class QualityRollerTest {
    private static QualityRoller.Inputs in(double score, StationTier t, int base, int mat, int skill, int spec, double luck) {
        return new QualityRoller.Inputs(score, t, base, mat, skill, spec, luck);
    }

    @Test
    void thresholdsMapPointsToQualities() {
        assertEquals(Quality.COMMON, Quality.byPoints(0));
        assertEquals(Quality.COMMON, Quality.byPoints(29.9));
        assertEquals(Quality.UNCOMMON, Quality.byPoints(30));
        assertEquals(Quality.RARE, Quality.byPoints(45));
        assertEquals(Quality.EPIC, Quality.byPoints(60));
        assertEquals(Quality.LEGENDARY, Quality.byPoints(75));
        assertEquals(Quality.MYTHICAL, Quality.byPoints(92));
        assertEquals(Quality.MYTHICAL, Quality.byPoints(500));
        assertEquals(6, Quality.values().length);
        for (int i = 1; i <= 6; i++) assertEquals(i, Quality.ofLevel(i).level());
        assertNull(Quality.ofLevel(0));
        assertNull(Quality.ofLevel(7));
        assertEquals(Quality.EPIC, Quality.byId("EPIC"));
    }

    @Test
    void theStationCeilingHoldsTheQualityBack() {
        // a perfect play with every bonus at a Basic station: points are huge, the quality stops at Rare
        QualityRoller.Result basic = QualityRoller.roll(in(1.0, StationTier.BASIC, 30, 8, 16, 6, 1.0));
        assertEquals(Quality.RARE, basic.quality());
        assertEquals(Quality.MYTHICAL, basic.uncapped());
        assertTrue(basic.cappedByStation());
        assertEquals(Quality.EPIC, QualityRoller.roll(in(1.0, StationTier.MAGISTEEL, 30, 8, 16, 6, 1.0)).quality());
        assertEquals(Quality.LEGENDARY, QualityRoller.roll(in(1.0, StationTier.MYTHRIL, 30, 8, 16, 6, 1.0)).quality());
        QualityRoller.Result top = QualityRoller.roll(in(1.0, StationTier.DRAGONFORGE, 30, 8, 16, 6, 1.0));
        assertEquals(Quality.MYTHICAL, top.quality());
        assertFalse(top.cappedByStation());
    }

    @Test
    void aMythicalNeedsADragonforgeStation() {
        for (StationTier t : StationTier.values()) {
            Quality q = QualityRoller.roll(in(1.0, t, 30, 8, 16, 6, 1.0)).quality();
            assertEquals(t == StationTier.DRAGONFORGE, q == Quality.MYTHICAL, t.toString());
        }
    }

    @Test
    void theEffortFloorStopsFlatBonusesFromCarryingAnItem() {
        QualityRoller.Result r = QualityRoller.roll(in(0.05, StationTier.DRAGONFORGE, 30, 8, 16, 6, 1.0));
        assertEquals(Quality.COMMON, r.quality());
        assertTrue(QualityRoller.roll(in(0.5, StationTier.DRAGONFORGE, 30, 8, 16, 6, 0)).quality().level() > 1);
    }

    @Test
    void flatBonusesAreCapped() {
        double a = QualityRoller.points(in(0.5, StationTier.BASIC, 10, 8, 16, 6, 0));
        double b = QualityRoller.points(in(0.5, StationTier.BASIC, 10, 999, 999, 6, 0));
        assertEquals(a, b, 1e-9);
        assertEquals(a, QualityRoller.points(in(0.5, StationTier.BASIC, 10, 8, 16, 6, 0)), 1e-9);
        // score and luck are clamped too
        assertEquals(QualityRoller.points(in(1.0, StationTier.BASIC, 0, 0, 0, 0, 1.0)), QualityRoller.points(in(7.0, StationTier.BASIC, 0, 0, 0, 0, 9.0)), 1e-9);
        assertTrue(QualityRoller.points(in(-3, StationTier.BASIC, -9, -9, -9, -9, -9)) >= 0);
    }

    @Test
    void theRollIsMonotonicInEveryInput() {
        double prev = -1;
        for (int s = 0; s <= 100; s++) {
            double p = QualityRoller.points(in(s / 100.0, StationTier.MYTHRIL, 12, 3, 5, 0, 0));
            assertTrue(p >= prev - 1e-9, "points must not fall as the score rises");
            prev = p;
        }
        for (StationTier t : StationTier.values()) {
            if (t.next() != null) assertTrue(QualityRoller.points(in(0.7, t.next(), 10, 0, 0, 0, 0)) > QualityRoller.points(in(0.7, t, 10, 0, 0, 0, 0)));
        }
        assertTrue(QualityRoller.points(in(0.7, StationTier.BASIC, 10, 0, 6, 0, 0)) > QualityRoller.points(in(0.7, StationTier.BASIC, 10, 0, 0, 0, 0)));
        assertTrue(QualityRoller.points(in(0.7, StationTier.BASIC, 10, 0, 0, 6, 0)) > QualityRoller.points(in(0.7, StationTier.BASIC, 10, 0, 0, 0, 0)));
        assertTrue(QualityRoller.points(in(0.7, StationTier.BASIC, 10, 4, 0, 0, 0)) > QualityRoller.points(in(0.7, StationTier.BASIC, 10, 0, 0, 0, 0)));
        assertTrue(QualityRoller.points(in(0.7, StationTier.BASIC, 10, 0, 0, 0, 0.5)) > QualityRoller.points(in(0.7, StationTier.BASIC, 10, 0, 0, 0, -0.5)));
    }

    @Test
    void luckMapsTheUnitIntervalToMinusOneToOne() {
        assertEquals(-1.0, QualityRoller.luckFrom(0.0), 1e-9);
        assertEquals(0.0, QualityRoller.luckFrom(0.5), 1e-9);
        assertEquals(1.0, QualityRoller.luckFrom(1.0), 1e-9);
        assertEquals(1.0, QualityRoller.luckFrom(5.0), 1e-9);
    }

    @Test
    void aTypicalPlayPathGivesEveryQualityAtTheRightPlace() {
        // an ordinary good play (score 0.8) with a modest base: Basic -> Rare, with help the higher tiers rise
        assertEquals(Quality.RARE, QualityRoller.roll(in(0.8, StationTier.BASIC, 10, 0, 0, 0, 0)).quality());
        assertEquals(Quality.EPIC, QualityRoller.roll(in(0.8, StationTier.MAGISTEEL, 14, 0, 2, 0, 0)).quality());
        assertEquals(Quality.LEGENDARY, QualityRoller.roll(in(0.9, StationTier.MYTHRIL, 15, 4, 6, 6, 0)).quality());
        assertEquals(Quality.MYTHICAL, QualityRoller.roll(in(0.95, StationTier.DRAGONFORGE, 18, 4, 8, 6, 0)).quality());
        assertEquals(Quality.COMMON, QualityRoller.roll(in(0.1, StationTier.BASIC, 5, 0, 0, 0, 0)).quality());
        assertEquals(Quality.UNCOMMON, QualityRoller.roll(in(0.4, StationTier.BASIC, 10, 0, 0, 0, 0)).quality());
    }

    @Test
    void stationsAndSpecializationsAreWellFormed() {
        assertEquals(4, StationTier.values().length);
        assertEquals(StationTier.MAGISTEEL, StationTier.BASIC.next());
        assertNull(StationTier.DRAGONFORGE.next());
        assertTrue(StationTier.MYTHRIL.canForge(2) && StationTier.MYTHRIL.canForge(0) && !StationTier.MYTHRIL.canForge(3));
        int prevBonus = -1, prevCeil = 0;
        for (StationTier t : StationTier.values()) {
            assertTrue(t.qualityBonus() > prevBonus && t.ceiling().level() > prevCeil, "each tier must be better than the last");
            prevBonus = t.qualityBonus();
            prevCeil = t.ceiling().level();
        }
        for (Specialization s : Specialization.values()) {
            assertEquals(Specialization.CATEGORY_POINTS, s.categoryPoints(s.category()));
            for (Category c : Category.values()) if (c != s.category()) assertEquals(0, s.categoryPoints(c));
            assertEquals(s, Specialization.byId(s.id()));
            assertTrue(s.pool().size() >= 4);
        }
        assertNull(Specialization.byId(""));
        assertNull(Specialization.byId("nope"));
    }
}
