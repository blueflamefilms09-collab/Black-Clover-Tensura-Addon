package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Gauntlet's techniques grow with the points they spend, so the budget has to be judged at the strongest point of each: every row passes the design's
 * budget, the strongest value the mechanics can reach (every point spent, an undead foe, the Spirit Bane passive) is exactly the row's {@code rawMax}, the
 * weakest is its {@code rawBase}, the ultimate's counted points cap where the pool does, and the ids are the gauntlet's and unique.
 */
class GauntletNumbersTest {
    @Test
    void everyTechniqueIsWithinTheBudget() {
        for (Budget.Spec s : GauntletNumbers.ALL) {
            List<String> bad = Budget.violations(s);
            assertTrue(bad.isEmpty(), bad.toString());
        }
        assertEquals(9, GauntletNumbers.ALL.size(), "the gauntlet has nine techniques (at least six are required)");
    }

    @Test
    void theStrongestValueIsTheRowsAndTheWeakestIsItsBase() {
        for (GauntletNumbers.Rank r : GauntletNumbers.RANKS) {
            assertEquals(r.spec().rawMax(), r.strongest(), 1e-9, r.spec().id() + ": rawMax is base + points * step + the Spirit Bane bonus");
            assertEquals(r.spec().rawBase(), r.raw(0, false), 1e-9, r.spec().id());
            assertEquals(r.raw(r.maxSpend(), false), r.raw(r.maxSpend() + 5, false), 1e-9, "points beyond the rule add nothing");
            assertEquals(r.raw(0, false), r.raw(-3, false), 1e-9, "negative points add nothing");
            assertTrue(r.maxSpend() >= 2 && r.perPoint() > 0.0);
        }
    }

    @Test
    void idsAreTheGauntletsAndUnique() {
        Set<String> seen = new HashSet<>();
        for (Budget.Spec s : GauntletNumbers.ALL) {
            assertTrue(s.id().startsWith("gauntlet."), s.id());
            assertTrue(seen.add(s.id()), "duplicate " + s.id());
        }
        assertEquals(1L, GauntletNumbers.ALL.stream().filter(Budget.Spec::ultimate).count(), "exactly one ultimate");
    }

    @Test
    void theUltimateCountsPointsUpToTheCapOfThePool() {
        assertEquals(0, GauntletNumbers.dragonPoints(-4, false));
        assertEquals(5, GauntletNumbers.dragonPoints(5, false));
        assertEquals(6, GauntletNumbers.dragonPoints(5, true), "Dragon Heart counts one more");
        assertEquals(PointPool.MAX, GauntletNumbers.dragonPoints(PointPool.MAX, true), "never above the pool's cap");
        assertEquals(20.0, GauntletNumbers.dragonRaw(GauntletNumbers.DRAGON_MIN, false), 1e-9, "20 raw at the minimum");
        assertEquals(20.0, GauntletNumbers.dragonRaw(GauntletNumbers.DRAGON_MIN - 3, false), 1e-9, "never below the base");
        assertEquals(28.0, GauntletNumbers.dragonRaw(PointPool.MAX, false), 1e-9);
        assertEquals(29.0, GauntletNumbers.dragonRaw(PointPool.MAX, true), 1e-9);
        assertEquals(GauntletNumbers.DRAGON_SLAYER.spec().rawMax(), GauntletNumbers.dragonRaw(PointPool.MAX, true), 1e-9);
        assertTrue(GauntletNumbers.DRAGON_SPLASH_RAW <= GauntletNumbers.DRAGON_SLAYER.spec().rawMax());
    }

    @Test
    void thePassivesNeverBreakTheCooldownFloors() {
        for (Budget.Spec s : List.of(GauntletNumbers.AFTERIMAGE_PALM.spec(), GauntletNumbers.TIGER_LEAP.spec())) {
            assertTrue(Math.round(s.cooldown() * GauntletNumbers.BLOSSOM_COOLDOWN) >= Budget.floor(s.kind()), s.id());
        }
        assertTrue(GauntletNumbers.COUNTER_RAW <= GauntletNumbers.IRON_GUARD.rawMax() + 1e-9, "the counter is the row's value");
        assertTrue(GauntletNumbers.GUARD_TAKEN < 1.0 && GauntletNumbers.HARDENED_TAKEN < 1.0 && GauntletNumbers.GUARD_TAKEN * GauntletNumbers.HARDENED_TAKEN > 0.3, "the guard never makes the wielder untouchable");
    }
}
