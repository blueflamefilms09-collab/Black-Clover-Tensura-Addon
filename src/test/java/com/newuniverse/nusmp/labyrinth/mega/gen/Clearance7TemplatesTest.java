package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the rule {@code clearance7} and the cuts that satisfy it are only believable when they are run on the REAL templates, not on shapes made up for a test. This reads
 * every template file that ships (through the dump of their sockets, {@code src/test/resources/mega/catalog.json}, and the NBT itself), treats every ordinary door as connected
 * (the worst case: every cut is made), cuts it exactly as the game does, and measures every doorway, corridor, chamber and staircase with the rule. A transition that the
 * cuts could not make 7 x 7 (a chest or a gate stands in it) is listed with the block that stops it; the count of those is pinned, so a new template that puts a chest in
 * a doorway fails the build instead of the game. It also prints the numbers the doc quotes.
 */
class Clearance7TemplatesTest {
    private static CatalogSource catalog() throws IOException { return CatalogSource.load(CatalogSource.DEFAULT); }

    private static Placed turned(Shape s) { return new Placed(0, s, "nusmp:mega_interior", 0, Vec.ZERO, 0, -1, -1, -1, false); }

    private static boolean[] allDoors(Shape s) {
        boolean[] c = new boolean[s.sockets.size()];
        for (int i = 0; i < c.length; i++) c[i] = Clearance7.isCuttableDoor(s.sockets.get(i)) && i != s.expansionSocket && !(s.sockets.get(i).sealed() && s.sockets.get(i).isDoor());      // a sealed end is never a passage
        return c;
    }

    /** What stands in the way of the transition after the cut: the first non-free block of its box. */
    private static String blocker(ClearanceGrid g, Clearance7.Cut c) {
        Box b = c.box();
        for (int y = b.minY(); y <= b.maxY(); y++) for (int x = b.minX(); x <= b.maxX(); x++) for (int z = b.minZ(); z <= b.maxZ(); z++) {
            if (!g.free(x, y, z)) return g.id(x, y, z) + " at " + x + "," + y + "," + z;
        }
        return "?";
    }

    /** How many cells of the shape's rubble seal (w x h x d at the expansion socket, centred, rising, inward) hold a block that is not free; 0 for a shape with no seal. */
    private static int sealBlocks(ClearanceGrid g, Shape sh) {
        if (sh.expansionSocket < 0) return 0;
        Socket s = sh.sockets.get(sh.expansionSocket);
        Facing in = s.facing().opposite(), lat = s.facing().cw();
        int n = 0;
        for (int i = -((sh.sealW - 1) / 2); i <= sh.sealW / 2; i++) {
            for (int y = 0; y < sh.sealH; y++) {
                for (int k = 0; k < sh.sealD; k++) {
                    Vec v = s.cell().step(lat, i).step(in, k).add(0, y, 0);
                    if (!g.free(v.x(), v.y(), v.z())) n++;
                }
            }
        }
        return n;
    }

    @Test
    void everyRealTemplateIsSevenBySevenAfterTheCuts() throws IOException {
        CatalogSource cat = catalog();
        if (cat.isEmpty()) return;
        int templates = 0, doors = 0, strips = 0, chambers = 0, stairs = 0, cleared = 0, kept = 0, capped = 0, lit = 0, missing = 0;
        List<String> stuck = new ArrayList<>();
        TreeMap<String, Integer> byKind = new TreeMap<>(), capsBy = new TreeMap<>();
        List<String> capsList = new ArrayList<>();
        int stairSteps = 0, stairShort = 0, seals = 0;
        for (Shape sh : cat.allShapes()) {
            if (sh.isCap()) continue;
            ClearanceGrid g = TemplateGrids.load(sh.id);
            if (g == null) {
                missing++;
                continue;
            }
            assertEquals(sh.sx, g.sx, sh.id);
            assertEquals(sh.sy, g.sy, sh.id);
            assertEquals(sh.sz, g.sz, sh.id);
            templates++;
            Placed p = turned(sh);
            List<Clearance7.Cut> cuts = new ArrayList<>(Clearance7.cuts(p, allDoors(sh)));
            if (sh.kind == RoomKind.STAIRWELL) {
                List<Vec> stands = g.stairStands();
                stairSteps += stands.size();
                stairShort += Clearance7.shortHeadroom(g, stands);
                cuts.addAll(Clearance7.stairCuts(0, stands, sh.id));
            }
            int sealBefore = sealBlocks(g, sh);
            ClearanceGrid.Stats st = g.cutAll(cuts);
            assertEquals(sealBefore, sealBlocks(g, sh), sh.id + ": the cuts must leave the rubble seal of the Way Onward exactly as it is (it is opened later, with the next sector)");
            if (sealBefore > 0) seals++;
            cleared += st.cleared();
            kept += st.kept();
            capped += st.capped();
            if (st.capped() > 0) capsBy.merge(sh.kind.name(), st.capped(), Integer::sum);
            if (st.capped() > 0) capsList.add(sh.id.replace("nusmp:mega/", "") + "=" + st.capped());
            lit += st.lit();
            for (Clearance7.Cut c : cuts) {
                switch (c.kind()) {
                    case DOORWAY -> doors++;
                    case CORRIDOR -> strips++;
                    case CHAMBER -> chambers++;
                    case STAIR -> stairs++;
                }
                Clearance7.Clearance m = Clearance7.measure(g, c);
                if (!m.ok()) {
                    stuck.add(sh.id + " " + c.kind() + " " + m + " blocked by " + blocker(g, c));
                    byKind.merge(c.kind().name(), 1, Integer::sum);
                }
            }
        }
        System.out.println(String.format(Locale.ROOT, "CLEARANCE7 real templates: %d templates (%d files not found), %d doorways, %d corridor strips, %d chambers, %d stair head-room columns cut; %d blocks removed, %d left standing, %d roof blocks, %d light blocks; %d transitions below 7x7 %s",
                templates, missing, doors, strips, chambers, stairs, cleared, kept, capped, lit, stuck.size(), byKind));
        System.out.println("CLEARANCE7 Way Onward seals left exactly as they are: " + seals);
        assertTrue(seals >= 3, "the Way Onward templates with a seal must be found: " + seals);
        System.out.println("CLEARANCE7 stairs: " + stairSteps + " stair blocks to stand on in the stairwells, " + stairShort + " of them had less than 7 free blocks above before the cut");
        System.out.println("CLEARANCE7 roof blocks by kind " + capsBy + ": " + capsList);
        for (String s : stuck) System.out.println("CLEARANCE7 stuck: " + s);
        assertTrue(templates > 100, "the real templates must be found (" + templates + ")");
        // The one known exception: a loot chest on a shelf inside the apron of a door of the fungus farm (a chest that holds loot is never cut away).
        for (String x : stuck) assertTrue(x.startsWith("nusmp:mega/hall/fungal_underdark_fungus_farm DOORWAY") && x.contains("minecraft:chest"), "a transition stays below 7x7 after the cuts: " + x);
        assertTrue(stuck.size() <= 1, "more than the one known exception: " + stuck);
    }

    @Test
    void theCutsOfARotatedPieceAreTheCutsOfTheTemplateTurned() throws IOException {
        CatalogSource cat = catalog();
        if (cat.isEmpty()) return;
        for (Shape sh : cat.allShapes()) {
            if (sh.isCap()) continue;
            List<Clearance7.Cut> base = Clearance7.cuts(turned(sh), allDoors(sh));
            for (int r = 1; r < 4; r++) {
                Placed p = new Placed(0, sh, "nusmp:mega_interior", r, new Vec(100, 56, -200), 0, -1, -1, -1, false);
                List<Clearance7.Cut> cuts = Clearance7.cuts(p, allDoors(sh));
                assertEquals(base.size(), cuts.size(), sh.id + " r" + r);
                for (int i = 0; i < cuts.size(); i++) {
                    Box a = base.get(i).box(), b = cuts.get(i).box();
                    int[] sa = {a.sizeX(), a.sizeZ()}, sb = {b.sizeX(), b.sizeZ()};
                    assertEquals(a.sizeY(), b.sizeY(), sh.id);
                    if ((r & 1) == 0) assertEquals(sa[0] + "x" + sa[1], sb[0] + "x" + sb[1], sh.id + " r" + r);
                    else assertEquals(sa[0] + "x" + sa[1], sb[1] + "x" + sb[0], sh.id + " r" + r);
                    assertTrue(p.box.contains(new Vec(b.minX(), b.minY(), b.minZ())) && p.box.contains(new Vec(b.maxX(), b.maxY(), b.maxZ())), sh.id + " r" + r + ": the cut leaves the piece");
                }
            }
        }
    }

    @Test
    void theCutsOnlyGrowTheOpeningNeverShrinkIt() throws IOException {
        // the doorway the template ships (3 wide, 4 high) must be a subset of the cut: nothing that was open becomes closed
        CatalogSource cat = catalog();
        if (cat.isEmpty()) return;
        for (Shape sh : cat.allShapes()) {
            if (sh.isCap()) continue;
            Placed p = turned(sh);
            for (Clearance7.Cut c : Clearance7.cuts(p, allDoors(sh))) {
                if (c.kind() != Clearance7.Kind.DOORWAY) continue;
                for (Socket s : sh.sockets) {
                    if (!c.box().contains(s.cell())) continue;
                    assertTrue(c.box().contains(s.cell().add(0, 3, 0)), sh.id + ": the cut does not cover the old 4-high doorway");
                    assertTrue(c.box().contains(s.cell().step(s.facing().cw(), 1)) && c.box().contains(s.cell().step(s.facing().ccw(), 1)), sh.id + ": the cut does not cover the old 3-wide doorway");
                    assertTrue(c.box().contains(s.cell().step(s.facing().opposite(), 1)), sh.id + ": the cut does not reach through both wall layers");
                }
            }
        }
    }
}
