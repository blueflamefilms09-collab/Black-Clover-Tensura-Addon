package com.newuniverse.nusmp.dungeon.floor.goliath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.35.0: the cracked Goliath (the Gates): a fork of three lines, a third stomp, the fist's aftershock, one more rubble circle, and its tougher build. */
class GoliathEnragedTest {
    @Test
    void theForkIsThreeLinesAtTheGatesAndOneBefore() {
        assertArrayEquals(new double[]{0.0}, GoliathMath.forkAngles(false));
        double[] fork = GoliathMath.forkAngles(true);
        assertEquals(3, fork.length);
        assertEquals(0.0, fork[0], "the first line is the one the wind-up aimed");
        assertEquals(-fork[1], fork[2], 1e-12, "the side lines are symmetric");
    }

    @Test
    void theForkLeavesRoomToStepBetweenTheLinesFromTenBlocksOut() {
        double angle = GoliathTable.FORK_ANGLE;
        double chord10 = 2 * 10 * Math.sin(angle / 2), chord20 = 2 * 20 * Math.sin(angle / 2);
        double gap10 = chord10 - 2 * GoliathTable.LINE_HALF_WIDTH, gap20 = chord20 - 2 * GoliathTable.LINE_HALF_WIDTH;
        assertTrue(gap10 >= 1.8, "a gap a player can stand in 10 blocks along the lines: " + gap10);
        assertTrue(gap20 > gap10 + 3, "and it widens");
        assertTrue(angle > 0.2 && angle < 1.0, "a fork, not a fan");
    }

    @Test
    void aRotatedLineKeepsItsLengthAndItsStart() {
        for (double yaw : new double[]{0.0, 0.55, -0.55, 2.0}) {
            double[] r = GoliathMath.rotate(10, 0, yaw);
            assertEquals(10.0, Math.hypot(r[0], r[1]), 1e-9);
        }
        double[] none = GoliathMath.rotate(3, 4, 0);
        assertEquals(3.0, none[0], 1e-12);
        assertEquals(4.0, none[1], 1e-12);
        double[] quarter = GoliathMath.rotate(1, 0, Math.PI / 2);
        assertEquals(0.0, quarter[0], 1e-12);
        assertEquals(1.0, quarter[1], 1e-12);
    }

    @Test
    void theStompHasThreeRingsAtTheGatesAndTheBudgetStillBoundsThem() {
        assertEquals(2, GoliathMath.stompRings(false));
        assertEquals(3, GoliathMath.stompRings(true));
        GoliathMath.DamageBudget b = new GoliathMath.DamageBudget(GoliathTable.STOMP_BUDGET);
        double spent = 0;
        for (float hit : new float[]{GoliathTable.STOMP_FIRST, GoliathTable.STOMP_SECOND, GoliathTable.STOMP_THIRD}) {
            double part = b.allow(1, 20.0, hit * 2);
            b.spend(1, hit * 2 * part);
            spent += hit * 2 * part;
        }
        assertTrue(spent <= GoliathTable.STOMP_BUDGET * 20.0 + 1e-9, "three rings take no more than the share: " + spent);
    }

    @Test
    void theRubbleHasOneMoreCircleAtTheGatesButNeverMoreThanEight() {
        for (int foes = 0; foes <= 12; foes++) {
            for (int slots = 1; slots <= 4; slots++) {
                int plain = GoliathMath.rubbleCount(foes, slots), gates = GoliathMath.rubbleCount(foes, slots, true);
                assertEquals(plain, GoliathMath.rubbleCount(foes, slots, false));
                assertTrue(gates >= plain && gates <= plain + 1 && gates <= 8, foes + "/" + slots);
            }
        }
        assertEquals(8, GoliathMath.rubbleCount(20, 4, true));
        assertEquals(4, GoliathMath.rubbleCount(0, 1, true));
    }

    @Test
    void theAftershockIsSmallerThanTheSlamAndTheBuildIsTougher() {
        assertTrue(GoliathTable.AFTERSHOCK_REACH < GoliathTable.SLAM_RADIUS);
        assertTrue(GoliathTable.AFTERSHOCK < GoliathTable.FIST);
        assertTrue(GoliathTable.AFTERSHOCK_DELAY >= 6, "the foes can step back before the second ring");
        assertTrue(GoliathTable.STOMP_THIRD < GoliathTable.STOMP_FIRST);
        assertTrue(GoliathTable.HEALTH > 1500 && GoliathTable.ATTACK >= 30);
        assertEquals(GoliathTable.BEDROCK_QUAKE, GoliathTable.specs().get(GoliathTable.specs().size() - 1).id(), "the ultimate still closes the list");
    }
}
