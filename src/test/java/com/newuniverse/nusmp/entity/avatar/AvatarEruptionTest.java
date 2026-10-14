package com.newuniverse.nusmp.entity.avatar;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 1.35.0: the Brimstone Eruption (spokes of brimstone), its table row and the Avatar's buffed build. */
class AvatarEruptionTest {
    private static final double HALF = AvatarMath.ERUPT_HALF_WIDTH;

    @Test
    void aSpokeCoversItsAxisAndNotTheGapBetweenTwoSpokes() {
        double base = 0.7;
        for (int n : new int[]{5, 7}) {
            for (int k = 0; k < n; k++) {
                double a = base + Math.PI * 2 * k / n;
                assertTrue(AvatarMath.inSpoke(Math.cos(a) * 8, Math.sin(a) * 8, base, n, HALF, AvatarMath.ERUPT_LENGTH), "on the axis of spoke " + k + " of " + n);
                assertTrue(AvatarMath.inSpoke(Math.cos(a) * AvatarMath.ERUPT_LENGTH, Math.sin(a) * AvatarMath.ERUPT_LENGTH, base, n, HALF, AvatarMath.ERUPT_LENGTH), "at the tip");
                assertFalse(AvatarMath.inSpoke(Math.cos(a) * (AvatarMath.ERUPT_LENGTH + HALF + 1), Math.sin(a) * (AvatarMath.ERUPT_LENGTH + HALF + 1), base, n, HALF, AvatarMath.ERUPT_LENGTH), "past the tip");
                double mid = a + Math.PI / n;
                assertFalse(AvatarMath.inSpoke(Math.cos(mid) * 10, Math.sin(mid) * 10, base, n, HALF, AvatarMath.ERUPT_LENGTH), "the middle of a gap at 10 blocks");
            }
        }
        assertTrue(AvatarMath.inSpoke(0, 0, base, 5, HALF, 15), "the Avatar's own feet are where the spokes meet");
        assertTrue(AvatarMath.inSpoke(0.5, -0.5, base, 5, HALF, 15));
    }

    @Test
    void theSecondWaveStandsInTheGapsOfTheFirst() {
        // the Gates: a second wave turned by half a step. Every direction is covered by at most one of the waves at 10 blocks, so a foe that left the first can not be in the second
        int n = AvatarMath.eruptionSpokes(true);
        double base = 1.1, second = base + Math.PI / n;
        int bothHit = 0, anyHit = 0, samples = 0;
        for (double a = 0; a < Math.PI * 2; a += 0.01) {
            boolean one = AvatarMath.inSpoke(Math.cos(a) * 10, Math.sin(a) * 10, base, n, HALF, AvatarMath.ERUPT_LENGTH);
            boolean two = AvatarMath.inSpoke(Math.cos(a) * 10, Math.sin(a) * 10, second, n, HALF, AvatarMath.ERUPT_LENGTH);
            samples++;
            if (one && two) bothHit++;
            if (one || two) anyHit++;
        }
        assertEquals(0, bothHit, "no direction at 10 blocks is struck by both waves");
        assertTrue(anyHit < samples, "and the two waves together still leave ground to stand on");
    }

    @Test
    void thereIsAlwaysRoomToStandInAGapOutsideTheCentre() {
        for (boolean gates : new boolean[]{false, true}) {
            int n = AvatarMath.eruptionSpokes(gates);
            assertEquals(gates ? 7 : 5, n);
            assertTrue(AvatarMath.gapAt(6, n, HALF) >= 1.2, "a player (0.6 wide) fits a gap 6 blocks out at n=" + n + ": " + AvatarMath.gapAt(6, n, HALF));
            assertTrue(AvatarMath.gapAt(10, n, HALF) > AvatarMath.gapAt(6, n, HALF), "gaps widen with distance");
            assertTrue(AvatarMath.gapAt(0, n, HALF) == 0.0, "never negative");
        }
        assertTrue(AvatarMath.gapAt(5, 5, HALF) >= 1.2, "the Avatar keeps 5 to 10 blocks from its foe: the opening wave has room there");
    }

    @Test
    void theEruptionIsAWellFormedMoveBeforeTheUltimate() {
        for (int v : new int[]{AvatarTable.LUCIFERO, AvatarTable.MEGICULA}) {
            List<AvatarTable.Spec> specs = AvatarTable.specs(v);
            AvatarTable.Spec e = AvatarTable.byId(v, AvatarTable.BRIMSTONE_ERUPTION);
            assertNotNull(e);
            assertEquals(60, e.minPower(), "from 60% power");
            assertTrue(Math.round(e.windUpTicks() * 0.75f) >= 14, "even at the Gates the wind-up (" + Math.round(e.windUpTicks() * 0.75f) + " ticks) is long enough to read the spokes");
            assertTrue(e.recovery() < 40 && !e.ultimate());
            assertTrue(specs.indexOf(e) == specs.size() - 2, "just before the ultimate, which still closes the list");
            assertSame(AvatarTable.byId(v, AvatarTable.ABYSSAL_COLLAPSE), specs.get(specs.size() - 1));
            assertFalse(AvatarTable.isGravity(e.id()) || AvatarTable.isCurse(e.id()), "both Avatars use it alike");
        }
        assertEquals(AvatarTable.byId(AvatarTable.LUCIFERO, AvatarTable.BRIMSTONE_ERUPTION).weight(), AvatarTable.byId(AvatarTable.MEGICULA, AvatarTable.BRIMSTONE_ERUPTION).weight());
        for (int v : new int[]{AvatarTable.LUCIFERO, AvatarTable.MEGICULA}) {
            String[] lines = AvatarText.quips(v, AvatarTable.BRIMSTONE_ERUPTION);
            assertTrue(lines.length >= 2);
            for (String l : lines) assertFalse(l.isBlank() || l.equals("..."));
        }
    }

    @Test
    void theAvatarIsToughenedAndGetsAFourthWaveOfDisciples() {
        assertTrue(AvatarTable.HEALTH > 900, "buffed from 900");
        assertTrue(AvatarTable.GATE_WAVES.length >= 4);
        for (int i = 1; i < AvatarTable.GATE_WAVES.length; i++) assertTrue(AvatarTable.GATE_WAVES[i] > AvatarTable.GATE_WAVES[i - 1], "waves in order");
        assertEquals(80, AvatarTable.GATE_WAVES[0]);
        assertEquals(260, AvatarTable.GATE_WAVES[1]);
        assertEquals(460, AvatarTable.GATE_WAVES[2]);
    }
}
