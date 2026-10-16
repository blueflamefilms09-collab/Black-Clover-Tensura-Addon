package com.newuniverse.nusmp.thread;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Ranking allies by how close they are to harm, and how Rouge crosses the distance. */
class ThreatMathTest {
    private static ThreatMath.Threat t(double hp, boolean hostile, boolean hurt, int eta, boolean adjacent, boolean boss) {
        return new ThreatMath.Threat(hp, hostile, hurt, eta, adjacent, boss);
    }

    @Test
    void aHealthyAllyWithNoEnemyIsCalm() {
        assertEquals(0.0, ThreatMath.score(t(1.0, false, false, -1, false, false)), 1e-9);
        assertTrue(ThreatMath.score(t(0.9, true, false, -1, true, false)) < ThreatMath.RUN_THRESHOLD, "a fight alone is not an emergency");
    }

    @Test
    void lowHealthWithAnEnemyOrARecentHitSendsHer() {
        assertTrue(ThreatMath.score(t(0.3, true, false, -1, false, false)) >= ThreatMath.RUN_THRESHOLD);
        assertTrue(ThreatMath.score(t(0.3, false, true, -1, false, false)) >= ThreatMath.RUN_THRESHOLD);
        assertTrue(ThreatMath.score(t(0.3, false, false, -1, false, false)) < ThreatMath.RUN_THRESHOLD, "low health alone, nobody near: wait");
        assertTrue(ThreatMath.score(t(0.1, true, false, -1, false, false)) > ThreatMath.score(t(0.35, true, false, -1, false, false)), "lower health, higher score");
    }

    @Test
    void aProjectileAboutToLandSendsHerAndSoonerIsWorse() {
        double soon = ThreatMath.score(t(1.0, false, false, 2, false, false));
        double late = ThreatMath.score(t(1.0, false, false, 11, false, false));
        assertTrue(soon >= ThreatMath.RUN_THRESHOLD && late >= ThreatMath.RUN_THRESHOLD);
        assertTrue(soon > late);
        assertEquals(0.0, ThreatMath.score(t(1.0, false, false, ThreatMath.PROJECTILE_HORIZON + 1, false, false)), 1e-9, "too far off to matter yet");
    }

    @Test
    void aBossRaisesTheScoreOnlyWhenItIsOnTheAlly() {
        double near = ThreatMath.score(t(0.5, true, false, -1, true, true));
        double farBoss = ThreatMath.score(t(0.5, false, false, -1, false, true));
        assertTrue(near > ThreatMath.score(t(0.5, true, false, -1, true, false)));
        assertEquals(0.0, farBoss, 1e-9);
    }

    @Test
    void projectileEtaIsTheCeilingOfDistanceOverSpeed() {
        assertEquals(4, ThreatMath.projectileEta(10.0, 3.0));
        assertEquals(0, ThreatMath.projectileEta(0.0, 3.0));
        assertEquals(-1, ThreatMath.projectileEta(10.0, 0.0), "not moving");
        assertEquals(-1, ThreatMath.projectileEta(10.0, -2.0), "moving away");
    }

    @Test
    void approachSplitsAtTheArrivalAndHopDistances() {
        assertEquals(ThreatMath.Approach.ARRIVED, ThreatMath.approach(1.0));
        assertEquals(ThreatMath.Approach.ARRIVED, ThreatMath.approach(ThreatMath.ARRIVED_DISTANCE));
        assertEquals(ThreatMath.Approach.SPRINT, ThreatMath.approach(4.0));
        assertEquals(ThreatMath.Approach.SPRINT, ThreatMath.approach(ThreatMath.HOP_DISTANCE));
        assertEquals(ThreatMath.Approach.HOP, ThreatMath.approach(8.5), "more than 8 blocks: hop through thread");
        assertTrue(ThreatMath.tooFarFromCaster(16.5));
        assertFalse(ThreatMath.tooFarFromCaster(16.0));
    }
}
