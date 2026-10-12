package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Monarch's numbers: health and phases, warnings, damage, soldiers. */
class MonarchRulesTest {
    @Test
    void itHasEightTimesZagredsHealthAndAHardBodyCap() {
        assertEquals(4800.0, MonarchRules.BASE_HEALTH * MonarchRules.SOAK, 1e-6, "eight times Zagred's 600");
        assertTrue(MonarchRules.BASE_HEALTH <= 1024, "the health attribute stops at 1024");
        assertEquals(1000f / 4.8f * 0.0f, MonarchRules.realDamage(0f, 1000f));
        assertEquals(10f, MonarchRules.realDamage(48f, 1000f), 1e-4, "a blow of 48 is soaked to 10");
        assertEquals(100f, MonarchRules.realDamage(1_000_000f, 1000f), 1e-4, "no blow takes more than a tenth of the bar");
        assertEquals(0f, MonarchRules.realDamage(-5f, 1000f));
        assertEquals(0f, MonarchRules.realDamage(5f, 0f));
        // so the bar needs at least ten blows, and in practice many more
        float hp = 1000f;
        int blows = 0;
        while (hp > 0 && blows < 100) {
            hp -= MonarchRules.realDamage(1e9f, 1000f);
            blows++;
        }
        assertTrue(blows >= 10);
    }

    @Test
    void thePhasesChangeAtTwoThirdsAndOneThird() {
        assertEquals(1, MonarchRules.phaseOf(1000f, 1000f));
        assertEquals(1, MonarchRules.phaseOf(670f, 1000f));
        assertEquals(2, MonarchRules.phaseOf(659f, 1000f));
        assertEquals(2, MonarchRules.phaseOf(340f, 1000f));
        assertEquals(3, MonarchRules.phaseOf(329f, 1000f));
        assertEquals(3, MonarchRules.phaseOf(1f, 1000f));
        assertEquals(3, MonarchRules.phaseOf(5f, 0f));
        for (int ph = 1; ph <= 3; ph++) assertTrue(MonarchRules.floorOf(ph, 1000f) <= (ph == 1 ? 660f : ph == 2 ? 330f : 0f));
        assertEquals(0f, MonarchRules.floorOf(3, 1000f));
    }

    @Test
    void aBlowCannotSkipAPhase() {
        float max = 1000f, hp = max;
        int phase = 1;
        int phaseChanges = 0;
        for (int i = 0; i < 200 && hp > 0; i++) {
            float dmg = MonarchRules.gate(hp, max, MonarchRules.realDamage(1e9f, max), phase);
            assertTrue(dmg >= 0f);
            hp -= dmg;
            int now = MonarchRules.phaseOf(hp, max);
            if (now != phase) {
                assertEquals(phase + 1, now, "one phase at a time");
                phase = now;
                phaseChanges++;
            }
        }
        assertEquals(2, phaseChanges);
        assertTrue(hp <= 0.01f, "the last phase can be finished: " + hp);
        // a huge blow on a full bar stops at the floor of the phase
        assertEquals(1000f - (1000f * MonarchRules.PHASE_2 - 0.01f), MonarchRules.gate(1000f, 1000f, 500f, 1), 1e-3);
        assertEquals(100f, MonarchRules.gate(1000f, 1000f, 100f, 1), "a small blow goes through whole");
        assertEquals(0f, MonarchRules.gate(659f, 1000f, 50f, 1), "already at the floor: nothing until the phase turns");
        assertEquals(5f, MonarchRules.gate(5f, 1000f, 50f, 3), "the last phase may be finished");
    }

    @Test
    void everyMoveGivesAtLeastASecondAndABitOfWarning() {
        for (MonarchMove m : MonarchMove.values()) {
            assertTrue(MonarchRules.warning(m) >= MonarchRules.MIN_WARNING, m + " warns for " + MonarchRules.warning(m));
            assertFalse(MonarchText.warning(m).isBlank());
        }
        assertTrue(MonarchRules.TRANSITION >= MonarchRules.MIN_WARNING);
        for (int ph = 1; ph <= 3; ph++) {
            for (MonarchMove m : MonarchMove.values()) {
                assertTrue(MonarchRules.freeze(m, ph) >= MonarchRules.warning(m) || m == MonarchMove.RAIN, m + " phase " + ph);
                assertTrue(MonarchRules.cooldown(m, ph) > MonarchRules.freeze(m, ph), "a move cools down longer than it takes: " + m);
            }
            assertTrue(MonarchRules.gap(ph) >= 40, "a pause between moves in phase " + ph);
        }
        assertTrue(MonarchRules.gap(1) > MonarchRules.gap(2) && MonarchRules.gap(2) > MonarchRules.gap(3), "faster as the fight goes on");
    }

    @Test
    void noHitIsAnInstantKillAndEveryOneIsBounded() {
        for (int ph = 1; ph <= 3; ph++) {
            for (MonarchMove m : MonarchMove.values()) {
                float share = MonarchRules.hitShare(m, ph);
                assertTrue(share >= 0f && share <= MonarchRules.SHARE_CEILING, m + " " + share);
                for (float max : new float[]{1f, 20f, 40f, 200f, 5000f}) {
                    float d = MonarchRules.hit(share, max);
                    assertTrue(d <= max * MonarchRules.SHARE_CEILING + 1e-4f, m + " would take " + d + " of " + max);
                    if (share > 0f) assertTrue(d >= Math.min(3f, max * MonarchRules.SHARE_CEILING) - 1e-4f);
                }
            }
        }
        // a full bar survives two hits in a row, even the hardest in the last phase
        float worst = MonarchRules.hitShare(MonarchMove.BEAM, 3);
        assertTrue(2 * worst < 1f);
        assertEquals(0f, MonarchRules.hit(0f, 40f));
        assertEquals(0f, MonarchRules.hitShare(MonarchMove.SUMMON, 3));
        assertEquals(0f, MonarchRules.hitShare(MonarchMove.AUTHORITY, 3));
        for (MonarchMove m : MonarchMove.values()) {
            for (int ph = 1; ph < 3; ph++) assertTrue(MonarchRules.hitShare(m, ph + 1) >= MonarchRules.hitShare(m, ph), "harder, never softer, with the phase");
        }
    }

    @Test
    void itsOwnHandAndItsSoldiersNeverKillInOneBlow() {
        for (float max : new float[]{20f, 40f, 100f, 1000f}) {
            assertTrue(MonarchRules.capMelee(36f, max, true) < max, "the Monarch's blow on " + max);
            assertTrue(MonarchRules.capMelee(1e6f, max, true) <= max * MonarchRules.SHARE_CEILING + 1e-4f);
            assertTrue(MonarchRules.capMelee(1e6f, max, false) < MonarchRules.capMelee(1e6f, max, true), "a soldier hits softer than the Monarch");
            assertEquals(1f, MonarchRules.capMelee(1f, max, true), 1e-6, "a small blow is left alone");
        }
        assertEquals(0f, MonarchRules.capMelee(0f, 20f, true));
        assertTrue(2 * MonarchRules.MELEE_SHARE < 1f);
    }

    @Test
    void nobodyIsStruckTwiceAtOnce() {
        assertFalse(MonarchRules.mayHit(100, 95));
        assertFalse(MonarchRules.mayHit(100, 89));
        assertTrue(MonarchRules.mayHit(100, 88));
        assertTrue(MonarchRules.mayHit(100, Long.MIN_VALUE / 2));
    }

    @Test
    void soldiersAreCappedAtEight() {
        for (int ph = 1; ph <= 3; ph++) {
            assertTrue(MonarchRules.minionCap(ph) <= 8);
            for (int alive = 0; alive <= 10; alive++) {
                int n = MonarchRules.summonCount(ph, alive);
                assertTrue(n >= 0 && n <= MonarchRules.summonBatch(ph));
                assertTrue(Math.max(alive, alive + n) <= Math.max(alive, MonarchRules.minionCap(ph)), "never past the cap: " + ph + "/" + alive + "+" + n);
            }
        }
        assertEquals(8, MonarchRules.minionCap(3));
        assertTrue(MonarchRules.minionCap(1) < MonarchRules.minionCap(2) && MonarchRules.minionCap(2) < MonarchRules.minionCap(3), "more in each phase");
        assertEquals(0, MonarchRules.summonCount(3, 8));
        assertEquals(0, MonarchRules.greaterAllowed(1));
        assertTrue(MonarchRules.greaterAllowed(3) > 0);
        assertTrue(MonarchRules.soldierHealth(3) > MonarchRules.soldierHealth(1));
    }

    @Test
    void theSlamFieldAndTheRainAreWhatTheyPromise() {
        for (int ph = 1; ph <= 3; ph++) {
            assertTrue(MonarchRules.slamRadius(ph) >= 14 && MonarchRules.slamRadius(ph) <= 18);
            assertTrue(MonarchRules.rainCircles(ph, 1) >= 5);
            assertTrue(MonarchRules.beams(ph) == ph);
            assertTrue(MonarchRules.authoritySlow(ph, true) < MonarchRules.authoritySlow(ph, false), "a Mana Zone softens the field");
            assertTrue(MonarchRules.authorityDuration(true) < MonarchRules.authorityDuration(false));
        }
        assertTrue(MonarchRules.rainCircles(3, 4) > MonarchRules.rainCircles(1, 1));
        assertEquals(MonarchRules.rainCircles(2, 4), MonarchRules.rainCircles(2, 50), "the rain does not grow without end with the crowd");
        assertTrue(MonarchRules.SLAM_REACH > 2.0, "the ring catches those on the steps of the dais too");
        assertTrue(MonarchRules.wakes(10, 10, 3) && MonarchRules.wakes(18, 0, 0) && !MonarchRules.wakes(19, 0, 0) && !MonarchRules.wakes(5, 5, 15));
    }
}
