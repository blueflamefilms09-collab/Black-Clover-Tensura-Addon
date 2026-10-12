package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.GravityCycle.Kind;
import com.newuniverse.nusmp.dungeon.nexus.zone.GravityCycle.Mode;
import com.newuniverse.nusmp.dungeon.nexus.zone.GravityCycle.Push;
import com.newuniverse.nusmp.dungeon.nexus.zone.GravityCycle.State;
import com.newuniverse.nusmp.dungeon.nexus.zone.GravityCycle.Zone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The slow cycle of the gravity zones of the Underworld Gate: warnings, calms, bounded pushes. */
class GravityCycleTest {
    private static Zone zone(Kind kind, int phase) { return new Zone(0, 100, 200, 8, -50, -30, kind, phase); }

    @Test
    void aZoneIsAVerticalCylinder() {
        Zone z = zone(Kind.WELL, 0);
        assertTrue(z.contains(100.5, -49, 200.5));
        assertTrue(z.contains(100.5 + 8, -40, 200.5));
        assertFalse(z.contains(100.5 + 8.1, -40, 200.5), "past the radius");
        assertFalse(z.contains(100.5, -51, 200.5), "under it");
        assertFalse(z.contains(100.5, -28.5, 200.5), "over it");
        assertEquals(5.0, z.distance(105.5, 200.5), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> new Zone(0, 0, 0, 0, 0, 5, Kind.WELL, 0));
        assertThrows(IllegalArgumentException.class, () -> new Zone(0, 0, 0, 3, 5, 5, Kind.WELL, 0));
    }

    @Test
    void everyRoundCoversItsPeriodWithoutAGap() {
        for (Kind k : Kind.values()) {
            int sum = 0;
            for (String s : GravityCycle.describe(k)) sum += Integer.parseInt(s.substring(s.indexOf(':') + 1));
            assertEquals(sum, GravityCycle.period(k));
            Zone z = zone(k, 0);
            for (long t = 0; t < 2L * GravityCycle.period(k); t++) assertNotNull(GravityCycle.at(z, t));
            assertEquals(GravityCycle.at(z, 17), GravityCycle.at(z, 17 + GravityCycle.period(k)), "the round repeats");
            assertEquals(GravityCycle.at(z, 17), GravityCycle.at(z, 17 - GravityCycle.period(k)), "and runs back too");
        }
    }

    @Test
    void aSpellIsAlwaysWarnedOfAndFollowedByACalm() {
        for (Kind k : Kind.values()) {
            Zone z = zone(k, 0);
            int period = GravityCycle.period(k);
            Mode before = null;
            int warnTicks = 0;
            for (long t = 0; t < period; t++) {
                State s = GravityCycle.at(z, t);
                if (s.mode() == Mode.CALM) {
                    if (s.telegraphing()) warnTicks++;
                    else warnTicks = 0;
                    assertEquals(0.0, s.strength(), 0.0, "no push while calm");
                } else {
                    assertFalse(s.telegraphing(), "a spell is not itself a warning");
                    if (before == Mode.CALM) assertTrue(warnTicks >= GravityCycle.WARN, k + ": a spell starts after " + warnTicks + " ticks of warning at the least");
                    warnTicks = 0;
                }
                before = s.mode();
            }
        }
    }

    @Test
    void theWarningNamesWhatIsComing() {
        Zone z = zone(Kind.TIDE, 0);
        boolean heavy = false, light = false, reversal = false;
        for (long t = 0; t < GravityCycle.period(Kind.TIDE); t++) {
            State s = GravityCycle.at(z, t);
            if (!s.telegraphing()) continue;
            long later = t + s.remaining();                              // the moment the calm ends
            Mode next = GravityCycle.at(z, later).mode();
            assertEquals(next, s.warning(), "at " + t);
            heavy |= next == Mode.HEAVY;
            light |= next == Mode.LIGHT;
            reversal |= next == Mode.REVERSAL;
        }
        assertTrue(heavy && light && reversal, "a tide warns of all three");
    }

    @Test
    void calmsAreLongEnoughToCatchABreath() {
        for (Kind k : Kind.values()) {
            Zone z = zone(k, 0);
            int run = 0, shortest = Integer.MAX_VALUE, period = GravityCycle.period(k);
            long start = 0;
            while (GravityCycle.at(z, start).mode() == Mode.CALM) start++;     // begin inside a spell, so no calm is cut by the start
            for (long t = start; t < start + period; t++) {
                if (GravityCycle.at(z, t).mode() == Mode.CALM) run++;
                else if (run > 0) {
                    shortest = Math.min(shortest, run);
                    run = 0;
                }
            }
            if (run > 0) shortest = Math.min(shortest, run);
            assertTrue(shortest >= 2 * GravityCycle.WARN, k + ": the shortest calm is " + shortest + " ticks");
            assertTrue(GravityCycle.activeShare(k) < 0.6, k + " is calm for at least two fifths of its round");
        }
    }

    @Test
    void noSpellIsLongerThanEighteenSecondsAndABurstIsShort() {
        for (Kind k : Kind.values()) {
            Zone z = zone(k, 0);
            int period = GravityCycle.period(k);
            for (long t = 0; t < period; t++) {
                State s = GravityCycle.at(z, t);
                if (s.mode() == Mode.CALM) continue;
                assertTrue(s.elapsed() + s.remaining() <= 360, k + " " + s.mode());
                if (s.mode() == Mode.REVERSAL) assertTrue(s.elapsed() + s.remaining() <= 40, "a reversal is a burst");
            }
        }
    }

    @Test
    void strengthRisesAndFallsSmoothlyAndStaysInRange() {
        Zone z = zone(Kind.WELL, 0);
        double last = 0;
        for (long t = 0; t < GravityCycle.period(Kind.WELL); t++) {
            State s = GravityCycle.at(z, t);
            assertTrue(s.strength() >= 0 && s.strength() <= 1.0 + 1e-12);
            assertTrue(Math.abs(s.strength() - last) < 0.2, "no jumps: " + last + " -> " + s.strength() + " at " + t);
            last = s.strength();
            if (s.mode() == Mode.HEAVY && s.elapsed() > GravityCycle.RAMP && s.remaining() > GravityCycle.RAMP) assertEquals(1.0, s.strength(), 1e-9, "full strength in the middle");
        }
    }

    @Test
    void phasesMoveZonesOutOfStep() {
        Zone a = zone(Kind.WELL, 0), b = zone(Kind.WELL, 500);
        int differing = 0;
        for (long t = 0; t < 2000; t++) if (GravityCycle.at(a, t).mode() != GravityCycle.at(b, t).mode()) differing++;
        assertTrue(differing > 500, "two wells do not press and release together");
    }

    @Test
    void theHeavyPushIsBoundedAndNeverLifts() {
        Zone z = zone(Kind.WELL, 0);
        for (long t = 0; t < GravityCycle.period(Kind.WELL); t++) {
            State s = GravityCycle.at(z, t);
            Push p = GravityCycle.push(s, z, -45);
            if (s.mode() == Mode.HEAVY) {
                assertTrue(p.gravityBoost() <= GravityCycle.MAX_GRAVITY_BOOST + 1e-12 && p.gravityBoost() >= 0);
                assertTrue(p.jumpCut() <= GravityCycle.MAX_JUMP_CUT + 1e-12 && p.jumpCut() >= 0);
                assertTrue(p.slowAmp() <= 1);
                assertEquals(-1, p.levitateAmp(), "a press never lifts");
                assertFalse(p.slowFall());
            } else if (s.mode() == Mode.CALM) {
                assertFalse(p.any(), "nothing at all while calm");
            }
        }
    }

    @Test
    void aLiftStopsShortOfTheTopOfTheZone() {
        Zone z = zone(Kind.FLOAT, 0);
        for (long t = 0; t < GravityCycle.period(Kind.FLOAT); t++) {
            State s = GravityCycle.at(z, t);
            if (s.mode() != Mode.LIGHT && s.mode() != Mode.REVERSAL) continue;
            Push low = GravityCycle.push(s, z, -49), nearTop = GravityCycle.push(s, z, z.yMax() - GravityCycle.CAP_MARGIN + 0.01), above = GravityCycle.push(s, z, z.yMax() + 5);
            assertEquals(-1, nearTop.levitateAmp(), "no lift in the top " + GravityCycle.CAP_MARGIN + " blocks");
            assertEquals(-1, above.levitateAmp());
            assertTrue(low.levitateAmp() <= 3);
            assertEquals(0.0, low.gravityBoost(), 0.0);
            assertTrue(low.slowFall() && nearTop.slowFall(), "every lift comes with a soft landing");
        }
    }

    @Test
    void aTideDoesBothAndAReversalLiftsHarderThanALight() {
        Zone z = zone(Kind.TIDE, 0);
        int maxLight = -1, maxReversal = -1;
        boolean heavy = false;
        for (long t = 0; t < GravityCycle.period(Kind.TIDE); t++) {
            State s = GravityCycle.at(z, t);
            Push p = GravityCycle.push(s, z, -49);
            if (s.mode() == Mode.LIGHT) maxLight = Math.max(maxLight, p.levitateAmp());
            if (s.mode() == Mode.REVERSAL) maxReversal = Math.max(maxReversal, p.levitateAmp());
            heavy |= s.mode() == Mode.HEAVY && p.gravityBoost() > 0.3;
        }
        assertTrue(heavy);
        assertTrue(maxReversal > maxLight && maxLight >= 0);
    }

    @Test
    void theColoursAreDistinct() {
        assertEquals(4, java.util.Set.of(GravityCycle.colour(Mode.HEAVY), GravityCycle.colour(Mode.LIGHT), GravityCycle.colour(Mode.REVERSAL), GravityCycle.colour(Mode.CALM)).size());
        for (Mode m : Mode.values()) assertTrue(GravityCycle.colour(m) >= 0 && GravityCycle.colour(m) <= 0xFFFFFF);
        for (Mode m : Mode.values()) {
            if (m == Mode.CALM) continue;
            assertFalse(UnderworldText.warning(m).isEmpty());
            assertFalse(UnderworldText.active(m).isEmpty());
        }
        assertEquals("", UnderworldText.warning(Mode.CALM));
    }
}
