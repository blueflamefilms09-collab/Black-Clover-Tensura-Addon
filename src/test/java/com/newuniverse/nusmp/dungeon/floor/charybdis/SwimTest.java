package com.newuniverse.nusmp.dungeon.floor.charybdis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** How the leviathan is steered: the orbit, the engagement, the brake, the lunge, being stuck. Played out tick by tick with no world. */
class SwimTest {
    private static final double CX = 100, CY = -5, CZ = -40;

    /** A boss swimming for a number of ticks; the position is integrated from the velocity, nothing is in the way. */
    private static final class Sim {
        final Swim swim;
        double x, y, z, maxSpeed, minDist = Double.MAX_VALUE;
        long tick;

        Sim(long seed) {
            swim = new Swim(CX, CY, CZ, seed);
            double[] o = swim.orbitPoint(0);
            x = o[0];
            y = o[1];
            z = o[2];
        }

        void run(int ticks, boolean target, double tx, double ty, double tz, boolean hold) {
            for (int i = 0; i < ticks; i++, tick++) {
                swim.step(x, y, z, target, tx, ty, tz, hold, 1.0, tick);
                x += swim.vx();
                y += swim.vy();
                z += swim.vz();
                maxSpeed = Math.max(maxSpeed, Math.sqrt(swim.vx() * swim.vx() + swim.vy() * swim.vy() + swim.vz() * swim.vz()));
                if (target) minDist = Math.min(minDist, Math.sqrt((x - tx) * (x - tx) + (y - ty) * (y - ty) + (z - tz) * (z - tz)));
            }
        }

        double flat() { return Math.hypot(x - CX, z - CZ); }
    }

    @Test
    void theOrbitIsASlowLapInsideTheBand() {
        for (long seed = 0; seed < 8; seed++) {
            Swim s = new Swim(CX, CY, CZ, seed);
            double minR = 1e9, maxR = -1e9, minH = 1e9, maxH = -1e9;
            double[] prev = s.orbitPoint(0);
            for (long t = 1; t < 4000; t++) {
                double[] p = s.orbitPoint(t);
                double r = Math.hypot(p[0] - CX, p[2] - CZ);
                minR = Math.min(minR, r);
                maxR = Math.max(maxR, r);
                minH = Math.min(minH, p[1] - CY);
                maxH = Math.max(maxH, p[1] - CY);
                double step = Math.sqrt((p[0] - prev[0]) * (p[0] - prev[0]) + (p[1] - prev[1]) * (p[1] - prev[1]) + (p[2] - prev[2]) * (p[2] - prev[2]));
                assertTrue(step < Swim.ORBIT_SPEED + 0.01, "the orbit asks for a slow lap: " + step);
                prev = p;
            }
            assertTrue(minR >= Swim.ORBIT_MIN - 1e-6 && maxR <= Swim.ORBIT_MAX + 1e-6, "radius 8..14: " + minR + " .. " + maxR);
            assertTrue(minH >= -Swim.BAND - 1e-6 && maxH <= Swim.BAND + 1e-6, "height within the band: " + minH + " .. " + maxH);
            assertTrue(maxR - minR > 4, "the radius breathes");
        }
        assertNotEquals(new Swim(0, 0, 0, 0).orbitPoint(100)[2], new Swim(0, 0, 0, 1).orbitPoint(100)[2], "the seed picks the direction of the lap");
        assertNotEquals(new Swim(0, 0, 0, 0).orbitPoint(0)[0], new Swim(0, 0, 0, 2L * 300).orbitPoint(0)[0], "and where on the circle it starts");
    }

    @Test
    void withNobodyToFightItLapsTheDaisWithoutLeavingTheBand() {
        Sim sim = new Sim(5);
        for (int i = 0; i < 6000; i++) {
            sim.run(1, false, 0, 0, 0, false);
            assertTrue(sim.flat() < Swim.ORBIT_MAX + 2.5, "close to the orbit at " + i + ": " + sim.flat());
            assertTrue(sim.y - CY <= Swim.BAND + 2 && sim.y - CY >= -Swim.BAND - 2);
        }
        assertTrue(sim.maxSpeed <= Swim.ORBIT_SPEED + 1e-9, "a slow lap: " + sim.maxSpeed);
    }

    @Test
    void aNearbyTargetIsApproachedToTheStandoffAndNoCloser() {
        Sim sim = new Sim(2);
        double tx = CX + 9, ty = CY + 1, tz = CZ + 4;
        sim.run(400, true, tx, ty, tz, false);
        double d = Math.sqrt((sim.x - tx) * (sim.x - tx) + (sim.y - ty) * (sim.y - ty) + (sim.z - tz) * (sim.z - tz));
        assertEquals(Swim.STANDOFF, d, 0.6, "it settles at the standoff: " + d);
        assertTrue(sim.minDist > Swim.STANDOFF - 0.8, "and never rams it: " + sim.minDist);
        assertTrue(sim.maxSpeed <= Swim.CHASE_SPEED + 1e-9, "no faster than its chase speed: " + sim.maxSpeed);
        assertTrue(Swim.CHASE_SPEED < 0.25, "slower than a dolphin's-grace swimmer");
    }

    @Test
    void aFarTargetIsLeftAloneAndTheBossStaysOnItsOrbit() {
        Sim sim = new Sim(3);
        sim.run(300, true, CX + 26, CY, CZ, false);                    // 26 blocks off: far past the chase range from anywhere on the orbit
        assertTrue(sim.flat() <= Swim.ORBIT_MAX + 2.5, "it did not leave the orbit: " + sim.flat());
    }

    @Test
    void aTargetAtTheWallCannotDrawItPastTheLeash() {
        Sim sim = new Sim(4);
        for (int i = 0; i < 2000; i++) {
            double a = i / 80.0;
            double tx = CX + Math.cos(a) * 27, tz = CZ + Math.sin(a) * 27;       // someone swimming round the foot of the wall
            sim.run(1, true, tx, CY - 4, tz, false);
            assertTrue(sim.flat() <= Swim.LEASH + 0.5, "inside the leash: " + sim.flat());
            assertTrue(sim.y >= CY - Swim.BELOW - 0.5 && sim.y <= CY + Swim.ABOVE + 0.5, "inside the vertical window: " + (sim.y - CY));
        }
    }

    @Test
    void whileItCastsItBrakesToAStandstill() {
        Sim sim = new Sim(6);
        sim.run(100, false, 0, 0, 0, false);
        assertTrue(Math.abs(sim.swim.vx()) + Math.abs(sim.swim.vz()) > 0.05, "it was swimming");
        int ticks = (int) Math.ceil(Swim.CHASE_SPEED / Swim.ACCEL) + 2;
        sim.run(ticks, false, 0, 0, 0, true);
        assertEquals(0.0, sim.swim.vx(), 1e-9);
        assertEquals(0.0, sim.swim.vy(), 1e-9);
        assertEquals(0.0, sim.swim.vz(), 1e-9);
        double before = sim.x;
        sim.run(60, true, CX + 5, CY, CZ, true);
        assertEquals(before, sim.x, 1e-9, "it stays put for the whole wind-up");
    }

    @Test
    void theVelocityNeverJerks() {
        Sim sim = new Sim(7);
        double px = 0, py = 0, pz = 0;
        for (int i = 0; i < 3000; i++) {
            boolean target = (i / 400) % 2 == 0;
            sim.run(1, target, CX + 6, CY + 2, CZ - 3, (i / 150) % 5 == 4);
            if (i > 0) {
                assertTrue(Math.abs(sim.swim.vx() - px) <= Swim.ACCEL + 1e-9 && Math.abs(sim.swim.vy() - py) <= Swim.ACCEL + 1e-9 && Math.abs(sim.swim.vz() - pz) <= Swim.ACCEL + 1e-9,
                        "a smooth change of velocity at " + i);
            }
            px = sim.swim.vx();
            py = sim.swim.vy();
            pz = sim.swim.vz();
        }
    }

    @Test
    void theLungeIsAShortFixedDiveThatGathersAndLosesSpeed() {
        assertEquals(0.0, Swim.lungeSpeed(0));
        assertEquals(0.0, Swim.lungeSpeed(Swim.LUNGE_TICKS + 1));
        assertTrue(Swim.lungeSpeed(Swim.LUNGE_TICKS) < Swim.lungeSpeed(Swim.LUNGE_TICKS - 3), "it gathers speed");
        assertEquals(Swim.LUNGE_SPEED, Swim.lungeSpeed(Swim.LUNGE_TICKS - 5), 1e-9);
        assertTrue(Swim.lungeSpeed(1) < Swim.lungeSpeed(5), "and slows at the end");
        double len = Swim.lungeLength();
        assertTrue(len > 8 && len < 12, "about ten blocks: " + len);

        Sim sim = new Sim(8);
        double x0 = sim.x, y0 = sim.y, z0 = sim.z;
        sim.swim.lunge(3, 0, 4);                                         // direction (0.6, 0, 0.8)
        assertTrue(sim.swim.lunging());
        sim.run(Swim.LUNGE_TICKS, false, 0, 0, 0, false);
        assertFalse(sim.swim.lunging());
        assertEquals(len, Math.sqrt((sim.x - x0) * (sim.x - x0) + (sim.z - z0) * (sim.z - z0)), 1e-6);
        assertEquals(0.6, (sim.x - x0) / len, 1e-9);
        assertEquals(0.8, (sim.z - z0) / len, 1e-9);
        assertEquals(y0, sim.y, 1e-9);
        sim.swim.lunge(0, 0, 0);
        assertFalse(sim.swim.lunging(), "no direction, no lunge");
    }

    @Test
    void aBossStuckAgainstStoneSwimsForTheOpenOrbitThenAsksToBePutBack() {
        Swim s = new Swim(CX, CY, CZ, 9);
        double x = CX + 10, y = CY, z = CZ + 4;                          // a wall holds it: the position never changes
        long tick = 0;
        for (; tick < Swim.STUCK_TELEPORT * 3 && !s.stuckTooLong(); tick++) s.step(x, y, z, true, CX + 3, CY, CZ + 3, false, 1.0, tick);
        assertTrue(s.stuckTooLong(), "after a few seconds of going nowhere it asks to be put back");
        assertTrue(tick >= Swim.STUCK_TELEPORT && tick <= Swim.STUCK_TELEPORT + 60, "in about ten seconds: " + tick);
        s.clearStuck();
        assertFalse(s.stuckTooLong());
        assertEquals(0.0, s.vx(), 1e-12);
    }

    @Test
    void aBossThatIsMovingFreelyIsNeverStuck() {
        Sim sim = new Sim(10);
        for (int i = 0; i < 5000; i++) {
            sim.run(1, i % 700 < 350, CX + 7, CY + 1, CZ + 5, false);
            assertFalse(sim.swim.stuckTooLong());
        }
    }

    @Test
    void holdingStillIsNotBeingStuck() {
        Swim s = new Swim(CX, CY, CZ, 11);
        for (long t = 0; t < 2000; t++) {
            s.step(CX + 10, CY, CZ, true, CX + 12, CY, CZ, true, 1.0, t);
            assertFalse(s.stuckTooLong(), "casting and recovering are not being stuck");
        }
    }

    @Test
    void yawAndPitchFollowMinecraftsConventions() {
        assertEquals(0f, Swim.yawOf(0, 1), 1e-4, "south is yaw 0");
        assertEquals(-90f, Swim.yawOf(1, 0), 1e-4, "east is -90");
        assertEquals(90f, Swim.yawOf(-1, 0), 1e-4, "west is 90");
        assertEquals(180f, Math.abs(Swim.yawOf(0, -1)), 1e-4, "north is 180");
        assertEquals(0f, Swim.pitchOf(1, 0, 0), 1e-4);
        assertEquals(45f, Swim.pitchOf(1, -1, 0), 1e-4, "down is positive");
        assertEquals(-45f, Swim.pitchOf(0, 1, 1), 1e-4);
        assertEquals(10f, Swim.turn(0f, 90f, 10f), 1e-4);
        assertEquals(-10f, Swim.turn(0f, -90f, 10f), 1e-4);
        assertEquals(179f, Swim.turn(170f, 179f, 20f), 1e-4);
        assertEquals(-175f, Swim.wrap(185f), 1e-4);
        assertEquals(170f, Swim.turn(160f, -170f, 10f), 1e-4, "the short way round across the seam");
    }
}
