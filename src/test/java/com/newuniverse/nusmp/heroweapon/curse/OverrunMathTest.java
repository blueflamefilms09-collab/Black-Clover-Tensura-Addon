package com.newuniverse.nusmp.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: an Overrun swings at the nearest living thing in reach every ten ticks and must not be able to one-shot a friend. The choice of the target and the cap on a
 * blow against another player are plain arithmetic and are pinned here.
 */
class OverrunMathTest {
    @Test
    void theNearestInReachIsChosenAndTiesGoToTheFirst() {
        List<double[]> things = List.of(new double[]{9.0}, new double[]{4.0}, new double[]{4.0}, new double[]{25.0});
        double[] best = OverrunMath.nearest(things, t -> t[0], 20.25);
        assertEquals(4.0, best[0], 1e-9);
        assertTrue(best == things.get(1), "the first of two equally near");
        assertNull(OverrunMath.nearest(things, t -> t[0], 3.0), "all too far");
        assertNull(OverrunMath.nearest(List.<double[]>of(), t -> t[0], 100.0), "nothing around");
        assertTrue(OverrunMath.nearest(things, t -> t[0], 4.0) == things.get(1), "exactly at the edge of reach is inside");
    }

    @Test
    void aBlowOnAnotherPlayerIsCutToAShareOfTheirHealth() {
        assertEquals(2.0f, OverrunMath.playerCap(50f, 20f, 0.10), 1e-6f);
        assertEquals(1.5f, OverrunMath.playerCap(1.5f, 20f, 0.10), 1e-6f, "a small blow is not raised");
        assertEquals(1.0f, OverrunMath.playerCap(50f, 4f, 0.10), 1e-6f, "the cap is at least one health point");
        assertEquals(0f, OverrunMath.playerCap(-3f, 20f, 0.10), 1e-6f);
        assertEquals(1.0f, OverrunMath.playerCap(50f, 20f, 0.0), 1e-6f, "a share of zero still lets the one-point minimum through");
        assertEquals(20f, OverrunMath.playerCap(50f, 20f, 1.0), 1e-6f);
        // two swings a second at the default share: a 20 health friend loses a fifth of it every second, so five seconds of standing still, not one
        float health = 20f;
        for (int swing = 0; swing < 4; swing++) health -= OverrunMath.playerCap(1000f, 20f, 0.10);
        assertTrue(health > 10f, "after two seconds of swings the friend is still on their feet: " + health);
    }
}
