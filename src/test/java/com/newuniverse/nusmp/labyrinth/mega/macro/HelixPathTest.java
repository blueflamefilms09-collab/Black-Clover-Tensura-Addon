package com.newuniverse.nusmp.labyrinth.mega.macro;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@link HelixPath} is the table behind both roads (the Spire's stair, the Chasm's ramp), and the owner's rule is that those roads exist whatever the noise
 * does, so the table itself must be right: it must climb one half block per block of arc without ever skipping a height (a skipped half step is a 1 block
 * wall in a staircase), hold the heights of the landings for the length it was asked to, flag only the final stop as open, find the arc step of a column
 * from its angle and radius (the model's whole classification rests on that), and mean the same thing for a path that winds either way.
 */
public class HelixPathTest {
    static HelixPath simple(int dir, double start) {
        return new HelixPath(dir, start, 100, q -> 12.0 + q * 0.05, List.of(new HelixPath.Stop(0, 8.0 / 12.0, false), new HelixPath.Stop(40, 7.5 / 14.0, false), new HelixPath.Stop(100, 5.5 / 17.0, true)));
    }

    @Test
    public void everyHalfStepIsVisitedAndNeverSkipped() {
        HelixPath p = simple(1, 0.3);
        int prevQ = p.q(0);
        assertEquals(0, prevQ, "the path starts at the bottom");
        for (int i = 1; i < p.steps(); i++) {
            int q = p.q(i);
            assertTrue(q == prevQ || q == prevQ + 1, "q goes up by at most one half block per block of arc, at step " + i + ": " + prevQ + " -> " + q);
            prevQ = q;
        }
        assertEquals(100, prevQ, "the path ends at qEnd");
    }

    @Test
    public void landingsHoldTheirHeightAndOnlyTheFinalStopIsOpen() {
        HelixPath p = simple(1, 0.0);
        int[] first = p.landingSteps(0), mid = p.landingSteps(40), last = p.landingSteps(100);
        assertNotNull(first);
        assertNotNull(mid);
        assertNotNull(last);
        assertEquals(0, first[0], "the first landing is the start");
        assertTrue(first[1] - first[0] + 1 >= 8, "the first landing holds at least its 8 blocks of arc: " + (first[1] - first[0] + 1));
        assertTrue(mid[1] - mid[0] + 1 >= 7, "the middle landing too");
        for (int i = mid[0]; i <= mid[1]; i++) assertEquals(40, p.q(i));
        for (int i = 0; i < p.steps(); i++) assertEquals(p.q(i) == 100, p.isOpen(i), "only the last stop is railing free, step " + i);
        assertEquals(last[0], p.firstOpenStep());
        assertNull(p.landingSteps(41), "no landing at a height that is not a stop");
    }

    @Test
    public void theAngleGrowsAlongThePathAndTheTotalIsTheSumOfTheArcs() {
        HelixPath p = simple(1, 0.0);
        double sum = 0;
        for (int i = 0; i < p.steps(); i++) {
            if (i > 0) assertTrue(p.thetaAt(i) > p.thetaAt(i - 1), "theta increases at step " + i);
            sum += 1.0 / p.radius(i);
        }
        assertEquals(sum, p.thetaEnd, 1e-9, "one block of arc per row at the radius of the row");
    }

    @Test
    public void aColumnFindsItsArcStepFromAngleAndRadius() {
        for (int dir : new int[] {1, -1}) {
            HelixPath p = simple(dir, 1.234);
            int checked = 0;
            for (int i = 0; i < p.steps(); i += 3) {
                double t = p.thetaAt(i) + 0.5 / p.radius(i);
                double angle = p.startAngle + dir * t;
                double sweep = p.sweepOf(angle);
                boolean found = false;
                int from = 0;
                while (true) {
                    int enc = p.next(sweep, p.radius(i), from, 0.2);
                    if (enc < 0) break;
                    if ((enc >> 3) == i) {
                        found = true;
                        assertEquals((int) Math.floor(t / Geo.TWO_PI), enc & 7, "the lap of step " + i);
                    }
                    from = (enc & 7) + 1;
                }
                assertTrue(found, "step " + i + " (dir " + dir + ") is found from its own angle and radius");
                checked++;
            }
            assertTrue(checked > 20);
        }
    }

    @Test
    public void aColumnOutsideTheWalkwayOrBeyondTheEndFindsNothing() {
        HelixPath p = simple(1, 0.0);
        int i = p.steps() / 3;
        double angle = p.startAngle + p.thetaAt(i) + 0.5 / p.radius(i);
        assertTrue(p.next(p.sweepOf(angle), p.radius(i) + 5.0, 0, 1.0) < 0, "five blocks off the walkway is not on it");
        // past thetaEnd of the last lap there is nothing: the sweep of an angle just beyond the end of the final revolution
        double beyond = p.thetaEnd + 0.05;
        assertTrue(p.next(Geo.wrapPi(beyond) < 0 ? Geo.wrapPi(beyond) + Geo.TWO_PI : Geo.wrapPi(beyond), p.radius(p.steps() - 1), (int) Math.floor(beyond / Geo.TWO_PI), 0.5) < 0);
    }

    @Test
    public void sweepOfIsTheDistanceTravelledRoundTheAxisInTheFirstRevolution() {
        HelixPath up = simple(1, 0.5), down = simple(-1, 0.5);
        assertEquals(0.0, up.sweepOf(0.5), 1e-12);
        assertEquals(1.0, up.sweepOf(1.5), 1e-12);
        assertEquals(1.0, down.sweepOf(-0.5), 1e-12);
        assertEquals(Geo.TWO_PI - 0.25, up.sweepOf(0.25), 1e-9, "behind the start is nearly a whole turn along");
        assertFalse(up.sweepOf(7.0) < 0 || up.sweepOf(7.0) >= Geo.TWO_PI);
    }

    @Test
    public void theTableIsDeterministic() {
        HelixPath a = simple(1, 0.1), b = simple(1, 0.1);
        assertEquals(a.steps(), b.steps());
        for (int i = 0; i < a.steps(); i++) {
            assertEquals(a.thetaAt(i), b.thetaAt(i));
            assertEquals(a.q(i), b.q(i));
        }
    }
}
