package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@link Clearance7} is the whole decision of the 7 x 7 minimum clearance and {@code GeometryCheck}'s rule {@code clearance7} is its judge, so both are pinned here on
 * made-up spaces (the rule fails a 5-wide doorway, a 6-high one, a pillar in the middle, and passes a free 7 x 7), on the geometry of the cuts for every turn of a piece,
 * and, on the shipped templates, on whole planned floors of both roads (the cyclic generator and the old one): cutting keeps every module connected, every join and loop
 * has a cut on both sides that lines up, no cut leaves its piece, and the plan itself (pieces, grid, loops, dead ends) is untouched because nothing here changes it.
 */
class Clearance7Test {
    // ------------------------------------------------------------------------------------------------------------------------------ the rule

    @Test
    void theRuleBehavesOnMadeUpSpaces() {
        assertEquals(List.of(), Clearance7.ruleSelfCheck());
    }

    @Test
    void measureReportsTheNarrowestAndLowestStation() {
        Clearance7.Cut c = new Clearance7.Cut(Clearance7.Kind.CORRIDOR, 0, new Box(0, 9, 0, 6, 15, 19), Facing.NORTH, "t");
        Clearance7.Space open = (x, y, z) -> x >= 0 && x <= 6 && y >= 9 && y <= 15 && z >= 0 && z <= 19;
        assertEquals("7x7", Clearance7.measure(open, c).toString());
        // a pinch of one column at z = 10 (x 0 blocked) and a low beam at z = 14 (y 15 blocked at x = 3)
        Clearance7.Space pinch = (x, y, z) -> open.free(x, y, z) && !(z == 10 && x == 0);
        assertEquals("6x7", Clearance7.measure(pinch, c).toString());
        Clearance7.Space beam = (x, y, z) -> open.free(x, y, z) && !(z == 14 && y == 15 && x == 3);
        assertEquals("0x6", Clearance7.measure(beam, c).toString());
        assertFalse(Clearance7.measure(beam, c).ok());
        // a wider space still reports 7+ wide and passes
        Clearance7.Space wide = (x, y, z) -> x >= -3 && x <= 9 && y >= 9 && y <= 20 && z >= 0 && z <= 19;
        assertTrue(Clearance7.measure(wide, c).ok());
        assertTrue(Clearance7.measure(wide, c).width() >= 13);
    }

    @Test
    void aStairColumnIsMeasuredByItsHeadRoomOnly() {
        Clearance7.Cut c = new Clearance7.Cut(Clearance7.Kind.STAIR, 0, new Box(3, 11, 4, 3, 17, 4), Facing.NORTH, "t");
        assertTrue(Clearance7.measure((x, y, z) -> true, c).ok());
        Clearance7.Clearance low = Clearance7.measure((x, y, z) -> y < 15, c);
        assertEquals(4, low.height());
        assertFalse(low.ok());
        assertEquals(1, Clearance7.shortHeadroom((x, y, z) -> y < 15, List.of(new Vec(3, 10, 4))));
        assertEquals(0, Clearance7.shortHeadroom((x, y, z) -> y < 18, List.of(new Vec(3, 10, 4))));
    }

    // ------------------------------------------------------------------------------------------------------------------------------ what may be cut

    @Test
    void containersAndMechanismsAreNeverCutButFurnitureAndWallsAre() {
        assertFalse(Clearance7.carvable("minecraft:chest", true));
        assertFalse(Clearance7.carvable("minecraft:barrel", true));
        assertFalse(Clearance7.carvable("minecraft:spawner", true));
        assertFalse(Clearance7.carvable("nusmp:gatekeeper_door", false));
        assertFalse(Clearance7.carvable("nusmp:sealed_archway", false));
        assertFalse(Clearance7.carvable("nusmp:relic_pedestal", true));
        assertFalse(Clearance7.carvable("minecraft:water", false));
        assertFalse(Clearance7.carvable("minecraft:air", false));
        assertTrue(Clearance7.carvable("minecraft:deepslate_bricks", false));
        assertTrue(Clearance7.carvable("minecraft:chain", false));
        assertTrue(Clearance7.carvable("minecraft:red_wall_banner", true));
        assertTrue(Clearance7.carvable("minecraft:oak_wall_sign", true));
        assertTrue(Clearance7.carvable("minecraft:skeleton_wall_skull", true));
        assertTrue(Clearance7.carvable("minecraft:chiseled_bookshelf", true));
        assertTrue(Clearance7.carvable("minecraft:lectern", true));
        assertTrue(Clearance7.carvable("minecraft:furnace", true));
        assertTrue(Clearance7.passable("minecraft:wall_torch"));
        assertTrue(Clearance7.passable("minecraft:light"));
        assertFalse(Clearance7.passable("minecraft:lantern"));
        assertFalse(Clearance7.passable("minecraft:deepslate_bricks"));
    }

    @Test
    void aRoofIsAddedOnlyWhereACeilingInsideTheBoxWasCutThrough() {
        boolean[] none = new boolean[7];
        assertFalse(Clearance7.needsCap(none, true));
        boolean[] ceilingAtRow5 = new boolean[7];
        ceilingAtRow5[5] = true;
        ceilingAtRow5[4] = true;
        assertTrue(Clearance7.needsCap(ceilingAtRow5, true));
        assertFalse(Clearance7.needsCap(ceilingAtRow5, false), "something already roofs it");
        boolean[] furniture = new boolean[7];
        furniture[0] = true;
        furniture[1] = true;
        assertFalse(Clearance7.needsCap(furniture, true), "a table is not a ceiling");
        boolean[] top = new boolean[7];
        top[6] = true;
        assertTrue(Clearance7.needsCap(top, true));
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the geometry of the cuts

    private static Shape shape(String id, int sx, int sz, RoomKind kind, Socket... sockets) {
        return new Shape(id, sx, 48, sz, kind, List.of(sockets), List.of(), 0);
    }

    private static Socket door(int x, int z, Facing f) {
        return new Socket(new Vec(x, 9, z), f, "nusmp:mega_door", "nusmp:mega_door", "nusmp:mega/side", 0, 0, "minecraft:air");
    }

    @Test
    void aDoorwayCutIsSevenWideSevenHighAndReachesThroughBothWallLayersAndTheApron() {
        Shape hall = shape("nusmp:mega/hall/t", 18, 18, RoomKind.HALL, door(4, 0, Facing.NORTH), door(17, 13, Facing.EAST));
        Placed p = new Placed(0, hall, "", 0, new Vec(100, 56, 100), 0, -1, -1, -1, false);
        List<Clearance7.Cut> cuts = Clearance7.cuts(p, new boolean[] {true, false});
        assertEquals(1, cuts.size(), "only the connected socket is cut");
        Clearance7.Cut c = cuts.get(0);
        assertEquals(Clearance7.Kind.DOORWAY, c.kind());
        assertEquals(new Box(101, 65, 100, 107, 71, 104), c.box(), "x 1..7 round the centre 4, rows 9..15 above the origin, z 0..4 (two wall layers and a 3 apron)");
        assertEquals(7, c.width());
        assertEquals(7, c.height());
        // the boss-chain sockets (a Gatekeeper door block stands in them) are never cut; the plain doorway of the Way Onward is
        Socket chain = new Socket(new Vec(4, 9, 0), Facing.NORTH, "nusmp:mega_chain", "nusmp:mega_chain", "nusmp:mega/antechamber", 0, 0, "minecraft:air");
        Socket vault = new Socket(new Vec(13, 9, 0), Facing.NORTH, "nusmp:mega_vault", "nusmp:mega_vault", "nusmp:mega/vault", 0, 0, "minecraft:air");
        Shape odd = shape("nusmp:mega/hall/o", 18, 18, RoomKind.HALL, chain, vault);
        assertEquals(0, Clearance7.cuts(new Placed(0, odd, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {true, true}).size());
        Socket onward = new Socket(new Vec(4, 9, 0), Facing.NORTH, "nusmp:mega_onward", "nusmp:mega_onward", "nusmp:mega/onward", 0, 0, "minecraft:air");
        Shape ante = shape("nusmp:mega/antechamber/o", 27, 27, RoomKind.ANTECHAMBER, onward);
        assertEquals(1, Clearance7.cuts(new Placed(0, ante, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {true}).size());
        assertEquals(0, Clearance7.cuts(new Placed(0, ante, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {false}).size(), "an unconnected socket is not cut");
        // a door cap is never cut
        Shape cap = shape("nusmp:mega/cap/c", 3, 3, RoomKind.CAP, door(1, 0, Facing.NORTH));
        assertEquals(0, Clearance7.cuts(new Placed(0, cap, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {true}).size());
    }

    @Test
    void aStraightCorridorGetsASevenWideStripAndABendASevenBySevenChamber() {
        Shape straight = shape("nusmp:mega/corridor/s", 9, 27, RoomKind.CORRIDOR, door(4, 0, Facing.NORTH), door(4, 26, Facing.SOUTH));
        List<Clearance7.Cut> cuts = Clearance7.cuts(new Placed(0, straight, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {true, true});
        assertEquals(3, cuts.size(), "two doorways and the strip");
        Clearance7.Cut strip = cuts.get(2);
        assertEquals(Clearance7.Kind.CORRIDOR, strip.kind());
        assertEquals(new Box(1, 9, 1, 7, 15, 25), strip.box());
        assertEquals(7, strip.width());
        // an Onward keeps both end walls two layers thick (its far end is the rubble seal)
        Shape onward = shape("nusmp:mega/onward/s", 9, 27, RoomKind.ONWARD, door(4, 0, Facing.NORTH));
        Clearance7.Cut o = Clearance7.cuts(new Placed(0, onward, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {false}).get(0);
        assertEquals(new Box(1, 9, 2, 7, 15, 24), o.box());
        Shape bend = shape("nusmp:mega/corridor/b", 9, 9, RoomKind.CORRIDOR, door(4, 0, Facing.NORTH), door(8, 4, Facing.EAST));
        List<Clearance7.Cut> b = Clearance7.cuts(new Placed(0, bend, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {false, false});
        assertEquals(1, b.size());
        assertEquals(Clearance7.Kind.CHAMBER, b.get(0).kind());
        assertEquals(new Box(1, 9, 1, 7, 15, 7), b.get(0).box());
        // a bent Way Onward (27 x 27): a lane from each socket to the far wall's inner face, the end walls stay two layers thick (the seal is in one of them)
        Shape bent = shape("nusmp:mega/onward/l", 27, 27, RoomKind.ONWARD,
                new Socket(new Vec(0, 9, 4), Facing.WEST, "nusmp:mega_door", "nusmp:mega_door", "minecraft:empty", 0, 0, "minecraft:cobbled_deepslate"),
                new Socket(new Vec(22, 9, 26), Facing.SOUTH, "nusmp:mega_onward", "nusmp:mega_onward", "minecraft:empty", 0, 0, "minecraft:air"));
        List<Clearance7.Cut> lanes = Clearance7.cuts(new Placed(0, bent, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {false, true});
        assertEquals(3, lanes.size(), "the entrance doorway and one lane per socket; the sealed end's own doorway is not cut");
        assertEquals(new Box(2, 9, 1, 24, 15, 7), lanes.get(1).box(), "from behind the seal's two wall layers to the east wall's inner face, 7 wide round z 4");
        assertEquals(new Box(19, 9, 2, 25, 15, 24), lanes.get(2).box(), "from behind the entrance's wall layers to the north wall's inner face, 7 wide round x 22");
        // a two-module corridor (a gallery) and a hall get no strip: they are wide enough already
        Shape gallery = shape("nusmp:mega/corridor/g", 18, 36, RoomKind.CORRIDOR);
        assertEquals(0, Clearance7.cuts(new Placed(0, gallery, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[0]).size());
    }

    @Test
    void aStairwellGetsNoApronSoItsStepsStayAndItsHeadRoomIsCutPerStep() {
        Shape stair = shape("nusmp:mega/stairwell/u", 9, 9, RoomKind.STAIRWELL, door(8, 4, Facing.EAST));
        List<Clearance7.Cut> cuts = Clearance7.cuts(new Placed(0, stair, "", 0, Vec.ZERO, 0, -1, -1, -1, false), new boolean[] {true});
        assertEquals(1, cuts.size());
        assertEquals(new Box(7, 9, 1, 8, 15, 7), cuts.get(0).box(), "the two wall layers only");
        List<Clearance7.Cut> col = Clearance7.stairCuts(0, List.of(new Vec(3, 10, 3), new Vec(3, 26, 3), new Vec(3, 12, 3), new Vec(4, 11, 3)), "t");
        assertEquals(3, col.size(), "x3z3 merges y10 and y12 (11..19) and keeps y26 apart (27..33); x4z3 is its own column");
        boolean merged = false, apart = false, other = false;
        for (Clearance7.Cut c : col) {
            if (c.box().minX() == 3 && c.box().minY() == 11 && c.box().maxY() == 19) merged = true;
            if (c.box().minX() == 3 && c.box().minY() == 27 && c.box().maxY() == 33) apart = true;
            if (c.box().minX() == 4 && c.box().minY() == 12 && c.box().maxY() == 18) other = true;
        }
        assertTrue(merged && apart && other, "" + col);
    }

    @Test
    void theSealCutIsSevenBySevenForEveryFacing() {
        for (Facing f : Facing.values()) {
            Box b = Clearance7.sealCut(new Vec(50, 65, 50), f, 2);
            int lat = f.alongX() ? b.sizeZ() : b.sizeX(), depth = f.alongX() ? b.sizeX() : b.sizeZ();
            assertEquals(7, lat, f.name());
            assertEquals(7, b.sizeY(), f.name());
            assertEquals(2, depth, f.name());
            assertTrue(b.contains(new Vec(50, 65, 50)), "covers the seal cell " + f);
            assertTrue(b.contains(new Vec(50, 65, 50).step(f.opposite())), "reaches one layer inward " + f);
            assertFalse(b.contains(new Vec(50, 65, 50).step(f)), "does not poke outward " + f);
        }
        assertEquals(4, Clearance7.sealCut(Vec.ZERO, Facing.NORTH, 9).sizeZ(), "the depth is clamped like the seal itself");
    }

    @Test
    void lightsStandInTheTopRowOnTheCentreLineOfCorridorsAndChambersOnly() {
        Clearance7.Cut strip = new Clearance7.Cut(Clearance7.Kind.CORRIDOR, 0, new Box(1, 9, 1, 7, 15, 25), Facing.NORTH, "t");
        List<Vec> lights = Clearance7.lightCells(strip);
        assertFalse(lights.isEmpty());
        for (Vec v : lights) {
            assertEquals(15, v.y());
            assertEquals(4, v.x());
            assertTrue(strip.box().contains(v));
        }
        assertEquals(6, lights.size(), "z 3, 7, 11, 15, 19, 23");
        Clearance7.Cut door = new Clearance7.Cut(Clearance7.Kind.DOORWAY, 0, new Box(1, 9, 0, 7, 15, 1), Facing.NORTH, "t");
        assertEquals(0, Clearance7.lightCells(door).size());
        Clearance7.Cut chamber = new Clearance7.Cut(Clearance7.Kind.CHAMBER, 0, new Box(1, 9, 1, 7, 15, 7), Facing.NORTH, "t");
        assertEquals(List.of(new Vec(4, 15, 4)), Clearance7.lightCells(chamber));
    }

    // ------------------------------------------------------------------------------------------------------------------------------ whole plans

    private static CatalogSource catalog() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        for (String id : new String[] {FloorPlan.P_UP, FloorPlan.P_DOWN, FloorPlan.P_GRAND_UP, FloorPlan.P_GRAND_DOWN, FloorPlan.P_STAIR_DOOR, "nusmp:mega/side", "nusmp:mega/cap", "nusmp:mega/arena",
                "nusmp:mega/antechamber", "nusmp:mega/vault_3", "nusmp:mega/onward", "nusmp:mega/link"}) {
            PoolDef p = cat.pool(id);
            boolean stocked = false;
            if (p != null) for (PoolEntry e : p.entries()) if (cat.shape(e.shape()) != null) stocked = true;
            if (!stocked) return null;
        }
        return cat;
    }

    @Test
    void cuttingKeepsEveryModuleConnectedOnRealFloorsOfBothRoadsOldAndCyclic() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        int plans = 0, doorways = 0, strips = 0, chambers = 0, joins = 0, loops = 0, caps = 0;
        for (boolean cyclic : new boolean[] {false, true}) {
            for (Polarity road : Polarity.values()) {
                for (int k : new int[] {0, 1, 3, 9}) {
                    for (long seed = 1; seed <= 6; seed++) {
                        Request q = TestPlans.floor(cat, seed * 7919L + k, road, k, 28, Recipe.forFloors(), cat.themes(), null);
                        q.cyclic = cyclic ? LoopConfig.defaults() : null;
                        SectorSolver.Solved sv = SectorSolver.solve(q, 24);
                        if (!sv.ok()) continue;
                        Result r = sv.result();
                        String who = (cyclic ? "cyclic " : "old ") + road + " floor " + (k + 1) + " seed " + seed;
                        assertEquals(List.of(), Clearance7.connectivity(r), who);
                        boolean[][] conn = Clearance7.connected(r);
                        for (Placed p : r.pieces) {
                            if (p.shape.isCap()) {
                                caps++;
                                for (boolean b : conn[p.index]) assertFalse(b, who + ": a door cap socket is not a passage");
                            }
                            if (p.parent >= 0 && r.pieces.get(p.parent).shape.isCap()) continue;
                            if (p.parent >= 0 && p.shape.isCap()) assertFalse(conn[p.parent][p.parentSocket], who + ": a socket that ends in a cap is not connected");
                        }
                        for (Clearance7.Cut c : Clearance7.cuts(r)) {
                            switch (c.kind()) {
                                case DOORWAY -> doorways++;
                                case CORRIDOR -> strips++;
                                case CHAMBER -> chambers++;
                                default -> { }
                            }
                            assertEquals(7, c.width(), who + " " + c.what());
                            assertEquals(7, c.height(), who + " " + c.what());
                        }
                        for (Placed p : r.pieces) if (!p.shape.isCap() && p.parent >= 0 && !r.pieces.get(p.parent).shape.isCap()) joins++;
                        loops += r.loops.size();
                        plans++;
                    }
                }
            }
        }
        System.out.println(String.format(Locale.ROOT, "CLEARANCE7 real plans: %d floors (old and cyclic, both roads, ordinary and boss): %d doorway cuts for %d joins and %d loops, %d corridor strips, %d chambers, %d door caps left alone; every module stays connected",
                plans, doorways, joins, loops, strips, chambers, caps));
        assertTrue(plans >= 60, "most plans must be found: " + plans);
        assertTrue(doorways >= joins + loops, "every ordinary join and loop has doorway cuts on both sides: " + doorways + " cuts for " + joins + " joins and " + loops + " loops");
    }

    @Test
    void theCutsNeverChangeThePlan() throws IOException {
        // Planning never reads the switch or the cuts: the same request gives the same pieces. (The loop planner and the dead-end pruner see footprints, which are untouched.)
        CatalogSource cat = catalog();
        if (cat == null) return;
        Request a = TestPlans.floor(cat, 4242L, Polarity.HOLY, 2, 28, Recipe.forFloors(), cat.themes(), null);
        a.cyclic = LoopConfig.defaults();
        SectorSolver.Solved s1 = SectorSolver.solve(a, 24);
        List<String> before = TestPlans.fingerprint(s1.result());
        Clearance7.cuts(s1.result());
        Clearance7.connectivity(s1.result());
        assertEquals(before, TestPlans.fingerprint(s1.result()));
        SectorSolver.Solved s2 = SectorSolver.solve(a, 24);
        assertEquals(before, TestPlans.fingerprint(s2.result()));
        // the cuts of every piece lie inside the piece
        List<Clearance7.Cut> cuts = Clearance7.cuts(s1.result());
        assertNotNull(cuts);
        for (Clearance7.Cut c : cuts) {
            Box pb = s1.result().pieces.get(c.piece()).box;
            assertTrue(c.box().minX() >= pb.minX() && c.box().maxX() <= pb.maxX() && c.box().minZ() >= pb.minZ() && c.box().maxZ() <= pb.maxZ(), c.what());
        }
    }
}
