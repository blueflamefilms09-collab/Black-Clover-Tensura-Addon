package com.newuniverse.nusmp.heroweapon.weapon.spear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow.Kind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the owner's balance budget (raw damage 3..12 ordinary, 10..20 heavy, at most 32 for an ultimate; cooldown at least 60 ticks for damage, 100 for
 * utility, 600 for an ultimate; cost 1 to 3% and 5 to 8% for an ultimate; at most 12 targets; nothing that repeats for longer than 20 seconds) is pinned for
 * every technique of the Legendary Spear, so a later edit of one number that breaks it fails here. The time reversal's own rules (a window of 8 seconds, a
 * cooldown of at least 1800 ticks, a heal of at most a quarter) are pinned too, and the table is checked against the id list so no skill is missing a row.
 */
class SpearNumbersTest {
    @Test
    void everyRowKeepsTheBudget() {
        List<String> bad = new ArrayList<>();
        for (SkillRow r : SpearNumbers.ROWS) bad.addAll(r.problems());
        assertTrue(bad.isEmpty(), "budget problems: " + bad);
    }

    @Test
    void theTableMatchesTheIdListOneForOne() {
        assertEquals(SpearIds.ALL.size(), SpearNumbers.ROWS.size());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < SpearIds.ALL.size(); i++) {
            assertEquals(SpearIds.ALL.get(i), SpearNumbers.ROWS.get(i).id(), "row " + i + " is out of order");
            assertTrue(seen.add(SpearNumbers.ROWS.get(i).id()), "duplicate " + SpearNumbers.ROWS.get(i).id());
            assertNotNull(SpearNumbers.row(SpearIds.ALL.get(i)));
        }
        assertNull(SpearNumbers.row("spear.nothing"));
        List<String> both = new ArrayList<>(SpearIds.ORDINARY);
        both.addAll(SpearIds.CURSE);
        assertEquals(SpearIds.ALL, both, "ORDINARY then CURSE is ALL");
    }

    @Test
    void everyIdBelongsToTheSpear() {
        for (String id : SpearIds.ALL) assertTrue(id.startsWith("spear."), id);
    }

    @Test
    void heavyIsHeavyAndOrdinaryIsOrdinary() {
        for (SkillRow r : SpearNumbers.ROWS) {
            if (r.budget() == Budget.HEAVY) assertTrue(r.raw() >= 10.0 && r.raw() <= 20.0, r.id() + " is labelled heavy but deals " + r.raw());
            if (r.budget() == Budget.ORDINARY && r.raw() > 0.0) assertTrue(r.raw() >= 3.0 && r.raw() <= 12.0, r.id() + " is ordinary but deals " + r.raw());
            if (r.kind() == Kind.ULTIMATE) assertTrue(r.raw() <= 32.0, r.id());
        }
    }

    @Test
    void thereAreAtLeastFourteenSkillsAndAtLeastThreeUltimates() {
        assertTrue(SpearNumbers.ROWS.size() >= 14);
        int ultimates = 0;
        for (SkillRow r : SpearNumbers.ROWS) if (r.kind() == Kind.ULTIMATE) ultimates++;
        assertEquals(3, ultimates, "Brionac, Time Reversal and Lust's Embrace");
        assertEquals(4, SpearIds.CURSE.size(), "about four curse skills");
    }

    @Test
    void theTimeReversalFollowsTheOwnersRules() {
        SkillRow r = SpearNumbers.TIME_REVERSAL;
        assertTrue(r.cooldown() >= 1800, "a time reversal waits at least 1800 ticks");
        assertTrue(r.cooldown() * HeroLevels.cooldownScale(HeroLevels.MAX) >= 1800 - 1e-6, "and still does at the top bond level, where the dispatcher shortens cooldowns by a quarter");
        assertEquals(Kind.ULTIMATE, r.kind());
        assertEquals(160, SpearNumbers.REWIND_WINDOW, "up to eight seconds");
        assertEquals(0.25, SpearNumbers.REWIND_HEAL_CAP, 1e-9, "a quarter of the maximum health");
        assertEquals(3.0, SpearNumbers.REWIND_RADIUS, 1e-9, "the nearest safe spot within three blocks");
        assertTrue(SpearNumbers.REWIND_MIN_AGE >= 40 && SpearNumbers.REWIND_MIN_AGE < SpearNumbers.REWIND_WINDOW, "a rewind must reach back a while but not past the window");
        assertEquals(20, SpearNumbers.REWIND_STEP, "one snapshot a second");
        assertTrue(SpearNumbers.REWIND_CAPACITY >= SpearNumbers.REWIND_WINDOW / SpearNumbers.REWIND_STEP + 1, "the buffer holds the whole window");
        assertEquals(0.0, r.raw(), 1e-9, "it does no damage");
    }

    @Test
    void anUltimateStillWaitsSixHundredTicksAtTheTopBondLevel() {
        for (SkillRow r : SpearNumbers.ROWS) {
            if (r.kind() == Kind.ULTIMATE) assertTrue(r.cooldown() * HeroLevels.cooldownScale(HeroLevels.MAX) >= 600, r.id() + " after the level scaling");
        }
    }

    @Test
    void nothingRepeatsForLongerThanTwentySeconds() {
        int limit = 20 * 20;
        assertTrue(SpearNumbers.RUSH_TICKS <= limit);
        assertTrue(SpearNumbers.MINUTE_DELAY + SpearNumbers.MINUTE_STEPS * SpearNumbers.MINUTE_STEP_TICKS <= limit);
        assertTrue(SpearNumbers.CHAINS_DELAY + (SpearNumbers.CHAINS_PULSES - 1) * SpearNumbers.CHAINS_INTERVAL <= limit);
        assertTrue(SpearNumbers.TEMPT_DELAY + SpearNumbers.TEMPT_STEPS * 2 <= limit);
        assertTrue(SpearNumbers.EMBRACE_DELAY + SpearNumbers.EMBRACE_PULL_STEPS * 2 <= limit);
        assertTrue(SpearNumbers.RIPOSTE_TICKS <= limit);
        assertTrue(SpearNumbers.JAVELIN_LIFE <= limit);
        assertTrue(SpearNumbers.FERVOR_TICKS <= limit);
    }

    @Test
    void theDamageOfTheHitsAddsUpToTheRow() {
        assertEquals(SpearNumbers.CHAOS_LANCES * SpearNumbers.CHAOS_PER_LANCE, SpearNumbers.CHAOS_SPEAR.raw(), 1e-9, "five lances on one foe");
        assertEquals(SpearNumbers.METEOR_RAW + SpearNumbers.METEOR_SPLASH, SpearNumbers.METEOR_SPEAR.raw(), 1e-9, "the lane and the burst on one foe");
        assertEquals(SpearNumbers.CHAINS_PULSES * SpearNumbers.CHAINS_RAW_PULSE, SpearNumbers.THORN_CHAINS.raw(), 1e-9, "every pulse on one foe");
        assertTrue(SpearNumbers.CHAOS_SPEAR.raw() <= 12.0 && SpearNumbers.THORN_CHAINS.raw() <= 12.0);
    }

    @Test
    void repeatedBlowsOnOneFoeAreFarEnoughApartForTheGameToCountThemAll() {
        assertTrue(SpearNumbers.CHAINS_INTERVAL >= SkillRow.HURT_IMMUNITY, "a pulse that comes sooner than the hurt immunity of the last one does nothing to a foe that stood in both");
    }

    @Test
    void theBlowsGrowOfTheLustFormsIsBounded() {
        assertTrue(SpearNumbers.FERVOR_MAX * SpearNumbers.FERVOR_PER_STACK <= 0.35, "at most +35% melee damage");
        assertTrue(SpearNumbers.AIM_SNAP_ANGLE <= 90.0 && SpearNumbers.AIM_SNAP_RANGE <= 12.0);
        assertTrue(SpearNumbers.KILL_VIGOR <= 0.05, "a kill never heals more than 5% of the maximum health");
        assertTrue(SpearNumbers.RAPTURE_HEAL <= 0.25, "no technique heals more than a quarter of the maximum health");
    }

    @Test
    void theLongestLineIsStillShorterThanTheSniperRange() {
        double longest = Math.max(SpearNumbers.BRIONAC_LENGTH, SpearNumbers.TRIAINA_LENGTH * SpearNumbers.LONGSHAFT);
        assertTrue(longest <= 32.0, "no spear line passes 32 blocks");
        assertTrue(SpearNumbers.THRUST_LENGTH * SpearNumbers.LONGSHAFT >= SpearNumbers.THRUST_LENGTH);
    }

    @Test
    void descriptionsCarryTheirNumbers() {
        assertTrue(SpearNumbers.PIERCING_THRUST.desc().contains("8 blocks"), SpearNumbers.PIERCING_THRUST.desc());
        assertTrue(SpearNumbers.CHAOS_SPEAR.desc().contains("12 on a lone foe"), SpearNumbers.CHAOS_SPEAR.desc());
        assertTrue(SpearNumbers.TIME_REVERSAL.desc().contains("8 seconds") && SpearNumbers.TIME_REVERSAL.desc().contains("25%"), SpearNumbers.TIME_REVERSAL.desc());
        assertTrue(SpearNumbers.MINUTE_HAND.desc().contains("1.5 seconds"), SpearNumbers.MINUTE_HAND.desc());
        assertTrue(SpearNumbers.JAVELIN.desc().contains("30 blocks"), SpearNumbers.JAVELIN.desc());
        assertTrue(SpearNumbers.RIPOSTE.desc().contains("65%") && SpearNumbers.RIPOSTE.desc().contains("1.2 seconds"), SpearNumbers.RIPOSTE.desc());
        assertEquals("6", SpearNumbers.n(6.0));
        assertEquals("2.4", SpearNumbers.n(2.4));
        assertEquals("1.5", SpearNumbers.sec(30));
        assertEquals("8", SpearNumbers.sec(160));
    }
}
