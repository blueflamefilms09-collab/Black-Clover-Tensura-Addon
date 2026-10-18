package com.newuniverse.nusmp.heroweapon.weapon.fan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the Fan's numbers must stay inside the owner's balance budget (docs/heroweapon/design.md section 5): this holds every technique's row to {@link Budget},
 * pins the pure rules the skills call (the armour bonus of the Tortoise Shell Cracker, the residue of the petal tornado, the Snow Plum heal and its quarter cap),
 * and checks the weapon has the nine techniques the design promises (at least six are required) with the ultimate where it belongs.
 */
class FanNumbersTest {
    @Test
    void everyTechniqueObeysTheBudget() {
        List<String> bad = Budget.violations(FanNumbers.table());
        assertTrue(bad.isEmpty(), "budget violations: " + bad);
    }

    @Test
    void theWeaponHasNineTechniquesAndOneUltimate() {
        assertEquals(9, FanNumbers.table().size());
        int ultimates = 0;
        for (Budget.Spec s : FanNumbers.table()) if (s.kind() == Budget.Kind.ULTIMATE) ultimates++;
        assertEquals(1, ultimates);
        assertEquals(Budget.Kind.ULTIMATE, FanNumbers.spec(FanNumbers.SNOW_MOON_FLOWER).kind());
        assertEquals(Budget.Kind.HEAVY, FanNumbers.spec(FanNumbers.TORTOISE_SHELL_CRACKER).kind());
        List<String> ids = new ArrayList<>();
        for (Budget.Spec s : FanNumbers.table()) {
            assertTrue(s.id().startsWith("fan."), "every id starts with fan.");
            ids.add(s.id());
        }
        assertEquals(FanText.skillIds(), ids, "the text table and the number table list the same skills in the same order");
    }

    @Test
    void anUnknownSkillIsAnErrorNotADefault() {
        boolean threw = false;
        try {
            FanNumbers.spec("fan.nope");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    void theTortoiseShellCrackerGrowsWithArmourUpToItsCap() {
        assertEquals(10.0, FanNumbers.tortoiseRaw(0), 1e-9);
        assertEquals(15.0, FanNumbers.tortoiseRaw(10), 1e-9);
        assertEquals(18.0, FanNumbers.tortoiseRaw(16), 1e-9);
        assertEquals(18.0, FanNumbers.tortoiseRaw(500), 1e-9, "capped at 8 more");
        assertEquals(10.0, FanNumbers.tortoiseRaw(-4), 1e-9, "negative armour gives no bonus and no penalty");
        assertTrue(FanNumbers.tortoiseRaw(1000) <= Budget.Kind.HEAVY.maxHit());
    }

    @Test
    void thePetalTornadoBurnsResidueAndNeverPassesTheUltimateLimit() {
        assertEquals(3.5, FanNumbers.snowMoonPulse(0), 1e-9);
        assertEquals(4.25, FanNumbers.snowMoonPulse(3), 1e-9);
        assertEquals(5.0, FanNumbers.snowMoonPulse(6), 1e-9);
        assertEquals(5.0, FanNumbers.snowMoonPulse(99), 1e-9, "residue is capped at 6");
        assertEquals(3.5, FanNumbers.snowMoonPulse(-2), 1e-9);
        assertTrue(FanNumbers.snowMoonPulse(6) * FanNumbers.SNOW_MOON_PULSES <= Budget.Kind.ULTIMATE.maxTotal());
        assertEquals(30.0, FanNumbers.snowMoonPulse(6) * FanNumbers.SNOW_MOON_PULSES, 1e-9);
    }

    @Test
    void theSweepSurgesOnlyWithTheDragonPassive() {
        assertEquals(5.0, FanNumbers.flowerRaw(false), 1e-9);
        assertEquals(7.0, FanNumbers.flowerRaw(true), 1e-9);
        assertEquals(5.5, FanNumbers.flowerRange(false), 1e-9);
        assertEquals(7.0, FanNumbers.flowerRange(true), 1e-9);
        assertTrue(FanNumbers.flowerRaw(true) <= Budget.Kind.ORDINARY.maxHit());
    }

    @Test
    void theSnowPlumHealIsSmallAndCappedAtAQuarter() {
        assertEquals(2.8, FanNumbers.plumHeal(20.0), 1e-9);
        assertEquals(2.0, FanNumbers.plumHeal(8.0), 1e-9, "a quarter of 8 is 2");
        assertEquals(1.0, FanNumbers.plumHeal(4.0), 1e-9);
        assertEquals(42.0, FanNumbers.plumHeal(1000.0), 1e-9);
        assertEquals(0.0, FanNumbers.plumHeal(0.0), 1e-9);
        for (double hp = 1.0; hp < 2000.0; hp += 7.0) {
            assertTrue(FanNumbers.plumHeal(hp) <= FanNumbers.PLUM_HEAL_CAP * hp + 1e-9, "never more than a quarter at " + hp);
            assertTrue(FanNumbers.plumHeal(hp) >= 0.0);
        }
    }

    @Test
    void theGlideNeedsSneakingInTheAirAndFallingFastOrFar() {
        assertTrue(FanNumbers.glides(false, true, false, 0.5, 0.0, false), "sneaking in the air while falling");
        assertTrue(FanNumbers.glides(false, true, false, FanNumbers.GLIDE_MIN_FALL, 0.0, false), "exactly the speed it takes hold");
        assertFalse(FanNumbers.glides(false, true, false, FanNumbers.GLIDE_MIN_FALL - 0.01, 0.0, false), "not falling fast enough yet");
        assertTrue(FanNumbers.glides(false, true, false, 0.0, FanNumbers.GLIDE_MIN_DISTANCE, false), "a long enough fall starts it even if the speed is not known");
        assertFalse(FanNumbers.glides(false, true, false, 0.0, FanNumbers.GLIDE_MIN_DISTANCE - 0.1, false), "a short fall does not");
        assertFalse(FanNumbers.glides(true, true, false, 0.9, 9.0, true), "on the ground");
        assertFalse(FanNumbers.glides(false, false, false, 0.9, 9.0, true), "not sneaking");
        assertFalse(FanNumbers.glides(false, true, true, 0.9, 9.0, true), "carried some other way (creative flight, elytra, water, a ladder, a vehicle)");
        assertTrue(FanNumbers.GLIDE_TICKS > 5, "renewed every five ticks, so it must outlast that");
    }

    @Test
    void aGlideThatHasSlowedTheFallStaysUpAsLongAsYouHoldSneak() {
        // slow falling takes the speed and the fallen distance down to nothing, so once it holds only "already gliding" keeps it going
        assertTrue(FanNumbers.glides(false, true, false, 0.05, 0.0, true), "slowed, still sneaking in the air: renewed");
        assertFalse(FanNumbers.glides(false, true, false, 0.05, 0.0, false), "slow and never started: no glide");
        assertFalse(FanNumbers.glides(false, false, false, 0.05, 0.0, true), "let go of sneak: the glide lapses");
        assertFalse(FanNumbers.glides(true, true, false, 0.05, 0.0, true), "landed: the glide is over");
    }

    @Test
    void theHardControlSkillSharesTheTwelveSecondLockSoItRestsLongEnough() {
        // Gale Pull is hard control and takes the shared 12 s (240 tick) lock; the rest of the skill must at least be a utility rest
        assertTrue(FanNumbers.spec(FanNumbers.GALE_PULL).cooldown() >= Budget.Kind.UTILITY.minCooldown());
        assertTrue(FanNumbers.PULL_MAX_SPEED <= 1.0, "a pull never flings a foe");
        assertTrue(FanNumbers.GUST_SHOVE <= 1.2, "a push is a shove, not a launch");
    }
}
