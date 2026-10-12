package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.entity.zogratis.Precognition.Verdict.*;
import static org.junit.jupiter.api.Assertions.*;

/** Lucius's pre-dodge charges: spent one by one, recharged, and beaten by out-of-sequence blows, anti-magic and empty charges. */
class PrecognitionTest {
    private static Precognition fresh(int power) {
        Precognition p = new Precognition();
        p.reset(power, 0);
        return p;
    }

    @Test
    void chargesGrowWithThePower() {
        assertEquals(1, Precognition.maxCharges(20));
        assertEquals(1, Precognition.maxCharges(39));
        assertEquals(2, Precognition.maxCharges(40));
        assertEquals(2, Precognition.maxCharges(79));
        assertEquals(3, Precognition.maxCharges(80));
        assertEquals(3, Precognition.maxCharges(100));
        assertTrue(Precognition.maxCharges(100) <= Precognition.MAX_CHARGES);
        int last = Integer.MAX_VALUE;
        for (int pw = 20; pw <= 100; pw += 20) {
            assertTrue(Precognition.rechargeTicks(pw) <= last, "recharges faster with power");
            last = Precognition.rechargeTicks(pw);
        }
    }

    @Test
    void aReadableBlowIsDodgedAndCostsACharge() {
        Precognition p = fresh(100);
        assertEquals(3, p.charges());
        assertEquals(DODGED, p.onHit(100, 7, false, 100));
        assertEquals(2, p.charges());
        assertEquals(DODGED, p.onHit(300, 7, false, 100), "the same attacker again, well after the window");
        assertEquals(1, p.charges());
    }

    @Test
    void whenTheChargesRunOutTheBlowLands() {
        Precognition p = fresh(20);
        assertEquals(DODGED, p.onHit(100, 1, false, 20));
        assertEquals(EMPTY, p.onHit(120, 1, false, 20));
        assertEquals(0, p.charges());
    }

    @Test
    void aDifferentAttackerInsideTheWindowIsOutOfSequence() {
        Precognition p = fresh(100);
        assertEquals(DODGED, p.onHit(100, 1, false, 100));
        assertEquals(OUT_OF_SEQUENCE, p.onHit(100 + Precognition.SEQUENCE_WINDOW, 2, false, 100), "inside the window");
        assertEquals(2, p.charges(), "an out-of-sequence blow costs him nothing");
        assertEquals(OUT_OF_SEQUENCE, p.onHit(105, 3, false, 100), "and again, for anyone but the first");
        assertEquals(DODGED, p.onHit(100 + Precognition.SEQUENCE_WINDOW + 1, 2, false, 100), "after the window the second one is read too");
    }

    @Test
    void theSameAttackerIsNotOutOfSequence() {
        Precognition p = fresh(100);
        assertEquals(DODGED, p.onHit(100, 1, false, 100));
        assertEquals(DODGED, p.onHit(103, 1, false, 100), "a flurry from one hand is read");
        assertEquals(DODGED, p.onHit(106, 1, false, 100));
        assertEquals(0, p.charges());
        assertEquals(EMPTY, p.onHit(109, 1, false, 100));
    }

    @Test
    void antiMagicAlwaysLandsAndBurnsACharge() {
        Precognition p = fresh(100);
        assertEquals(ANTI_MAGIC, p.onHit(100, 5, true, 100));
        assertEquals(2, p.charges());
        assertEquals(ANTI_MAGIC, p.onHit(101, 5, true, 100));
        assertEquals(1, p.charges());
        assertEquals(ANTI_MAGIC, p.onHit(102, 5, true, 100));
        assertEquals(ANTI_MAGIC, p.onHit(103, 5, true, 100), "with none left it still lands and nothing goes below zero");
        assertEquals(0, p.charges());
    }

    @Test
    void aBlowWithNoAttackerLandsAndSpendsNothing() {
        Precognition p = fresh(100);
        assertEquals(NO_ATTACKER, p.onHit(100, -1, false, 100));
        assertEquals(NO_ATTACKER, p.onHit(100, -1, true, 100), "poison is poison, whatever it is");
        assertEquals(3, p.charges());
    }

    @Test
    void suspendedPrecognitionLandsEverything() {
        Precognition p = fresh(100);
        p.suspend(500);
        assertTrue(p.off(499));
        assertFalse(p.off(500));
        assertEquals(OFF, p.onHit(200, 1, false, 100));
        assertEquals(OFF, p.onHit(200, 1, true, 100));
        assertEquals(3, p.charges());
        assertEquals(DODGED, p.onHit(500, 1, false, 100), "back on at the end");
        p.suspend(100);
        assertFalse(p.off(500), "a suspension that is already over changes nothing");
        p.suspend(900);
        p.suspend(700);
        assertTrue(p.off(899), "a later call can only extend");
    }

    @Test
    void chargesComeBackOneAtATime() {
        Precognition p = fresh(100);
        int gap = Precognition.rechargeTicks(100);
        p.onHit(1000, 1, false, 100);
        p.onHit(1100, 1, false, 100);
        p.onHit(1200, 1, false, 100);
        assertEquals(0, p.charges());
        p.tick(1200 + gap - 1, 100);
        assertEquals(0, p.charges());
        p.tick(1200 + gap + 1, 100);
        assertEquals(1, p.charges(), "one, not all");
        p.tick(1200 + gap + 2, 100);
        assertEquals(1, p.charges());
        p.tick(1200 + 2 * gap + 3, 100);
        assertEquals(2, p.charges());
        p.tick(1200 + 3 * gap + 4, 100);
        assertEquals(3, p.charges());
        p.tick(1200 + 9 * gap, 100);
        assertEquals(3, p.charges(), "never past the cap");
    }

    @Test
    void aSuspensionPausesTheRecharge() {
        Precognition p = fresh(100);
        p.onHit(100, 1, false, 100);
        int gap = Precognition.rechargeTicks(100);
        p.suspend(100 + 5 * gap);
        p.tick(100 + 2 * gap, 100);
        assertEquals(2, p.charges(), "nothing returns while it is off");
        p.tick(100 + 5 * gap, 100);
        assertEquals(2, p.charges(), "the recharge restarts at the end of the suspension");
        p.tick(100 + 6 * gap + 5, 100);
        assertEquals(3, p.charges());
    }

    @Test
    void aNewPowerCapsOrRefills() {
        Precognition p = fresh(100);
        p.tick(10, 20);
        assertEquals(1, p.charges(), "a lower cap applies");
        p.refill(60, 20);
        assertEquals(2, p.charges());
        p.grant(60);
        assertEquals(2, p.charges(), "grant never passes the cap");
        Precognition q = fresh(100);
        q.onHit(10, 1, false, 100);
        q.grant(100);
        assertEquals(3, q.charges());
    }

    @Test
    void ticksToChargeCountsDown() {
        Precognition p = fresh(100);
        assertEquals(0, p.ticksToCharge(0, 100), "full: nothing to wait for");
        p.onHit(100, 1, false, 100);
        assertEquals(Precognition.rechargeTicks(100), p.ticksToCharge(100, 100));
        assertEquals(Precognition.rechargeTicks(100) - 30, p.ticksToCharge(130, 100));
    }

    @Test
    void aSwarmOfDifferentAttackersGetsMostBlowsThrough() {
        Precognition p = fresh(100);
        int landed = 0, dodged = 0;
        for (int i = 0; i < 10; i++) {
            Precognition.Verdict v = p.onHit(100 + i, 1 + i, false, 100);
            if (v == DODGED) dodged++;
            else landed++;
        }
        assertEquals(1, dodged, "only the first of a simultaneous swarm is read");
        assertEquals(9, landed);
    }
}
