package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.dungeon.nexus.vault.VaultLayout.Role;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultLayout.Spot;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultOp.Pal;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The hidden-room planner (docs/living_dungeon.md, "Hidden Mana Vaults"): rolls, the search through a made-up cave, and the layouts. */
class VaultPlanTest {
    private static final VaultPlan.Rules ANY = (x, y, z) -> true;

    /** A candidate, with a cave opened in front of its wall at floor height y0 so that exactly that height has a doorway. */
    private static SimWorld caveFor(VaultPlan.Candidate c, int y0, int openCode) {
        SimWorld w = SimWorld.solid();
        VaultSite s = VaultPlan.siteAt(c, y0, c.hidden());
        for (int u = -2; u < s.width() + 2; u++)
            for (int v = -8; v <= -2; v++)
                for (int y = 0; y <= 3; y++) w.set(s.worldX(u, v), y0 + y, s.worldZ(u, v), openCode);
        if (openCode == VaultTerrain.AIR) {
            // the floor under the doorway stays solid, as it is
        }
        return w;
    }

    private static VaultPlan.Candidate candidate(VaultKind kind) {
        int grid = kind == VaultKind.GATE ? VaultPlan.GRID_GATE : VaultPlan.GRID_VAULT;
        return VaultPlan.candidate(grid, kind, 3, -2, 0);
    }

    @Test
    void aboutOneCellInFourHoldsAVault() {
        int with = 0, total = 0;
        Map<VaultKind, Integer> kinds = new EnumMap<>(VaultKind.class);
        for (int x = -40; x < 40; x++) {
            for (int z = -40; z < 40; z++) {
                VaultKind k = VaultPlan.vaultAt(x, z);
                total++;
                if (k != null) {
                    with++;
                    kinds.merge(k, 1, Integer::sum);
                    assertNotEquals(VaultKind.GATE, k, "a plain cell never holds a shrine");
                }
                assertEquals(k, VaultPlan.vaultAt(x, z), "stable");
            }
        }
        double share = with / (double) total;
        assertTrue(share > 0.20 && share < 0.30, "vault share " + share);
        for (VaultKind k : new VaultKind[]{VaultKind.PUZZLE, VaultKind.NUTRIENT, VaultKind.MINIBOSS}) assertTrue(kinds.getOrDefault(k, 0) > with / 6, k + " is common enough");
    }

    @Test
    void shrinesHoldInAboutHalfTheShrineCells() {
        int with = 0, total = 0;
        for (int x = -40; x < 40; x++) for (int z = -40; z < 40; z++) { total++; if (VaultPlan.gateAt(x, z)) with++; }
        double share = with / (double) total;
        assertTrue(share > 0.48 && share < 0.62, "shrine share " + share);
    }

    @Test
    void candidatesStayInsideTheirCellAndAreStable() {
        for (int grid = 0; grid < 3; grid++) {
            for (VaultKind kind : VaultKind.values()) {
                if (grid == VaultPlan.GRID_VAULT && kind == VaultKind.GATE) continue;
                for (int cx = -3; cx <= 3; cx++) {
                    for (int cz = -3; cz <= 3; cz++) {
                        for (int a = 0; a < VaultPlan.ATTEMPTS; a++) {
                            VaultPlan.Candidate c = VaultPlan.candidate(grid, kind, cx, cz, a);
                            assertEquals(c, VaultPlan.candidate(grid, kind, cx, cz, a));
                            int size = VaultPlan.CELL[grid];
                            VaultSite s = VaultPlan.siteAt(c, 40, c.hidden());
                            // the shell and the doorway stay inside the cell
                            int x0 = Math.min(s.ix() - 1, s.doorX()) - 1, x1 = Math.max(s.maxX() + 1, s.doorX()) + 1;
                            int z0 = Math.min(s.iz() - 1, s.doorZ()) - 1, z1 = Math.max(s.maxZ() + 1, s.doorZ()) + 1;
                            if (grid != VaultPlan.GRID_CORE) {
                                assertTrue(x0 >= cx * size && x1 < (cx + 1) * size, "x in cell " + c);
                                assertTrue(z0 >= cz * size && z1 < (cz + 1) * size, "z in cell " + c);
                            } else {
                                double dx = c.centreX() - (cx * size + size / 2.0), dz = c.centreZ() - (cz * size + size / 2.0);
                                double r = Math.hypot(dx, dz);
                                assertTrue(r >= 21 && r <= 45, "core shrine near the centre: " + r);
                                assertTrue(x0 >= cx * size && x1 < (cx + 1) * size && z0 >= cz * size && z1 < (cz + 1) * size, "inside the core cell");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void aRoundHasItsColumnsAndTheSearchTriesEveryWayAColumnCanFace() {
        List<VaultPlan.Candidate> round = VaultPlan.round(VaultPlan.GRID_VAULT, VaultKind.PUZZLE, 3, -2, 5);
        assertEquals(VaultPlan.COLUMNS, round.size());
        assertEquals(round, VaultPlan.round(VaultPlan.GRID_VAULT, VaultKind.PUZZLE, 3, -2, 5), "stable");
        assertEquals(new HashSet<>(round).size(), round.size(), "different columns");
        VaultPlan.Candidate c = round.get(1);
        for (int f = 0; f < 4; f++) {
            int turn = (c.facing() + f) % 4;
            SimWorld w = caveFor(c.withFacing(turn), 50, VaultTerrain.AIR);
            VaultPlan.Result r = VaultPlan.search(VaultPlan.GRID_VAULT, VaultKind.PUZZLE, 3, -2, 5, 40, 60, ANY, w);
            assertEquals(VaultPlan.Status.FOUND, r.status(), "a cave on the " + turn + " side");
            assertEquals(turn, r.site().facing());
            assertEquals(50, r.site().iy());
            assertEquals(c.centreX() - ((turn & 1) == 0 ? c.width() : c.depth()) / 2, r.site().ix());
        }
        assertEquals(VaultPlan.Status.NONE, VaultPlan.search(VaultPlan.GRID_VAULT, VaultKind.PUZZLE, 3, -2, 5, 40, 60, ANY, SimWorld.solid()).status());
        assertEquals(VaultPlan.Status.UNKNOWN, VaultPlan.search(VaultPlan.GRID_VAULT, VaultKind.PUZZLE, 3, -2, 5, 40, 60, ANY, (x, y, z) -> VaultTerrain.UNKNOWN).status());
    }

    @Test
    void theRoomFrameIsAnExactTurnOfTheSameBox() {
        for (int facing = 0; facing < 4; facing++) {
            VaultSite s = new VaultSite(VaultKind.PUZZLE, 100, 20, -50, 7, 5, 9, facing, true, 5);
            Set<Long> cells = new HashSet<>();
            for (int u = -1; u <= s.width(); u++) {
                for (int w = -1; w <= s.depth(); w++) {
                    int x = s.worldX(u, w), z = s.worldZ(u, w);
                    assertTrue(x >= s.ix() - 1 && x <= s.maxX() + 1 && z >= s.iz() - 1 && z <= s.maxZ() + 1, "inside the shell box");
                    assertTrue(cells.add(SimWorld.key(x, 0, z)), "no two local cells share a block");
                    // one step further out is one step out of the room
                    assertEquals(s.worldX(u, w) + s.outX(), s.worldX(u, w - 1), "w - 1 is outwards");
                    assertEquals(s.worldZ(u, w) + s.outZ(), s.worldZ(u, w - 1), "w - 1 is outwards");
                }
            }
            assertEquals((s.width() + 2) * (s.depth() + 2), cells.size());
            assertTrue(s.insideInterior(s.worldX(3, 4), 22, s.worldZ(3, 4), 0));
            assertFalse(s.insideInterior(s.worldX(3, -1), 22, s.worldZ(3, -1), 0), "the wall is not the interior");
            assertTrue(s.contains(s.worldX(3, -1), 22, s.worldZ(3, -1), 0), "but it is inside the shell");
        }
    }

    @Test
    void aCaveInFrontOfTheWallFindsTheRoomAtExactlyThatHeight() {
        for (VaultKind kind : VaultKind.values()) {
            VaultPlan.Candidate c = candidate(kind);
            SimWorld w = caveFor(c, 50, VaultTerrain.AIR);
            VaultPlan.Result r = VaultPlan.evaluate(c, 40, 60, ANY, w);
            assertEquals(VaultPlan.Status.FOUND, r.status(), kind.toString());
            assertEquals(50, r.site().iy(), kind + " found at the opened height");
            assertEquals(kind, r.site().kind());
            assertEquals(c.facing(), r.site().facing());
            assertEquals(r, VaultPlan.evaluate(c, 40, 60, ANY, w), "same answer twice");
        }
    }

    @Test
    void noCaveMeansNoRoomAndNeverGuesses() {
        VaultPlan.Candidate c = candidate(VaultKind.PUZZLE);
        assertEquals(VaultPlan.Status.NONE, VaultPlan.evaluate(c, -40, 100, ANY, SimWorld.solid()).status(), "solid rock has no doorway");
        SimWorld hollow = new SimWorld((x, y, z) -> VaultTerrain.AIR);
        assertEquals(VaultPlan.Status.NONE, VaultPlan.evaluate(c, -40, 100, ANY, hollow).status(), "an open cavern is not solid rock");
    }

    @Test
    void anUnloadedWorldGivesUnknownNotNone() {
        VaultPlan.Candidate c = candidate(VaultKind.MINIBOSS);
        assertEquals(VaultPlan.Status.UNKNOWN, VaultPlan.evaluate(c, 40, 60, ANY, (x, y, z) -> VaultTerrain.UNKNOWN).status());
        // one unloaded block that the opened room needs
        SimWorld w = caveFor(c, 50, VaultTerrain.AIR);
        VaultSite s = VaultPlan.siteAt(c, 50, true);
        w.set(s.worldX(0, s.depth()), 51, s.worldZ(0, s.depth()), VaultTerrain.UNKNOWN);
        assertEquals(VaultPlan.Status.UNKNOWN, VaultPlan.evaluate(c, 50, 50, ANY, w).status(), "one block the room needs is not loaded");
    }

    @Test
    void aFloodedWorldCostsABoundedNumberOfReads() {
        // water in front of every height: every floor has a doorway, so the room itself is read for each
        VaultPlan.Candidate c = candidate(VaultKind.MINIBOSS);
        SimWorld w = new SimWorld((x, y, z) -> VaultTerrain.STONE);
        VaultSite s = VaultPlan.siteAt(c, 0, true);
        for (int u = -2; u < s.width() + 2; u++) for (int v = -8; v <= -2; v++) for (int y = -50; y <= 110; y++) w.set(s.worldX(u, v), y, s.worldZ(u, v), VaultTerrain.WATER);
        VaultPlan.Result r = VaultPlan.evaluate(c, -44, 108, ANY, w);
        assertEquals(VaultPlan.Status.FOUND, r.status());
        assertTrue(w.reads < 15_000, "reads " + w.reads);
    }

    @Test
    void lavaOrABadWallOrTheRulesRefuseTheRoom() {
        VaultPlan.Candidate c = candidate(VaultKind.NUTRIENT);
        VaultSite s = VaultPlan.siteAt(c, 50, true);
        // lava in the room
        SimWorld lava = caveFor(c, 50, VaultTerrain.AIR);
        lava.set(s.worldX(0, 0), 50, s.worldZ(0, 0), VaultTerrain.LAVA);
        assertNotEquals(VaultPlan.Status.FOUND, VaultPlan.evaluate(c, 50, 50, ANY, lava).status());
        // a wall full of ore and odd rock
        SimWorld odd = caveFor(c, 50, VaultTerrain.AIR);
        for (int u = 0; u < s.width(); u++) for (int y = 0; y < s.height(); y++) odd.set(s.worldX(u, -1), 50 + y, s.worldZ(u, -1), VaultTerrain.OTHER);
        assertNotEquals(VaultPlan.Status.FOUND, VaultPlan.evaluate(c, 50, 50, ANY, odd).status());
        // a wall that is mostly open is no wall
        SimWorld open = caveFor(c, 50, VaultTerrain.AIR);
        for (int u = 0; u < s.width(); u++) for (int y = 0; y < s.height(); y++) open.set(s.worldX(u, -1), 50 + y, s.worldZ(u, -1), VaultTerrain.AIR);
        assertNotEquals(VaultPlan.Status.FOUND, VaultPlan.evaluate(c, 50, 50, ANY, open).status());
        // the rules (a core cell, say)
        assertNotEquals(VaultPlan.Status.FOUND, VaultPlan.evaluate(c, 50, 50, (x, y, z) -> false, caveFor(c, 50, VaultTerrain.AIR)).status());
        // a wall with a few ores is fine
        SimWorld ores = caveFor(c, 50, VaultTerrain.AIR);
        ores.set(s.worldX(1, -1), 50, s.worldZ(1, -1), VaultTerrain.OTHER);
        ores.set(s.worldX(4, -1), 52, s.worldZ(4, -1), VaultTerrain.OTHER);
        assertEquals(VaultPlan.Status.FOUND, VaultPlan.evaluate(c, 50, 50, ANY, ores).status());
    }

    @Test
    void aFloodedDoorwayForcesAnAirtightIllusionWall() {
        VaultPlan.Candidate c = null;
        for (int cx = 0; cx < 40 && c == null; cx++) {
            VaultPlan.Candidate t = VaultPlan.candidate(VaultPlan.GRID_GATE, VaultKind.GATE, cx, 0, 0);
            if (!t.hidden()) c = t;
        }
        assertNotNull(c, "some shrine candidate has an open door");
        VaultPlan.Result dry = VaultPlan.evaluate(c, 50, 50, ANY, caveFor(c, 50, VaultTerrain.AIR));
        assertEquals(VaultPlan.Status.FOUND, dry.status());
        assertFalse(dry.site().hidden(), "a dry open-door shrine stays open");
        VaultPlan.Result wet = VaultPlan.evaluate(c, 50, 50, ANY, caveFor(c, 50, VaultTerrain.WATER));
        assertEquals(VaultPlan.Status.FOUND, wet.status());
        assertTrue(wet.site().hidden(), "water outside means an illusion wall, so the water cannot come in");
    }

    // ---------------------------------------------------------------- layouts
    private static VaultSite site(VaultKind kind, int facing, int variant) {
        int w = kind == VaultKind.PUZZLE ? 7 : kind == VaultKind.MINIBOSS ? 9 : kind == VaultKind.NUTRIENT ? 7 : 5;
        int h = kind == VaultKind.PUZZLE ? 5 : kind == VaultKind.MINIBOSS ? 6 : kind == VaultKind.NUTRIENT ? 5 : 4;
        int d = kind == VaultKind.PUZZLE ? 9 : kind == VaultKind.MINIBOSS ? 9 : kind == VaultKind.NUTRIENT ? 7 : 5;
        return new VaultSite(kind, 200, 30, 300, w, h, d, facing, kind != VaultKind.GATE, variant);
    }

    @Test
    void everyLayoutStaysInsideItsShellAndIsStable() {
        for (VaultKind kind : VaultKind.values()) {
            for (int facing = 0; facing < 4; facing++) {
                VaultSite s = site(kind, facing, 12345 + facing);
                VaultLayout l = VaultPlan.layout(s);
                assertEquals(l.ops, VaultPlan.layout(s).ops, "stable");
                assertTrue(l.cells() < 2500, kind + " is a small build: " + l.cells());
                for (VaultOp o : l.ops) {
                    assertTrue(s.contains(o.x1(), o.y1(), o.z1(), 0) && s.contains(o.x2(), o.y2(), o.z2(), 0), kind + " op inside the shell: " + o);
                }
                for (Role r : Role.values()) {
                    if (r == Role.DOOR) continue;
                    for (Spot sp : l.spots(r)) assertTrue(s.insideInterior(sp.x(), sp.y(), sp.z(), 0) || r == Role.PLATE, kind + " " + r + " spot inside the room");
                }
                Spot door = l.first(Role.DOOR);
                assertNotNull(door);
                assertFalse(s.contains(door.x(), door.y(), door.z(), 0), "the door spot is outside the shell");
            }
        }
    }

    @Test
    void appliedToSolidRockARoomIsHollowAndWalledWithIllusion() {
        for (VaultKind kind : VaultKind.values()) {
            for (int facing = 0; facing < 4; facing++) {
                VaultSite s = site(kind, facing, 777);
                VaultLayout l = VaultPlan.layout(s);
                SimWorld w = SimWorld.solid();
                w.apply(l);
                // the wall plane
                for (int u = 0; u < s.width(); u++) {
                    for (int y = 0; y < s.height(); y++) {
                        int x = s.worldX(u, -1), z = s.worldZ(u, -1);
                        if (s.hidden()) assertEquals(Pal.ILLUSION, w.palAt(x, s.iy() + y, z), kind + " illusion wall at " + u + "," + y);
                        else if (u == s.width() / 2 && y < 2) assertEquals(VaultTerrain.AIR, w.at(x, s.iy() + y, z), "a doorway");
                        else assertTrue(VaultTerrain.solid(w.at(x, s.iy() + y, z)), "a rock wall beside the doorway");
                    }
                }
                // the shell is closed all round: every cell just outside the interior except the wall plane is solid
                for (int u = -1; u <= s.width(); u++) {
                    for (int y = -1; y <= s.height(); y++) {
                        for (int v = -1; v <= s.depth(); v++) {
                            boolean interior = u >= 0 && u < s.width() && y >= 0 && y < s.height() && v >= 0 && v < s.depth();
                            if (interior || v == -1 && u >= 0 && u < s.width() && y >= 0 && y < s.height()) continue;
                            assertTrue(VaultTerrain.solid(w.at(s.worldX(u, v), s.iy() + y, s.worldZ(u, v))), kind + " shell closed at " + u + "," + y + "," + v);
                        }
                    }
                }
                // the plates and the bars exist where the puzzle says
                if (kind == VaultKind.PUZZLE) {
                    assertEquals(9, l.spots(Role.PLATE).size());
                    for (Spot p : l.spots(Role.PLATE)) assertEquals(Pal.PLATE_IDLE, w.palAt(p.x(), p.y(), p.z()));
                    assertEquals(3, l.spots(Role.BARS).size());
                    for (Spot p : l.spots(Role.BARS)) assertEquals(Pal.IRON_BARS, w.palAt(p.x(), p.y(), p.z()));
                    Spot ped = l.first(Role.PEDESTAL);
                    assertEquals(VaultTerrain.AIR, w.at(ped.x(), ped.y(), ped.z()), "the pedestal cell is free for the glue");
                    assertEquals(VaultTerrain.AIR, w.at(ped.x(), ped.y() + 1, ped.z()));
                    // nothing but the bars stands between the room and the cache
                    Spot b0 = l.spots(Role.BARS).get(0);
                    assertEquals(1, Math.abs(b0.x() - ped.x()) + Math.abs(b0.z() - ped.z()), "bars sit right in front of the pedestal");
                }
                if (kind == VaultKind.GATE) {
                    Spot g = l.first(Role.GATE), st = l.first(Role.STAND);
                    assertEquals(Pal.GATE_DOWN, w.palAt(g.x(), g.y(), g.z()));
                    assertEquals(VaultTerrain.AIR, w.at(st.x(), st.y(), st.z()), "the arrival cell is open");
                    assertEquals(VaultTerrain.AIR, w.at(st.x(), st.y() + 1, st.z()));
                    assertTrue(VaultTerrain.solid(w.at(st.x(), st.y() - 1, st.z())), "with a floor");
                }
                if (kind == VaultKind.MINIBOSS) {
                    Spot g = l.first(Role.GUARDIAN), p = l.first(Role.PEDESTAL);
                    assertEquals(VaultTerrain.AIR, w.at(g.x(), g.y(), g.z()));
                    assertEquals(VaultTerrain.AIR, w.at(p.x(), p.y(), p.z()));
                }
            }
        }
    }

    @Test
    void theGardenHasAtMostTenBloomsAllOnMoss() {
        for (int variant = 0; variant < 200; variant += 7) {
            VaultSite s = new VaultSite(VaultKind.NUTRIENT, 10, 30, 10, (variant & 1) == 1 ? 9 : 7, 5, (variant & 1) == 1 ? 9 : 7, variant & 3, true, variant);
            VaultLayout l = VaultPlan.layout(s);
            int n = l.spots(Role.BLOOM).size();
            assertTrue(n <= 10, "blooms " + n);
            SimWorld w = SimWorld.solid();
            w.apply(l);
            for (Spot b : l.spots(Role.BLOOM)) {
                Pal p = w.palAt(b.x(), b.y(), b.z());
                assertTrue(p == Pal.BLOOM_1 || p == Pal.BLOOM_2 || p == Pal.BLOOM_3, "a bloom of some age: " + p);
                assertEquals(Pal.MOSS, w.palAt(b.x(), b.y() - 1, b.z()), "on moss");
            }
        }
        int total = 0;
        for (int variant = 0; variant < 50; variant++) total += VaultPlan.layout(new VaultSite(VaultKind.NUTRIENT, 0, 30, 0, 7, 5, 7, 0, true, variant)).spots(Role.BLOOM).size();
        assertTrue(total > 50 * 3, "gardens are not bare");
    }

    @Test
    void theChamberIsASealedShrineWhoseGateLeadsUp() {
        VaultSite s = new VaultSite(VaultKind.GATE, 0, 0, 0, 5, 4, 5, 2, false, 3);
        VaultLayout l = VaultPlan.chamberLayout(s);
        assertTrue(l.site.hidden(), "a chamber is always walled with illusion");
        SimWorld w = new SimWorld((x, y, z) -> VaultTerrain.WATER);                 // cut in open water
        w.apply(l);
        Spot g = l.first(Role.GATE);
        assertEquals(Pal.GATE_UP, w.palAt(g.x(), g.y(), g.z()));
        // sealed: no water cell touches an air cell of the room
        for (int u = 0; u < 5; u++) for (int y = 0; y < 4; y++) for (int v = 0; v < 5; v++) {
            int x = l.site.worldX(u, v), z = l.site.worldZ(u, v);
            for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                assertNotEquals(VaultTerrain.WATER, w.at(x + d[0], l.site.iy() + y + d[1], z + d[2]), "water against the chamber at " + u + "," + y + "," + v);
            }
        }
    }

    @Test
    void theChamberSearchPrefersRockAndAnOpenDoor() {
        // everything is stone except a pool of water to the north of x=0, z=0
        SimWorld w = new SimWorld((x, y, z) -> z < -3 && y >= 0 && y <= 10 ? VaultTerrain.WATER : VaultTerrain.STONE);
        VaultPlan.Result r = VaultPlan.chamber(0, 0, 0, 8, 1, ANY, w);
        assertEquals(VaultPlan.Status.FOUND, r.status());
        assertEquals(VaultSite.NORTH, r.site().facing(), "the exit faces the water");
        assertEquals(VaultPlan.Status.UNKNOWN, VaultPlan.chamber(0, 0, 0, 8, 1, ANY, (x, y, z) -> VaultTerrain.UNKNOWN).status());
        assertEquals(VaultPlan.Status.NONE, VaultPlan.chamber(0, 0, 0, 8, 1, (x, y, z) -> false, w).status());
        assertEquals(VaultPlan.Status.NONE, VaultPlan.chamber(0, 0, 0, 8, 1, ANY, (x, y, z) -> VaultTerrain.LAVA).status());
    }

    @Test
    void aCursorInSmallSlicesBuildsTheSameRoomAsOneBigRun() {
        VaultSite s = site(VaultKind.PUZZLE, 1, 99);
        VaultLayout l = VaultPlan.layout(s);
        SimWorld whole = SimWorld.solid();
        whole.apply(l);
        SimWorld sliced = SimWorld.solid();
        VaultCursor c = new VaultCursor(l, 0, 0);
        int ticks = 0;
        while (!c.done()) {
            long spent = c.run((op, x, y, z) -> sliced.place(op, x, y, z), 400);
            assertTrue(spent <= 400 + VaultCursor.WRITE, "within budget");
            assertTrue(++ticks < 10_000);
        }
        assertTrue(ticks > 1, "a room takes more than one slice at a small budget");
        for (int x = s.ix() - 2; x <= s.maxX() + 2; x++)
            for (int y = s.iy() - 2; y <= s.maxY() + 2; y++)
                for (int z = s.iz() - 2; z <= s.maxZ() + 2; z++) {
                    assertEquals(whole.at(x, y, z), sliced.at(x, y, z), x + "," + y + "," + z);
                    assertEquals(whole.palAt(x, y, z), sliced.palAt(x, y, z), x + "," + y + "," + z);
                }
    }

    @Test
    void resumingEarlyOrApplyingTwiceChangesNothing() {
        VaultSite s = site(VaultKind.MINIBOSS, 3, 5);
        VaultLayout l = VaultPlan.layout(s);
        SimWorld w = SimWorld.solid();
        VaultCursor c = new VaultCursor(l, 0, 0);
        c.run((op, x, y, z) -> w.place(op, x, y, z), 600);
        int op = c.op();
        VaultCursor again = VaultCursor.resume(l, op);
        assertTrue(again.op() <= op && again.op() >= op - VaultCursor.REWIND);
        again.run((o, x, y, z) -> w.place(o, x, y, z), Long.MAX_VALUE);
        SimWorld once = SimWorld.solid();
        once.apply(l);
        for (int x = s.ix() - 2; x <= s.maxX() + 2; x++)
            for (int y = s.iy() - 2; y <= s.maxY() + 2; y++)
                for (int z = s.iz() - 2; z <= s.maxZ() + 2; z++) assertEquals(once.at(x, y, z), w.at(x, y, z));
        w.apply(l);                                                                // the whole layout over a finished room: the same room
        for (int x = s.ix() - 2; x <= s.maxX() + 2; x++)
            for (int y = s.iy() - 2; y <= s.maxY() + 2; y++)
                for (int z = s.iz() - 2; z <= s.maxZ() + 2; z++) {
                    assertEquals(once.at(x, y, z), w.at(x, y, z), "applied twice " + x + "," + y + "," + z);
                    assertEquals(once.palAt(x, y, z), w.palAt(x, y, z), "applied twice " + x + "," + y + "," + z);
                }
    }

    @Test
    void theChunksOfASiteCoverItsShellAndItsDoorway() {
        for (int facing = 0; facing < 4; facing++) {
            for (int ix = -40; ix < 40; ix += 7) {
                VaultSite s = new VaultSite(VaultKind.MINIBOSS, ix, 20, ix * 3 - 5, 9, 6, 9, facing, true, 1);
                Set<Long> chunks = new HashSet<>();
                for (int[] c : s.chunks()) chunks.add(((long) c[0] << 32) | (c[1] & 0xFFFFFFFFL));
                assertEquals(s.chunks().size(), chunks.size(), "no chunk twice");
                for (int u = -1; u <= s.width(); u++) {
                    for (int w = -3; w <= s.depth(); w++) {
                        int x = s.worldX(u, w), z = s.worldZ(u, w);
                        long key = ((long) Math.floorDiv(x, 16) << 32) | (Math.floorDiv(z, 16) & 0xFFFFFFFFL);
                        assertTrue(chunks.contains(key), "chunk of " + u + "," + w + " is listed");
                    }
                }
            }
        }
    }

    @Test
    void sitesThatShareABoxIntersect() {
        VaultSite a = new VaultSite(VaultKind.PUZZLE, 0, 30, 0, 7, 5, 9, 0, true, 1);
        VaultSite near = new VaultSite(VaultKind.GATE, 9, 30, 0, 5, 4, 5, 0, true, 1);
        VaultSite far = new VaultSite(VaultKind.GATE, 40, 30, 0, 5, 4, 5, 0, true, 1);
        assertFalse(a.intersects(near, 0), "a clear block between the shells");
        assertTrue(a.intersects(near, 1), "but not with a margin of one");
        assertTrue(a.intersects(a, 0));
        assertFalse(a.intersects(far, 0));
        assertTrue(a.intersects(new VaultSite(VaultKind.GATE, 7, 30, 0, 5, 4, 5, 0, true, 1), 0), "shells that touch overlap");
        assertTrue(a.intersects(far, 40), "a wide margin reaches");
    }
}
