package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Blood-Red Commander's numbers: health and phases, warnings, damage, the three arts, soldiers and when they are called. */
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
    void thePhasesChangeAtHalfTheBarAndAtAQuarterOfIt() {
        assertEquals(0.50f, MonarchRules.PHASE_2, "the first summoning is at 50%");
        assertEquals(0.25f, MonarchRules.PHASE_3, "the second is at 25%");
        assertEquals(1, MonarchRules.phaseOf(1000f, 1000f));
        assertEquals(1, MonarchRules.phaseOf(670f, 1000f), "the old threshold of two thirds no longer turns the phase");
        assertEquals(1, MonarchRules.phaseOf(501f, 1000f));
        assertEquals(2, MonarchRules.phaseOf(499f, 1000f));
        assertEquals(2, MonarchRules.phaseOf(330f, 1000f), "nor does the old third");
        assertEquals(2, MonarchRules.phaseOf(251f, 1000f));
        assertEquals(3, MonarchRules.phaseOf(249f, 1000f));
        assertEquals(3, MonarchRules.phaseOf(1f, 1000f));
        assertEquals(3, MonarchRules.phaseOf(5f, 0f));
        for (int ph = 1; ph <= 3; ph++) assertTrue(MonarchRules.floorOf(ph, 1000f) <= (ph == 1 ? 500f : ph == 2 ? 250f : 0f));
        assertEquals(0f, MonarchRules.floorOf(3, 1000f));
        assertTrue(MonarchRules.floorOf(1, 1000f) > 495f && MonarchRules.floorOf(2, 1000f) > 245f, "the floor is only a hair under the threshold");
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
        assertEquals(1000f - (1000f * MonarchRules.PHASE_2 - 0.01f), MonarchRules.gate(1000f, 1000f, 600f, 1), 1e-3);
        assertEquals(100f, MonarchRules.gate(1000f, 1000f, 100f, 1), "a small blow goes through whole");
        assertEquals(0f, MonarchRules.gate(499f, 1000f, 50f, 1), "already at the floor: nothing until the phase turns");
        assertEquals(0f, MonarchRules.gate(249f, 1000f, 50f, 2), "the same at a quarter");
        assertEquals(5f, MonarchRules.gate(5f, 1000f, 50f, 3), "the last phase may be finished");
    }

    @Test
    void everyMoveGivesTheWarningItNeeds() {
        for (MonarchMove m : MonarchMove.values()) {
            int w = MonarchRules.warning(m);
            if (m.isArt()) assertTrue(w >= MonarchRules.MIN_ART_WARNING && w <= MonarchRules.MAX_ART_WARNING, m + " is fast but never under 12 ticks nor over 20: " + w);
            else assertTrue(w >= MonarchRules.MIN_WARNING, m + " warns for " + w);
            assertFalse(MonarchText.warning(m).isBlank());
        }
        assertEquals(12, MonarchRules.MIN_ART_WARNING);
        assertEquals(24, MonarchRules.MIN_WARNING, "the big moves keep their second and a bit");
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
    void theThreeArtsAreTheNewMovesAndTheOldNumbersStayPut() {
        MonarchMove[] all = MonarchMove.values();
        assertEquals(8, all.length);
        assertEquals(MonarchMove.SLAM, all[0]);
        assertEquals(MonarchMove.RAIN, all[1]);
        assertEquals(MonarchMove.SUMMON, all[2]);
        assertEquals(MonarchMove.BEAM, all[3]);
        assertEquals(MonarchMove.AUTHORITY, all[4]);
        assertEquals(MonarchMove.CROSS_SLASH, all[5], "entries are only appended");
        assertEquals(MonarchMove.LUNGE, all[6]);
        assertEquals(MonarchMove.SWEEP, all[7]);
        int arts = 0;
        for (MonarchMove m : all) if (m.isArt()) arts++;
        assertEquals(3, arts);
        // the summoning keeps its numbers
        assertEquals(44, MonarchRules.SUMMON_CHANNEL);
        assertEquals(60, MonarchRules.freeze(MonarchMove.SUMMON, 2));
        assertEquals(480, MonarchRules.cooldown(MonarchMove.SUMMON, 2));
        assertEquals(44, MonarchRules.warning(MonarchMove.SUMMON));
        assertEquals(0f, MonarchRules.hitShare(MonarchMove.SUMMON, 3));
    }

    @Test
    void theArtsAreFastButFairAndBounded() {
        for (MonarchMove m : new MonarchMove[]{MonarchMove.CROSS_SLASH, MonarchMove.LUNGE, MonarchMove.SWEEP}) {
            assertTrue(m.isArt());
            for (int ph = 1; ph <= 3; ph++) {
                // all of it can land on one body, and a full bar still survives that
                assertTrue(MonarchRules.totalShare(m, ph) < 0.5f, m + " phase " + ph + ": " + MonarchRules.totalShare(m, ph));
                assertTrue(MonarchRules.hitShare(m, ph) <= MonarchRules.SHARE_CEILING);
                for (float max : new float[]{20f, 40f, 200f}) assertTrue(MonarchRules.hit(MonarchRules.hitShare(m, ph), max) < max * 0.46f);
                if (ph < 3) assertTrue(MonarchRules.hitShare(m, ph + 1) >= MonarchRules.hitShare(m, ph), "harder, never softer, with the phase");
                assertTrue(MonarchRules.freeze(m, ph) >= MonarchRules.warning(m) + MonarchRules.RECOVERY, m + " stands open after it lands");
            }
        }
        // one cross per phase number; the arc comes back only from the second phase on
        assertEquals(1, MonarchRules.crosses(1));
        assertEquals(2, MonarchRules.crosses(2));
        assertEquals(3, MonarchRules.crosses(3));
        assertEquals(1, MonarchRules.sweepCuts(1));
        assertEquals(2, MonarchRules.sweepCuts(2));
        assertEquals(2, MonarchRules.sweepCuts(3));
        assertEquals(1, MonarchRules.cuts(MonarchMove.LUNGE, 3));
        assertEquals(3, MonarchRules.cuts(MonarchMove.CROSS_SLASH, 3));
        assertEquals(1, MonarchRules.cuts(MonarchMove.SLAM, 3));
        assertEquals(Math.PI / 4, MonarchRules.CROSS_TURN, 1e-12, "the next cross is turned 45 degrees");
        assertEquals(20, MonarchRules.CROSS_GAP, "and begins 20 ticks after the last");
        assertTrue(MonarchRules.CROSS_GAP >= MonarchRules.CROSS_WINDUP, "only one cross is live at a time: the next is marked once the last has been cut");
        // the freeze covers every cut and the recovery
        assertEquals(MonarchRules.CROSS_WINDUP + MonarchRules.CROSS_CUT + MonarchRules.RECOVERY, MonarchRules.freeze(MonarchMove.CROSS_SLASH, 1));
        assertEquals(MonarchRules.CROSS_WINDUP + 2 * MonarchRules.CROSS_GAP + MonarchRules.CROSS_CUT + MonarchRules.RECOVERY, MonarchRules.freeze(MonarchMove.CROSS_SLASH, 3));
        assertEquals(MonarchRules.SWEEP_WINDUP + MonarchRules.SWEEP_CUT + MonarchRules.RECOVERY, MonarchRules.freeze(MonarchMove.SWEEP, 1));
        assertEquals(MonarchRules.SWEEP_WINDUP + MonarchRules.SWEEP_BACK + MonarchRules.SWEEP_CUT + MonarchRules.RECOVERY, MonarchRules.freeze(MonarchMove.SWEEP, 2));
    }

    @Test
    void anArtIsOverBeforeTheCommanderMovesAgain() {
        for (MonarchMove m : new MonarchMove[]{MonarchMove.CROSS_SLASH, MonarchMove.LUNGE, MonarchMove.SWEEP}) {
            for (int ph = 1; ph <= 3; ph++) {
                int active = MonarchRules.activeUntil(m, ph), freeze = MonarchRules.freeze(m, ph);
                assertTrue(active >= MonarchRules.warning(m), m + " lands after its warning");
                assertTrue(active + MonarchRules.RECOVERY <= freeze, m + " phase " + ph + ": it stands still for the recovery after the last thing it does (" + active + " + 16 <= " + freeze + ")");
            }
        }
        assertEquals(18, MonarchRules.activeUntil(MonarchMove.CROSS_SLASH, 1), "one cross is cut 18 ticks after the move starts");
        assertEquals(18 + 20, MonarchRules.activeUntil(MonarchMove.CROSS_SLASH, 2), "the second 20 ticks after that");
        assertEquals(18 + 40, MonarchRules.activeUntil(MonarchMove.CROSS_SLASH, 3), "the third 20 after that");
        assertEquals(20 + 4, MonarchRules.activeUntil(MonarchMove.SWEEP, 1));
        assertEquals(20 + 14 + 4, MonarchRules.activeUntil(MonarchMove.SWEEP, 2));
        assertEquals(MonarchRules.warning(MonarchMove.SLAM), MonarchRules.activeUntil(MonarchMove.SLAM, 1));
    }

    @Test
    void everyArtCanBeAnsweredInTimeByWalkingOrJumping() {
        final int reaction = 5;                                                // a fifth of a second to see the mark
        final double walk = 0.216;                                             // blocks a tick, a plain walk
        double crossClear = (MonarchRules.CROSS_HALF_WIDTH + 0.3) / Math.sin(Math.PI / 4);            // from the middle of a cross to the wedge between its lines
        assertTrue(crossClear / walk <= MonarchRules.CROSS_WINDUP - reaction, "a cross can be left by walking: " + crossClear);
        assertTrue(crossClear / (walk * 0.85) <= MonarchRules.CROSS_WINDUP - reaction, "even at Slowness I: " + crossClear);
        // slower than that the cross (and the arc, and the slam) are answered by a jump: the cut ignores a body that is not on the ground
        assertFalse(MonarchShapes.ringHurts(false, 65.5, 65.0, MonarchRules.CROSS_REACH));
        assertFalse(MonarchShapes.ringHurts(false, 65.5, 65.0, MonarchRules.SWEEP_REACH));
        double laneClear = MonarchRules.LUNGE_HALF + 0.3;
        for (int amp = -1; amp <= MonarchRules.LUNGE_MAX_SLOW; amp++) {
            double pace = walk * (amp < 0 ? 1.0 : 1.0 - 0.15 * (amp + 1));
            assertTrue(laneClear / pace <= MonarchRules.LUNGE_WINDUP - reaction, "the lane can be left at Slowness amplifier " + amp + ": " + laneClear / pace);
        }
        assertTrue(MonarchRules.lungeMayTarget(-1) && MonarchRules.lungeMayTarget(0) && MonarchRules.lungeMayTarget(1));
        assertFalse(MonarchRules.lungeMayTarget(2), "Slowness III (the field of the last phase) is too slow for a lane that cannot be jumped");
        assertFalse(MonarchRules.lungeMayTarget(4));
        // the dash starts after the whole mark has been up for its wind-up, and the first body it can reach is touching the Commander
        assertTrue(MonarchRules.LUNGE_WINDUP >= MonarchRules.MIN_ART_WARNING);
        // the arc: its radius is 9 to 13, the fan 200 degrees; a jump (12 ticks in the air) fits inside its wind-up
        assertEquals(9.0, MonarchRules.sweepRadius(1));
        assertEquals(11.0, MonarchRules.sweepRadius(2));
        assertEquals(13.0, MonarchRules.sweepRadius(3));
        assertEquals(200.0, Math.toDegrees(MonarchRules.SWEEP_HALF_ANGLE * 2), 1e-9);
        assertTrue(MonarchRules.SWEEP_BACK >= 12, "a jump over the first cut can be followed by a second jump for the reverse cut");
    }

    @Test
    void theLungeLaneIsCappedAndReachesPastTheTarget() {
        assertEquals(32.0, MonarchRules.LUNGE_MAX, "a hard cap on the dash");
        assertEquals(MonarchRules.LUNGE_MAX, MonarchRules.lungeLength(100.0));
        assertEquals(MonarchRules.LUNGE_MAX, MonarchRules.lungeLength(24.0));
        assertEquals(20.0, MonarchRules.lungeLength(12.0), 1e-9, "past the target by eight blocks");
        assertTrue(MonarchRules.lungeLength(0.0) > MonarchRules.LUNGE_MIN_RUN, "a target right under it still gets a run");
        for (double d = 0; d < 80; d += 0.7) {
            double len = MonarchRules.lungeLength(d);
            assertTrue(len <= 32.0 && len >= MonarchRules.LUNGE_MIN_RUN, "length " + len + " for " + d);
            assertTrue(MonarchRules.lungeTicks(len) * MonarchRules.LUNGE_SPEED >= len - 1e-9);
            assertTrue(MonarchRules.lungeTicks(len) <= 16, "a dash lasts at most 16 ticks");
        }
        assertEquals(16, MonarchRules.lungeTicks(32.0));
        assertEquals(0, MonarchRules.lungeTicks(-3.0));
        // freeze covers the longest dash, so the Commander is never released in mid-air
        assertEquals(MonarchRules.LUNGE_WINDUP + 16 + MonarchRules.RECOVERY, MonarchRules.freeze(MonarchMove.LUNGE, 1));
        assertEquals(MonarchRules.freeze(MonarchMove.LUNGE, 1), MonarchRules.freeze(MonarchMove.LUNGE, 3));
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
        float worst = 0f;
        for (MonarchMove m : MonarchMove.values()) worst = Math.max(worst, MonarchRules.hitShare(m, 3));
        assertEquals(MonarchRules.hitShare(MonarchMove.BEAM, 3), worst, "Soul Rend is still the hardest single blow");
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
            assertTrue(MonarchRules.capMelee(36f, max, true) < max, "the Commander's blow on " + max);
            assertTrue(MonarchRules.capMelee(1e6f, max, true) <= max * MonarchRules.SHARE_CEILING + 1e-4f);
            assertTrue(MonarchRules.capMelee(1e6f, max, false) < MonarchRules.capMelee(1e6f, max, true), "a soldier hits softer than the Commander");
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
        assertEquals(12, MonarchRules.HIT_GRACE, "the grace per victim stays");
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
        assertEquals(4, MonarchRules.minionCap(1), "the caps stay 4, 6 and 8");
        assertEquals(6, MonarchRules.minionCap(2));
        assertTrue(MonarchRules.minionCap(1) < MonarchRules.minionCap(2) && MonarchRules.minionCap(2) < MonarchRules.minionCap(3), "more in each phase");
        assertEquals(0, MonarchRules.summonCount(3, 8));
        assertEquals(0, MonarchRules.greaterAllowed(1));
        assertTrue(MonarchRules.greaterAllowed(3) > 0);
        assertTrue(MonarchRules.soldierHealth(3) > MonarchRules.soldierHealth(1));
    }

    @Test
    void theSummoningsComeAtHalfAndAQuarterAndEachBringsItsFullBatch() {
        // the thresholds: the phase turning to 2 or to 3 calls the shadows, whatever stands about
        assertTrue(MonarchRules.summonDue(1, 2, 0, 0));
        assertTrue(MonarchRules.summonDue(1, 2, 4, 10));
        assertTrue(MonarchRules.summonDue(2, 3, 0, 0));
        assertTrue(MonarchRules.summonDue(2, 3, 6, 10), "even with soldiers still alive");
        assertTrue(MonarchRules.summonDue(1, 3, 0, 0), "a turn that somehow skipped a phase is still one summoning");
        // nothing at the start, nothing when the phase goes back, nothing while it stays in phase 1 or 2
        assertFalse(MonarchRules.summonDue(1, 1, 0, 100000));
        assertFalse(MonarchRules.summonDue(2, 2, 0, 100000), "the refill is a last-phase thing");
        assertFalse(MonarchRules.summonDue(3, 2, 0, 100000));
        assertFalse(MonarchRules.summonDue(2, 1, 0, 100000));
        // the refill: phase 3, every soldier dead, 25 seconds since the last summoning
        assertEquals(500, MonarchRules.REFILL_AFTER, "25 seconds");
        assertFalse(MonarchRules.summonDue(3, 3, 0, 499));
        assertTrue(MonarchRules.summonDue(3, 3, 0, 500));
        assertTrue(MonarchRules.summonDue(3, 3, 0, 9999));
        assertFalse(MonarchRules.summonDue(3, 3, 1, 9999), "not while one soldier stands");
        assertFalse(MonarchRules.summonDue(3, 3, 8, 9999));
        // bounded: two thresholds and one refill, and no more
        assertEquals(3, MonarchRules.MAX_SUMMONINGS);
        assertTrue(MonarchRules.mayRefill(2), "after the two thresholds one refill is left");
        assertFalse(MonarchRules.mayRefill(3));
        assertFalse(MonarchRules.mayRefill(7));
        // the batches: 3 at half the bar and 4 at a quarter, both whole, never past the cap of 8 together
        int first = MonarchRules.summonCount(2, 0), second = MonarchRules.summonCount(3, first);
        assertEquals(MonarchRules.summonBatch(2), first, "the first summoning brings its full batch");
        assertEquals(MonarchRules.summonBatch(3), second, "and so does the second, with the first troop still standing");
        assertTrue(first + second <= MonarchRules.minionCap(3), "7 of 8");
        assertEquals(MonarchRules.summonBatch(3), MonarchRules.summonCount(3, 0), "a refill of an empty field is a full batch too");
        // the whole fight, a blow at a time: exactly two summonings are due before the refill rule can matter
        float max = 1000f, hp = max;
        int phase = 1, due = 0;
        for (int i = 0; i < 300 && hp > 0; i++) {
            hp -= MonarchRules.gate(hp, max, MonarchRules.realDamage(1e9f, max), phase);
            int now = MonarchRules.phaseOf(hp, max);
            if (MonarchRules.summonDue(phase, now, 0, 0)) due++;
            phase = now;
        }
        assertEquals(2, due, "one at 50% and one at 25%");
    }

    @Test
    void theFightCallsTheShadowsTwiceAndRefillsAnEmptyFieldAtMostOnce() {
        // a pure model of the fight's bookkeeping: thresholds as the health falls, the shadows all killed at once, a tick at a time
        for (int deathDelay : new int[]{0, 40, 400, 5000}) {
            int phase = 1, summons = 0, alive = 0;
            long last = 0, deadSince = Long.MAX_VALUE;
            int summonings = 0, refills = 0;
            for (long now = 0; now < 100000; now++) {
                if (now == 1000 || now == 4000) {                                  // half the bar, then a quarter of it
                    int to = phase + 1;
                    if (MonarchRules.summonDue(phase, to, alive, now - last)) {
                        phase = to;
                        alive += MonarchRules.summonCount(phase, alive);
                        summons++;
                        summonings++;
                        last = now;
                        deadSince = now + deathDelay;
                    } else {
                        phase = to;
                    }
                    continue;
                }
                if (alive > 0 && now >= deadSince) alive = 0;                      // the players kill them all
                if (now % 10 == 0 && MonarchRules.refillDue(phase, summons, alive, now - last)) {
                    alive += MonarchRules.summonCount(phase, alive);
                    summons++;
                    summonings++;
                    refills++;
                    last = now;
                    deadSince = now + deathDelay;
                }
                assertTrue(alive <= MonarchRules.minionCap(phase), "never past the cap");
            }
            assertEquals(1, refills, "one refill, however long the fight goes on (delay " + deathDelay + ")");
            assertEquals(MonarchRules.MAX_SUMMONINGS, summonings, "three summonings in all");
            assertEquals(3, summons);
        }
        // never before the last phase, never while a soldier lives, never early
        assertFalse(MonarchRules.refillDue(2, 2, 0, 99999));
        assertFalse(MonarchRules.refillDue(3, 2, 1, 99999));
        assertFalse(MonarchRules.refillDue(3, 2, 0, 499));
        assertTrue(MonarchRules.refillDue(3, 2, 0, 500));
        assertFalse(MonarchRules.refillDue(3, 3, 0, 99999), "the allowance is spent");
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
        assertTrue(MonarchRules.SWEEP_REACH >= MonarchRules.SLAM_REACH, "the arc catches those on the steps of the dais too");
    }
}
