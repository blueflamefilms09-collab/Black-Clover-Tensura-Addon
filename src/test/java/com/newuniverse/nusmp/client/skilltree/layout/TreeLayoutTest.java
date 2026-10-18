package com.newuniverse.nusmp.client.skilltree.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: this class turns a data file's grid into pixels and answers "which node is under the mouse", so its edges are where a screen goes wrong: the last pixel of a box, a pan that
 * leaves a gap, a zoom that drifts away from the cursor, a parent line that ends inside the wrong node. Each is pinned on the shape of the Fire Dragon Slayer tree (the owner's
 * example) and on the extremes the schema allows (one node, a 13 x 10 grid).
 */
class TreeLayoutTest {
    private static final SkillRoute FIRE = SampleRoutes.fireDragon();
    private static final TreeLayout L = TreeLayout.of(FIRE.nodes());

    private static int index(String id) {
        for (int i = 0; i < FIRE.nodes().size(); i++) if (FIRE.nodes().get(i).id().equals(id)) return i;
        throw new AssertionError("no node " + id);
    }

    @Test
    void contentSizeAndNodeCentresFollowTheGrid() {
        assertEquals(24 + 22 + 6 * 38, L.contentWidth());                 // 7 columns
        assertEquals(24 + 22 + 6 * 32, L.contentHeight());                // 7 rows
        assertEquals(23, L.centerX(index("iron_fist")));                  // PAD 12 + half node 11, column 0
        assertEquals(23 + 3 * 32, L.centerY(index("iron_fist")));
        assertEquals(23 + 6 * 38, L.centerX(index("crimson_lotus")));
        assertEquals(23, L.centerY(index("brilliant_flame")));            // row 0
    }

    @Test
    void layoutIsRelativeToTheTreesOwnBoundingBox() {
        SkillRoute shifted = SampleRoutes.route("nusmp:shifted", SampleRoutes.node("a", 5, 4, 1, 1), SampleRoutes.node("b", 7, 4, 1, 1, "a", 1));
        TreeLayout t = TreeLayout.of(shifted.nodes());
        assertEquals(23, t.centerX(0), "the leftmost node starts at the margin, not at column 5");
        assertEquals(23, t.centerY(0));
        assertEquals(23 + 2 * 38, t.centerX(1));
    }

    @Test
    void emptyTreeHasOnlyItsMargin() {
        TreeLayout e = TreeLayout.of(List.of());
        assertEquals(0, e.size());
        assertEquals(24, e.contentWidth());
        assertEquals(-1, e.hit(5, 5, 1f, 0f, 0f, 2f));
        assertTrue(e.edges(List.of()).isEmpty());
    }

    @Test
    void hitTestingIsExactAtTheEdgesOfABox() {
        int i = index("roar");
        int cx = L.centerX(i), cy = L.centerY(i);
        assertEquals(i, L.hit(cx - 11, cy - 11, 1f, 0f, 0f, 0f), "top-left pixel is inside");
        assertEquals(i, L.hit(cx + 10.99, cy + 10.99, 1f, 0f, 0f, 0f), "last pixel is inside");
        assertEquals(-1, L.hit(cx + 11, cy, 1f, 0f, 0f, 0f), "right edge is outside (exclusive)");
        assertEquals(-1, L.hit(cx, cy + 11, 1f, 0f, 0f, 0f), "bottom edge is outside (exclusive)");
        assertEquals(-1, L.hit(cx - 11.01, cy, 1f, 0f, 0f, 0f), "just left of the box is outside");
        assertEquals(i, L.hit(cx + 12.5, cy, 1f, 0f, 0f, 2f), "slop widens the box");
        assertEquals(-1, L.hit(cx + 13.5, cy, 1f, 0f, 0f, 2f), "but only by the slop");
    }

    @Test
    void hitTestingFollowsPanAndZoom() {
        int i = index("claw");
        float zoom = 0.5f, panX = 30f, panY = -10f;
        double sx = panX + L.centerX(i) * zoom, sy = panY + L.centerY(i) * zoom;
        assertEquals(i, L.hit(sx, sy, zoom, panX, panY, 0f));
        assertEquals(i, L.hit(sx + 5.4, sy - 5.4, zoom, panX, panY, 0f), "half the box at zoom 0.5 is 5.5 px");
        assertEquals(-1, L.hit(sx + 5.6, sy, zoom, panX, panY, 0f));
        assertTrue(L.hit(sx, sy, 1f, panX, panY, 0f) != i, "the same point at another zoom is not the same node");
    }

    @Test
    void overlappingBoxesGiveTheLaterNode() {
        SkillRoute r = SampleRoutes.route("nusmp:overlap", SampleRoutes.node("a", 0, 0, 1, 1), SampleRoutes.node("b", 0, 0, 1, 1));
        TreeLayout t = TreeLayout.of(r.nodes());
        assertEquals(1, t.hit(t.centerX(0), t.centerY(0), 1f, 0f, 0f, 0f));
    }

    @Test
    void fitZoomStaysInItsRange() {
        TreeLayout wide = TreeLayout.of(SampleRoutes.wide().nodes());
        assertEquals(TreeLayout.MIN_ZOOM, wide.fitZoom(190, 148, TreeLayout.MIN_ZOOM, 1f), 1e-6f, "a 13 x 10 tree in a small viewport bottoms out at the minimum");
        TreeLayout one = TreeLayout.of(SampleRoutes.single().nodes());
        assertEquals(1f, one.fitZoom(190, 148, TreeLayout.MIN_ZOOM, 1f), 1e-6f, "a tiny tree is not blown up past the limit");
        float z = L.fitZoom(190, 148, TreeLayout.MIN_ZOOM, 1f);
        assertTrue(L.contentWidth() * z <= 190.01f && L.contentHeight() * z <= 148.01f, "the fitted tree fits the viewport");
        assertEquals(TreeLayout.MAX_ZOOM, TreeLayout.clampZoom(9f));
        assertEquals(TreeLayout.MIN_ZOOM, TreeLayout.clampZoom(0.01f));
    }

    @Test
    void theFirstViewShowsTheWholeTreeWhenItFitsAndTheRootsWhenItDoesNot() {
        TreeLayout one = TreeLayout.of(SampleRoutes.single().nodes());
        float[] v = one.initialView(190, 148);
        assertEquals(1f, v[0], 1e-6f);
        assertEquals((190 - one.contentWidth()) / 2f, v[1], 1e-4f, "a small tree is centred");
        assertEquals((148 - one.contentHeight()) / 2f, v[2], 1e-4f);
        float[] big = L.initialView(190, 148);                            // 274 x 238 content does not fit 190 x 148 at 0.6 (164 x 143 does in width, not in height)
        assertTrue(big[0] >= 0.6f - 1e-6f && big[0] <= 1f, "the zoom stays readable");
        float[] huge = TreeLayout.of(SampleRoutes.wide().nodes()).initialView(190, 148);
        assertEquals(0.6f, huge[0], 1e-6f);
        assertEquals(0f, huge[1], "the roots are at the left edge");
        float rootY = TreeLayout.of(SampleRoutes.wide().nodes()).centerY(0);
        assertEquals(Math.max(148 - TreeLayout.of(SampleRoutes.wide().nodes()).contentHeight() * 0.6f, Math.min(0f, 148 / 2f - rootY * 0.6f)), huge[2], 1e-3f, "first root centred, then clamped");
    }

    @Test
    void fitViewShowsEverythingDownToTheMinimumZoom() {
        float[] v = L.fitView(190, 148);
        assertTrue(L.contentWidth() * v[0] <= 190.01f && L.contentHeight() * v[0] <= 148.01f);
        float[] w = TreeLayout.of(SampleRoutes.wide().nodes()).fitView(190, 148);
        assertEquals(TreeLayout.MIN_ZOOM, w[0], 1e-6f);
    }

    @Test
    void panIsClampedPerAxis() {
        float z = 1f;                                                     // content 274 x 238
        assertEquals(0f, L.clampPanX(50f, z, 190), "dragging right past the left edge stops at the edge");
        assertEquals(190 - 274f, L.clampPanX(-500f, z, 190), "dragging left past the right edge stops at the edge");
        assertEquals(-40f, L.clampPanX(-40f, z, 190), "a legal pan is left alone");
        assertEquals((400 - 274f) / 2f, L.clampPanX(7f, z, 400), "a tree narrower than the viewport is centred whatever the pan");
        assertEquals(0f, L.clampPanY(1f, z, 148));
        assertEquals(148 - 238f, L.clampPanY(-999f, z, 148));
    }

    @Test
    void zoomKeepsThePointUnderTheCursorFixed() {
        float pan = -37f, zOld = 0.8f, zNew = 1.5f, anchor = 61f;
        float content = (anchor - pan) / zOld;
        float panNew = TreeLayout.panForZoomAt(pan, zOld, zNew, anchor);
        assertEquals(anchor, panNew + content * zNew, 1e-3f);
        assertEquals(pan, TreeLayout.panForZoomAt(pan, 1f, 1f, anchor), 1e-6f, "no change of zoom, no change of pan");
    }

    @Test
    void everyParentLinkBecomesALineThatStartsAndEndsOnTheRightBoxes() {
        List<TreeLayout.Edge> edges = L.edges(FIRE.nodes());
        assertEquals(9, edges.size());                                    // 0 + 1 + 1 + 1 + 1 + 1 + 1 + 2 + 1
        for (TreeLayout.Edge e : edges) {
            int[] p = e.points();
            assertTrue(p.length >= 4 && p.length % 2 == 0);
            Rect from = L.box(e.parent()).inset(-1), to = L.box(e.child()).inset(-1);
            assertTrue(from.contains(p[0], p[1]), "starts on the parent box: " + e);
            assertTrue(to.contains(p[p.length - 2], p[p.length - 1]), "ends on the child box: " + e);
            for (int i = 0; i + 3 < p.length; i += 2) assertTrue(p[i] == p[i + 2] || p[i + 1] == p[i + 3], "axis-aligned piece");
            assertTrue(Polyline.length(p) > 0);
        }
    }

    @Test
    void theSecretArtGetsOneLineFromEachBranch() {
        int lotus = index("crimson_lotus");
        int fromRoar = 0, fromClaw = 0;
        for (TreeLayout.Edge e : L.edges(FIRE.nodes())) {
            if (e.child() != lotus) continue;
            if (e.parent() == index("roar")) { fromRoar++; assertEquals(3, e.requiredLevel()); }
            if (e.parent() == index("claw")) { fromClaw++; assertEquals(3, e.requiredLevel()); }
        }
        assertEquals(1, fromRoar);
        assertEquals(1, fromClaw);
    }

    @Test
    void noLineCrossesAnUnrelatedNodeInTheSampleShape() {
        for (TreeLayout.Edge e : L.edges(FIRE.nodes())) {
            for (int i = 0; i < FIRE.nodes().size(); i++) {
                if (i == e.parent() || i == e.child()) continue;
                Rect b = L.box(i);
                assertTrue(LineRouter.clear(e.points(), List.of(new int[]{b.x(), b.y(), b.right(), b.bottom()})), "edge " + e.parent() + "->" + e.child() + " crosses node " + i);
            }
        }
    }

    @Test
    void unknownParentsAndSelfParentsAreSkippedNotCrashed() {
        SkillRoute bad = SampleRoutes.route("nusmp:bad", SampleRoutes.node("a", 0, 0, 1, 1, "ghost", 1, "a", 1), SampleRoutes.node("b", 1, 0, 1, 1, "a", 1));
        TreeLayout t = TreeLayout.of(bad.nodes());
        List<TreeLayout.Edge> edges = t.edges(bad.nodes());
        assertEquals(1, edges.size());
        assertEquals(0, edges.get(0).parent());
        assertEquals(1, edges.get(0).child());
    }

    @Test
    void extremeSchemaGridStillLaysOut() {
        SkillRoute w = SampleRoutes.wide();
        TreeLayout t = TreeLayout.of(w.nodes());
        assertEquals(24 + 22 + 12 * 38, t.contentWidth());
        assertEquals(24 + 22 + 9 * 32, t.contentHeight());
        assertEquals(4, t.edges(w.nodes()).size());                       // b<-a, c<-a, d<-b, d<-c
        for (TreeLayout.Edge e : t.edges(w.nodes())) assertTrue(Polyline.length(e.points()) > 0);
    }
}
