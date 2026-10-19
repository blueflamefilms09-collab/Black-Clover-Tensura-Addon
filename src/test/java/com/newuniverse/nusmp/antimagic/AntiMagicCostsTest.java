package com.newuniverse.nusmp.antimagic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Aura economy of Anti-Magic (1.94.0): the cost table, the drain maths and the migration of stored AMP. Pure logic, no game needed. */
class AntiMagicCostsTest {
    private static final double EPS = 1e-9;

    @Test
    void everyPageHasAnAuraPriceInTheRightBand() {
        assertEquals(8, AntiMagicCosts.PAGES.size());
        for (var e : AntiMagicCosts.PAGES.entrySet()) {
            var c = e.getValue();
            assertTrue(c.frac() >= 0.01 && c.frac() <= 0.15, e.getKey() + " fraction " + c.frac());
            assertTrue(c.floor() > 0, e.getKey() + " has a floor");
        }
        // cheap utility 1 to 3 percent, strong 5 to 10 percent, ultimate more
        assertTrue(AntiMagicCosts.page("demon_slayer_slash").frac() <= 0.03);
        assertTrue(AntiMagicCosts.page("bull_thrust").frac() <= 0.03);
        assertTrue(AntiMagicCosts.page("infinite_slash").frac() >= 0.05 && AntiMagicCosts.page("infinite_slash").frac() <= 0.10);
        assertTrue(AntiMagicCosts.page("black_hurricane").frac() >= 0.05 && AntiMagicCosts.page("black_hurricane").frac() <= 0.10);
        assertTrue(AntiMagicCosts.page("infinite_slash_equinox").frac() > 0.10);
        assertTrue(AntiMagicCosts.page("black_form").frac() > 0.10);
        assertTrue(AntiMagicCosts.page("infinite_slash_equinox").frac() > AntiMagicCosts.page("infinite_slash").frac());
        assertTrue(AntiMagicCosts.page("black_divider").frac() > AntiMagicCosts.page("demon_slayer_slash").frac());
    }

    @Test
    void thePageOrderIsTheOrderOfTheBook() {
        assertEquals(java.util.List.of("demon_slayer_slash", "black_divider", "black_meteorite", "black_hurricane", "black_form", "bull_thrust",
                "infinite_slash", "infinite_slash_equinox"), java.util.List.copyOf(AntiMagicCosts.PAGES.keySet()));
    }

    @Test
    void aUniversalPageCostsNothingHere() {
        assertEquals(0, AntiMagicCosts.page("mana_strike").amount(10000), EPS);
        assertEquals(0, AntiMagicCosts.page("summon_grimoire").frac(), EPS);
    }

    @Test
    void costIsTheFractionOfTheMaxWithAFloor() {
        var c = AntiMagicCosts.page("black_divider");               // 4 percent, floor 15
        assertEquals(400, c.amount(10000), EPS);
        assertEquals(15, c.amount(100), EPS);                      // 4 of 100 is below the floor
        assertEquals(15, c.amount(0), EPS);
        assertEquals(15, c.amount(-50), EPS);
    }

    @Test
    void theFailureMessageIsTheUsualOne() {
        assertEquals("Not enough aura to hold Black Form.", AntiMagicCosts.notEnough("hold Black Form"));
        assertTrue(AntiMagicCosts.notEnough("x").startsWith("Not enough aura to "));
    }

    @Test
    void payingTakesAuraFirstThenMagicules() {
        double[] r = AntiMagicCosts.pay(1000, 500, 1000, 800, 0.05, 10);          // 50 aura
        assertEquals(950, r[0], EPS);
        assertEquals(500, r[1], EPS);
        r = AntiMagicCosts.pay(20, 500, 1000, 800, 0.05, 10);                   // aura 20 < 50: 40 magicules instead
        assertEquals(20, r[0], EPS);
        assertEquals(460, r[1], EPS);
        assertNull(AntiMagicCosts.pay(20, 10, 1000, 800, 0.05, 10));            // neither pool can pay
        assertNull(AntiMagicCosts.pay(0, 0, 1000, 800, 0.0, 1));
    }

    @Test
    void theFloorAppliesToTheFallbackToo() {
        double[] r = AntiMagicCosts.pay(0, 100, 100, 100, 0.01, 12);              // 1 percent of 100 is 1, the floor is 12
        assertEquals(88, r[1], EPS);
        assertNull(AntiMagicCosts.pay(0, 11, 100, 100, 0.01, 12));
    }

    @Test
    void blackFormDrainsEverySecondAndRunsDry() {
        double max = 2000, aura = max;
        var drain = AntiMagicCosts.FORM_DRAIN;                                   // 1.5 percent = 30 a second
        assertEquals(30, drain.amount(max), EPS);
        assertEquals(66, AntiMagicCosts.secondsLeft(aura, max, drain));          // 2000 / 30
        int held = 0;
        while (true) {
            double[] r = AntiMagicCosts.pay(aura, 0, max, 0, drain.frac(), drain.floor());
            if (r == null) break;
            aura = r[0];
            held++;
            assertTrue(held < 1000, "the drain must end");
        }
        assertEquals(66, held);
        assertTrue(aura < 30, "less than a second is left when it ends");
    }

    @Test
    void theStartPriceIsLessThanTwoSecondsOfTheForm() {
        assertTrue(AntiMagicCosts.FORM_START.frac() <= 2 * AntiMagicCosts.FORM_DRAIN.frac());
        assertTrue(AntiMagicCosts.FIELD_DRAIN.frac() < AntiMagicCosts.FORM_DRAIN.frac());
    }

    @Test
    void aSmallPoolStillPaysTheFloorPerSecond() {
        assertEquals(3, AntiMagicCosts.FORM_DRAIN.amount(100), EPS);
        assertEquals(10, AntiMagicCosts.secondsLeft(30, 100, AntiMagicCosts.FORM_DRAIN));
        assertEquals(0, AntiMagicCosts.secondsLeft(2, 100, AntiMagicCosts.FORM_DRAIN));
    }

    @Test
    void auraShareIsClamped() {
        assertEquals(0.5, AntiMagicCosts.auraShare(50, 100), EPS);
        assertEquals(1, AntiMagicCosts.auraShare(500, 100), EPS);
        assertEquals(0, AntiMagicCosts.auraShare(-5, 100), EPS);
        assertEquals(0, AntiMagicCosts.auraShare(5, 0), EPS);
    }

    // ------------------------------------------------------------------ migration
    @Test
    void aLordWithStoredAmpKeepsEverything() {
        var out = AntiMagicCosts.migrate(new AntiMagicCosts.Stored(true, true, 640, 0), false);
        assertTrue(out.lord());
        assertTrue(out.form());
        assertEquals(640, out.amp());
        assertEquals(AntiMagicCosts.DATA_VERSION, out.version());
    }

    @Test
    void aLegacyLordSkillCountsAsAwakened() {
        var out = AntiMagicCosts.migrate(new AntiMagicCosts.Stored(false, false, 120, 0), true);
        assertTrue(out.lord());
        assertEquals(120, out.amp());
    }

    @Test
    void aFormWithoutTheLordIsDropped() {
        var out = AntiMagicCosts.migrate(new AntiMagicCosts.Stored(false, true, 0, 0), false);
        assertFalse(out.lord());
        assertFalse(out.form());
    }

    @Test
    void ampIsClampedAndMigrationIsIdempotent() {
        var once = AntiMagicCosts.migrate(new AntiMagicCosts.Stored(true, false, 99999, 0), false);
        assertEquals(AntiMagicCosts.AMP_CAP, once.amp());
        assertEquals(once, AntiMagicCosts.migrate(once, false));
        assertEquals(0, AntiMagicCosts.migrate(new AntiMagicCosts.Stored(false, false, -5, 0), false).amp());
        assertEquals(7, AntiMagicCosts.migrate(new AntiMagicCosts.Stored(false, false, 1, 7), false).version(), "a newer version is never lowered");
    }

    @Test
    void ampIsOnlyAStat() {
        assertTrue(AntiMagicCosts.ampIsOnlyAStat());
        assertNotNull(AntiMagicCosts.FIELD_DRAIN);
    }
}
