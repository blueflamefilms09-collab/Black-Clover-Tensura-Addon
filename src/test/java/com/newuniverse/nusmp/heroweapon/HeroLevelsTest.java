package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import org.junit.jupiter.api.Test;

/**
 * Why: the level curve decides how strong every skill is and how fast the cooldowns get, so its edges are pinned: level 0 is the floor, the curve rises
 * strictly, the inverse of the curve agrees with the curve at every level, the level never passes the maximum, and the power and cooldown factors have the
 * documented ends.
 */
class HeroLevelsTest {
    @Test
    void theCurveRisesStrictlyAndTheInverseAgrees() {
        long last = -1;
        for (int l = 0; l <= HeroLevels.MAX; l++) {
            long e = HeroLevels.expFor(l);
            assertTrue(e > last, "level " + l + " must need more than level " + (l - 1));
            last = e;
            assertEquals(l, HeroLevels.level(e), "exactly at the threshold of level " + l);
            if (l > 0) assertEquals(l - 1, HeroLevels.level(e - 1), "one point below the threshold of level " + l);
        }
    }

    @Test
    void edgesAreClamped() {
        assertEquals(0, HeroLevels.level(-5));
        assertEquals(0, HeroLevels.level(0));
        assertEquals(HeroLevels.MAX, HeroLevels.level(Long.MAX_VALUE / 4));
        assertEquals(0L, HeroLevels.expFor(-3));
        assertEquals(HeroLevels.expFor(HeroLevels.MAX), HeroLevels.expFor(HeroLevels.MAX + 50));
    }

    @Test
    void powerAndCooldownHaveTheirDocumentedEnds() {
        assertEquals(1.0, HeroLevels.power(0), 1e-9);
        assertEquals(2.2, HeroLevels.power(100), 1e-9);
        assertEquals(2.2, HeroLevels.power(5000), 1e-9);
        assertEquals(1.0, HeroLevels.cooldownScale(0), 1e-9);
        assertEquals(0.75, HeroLevels.cooldownScale(100), 1e-9);
        assertEquals(0.75, HeroLevels.cooldownScale(900), 1e-9);
    }

    @Test
    void progressIsAShareOfTheWayToTheNextLevel() {
        assertEquals(0.0, HeroLevels.progress(0), 1e-9);
        long mid = (HeroLevels.expFor(10) + HeroLevels.expFor(11)) / 2;
        double p = HeroLevels.progress(mid);
        assertTrue(p > 0.4 && p < 0.6, "got " + p);
        assertEquals(1.0, HeroLevels.progress(HeroLevels.expFor(HeroLevels.MAX) + 1000), 1e-9);
    }
}
