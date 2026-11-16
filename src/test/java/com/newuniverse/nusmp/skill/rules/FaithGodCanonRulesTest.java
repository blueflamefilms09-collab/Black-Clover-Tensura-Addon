package com.newuniverse.nusmp.skill.rules;

import com.newuniverse.nusmp.skill.rules.FaithGodRules.God;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Move;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the canon pass of the eight Faith gods (docs/faith_gods.md "Canon pass", SERVER setting faithGods.canon) is a second table plus a few pure rules. This pins that the canon table is the same eight lines as
 * 4.6.0 (ids, classes, franchises, pool sizes), that every row cites an English wiki, that the unlock order, the gates, the holy factor and the health prices are bounded, and that the 4.6.0 table and its numbers
 * are untouched when the setting is off.
 */
class FaithGodCanonRulesTest {
    private static God canon(String id) { return FaithGodRules.pick(FaithGodRules.god(id), true); }

    @Test void theCanonTableIsTheSameEightLines() {
        assertEquals(FaithGodRules.GODS.size(), FaithGodRules.CANON_GODS.size());
        for (God g : FaithGodRules.GODS) {
            God c = canon(g.id());
            assertNotSame(g, c, g.id());
            assertSame(g, FaithGodRules.pick(g, false), "canon off is the 4.6.0 row itself");
            assertEquals(g.uniqueId(), c.uniqueId());
            assertEquals(g.ultimateId(), c.ultimateId());
            assertEquals(g.name(), c.name(), "the Unique keeps its registered name");
            assertEquals(g.cls(), c.cls(), "classes are kept: " + g.id());
            assertEquals(g.franchise(), c.franchise());
            assertEquals(FaithGodRules.POOL_ACTIVES, c.moves().size());
            assertEquals(FaithGodRules.POOL_PASSIVES, c.passives().size());
            assertNull(g.canonRules(), "the 4.6.0 row has no canon rules");
            assertNotNull(c.canonRules());
        }
        assertNull(FaithGodRules.pick(null, true));
    }

    @Test void everyCanonRowCitesAnEnglishWikiAndSaysWhy() {
        for (God c : FaithGodRules.CANON_GODS) {
            assertTrue(c.source().contains(".fandom.com") && c.source().contains("English"), "an English wiki source: " + c.id());
            assertFalse(c.canon().isBlank() || c.domain().isBlank());
            var k = c.canonRules();
            assertFalse(k.worship().isBlank() || k.limit().isBlank() || k.classWhy().isBlank(), c.id());
            assertTrue(c.rgb() >= 0 && c.rgb() <= 0xFFFFFF && (k.ultRgb() == -1 || (k.ultRgb() >= 0 && k.ultRgb() <= 0xFFFFFF)), "colours: " + c.id());
            assertTrue(k.base() > 0 && k.base() <= 1.0 && k.devotionMult() >= 1.0 && k.devotionMult() <= 3.0, "strength factors: " + c.id());
            assertTrue(c.offeringCount() >= 1 && c.offeringCount() <= 64 && c.offering().startsWith("minecraft:"), c.id());
            Set<String> ids = new HashSet<>();
            for (Move m : c.moves()) {
                assertTrue(ids.add(m.id()), c.id() + " repeats " + m.id());
                assertTrue(m.faith() >= 1 && m.faith() <= 5 && m.mp() > 0 && m.cooldownTicks() >= 100 && m.radius() >= 0 && m.power() >= 0, c.id() + "." + m.id());
                assertFalse(m.text().isBlank());
                assertTrue(m.unlock() >= 0.0 && m.unlock() <= 0.5, "an unlock a Unique can reach: " + m.id());
                assertTrue(m.minWorshippers() >= 0 && m.minWorshippers() <= 8 && m.hpFrac() >= 0.0 && m.hpFrac() <= 0.5, m.id());
            }
            Set<String> pids = new HashSet<>();
            for (var p : c.passives()) assertTrue(pids.add(p.id()) && !p.text().isBlank(), c.id() + " passive " + p.id());
        }
    }

    @Test void theUniqueOpensItsThreeMovesWithMasteryAndTheGodFormHasThemAll() {
        for (God c : FaithGodRules.CANON_GODS) {
            var m = FaithGodRules.actives(c, false);
            assertEquals(3, m.size());
            assertEquals(0.0, m.get(0).unlock(), c.id());
            assertTrue(m.get(0).unlock() <= m.get(1).unlock() && m.get(1).unlock() <= m.get(2).unlock(), "unlock order rises: " + c.id());
            assertTrue(FaithGodRules.unlocked(m.get(0), false, 0.0));
            for (Move x : c.moves()) assertTrue(FaithGodRules.unlocked(x, true, 0.0), "the god form has every move: " + x.id());
        }
        Move late = new Move("x", "X", FaithGodRules.Verb.BLESS, 0, 1, 1, 100, 1, "t", 0.5, 0, 0);
        assertFalse(FaithGodRules.unlocked(late, false, 0.49));
        assertTrue(FaithGodRules.unlocked(late, false, 0.5));
        assertFalse(FaithGodRules.unlocked(late, false, Double.NaN));
        Move legacy = new Move("y", "Y", FaithGodRules.Verb.BLESS, 0, 1, 1, 100, 1, "t");
        assertTrue(FaithGodRules.unlocked(legacy, false, 0.0), "a 4.6.0 move is always open");
        assertEquals(0.0, legacy.unlock());
        assertEquals(0, legacy.minWorshippers());
        assertEquals(0.0, legacy.hpFrac());
    }

    @Test void theOldTableKeepsItsExactStrength() {
        God g = FaithGodRules.god("femto");
        assertEquals(0.6 * 1.16, FaithGodRules.strength(g, false, 4, 4, 100), 1e-9);
        assertEquals(1.6 * 1.0, FaithGodRules.strength(g, true, 0, 4, 100), 1e-9);
        God a = FaithGodRules.god("aqua");
        assertEquals(0.6 * 1.16, FaithGodRules.strength(a, false, 4, 4, 100), 1e-9, "no canon base on the 4.6.0 row");
    }

    @Test void canonStrengthFollowsEachSeriesRules() {
        God aqua = canon("aqua"), luminus = canon("luminus"), lucifero = canon("lucifero"), femto = canon("femto");
        assertEquals(0.6 * 0.7, FaithGodRules.strength(aqua, false, 0, 4, 100), 1e-9, "Aqua is weakened on the mortal plane");
        assertEquals(0.6 * FaithGodRules.devotion(4, 8) * 0.7, FaithGodRules.strength(aqua, false, 4, 4, 100), 1e-9, "her followers count double");
        assertEquals(0.6 * FaithGodRules.devotion(4, 6), FaithGodRules.strength(luminus, false, 4, 4, 100), 1e-9, "Luminus: 1.5 times the devotion");
        assertEquals(0.6 * 0.7, FaithGodRules.strength(lucifero, false, 2, 4, 100), 1e-9, "first gate");
        assertEquals(0.6 * 1.0, FaithGodRules.strength(lucifero, false, 3, 4, 100), 1e-9, "second gate");
        assertEquals(0.6 * 1.3, FaithGodRules.strength(lucifero, false, 6, 4, 100), 1e-9, "third gate");
        assertEquals(FaithGodRules.strength(lucifero, false, 6, 4, 100), FaithGodRules.strength(lucifero, false, 60, 4, 100), 1e-9, "no linear devotion past the gates");
        assertEquals(0.6 * FaithGodRules.devotion(4, 4), FaithGodRules.strength(femto, false, 4, 4, 100), 1e-9, "Femto keeps the plain devotion");
        assertTrue(FaithGodRules.strength(aqua, true, 1_000_000, 20, 300) <= 1.0 * 2.0 * 3.0 * 0.7 + 1e-9, "still capped");
        assertEquals(1, FaithGodRules.gateOf(0));
        assertEquals(2, FaithGodRules.gateOf(3));
        assertEquals(3, FaithGodRules.gateOf(6));
    }

    @Test void theGodFormHasItsOwnColourWhereTheSeriesGivesOne() {
        God ssg = canon("super_saiyan_god");
        assertEquals(0xFF4A2A, FaithGodRules.rgbOf(ssg, false));
        assertEquals(0x38B8FF, FaithGodRules.rgbOf(ssg, true), "Super Saiyan Blue is blue");
        God lucifero = canon("lucifero");
        assertEquals(lucifero.rgb(), FaithGodRules.rgbOf(lucifero, true), "-1 keeps the line's colour");
        God old = FaithGodRules.god("super_saiyan_god");
        assertEquals(old.rgb(), FaithGodRules.rgbOf(old, true), "the 4.6.0 row has one colour");
    }

    @Test void masteryGainAndWorshipperRules() {
        assertEquals(1.0, FaithGodRules.masteryGain(false, 100_000));
        assertEquals(1.0, FaithGodRules.masteryGain(true, 100));
        assertEquals(5.0, FaithGodRules.masteryGain(true, 1000), 1e-9);
        assertEquals(1.0, FaithGodRules.masteryGain(true, -5));
        Move needs = new Move("m", "M", FaithGodRules.Verb.DOMAIN, 0, 1, 1, 100, 1, "t", 0, 2, 0);
        assertFalse(FaithGodRules.enoughWorshippers(needs, 1));
        assertTrue(FaithGodRules.enoughWorshippers(needs, 2));
        God ssg = canon("super_saiyan_god");
        assertFalse(FaithGodRules.ritualWorshippersOk(ssg, 4));
        assertTrue(FaithGodRules.ritualWorshippersOk(ssg, 5));
        assertTrue(FaithGodRules.ritualWorshippersOk(FaithGodRules.god("super_saiyan_god"), 0), "4.6.0: no worshipper minimum");
        for (God c : FaithGodRules.CANON_GODS) if (!c.id().equals("super_saiyan_god")) assertTrue(FaithGodRules.ritualWorshippersOk(c, 0), c.id());
    }

    @Test void healthPricesAreBoundedAndNeverKill() {
        assertEquals(0.0, FaithGodRules.lawCostFrac(0));
        assertEquals(0.0, FaithGodRules.lawCostFrac(1));
        assertEquals(0.08, FaithGodRules.lawCostFrac(3), 1e-9);
        assertEquals(0.24, FaithGodRules.lawCostFrac(24), 1e-9);
        assertEquals(0.24, FaithGodRules.lawCostFrac(1000), 1e-9);
        assertEquals(6.0, FaithGodRules.hpPaid(20.0, 20.0, 0.3), 1e-9);
        assertEquals(1.0, FaithGodRules.hpPaid(3.0, 20.0, 0.3), 1e-9, "never under one heart");
        assertEquals(0.0, FaithGodRules.hpPaid(2.0, 20.0, 0.3));
        assertEquals(0.0, FaithGodRules.hpPaid(0.0, 20.0, 0.3));
        assertEquals(0.0, FaithGodRules.hpPaid(Double.NaN, 20.0, 0.3));
        assertEquals(0.0, FaithGodRules.hpPaid(20.0, 20.0, -1.0));
        assertEquals(18.0, FaithGodRules.hpPaid(20.0, 20.0, 5.0), 1e-9, "a fraction above 1 is clamped, and the last heart stays");
        assertEquals(1.0, FaithGodRules.lifeBeamFactor(0.0, 20.0));
        assertEquals(1.6, FaithGodRules.lifeBeamFactor(6.0, 20.0), 1e-9);
        assertEquals(1.0, FaithGodRules.lifeBeamFactor(6.0, 0.0));
        assertEquals(1.0, FaithGodRules.lifeBeamFactor(-4.0, 20.0));
    }

    @Test void holyMagicBitesTheUndeadAndTheSuperSaiyanGodTiresUntilMastered() {
        assertEquals(1.6, FaithGodRules.holyFactor(true));
        assertEquals(0.6, FaithGodRules.holyFactor(false));
        God ssg = canon("super_saiyan_god");
        assertTrue(FaithGodRules.tollApplies(ssg, false, false));
        assertFalse(FaithGodRules.tollApplies(ssg, false, true), "mastered");
        assertFalse(FaithGodRules.tollApplies(ssg, true, false), "the god form is not tired");
        assertFalse(FaithGodRules.tollApplies(FaithGodRules.god("super_saiyan_god"), false, false), "off with canon off");
        assertFalse(FaithGodRules.tollApplies(canon("femto"), false, false));
    }

    @Test void theNewVerbsAreAppendedAfterTheOldOnes() {
        FaithGodRules.Verb[] v = FaithGodRules.Verb.values();
        assertEquals(FaithGodRules.Verb.BLESS, v[0]);
        assertEquals(FaithGodRules.Verb.DOMAIN, v[10], "the 4.6.0 verbs keep their order");
        assertEquals(FaithGodRules.Verb.WELL, v[11]);
        assertEquals(FaithGodRules.Verb.LIFE_BEAM, v[15]);
        assertEquals(16, v.length);
    }
}
