package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.vfx.client.layer.ManaZoneGeometry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure maths of an open Mana Zone: growth from the start tick, the EP cap, costs, offense, defence and the client budgets. */
class ManaZoneMathTest {
    private static final ManaZoneRules.Zone Z = ManaZoneRules.DEFAULT_ZONE;
    private static final double GATE_EP = ManaZoneRules.DEFAULT_GATE.minEp();

    @Test
    void theRadiusGrowsFromTheStartTickWithoutState() {
        assertEquals(0.0, ManaZoneMath.radiusAt(0, Z.growTicks(), 30), 0.0);
        assertEquals(0.0, ManaZoneMath.radiusAt(-5, Z.growTicks(), 30), 0.0);
        assertEquals(30.0, ManaZoneMath.radiusAt(Z.growTicks(), Z.growTicks(), 30), 1e-9);
        assertEquals(30.0, ManaZoneMath.radiusAt(100_000, Z.growTicks(), 30), 1e-9, "it stops at the cap");
        double prev = -1;
        for (long t = 0; t <= Z.growTicks(); t++) {
            double r = ManaZoneMath.radiusAt(t, Z.growTicks(), 30);
            assertTrue(r >= prev, "monotonic at " + t);
            prev = r;
        }
        // the same answer however it is asked: a late joiner computes the same radius from the start tick
        assertEquals(ManaZoneMath.radiusAt(37, 60, 22.5), ManaZoneMath.radiusAt(37, 60, 22.5), 0.0);
        assertTrue(ManaZoneMath.radiusAt(20, 60, 30) > 30 * 20 / 60.0, "ease-out: faster than linear at first");
    }

    @Test
    void theCapGrowsWithEpAndStopsAtTheMaximum() {
        assertEquals(Z.baseRadius(), ManaZoneMath.capRadius(GATE_EP, GATE_EP, Z), 1e-9);
        assertEquals(Z.baseRadius(), ManaZoneMath.capRadius(10, GATE_EP, Z), 1e-9, "under the gate it is the base (a forced zone)");
        assertEquals(Z.baseRadius() + Z.radiusPerEpDecade(), ManaZoneMath.capRadius(GATE_EP * 10, GATE_EP, Z), 1e-9);
        assertEquals(Z.maxRadius(), ManaZoneMath.capRadius(1e30, GATE_EP, Z), 0.0);
        assertTrue(ManaZoneMath.capRadius(2e7, GATE_EP, Z) < ManaZoneMath.capRadius(2e9, GATE_EP, Z));
    }

    @Test
    void upkeepGrowsWithTheRadiusAndTheCollapsePaysForIt() {
        assertTrue(ManaZoneMath.upkeepPercent(40, Z) > ManaZoneMath.upkeepPercent(14, Z));
        assertEquals(Z.upkeepBasePercent() + Z.upkeepPerBlockPercent() * 20, ManaZoneMath.upkeepPercent(20, Z), 1e-12);
        assertTrue(ManaZoneMath.cooldownTicks(40, Z) > ManaZoneMath.cooldownTicks(14, Z));
        assertTrue(ManaZoneMath.cooldownTicks(14, Z) >= Z.cooldownBaseTicks());
        assertTrue(ManaZoneMath.exhaustionTicks(4800, false, Z) <= Z.exhaustionMaxTicks() * 2);
        assertEquals(ManaZoneMath.exhaustionTicks(600, false, Z) * 2, ManaZoneMath.exhaustionTicks(600, true, Z), "running out of mana doubles it");
        assertTrue(ManaZoneMath.costMult(Z, false) < 1.0);
        assertTrue(ManaZoneMath.costMult(Z, true) > 1.0, "exhausted, a spell costs more");
    }

    @Test
    void powerRampsInAndGrowsWithDensity() {
        assertEquals(1.0, ManaZoneMath.powerMult(Z, 0, 3.0), 1e-12);
        assertEquals(1.0 + Z.powerBonus(), ManaZoneMath.powerMult(Z, 1, 1.0), 1e-12);
        assertTrue(ManaZoneMath.powerMult(Z, 1, 3.0) > ManaZoneMath.powerMult(Z, 1, 1.0));
        assertTrue(ManaZoneMath.powerMult(Z, 1, 99) <= 1.0 + Z.powerBonus() + Z.powerDensityBonus(), "the density bonus is capped");
        assertEquals(Z.speedBonus(), ManaZoneMath.speedBonus(Z, 1), 0.0);
        assertEquals(0.0, ManaZoneMath.speedBonus(Z, 0), 0.0);
    }

    @Test
    void strikeReachAndCountGrow() {
        assertEquals(24 * Z.rangeMult(), ManaZoneMath.strikeRange(24, 40, Z), 1e-9);
        assertEquals(24.0, ManaZoneMath.strikeRange(24, 0, Z), 1e-9, "a zone that has not grown yet gives no extra reach");
        assertEquals(3 + Z.extraTargets(), ManaZoneMath.strikeTargets(3, Z));
    }

    @Test
    void anOmniVolleyStartsBehindTheTargetAndAllRoundIt() {
        double[] o = new double[3];
        // caster at the origin, target 10 blocks along +x: bolt 0 comes from beyond the target
        ManaZoneMath.omniOrigin(0, 4, 10, 0, 0, 0, 0, 3, o);
        assertEquals(13.0, o[0], 1e-9);
        assertEquals(0.0, o[2], 1e-9);
        assertTrue(o[0] > 10, "behind the target, away from the caster");
        for (int i = 0; i < 4; i++) {
            ManaZoneMath.omniOrigin(i, 4, 10, 0, 0, 0, 0, 3, o);
            double dx = o[0] - 10, dz = o[2];
            assertEquals(3.0, Math.sqrt(dx * dx + dz * dz), 1e-9, "every origin is 3 blocks from the target");
            assertTrue(o[1] >= 1.0);
        }
        ManaZoneMath.omniOrigin(2, 4, 10, 0, 0, 0, 0, 3, o);
        assertEquals(7.0, o[0], 1e-9, "the opposite bolt comes from the caster's side");
    }

    @Test
    void foresightGrowsWithReinforcementAndIsCappedAgainstBosses() {
        double plain = ManaZoneMath.predictReduction(Z, 0, false);
        double reinforced = ManaZoneMath.predictReduction(Z, 1, false);
        assertEquals(Z.predictBase(), plain, 1e-12);
        assertEquals(Z.predictReinforced(), reinforced, 1e-12);
        assertTrue(reinforced > plain);
        assertEquals(Z.predictBossCap(), ManaZoneMath.predictReduction(Z, 1, true), 1e-12, "a boss's blow is only foreseen up to the cap");
        assertTrue(ManaZoneMath.allyFactor(Z) < 1.0 && ManaZoneMath.allyFactor(Z) > 0.5);
        assertTrue(ManaZoneMath.bubbleFactor(Z, true) > ManaZoneMath.bubbleFactor(Z, false), "a pressurised space holds a boss less");
        assertTrue(ManaZoneMath.bossProjectileFactor(Z) < 1.0 && ManaZoneMath.bossProjectileFactor(Z) > 0.0);
    }

    @Test
    void periodicJobsAreStaggered() {
        assertEquals(1004, ManaZoneMath.nextDue(1001, 4, 0));
        assertEquals(1005, ManaZoneMath.nextDue(1001, 4, 1));
        assertEquals(1005, ManaZoneMath.nextDue(1004, 4, 1));
        assertEquals(1008, ManaZoneMath.nextDue(1004, 4, 0));
        for (long now = 0; now < 200; now++)
            for (int s = 0; s < 9; s++) {
                long due = ManaZoneMath.nextDue(now, 4, s);
                assertTrue(due > now && due - now <= 4, now + "/" + s);
                assertEquals(Math.floorMod(s, 4), due % 4);
            }
    }

    @Test
    void theClientBudgetsHold() {
        assertEquals(40, ManaZoneMath.lodSegments(5, 30, 40, 8));
        assertEquals(30, ManaZoneMath.lodSegments(50, 30, 40, 8));
        assertEquals(20, ManaZoneMath.lodSegments(100, 30, 40, 8));
        assertEquals(14, ManaZoneMath.lodSegments(300, 30, 40, 8));
        assertEquals(0, ManaZoneMath.lodSegments(10_000, 30, 40, 8), "far away it is not drawn at all");
        assertEquals(8, ManaZoneMath.lodSegments(300, 30, 10, 8));
        assertTrue(ManaZoneMath.shellAlpha(30, 30) < ManaZoneMath.shellAlpha(10, 30), "the shell fades when the camera is on it");
        assertTrue(ManaZoneMath.shellAlpha(30, 30) >= 0.25);
        // each instance of a zone stays under the vertex buffer's cap: the shell (96 quads at full detail and the outline sprite), the ground (disc, front or wave, pad, glow, two rings, motes, streaks)
        assertTrue(ManaZoneMath.shellQuads(ManaZoneGeometry.SHELL_LON, ManaZoneGeometry.SHELL_LAT) * 4 + 4 <= ManaZoneMath.VERTEX_BUDGET);
        assertTrue((4 + 2 * ManaZoneGeometry.RING_SEGMENTS + ManaZoneGeometry.MOTES + ManaZoneGeometry.STREAKS) * 4 <= ManaZoneMath.VERTEX_BUDGET);
    }

    @Test
    void easeStaysInRange() {
        for (double x = -1; x <= 2; x += 0.05) {
            double e = ManaZoneMath.ease(x);
            assertTrue(e >= 0 && e <= 1);
        }
    }
}
