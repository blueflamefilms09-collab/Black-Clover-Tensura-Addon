package com.newuniverse.nusmp.avatar.aptitude;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The bill of the Soul Forge and the odds of the Instant Skill Awakening, with exact numbers. */
class ForgeCostsTest {
    private static Aptitude apt(double tax) { return new Aptitude("x", "x", Aptitude.Rarity.RARE, tax, "", List.of()); }

    private static ForgeSkill skill(ForgeSkill.Kind k, double tax) { return new ForgeSkill("tensura:s", k, "s", tax, 0, 0, "", ""); }

    @Test
    void nothingChosenCostsNothingButTheCore() {
        ForgeCosts.Quote q = ForgeCosts.quote(List.of(), null, null, false, ForgeRules.DEFAULT, 5000);
        assertEquals(0.0, q.permanentTax(), 1e-12);
        assertEquals(0.0, q.upfrontMagicule(), 1e-12);
        assertEquals(1, q.coresNeeded());
        assertFalse(q.taxed());
    }

    @Test
    void thePartsAddUp() {
        ForgeCosts.Quote q = ForgeCosts.quote(List.of(apt(10), apt(8)), skill(ForgeSkill.Kind.UNIQUE, 6), skill(ForgeSkill.Kind.ULTIMATE, 12), true, ForgeRules.DEFAULT, 20_000);
        assertEquals(0.18, q.aptitudeTax(), 1e-9);
        assertEquals(0.18, q.skillTax(), 1e-9);
        assertEquals(0.05, q.awakeningTax(), 1e-9);
        assertEquals(0.41, q.permanentTax(), 1e-9);
        assertEquals(0.41 * 20_000, q.upfrontMagicule(), 1e-6);
        assertEquals(2, q.coresNeeded());
        assertTrue(q.taxed());
    }

    @Test
    void scalesMultiplyTheirOwnPart() {
        ForgeRules r = new ForgeRules(2, 3.0, 0.5, 0.9, 2.0, 5, 75, 0.05, 2, 3, 0.06, true);
        ForgeCosts.Quote q = ForgeCosts.quote(List.of(apt(10)), skill(ForgeSkill.Kind.UNIQUE, 10), null, true, r, 1000);
        assertEquals(0.30, q.aptitudeTax(), 1e-9);
        assertEquals(0.05, q.skillTax(), 1e-9);
        assertEquals(0.05, q.awakeningTax(), 1e-9);
        assertEquals(0.40, q.permanentTax(), 1e-9);
        assertEquals(0.40 * 2.0 * 1000, q.upfrontMagicule(), 1e-6);
        assertEquals(3, q.coresNeeded());
    }

    @Test
    void theTaxIsNeverMoreThanNinetyPercent() {
        List<Aptitude> many = new ArrayList<>();
        for (int i = 0; i < 8; i++) many.add(apt(90));
        assertEquals(0.9, ForgeCosts.quote(many, null, null, false, ForgeRules.DEFAULT, 100).permanentTax(), 1e-12);
    }

    @Test
    void nullEntriesAreIgnored() {
        List<Aptitude> l = new ArrayList<>();
        l.add(null);
        l.add(apt(10));
        assertEquals(0.10, ForgeCosts.quote(l, null, null, false, ForgeRules.DEFAULT, 1).permanentTax(), 1e-9);
        assertEquals(0.0, ForgeCosts.quote(null, null, null, false, ForgeRules.DEFAULT, -50).upfrontMagicule(), 1e-12);
    }

    // ---------------------------------------------------------------- Instant Skill Awakening: a d100 strictly below 5 + luck
    @Test
    void withNoLuckARollOfOneToFourAwakens() {
        for (int roll = 1; roll <= 4; roll++) assertTrue(ForgeCosts.awakens(ForgeRules.DEFAULT, 0.0, roll), "roll " + roll);
        for (int roll = 5; roll <= 100; roll++) assertFalse(ForgeCosts.awakens(ForgeRules.DEFAULT, 0.0, roll), "roll " + roll);
        assertEquals(0.04, ForgeCosts.awakeningChance(ForgeRules.DEFAULT, 0.0), 1e-12);
    }

    @Test
    void luckWidensTheWindowOnePointPerPoint() {
        assertEquals(0.14, ForgeCosts.awakeningChance(ForgeRules.DEFAULT, 10.0), 1e-12);
        assertTrue(ForgeCosts.awakens(ForgeRules.DEFAULT, 10.0, 14));
        assertFalse(ForgeCosts.awakens(ForgeRules.DEFAULT, 10.0, 15));
        assertEquals(0.07, ForgeCosts.awakeningChance(ForgeRules.DEFAULT, 2.5), 1e-12, "5 + 2.5 = 7.5: rolls 1..7");
    }

    @Test
    void theChanceIsCappedAndNeverNegative() {
        assertEquals(0.64, ForgeCosts.awakeningChance(ForgeRules.DEFAULT, 60.0), 1e-12, "the highest luck the game allows is still under the cap");
        assertEquals(0.75, ForgeCosts.awakeningChance(ForgeRules.DEFAULT, 90.0), 1e-12);
        assertFalse(ForgeCosts.awakens(ForgeRules.DEFAULT, 90.0, 76));
        assertTrue(ForgeCosts.awakens(ForgeRules.DEFAULT, 90.0, 75));
        assertEquals(0.0, ForgeCosts.awakeningChance(ForgeRules.DEFAULT, -50.0), 1e-12);
        assertEquals(0.04, ForgeCosts.awakeningChance(ForgeRules.DEFAULT, Double.NaN), 1e-12);
    }

    @Test
    void aRollOutsideOneToHundredNeverAwakens() {
        assertFalse(ForgeCosts.awakens(ForgeRules.DEFAULT, 99.0, 0));
        assertFalse(ForgeCosts.awakens(ForgeRules.DEFAULT, 99.0, 101));
        assertFalse(ForgeCosts.awakens(ForgeRules.DEFAULT, 99.0, -4));
    }

    @Test
    void theBaseComesFromTheRules() {
        ForgeRules r = new ForgeRules(2, 1, 1, 0.6, 1, 20, 75, 0.05, 1, 3, 0.06, true);
        assertEquals(0.19, ForgeCosts.awakeningChance(r, 0.0), 1e-12);
    }

    @Test
    void rulesClampNonsense() {
        ForgeRules r = new ForgeRules(99, -1, Double.NaN, 5, 1e9, -3, 500, 7, 99, 99, 9, false);
        assertEquals(8, r.maxAptitudes());
        assertEquals(0.0, r.aptitudeTaxScale(), 1e-12);
        assertEquals(0.0, r.skillTaxScale(), 1e-12);
        assertEquals(0.9, r.maxTotalTax(), 1e-12);
        assertEquals(10.0, r.upfrontFactor(), 1e-12);
        assertEquals(0.0, r.awakeningBase(), 1e-12);
        assertEquals(100.0, r.awakeningMaxChance(), 1e-12);
        assertEquals(0.9, r.awakeningTax(), 1e-12);
        assertEquals(8, r.awakeningExtraCores());
        assertEquals(20, r.rollsMax());
        assertEquals(1.0, r.luckStrength(), 1e-12);
    }
}
