package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.mana.zone.ZoneUpRules.OriginMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure maths of the Mana Zone upgrade: the aim packet's checks, the origin of a zone-cast spell, the look, and the evasion. */
class ZoneUpMathTest {
    // ---------------------------------------------------------------- the packet
    @Test
    void finiteRefusesNanInfinityAndAbsurdCoordinates() {
        assertTrue(ZoneUpMath.finite(0, 64, -30_000_000));
        assertFalse(ZoneUpMath.finite(Double.NaN, 0, 0));
        assertFalse(ZoneUpMath.finite(0, Double.POSITIVE_INFINITY, 0));
        assertFalse(ZoneUpMath.finite(0, 0, Double.NEGATIVE_INFINITY));
        assertFalse(ZoneUpMath.finite(3.0e7 + 1, 0, 0));
        assertFalse(ZoneUpMath.finite(1.0e300, 0, 0));
        assertTrue(ZoneUpMath.finite());
    }

    @Test
    void entityIdsAreMinusOneOrSmall() {
        assertTrue(ZoneUpMath.entityIdOk(-1));
        assertTrue(ZoneUpMath.entityIdOk(0));
        assertTrue(ZoneUpMath.entityIdOk(123_456));
        assertFalse(ZoneUpMath.entityIdOk(-2));
        assertFalse(ZoneUpMath.entityIdOk(Integer.MIN_VALUE));
        assertFalse(ZoneUpMath.entityIdOk(Integer.MAX_VALUE));
    }

    @Test
    void theRateLimitHoldsPerSenderAndSurvivesAClockThatWentBack() {
        assertFalse(ZoneUpMath.rateOk(100, 100, 2));
        assertFalse(ZoneUpMath.rateOk(101, 100, 2));
        assertTrue(ZoneUpMath.rateOk(102, 100, 2));
        assertTrue(ZoneUpMath.rateOk(50, 100, 2), "a reloaded world: the old stamp is in the future");
        assertTrue(ZoneUpMath.rateOk(0, Long.MIN_VALUE / 4, 2), "a sender never seen before");
        assertTrue(ZoneUpMath.rateOk(Long.MAX_VALUE / 2, Long.MIN_VALUE / 4, 2), "no overflow for the never-seen stamp");
    }

    @Test
    void anAimIsFreshForItsTtlOnly() {
        assertTrue(ZoneUpMath.fresh(100, 100, 30));
        assertTrue(ZoneUpMath.fresh(130, 100, 30));
        assertFalse(ZoneUpMath.fresh(131, 100, 30));
        assertFalse(ZoneUpMath.fresh(99, 100, 30), "an aim from the future is not fresh");
    }

    @Test
    void theAimChecksAreInTheRightOrderAndEachOneBites() {
        double r = 20, tol = 1.5;
        // OK: inside
        assertEquals(ZoneUpMath.Aim.OK, ZoneUpMath.checkAim(true, true, true, 10, 0, 0, 0, 0, 0, r, tol, 100, 0, 2));
        // the boundary and the tolerance
        assertEquals(ZoneUpMath.Aim.OK, ZoneUpMath.checkAim(true, true, true, 21.5, 0, 0, 0, 0, 0, r, tol, 100, 0, 2));
        assertEquals(ZoneUpMath.Aim.OUTSIDE_ZONE, ZoneUpMath.checkAim(true, true, true, 21.6, 0, 0, 0, 0, 0, r, tol, 100, 0, 2));
        assertEquals(ZoneUpMath.Aim.OUTSIDE_ZONE, ZoneUpMath.checkAim(true, true, true, 15, 15, 15, 0, 0, 0, r, tol, 100, 0, 2), "the sphere, not the box");
        // the zone is the server's: a different centre moves the sphere
        assertEquals(ZoneUpMath.Aim.OK, ZoneUpMath.checkAim(true, true, true, 110, 0, 0, 100, 0, 0, r, tol, 100, 0, 2));
        // the cheap refusals come first
        assertEquals(ZoneUpMath.Aim.DISABLED, ZoneUpMath.checkAim(false, true, true, Double.NaN, 0, 0, 0, 0, 0, r, tol, 100, 100, 2));
        assertEquals(ZoneUpMath.Aim.NO_ZONE, ZoneUpMath.checkAim(true, false, true, 1, 0, 0, 0, 0, 0, r, tol, 100, 0, 2));
        assertEquals(ZoneUpMath.Aim.WRONG_DIMENSION, ZoneUpMath.checkAim(true, true, false, 1, 0, 0, 0, 0, 0, r, tol, 100, 0, 2));
        assertEquals(ZoneUpMath.Aim.NOT_FINITE, ZoneUpMath.checkAim(true, true, true, Double.NaN, 0, 0, 0, 0, 0, r, tol, 100, 0, 2));
        assertEquals(ZoneUpMath.Aim.NOT_FINITE, ZoneUpMath.checkAim(true, true, true, 0, Double.POSITIVE_INFINITY, 0, 0, 0, 0, r, tol, 100, 0, 2));
        assertEquals(ZoneUpMath.Aim.TOO_FAST, ZoneUpMath.checkAim(true, true, true, 1, 0, 0, 0, 0, 0, r, tol, 100, 99, 2));
        // a zone that has no radius yet accepts only what is on its owner
        assertEquals(ZoneUpMath.Aim.OUTSIDE_ZONE, ZoneUpMath.checkAim(true, true, true, 8, 0, 0, 0, 0, 0, 0, tol, 100, 0, 2));
        assertEquals(ZoneUpMath.Aim.OK, ZoneUpMath.checkAim(true, true, true, 1, 0, 0, 0, 0, 0, 0, tol, 100, 0, 2));
        // a negative radius or tolerance never widens the zone
        assertEquals(ZoneUpMath.Aim.OUTSIDE_ZONE, ZoneUpMath.checkAim(true, true, true, 5, 0, 0, 0, 0, 0, -10, -10, 100, 0, 2));
    }

    // ---------------------------------------------------------------- pages
    @Test
    void pagesThatActOnTheCasterAreNotZoneCast() {
        for (String ok : new String[]{"fire_ball", "mana_strike", "lightning_lance", "Water Dragon", "thorn_whip"}) assertTrue(ZoneUpMath.pageEligible(ok), ok);
        for (String bad : new String[]{"zone_omni_volley", "zone_pressure_space", "mana_zone", "wind_dash", "blink_step", "spatial_teleport", "summon_grimoire", "store_weapon", "draw_weapon", "spirit_dive",
                "devil_union", "mode_armor", "armour_page", "flight", "true_magic_rune_array_zone", "Zone"}) assertFalse(ZoneUpMath.pageEligible(bad), bad);
        assertFalse(ZoneUpMath.pageEligible(null));
        assertFalse(ZoneUpMath.pageEligible(""));
    }

    // ---------------------------------------------------------------- the origin
    private static double d(double[] a, double x, double y, double z) { return Math.sqrt((a[0] - x) * (a[0] - x) + (a[1] - y) * (a[1] - y) + (a[2] - z) * (a[2] - z)); }

    @Test
    void behindIsOnTheFarSideOfTheTargetAsSeenFromTheCaster() {
        double[] o = new double[3];
        ZoneUpMath.origin(OriginMode.BEHIND, 0, 0, 0, 40, 0, 1.6, 0, 10, 1.6, 0, 3.0, o);
        assertEquals(13.0, o[0], 1e-9);
        assertEquals(1.6, o[1], 1e-9);
        assertEquals(0.0, o[2], 1e-9);
        // the caster looks along the line caster -> origin -> target reversed: the spell travels from the origin back to the target
        assertEquals(3.0, d(o, 10, 1.6, 0), 1e-9);
        ZoneUpMath.origin(OriginMode.BEHIND, 0, 0, 0, 40, 0, 1.6, 0, 0, 20, 0, 3.0, o);       // straight up: behind is higher still
        assertEquals(23.0, o[1], 1e-9);
        assertEquals(0.0, o[0], 1e-9);
    }

    @Test
    void everyOriginIsInsideTheZoneWhateverTheTarget() {
        long seed = 12345;
        double[] o = new double[3];
        for (OriginMode m : OriginMode.values()) {
            for (int i = 0; i < 4000; i++) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                double a = ((seed >>> 11) & 0xFFFF) / 65535.0, b = ((seed >>> 27) & 0xFFFF) / 65535.0, c = ((seed >>> 43) & 0xFFFF) / 65535.0;
                double radius = 8 + a * 32;
                // a target inside the zone (tolerance included), a caster near the centre
                double tx = (a - 0.5) * 2 * radius * 0.7, ty = (b - 0.5) * 2 * radius * 0.7, tz = (c - 0.5) * 2 * radius * 0.7;
                ZoneUpMath.origin(m, 5, 70, -3, radius, 5 + (b - 0.5), 71.6, -3 + (c - 0.5), 5 + tx, 70 + ty, -3 + tz, 3.0, o);
                assertTrue(d(o, 5, 70, -3) <= radius * ZoneUpMath.INSIDE + 1e-6, m + " origin outside the zone: " + d(o, 5, 70, -3) + " of " + radius);
                assertTrue(ZoneUpMath.finite(o[0], o[1], o[2]), m + " origin not finite");
            }
        }
    }

    @Test
    void theEdgeOriginLiesOnTheLineFromTheCentreThroughTheTargetAndRunsAtMostSixteen() {
        double[] o = new double[3];
        ZoneUpMath.origin(OriginMode.EDGE, 0, 0, 0, 20, 0, 0, 0, 5, 0, 0, 3.0, o);
        assertEquals(5.0 + 14.4, o[0], 1e-9, "radius 20 * 0.97 = 19.4 from the centre, 14.4 past the target");
        assertEquals(0.0, o[1], 1e-9);
        ZoneUpMath.origin(OriginMode.EDGE, 0, 0, 0, 40, 0, 0, 0, 5, 0, 0, 3.0, o);
        assertEquals(5.0 + ZoneUpMath.MAX_EDGE_RUN, o[0], 1e-9, "a big zone: the run is capped");
        // a target on the centre: the line falls back to the caster's side, then to +z
        ZoneUpMath.origin(OriginMode.EDGE, 0, 0, 0, 20, 4, 0, 0, 0, 0, 0, 3.0, o);
        assertTrue(Math.abs(o[0]) > 1 && Math.abs(o[2]) < 1e-9, "away from the caster along x");
        ZoneUpMath.origin(OriginMode.EDGE, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 3.0, o);
        assertEquals(0.0, o[0], 1e-9);
        assertTrue(o[2] > 1);
        // a target on the boundary: no room, the origin is the target
        ZoneUpMath.origin(OriginMode.EDGE, 0, 0, 0, 20, 0, 0, 0, 19.4, 0, 0, 3.0, o);
        assertEquals(19.4, o[0], 1e-9);
    }

    @Test
    void theAboveOriginIsOverTheTargetAndAZeroLengthBehindFallsBackToAnAxis() {
        double[] o = new double[3];
        ZoneUpMath.origin(OriginMode.ABOVE, 0, 0, 0, 40, 0, 0, 0, 7, 2, 3, 3.0, o);
        assertEquals(7.0, o[0], 1e-9);
        assertEquals(2.0 + 3.0 + 1.0, o[1], 1e-9);
        assertEquals(3.0, o[2], 1e-9);
        ZoneUpMath.origin(OriginMode.BEHIND, 0, 0, 0, 40, 5, 5, 5, 5, 5, 5, 3.0, o);          // target on the caster's eye
        assertEquals(8.0, o[2], 1e-9);
        ZoneUpMath.origin(null, 0, 0, 0, 40, 0, 0, 0, 10, 0, 0, 3.0, o);                       // no mode: behind
        assertEquals(13.0, o[0], 1e-9);
    }

    @Test
    void clampInsideOnlyMovesWhatIsOutside() {
        double[] p = {3, 4, 0};
        ZoneUpMath.clampInside(0, 0, 0, 10, p);
        assertEquals(3.0, p[0], 0.0);
        p = new double[]{30, 40, 0};
        ZoneUpMath.clampInside(0, 0, 0, 10, p);
        assertEquals(9.7, Math.sqrt(p[0] * p[0] + p[1] * p[1]), 1e-9);
        assertEquals(0.6, p[0] / Math.sqrt(p[0] * p[0] + p[1] * p[1]), 1e-9, "the direction is kept");
        p = new double[]{0, 0, 0};
        ZoneUpMath.clampInside(0, 0, 0, 0, p);
        assertEquals(0.0, p[0], 0.0);
    }

    // ---------------------------------------------------------------- the look
    @Test
    void theLookPointsFromTheOriginAtTheTargetInMinecraftsConvention() {
        float[] l = new float[2];
        ZoneUpMath.look(0, 0, 0, 0, 0, 10, l);
        assertEquals(0f, l[0], 1e-4f);
        assertEquals(0f, l[1], 1e-4f);
        ZoneUpMath.look(0, 0, 0, 10, 0, 0, l);
        assertEquals(-90f, l[0], 1e-3f, "+x is yaw -90");
        ZoneUpMath.look(0, 0, 0, -10, 0, 0, l);
        assertEquals(90f, l[0], 1e-3f);
        ZoneUpMath.look(0, 0, 0, 0, 10, 0, l);
        assertEquals(-90f, l[1], 1e-3f, "up is pitch -90");
        ZoneUpMath.look(0, 0, 0, 0, -10, 0, l);
        assertEquals(90f, l[1], 1e-3f);
        // Minecraft's own view vector from (yaw, pitch) comes back to the direction asked
        long seed = 99;
        for (int i = 0; i < 2000; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double dx = ((seed >>> 11) & 0xFFFF) / 65535.0 - 0.5, dy = ((seed >>> 27) & 0xFFFF) / 65535.0 - 0.5, dz = ((seed >>> 43) & 0xFFFF) / 65535.0 - 0.5;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-3) continue;
            ZoneUpMath.look(1, 2, 3, 1 + dx * 40, 2 + dy * 40, 3 + dz * 40, l);
            double yaw = Math.toRadians(l[0]), pitch = Math.toRadians(l[1]);
            double vx = -Math.sin(yaw) * Math.cos(pitch), vy = -Math.sin(pitch), vz = Math.cos(yaw) * Math.cos(pitch);          // Entity.calculateViewVector
            assertEquals(dx / len, vx, 1e-4);
            assertEquals(dy / len, vy, 1e-4);
            assertEquals(dz / len, vz, 1e-4);
        }
    }

    // ---------------------------------------------------------------- evasion
    @Test
    void aProjectileFlyingAtTheBodyIsPredictedAndOneFlyingAwayOrWideIsNot() {
        // 6 blocks away, 1.2 per tick straight at the body: arrives in about 5 ticks (to the 0.9 radius: 4.25)
        double t = ZoneUpMath.timeToHit(6, 0, 0, -1.2, 0, 0, 0, 0, 0, 0.9, 6);
        assertTrue(t > 4 && t < 5.1, "time " + t);
        assertEquals(-1.0, ZoneUpMath.timeToHit(6, 0, 0, 1.2, 0, 0, 0, 0, 0, 0.9, 6), 0.0, "flying away");
        assertEquals(-1.0, ZoneUpMath.timeToHit(6, 0, 5, -1.2, 0, 0, 0, 0, 0, 0.9, 6), 0.0, "passing 5 blocks wide");
        assertTrue(ZoneUpMath.timeToHit(6, 0, 0.8, -1.2, 0, 0, 0, 0, 0, 0.9, 6) >= 0, "passing 0.8 wide still hits a 0.9 body");
        assertEquals(-1.0, ZoneUpMath.timeToHit(6, 0, 1.0, -1.2, 0, 0, 0, 0, 0, 0.9, 6), 0.0, "1.0 wide misses");
        assertEquals(-1.0, ZoneUpMath.timeToHit(60, 0, 0, -1.2, 0, 0, 0, 0, 0, 0.9, 6), 0.0, "beyond the horizon");
        assertEquals(-1.0, ZoneUpMath.timeToHit(6, 0, 0, 0, 0, 0, 0, 0, 0, 0.9, 6), 0.0, "standing still");
        assertEquals(0.0, ZoneUpMath.timeToHit(0.3, 0, 0, 0, 0, 0, 0, 0, 0, 0.9, 6), 0.0, "already on the body");
        assertTrue(ZoneUpMath.timeToHit(0, 20, 0, 0, -2.0, 0, 0, 0, 0, 0.9, 12) >= 0, "falling on the body from above counts");
        assertEquals(-1.0, ZoneUpMath.timeToHit(0, 20, 0, 0, -2.0, 0, 0, 0, 0, 0.9, 6), 0.0, "...but not outside the horizon");
    }

    @Test
    void theDodgeChanceGrowsWithReinforcementHalvesAgainstABossAndNeverPassesNinetyFivePercent() {
        assertEquals(0.6, ZoneUpMath.dodgeChance(0.6, 0, false), 1e-12);
        assertEquals(0.85, ZoneUpMath.dodgeChance(0.6, 1, false), 1e-12);
        assertEquals(0.3, ZoneUpMath.dodgeChance(0.6, 0, true), 1e-12);
        assertEquals(0.95, ZoneUpMath.dodgeChance(0.95, 1, false), 1e-12);
        assertEquals(0.0, ZoneUpMath.dodgeChance(0.0, 0, false), 0.0);
        assertEquals(0.85, ZoneUpMath.dodgeChance(0.6, 7, false), 1e-12, "mastery over 1 counts as 1");
        assertEquals(0.6, ZoneUpMath.dodgeChance(0.6, -3, false), 1e-12);
        for (double base = 0; base <= 0.95; base += 0.05)
            for (double m = 0; m <= 1; m += 0.25) assertTrue(ZoneUpMath.dodgeChance(base, m, true) <= ZoneUpMath.dodgeChance(base, m, false) + 1e-12);
    }

    @Test
    void theSidestepIsSidewaysTowardsTheSideTheOwnerIsOn() {
        double[] s = new double[2];
        // an arrow flying -x, the owner a little to its +z: step +z
        ZoneUpMath.sidestep(-1.2, 0, -6, 0.2, 0.6, 0, s);
        assertEquals(0.0, s[0], 1e-9);
        assertEquals(0.6, Math.abs(s[1]), 1e-9);
        double flyDir = -1.2;
        double side = s[1] * 0.2;
        assertTrue(side > 0 || flyDir > 0 && side < 0, "away from the line: the owner at +0.2 steps towards +z");
        ZoneUpMath.sidestep(-1.2, 0, -6, 0.2, 0.6, 0, s);
        double pz1 = s[1];
        ZoneUpMath.sidestep(-1.2, 0, -6, -0.2, 0.6, 0, s);
        assertEquals(-pz1, s[1], 1e-9, "the other side, the other way");
        // length is the impulse for any direction
        long seed = 5;
        for (int i = 0; i < 500; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double vx = ((seed >>> 11) & 0xFFFF) / 6553.5 - 5, vz = ((seed >>> 27) & 0xFFFF) / 6553.5 - 5, rx = ((seed >>> 43) & 0xFFF) / 100.0 - 20, rz = ((seed >>> 3) & 0xFFF) / 100.0 - 20;
            ZoneUpMath.sidestep(vx, vz, rx, rz, 0.6, i & 1, s);
            assertEquals(0.6, Math.sqrt(s[0] * s[0] + s[1] * s[1]), 1e-9);
            if (Math.sqrt(vx * vx + vz * vz) > 1e-3) assertEquals(0.0, (s[0] * vx + s[1] * vz) / Math.sqrt(vx * vx + vz * vz), 1e-9, "perpendicular to the flight");
        }
        // exactly on the line: the tie decides, both ways are used
        ZoneUpMath.sidestep(-1, 0, -5, 0, 0.6, 0, s);
        double a = s[1];
        ZoneUpMath.sidestep(-1, 0, -5, 0, 0.6, 1, s);
        assertEquals(-a, s[1], 1e-9);
        // falling straight down: away from where it falls; on it exactly: the tie
        ZoneUpMath.sidestep(0, 0, 3, 0, 0.6, 0, s);
        assertEquals(0.6, s[0], 1e-9);
        ZoneUpMath.sidestep(0, 0, 0, 0, 0.6, 1, s);
        assertEquals(-0.6, s[0], 1e-9);
    }
}
