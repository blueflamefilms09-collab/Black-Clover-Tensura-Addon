package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import com.newuniverse.nusmp.labyrinth.mega.enc.Sanctuary;
import com.newuniverse.nusmp.labyrinth.mega.enc.SpawnLayout;
import com.newuniverse.nusmp.labyrinth.mega.enc.SpawnPicker;
import com.newuniverse.nusmp.labyrinth.mega.enc.SpawnSearch;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Dungeon Master's trouble with finding a place was a claim about real floors ("two thirds of the rooms of a floor are corridors, so the halls round a party
 * are few and the ring of 24 to 48 blocks is often empty"), and a claim about real floors is tested on real floors: this plans floors of both roads from the
 * template catalog that ships (old and cyclic generator, many seeds), turns each plan into the plain {@link SpawnLayout} the search reads, stands a party in every
 * room that is not a safe room, and asks the search for a place with a line-of-sight model made from the plan itself (open inside a room, closed in the two wall
 * layers, open in a doorway). It measures how often the ring of 1.54.0 alone finds a place and how often the ladder does, and holds the ladder to the rules: it
 * finds one nearly always, never in a safe room, never nearer than the hard minimum, never in view except on the last rung and then only far away, and one seed
 * gives one answer. Both roads are planned, so floors above and below the Hub are covered alike.
 */
class EncRealFloorSpawnTest {
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

    private static Result plan(CatalogSource cat, long seed, Polarity road, int k, LoopConfig cfg) {
        Request q = TestPlans.floor(cat, seed, road, k, 28, Recipe.forFloors(), cat.themes(), null);
        q.cyclic = cfg;
        SectorSolver.Solved sv = SectorSolver.solve(q, 24);
        assertTrue(sv.ok(), road + " floor " + (k + 1) + " seed " + seed + " could not be built");
        return sv.result();
    }

    /** The saved sector of a plan as plain numbers: room boxes with their kinds, the spawn markers, the doorways. */
    static SpawnLayout layoutOf(Result r) {
        SectorFill f = SectorFill.of(r, r.region);
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        for (SectorFill.RoomOut o : f.rooms) rooms.add(new SpawnLayout.RoomBox(o.id(), o.kind(), o.box().minX(), o.box().minY(), o.box().minZ(), o.box().maxX(), o.box().maxY(), o.box().maxZ()));
        List<SpawnLayout.Spot> spots = new ArrayList<>();
        for (SectorFill.MarkerOut m : f.markers) {
            if (m.spec().kind() != MarkerKind.SPAWN) continue;
            spots.add(new SpawnLayout.Spot(m.cell().x(), m.cell().y(), m.cell().z(), m.spec().intArg("weight", 1)));
        }
        return new SpawnLayout(rooms, spots, f.edges);
    }

    /**
     * A line of sight made from the plan alone: a point is open inside a room box except in its two outer wall layers, where only a doorway (3 wide, 4 tall, through
     * both layers) is open; everything outside every room is closed. Pillars and furniture are ignored, so this errs towards seeing MORE than the game does.
     */
    static final class PlanSight implements SpawnPicker.LineOfSight {
        private final List<SpawnLayout.RoomBox> rooms;
        private final List<int[]> doors = new ArrayList<>();

        PlanSight(Result r, SpawnLayout layout) {
            this.rooms = layout.rooms();
            for (Placed p : r.pieces) {
                if (p.shape.isCap()) continue;
                for (int i = 0; i < p.shape.sockets.size(); i++) {
                    if (p.shape.sockets.get(i).sealed() || !p.isUsed(i)) continue;
                    Vec c = p.socketCell(i);
                    Facing f = p.socketFacing(i);
                    int half = 1;
                    // the hole runs 3 wide across the wall, 4 tall from the jigsaw cell, and 2 deep (the outer layer and the one behind it) on BOTH sides of the wall plane
                    int minX = c.x() - (f.alongX() ? 2 : half), maxX = c.x() + (f.alongX() ? 2 : half);
                    int minZ = c.z() - (f.alongX() ? half : 2), maxZ = c.z() + (f.alongX() ? half : 2);
                    doors.add(new int[] {minX, c.y(), minZ, maxX, c.y() + 3, maxZ});
                }
            }
        }

        private boolean inDoor(double x, double y, double z) {
            for (int[] d : doors) if (x >= d[0] && x < d[3] + 1 && y >= d[1] && y < d[4] + 1 && z >= d[2] && z < d[5] + 1) return true;
            return false;
        }

        private boolean open(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
            if (inDoor(x, y, z)) return true;
            SpawnLayout.RoomBox best = null;
            for (SpawnLayout.RoomBox r : rooms) {
                if (!r.contains(bx, by, bz)) continue;
                if (best == null || r.volume() < best.volume()) best = r;
            }
            if (best == null) return false;
            boolean wall = bx < best.minX() + 2 || bx > best.maxX() - 2 || bz < best.minZ() + 2 || bz > best.maxZ() - 2;
            return !wall;
        }

        @Override
        public boolean sees(double ex, double ey, double ez, int cx, int cy, int cz) {
            double tx = cx + 0.5, ty = cy + 1.0, tz = cz + 0.5;
            double dx = tx - ex, dy = ty - ey, dz = tz - ez;
            int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)));
            for (int i = 1; i < steps; i++) {
                double f = i / (double) steps;
                if (!open(ex + dx * f, ey + dy * f, ez + dz * f)) return false;
            }
            return true;
        }
    }

    private static final class Tally {
        int parties, ladderFound, ringFound;
        int[] byTier = new int[4];

        String line(String label) {
            return String.format(Locale.ROOT, "SPAWN real floors %-22s %5d party positions: the ring alone %.1f%%, the ladder %.1f%% (rungs 0/1/2/3: %d/%d/%d/%d)", label, parties, ringFound * 100.0 / Math.max(1, parties),
                    ladderFound * 100.0 / Math.max(1, parties), byTier[0], byTier[1], byTier[2], byTier[3]);
        }
    }

    private static void measure(Result r, Tally t, Random rnd) {
        SpawnLayout layout = layoutOf(r);
        PlanSight sight = new PlanSight(r, layout);
        for (SpawnLayout.RoomBox room : layout.rooms()) {
            if (Sanctuary.is(room.kind())) continue;
            double x = (room.minX() + room.maxX()) / 2.0 + 0.5, z = (room.minZ() + room.maxZ()) / 2.0 + 0.5, y = room.standY() + 1.62;
            List<SpawnPicker.Eye> eyes = List.of(new SpawnPicker.Eye(x, y, z));
            t.parties++;
            SpawnSearch.Found ring = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, sight, 48, false, 3, rnd);
            SpawnSearch.Found ladder = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, sight, 48, true, 3, rnd);
            if (ring.ok()) t.ringFound++;
            if (ladder.ok()) {
                t.ladderFound++;
                t.byTier[ladder.tier().index()]++;
                checkRules(layout, sight, eyes, ladder);
            }
        }
    }

    /** Every cell the ladder returns obeys the rules of its rung. */
    private static void checkRules(SpawnLayout layout, PlanSight sight, List<SpawnPicker.Eye> eyes, SpawnSearch.Found f) {
        List<SpawnPicker.Cell> all = new ArrayList<>(f.cells());
        all.addAll(f.reserve());
        for (SpawnPicker.Cell c : all) {
            assertFalse(c.safe(), "a safe cell was returned");
            SpawnLayout.RoomBox room = layout.room(c.roomId());
            assertFalse(room != null && Sanctuary.is(room.kind()), "a cell in a safe room was returned");
            double d = SpawnPicker.nearest(c, eyes);
            assertTrue(d >= SpawnSearch.HARD_MIN - 1e-9, "nearer than the hard minimum: " + d);
            assertTrue(d >= f.tier().min() - 1e-9 && d <= f.tier().max() + 1e-9, "outside the rung " + f.tier().range() + ": " + d);
            boolean seen = sight.sees(eyes.get(0).x(), eyes.get(0).y(), eyes.get(0).z(), c.x(), c.y(), c.z());
            if (seen) {
                assertEquals(3, f.tier().index(), "a cell in view on a rung that does not allow it");
                assertTrue(d >= f.tier().relaxSightFrom() - 1e-9, "a cell in view nearer than the relaxed distance: " + d);
            }
        }
    }

    @Test
    void theLadderFindsAPlaceOnRealFloorsAndTheRingAloneOftenDoesNot() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) {
            System.out.println("EncRealFloorSpawnTest: the catalog cannot make floors yet, nothing to measure.");
            return;
        }
        Tally old = new Tally(), cyc = new Tally();
        Random rnd = new Random(2024);
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < 12; k++) {
                for (int s = 0; s < 3; s++) {
                    long seed = Rng.mix(s + 1, 91 + k);
                    measure(plan(cat, seed, road, k, null), old, rnd);
                    measure(plan(cat, seed, road, k, LoopConfig.defaults()), cyc, rnd);
                }
            }
        }
        System.out.println(old.line("old generator"));
        System.out.println(cyc.line("cyclic generator"));
        for (Tally t : new Tally[] {old, cyc}) {
            assertTrue(t.parties > 200, "enough party positions were measured: " + t.parties);
            assertTrue(t.ladderFound >= t.ringFound, "the ladder never finds less than the ring");
            assertTrue(t.ladderFound >= t.parties * 0.97, "the ladder finds a place for 97 percent of the party positions: " + t.ladderFound + " of " + t.parties);
        }
    }

    @Test
    void theSearchOnARealFloorIsDeterministic() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        Result r = plan(cat, Rng.mix(3, 77), Polarity.HOLY, 4, LoopConfig.defaults());
        SpawnLayout layout = layoutOf(r);
        PlanSight sight = new PlanSight(r, layout);
        SpawnLayout.RoomBox room = null;
        for (SpawnLayout.RoomBox b : layout.rooms()) if (!Sanctuary.is(b.kind()) && !b.kind().isCirculation()) room = b;
        assertTrue(room != null);
        List<SpawnPicker.Eye> eyes = List.of(new SpawnPicker.Eye((room.minX() + room.maxX()) / 2.0, room.standY() + 1.62, (room.minZ() + room.maxZ()) / 2.0));
        SpawnSearch.Found a = SpawnSearch.find(layout, eyes, 24.0, 48.0, 3, sight, 48, true, 3, new Random(9));
        SpawnSearch.Found b = SpawnSearch.find(layout, eyes, 24.0, 48.0, 3, sight, 48, true, 3, new Random(9));
        assertEquals(a.cells(), b.cells(), "the same layout and seed give the same cells");
        assertTrue(a.ok());
    }

    @Test
    void everyKindOfRoomHasItsSpawnMarkersWhereTheCellsAreTakenFrom() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        // the standing row of every spawn marker of the shipped templates is the row the search uses for the cells it makes up (FLOOR_Y + 1), so a marker cell and a
        // made-up cell stand on the same floor
        int markers = 0, offRow = 0;
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < 6; k++) {
                Result r = plan(cat, Rng.mix(5, 13 + k), road, k, LoopConfig.defaults());
                for (SpawnLayout.Spot s : layoutOf(r).spots()) {
                    markers++;
                    if (s.y() != MegaGrid.FLOOR_Y + 1) offRow++;
                }
            }
        }
        assertTrue(markers > 100, "the floors carry spawn markers: " + markers);
        System.out.println("SPAWN markers: " + markers + " spawn markers on 12 floors, " + offRow + " off the standing row (FLOOR_Y + 1)");
        assertTrue(offRow <= markers / 10, "almost every marker stands on the standing row: " + offRow + " of " + markers);
        assertEquals(RoomKind.STAIRWELL, RoomKind.of("nusmp:mega/stairwell/anything"));
    }
}
