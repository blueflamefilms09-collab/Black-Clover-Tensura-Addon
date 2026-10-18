package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * every technique of the Legendary Bow, so a later edit of one number that breaks it fails here. The Bow is range and utility, so its plain arrow is pinned
 * below a damage per second that a melee weapon of the set can beat, and the sum of everything one foe can take from one cast is checked against the row.
 */
class BowNumbersTest {
    @Test
    void everyRowKeepsTheBudget() {
        List<String> bad = new ArrayList<>();
        for (SkillRow r : BowNumbers.ROWS) bad.addAll(r.problems());
        assertTrue(bad.isEmpty(), "budget problems: " + bad);
    }

    @Test
    void theTableMatchesTheIdListOneForOne() {
        assertEquals(BowIds.ALL.size(), BowNumbers.ROWS.size());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < BowIds.ALL.size(); i++) {
            assertEquals(BowIds.ALL.get(i), BowNumbers.ROWS.get(i).id(), "row " + i + " is out of order");
            assertTrue(seen.add(BowNumbers.ROWS.get(i).id()), "duplicate " + BowNumbers.ROWS.get(i).id());
            assertNotNull(BowNumbers.row(BowIds.ALL.get(i)));
            assertTrue(BowIds.ALL.get(i).startsWith("bow."), BowIds.ALL.get(i));
        }
        assertNull(BowNumbers.row("bow.nothing"));
        List<String> both = new ArrayList<>(BowIds.ORDINARY);
        both.addAll(BowIds.CURSE);
        assertEquals(BowIds.ALL, both, "ORDINARY then CURSE is ALL");
    }

    @Test
    void heavyIsHeavyAndOrdinaryIsOrdinary() {
        for (SkillRow r : BowNumbers.ROWS) {
            if (r.budget() == Budget.HEAVY) assertTrue(r.raw() >= 10.0 && r.raw() <= 20.0, r.id() + " is labelled heavy but deals " + r.raw());
            if (r.budget() == Budget.ORDINARY && r.raw() > 0.0) assertTrue(r.raw() >= 3.0 && r.raw() <= 12.0, r.id() + " is ordinary but deals " + r.raw());
            if (r.kind() == Kind.ULTIMATE) assertTrue(r.raw() <= 32.0, r.id());
        }
    }

    @Test
    void thereAreAtLeastFourteenSkillsAndTwoUltimates() {
        assertTrue(BowNumbers.ROWS.size() >= 14);
        int ultimates = 0;
        for (SkillRow r : BowNumbers.ROWS) if (r.kind() == Kind.ULTIMATE) ultimates++;
        assertEquals(2, ultimates, "Starfall Volley and Autocracy Justice");
        assertEquals(4, BowIds.CURSE.size(), "about four curse skills");
    }

    @Test
    void theBowIsRangeAndUtilityNotTheSwordsDamage() {
        // a full draw takes 20 ticks and the next arrow may leave 4 ticks after the last: the plain arrow's raw damage per second at its strongest
        double perSecond = BowNumbers.BASIC_MAX_RAW / ((DrawMath.FULL_DRAW + BowNumbers.SHOT_LOCK) / 20.0);
        assertTrue(perSecond <= 10.0, "the plain arrow at its very strongest does " + perSecond + " raw damage a second");
        double usual = BowNumbers.BASIC_RAW / ((DrawMath.FULL_DRAW + BowNumbers.SHOT_LOCK) / 20.0);
        assertTrue(usual <= 6.0, "the plain arrow of an untrained bow does " + usual + " raw damage a second");
        assertTrue(BowNumbers.BASIC_MAX_RAW <= 12.0, "the plain arrow keeps the ordinary budget");
        assertTrue(BowNumbers.BASIC_RAW <= BowNumbers.BASIC_MAX_RAW);
    }

    @Test
    void theHitsOfOneCastAddUpToTheRow() {
        assertEquals(BowNumbers.RAIN_WAVES * BowNumbers.RAIN_RAW_WAVE, BowNumbers.ARROW_RAIN.raw(), 1e-9);
        assertEquals(BowNumbers.FROZEN_WAVES * BowNumbers.FROZEN_RAW_WAVE, BowNumbers.FROZEN_RAIN.raw(), 1e-9);
        assertEquals(BowNumbers.STARFALL_WAVES * BowNumbers.STARFALL_RAW_WAVE, BowNumbers.STARFALL_VOLLEY.raw(), 1e-9);
        assertEquals(BowNumbers.AUTOCRACY_WAVES * BowNumbers.AUTOCRACY_RAW_WAVE, BowNumbers.AUTOCRACY_JUSTICE.raw(), 1e-9);
        assertEquals(BowNumbers.SQUALL_ARROWS * BowNumbers.SQUALL_PER_ARROW, BowNumbers.ARROW_SQUALL.raw(), 1e-9);
        assertTrue(BowNumbers.ARROW_RAIN.raw() <= 12.0 && BowNumbers.ARROW_SQUALL.raw() <= 12.0 && BowNumbers.FROZEN_RAIN.raw() <= 12.0);
    }

    @Test
    void repeatedBlowsOnOneFoeAreFarEnoughApartForTheGameToCountThemAll() {
        int gap = SkillRow.HURT_IMMUNITY;
        assertTrue(BowNumbers.RAIN_INTERVAL >= gap && BowNumbers.FROZEN_INTERVAL >= gap && BowNumbers.STARFALL_INTERVAL >= gap && BowNumbers.AUTOCRACY_INTERVAL >= gap,
                "a wave that comes sooner than the hurt immunity of the last one does nothing to a foe that stood in both");
        assertTrue(BowNumbers.SECOND_DELAY >= gap, "a second arrow weaker than the first and sooner than the immunity would be ignored");
    }

    @Test
    void theSecondArrowNeverBreaksTheOrdinaryBudget() {
        for (double first = 0.5; first <= BowNumbers.BASIC_MAX_RAW; first += 0.5) {
            double second = BowNumbers.secondRaw(first);
            assertTrue(second >= 0.0, "never negative");
            assertTrue(first + second <= BowNumbers.BASIC_MAX_RAW + 1e-9, "first " + first + " + second " + second + " is above the budget");
            assertTrue(second <= first * BowNumbers.SECOND_FACTOR + 1e-9, "never more than the share");
        }
        assertEquals(4.2, BowNumbers.secondRaw(7.0), 1e-9, "a normal full draw: 60% of 7");
        assertEquals(2.2, BowNumbers.secondRaw(9.8), 1e-9, "a strong bow: only what is left of the budget");
        assertEquals(0.0, BowNumbers.secondRaw(12.0), 1e-9);
    }

    @Test
    void nothingRepeatsForLongerThanTwentySeconds() {
        int limit = 20 * 20;
        assertTrue(BowNumbers.RAIN_DELAY + (BowNumbers.RAIN_WAVES - 1) * BowNumbers.RAIN_INTERVAL <= limit);
        assertTrue(BowNumbers.FROZEN_DELAY + (BowNumbers.FROZEN_WAVES - 1) * BowNumbers.FROZEN_INTERVAL <= limit);
        assertTrue(BowNumbers.STARFALL_DELAY + (BowNumbers.STARFALL_WAVES - 1) * BowNumbers.STARFALL_INTERVAL <= limit);
        assertTrue(BowNumbers.AUTOCRACY_DELAY + (BowNumbers.AUTOCRACY_WAVES - 1) * BowNumbers.AUTOCRACY_INTERVAL <= limit);
        assertTrue(BowNumbers.SEEK_LIFE <= limit && BowNumbers.ARMED_TICKS <= limit && BowNumbers.LAW_TICKS <= limit);
        assertTrue(BowNumbers.NET_ROOT <= 60 && BowNumbers.BULL_ROOT <= 60, "a root never passes the 3 seconds a player may be held");
    }

    @Test
    void theLongestShotIsTheSniperAndNothingGoesPastIt() {
        assertTrue(BowNumbers.RANGE_MAX * BowNumbers.FARSIGHT <= BowNumbers.SNIPER_RANGE + 1e-9 || DrawMath.range(1.0, true) <= BowNumbers.SNIPER_RANGE);
        assertTrue(BowNumbers.SNIPER_RANGE <= 64.0);
        assertTrue(BowNumbers.METEOR_RANGE <= BowNumbers.SNIPER_RANGE && BowNumbers.RAIN_RANGE <= BowNumbers.SNIPER_RANGE);
    }

    @Test
    void descriptionsCarryTheirNumbers() {
        assertTrue(BowNumbers.PIERCING_SHOT.desc().contains("64 blocks") && BowNumbers.PIERCING_SHOT.desc().contains("90%"), BowNumbers.PIERCING_SHOT.desc());
        assertTrue(BowNumbers.NET_ARROW.desc().contains("50%") && BowNumbers.NET_ARROW.desc().contains("1.5 seconds"), BowNumbers.NET_ARROW.desc());
        assertTrue(BowNumbers.ARROW_SQUALL.desc().contains("7 arrows") && BowNumbers.ARROW_SQUALL.desc().contains("40 degree"), BowNumbers.ARROW_SQUALL.desc());
        assertTrue(BowNumbers.METEOR_BOW.desc().contains("1.2 seconds"), BowNumbers.METEOR_BOW.desc());
        assertTrue(BowNumbers.SECOND_ARROW.desc().contains("60%") && BowNumbers.SECOND_ARROW.desc().contains("12 base damage"), BowNumbers.SECOND_ARROW.desc());
        assertEquals("30%", BowNumbers.pct(0.3));
        assertEquals("6", BowNumbers.n(6.0));
        assertEquals("1.7", BowNumbers.n(1.7));
    }
}
