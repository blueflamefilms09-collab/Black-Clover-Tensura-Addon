package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: this is the test of the heart of the Mega-Dungeon, run on the synthetic catalog (shapes that obey the same connector contract as the real
 * templates, see {@link SyntheticCatalog}) so it works before the real templates exist and keeps working after. It checks the algorithm of the design
 * document step by step with checks that do not use the audit: every piece lies inside the region and overlaps nothing, every join lines up
 * door to door with matching names, exclusions are respected, the same seed gives the same plan and another seed another one, the piece limit holds,
 * sealed doors are left alone, forced pools decide the vault, the recipe is met, loops are real doorways, door caps fill doors that face free space and
 * are absent where the space is forbidden, and a pool's empty element only ends the primary candidates and never leaves a doorway uncapped.
 */
class AssemblerTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    private static Result run(long seed) { return MegaAssembler.assemble(TestPlans.first(CAT, seed)); }

    @Test
    void everyPlanIsStructurallySound() {
        for (long s = 0; s < 25; s++) {
            Request q = TestPlans.first(CAT, s);
            Result r = MegaAssembler.assemble(q);
            if (!r.assembled()) continue;                                  // a try may fail; the solver retries (tested elsewhere)
            assertEquals(List.of(), TestPlans.brute(r, q), "seed " + s);
            Placed land = r.landingPiece();
            assertEquals(RoomKind.LANDING, land.shape.kind);
            assertEquals(q.start.frontCell(), land.socketCell(land.ownSocket), "the Landing's door stands on the start cell");
            assertEquals(q.start.growth().opposite(), land.socketFacing(land.ownSocket), "and points back against the growth");
            assertEquals(0, land.depth);
        }
    }

    @Test
    void mostSeedsAssembleAtTheFirstTry() {
        int ok = 0;
        for (long s = 0; s < 30; s++) if (run(s).assembled()) ok++;
        assertTrue(ok >= 27, "the chain should usually be found: " + ok + " of 30");
    }

    @Test
    void theBossChainIsAnArenaAnAntechamberAVaultAndAWayOnward() {
        for (long s = 0; s < 15; s++) {
            Request q = TestPlans.first(CAT, s);
            Result r = MegaAssembler.assemble(q);
            if (!r.assembled()) continue;
            assertEquals(1, r.count(RoomKind.ARENA));
            Placed arena = null, ante = null, vault = null, onward = null;
            for (Placed p : r.pieces) {
                if (p.shape.kind == RoomKind.ARENA) arena = p;
            }
            for (Placed p : r.pieces) {
                if (p.parent == arena.index && p.shape.kind == RoomKind.ANTECHAMBER) ante = p;
            }
            assertNotNull(ante, "the arena's chain door leads to the antechamber, seed " + s);
            assertEquals(MegaGrid.CHAIN, arena.shape.sockets.get(ante.parentSocket).name());
            for (Placed p : r.pieces) {
                if (p.parent == ante.index && p.shape.kind == RoomKind.VAULT) vault = p;
                if (p.parent == ante.index && p.shape.kind == RoomKind.ONWARD) onward = p;
            }
            assertNotNull(vault);
            assertNotNull(onward);
            assertEquals(MegaGrid.VAULT, ante.shape.sockets.get(vault.parentSocket).name());
            assertEquals(MegaGrid.ONWARD, ante.shape.sockets.get(onward.parentSocket).name());
            assertEquals(q.relicCount, vault.shape.countMarkers(MarkerKind.PEDESTAL), "the vault offers one relic per pedestal");
            Result.Expansion e = r.expansion;
            assertNotNull(e);
            assertEquals(onward.index, e.piece());
            assertEquals(onward.socketCell(onward.shape.expansionSocket), e.seal());
            assertEquals(onward.socketFacing(onward.shape.expansionSocket), e.facing());
            assertEquals(9, e.facing().alongX() ? e.strip().sizeZ() : e.strip().sizeX(), "the strip is one module wide");
            for (Placed p : r.pieces) {
                if (p.index == onward.index) continue;
                assertFalse(p.box.overlaps(p.shape.isCap() ? e.inner() : e.strip()), "seed " + s + ": " + p + " stands in the keep-clear strip");
            }
            assertTrue(e.strip().contains(e.seal().step(e.facing())), "the strip starts at the cell in front of the seal");
        }
    }

    @Test
    void theRecipeIsMet() {
        Recipe rc = new Recipe();
        for (long s = 0; s < 15; s++) {
            Request q = TestPlans.first(CAT, s);
            SectorSolver.Solved sv = SectorSolver.solve(q, 24);
            assertTrue(sv.ok(), "seed " + s);
            assertFalse(sv.fallback(), "seed " + s + " should not need the fallback layout");
            Result r = sv.result();
            assertEquals(1, r.count(RoomKind.ARENA));
            assertTrue(r.count(RoomKind.TRIAL) >= rc.min(RoomKind.TRIAL), "trial rooms");
            assertTrue(r.count(RoomKind.TREASURE) >= rc.min(RoomKind.TREASURE), "treasure closets");
            assertTrue(r.count(RoomKind.SET_PIECE) <= 1, "at most one set piece");
            int rooms = 0;
            for (Placed p : r.pieces) if (Result.isRoom(p) && !p.link) rooms++;
            assertTrue(rooms >= rc.minRooms, "rooms: " + rooms);
            assertTrue(r.loops.size() >= rc.minLoops, "loops: " + r.loops.size());
            double safe = 0;
            Placed land = r.landingPiece();
            for (Placed p : r.pieces) {
                if (p.shape.kind != RoomKind.SAFE) continue;
                double d = Math.hypot(p.box.centerX() - land.box.centerX(), p.box.centerZ() - land.box.centerZ()) / 9.0;
                if (d >= rc.safeMinDistance && d <= rc.safeMaxDistance) safe++;
            }
            assertTrue(safe >= rc.extraSafe, "an extra safe room 5..12 modules from the Landing");
            Placed arena = r.pieces.stream().filter(p -> p.shape.kind == RoomKind.ARENA).findFirst().orElseThrow();
            Vec front = r.pieces.get(arena.parent).socketFront(arena.parentSocket);
            assertTrue(Math.hypot(front.x() - land.box.centerX(), front.z() - land.box.centerZ()) / 9.0 >= rc.arenaMinDistance, "the arena is far from the Landing");
            assertTrue(arena.depth >= rc.arenaMinDepth, "and deep in the tree: " + arena.depth);
        }
    }

    @Test
    void theSameSeedGivesTheSamePlanAndAnotherSeedAnotherOne() {
        Request q = TestPlans.first(CAT, 4);
        Result a = MegaAssembler.assemble(q), b = MegaAssembler.assemble(q);
        assertEquals(TestPlans.fingerprint(a), TestPlans.fingerprint(b));
        assertEquals(a.loops, b.loops);
        assertEquals(a.expansion, b.expansion);
        Result other = MegaAssembler.assemble(q.withSeed(q.seed + 1, false));
        assertNotEquals(TestPlans.fingerprint(a), TestPlans.fingerprint(other));
        // a rebuilt request (what a restart does) gives the same plan too
        assertEquals(TestPlans.fingerprint(a), TestPlans.fingerprint(MegaAssembler.assemble(TestPlans.first(SyntheticCatalog.standard(), 4))));
    }

    @Test
    void thePieceLimitHoldsCapsIncluded() {
        for (int limit : new int[] {170, 200, 260}) {
            for (long s = 0; s < 8; s++) {
                Request q = TestPlans.first(CAT, s, new Recipe(), limit);
                Result r = MegaAssembler.assemble(q);
                if (!r.assembled()) continue;
                assertTrue(r.pieces.size() <= limit, "limit " + limit + " seed " + s + " gave " + r.pieces.size());
            }
        }
    }

    @Test
    void sealedDoorsGetNoChildAndNoCap() {
        for (long s = 0; s < 10; s++) {
            Result r = run(s);
            if (!r.assembled()) continue;
            for (Placed child : r.pieces) {
                if (child.parent < 0) continue;
                Socket via = r.pieces.get(child.parent).shape.sockets.get(child.parentSocket);
                assertFalse(via.sealed(), "a sealed door has a child: " + child + " on " + r.pieces.get(child.parent));
            }
            // nothing stands on the front cell of a sealed door (its seal faces the strip, not a cap)
            for (Placed p : r.pieces) {
                for (int i = 0; i < p.shape.sockets.size(); i++) {
                    if (!p.shape.sockets.get(i).sealed() || i == p.ownSocket) continue;
                    Vec f = p.socketFront(i);
                    for (Placed other : r.pieces) {
                        if (other.index != p.index && other.shape.isCap()) assertFalse(other.box.contains(f), "a cap on the sealed door of " + p);
                    }
                }
            }
        }
    }

    @Test
    void theForcedPoolDecidesTheVaultForEveryRelicCount() {
        for (int relics = 1; relics <= 3; relics++) {
            int found = 0;
            for (long s = 0; s < 6; s++) {
                Request q = TestPlans.first(CAT, s);
                q.relicCount = relics;
                q.forced = Map.of(MegaGrid.VAULT, MegaGrid.P_VAULT + "_" + relics);
                Result r = MegaAssembler.assemble(q);
                if (!r.assembled()) continue;
                found++;
                for (Placed p : r.pieces) if (p.shape.kind == RoomKind.VAULT) assertEquals(relics, p.shape.countMarkers(MarkerKind.PEDESTAL));
            }
            assertTrue(found >= 4, "relic count " + relics + ": " + found + " of 6 assembled");
        }
    }

    @Test
    void aVaultWithTheWrongPedestalCountIsNeverAccepted() {
        // no forced pool: the generic vault pool holds one-, two- and three-pedestal vaults and only the matching one may be chosen
        for (long s = 0; s < 8; s++) {
            Request q = TestPlans.first(CAT, s);
            q.forced = Map.of();
            q.relicCount = 2;
            Result r = MegaAssembler.assemble(q);
            if (!r.assembled()) continue;
            for (Placed p : r.pieces) if (p.shape.kind == RoomKind.VAULT) assertEquals(2, p.shape.countMarkers(MarkerKind.PEDESTAL));
        }
    }

    @Test
    void loopsAreRealDoorwaysBetweenTouchingRooms() {
        int total = 0;
        for (long s = 0; s < 12; s++) {
            Result r = run(s);
            if (!r.assembled()) continue;
            for (Result.Loop l : r.loops) {
                total++;
                Placed a = r.pieces.get(l.pieceA()), b = r.pieces.get(l.pieceB());
                assertNotEquals(a.index, b.index);
                assertEquals(l.cell(), a.socketCell(l.socketA()));
                assertEquals(b.socketCell(l.socketB()), a.socketFront(l.socketA()), "the doors stand on each other's front cells");
                assertEquals(a.socketCell(l.socketA()), b.socketFront(l.socketB()));
                assertEquals(a.socketFacing(l.socketA()), b.socketFacing(l.socketB()).opposite());
                assertTrue(a.box.inflateXZ(1).overlaps(b.box), "the rooms touch");
                assertFalse(a.box.overlaps(b.box));
                assertTrue(a.shape.sockets.get(l.socketA()).target().equals(b.shape.sockets.get(l.socketB()).name()));
                assertFalse(a.shape.isCap() || b.shape.isCap());
            }
        }
        assertTrue(total > 20, "the dense synthetic map has plenty of loops: " + total);
    }

    @Test
    void doorCapsFillDoorsThatFaceFreeSpaceAndThereAreNoOpenDoorways() {
        for (long s = 0; s < 10; s++) {
            Request q = TestPlans.first(CAT, s);
            SectorSolver.Solved sv = SectorSolver.solve(q, 24);
            assertTrue(sv.ok());
            Result r = sv.result();
            assertTrue(r.count(RoomKind.CAP) > 10, "caps are present: " + r.count(RoomKind.CAP));
            // every unused, unsealed door either faces another piece (a niche backed by a wall, or a loop) or is a loop door: none faces the void
            for (Placed p : r.pieces) {
                if (p.shape.isCap()) continue;
                for (int i = 0; i < p.shape.sockets.size(); i++) {
                    if (p.isUsed(i) || p.shape.sockets.get(i).sealed()) continue;
                    Vec f = p.socketFront(i);
                    boolean covered = false;
                    for (Placed o : r.pieces) if (o.box.contains(f)) covered = true;
                    assertTrue(covered, "seed " + s + ": the door " + i + " of " + p + " opens onto the void at " + f);
                }
            }
            for (Placed c : r.pieces) {
                if (!c.shape.isCap()) continue;
                assertTrue(c.box.sizeX() == 3 || c.box.sizeZ() == 3, "a cap is 3 wide");
                assertEquals(6, c.box.sizeY());
                assertEquals(MegaGrid.FLOOR_Y, c.box.minY(), "a cap starts at the floor row");
            }
        }
    }

    @Test
    void aCapIsAbsentWhereTheSpaceIsForbidden() {
        Request q = TestPlans.first(CAT, 3);
        Result open = MegaAssembler.assemble(q);
        assertTrue(open.assembled());
        Placed land = open.landingPiece();
        // forbid the strip behind the Landing's entry face: every door of the Landing facing out through it must stay without a cap
        Box behind = new Box(-8, MegaGrid.ORIGIN_Y, -90, -1, MegaGrid.ORIGIN_Y + 47, 90);
        q.exclusions = List.of(behind);
        Result closed = MegaAssembler.assemble(q);
        assertTrue(closed.assembled());
        for (Placed p : closed.pieces) assertFalse(p.box.overlaps(behind), "nothing may stand in the forbidden box: " + p);
        boolean capBehind = false;
        for (Placed p : open.pieces) if (p.shape.isCap() && p.box.overlaps(behind)) capBehind = true;
        assertTrue(capBehind, "without the exclusion the Landing's back door has a cap");
        List<String> audit = SectorAudit.audit(closed, q, true);
        boolean flagged = false;
        for (String a : audit) if (a.contains("open onto the void")) flagged = true;
        assertTrue(flagged, "the strict audit reports the uncapped doorway: " + audit);
        assertEquals(RoomKind.LANDING, land.shape.kind);
    }

    @Test
    void exclusionsKeepEverythingOutOfTheirBox() {
        Request q = TestPlans.first(CAT, 6);
        Box hole = new Box(60, MegaGrid.ORIGIN_Y, -30, 100, MegaGrid.ORIGIN_Y + 47, 30);
        q.exclusions = List.of(hole);
        Result r = MegaAssembler.assemble(q);
        assertTrue(r.assembled());
        for (Placed p : r.pieces) assertFalse(p.box.overlaps(hole), p + " is inside the excluded box");
        assertEquals(List.of(), TestPlans.brute(r, q));
    }

    @Test
    void aPoolsEmptyElementOnlyEndsThePrimaryCandidatesNotTheCaps() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        // the side pool is now almost always "empty": hardly any room is ever drawn, but every doorway must still be capped
        c.pool(MegaGrid.P_SIDE, 100000, MegaGrid.P_CAP, "nusmp:mega/hall/cell_1x1", 1);
        Request q = TestPlans.first(c, 5);
        q.fallback = true;                                                    // only the Landing and the chain are needed here
        Result r = MegaAssembler.assemble(q);
        assertTrue(r.assembled(), r.failure);
        int rooms = 0;
        for (Placed p : r.pieces) if (!p.shape.isCap()) rooms++;
        assertTrue(rooms < 12, "almost nothing was drawn: " + rooms);
        assertTrue(r.count(RoomKind.CAP) > 3, "yet the doors got caps");
        assertEquals(List.of(), SectorAudit.audit(r, q, false));
        boolean open = SectorAudit.audit(r, q, true).stream().anyMatch(a -> a.contains("open onto the void"));
        assertFalse(open, "no doorway opens onto the void");
    }

    @Test
    void aRegionTooSmallForTheLandingFailsCleanly() {
        Request q = TestPlans.first(CAT, 1);
        q.region = new Box(0, MegaGrid.ORIGIN_Y, 0, 20, MegaGrid.ORIGIN_Y + 47, 20);
        Result r = MegaAssembler.assemble(q);
        assertFalse(r.assembled());
        assertNotNull(r.failure);
        assertTrue(r.failure.contains("Landing"), r.failure);
        assertTrue(r.pieces.isEmpty());
    }

    @Test
    void aMissingArenaPoolFailsWithAWarningNotAnException() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        c.pools().remove(MegaGrid.P_ARENA);
        Result r = MegaAssembler.assemble(TestPlans.first(c, 2));
        assertFalse(r.assembled());
        assertTrue(r.warnings.stream().anyMatch(w -> w.contains(MegaGrid.P_ARENA)), r.warnings.toString());
        assertTrue(r.failure.startsWith("no boss chain"), r.failure);
    }

    @Test
    void aPoolNamingAMissingTemplateIsAWarningAndTheRestStillWorks() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        c.pool(MegaGrid.P_SIDE, 2, MegaGrid.P_CAP, "nusmp:mega/hall/ghost", 50, "nusmp:mega/hall/hall_2x2", 5, "nusmp:mega/corridor/straight_1x3", 5, "nusmp:mega/hall/cell_1x1", 5,
                "nusmp:mega/trial/archway_3x3", 3, "nusmp:mega/treasure/closet_1x1", 5, "nusmp:mega/safe/hearth_2x2", 2);
        Request q = TestPlans.first(c, 8);
        Result r = MegaAssembler.assemble(q);
        assertTrue(r.warnings.stream().anyMatch(w -> w.contains("nusmp:mega/hall/ghost")), r.warnings.toString());
        if (r.assembled()) assertEquals(List.of(), TestPlans.brute(r, q));
    }

    @Test
    void aTinyStepBudgetStopsTheChainSearchAndSaysSo() {
        Recipe rc = new Recipe();
        rc.stepBudget = 1000;
        Request q = TestPlans.first(CAT, 3, rc, 450);
        Result r = MegaAssembler.assemble(q);
        // the budget is checked only while the chain is searched; a sector may still succeed in under 1000 steps, but if it fails it must say why
        if (!r.assembled()) assertTrue(r.failure.contains("step budget"), r.failure);
    }

    @Test
    void theTunnelToTheLandingIsPartOfThePlanAndTheLandingHangsFromIt() {
        SimWorld w = new SimWorld(CAT, 77, new Recipe(), 450, 40, 24);
        assertTrue(w.first());
        assertTrue(w.next(), w.stopped);
        SimWorld.Entry e = w.sectors.get(1);
        Result r = e.result();
        Request q = e.prepared().request();
        assertEquals(List.of(), TestPlans.brute(r, q));
        int tunnel = 0;
        for (Placed p : r.pieces) if (p.link) tunnel++;
        assertEquals(e.plan().linkModules(), tunnel, "one tunnel piece per segment");
        assertTrue(tunnel >= 2, "the bridge crosses the gap");
        Placed land = r.landingPiece();
        assertEquals(tunnel, land.index, "the Landing comes right after the tunnel");
        assertEquals(tunnel - 1, land.parent);
        for (int i = 0; i < tunnel; i++) assertTrue(r.pieces.get(i).link);
        Result.Expansion prev = w.sectors.get(0).result().expansion;
        Placed first = r.pieces.get(0);
        assertEquals(prev.seal().step(prev.facing()), first.socketCell(first.ownSocket), "the first segment starts on the cell in front of the old seal");
        assertEquals(prev.facing().opposite(), first.socketFacing(first.ownSocket));
        // the tunnel is straight: every segment starts on the cell in front of the previous one's far door and advances one module along the facing
        Facing f = prev.facing();
        for (int i = 1; i < tunnel; i++) {
            Placed a = r.pieces.get(i - 1), b = r.pieces.get(i);
            assertEquals(i - 1, b.parent);
            assertEquals(a.socketFront(b.parentSocket), b.socketCell(b.ownSocket), "segment " + i + " starts in front of segment " + (i - 1));
            assertEquals(a.box.far(f) + 1, b.box.near(f), "segments follow each other with no gap and no overlap");
            assertEquals(a.socketCell(b.parentSocket).along(f.cw()), b.socketCell(b.ownSocket).along(f.cw()), "and stay on the same line sideways");
            assertEquals(f.opposite(), b.socketFacing(b.ownSocket));
        }
        assertEquals(prev.seal().along(f.cw()), r.pieces.get(tunnel - 1).socketCell(land.parentSocket).along(f.cw()), "the run is a straight line from the seal to the Landing door");
        assertEquals(prev.seal().along(f.cw()), land.socketCell(land.ownSocket).along(f.cw()));
        assertEquals(List.of(), SectorAudit.audit(r, q, true));
    }

    @Test
    void theRoomGraphConnectsEveryRoomAndHasEdgesForJoinsAndLoops() {
        Result r = run(9);
        assertTrue(r.assembled());
        int[] ids = r.roomIds();
        int rooms = 0;
        for (int id : ids) if (id >= 0) rooms++;
        List<int[]> edges = r.edges();
        assertTrue(edges.size() >= rooms - 1 + r.loops.size(), "a spanning tree plus the loops: " + edges.size());
        for (int[] e : edges) assertTrue(e[0] >= 0 && e[1] >= 0 && e[0] < rooms && e[1] < rooms);
        List<String> audit = SectorAudit.audit(r, TestPlans.first(CAT, 9), false);
        assertFalse(audit.stream().anyMatch(a -> a.contains("cannot be reached")), audit.toString());
    }

    @Test
    void theRoomListTheIdsAndTheBoundsAreConsistent() {
        Result r = run(11);
        assertTrue(r.assembled());
        assertEquals(r.pieces.size(), r.rooms().size() + r.count(RoomKind.CAP));
        int[] ids = r.roomIds();
        int next = 0;
        for (Placed p : r.pieces) {
            if (p.shape.isCap()) assertEquals(-1, ids[p.index], "a cap is not a room");
            else assertEquals(next++, ids[p.index], "room ids count up in placement order");
        }
        assertEquals(next, r.rooms().size());
        Box b = r.bounds();
        for (Placed p : r.pieces) {
            assertTrue(b.contains(new Vec(p.box.minX(), p.box.minY(), p.box.minZ())), "bounds hold the min corner of " + p);
            assertTrue(b.contains(new Vec(p.box.maxX(), p.box.maxY(), p.box.maxZ())), "bounds hold the max corner of " + p);
        }
        Box tight = null;
        for (Placed p : r.pieces) tight = tight == null ? p.box : tight.union(p.box);
        assertEquals(tight, b, "and are exactly their union");
    }
}
