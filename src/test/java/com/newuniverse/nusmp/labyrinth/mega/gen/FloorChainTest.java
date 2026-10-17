package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a single floor can be right while the dungeon is wrong: floors that touch, a boss on the wrong floor, a theme that changes in the middle of a decade, a floor
 * whose plan depends on the floors around it (so a regeneration after a crash would not be the floor that was placed). This grows a whole simulated chain of 25 floors
 * on each road the way the game does (draft, plan, solve, one floor after the other, the theme of the floor before feeding the next draft) and checks the properties
 * the owner's model rests on: a boss only on floors 10 and 20, one theme per decade, the road's own look at the start, floors in their slots with a clear gap, the
 * threat equal to the floor, and a floor planned alone equal to the same floor planned inside the chain.
 */
class FloorChainTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.floors();
    private static final long SEED = 20261017L;
    private static final int FLOORS = 25;

    private record Floor(int chain, SectorDraft draft, Request request, SectorSolver.Solved solved) {
        Result result() { return solved.result(); }
    }

    private static List<Floor> grow(Polarity road) {
        List<Floor> out = new ArrayList<>();
        RegionTheme previous = null;
        for (int k = 0; k < FLOORS; k++) {
            SectorDraft draft = SectorDraft.of(SEED, k * 2 + (road == Polarity.ABYSSAL ? 1 : 0), road, k, 1, previous, null);
            Request q = TestPlans.floor(CAT, SEED, road, k, 28, Recipe.forFloors(), TemplateThemes.none(), previous);
            q.themeId = draft.theme().id();
            out.add(new Floor(k, draft, q, SectorSolver.solve(q, 24)));
            previous = draft.theme();
        }
        return out;
    }

    @Test
    void twentyFiveFloorsOfEachRoadAreBuiltAndAuditedWithBossesOnlyEveryTenth() {
        for (Polarity road : Polarity.values()) {
            List<Floor> chain = grow(road);
            for (Floor f : chain) {
                int number = FloorMap.floorOfSector(road, f.chain());
                assertTrue(f.solved().ok(), "floor " + number + " is built: " + SectorSolver.summary(f.solved().failures(), 3));
                assertFalse(f.solved().fallback(), "floor " + number + " needed the fallback layout");
                boolean boss = FloorMap.isBossFloor(number);
                assertEquals(boss, f.request().floor.boss(), "the boss flag of floor " + number);
                assertEquals(boss ? 1 : 0, f.result().count(RoomKind.ARENA), "a Guardian's arena exactly on boss floors, floor " + number);
                assertEquals(boss ? 1 : 0, f.result().count(RoomKind.VAULT), "and a vault");
                assertEquals(2, f.result().count(RoomKind.STAIRWELL), "two stairwells on floor " + number);
                assertEquals(Math.abs(number), f.draft().threat(), "the threat is the floor number");
                assertEquals(List.of(), TestPlans.brute(f.result(), f.request()), "a sound plan, floor " + number);
            }
            assertEquals(2, chain.stream().filter(f -> f.request().floor.boss()).count(), "bosses on floors 10 and 20 of 25");
        }
    }

    @Test
    void aDecadeOfFloorsSharesOneThemeAndTheRoadStartsInItsOwnLook() {
        for (Polarity road : Polarity.values()) {
            List<Floor> chain = grow(road);
            assertEquals(road == Polarity.HOLY ? RegionTheme.RADIANT : RegionTheme.ABYSS, chain.get(0).draft().theme(), "the first decade wears the road's signature");
            for (int d = 0; d < 3; d++) {
                RegionTheme t = chain.get(d * 10).draft().theme();
                for (int k = d * 10; k < Math.min(FLOORS, d * 10 + 10); k++) assertEquals(t, chain.get(k).draft().theme(), "floor " + (k + 1) + " wears its decade's theme");
                if (d > 0) assertTrue(t != chain.get(d * 10 - 1).draft().theme(), "a new decade is a new look");
            }
        }
    }

    @Test
    void theRelicOfTheCruciblePicksTheNextDecadesThemeAndNothingElse() {
        // an override is consulted on the first floor of a decade only; on every other floor it is ignored
        RegionTheme prev = RegionTheme.RADIANT;
        for (int k = 1; k < 30; k++) {
            SectorDraft d = SectorDraft.of(SEED, k, Polarity.HOLY, k, 1, prev, RegionTheme.FROST);
            if (k % 10 == 0) assertEquals(RegionTheme.FROST, d.theme(), "floor " + (k + 1) + " takes the chosen theme");
            else assertEquals(prev, d.theme(), "floor " + (k + 1) + " keeps the decade's theme");
            prev = d.theme();
        }
        // the old model (ten floors per sector) asks the override every sector, as in 1.55.0
        assertEquals(RegionTheme.FROST, SectorDraft.of(SEED, 3, Polarity.HOLY, 3, 10, RegionTheme.RADIANT, RegionTheme.FROST).theme());
    }

    @Test
    void floorsStandInTheirSlotsWithAClearGapAndTheRoadsNeverMeet() {
        List<Floor> holy = grow(Polarity.HOLY), abyss = grow(Polarity.ABYSSAL);
        for (List<Floor> chain : List.of(holy, abyss)) {
            for (int i = 0; i < chain.size(); i++) {
                Box ri = chain.get(i).request().region;
                for (int j = i + 1; j < chain.size(); j++) {
                    long gap = ri.gapXZ(chain.get(j).request().region);
                    assertTrue(gap >= SectorPlanner.GAP, "floors " + (i + 1) + " and " + (j + 1) + " are " + gap + " blocks apart");
                }
            }
        }
        for (Floor a : holy) for (Floor b : abyss) assertTrue(a.request().region.gapXZ(b.request().region) > 100_000, "the roads stay far apart");
        assertEquals(0, holy.get(0).request().start.frontCell().x(), "floor 1 of the Holy road stands where 1.54.0 put its sector 0");
        assertEquals(FloorPlan.PITCH, holy.get(1).request().start.frontCell().x() - holy.get(0).request().start.frontCell().x());
    }

    @Test
    void aFloorPlannedAloneIsTheFloorPlannedInsideTheChain() {
        for (Polarity road : Polarity.values()) {
            List<Floor> chain = grow(road);
            for (int k : new int[] {0, 7, 9, 12, 19, 24}) {
                RegionTheme prev = k == 0 ? null : chain.get(k - 1).draft().theme();
                Request alone = TestPlans.floor(CAT, SEED, road, k, 28, Recipe.forFloors(), TemplateThemes.none(), prev);
                alone.themeId = chain.get(k).draft().theme().id();
                SectorSolver.Solved s = SectorSolver.solve(alone, 24);
                assertEquals(TestPlans.fingerprint(chain.get(k).result()), TestPlans.fingerprint(s.result()), "floor " + (k + 1) + " does not depend on any other floor");
            }
        }
    }

    @Test
    void theStairsOfEveryFloorLeadToTheFloorsAroundIt() {
        for (int k = 0; k < FLOORS; k++) {
            StairLinks.Dest back = StairLinks.back(k), on = StairLinks.forward(k);
            if (k == 0) assertTrue(back.hub(), "the first floor's back stair leads to Floor 0");
            else {
                assertFalse(back.hub());
                assertEquals(k - 1, back.chainIndex());
                assertTrue(back.arrivesAtForward(), "you come back to the floor before by its forward stairs");
            }
            assertEquals(k + 1, on.chainIndex());
            assertFalse(on.arrivesAtForward(), "you go on to the arrival stairs of the next floor");
        }
    }
}
