package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The lane of the Blood-Red Lunge: it goes as far as the ground carries it, never through a block and never past its cap. */
class MonarchLungeTest {
    private static final double NAN = Double.NaN;

    /** A flat floor at y = 64 with a wall from x = wall on (no room to stand) and a pit from pitFrom to pitTo. */
    private static MonarchLunge.Ground flat(double wall, double pitFrom, double pitTo) {
        return (x, y, z) -> x >= wall || (x >= pitFrom && x < pitTo) ? NAN : 64.0;
    }

    @Test
    void theLaneStopsAtTheFirstWallAndNeverPassesIt() {
        List<double[]> path = MonarchLunge.path(0, 64, 0, 1, 0, 32, flat(10.0, 1e9, 1e9));
        double[] last = path.get(path.size() - 1);
        assertTrue(last[0] < 10.0, "the last sample is before the wall: " + last[0]);
        assertTrue(last[0] >= 9.4, "and goes right up to it: " + last[0]);
        assertEquals(last[0], MonarchLunge.length(path), 1e-9);
        for (double[] p : path) assertTrue(p[0] < 10.0, "no sample inside or beyond the wall");
        // the dash that follows moves a step at a time and every point it reaches is on the lane
        double travelled = 0;
        while (travelled < MonarchLunge.length(path)) {
            travelled = Math.min(MonarchLunge.length(path), travelled + MonarchRules.LUNGE_SPEED);
            double[] at = MonarchLunge.at(path, travelled);
            assertTrue(at[0] < 10.0, "it never goes through a block: " + at[0]);
            assertEquals(64.0, at[1], 1e-9);
            assertEquals(0.0, at[2], 1e-9);
        }
        assertEquals(last[0], travelled, 1e-9, "and stops at the end of the lane");
    }

    @Test
    void theLaneStopsAtAPitAndALaneWithNoWayForwardIsJustTheStart() {
        List<double[]> path = MonarchLunge.path(0, 64, 0, 0, 1, 32, flat(1e9, 6.0, 9.0));
        // heading +z the pit (a band in x) is never met: the lane runs its whole length
        assertEquals(32.0, MonarchLunge.length(path), 1e-9);
        List<double[]> toPit = MonarchLunge.path(0, 64, 0, 1, 0, 32, flat(1e9, 6.0, 9.0));
        assertTrue(MonarchLunge.length(toPit) < 6.0 && MonarchLunge.length(toPit) >= 5.4, "it stops at the edge of the pit: " + MonarchLunge.length(toPit));
        List<double[]> blocked = MonarchLunge.path(0, 64, 0, 1, 0, 32, flat(0.4, 1e9, 1e9));
        assertEquals(1, blocked.size());
        assertEquals(0.0, MonarchLunge.length(blocked), 1e-12);
        assertTrue(MonarchLunge.length(blocked) < MonarchRules.LUNGE_MIN_RUN, "too short a run: the move is given up");
        double[] only = MonarchLunge.at(blocked, 5.0);
        assertEquals(0.0, only[0], 1e-12);
        assertEquals(0.0, MonarchLunge.path(0, 64, 0, 0, 0, 32, flat(1e9, 1e9, 1e9)).get(0)[0], 1e-12);
        assertEquals(1, MonarchLunge.path(0, 64, 0, 0, 0, 32, flat(1e9, 1e9, 1e9)).size(), "no heading, no lane");
    }

    @Test
    void theLaneIsNeverLongerThanTheCapOrWhatWasAsked() {
        MonarchLunge.Ground open = (x, y, z) -> 64.0;
        for (double ask : new double[]{0.0, 0.4, 3.0, 12.0, 32.0, 50.0, 1000.0}) {
            List<double[]> path = MonarchLunge.path(3, 64, -7, 0.6, 0.8, ask, open);
            double len = MonarchLunge.length(path);
            assertTrue(len <= Math.max(0.0, ask) + 1e-9, "no longer than asked: " + len + " for " + ask);
            assertTrue(path.size() <= 1 + (int) Math.floor(ask / MonarchLunge.SPACING + 1e-9), "and no more samples than that");
        }
        double cap = MonarchRules.lungeLength(1e6);
        assertEquals(32.0, MonarchLunge.length(MonarchLunge.path(0, 64, 0, 1, 1, cap, open)), 1e-9, "the dash is capped at 32 blocks");
        // the samples lie on the line, in order, half a block apart
        List<double[]> diag = MonarchLunge.path(1, 64, 2, 3, 4, 10, open);
        for (int i = 1; i < diag.size(); i++) {
            assertEquals(MonarchLunge.SPACING, Math.hypot(diag.get(i)[0] - diag.get(i - 1)[0], diag.get(i)[2] - diag.get(i - 1)[2]), 1e-9);
            assertEquals(0.6, (diag.get(i)[0] - 1) / (MonarchLunge.SPACING * i), 1e-9);
            assertEquals(0.8, (diag.get(i)[2] - 2) / (MonarchLunge.SPACING * i), 1e-9);
        }
        assertTrue(MonarchLunge.SPACING < 1.0, "less than the width of the body: nothing thin lies between two samples");
    }

    @Test
    void stairsAreWalkedAndThePointsBetweenSamplesAreInterpolated() {
        // the floor drops a block every three blocks of x, like the steps of the dais
        MonarchLunge.Ground stairs = (x, y, z) -> 64.0 - Math.floor(x / 3.0);
        List<double[]> path = MonarchLunge.path(0, 64, 0, 1, 0, 12, stairs);
        assertEquals(12.0, MonarchLunge.length(path), 1e-9);
        assertEquals(64.0, path.get(0)[1], 1e-9);
        assertEquals(60.0, path.get(path.size() - 1)[1], 1e-9);
        double[] mid = MonarchLunge.at(path, 1.25);
        assertEquals(1.25, mid[0], 1e-9);
        assertEquals(64.0, mid[1], 1e-9);
        double[] edge = MonarchLunge.at(path, 2.75);
        assertEquals(2.75, edge[0], 1e-9);
        assertEquals(63.5, edge[1], 1e-9, "a step down is eased over half a block");
        double[] before = MonarchLunge.at(path, -5.0), after = MonarchLunge.at(path, 99.0);
        assertEquals(0.0, before[0], 1e-12, "before the start it is the start");
        assertEquals(12.0, after[0], 1e-9, "past the end it is the end");
        // a ground that refuses to climb: a wall of height two stops it, a step of one does not (the ground decides, the plan only obeys)
        MonarchLunge.Ground ledge = (x, y, z) -> x >= 5 ? (y + 1.0 > 65.0 ? NAN : y + 1.0) : y;
        List<double[]> stopped = MonarchLunge.path(0, 64, 0, 1, 0, 20, ledge);
        assertEquals(65.0, stopped.get(stopped.size() - 1)[1], 1e-9);
        assertTrue(MonarchLunge.length(stopped) < 6.0, "the second step up is a wall: " + MonarchLunge.length(stopped));
    }

    @Test
    void theLaneIsDrawnWithItsTwoEdgesAndItsMiddleAndNeverWithTooManyDots() {
        MonarchLunge.Ground open = (x, y, z) -> 64.0;
        List<double[]> path = MonarchLunge.path(0, 64, 0, 0, 1, 32, open);
        List<double[]> rows = MonarchLunge.rows(path, 0, 1, MonarchRules.LUNGE_HALF, 1.5, 20);
        assertEquals(60, rows.size(), "three rows of 20");
        for (int i = 0; i < rows.size(); i++) {
            double[] p = rows.get(i);
            double side = i < 20 ? 1.0 : i < 40 ? -1.0 : 0.0;                 // heading +z: one edge on +x, the other on -x, the middle on the line
            assertEquals(side * MonarchRules.LUNGE_HALF, p[0], 1e-9, "an edge on each side and the middle");
            assertTrue(p[2] >= -1e-9 && p[2] <= 32 + 1e-9);
            assertEquals(64.0, p[1], 1e-9);
        }
        assertEquals(0.0, rows.get(0)[2], 1e-9);
        assertEquals(32.0, rows.get(19)[2], 1e-9, "the strip is as long as the lane");
        // a short lane gets fewer dots, a lane of no length none
        assertTrue(MonarchLunge.rows(MonarchLunge.path(0, 64, 0, 0, 1, 6, open), 0, 1, 1.6, 1.5, 20).size() < 60);
        assertTrue(MonarchLunge.rows(MonarchLunge.path(0, 64, 0, 0, 1, 0, open), 0, 1, 1.6, 1.5, 20).isEmpty());
        assertTrue(MonarchLunge.rows(path, 0, 0, 1.6, 1.5, 20).isEmpty(), "no heading, nothing to draw");
    }

    @Test
    void whatTheDashHitsIsWhatTheMarkShows() {
        // a body stands in the strip exactly when the distance to the line from the start to the stop is within the half width
        MonarchLunge.Ground open = (x, y, z) -> 64.0;
        List<double[]> path = MonarchLunge.path(0, 64, 0, 1, 0, 20, open);
        double[] end = MonarchLunge.at(path, MonarchLunge.length(path));
        double half = MonarchRules.LUNGE_HALF + 0.3;
        assertTrue(MonarchShapes.distToSegment(10, 1.8, 0, 0, end[0], end[2]) <= half, "inside the lane");
        assertFalse(MonarchShapes.distToSegment(10, 2.3, 0, 0, end[0], end[2]) <= half, "beside the lane");
        assertFalse(MonarchShapes.distToSegment(end[0] + 3, 0, 0, 0, end[0], end[2]) <= half, "beyond where it stops");
        // swept in steps of two blocks, no body in the lane is skipped
        for (double z = -1.9; z <= 1.9; z += 0.1) {
            for (double x = 0; x <= 20; x += 0.37) {
                boolean hit = false;
                double prev = 0;
                for (double d = MonarchRules.LUNGE_SPEED; ; d = Math.min(20, d + MonarchRules.LUNGE_SPEED)) {
                    if (MonarchShapes.distToSegment(x, z, prev, 0, d, 0) <= half) hit = true;
                    prev = d;
                    if (d >= 20) break;
                }
                assertTrue(hit, "a body at (" + x + ", " + z + ") in the lane is struck by the dash");
            }
        }
    }
}
