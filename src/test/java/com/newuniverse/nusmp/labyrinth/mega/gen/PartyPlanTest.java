package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyLanes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: instancing the dungeon per party rests on the planner putting a party's floors on that party's lane and nowhere else, giving each party seeds of its own, and
 * leaving party 0 (the shared lane, every world saved before parties, {@code party.mode = shared}) planning EXACTLY what it always planned. These are pure arithmetic, so they
 * are proved here for many parties and floors at once: the legacy equivalence, the lane of every floor, the distinct seeds, the floors of different parties never closer than
 * the gap between two regions, the draft carrying the party, and the obstacle rule that keeps one party's plan independent of how far another party has got.
 */
public class PartyPlanTest {
    private static final long SEED = 0x5EED_1234_ABCDL;

    @Test
    public void partyZeroPlansExactlyWhatItAlwaysPlanned() {
        for (Polarity road : Polarity.values()) {
            assertEquals(SectorPlanner.first(SEED, 3, road), SectorPlanner.first(SEED, 3, road, 0));
            assertEquals(SectorPlanner.sectorSeed(SEED, SectorDraft.seedKey(road, 0)), SectorPlanner.first(SEED, 3, road).seed(), "the seed formula of 1.55.0");
            for (int idx = 0; idx < 25; idx++) {
                boolean boss = FloorMap.isBossFloor(FloorMap.floorOfSector(road, idx));
                SectorPlanner.Plan legacy = SectorPlanner.floor(SEED, 7, road, idx, 28, boss);
                assertEquals(legacy, SectorPlanner.floor(SEED, 7, road, idx, 28, boss, 0), "floor " + idx + " of the " + road.id() + " road");
                assertEquals(SectorPlanner.sectorSeed(SEED, SectorDraft.seedKey(road, idx)), legacy.seed(), "the seed formula of 1.58.0");
                assertEquals(FloorPlan.entry(road, idx), FloorPlan.entry(road, idx, 0));
                assertEquals(MegaGrid.laneZ(road), legacy.entry().z());
            }
            SectorDraft d = SectorDraft.of(SEED, 5, road, 12, 1, RegionTheme.FROST, null);
            assertEquals(d, SectorDraft.of(SEED, 5, road, 12, 1, RegionTheme.FROST, null, 0));
            assertEquals(0, d.party());
            assertEquals(SEED, SectorDraft.partySeed(SEED, 0));
            assertEquals(SEED, SectorDraft.partySeed(SEED, -4), "a negative index is read as 0");
        }
    }

    @Test
    public void everyFloorOfEveryPartyStandsOnItsPartysLaneAndOnlyThere() {
        for (int party = 0; party < 64; party++) {
            for (Polarity road : Polarity.values()) {
                int lane = MegaGrid.laneZ(road, party);
                for (int idx : new int[] {0, 1, 9, 24}) {
                    boolean boss = FloorMap.isBossFloor(FloorMap.floorOfSector(road, idx));
                    SectorPlanner.Plan p = SectorPlanner.floor(SEED, 1, road, idx, 28, boss, party);
                    assertEquals(new Vec(idx * FloorPlan.PITCH, SectorPlanner.DOOR_Y, lane), p.entry(), "party " + party + " " + road.id() + " floor " + idx);
                    assertTrue(p.region().minZ() >= lane - PartyLanes.REACH && p.region().maxZ() <= lane + PartyLanes.REACH, "the region stays within the reach of its lane");
                    assertTrue(p.region().minZ() <= lane && p.region().maxZ() >= lane, "the region holds the lane");
                    assertEquals(party, PartyLanes.partyAt(p.region().minZ(), 64));
                    assertEquals(party, PartyLanes.partyAt(p.region().maxZ(), 64));
                    assertEquals(road, PartyLanes.roadAt(p.region().minZ()));
                }
            }
        }
    }

    @Test
    public void floorsOfDifferentPartiesNeverComeNearEachOther() {
        List<Box> regions = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int party = 0; party < 6; party++) {
            for (Polarity road : Polarity.values()) {
                for (int idx = 0; idx < 12; idx++) {
                    boolean boss = FloorMap.isBossFloor(FloorMap.floorOfSector(road, idx));
                    regions.add(SectorPlanner.floor(SEED, 1, road, idx, 28, boss, party).region());
                    names.add("party " + party + " " + road.id() + " floor " + idx);
                }
            }
        }
        for (int i = 0; i < regions.size(); i++) {
            for (int j = i + 1; j < regions.size(); j++) {
                assertFalse(regions.get(i).overlaps(regions.get(j)), names.get(i) + " and " + names.get(j) + " overlap");
                boolean sameChain = names.get(i).substring(0, names.get(i).lastIndexOf(" floor")).equals(names.get(j).substring(0, names.get(j).lastIndexOf(" floor")));
                if (!sameChain) assertTrue(regions.get(i).gapXZ(regions.get(j)) > 3000, names.get(i) + " and " + names.get(j) + " are only " + regions.get(i).gapXZ(regions.get(j)) + " blocks apart");
                else assertTrue(regions.get(i).gapXZ(regions.get(j)) >= SectorPlanner.GAP, "two floors of one chain keep the gap of two regions");
            }
        }
    }

    @Test
    public void everyPartyHasSeedsOfItsOwn() {
        Set<Long> seen = new HashSet<>();
        for (int party = 0; party < MegaGrid.MAX_PARTIES; party++) {
            for (Polarity road : Polarity.values()) {
                for (int idx = 0; idx < 6; idx++) {
                    long seed = SectorPlanner.floor(SEED, 1, road, idx, 28, false, party).seed();
                    assertTrue(seen.add(seed), "party " + party + " " + road.id() + " floor " + idx + " has a seed that another floor has");
                    assertEquals(seed, SectorDraft.of(SEED, 1, road, idx, 1, null, null, party).seed(), "the draft and the plan agree on the seed");
                }
            }
        }
        assertEquals(MegaGrid.MAX_PARTIES * 2 * 6, seen.size());
    }

    @Test
    public void aDraftCarriesItsPartyAndAPartysThemesAreItsOwn() {
        assertEquals(7, SectorDraft.of(SEED, 3, Polarity.HOLY, 4, 1, RegionTheme.FROST, null, 7).party());
        assertEquals(0, SectorDraft.of(SEED, 3, Polarity.HOLY, 4, 1, RegionTheme.FROST, null, -1).party());
        assertEquals(9, SectorDraft.first(SEED, 3, Polarity.ABYSSAL, 1, 9).party());
        assertEquals(0, SectorDraft.first(SEED, 3, Polarity.ABYSSAL, 1).party());
        // the first floor of the second decade draws a theme from the party's seed: across many parties it is not always the same one
        Set<RegionTheme> themes = new HashSet<>();
        for (int party = 0; party < 40; party++) themes.add(SectorDraft.of(SEED, 3, Polarity.HOLY, 10, 1, RegionTheme.GRAVITY, null, party).theme());
        assertTrue(themes.size() > 3, "forty parties drew only " + themes.size() + " different themes for their second decade");
        // and a party's draw is deterministic
        assertEquals(SectorDraft.of(SEED, 3, Polarity.HOLY, 10, 1, RegionTheme.GRAVITY, null, 11), SectorDraft.of(SEED, 3, Polarity.HOLY, 10, 1, RegionTheme.GRAVITY, null, 11));
        // the Crucible's choice still wins on the first floor of a decade, for every party
        for (int party = 0; party < 5; party++) assertEquals(RegionTheme.STORM, SectorDraft.of(SEED, 3, Polarity.HOLY, 10, 1, RegionTheme.GRAVITY, RegionTheme.STORM, party).theme());
    }

    @Test
    public void theFirstSectorOfOldModelRoadsLiveOnThePartysLaneToo() {
        for (int party : new int[] {0, 1, 5, 63}) {
            for (Polarity road : Polarity.values()) {
                SectorPlanner.Plan p = SectorPlanner.first(SEED, 1, road, party);
                assertEquals(new Vec(0, SectorPlanner.DOOR_Y, MegaGrid.laneZ(road, party)), p.entry());
                assertEquals(Facing.EAST, p.forward());
                assertEquals(party == 0, p.equals(SectorPlanner.first(SEED, 1, road)));
            }
        }
    }

    @Test
    public void aPartysObstaclesAreOnlyItsOwnSectors() {
        Box a = new Box(0, 56, -100, 170, 103, 100), b = new Box(5000, 56, -100, 5170, 103, 100), c = new Box(0, 56, 3996, 170, 103, 4196);
        List<WorldObstacles.Known> known = List.of(
                new WorldObstacles.Known(0, a, null, null, new Vec(0, 65, 0), Polarity.HOLY, 0),
                new WorldObstacles.Known(1, b, null, null, new Vec(5000, 65, 0), Polarity.HOLY, 0),
                new WorldObstacles.Known(2, c, null, null, new Vec(0, 65, 4096), Polarity.HOLY, 1));
        assertEquals(3, WorldObstacles.of(known, 10, -1, Polarity.HOLY).size(), "without a party every party counts (the way a world with one party asked)");
        assertEquals(3, WorldObstacles.of(known, 10, -1, Polarity.HOLY, -1).size());
        assertEquals(2, WorldObstacles.of(known, 10, -1, Polarity.HOLY, 0).size());
        assertEquals(1, WorldObstacles.of(known, 10, -1, Polarity.HOLY, 1).size());
        assertEquals(0, WorldObstacles.of(known, 10, -1, Polarity.HOLY, 2).size());
        assertEquals(0, WorldObstacles.of(known, 10, -1, Polarity.ABYSSAL, 0).size(), "another road is still no obstacle");
        // a sector made with the old constructors is a sector of party 0
        assertEquals(0, new WorldObstacles.Known(0, a, null, null, new Vec(0, 65, 0)).party());
        assertEquals(0, new WorldObstacles.Known(0, a, null, null, new Vec(0, 65, 0), Polarity.ABYSSAL).party());
    }
}
