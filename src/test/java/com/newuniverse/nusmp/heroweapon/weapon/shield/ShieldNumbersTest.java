package com.newuniverse.nusmp.heroweapon.weapon.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.shield.ShieldNumbers.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.shield.ShieldNumbers.Spec;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the owner's balance law for the hero weapons (design.md section 5) is a list of numbers, and the Legendary Shield's every technique has a row in
 * {@link ShieldNumbers}; this test walks the rows against the budget so a later edit that makes a technique too strong, too cheap or too quick fails the build.
 */
class ShieldNumbersTest {
    @Test
    void theShieldHasAtLeastFourteenTechniquesAndFourFromTheWrathSeries() {
        int ordinary = 0, curse = 0;
        for (Spec s : ShieldNumbers.all()) {
            if (s.curse()) curse++;
            else ordinary++;
        }
        assertTrue(ordinary >= 10, "the ordinary tree needs about ten techniques, has " + ordinary);
        assertTrue(ordinary + curse >= 14, "a Holy Weapon has at least 14 techniques, has " + (ordinary + curse));
        assertEquals(4, curse, "the Wrath series has four");
    }

    @Test
    void idsAreUniqueLowerCaseAndStartWithTheWeaponsName() {
        Set<String> seen = new HashSet<>();
        for (Spec s : ShieldNumbers.all()) {
            assertTrue(seen.add(s.id()), "duplicate id " + s.id());
            assertTrue(s.id().matches("shield\\.[a-z_]+"), "bad id " + s.id());
            assertTrue(s.name() != null && !s.name().isBlank(), "no name for " + s.id());
            assertNotNull(ShieldNumbers.spec(s.id()), "lookup " + s.id());
        }
    }

    @Test
    void everyRowKeepsTheBudgetOfItsClass() {
        for (Spec s : ShieldNumbers.all()) {
            String id = s.id();
            assertTrue(s.raw() >= 0 && s.hits() >= 0, id + " has negative damage");
            assertTrue(s.raw() == 0 || s.raw() >= ShieldNumbers.MIN_HIT, id + " one hit is at least " + ShieldNumbers.MIN_HIT + " raw, has " + s.raw());
            switch (s.kind()) {
                case UTILITY -> {
                    assertTrue(s.cooldown() >= ShieldNumbers.MIN_UTILITY_COOLDOWN, id + " utility cooldown " + s.cooldown());
                    assertTrue(s.total() <= 5, id + " a utility technique should barely hurt, total " + s.total());
                    assertTrue(s.cost() >= ShieldNumbers.MIN_COST && s.cost() <= ShieldNumbers.MAX_COST, id + " cost " + s.cost());
                }
                case DAMAGE -> {
                    assertTrue(s.cooldown() >= ShieldNumbers.MIN_DAMAGE_COOLDOWN, id + " damage cooldown " + s.cooldown());
                    assertTrue(s.total() > 0 && s.total() <= ShieldNumbers.ORDINARY_MAX, id + " ordinary total " + s.total());
                    assertTrue(s.cost() >= ShieldNumbers.MIN_COST && s.cost() <= ShieldNumbers.MAX_COST, id + " cost " + s.cost());
                }
                case HEAVY -> {
                    assertTrue(s.cooldown() >= ShieldNumbers.MIN_DAMAGE_COOLDOWN, id + " heavy cooldown " + s.cooldown());
                    assertTrue(s.total() > ShieldNumbers.ORDINARY_MAX - 2 && s.total() <= ShieldNumbers.HEAVY_MAX, id + " heavy total " + s.total());
                    assertTrue(s.cost() >= ShieldNumbers.MIN_COST && s.cost() <= ShieldNumbers.MAX_COST, id + " cost " + s.cost());
                }
                case ULTIMATE -> {
                    assertTrue(s.cooldown() >= ShieldNumbers.MIN_ULTIMATE_COOLDOWN, id + " ultimate cooldown " + s.cooldown());
                    assertTrue(s.total() <= ShieldNumbers.ULTIMATE_MAX, id + " ultimate total " + s.total());
                    assertTrue(s.cost() >= ShieldNumbers.MIN_ULTIMATE_COST && s.cost() <= ShieldNumbers.MAX_ULTIMATE_COST, id + " ultimate cost " + s.cost());
                }
            }
        }
    }

    @Test
    void exactlyTwoUltimatesOneForEachSeries() {
        int ordinary = 0, curse = 0;
        for (Spec s : ShieldNumbers.all()) {
            if (s.kind() != Kind.ULTIMATE) continue;
            if (s.curse()) curse++;
            else ordinary++;
        }
        assertEquals(1, ordinary);
        assertEquals(1, curse);
    }

    @Test
    void theShieldIsTheWeakestHitterAndTheDefenceIsTheSpecialty() {
        // the Shield cannot deal direct damage easily (the owner's text): none of its ordinary-tree attacks passes the ordinary budget
        for (Spec s : ShieldNumbers.all()) {
            if (s.curse() || s.kind() == Kind.ULTIMATE) continue;
            assertTrue(s.total() <= ShieldNumbers.HEAVY_MAX, s.id());
        }
        assertTrue(ShieldNumbers.CHANGE_ATTACK.raw() <= ShieldNumbers.ORDINARY_MAX);
        assertTrue(ShieldNumbers.GUARD_CAPACITY >= 1.0, "the guard soaks at least a full health bar");
        assertTrue(ShieldNumbers.BULWARK_CAPACITY > ShieldNumbers.GUARD_CAPACITY);
    }

    @Test
    void noTaskRunsLongerThanTwentySecondsAndNoTechniqueMakesTooManyThings() {
        int[] ticks = {ShieldNumbers.PANEL_TICKS, ShieldNumbers.COVER_TICKS, ShieldNumbers.WARD_TICKS, ShieldNumbers.AEGIS_TICKS, ShieldNumbers.PRISON_TICKS,
                ShieldNumbers.CHAIN_PULSES * ShieldNumbers.CHAIN_INTERVAL, ShieldNumbers.DEFENCE_TICKS, ShieldNumbers.UNDEAD_TICKS, ShieldNumbers.PROVOKE_TICKS,
                ShieldNumbers.SOUL_HOLD, ShieldNumbers.BLOOD_CURSE_TICKS};
        for (int t : ticks) assertTrue(t > 0 && t <= ShieldNumbers.MAX_TASK_TICKS * 2, "task of " + t + " ticks");
        // the running tasks themselves (zones) are bounded by the 20 s rule
        for (int t : new int[]{ShieldNumbers.PANEL_TICKS, ShieldNumbers.COVER_TICKS, ShieldNumbers.WARD_TICKS, ShieldNumbers.AEGIS_TICKS, ShieldNumbers.PRISON_TICKS,
                ShieldNumbers.DEFENCE_TICKS, ShieldNumbers.UNDEAD_TICKS, ShieldNumbers.PROVOKE_TICKS}) assertTrue(t <= ShieldNumbers.MAX_TASK_TICKS, "zone of " + t + " ticks");
        assertTrue(ShieldNumbers.MAX_PANELS <= ShieldNumbers.MAX_SPAWNS);
        assertTrue(ShieldNumbers.CHAIN_TARGETS <= ShieldNumbers.MAX_TARGETS);
        assertTrue(ShieldNumbers.COVER_MAX_ALLIES <= ShieldNumbers.MAX_TARGETS);
    }

    @Test
    void priceOfTheBloodSkillsNeverKillsTheWielder() {
        assertTrue(ShieldNumbers.BLOOD_HEALTH_SHARE + ShieldNumbers.BLOOD_HEALTH_FLOOR < 1.0);
        assertTrue(ShieldNumbers.MEGIDDO_HEALTH_SHARE + ShieldNumbers.MEGIDDO_HEALTH_FLOOR < 1.0);
        assertTrue(ShieldNumbers.BLOOD_HEALTH_FLOOR >= 0.2 && ShieldNumbers.MEGIDDO_HEALTH_FLOOR >= 0.2, "the wielder keeps a fifth of their health at least");
        assertTrue(ShieldNumbers.COVER_FLOOR >= 0.2);
    }

    @Test
    void everyStartingNumberOfACounterAttackOrASplashIsAtLeastTheMinimumHit() {
        assertTrue(ShieldNumbers.CHANGE_BASE >= ShieldNumbers.MIN_HIT && ShieldNumbers.IRON_BASE >= ShieldNumbers.MIN_HIT);
        assertTrue(ShieldNumbers.NEEDLE_BURST_BASE >= ShieldNumbers.MIN_HIT && ShieldNumbers.BLOOD_SPLASH >= ShieldNumbers.MIN_HIT);
        assertEquals(ShieldNumbers.CHANGE_ATTACK.raw(), ShieldNumbers.CHANGE_BASE + ShieldNumbers.CHANGE_SPAN, 1e-9);
        assertEquals(ShieldNumbers.IRON_MAIDEN.raw(), ShieldNumbers.IRON_BASE + ShieldNumbers.IRON_SPAN, 1e-9);
        assertEquals(ShieldNumbers.NEEDLE_BURST.raw(), ShieldNumbers.NEEDLE_BURST_BASE + ShieldNumbers.NEEDLE_BURST_SPAN, 1e-9);
    }

    @Test
    void theHealIsUnderTheQuarterCapOfTheLaw() {
        assertTrue(ShieldNumbers.HERB_SHARE <= 0.25);
        assertTrue(ShieldNumbers.WARD_BANK_SHARE <= 0.5);
    }

    @Test
    void theCounterAndGuardNumbersHangTogether() {
        assertTrue(ShieldNumbers.PARRY_WINDOW > 0 && ShieldNumbers.PARRY_WINDOW_BULWARK > ShieldNumbers.PARRY_WINDOW);
        assertTrue(ShieldNumbers.PARRY_PAUSE > ShieldNumbers.BLOCK_DELAY + ShieldNumbers.PARRY_WINDOW_BULWARK, "re-raising the guard cannot give a parry on every blow");
        assertTrue(ShieldNumbers.BREAK_TICKS >= 20 && ShieldNumbers.BREAK_TICKS <= 100, "a broken guard comes back in one to five seconds");
        assertTrue(ShieldNumbers.AFTER_BREAK > 0 && ShieldNumbers.AFTER_BREAK < 1);
        // the guard refills in well under a minute
        double secondsToFull = 1.0 / (ShieldNumbers.REGEN_PER_TICK * 20.0);
        assertTrue(secondsToFull > 5 && secondsToFull < 30, "refill takes " + secondsToFull + " s");
        // the Counter charge fades slowly enough to be used and fast enough not to be kept for ever
        double secondsToFade = ShieldNumbers.COUNTER_MAX / (ShieldNumbers.COUNTER_DECAY_PER_TICK * 20.0);
        assertTrue(secondsToFade > 10 && secondsToFade < 60, "fade takes " + secondsToFade + " s");
        assertFalse(ShieldNumbers.PASSIVES.isEmpty());
        assertTrue(ShieldNumbers.PASSIVES.contains("bulwark") && ShieldNumbers.PASSIVES.contains("needle_counter"));
    }

    @Test
    void theReflectedDamageIsOnlyAShareOfTheBlow() {
        assertTrue(ShieldNumbers.NEEDLE_SHARE > 0 && ShieldNumbers.NEEDLE_SHARE <= 1.0, "an answer is never worth more than the blow");
        assertTrue(ShieldNumbers.NEEDLE_RAW_CAP <= ShieldNumbers.ORDINARY_MAX);
        assertTrue(ShieldNumbers.NEEDLE_ICD >= 5);
    }
}
