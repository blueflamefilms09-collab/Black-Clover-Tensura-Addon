package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the template-by-template test proves the cuts on a piece that is not turned; the game places pieces turned and glued door to door, and the cut boxes are turned with
 * them by {@code Placed.world}. A mistake there (a quarter turn applied to the wrong corner) would show only as walls in the wrong place in the game. So this builds WHOLE
 * planned floors out of the real template files, pasted at their real rotations and origins into one grid, cuts every piece exactly as the placer does, and then measures
 * with the rule: every doorway cut, every corridor and chamber, and every JOINT (the two cuts of a join or a loop taken together must make one 7 x 7 tunnel through all
 * four wall layers). A transition that stays below 7 x 7 must be blocked by a block the cut may not remove.
 */
class Clearance7PlanWorldTest {
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

    /** The whole plan as one grid, origin shifted so the plan's lowest corner is (0,0,0). */
    private static ClearanceGrid paste(Result r, Map<String, ClearanceGrid> cache, Vec[] shiftOut) throws IOException {
        Box b = r.bounds();
        Vec shift = new Vec(-b.minX(), -b.minY(), -b.minZ());
        shiftOut[0] = shift;
        ClearanceGrid g = new ClearanceGrid(b.sizeX(), b.sizeY(), b.sizeZ());
        for (Placed p : r.pieces) {
            ClearanceGrid t = cache.get(p.shape.id);
            if (t == null) {
                t = TemplateGrids.load(p.shape.id);
                if (t == null) throw new IllegalStateException("template file missing: " + p.shape.id);
                cache.put(p.shape.id, t);
            }
            t.forEach((x, y, z, id, be, st) -> {
                Vec w = p.world(new Vec(x, y, z)).add(shift);
                g.set(w.x(), w.y(), w.z(), id, be, st);
            });
        }
        return g;
    }

    private static Clearance7.Cut shifted(Clearance7.Cut c, Vec s) {
        Box b = c.box();
        return new Clearance7.Cut(c.kind(), c.piece(), new Box(b.minX() + s.x(), b.minY() + s.y(), b.minZ() + s.z(), b.maxX() + s.x(), b.maxY() + s.y(), b.maxZ() + s.z()), c.axis(), c.what());
    }

    private static String blocker(ClearanceGrid g, Box b) {
        for (int y = b.minY(); y <= b.maxY(); y++) for (int x = b.minX(); x <= b.maxX(); x++) for (int z = b.minZ(); z <= b.maxZ(); z++) {
            if (!g.free(x, y, z)) return Clearance7.carvable(g.id(x, y, z), g.blockEntity(x, y, z)) ? null : g.id(x, y, z);
        }
        return "?";
    }

    /**
     * An independent look at one doorway, written without the cut boxes: from the jigsaw cell of the outer layer of one room, the four wall layers of the two rooms
     * (one cell back, the cell, the cell in front, the next) at 7 wide and 7 high round the centre line must be free. Returns null (free), "" (something stands there that is
     * never cut) or a sentence.
     */
    private static String tunnelProblem(ClearanceGrid g, Vec cell, Facing out) {
        Facing lat = out.cw();
        boolean never = false;
        for (int along = -1; along <= 2; along++) {
            for (int side = -3; side <= 3; side++) {
                for (int up = 0; up < 7; up++) {
                    Vec v = cell.step(out, along).step(lat, side).add(0, up, 0);
                    if (g.free(v.x(), v.y(), v.z())) continue;
                    if (Clearance7.carvable(g.id(v.x(), v.y(), v.z()), g.blockEntity(v.x(), v.y(), v.z()))) return g.id(v.x(), v.y(), v.z()) + " at " + v + " (layer " + along + ", side " + side + ", up " + up + ") stands in the doorway and could have been cut";
                    never = true;
                }
            }
        }
        return never ? "" : null;
    }

    @Test
    void wholeFloorsBuiltFromTheRealTemplatesHaveAClear7x7TunnelAtEveryJoint() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        Map<String, ClearanceGrid> cache = new HashMap<>();
        int plans = 0, cuts = 0, joints = 0, blocked = 0, rotated = 0;
        List<String> wrong = new ArrayList<>();
        for (boolean cyclic : new boolean[] {false, true}) {
            for (Polarity road : Polarity.values()) {
                for (int k : new int[] {0, 9}) {
                    for (long seed = 1; seed <= 2; seed++) {
                        Request q = TestPlans.floor(cat, seed * 104729L + k, road, k, 28, Recipe.forFloors(), cat.themes(), null);
                        q.cyclic = cyclic ? LoopConfig.defaults() : null;
                        SectorSolver.Solved sv = SectorSolver.solve(q, 24);
                        if (!sv.ok()) continue;
                        Result r = sv.result();
                        String who = (cyclic ? "cyclic " : "old ") + road + " floor " + (k + 1) + " seed " + seed;
                        Vec[] sh = new Vec[1];
                        ClearanceGrid g = paste(r, cache, sh);
                        List<Clearance7.Cut> all = new ArrayList<>();
                        for (Clearance7.Cut c : Clearance7.cuts(r)) all.add(shifted(c, sh[0]));
                        for (Placed p : r.pieces) {
                            if (p.shape.kind == RoomKind.STAIRWELL) {
                                List<Vec> stands = new ArrayList<>();
                                for (Vec v : cache.get(p.shape.id).stairStands()) stands.add(p.world(v).add(sh[0]));
                                all.addAll(Clearance7.stairCuts(p.index, stands, p.shape.id));
                            }
                            if (p.rot != 0) rotated++;
                        }
                        g.cutAll(all);
                        for (Clearance7.Cut c : all) {
                            cuts++;
                            Clearance7.Clearance m = Clearance7.measure(g, c);
                            if (m.ok()) continue;
                            String why = blocker(g, c.box());
                            if (why == null) wrong.add(who + ": " + c.what() + " " + c.kind() + " is " + m + " and could have been cut");
                            else blocked++;
                        }
                        // the joints: the two doorway cuts of a join / loop make one tunnel
                        boolean[][] conn = Clearance7.connected(r);
                        List<Clearance7.Cut> doors = new ArrayList<>();
                        for (Clearance7.Cut c : all) if (c.kind() == Clearance7.Kind.DOORWAY) doors.add(c);
                        for (Placed p : r.pieces) {
                            if (p.shape.isCap() || p.parent < 0 || r.pieces.get(p.parent).shape.isCap()) continue;
                            Placed par = r.pieces.get(p.parent);
                            if (!Clearance7.isCuttableDoor(par.shape.sockets.get(p.parentSocket)) || !Clearance7.isCuttableDoor(p.shape.sockets.get(p.ownSocket))) continue;
                            Box a = null, b = null;
                            Vec sa = par.socketCell(p.parentSocket).add(sh[0]), sb = p.socketCell(p.ownSocket).add(sh[0]);
                            for (Clearance7.Cut c : doors) {
                                if (c.piece() == par.index && c.box().contains(sa)) a = c.box();
                                if (c.piece() == p.index && c.box().contains(sb)) b = c.box();
                            }
                            if (a == null || b == null) {
                                wrong.add(who + ": no doorway cut for the join " + par + " -> " + p);
                                continue;
                            }
                            String tp = tunnelProblem(g, sa, par.socketFacing(p.parentSocket));
                            if (tp != null && !tp.isEmpty()) wrong.add(who + ": the join " + par + " -> " + p + ": " + tp);
                            Box u = a.union(b);
                            Facing axis = par.socketFacing(p.parentSocket);
                            Clearance7.Clearance m = Clearance7.measure(g, new Clearance7.Cut(Clearance7.Kind.DOORWAY, p.index, u, axis, "joint"));
                            joints++;
                            if (!m.ok()) {
                                String why = blocker(g, u);
                                if (why == null) wrong.add(who + ": the joint " + par.shape.id + " -> " + p.shape.id + " is " + m + " and could have been cut");
                                else blocked++;
                            }
                        }
                        for (Result.Loop l : r.loops) {
                            Placed pa = r.pieces.get(l.pieceA()), pb = r.pieces.get(l.pieceB());
                            if (!Clearance7.isCuttableDoor(pa.shape.sockets.get(l.socketA())) || !Clearance7.isCuttableDoor(pb.shape.sockets.get(l.socketB()))) continue;
                            String tp = tunnelProblem(g, pa.socketCell(l.socketA()).add(sh[0]), pa.socketFacing(l.socketA()));
                            joints++;
                            if (tp != null && !tp.isEmpty()) wrong.add(who + ": the loop " + pa + " <-> " + pb + ": " + tp);
                        }
                        assertTrue(conn.length == r.pieces.size());
                        plans++;
                    }
                }
            }
        }
        System.out.println(String.format(Locale.ROOT, "CLEARANCE7 whole floors pasted from the real templates: %d floors, %d pieces turned, %d cut boxes and %d joints measured, %d only blocked by a block that is never cut; %d wrong",
                plans, rotated, cuts, joints, blocked, wrong.size()));
        for (String w : wrong) System.out.println("CLEARANCE7 wrong: " + w);
        assertEquals(List.of(), wrong);
        assertTrue(plans >= 12, "plans found: " + plans);
        assertTrue(rotated > 100, "the test must exercise turned pieces: " + rotated);
    }
}
