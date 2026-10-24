package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.star.StarMath.P;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Conjunction's choice of a star, the quartet's order, the oriented pane of Quartile Scutum, the lance of Hasta and the chain of the whip are geometry, and a wrong sign would only show in a game nobody can
 * play here. Pure maths, so each is pinned: the look pick, the loop order (a convex quadrilateral, never a bow-tie), the pane's box and the slab test, the lance clamp, the whip chain and its smooth path.
 */
class StarMathTest {
    private static double area(List<P> loop) {                     // signed area in the x-z plane (shoelace)
        double s = 0;
        for (int i = 0; i < loop.size(); i++) { P a = loop.get(i), b = loop.get((i + 1) % loop.size()); s += a.x() * b.z() - b.x() * a.z(); }
        return s / 2;
    }

    @Test
    void conjunctionPicksTheStarNearestTheLookWithinTwentyDegreesAndFortyEightBlocks() {
        P eye = new P(0, 1.6, 0), look = new P(0, 0, 1);
        List<P> stars = List.of(new P(10, 4, 10), new P(1, 4, 12), new P(0, 4, -9), new P(0, 4, 60));
        assertEquals(1, StarMath.pickByLook(stars, eye, look, 20, 48), "the one almost straight ahead");
        assertEquals(-1, StarMath.pickByLook(List.of(new P(0, 4, -9)), eye, look, 20, 48), "behind the mage");
        assertEquals(-1, StarMath.pickByLook(List.of(new P(0, 4, 60)), eye, look, 20, 48), "over the range");
        assertEquals(-1, StarMath.pickByLook(List.of(new P(9, 1.6, 10)), eye, look, 20, 48), "more than 20 degrees off");
        assertEquals(0, StarMath.pickByLook(List.of(new P(9, 1.6, 10)), eye, look, 60, 48));
        assertEquals(-1, StarMath.pickByLook(List.of(), eye, look, 20, 48));
        assertEquals(-1, StarMath.pickByLook(List.of(new P(0, 1.6, 0.2)), eye, look, 20, 48), "a star inside the mage is not a destination");
    }

    @Test
    void theNewestStarsWithinRangeAreTheQuartet() {
        List<P> all = new ArrayList<>();
        for (int i = 0; i < 7; i++) all.add(new P(i * 3, 5, 0));
        all.add(new P(500, 5, 0));
        List<P> q = StarMath.newest(all, new P(0, 0, 0), 4, 48);
        assertEquals(4, q.size());
        assertEquals(new P(18, 5, 0), q.get(3), "the newest star that is in range is last");
        assertEquals(new P(9, 5, 0), q.get(0));
        assertEquals(2, StarMath.newest(List.of(new P(1, 1, 1), new P(2, 2, 2)), new P(0, 0, 0), 4, 48).size(), "fewer than four in range");
    }

    @Test
    void theLoopOrderIsAConvexQuadrilateralWhateverTheInputOrder() {
        List<P> corners = List.of(new P(0, 5, 0), new P(6, 5.2, 0), new P(6, 4.9, 6), new P(0, 5.1, 6));
        for (int rot = 0; rot < 4; rot++) {
            List<P> shuffled = new ArrayList<>();
            for (int i = 0; i < 4; i++) shuffled.add(corners.get((i * 3 + rot) % 4));          // 0,3,2,1 rotated: a bow-tie order if used as is
            List<P> loop = StarMath.orderAround(shuffled);
            assertEquals(4, loop.size());
            assertEquals(36.0, Math.abs(area(loop)), 0.5, "the full 6 x 6 square, not a bow-tie");
        }
        List<P> vertical = List.of(new P(0, 2, 0), new P(0, 6, 0), new P(0.1, 6, 4), new P(-0.1, 2, 4));
        List<P> loop = StarMath.orderAround(List.of(vertical.get(0), vertical.get(2), vertical.get(1), vertical.get(3)));
        double yz = 0;
        for (int i = 0; i < 4; i++) { P a = loop.get(i), b = loop.get((i + 1) % 4); yz += a.y() * b.z() - b.y() * a.z(); }
        assertEquals(16.0, Math.abs(yz / 2), 0.5, "a pane standing upright is ordered in its own plane");
        assertEquals(5, StarMath.orderAround(List.of(new P(0, 0, 0), new P(1, 0, 3), new P(3, 0, 4), new P(5, 0, 2), new P(4, 0, -1))).size());
    }

    @Test
    void theScutumPaneFacesTheCasterAndIsAsBigAsTheStarsWithinTheClamp() {
        List<P> stars = List.of(new P(0, 2, 0), new P(4, 2, 0), new P(4, 5, 0), new P(0, 5, 0));
        StarMath.Pane pane = StarMath.pane(stars, new P(2, 2, 10), 6, 5);
        assertEquals(new P(2, 3.5, 0), pane.center());
        assertEquals(4.0, pane.width(), 1e-6);
        assertEquals(3.0, pane.height(), 1e-6);
        assertEquals(1.5, pane.thick());
        assertEquals(1.0, pane.normal().z(), 1e-6, "the normal points from the stars to the caster");
        assertTrue(StarMath.contains(pane, new P(2, 3.5, 0.5)));
        assertFalse(StarMath.contains(pane, new P(2, 3.5, 1.5)), "thicker than 1.5 blocks");
        assertFalse(StarMath.contains(pane, new P(5, 3.5, 0)), "wider than the pane");
        assertFalse(StarMath.contains(pane, new P(2, 6, 0)), "taller than the pane");
        StarMath.Pane huge = StarMath.pane(List.of(new P(0, 0, 0), new P(40, 0, 0), new P(40, 30, 0), new P(0, 30, 0)), new P(0, 0, 9), 6, 5);
        assertEquals(6.0, huge.width());
        assertEquals(5.0, huge.height());
        StarMath.Pane tiny = StarMath.pane(List.of(new P(0, 0, 0), new P(0.2, 0, 0), new P(0.2, 0.2, 0), new P(0, 0.2, 0)), new P(0, 0, 9), 6, 5);
        assertEquals(1.5, tiny.width());
        assertEquals(1.5, tiny.height());
        StarMath.Pane quint = StarMath.pane(List.of(new P(0, 0, 0), new P(40, 0, 0), new P(40, 30, 0)), new P(0, 0, 9), StarMath.PANE_MAX_W_QUINTILE, StarMath.PANE_MAX_H_QUINTILE);
        assertEquals(8.0, quint.width());
        assertEquals(6.0, quint.height());
        StarMath.Pane onTop = StarMath.pane(stars, new P(2, 3.5, 0), 6, 5);
        assertTrue(Double.isFinite(onTop.normalYaw()), "a caster standing in the pane's middle does not break the maths");
    }

    @Test
    void aSegmentCrossesThePaneOnlyThroughItsBox() {
        StarMath.Pane pane = new StarMath.Pane(new P(0, 3, 0), 0, 4, 3, 1.5);          // normal along +x, side along +z: it is a wall across the x axis
        assertTrue(StarMath.crosses(pane, new P(5, 3, 0), new P(-5, 3, 0)), "straight through");
        assertTrue(StarMath.crosses(pane, new P(5, 3, 1.9), new P(-5, 3, -1.9)), "slanted but through the face");
        assertFalse(StarMath.crosses(pane, new P(5, 3, 3), new P(-5, 3, 3)), "beside it");
        assertFalse(StarMath.crosses(pane, new P(5, 6, 0), new P(-5, 6, 0)), "over it");
        assertFalse(StarMath.crosses(pane, new P(5, 3, 0), new P(2, 3, 0)), "stops before it");
        assertTrue(StarMath.crosses(pane, new P(0.3, 3, 0), new P(0.5, 3, 0.1)), "a short step inside the box");
        assertFalse(StarMath.crosses(pane, new P(1, 3, 1), new P(1, 3, 1)), "a point outside");
        double[] b = pane.bounds();
        assertEquals(-0.75, b[0], 1e-9);
        assertEquals(1.5, b[3] - b[0], 1e-9, "the box is 1.5 thick in x");
        assertEquals(4.0, b[5] - b[2], 1e-9, "and 4 wide in z");
    }

    @Test
    void theLanceNeverExceedsThirtyTwoBlocksAndStartsAtTheCentroid() {
        P a = new P(0, 3, 0);
        assertEquals(new P(10, 3, 0), StarMath.lanceEnd(a, new P(10, 3, 0), 32));
        P end = StarMath.lanceEnd(a, new P(100, 3, 0), 32);
        assertEquals(32.0, end.dist(a), 1e-9);
        assertEquals(new P(0, 3, 0), StarMath.centroid(List.of(new P(-2, 3, -2), new P(2, 3, -2), new P(2, 3, 2), new P(-2, 3, 2))));
        assertEquals(0.0, StarMath.distToSegment(new P(1, 0, 0), new P(0, 0, 0), new P(2, 0, 0)), 1e-9);
        assertEquals(3.0, StarMath.distToSegment(new P(5, 0, 0), new P(0, 0, 0), new P(2, 0, 0)), 1e-9);
    }

    @Test
    void theWhipChainVisitsEveryStarFromTheNearestAndThePathIsSmoothAndThroughThem() {
        P hand = new P(0, 1.5, 0);
        List<P> stars = List.of(new P(9, 4, 0), new P(3, 3, 0), new P(6, 4, 3), new P(12, 5, 0));
        List<P> chain = StarMath.chain(hand, stars);
        assertEquals(4, chain.size());
        assertEquals(new P(3, 3, 0), chain.get(0), "the whip starts at the star nearest the hand");
        assertTrue(chain.containsAll(stars));
        List<P> path = StarMath.whipPath(hand, chain, 6);
        assertEquals(4 * 6 + 1, path.size());
        assertEquals(hand, path.get(0));
        assertEquals(chain.get(3), path.get(path.size() - 1));
        for (int i = 0; i < chain.size(); i++) assertEquals(0.0, path.get((i + 1) * 6 == path.size() ? path.size() - 1 : (i + 1) * 6).dist(chain.get(i)), 1e-9, "the path passes through star " + i);
        for (int i = 1; i < path.size(); i++) assertTrue(path.get(i).dist(path.get(i - 1)) < 4.0, "no jumps");
    }

    @Test
    void theAutomaticQuartetIsARingInFrontOfTheCasterAndTheStarHangsOverTheAimedPoint() {
        List<P> ring = StarMath.ring(new P(10, 4, 0), 0, 4, 3);
        assertEquals(4, ring.size());
        assertEquals(new P(13, 4, 0), ring.get(0), "the first point is straight ahead");
        for (P p : ring) assertEquals(3.0, p.dist(new P(10, 4, 0)), 1e-9);
        assertEquals(new P(1, 8, 2), StarMath.hang(new P(1, 5, 2), 3));
        assertEquals(0.0, StarMath.catmull(new P(0, 0, 0), new P(1, 1, 1), new P(2, 2, 2), new P(3, 3, 3), 0).dist(new P(1, 1, 1)), 1e-9);
        assertEquals(0.0, StarMath.catmull(new P(0, 0, 0), new P(1, 1, 1), new P(2, 2, 2), new P(3, 3, 3), 1).dist(new P(2, 2, 2)), 1e-9);
    }
}
