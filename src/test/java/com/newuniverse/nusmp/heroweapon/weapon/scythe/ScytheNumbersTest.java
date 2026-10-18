package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the Scythe's numbers must stay inside the owner's balance budget (docs/heroweapon/design.md section 5): this holds every technique's row to
 * {@link Budget}, checks the weapon has the nine techniques the design promises (at least six are required) with the ultimate where it belongs, and pins the
 * edges that matter most: the Soul Reap at its doubled 20 is exactly the heavy limit, the Soul Harvest at full souls on a weak foe is exactly the ultimate
 * limit, the dance totals 12 and the floating scythe 18.
 */
class ScytheNumbersTest {
    @Test
    void everyTechniqueObeysTheBudget() {
        List<String> bad = Budget.violations(ScytheNumbers.table());
        assertTrue(bad.isEmpty(), "budget violations: " + bad);
    }

    @Test
    void theWeaponHasNineTechniquesAndOneUltimate() {
        assertEquals(9, ScytheNumbers.table().size());
        int ultimates = 0;
        List<String> ids = new ArrayList<>();
        for (Budget.Spec s : ScytheNumbers.table()) {
            assertTrue(s.id().startsWith("scythe."), "every id starts with scythe.");
            if (s.kind() == Budget.Kind.ULTIMATE) ultimates++;
            ids.add(s.id());
        }
        assertEquals(1, ultimates);
        assertEquals(Budget.Kind.ULTIMATE, ScytheNumbers.spec(ScytheNumbers.SOUL_HARVEST).kind());
        assertEquals(ScytheText.skillIds(), ids, "the text table and the number table list the same skills in the same order");
    }

    @Test
    void anUnknownSkillIsAnErrorNotADefault() {
        boolean threw = false;
        try {
            ScytheNumbers.spec("scythe.nope");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    void theEdgesAreExactlyTheOwnersLimits() {
        assertEquals(20.0, ReapRules.reapRaw(true), 1e-9);
        assertEquals(20.0, Budget.Kind.HEAVY.maxTotal(), 1e-9);
        assertEquals(32.0, ScytheNumbers.spec(ScytheNumbers.SOUL_HARVEST).total(), 1e-9);
        assertEquals(12.0, ScytheNumbers.spec(ScytheNumbers.DEADLY_DANCE).total(), 1e-9);
        assertEquals(18.0, ScytheNumbers.spec(ScytheNumbers.FLOATING_SCYTHE).total(), 1e-9);
        assertEquals(10.0, ScytheNumbers.spec(ScytheNumbers.FLYING_CIRCLE).total(), 1e-9);
        assertEquals(17.0, ScytheNumbers.spec(ScytheNumbers.PROTECTION_BREAK).total(), 1e-9);
        assertEquals(Budget.Kind.UTILITY, ScytheNumbers.spec(ScytheNumbers.SCYTHE_ZERO).kind());
        assertEquals(0.0, ScytheNumbers.spec(ScytheNumbers.SCYTHE_ZERO).total(), 1e-9, "Scythe 0 deals no damage");
    }

    @Test
    void theFloatingScytheStaysUnderTwentySeconds() {
        assertTrue(ScytheNumbers.FLOAT_STRIKES * ScytheNumbers.FLOAT_INTERVAL <= Budget.MAX_DURATION_TICKS);
        assertTrue(ScytheNumbers.DANCE_SWINGS * ScytheNumbers.DANCE_INTERVAL <= Budget.MAX_DURATION_TICKS);
        assertTrue(ScytheNumbers.SOUL_FADE_TICKS > Budget.MAX_DURATION_TICKS, "souls are state, not a running task");
    }
}
