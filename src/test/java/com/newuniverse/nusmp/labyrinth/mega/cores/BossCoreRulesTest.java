package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Family;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Kit;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Resource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure rules of the Boss Cores: which family and resource a level pays, how many by floor, the tiers and their cap, the numbers and their clamps. */
class BossCoreRulesTest {
    private static int count(int floor) { return BossCoreRules.count(floor, 250, 500, 750, 1000, 2, 3, 4, 5); }

    @Test
    void familyIsLevelModFourAndResourceIsLevelOverFourModFour() {
        assertEquals(Family.AEGIS, BossCoreRules.familyOfLevel(28));
        assertEquals(Family.STRIKER, BossCoreRules.familyOfLevel(25));
        assertEquals(Family.WELLSPRING, BossCoreRules.familyOfLevel(26));
        assertEquals(Family.ASCENDANT, BossCoreRules.familyOfLevel(27));
        assertEquals(Resource.KI, BossCoreRules.resourceOfLevel(26));         // 26 / 4 = 6, 6 mod 4 = 2
        assertEquals(Resource.NIHILITY, BossCoreRules.resourceOfLevel(30));
        assertEquals(Resource.MAGICULE, BossCoreRules.resourceOfLevel(34));
        assertEquals(Resource.AURA, BossCoreRules.resourceOfLevel(38));
        assertEquals(Kit.WELLSPRING_KI, BossCoreRules.kitOfLevel(26));
        assertEquals(Kit.STRIKER, BossCoreRules.kitOfLevel(25));
    }

    @Test
    void bothSidesPayTheSameAndEveryCoreIsReachable() {
        Set<Kit> seen = new HashSet<>();
        for (int level = 25; level <= 100; level++) {
            assertEquals(BossCoreRules.kitOfLevel(level), BossCoreRules.kitOfLevel(-level), "level " + level);
            seen.add(BossCoreRules.kitOfLevel(level));
        }
        assertEquals(BossCoreRules.KITS, seen.size());
        assertEquals(7, Kit.values().length);
    }

    @Test
    void deterministicAndTotalOverTheWholeRange() {
        for (int level = -1000; level <= 1000; level++) {
            assertEquals(BossCoreRules.kitOfLevel(level), BossCoreRules.kitOfLevel(level));
            assertTrue(BossCoreRules.kitOfLevel(level) != null);
        }
        assertTrue(BossCoreRules.kitOfLevel(Integer.MIN_VALUE) != null);
    }

    @Test
    void countsGrowByBandAndFloor249PaysNothing() {
        assertEquals(0, count(0));
        assertEquals(0, count(240));
        assertEquals(0, count(249));
        assertEquals(2, count(250));
        assertEquals(2, count(499));
        assertEquals(3, count(500));
        assertEquals(3, count(749));
        assertEquals(4, count(750));
        assertEquals(4, count(999));
        assertEquals(5, count(1000));
        assertEquals(5, count(5000));
        assertEquals(2, count(-250));
        assertEquals(5, count(-1000));
        assertEquals(0, count(-249));
        assertEquals(5, count(Integer.MIN_VALUE));
    }

    @Test
    void countsAreClampedAndTheMinFloorMoves() {
        assertEquals(64, BossCoreRules.count(300, 250, 500, 750, 1000, 999, 3, 4, 5));
        assertEquals(0, BossCoreRules.count(300, 250, 500, 750, 1000, -4, 3, 4, 5));
        assertEquals(2, BossCoreRules.count(100, 100, 500, 750, 1000, 2, 3, 4, 5));
        assertEquals(0, BossCoreRules.count(99, 100, 500, 750, 1000, 2, 3, 4, 5));
    }

    @Test
    void idsFollowTheKit() {
        assertEquals("core_aegis", Kit.AEGIS.coreId());
        assertEquals("core_wellspring_nihility", Kit.WELLSPRING_NIHILITY.coreId());
        assertEquals("aegis_helmet", Kit.AEGIS.pieceId(BossCoreRules.Piece.HELMET));
        assertEquals("wellspring_aura_boots", Kit.WELLSPRING_AURA.pieceId(BossCoreRules.Piece.BOOTS));
        assertEquals(Kit.WELLSPRING_KI, Kit.of(Family.WELLSPRING, Resource.KI));
        assertEquals(Kit.ASCENDANT, Kit.of(Family.ASCENDANT, null));
    }

    @Test
    void tiersFollowThePiecesAndMixingIsCapped() {
        assertEquals(0, BossCoreRules.tierOfPieces(1));
        assertEquals(1, BossCoreRules.tierOfPieces(2));
        assertEquals(1, BossCoreRules.tierOfPieces(3));
        assertEquals(2, BossCoreRules.tierOfPieces(4));
        int[] mix = new int[BossCoreRules.KITS];
        mix[Kit.AEGIS.ordinal()] = 2;
        mix[Kit.STRIKER.ordinal()] = 2;
        int[] both = BossCoreRules.cappedTiers(mix, 2);
        assertEquals(1, both[Kit.AEGIS.ordinal()]);
        assertEquals(1, both[Kit.STRIKER.ordinal()]);
        int[] one = BossCoreRules.cappedTiers(mix, 1);
        assertEquals(1, one[Kit.AEGIS.ordinal()] + one[Kit.STRIKER.ordinal()]);
        int[] full = new int[BossCoreRules.KITS];
        full[Kit.ASCENDANT.ordinal()] = 4;
        assertEquals(2, BossCoreRules.cappedTiers(full, 2)[Kit.ASCENDANT.ordinal()]);
        assertEquals(1, BossCoreRules.cappedTiers(full, 1)[Kit.ASCENDANT.ordinal()]);
        assertEquals(0, BossCoreRules.cappedTiers(full, 0)[Kit.ASCENDANT.ordinal()]);
        assertEquals(0, BossCoreRules.cappedTiers(new int[BossCoreRules.KITS], 4)[0]);
    }

    @Test
    void aegisResistanceAddsUpAndIsCapped() {
        assertEquals(0.0, BossCoreRules.resistPercent(0, 0, 5, 3, 7, 60), 1e-9);
        assertEquals(5.0, BossCoreRules.resistPercent(1, 0, 5, 3, 7, 60), 1e-9);
        assertEquals(13.0, BossCoreRules.resistPercent(2, 1, 5, 3, 7, 60), 1e-9);
        assertEquals(27.0, BossCoreRules.resistPercent(4, 2, 5, 3, 7, 60), 1e-9);
        assertEquals(60.0, BossCoreRules.resistPercent(4, 2, 25, 50, 50, 60), 1e-9);
        assertEquals(0.0, BossCoreRules.resistPercent(4, 2, -5, -3, -7, 60), 1e-9);
    }

    @Test
    void wellspringRegenIsPerPieceTimesTierFactorAndCapped() {
        assertEquals(0.5, BossCoreRules.regenPercent(1, 0, 0.5, 1.5, 2.0, 6), 1e-9);
        assertEquals(4.0, BossCoreRules.regenPercent(4, 2, 0.5, 1.5, 2.0, 6), 1e-9);
        assertEquals(6.0, BossCoreRules.regenPercent(4, 2, 10, 5, 10, 6), 1e-9);
        assertEquals(0.0, BossCoreRules.regenPercent(0, 2, 0.5, 1.5, 2.0, 6), 1e-9);
    }

    @Test
    void epGainIsAShareOfTheVictimAndCappedByTheWearer() {
        assertEquals(0.0, BossCoreRules.epBonus(0, 4, 2, 0.5, 1.5, 2.0, 1000, 0.5), 1e-9);
        assertEquals(0.0, BossCoreRules.epBonus(1000, 0, 0, 0.5, 1.5, 2.0, 1000, 0.5), 1e-9);
        assertEquals(1.0, BossCoreRules.epBonus(100, 2, 0, 0.5, 1.5, 2.0, 1_000_000, 0.5), 1e-9);
        assertEquals(5.0, BossCoreRules.epBonus(1e15, 4, 2, 0.5, 1.5, 2.0, 1000, 0.5), 1e-9);
        assertEquals(0.0, BossCoreRules.epBonus(1e15, 4, 2, 0.5, 1.5, 2.0, -5, 0.5), 1e-9);
    }

    @Test
    void cooldownFallsAtLeastByOneAndNeverBelowZero() {
        assertEquals(0, BossCoreRules.reduceCooldown(0, 50));
        assertEquals(9, BossCoreRules.reduceCooldown(10, 0));
        assertEquals(5, BossCoreRules.reduceCooldown(10, 50));
        assertEquals(0, BossCoreRules.reduceCooldown(1, 50));
        assertEquals(0, BossCoreRules.reduceCooldown(3, 100));
    }

    @Test
    void secondDamageTypeShareAndForeignNamespaces() {
        assertEquals(1.0f, BossCoreRules.extraShare(10f, 10.0), 1e-6);
        assertEquals(0f, BossCoreRules.extraShare(0f, 10.0), 1e-6);
        assertEquals(0f, BossCoreRules.extraShare(10f, 0.0), 1e-6);
        assertTrue(BossCoreRules.foreignNamespace("tensura"));
        assertTrue(BossCoreRules.foreignNamespace("tensuranoindex"));
        assertFalse(BossCoreRules.foreignNamespace("minecraft"));
        assertFalse(BossCoreRules.foreignNamespace(""));
        assertFalse(BossCoreRules.foreignNamespace(null));
        assertNotEquals(Kit.AEGIS, Kit.STRIKER);
    }
}
