package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the dungeon's transitions are 7 x 7 minimum ({@link Clearance7}, docs/labyrinth/clearance7.md) and the rebuilt rooms must keep that rule, on the files, for every door and every
 * lane: this takes each rebuilt template, treats every ordinary door as connected (the worst case: every cut is made), cuts it exactly as the generator does and measures every transition
 * with the rule. The corridors, bends, tees, crosses and links of the rebuilt set are drawn native to 7 x 7, so their lane cuts must remove NOTHING (the lane is empty in the file), and
 * every doorway of every rebuilt room must measure 7 x 7 after its cut, with at most the one exception the shipped rooms have (a loot chest on a shelf in the apron of a door of the
 * fungus farm). Uses the shipped sockets (the sockets of a rebuilt file are proved equal in {@link RoomsRebuiltFilesTest}).
 */
class RoomsRebuiltClearanceTest {
    private static Placed turned(Shape s) { return new Placed(0, s, "nusmp:mega_interior", 0, Vec.ZERO, 0, -1, -1, -1, false); }

    private static boolean[] allDoors(Shape s) {
        boolean[] c = new boolean[s.sockets.size()];
        for (int i = 0; i < c.length; i++) c[i] = Clearance7.isCuttableDoor(s.sockets.get(i)) && i != s.expansionSocket && !(s.sockets.get(i).sealed() && s.sockets.get(i).isDoor());
        return c;
    }

    @Test
    void everyDoorwayAndEveryLaneOfTheRebuiltRoomsIsSevenBySeven() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        assertTrue(!cat.isEmpty(), "the catalog of the shipped templates is needed");
        RoomVariants.Table table = RoomVariants.table();
        int files = 0, lanes = 0, doorways = 0;
        List<String> stuck = new ArrayList<>(), dirty = new ArrayList<>();
        for (Shape sh : cat.allShapes()) {
            int n = table.count(sh.id);
            if (n == 0) continue;
            for (int k = 1; k <= n; k++) {
                String id = RoomVariants.variantId(sh.id, k);
                ClearanceGrid g = TemplateGrids.load(id), lane = TemplateGrids.load(id);
                assertNotNull(g, id);
                assertEquals(sh.sx, g.sx, id);
                assertEquals(sh.sy, g.sy, id);
                assertEquals(sh.sz, g.sz, id);
                files++;
                List<Clearance7.Cut> cuts = new ArrayList<>(Clearance7.cuts(turned(sh), allDoors(sh)));
                for (Clearance7.Cut c : cuts) {
                    if (c.kind() == Clearance7.Kind.CORRIDOR || c.kind() == Clearance7.Kind.CHAMBER) {
                        lanes++;
                        ClearanceGrid.Stats st = lane.cut(c);
                        if (st.cleared() != 0 || st.kept() != 0 || st.capped() != 0) dirty.add(id + " " + c.kind() + " removes " + st.cleared() + ", keeps " + st.kept() + ", roofs " + st.capped());
                    }
                }
                g.cutAll(cuts);
                for (Clearance7.Cut c : cuts) {
                    if (c.kind() == Clearance7.Kind.DOORWAY) doorways++;
                    Clearance7.Clearance m = Clearance7.measure(g, c);
                    if (!m.ok()) stuck.add(id + " " + c.kind() + " " + m);
                }
            }
        }
        System.out.println("ROOMS REBUILT clearance: " + files + " rebuilt files, " + doorways + " doorways, " + lanes + " lane cuts; stuck " + stuck + "; lanes not empty " + dirty);
        assertTrue(files >= 400, "rebuilt files found: " + files);
        assertTrue(dirty.isEmpty(), "a lane or chamber of a rebuilt piece is not empty (the 7 x 7 cut would have to remove blocks): " + dirty);
        for (String s : stuck) assertTrue(s.startsWith("nusmp:mega_r/hall/fungal_underdark_fungus_farm_v"), "a transition of a rebuilt room is below 7 x 7 after the cuts: " + s);
        assertTrue(stuck.size() <= 4, "only the fungus farm's chest may stay in a doorway: " + stuck);
    }
}
