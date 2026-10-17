package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of Lucifero's room-sized gravity: lanes with gaps, a well that cannot outrun walking, kneels under the law, pockets that can be reached. */
class WeightMathTest {
    @Test
    void theCombAlwaysLeavesAGapWideEnoughToStandIn() {
        assertTrue(WeightMath.laneGap() >= 3.0, "gap " + WeightMath.laneGap());
        assertEquals(3, WeightMath.laneCount(20));
        assertEquals(3, WeightMath.laneCount(60));
        assertEquals(5, WeightMath.laneCount(80));
        assertEquals(5, WeightMath.laneCount(100));
        for (int n : new int[]{1, 3, 5}) {
            double[] o = WeightMath.laneOffsets(n);
            assertEquals(n, o.length);
            double sum = 0;
            for (double v : o) sum += v;
            assertEquals(0.0, sum, 1e-9, "centred on the aim line");
            for (int i = 1; i < o.length; i++) assertEquals(WeightMath.LANE_SPACING, o[i] - o[i - 1], 1e-9);
        }
    }

    @Test
    void aPointInALaneIsHitAndOneInAGapIsNot() {
        // three lanes at -7.2, 0, +7.2 with a width of 3.2
        assertTrue(WeightMath.inComb(0, 0, 3), "the middle lane on the aim line");
        assertTrue(WeightMath.inComb(7.2 + 1.5, 5, 3), "the right lane's edge");
        assertFalse(WeightMath.inComb(7.2 + 1.7, 5, 3), "just outside it");
        assertFalse(WeightMath.inComb(3.6, 0, 3), "the middle of the gap");
        assertFalse(WeightMath.inComb(0, WeightMath.LANE_LENGTH / 2 + 0.1, 3), "beyond the end of the lanes");
        assertTrue(WeightMath.inComb(0, WeightMath.LANE_LENGTH / 2 - 0.1, 3));
        assertFalse(WeightMath.inComb(14.4, 0, 3), "no fourth lane in a three-lane comb");
        assertTrue(WeightMath.inComb(14.4, 0, 5), "but in a five-lane one");
        // sweep the whole floor: a good share of it is safe even at five lanes
        int hit = 0, total = 0;
        for (double lat = -20; lat <= 20; lat += 0.25)
            for (double al = -12; al <= 12; al += 0.5) {
                total++;
                if (WeightMath.inComb(lat, al, 5)) hit++;
            }
        assertTrue(hit < total * 0.5, "less than half of the strip is lane: " + hit + "/" + total);
    }

    @Test
    void theHorizonPullNeverBeatsWalkingAndIsZeroOutOfReach() {
        double max = 0;
        for (double d = 0; d <= 40; d += 0.25) {
            double p = WeightMath.pullSpeed(d);
            assertTrue(p >= 0 && p < WeightMath.WALK, "at " + d + " blocks the pull " + p + " is under walking");
            max = Math.max(max, p);
        }
        assertEquals(0.0, WeightMath.pullSpeed(WeightMath.PULL_RADIUS + 0.1), 1e-12);
        assertEquals(0.0, WeightMath.pullSpeed(1.9), 1e-12, "nothing shakes you inside two blocks");
        assertTrue(WeightMath.pullSpeed(3) > WeightMath.pullSpeed(20), "stronger near the core");
        assertTrue(max <= WeightMath.PULL_MAX + 1e-12);
        assertTrue(max < WeightMath.WALK * 0.7, "and a walker slowed to 70% (Slowness II) still out-walks it: " + max);
    }

    @Test
    void theCoreIsASingleNineBlockCircle() {
        assertEquals(9.0, WeightMath.CORE_RADIUS, 1e-9);
        assertTrue(WeightMath.inCore(8.9) && !WeightMath.inCore(9.1));
    }

    @Test
    void anyoneAtTheKingsFeetCanWalkOutOfTheCoreBeforeItCollapses() {
        int windUp = LuciferoTable.byId(LuciferoTable.EVENT_HORIZON).windUpTicks();
        for (int power : new int[]{80, 100}) {
            int ticks = (int) Math.round(windUp * com.newuniverse.nusmp.entity.zogratis.PowerCurve.tuning(power).windUpMultiplier());
            double out = WeightMath.escapeDistance(ticks);
            assertTrue(out > WeightMath.CORE_RADIUS, "at " + power + "% a walker from the very middle reaches " + out + " of " + WeightMath.CORE_RADIUS + " blocks");
        }
        assertEquals(0.0, WeightMath.escapeDistance(0), 1e-12);
        assertTrue(WeightMath.escapeDistance(10) < WeightMath.escapeDistance(11), "more time, farther");
    }

    @Test
    void aPocketCanBeReachedOnFootDuringTheKneelsWindUp() {
        int windUp = LuciferoTable.byId(LuciferoTable.KNEEL_BEFORE_THE_KING).windUpTicks();
        for (int power : new int[]{60, 80, 100}) {
            int ticks = (int) Math.round(windUp * com.newuniverse.nusmp.entity.zogratis.PowerCurve.tuning(power).windUpMultiplier());
            double reach = (ticks - 8) * WeightMath.WALK;             // a few ticks to read the rings
            assertTrue(reach >= PocketMath.FAR, "at " + power + "% the farthest pocket (" + PocketMath.FAR + ") is " + reach + " walkable blocks away");
        }
    }

    @Test
    void flatteningLastsAboutEightSecondsAndGrowsAStepAtATime() {
        assertEquals(140, WeightMath.flattenTicks(20));
        assertEquals(140, WeightMath.flattenTicks(40));
        assertEquals(150, WeightMath.flattenTicks(60));
        assertEquals(160, WeightMath.flattenTicks(80));
        assertEquals(170, WeightMath.flattenTicks(100));
    }

    @Test
    void aKneelNeverOutlastsTheBalanceLaw() {
        assertEquals(50, WeightMath.kneelTicks(2.5, 60));
        assertEquals(60, WeightMath.kneelTicks(9.0, 60), "the law's three seconds");
        assertEquals(60, WeightMath.kneelTicks(3.0, 600), "the law's three seconds even if more is allowed");
        assertEquals(15, WeightMath.kneelTicks(2.5, 15), "the diminishing returns hand back less");
        assertEquals(5, WeightMath.kneelTicks(2.5, 0), "never under five ticks");
        assertEquals(5, WeightMath.kneelTicks(0.0, 60), "a quarter second at least: 5 ticks");
        for (double s = 0; s <= 12; s += 0.5) for (int allowed = 0; allowed <= 100; allowed += 10) assertTrue(WeightMath.kneelTicks(s, allowed) <= WeightMath.LAW_CAP);
    }

    @Test
    void theSixArmsCoverTheCircleEvenly() {
        double[] a = WeightMath.armAngles(6, 0.3);
        assertEquals(6, a.length);
        assertEquals(0.3, a[0], 1e-9);
        for (int i = 1; i < a.length; i++) assertEquals(Math.PI / 3, a[i] - a[i - 1], 1e-9);
        assertTrue(WeightMath.inArm(0.3, 0.3, 5.0));
        assertTrue(WeightMath.inArm(0.3 + Math.toRadians(23), 0.3, 5.8));
        assertFalse(WeightMath.inArm(0.3 + Math.toRadians(25), 0.3, 5.0), "outside the arm's wedge");
        assertFalse(WeightMath.inArm(0.3, 0.3, 5.9), "beyond its reach");
        // the six wedges leave gaps between them: not every angle is struck by some arm
        int struck = 0;
        for (int deg = 0; deg < 360; deg++) {
            double ang = Math.toRadians(deg);
            for (double arm : a) if (WeightMath.inArm(ang, arm, 3)) { struck++; break; }
        }
        assertTrue(struck < 360 && struck > 200, "six limbs cover most but not all of the circle: " + struck);
    }

    @Test
    void pocketsAreReachableApartAndNotOnTheKing() {
        double[][] foes = {{10, 0}, {-8, 6}, {0, -12}, {14, 14}};
        for (long seed = 1; seed <= 400; seed++) {
            List<double[]> p = PocketMath.plan(foes, 0, 0, 4, WeightMath.POCKET_RADIUS, seed);
            assertTrue(p.size() <= 4, "never more than asked");
            assertTrue(p.size() >= 3, "seed " + seed + " placed " + p.size());
            for (int i = 0; i < p.size(); i++) {
                double[] a = p.get(i);
                assertTrue(Math.hypot(a[0], a[1]) >= PocketMath.MIN_FROM_BOSS + WeightMath.POCKET_RADIUS - 1e-9, "off the King");
                double[] f = foes[i % foes.length];
                // pocket i is laid for foe i, unless an earlier one could not be placed and shifted the numbering: still within reach of SOME foe
                double nearest = Double.MAX_VALUE;
                for (double[] g : foes) nearest = Math.min(nearest, Math.hypot(a[0] - g[0], a[1] - g[1]));
                assertTrue(nearest <= PocketMath.FAR + 1e-9, "a pocket is within " + PocketMath.FAR + " of some foe: " + nearest);
                for (int j = i + 1; j < p.size(); j++)
                    assertTrue(Math.hypot(a[0] - p.get(j)[0], a[1] - p.get(j)[1]) >= 2 * WeightMath.POCKET_RADIUS + 1.5 - 1e-9, "pockets apart");
            }
        }
    }

    @Test
    void pocketsAreSeededAndHandleNothing() {
        double[][] foes = {{10, 0}, {-8, 6}};
        List<double[]> a = PocketMath.plan(foes, 0, 0, 3, 2.4, 77), b = PocketMath.plan(foes, 0, 0, 3, 2.4, 77);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i)[0], b.get(i)[0], 1e-12);
            assertEquals(a.get(i)[1], b.get(i)[1], 1e-12);
        }
        assertTrue(PocketMath.plan(null, 0, 0, 3, 2.4, 1).isEmpty());
        assertTrue(PocketMath.plan(new double[0][], 0, 0, 3, 2.4, 1).isEmpty());
        assertTrue(PocketMath.plan(foes, 0, 0, 0, 2.4, 1).isEmpty());
        assertEquals(2, WeightMath.pocketCount(1));
        assertEquals(2, WeightMath.pocketCount(2));
        assertEquals(4, WeightMath.pocketCount(4));
        assertEquals(5, WeightMath.pocketCount(12));
        assertTrue(WeightMath.inPocket(1, 1, 0, 0) && !WeightMath.inPocket(2, 2, 0, 0));
    }
}
