package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: what is decided when a sector is created (its road, its place in it, its floors, its threat, its theme, its seed and its relic count) is what every other
 * module of the Mega-Dungeon reads, and it must be the same in every world with the same dungeon seed whichever road the players opened first. These tests pin each
 * of those decisions, longhand, so a change in {@link SectorDraft} or {@link SectorThreat} cannot slip through as a change in difficulty or in looks.
 */
class SectorDraftTest {
    private static final long SEED = 0x5EEDL;

    @Test
    void threatIsTheDistanceFromFloorZeroOfTheSectorsFirstFloor() {
        assertEquals(1, SectorThreat.of(Polarity.HOLY, 0, 10), "the first Holy sector keeps the threat 1.54.0 gave its sector 0");
        assertEquals(11, SectorThreat.of(Polarity.HOLY, 1, 10));
        assertEquals(31, SectorThreat.of(Polarity.HOLY, 3, 10));
        assertEquals(1, SectorThreat.of(Polarity.ABYSSAL, 0, 10), "the roads are equally hard at the same distance");
        assertEquals(21, SectorThreat.of(Polarity.ABYSSAL, 2, 10));
        assertEquals(6, SectorThreat.of(Polarity.HOLY, 1, 5), "five floors per sector: the second sector starts at floor 6");
        for (int k = 0; k < 20; k++) {
            assertEquals(FloorMap.threat(FloorMap.firstFloor(Polarity.HOLY, k, 10)), SectorThreat.of(Polarity.HOLY, k, 10));
            assertEquals(SectorThreat.of(Polarity.HOLY, k, 10), SectorThreat.of(Polarity.ABYSSAL, k, 10));
        }
        assertEquals(1, SectorThreat.of(Polarity.HOLY, -4, 0), "a corrupt index or length still gives a sane threat");
    }

    @Test
    void theSeedKeyIsEvenForTheHolyRoadAndOddForTheAbyssalOne() {
        assertEquals(0, SectorDraft.seedKey(Polarity.HOLY, 0), "the number 1.54.0 used for its sector 0");
        assertEquals(1, SectorDraft.seedKey(Polarity.ABYSSAL, 0));
        assertEquals(6, SectorDraft.seedKey(Polarity.HOLY, 3));
        assertEquals(7, SectorDraft.seedKey(Polarity.ABYSSAL, 3));
        Set<Integer> seen = new HashSet<>();
        for (int k = 0; k < 100; k++) {
            assertTrue(seen.add(SectorDraft.seedKey(Polarity.HOLY, k)));
            assertTrue(seen.add(SectorDraft.seedKey(Polarity.ABYSSAL, k)));
        }
    }

    @Test
    void theSeedBelongsToThePlaceInTheRoadNotToTheGlobalId() {
        for (int k = 0; k < 6; k++) {
            SectorDraft a = SectorDraft.of(SEED, 2 * k, Polarity.HOLY, k, 10, RegionTheme.GRAVITY, null);
            SectorDraft b = SectorDraft.of(SEED, 7 * k + 3, Polarity.HOLY, k, 10, RegionTheme.GRAVITY, null);
            assertEquals(a.seed(), b.seed(), "the Holy road's sector " + k + " is the same sector whichever id it was given");
            assertTrue(a.seed() != SectorDraft.of(SEED, 2 * k, Polarity.ABYSSAL, k, 10, RegionTheme.GRAVITY, null).seed(), "the roads have their own seeds");
            assertTrue(a.seed() != SectorDraft.of(SEED + 1, 2 * k, Polarity.HOLY, k, 10, RegionTheme.GRAVITY, null).seed(), "another dungeon seed, another sector");
        }
        assertEquals(SectorPlanner.sectorSeed(SEED, 0), SectorPlanner.first(SEED).seed(), "sector 0 of 1.54.0 keeps its seed");
        assertEquals(SectorPlanner.first(SEED).seed(), SectorDraft.first(SEED, 0, Polarity.HOLY).seed());
        assertTrue(SectorDraft.first(SEED, 0, Polarity.HOLY).seed() != SectorDraft.first(SEED, 1, Polarity.ABYSSAL).seed(), "the first sectors of the two roads have different seeds");
    }

    @Test
    void theFirstSectorOfARoadWearsTheRoadsSignatureTheme() {
        assertEquals(RegionTheme.RADIANT, SectorDraft.first(SEED, 0, Polarity.HOLY).theme());
        assertEquals(RegionTheme.ABYSS, SectorDraft.first(SEED, 1, Polarity.ABYSSAL).theme());
        for (long s = 0; s < 40; s++) {
            assertEquals(RegionTheme.RADIANT, SectorDraft.first(s, 5, Polarity.HOLY).theme());
            assertEquals(RegionTheme.ABYSS, SectorDraft.first(s, 5, Polarity.ABYSSAL).theme());
        }
    }

    @Test
    void theCrucibleChoiceForcesTheThemeAndTheSeededDrawOtherwiseNeverRepeatsTheLastOne() {
        for (RegionTheme forced : RegionTheme.values()) {
            SectorDraft d = SectorDraft.of(SEED, 4, Polarity.HOLY, 2, 10, RegionTheme.FROST, forced);
            assertEquals(forced, d.theme(), "the relic taken in the vault decides, even if it repeats the last theme");
        }
        for (Polarity road : Polarity.values()) {
            for (int k = 1; k < 60; k++) {
                for (RegionTheme prev : RegionTheme.values()) {
                    SectorDraft d = SectorDraft.of(SEED, k, road, k, 10, prev, null);
                    assertTrue(prev != d.theme(), "two identical regions in a row would read as a bug");
                    assertEquals(d.theme(), SectorDraft.of(SEED, k + 11, road, k, 10, prev, null).theme(), "and the draw does not depend on the global id");
                }
            }
        }
    }

    @Test
    void aDraftCarriesTheRoadThePlaceTheFloorsAndTheRelicCount() {
        SectorDraft d = SectorDraft.of(SEED, 9, Polarity.ABYSSAL, 3, 10, RegionTheme.ABYSS, null);
        assertEquals(9, d.id());
        assertEquals(Polarity.ABYSSAL, d.road());
        assertEquals(3, d.chainIndex());
        assertEquals(10, d.perRegion());
        assertEquals(31, d.threat());
        assertEquals(3, d.relicCount(), "every theme offers exactly three relics");
        assertEquals(d.theme().relicCount(SEED, SectorDraft.seedKey(Polarity.ABYSSAL, 3)), d.relicCount());
        SectorDraft clamped = SectorDraft.of(SEED, 0, Polarity.HOLY, -5, 0, null, null);
        assertEquals(0, clamped.chainIndex());
        assertEquals(1, clamped.perRegion());
    }

    @Test
    void theFirstSectorsOfTheTwoRoadsStandOnTheirLanesAndGrowEast() {
        SectorPlanner.Plan holy = SectorPlanner.first(SEED, 0, Polarity.HOLY), abyss = SectorPlanner.first(SEED, 1, Polarity.ABYSSAL);
        assertEquals(new Vec(0, SectorPlanner.DOOR_Y, 0), holy.entry());
        assertEquals(new Vec(0, SectorPlanner.DOOR_Y, MegaGrid.LANE_Z), abyss.entry());
        assertEquals(Facing.EAST, holy.forward());
        assertEquals(Facing.EAST, abyss.forward());
        assertEquals(new Vec(0, SectorPlanner.DOOR_Y - 1, MegaGrid.LANE_Z), abyss.entryFloor(), "the cell the saved sector stores is the floor cell of the door");
        assertEquals(holy.region().minZ() + MegaGrid.LANE_Z, abyss.region().minZ(), "the two regions are the same square 300 000 blocks apart");
        assertEquals(holy.region().maxX(), abyss.region().maxX());
        assertTrue(holy.region().gapXZ(abyss.region()) > 200_000, "the roads never come near each other");
        assertEquals(1, abyss.id());
        assertEquals(SectorPlanner.first(SEED), SectorPlanner.first(SEED, 0, Polarity.HOLY), "the old call is the Holy road");
    }
}
