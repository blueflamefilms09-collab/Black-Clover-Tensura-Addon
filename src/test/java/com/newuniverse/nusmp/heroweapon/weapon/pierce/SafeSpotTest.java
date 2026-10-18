package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: a blink or a rewind that lands the wielder in a wall is the worst thing these techniques can do, and the rule (the wanted spot if it is fine, else
 * the nearest fine spot within a small radius, else nothing) is pure, so it is tested against small hand-built worlds: a free spot, a blocked spot with a
 * neighbour, a spot beyond the radius, a tie, and the order independence.
 */
class SafeSpotTest {
    private static SafeSpot.Probe world(int... xyz) {
        Set<String> ok = new HashSet<>();
        for (int i = 0; i + 2 < xyz.length; i += 3) ok.add(xyz[i] + "," + xyz[i + 1] + "," + xyz[i + 2]);
        return (x, y, z) -> ok.contains(x + "," + y + "," + z);
    }

    @Test
    void theWantedSpotWinsWhenItIsFine() {
        SafeSpot.Spot s = SafeSpot.nearest(world(5, 64, 5, 6, 64, 5), 5, 64, 5, 3.0);
        assertEquals(new SafeSpot.Spot(5, 64, 5), s);
    }

    @Test
    void aBlockedSpotFallsBackToTheNearestFineNeighbour() {
        SafeSpot.Spot s = SafeSpot.nearest(world(7, 64, 5, 5, 64, 8), 5, 64, 5, 3.0);
        assertEquals(new SafeSpot.Spot(7, 64, 5), s, "two blocks away beats three");
    }

    @Test
    void nothingBeyondTheRadiusIsAccepted() {
        assertNull(SafeSpot.nearest(world(9, 64, 5), 5, 64, 5, 3.0), "four blocks away is too far");
        assertNull(SafeSpot.nearest(world(8, 64, 5), 5, 64, 5, 2.9), "three blocks is out of a 2.9 radius");
        assertNotNull(SafeSpot.nearest(world(8, 64, 5), 5, 64, 5, 3.0), "three blocks is inside a 3.0 radius");
        assertNull(SafeSpot.nearest(world(5, 66, 5, 5, 62, 5), 5, 64, 5, 1.9), "straight up or down counts the same");
    }

    @Test
    void anEmptyWorldGivesNothing() {
        assertNull(SafeSpot.nearest((x, y, z) -> false, 0, 64, 0, 3.0));
    }

    @Test
    void aTieGoesToTheSmallerVerticalStepThenToTheSmallerX() {
        // (6,64,5) and (5,65,5) are both one block away: the level one wins
        assertEquals(new SafeSpot.Spot(6, 64, 5), SafeSpot.nearest(world(5, 65, 5, 6, 64, 5), 5, 64, 5, 3.0));
        // (4,64,5) and (6,64,5) are both one block away on the level: the smaller dx wins
        assertEquals(new SafeSpot.Spot(4, 64, 5), SafeSpot.nearest(world(6, 64, 5, 4, 64, 5), 5, 64, 5, 3.0));
    }

    @Test
    void theAnswerDoesNotDependOnTheOrderTheProbeIsAsked() {
        SafeSpot.Probe p = world(7, 65, 5, 3, 64, 6, 5, 64, 8, 5, 61, 5);
        SafeSpot.Spot first = SafeSpot.nearest(p, 5, 64, 5, 3.0);
        SafeSpot.Spot second = SafeSpot.nearest(p, 5, 64, 5, 3.0);
        assertEquals(first, second);
        assertEquals(new SafeSpot.Spot(3, 64, 6), first, "(7,65,5) and (3,64,6) are both sqrt(5) away; the level one wins; (5,64,8) and (5,61,5) are three away");
    }

    @Test
    void aZeroRadiusAsksOnlyTheWantedBlock() {
        int[] asked = {0};
        SafeSpot.nearest((x, y, z) -> { asked[0]++; return false; }, 1, 2, 3, 0.0);
        assertEquals(1, asked[0]);
        assertTrue(SafeSpot.nearest(world(1, 2, 3), 1, 2, 3, 0.0) != null);
    }
}
