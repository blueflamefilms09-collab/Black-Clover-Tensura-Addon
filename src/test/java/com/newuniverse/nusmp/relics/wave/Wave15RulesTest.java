package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.wave.Wave15Rules.Charges;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 15: the pure rules of the footwear, cloaks and bracers. */
class Wave15RulesTest {
    @Test
    void anUntouchedPoolIsFullAndSpendingLeavesTheRest() {
        Charges c = new Charges(5, 5, 0L).at(10, 3);
        assertEquals(5, c.left());
        assertTrue(c.canSpend(3));
        c = c.spend(3);
        assertEquals(2, c.left());
        assertFalse(c.canSpend(3), "two charges cannot pay for three");
        assertEquals(2, c.spend(3).left(), "a refused spend changes nothing");
        assertFalse(c.canSpend(0));
    }

    @Test
    void chargesComeBackAtDawnNotBefore() {
        Charges c = new Charges(5, 5, 0L).at(10, 3).spend(5);
        assertEquals(0, c.left());
        assertEquals(0, c.at(10, 3).left(), "same day: nothing yet");
        assertEquals(3, c.at(11, 3).left(), "next dawn: the roll comes back");
        assertEquals(5, c.at(12, 3).left(), "two dawns: full again, capped at the maximum");
        assertEquals(5, c.at(400, 5).left());
        assertEquals(0, c.at(9, 3).left(), "a clock that runs backwards gives nothing");
    }

    @Test
    void theDawnRollIsOneDFourPlusOne() {
        for (int roll = 0; roll < 4; roll++) assertTrue(Wave15Rules.dawnGain(roll) >= 2 && Wave15Rules.dawnGain(roll) <= 5);
        assertEquals(2, Wave15Rules.dawnGain(-9));
        assertEquals(5, Wave15Rules.dawnGain(99));
        assertEquals(Wave15Rules.ELVEN_MAX, Wave15Rules.INVISIBILITY_COST + Wave15Rules.DARKNESS_COST, "all three spells fit in one pool of five");
    }

    @Test
    void aWallSlipLandsOnTheFirstPlaceToStandBehindTheWall() {
        int n = Wave15Rules.SLIP_STEPS + 1;
        boolean[] blocked = new boolean[n], stand = new boolean[n];
        for (int i = 1; i <= 3; i++) blocked[i] = true;                  // a wall of three steps starting one step ahead
        stand[1] = stand[2] = false;
        for (int i = 4; i < n; i++) stand[i] = true;
        assertEquals(4, Wave15Rules.slipIndex(blocked, stand));
    }

    @Test
    void aWallSlipRefusesNoWallAFarWallAndAWallThatIsTooThick() {
        int n = Wave15Rules.SLIP_STEPS + 1;
        boolean[] blocked = new boolean[n], stand = new boolean[n];
        java.util.Arrays.fill(stand, true);
        assertEquals(-1, Wave15Rules.slipIndex(blocked, stand), "no wall: nothing to slip through");
        blocked[Wave15Rules.WALL_START_STEPS + 1] = true;
        assertEquals(-1, Wave15Rules.slipIndex(blocked, stand), "a wall too far away");
        blocked = new boolean[n];
        stand = new boolean[n];
        for (int i = 2; i < 15; i++) blocked[i] = true;                  // thirteen steps thick
        stand[16] = true;
        assertEquals(-1, Wave15Rules.slipIndex(blocked, stand), "too thick to cross");
        blocked = new boolean[n];
        stand = new boolean[n];
        for (int i = 2; i <= 6; i++) blocked[i] = true;                  // five steps thick: crossable
        stand[7] = true;
        assertEquals(7, Wave15Rules.slipIndex(blocked, stand));
        assertEquals(-1, Wave15Rules.slipIndex(new boolean[2], new boolean[3]), "tiny arrays are safe");
    }

    @Test
    void aWallSlipNeverLandsInsideTheWall() {
        int n = Wave15Rules.SLIP_STEPS + 1;
        boolean[] blocked = new boolean[n], stand = new boolean[n];
        blocked[2] = blocked[3] = blocked[4] = true;
        stand[3] = true;                                                 // marked standable but blocked: must not be chosen
        stand[6] = true;
        assertEquals(6, Wave15Rules.slipIndex(blocked, stand));
    }

    @Test
    void theFlurryTurnsAsideOneBlowASecondOnlyWhileItLasts() {
        long until = 1000;
        assertTrue(Wave15Rules.flurryEvades(500, until, 0));
        assertFalse(Wave15Rules.flurryEvades(505, until, 500), "a second blow in the same second lands");
        assertTrue(Wave15Rules.flurryEvades(520, until, 500));
        assertFalse(Wave15Rules.flurryEvades(1000, until, 0), "over");
        assertEquals(360, Wave15Rules.FLURRY_TICKS, "three rounds of six seconds");
    }

    @Test
    void theBlurMissesAQuarterOfTheBlowsForAMinute() {
        int missed = 0;
        for (int roll = 0; roll < 100; roll++) if (Wave15Rules.blurEvades(10, 100, roll)) missed++;
        assertEquals(25, missed);
        assertFalse(Wave15Rules.blurEvades(100, 100, 0), "over");
        assertEquals(1200, Wave15Rules.BLUR_TICKS);
    }

    @Test
    void aThrowFliesFartherForSmallerCreaturesAndNotAtAllForGiants() {
        assertEquals(1.0, Wave15Rules.throwPower(1.0));
        assertEquals(2.0, Wave15Rules.throwPower(0.2));
        assertEquals(0.5, Wave15Rules.throwPower(2.0));
        assertEquals(0.0, Wave15Rules.throwPower(5.0));
        assertEquals(0.5, Wave15Rules.throwPower(4.0), "four times is still liftable");
        assertTrue(Wave15Rules.throwPower(0.1) > Wave15Rules.throwPower(1.0));
    }

    @Test
    void theCapeOfProjectionReflectsAQuarterUpToSix() {
        assertEquals(2.0f, Wave15Rules.reflected(8f));
        assertEquals(6.0f, Wave15Rules.reflected(100f));
        assertEquals(0f, Wave15Rules.reflected(-3f));
    }

    @Test
    void aFriendsPullBackLeavesOneHitPointAndNeverSavesTheNearlyDead() {
        assertEquals(9.0f, Wave15Rules.stabilised(10f, 15f));
        assertEquals(9.0f, Wave15Rules.stabilised(10f, 10f), "exactly lethal is cut too");
        assertEquals(4f, Wave15Rules.stabilised(10f, 4f), "a blow that does not kill is untouched");
        assertEquals(30f, Wave15Rules.stabilised(1.5f, 30f), "no rescue from a hair's breadth: no endless 1 hp loop");
    }

    @Test
    void theCoinHasTwoSidesAndTheBondHasARange() {
        int heads = 0;
        for (int i = 0; i < 100; i++) if (Wave15Rules.coin(i)) heads++;
        assertEquals(50, heads);
        assertTrue(Wave15Rules.inBond(99.0));
        assertTrue(Wave15Rules.inBond(100.0));
        assertFalse(Wave15Rules.inBond(100.1));
    }

    @Test
    void dailyMarksResetAtDawn() {
        assertTrue(Wave15Rules.dailyReady(0, 5));
        long mark = Wave15Rules.dailyMark(5);
        assertFalse(Wave15Rules.dailyReady(mark, 5));
        assertTrue(Wave15Rules.dailyReady(mark, 6));
        assertEquals(5, Wave15Rules.dayOf(5 * 24000L + 12000));
        assertEquals(-1, Wave15Rules.dayOf(-1));
    }

    @Test
    void theSwarmCannotKillAPlayerOutright() {
        assertEquals(6.0f, Wave15Rules.swarmTotal());
        assertTrue(Wave15Rules.swarmTotal() <= 20f * 0.30f, "30 percent of a 20 hp player");
    }

    @Test
    void everyFlagIsDistinct() {
        String[] f = {Wave15Rules.F_ANY, Wave15Rules.F_BOOTS, Wave15Rules.F_TOWEL, Wave15Rules.F_BEES, Wave15Rules.F_SNOW, Wave15Rules.F_AUTUMN, Wave15Rules.F_ELVEN,
                Wave15Rules.F_LEVITATE, Wave15Rules.F_PROJECTION, Wave15Rules.F_MANTLE, Wave15Rules.F_PHASING, Wave15Rules.F_INITIATIVE, Wave15Rules.F_PHANTOM,
                Wave15Rules.F_MANHANDLING, Wave15Rules.F_BLADES, Wave15Rules.F_BEAR, Wave15Rules.F_FRIENDS, Wave15Rules.F_CREST};
        assertEquals(f.length, java.util.Arrays.stream(f).distinct().count());
    }
}
