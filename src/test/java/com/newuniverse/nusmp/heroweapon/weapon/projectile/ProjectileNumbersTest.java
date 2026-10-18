package com.newuniverse.nusmp.heroweapon.weapon.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Vassal Projectile must stay inside the balance budget whatever anybody edits later, so the whole table is judged here: every technique passes the
 * budget, the ids are the projectile's and unique, the table agrees with the mechanics that multiply a throw (the second blade, the extra ricochet, the number of
 * knives and blades, the pulses of the tornado and the volleys of the rain), and Over Custom never takes a cooldown below the floor of its kind.
 */
class ProjectileNumbersTest {
    @Test
    void everyTechniqueIsWithinTheBudget() {
        for (Budget.Spec s : ProjectileNumbers.ALL) {
            List<String> bad = Budget.violations(s);
            assertTrue(bad.isEmpty(), bad.toString());
        }
        assertEquals(10, ProjectileNumbers.ALL.size(), "the projectile has ten techniques (at least six are required)");
    }

    @Test
    void idsAreTheProjectilesAndUnique() {
        Set<String> seen = new HashSet<>();
        for (Budget.Spec s : ProjectileNumbers.ALL) {
            assertTrue(s.id().startsWith("projectile."), s.id());
            assertTrue(seen.add(s.id()), "duplicate " + s.id());
        }
        assertEquals(1L, ProjectileNumbers.ALL.stream().filter(Budget.Spec::ultimate).count(), "exactly one ultimate");
    }

    @Test
    void theTableAgreesWithTheMechanicsThatMultiplyAThrow() {
        assertEquals(2, ProjectileNumbers.INSTANT_THROW.hitsPerTarget(), "Second Throw: one foe can take both blades");
        assertTrue(ProjectileNumbers.INSTANT_THROW.maxTargets() >= 2 * ProjectileNumbers.THROW_PIERCE, "two blades of two piercings can touch four foes");
        assertEquals(1 + ProjectileNumbers.RICOCHET_JUMPS + 1, ProjectileNumbers.RICOCHET.maxTargets(), "the first foe, the jumps and the extra jump of Wide Ricochet");
        assertEquals(2, ProjectileNumbers.BOOMERANG.hitsPerTarget(), "out and back");
        assertEquals(ProjectileNumbers.BOOMERANG_PIERCE, ProjectileNumbers.BOOMERANG.maxTargets());
        assertTrue(ProjectileNumbers.KNIFE_CAP <= ProjectileNumbers.KNIFE_RAIN.hitsPerTarget(), "a foe takes at most the capped number of knives");
        assertTrue(ProjectileNumbers.KNIFE_COUNT >= ProjectileNumbers.KNIFE_RAIN.maxTargets(), "the fan has a knife for every foe it may touch");
        assertEquals(ProjectileNumbers.TORNADO_TICKS / ProjectileNumbers.TORNADO_INTERVAL + 1, ProjectileNumbers.TORNADO_THROW.hitsPerTarget(), "the tornado pulses as often as the table says");
        assertEquals(ProjectileNumbers.BLADES_VOLLEYS, ProjectileNumbers.THOUSAND_BLADES.hitsPerTarget(), "the rain falls as often as the table says");
        assertEquals(ProjectileNumbers.DRITTE_COUNT, ProjectileNumbers.DRITTE_THROW.hitsPerTarget(), "all three blades may go to one foe");
        assertEquals(ProjectileNumbers.DRITTE_COUNT, ProjectileNumbers.DRITTE_THROW.maxTargets());
        assertTrue(ProjectileNumbers.BLADES_INTERVAL * ProjectileNumbers.BLADES_VOLLEYS < 200, "the field of blades ends well inside twenty seconds");
    }

    @Test
    void overCustomNeverBreaksTheFloorOfAKind() {
        for (Budget.Spec s : ProjectileNumbers.ALL) {
            int custom = ProjectileNumbers.cooldown(s, true);
            assertTrue(custom >= Budget.floor(s.kind()), s.id());
            assertTrue(custom <= s.cooldown(), s.id() + " is never slower than nominal");
            assertEquals(s.cooldown(), ProjectileNumbers.cooldown(s, false));
        }
        assertEquals(60, ProjectileNumbers.cooldown(ProjectileNumbers.INSTANT_THROW, true), "60 ticks is already the floor");
        assertEquals(126, ProjectileNumbers.cooldown(ProjectileNumbers.DRITTE_THROW, true), "140 * 0.9 is 126 and the floor is 60");
    }
}
