package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.elite.world.rules.CrystalGrowth;
import com.newuniverse.nusmp.elite.world.rules.CrystalStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The growth rules: the five stages in the owner's order, thresholds, chances, the Mother Rock conditions, charge and seeding. */
class CrystalGrowthTest {
    @Test
    void theFiveStagesAreInTheOwnersOrderAndChainUp() {
        CrystalStage[] s = CrystalStage.values();
        assertEquals(5, s.length);
        assertEquals(CrystalStage.SMALL_BUD, s[0]);
        assertEquals(CrystalStage.MEDIUM_BUD, s[1]);
        assertEquals(CrystalStage.LARGE_BUD, s[2]);
        assertEquals(CrystalStage.CLUSTER, s[3]);
        assertEquals(CrystalStage.MOTHER_ROCK, s[4]);
        assertEquals(CrystalStage.MEDIUM_BUD, CrystalStage.SMALL_BUD.next());
        assertEquals(CrystalStage.MOTHER_ROCK, CrystalStage.CLUSTER.next());
        assertNull(CrystalStage.MOTHER_ROCK.next());
        assertTrue(CrystalStage.LARGE_BUD.isBud());
        assertFalse(CrystalStage.CLUSTER.isBud());
    }

    @Test
    void budsOnlyRipenAboveTheirThreshold() {
        assertEquals(0.0, CrystalGrowth.growChance(CrystalStage.SMALL_BUD, 0.10, 0.25), 1e-9);
        assertTrue(CrystalGrowth.growChance(CrystalStage.SMALL_BUD, 0.15, 0.25) > 0.0);
        assertEquals(0.0, CrystalGrowth.growChance(CrystalStage.MEDIUM_BUD, 0.29, 0.25), 1e-9);
        assertEquals(0.0, CrystalGrowth.growChance(CrystalStage.LARGE_BUD, 0.44, 0.25), 1e-9);
        assertTrue(CrystalGrowth.growChance(CrystalStage.LARGE_BUD, 0.50, 0.25) > 0.0);
    }

    @Test
    void chanceRisesWithDensityAndWithTheBase() {
        double lo = CrystalGrowth.growChance(CrystalStage.SMALL_BUD, 0.3, 0.25);
        double hi = CrystalGrowth.growChance(CrystalStage.SMALL_BUD, 0.9, 0.25);
        assertTrue(hi > lo);
        assertTrue(hi <= 0.25 + 1e-9, "never above the base");
        assertEquals(0.0, CrystalGrowth.growChance(CrystalStage.SMALL_BUD, 0.9, 0.0), 1e-9, "base 0 switches growth off");
        assertTrue(CrystalGrowth.growChance(CrystalStage.SMALL_BUD, 0.9, 5.0) <= 1.0);
    }

    @Test
    void clusterAndMotherRockNeverRipenByTheOrdinaryRule() {
        assertEquals(0.0, CrystalGrowth.growChance(CrystalStage.CLUSTER, 1.0, 1.0), 1e-9);
        assertEquals(0.0, CrystalGrowth.growChance(CrystalStage.MOTHER_ROCK, 1.0, 1.0), 1e-9);
        assertEquals(0.0, CrystalGrowth.growChance(null, 1.0, 1.0), 1e-9);
    }

    @Test
    void aMotherRockNeedsDensityAGroveRoomAndLuck() {
        assertFalse(CrystalGrowth.motherReady(0.80, 5, 0, 2, 0.04, 0.0), "not dense enough");
        assertFalse(CrystalGrowth.motherReady(0.95, 2, 0, 2, 0.04, 0.0), "a lone cluster is not a grove");
        assertFalse(CrystalGrowth.motherReady(0.95, 5, 2, 2, 0.04, 0.0), "the chunk is full");
        assertFalse(CrystalGrowth.motherReady(0.95, 5, 0, 2, 0.04, 0.99), "bad luck");
        assertTrue(CrystalGrowth.motherReady(0.95, 5, 0, 2, 0.04, 0.0));
        assertFalse(CrystalGrowth.motherReady(1.0, 9, 0, 0, 1.0, 0.0), "a cap of 0 forbids mother rocks");
    }

    @Test
    void chargeAndSeeding() {
        assertTrue(CrystalGrowth.chargeChance(0, 0.5) > 0.0);
        assertEquals(0.0, CrystalGrowth.chargeChance(CrystalGrowth.MAX_CHARGE, 1.0), 1e-9, "a full rock gains nothing");
        assertEquals(0.0, CrystalGrowth.seedChance(0, 1.0, 1.0), 1e-9, "no charge, no seed");
        assertTrue(CrystalGrowth.seedChance(3, 1.0, 0.1) > CrystalGrowth.seedChance(1, 1.0, 0.1), "more charge seeds more");
        assertTrue(CrystalGrowth.seedChance(3, 1.0, 100.0) <= 1.0);
        assertEquals(0.0, CrystalGrowth.seedChance(3, 1.0, 0.0), 1e-9);
    }

    @Test
    void motherBonusIsCapped() {
        assertEquals(0.0, CrystalGrowth.motherBonus(0, 5), 1e-9);
        assertEquals(0.06 + 0.04, CrystalGrowth.motherBonus(1, 2), 1e-9);
        assertEquals(0.30, CrystalGrowth.motherBonus(50, 150), 1e-9);
    }

    @Test
    void seedingNeedsAFreeFaceNextToSolid() {
        assertTrue(CrystalGrowth.canSeed(true, true));
        assertFalse(CrystalGrowth.canSeed(false, true));
        assertFalse(CrystalGrowth.canSeed(true, false));
    }

    @Test
    void stageWeightsMatchTheDocumentedScore() {
        assertEquals(1, CrystalStage.SMALL_BUD.weight);
        assertEquals(2, CrystalStage.MEDIUM_BUD.weight);
        assertEquals(3, CrystalStage.LARGE_BUD.weight);
        assertEquals(5, CrystalStage.CLUSTER.weight);
        assertEquals(0, CrystalStage.MOTHER_ROCK.weight, "the Mother Rock counts through its own bonus, not the score");
    }
}
