package com.newuniverse.nusmp.avatar.aptitude;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The server's judge of a Finalize request, played by a hacked client: too many aptitudes, an unknown id, a rare aptitude claimed as free, a stale roll
 * token, a skill in the wrong slot, a pool it cannot afford, a tax over the cap, a missing Genesis Core. Each ends in the documented refusal, and the honest
 * request ends in a plan whose numbers come only from the catalogs and the context.
 */
class ForgeValidatorTest {
    private static final Map<String, Aptitude> APT = new HashMap<>();
    private static final Map<String, ForgeSkill> SKILL = new HashMap<>();

    static {
        add(new Aptitude("fire", "Fire", Aptitude.Rarity.COMMON, 0, "flame", List.of("tensura:fire_ball")));
        add(new Aptitude("water", "Water", Aptitude.Rarity.COMMON, 0, "water", List.of()));
        add(new Aptitude("spatial", "Spatial", Aptitude.Rarity.RARE, 10, "spatial", List.of()));
        add(new Aptitude("time", "Time", Aptitude.Rarity.LEGENDARY, 20, "time", List.of()));
        add(new Aptitude("anti_magic", "Anti-Magic", Aptitude.Rarity.LEGENDARY, 25, "anti_magic", List.of()));
        SKILL.put("tensura:predator", new ForgeSkill("tensura:predator", ForgeSkill.Kind.UNIQUE, "Predator", 8, 1500, 0, "", ""));
        SKILL.put("tensura:gluttony", new ForgeSkill("tensura:gluttony", ForgeSkill.Kind.ULTIMATE, "Gluttony", 14, 3500, 0, "", ""));
    }

    private static void add(Aptitude a) { APT.put(a.id(), a); }

    private static final ForgeRules RULES = ForgeRules.DEFAULT;

    /** A creator with a full pool of 10000 magicule, 5000 EP, no tax yet, two cores, no roll. */
    private static ForgeContext ctx() { return new ForgeContext(0.0, 10_000, 10_000, 5000, 0.0, 2, false, "", 0L, 1000); }

    private static ForgeChoices picks(String... ids) { return new ForgeChoices(List.of(ids), 0L, "", "", false); }

    private static ForgeValidator.Verdict run(ForgeChoices c, ForgeContext x) { return run(c, x, RULES); }

    private static ForgeValidator.Verdict run(ForgeChoices c, ForgeContext x, ForgeRules r) { return ForgeValidator.validate(c, x, r, APT::get, SKILL::get); }

    private static void refused(ForgeValidator.Code code, ForgeValidator.Verdict v) {
        assertFalse(v.ok(), "expected a refusal " + code);
        assertEquals(code, v.refusal().code(), v.refusal().text());
        assertFalse(v.refusal().text().isBlank());
    }

    @Test
    void anHonestRequestBecomesAPlanWithTheServersOwnNumbers() {
        ForgeValidator.Verdict v = run(picks("spatial", "fire"), ctx());
        assertTrue(v.ok());
        ForgePlan p = v.plan();
        assertEquals(List.of("spatial", "fire"), p.aptitudes());
        assertEquals(0.10, p.permanentTax(), 1e-9, "only the rare pick is taxed");
        assertEquals(1000.0, p.upfrontMagicule(), 1e-6, "10% of a 10000 pool, paid now");
        assertEquals(1, p.coresNeeded());
        assertEquals("", p.rolledAptitude());
    }

    @Test
    void aCommonAptitudeIsFreeAndNoChoiceAtAllIsAFreeAvatar() {
        assertEquals(0.0, run(picks("fire", "water"), ctx()).plan().permanentTax(), 1e-12);
        ForgePlan none = run(ForgeChoices.NONE, ctx()).plan();
        assertEquals(0.0, none.permanentTax(), 1e-12);
        assertEquals(0.0, none.upfrontMagicule(), 1e-12);
        assertEquals(List.of(), none.aptitudes());
    }

    @Test
    void tooManyAptitudesAreRefused() {
        refused(ForgeValidator.Code.TOO_MANY_APTITUDES, run(picks("fire", "water", "spatial"), ctx()));
        refused(ForgeValidator.Code.TOO_MANY_APTITUDES, run(picks("fire"), ctx(), new ForgeRules(0, 1, 1, 0.6, 1, 5, 75, 0.05, 1, 3, 0.06, true)));
    }

    @Test
    void aThousandIdsFromAHackedClientAreStillJustARefusal() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 1000; i++) many.add(i % 2 == 0 ? "fire" : "water");
        ForgeValidator.Verdict v = run(new ForgeChoices(many, 0L, "", "", false), ctx());
        assertFalse(v.ok());
        List<String> withNulls = new ArrayList<>();
        withNulls.add(null);
        withNulls.add("fire");
        refused(ForgeValidator.Code.UNKNOWN_APTITUDE, run(new ForgeChoices(withNulls, 0L, "", "", false), ctx()));
    }

    @Test
    void unknownOrMalformedIdsAreRefused() {
        refused(ForgeValidator.Code.UNKNOWN_APTITUDE, run(picks("nope"), ctx()));
        refused(ForgeValidator.Code.UNKNOWN_APTITUDE, run(picks("fire; drop"), ctx()));
        refused(ForgeValidator.Code.UNKNOWN_APTITUDE, run(picks(""), ctx()));
        refused(ForgeValidator.Code.UNKNOWN_APTITUDE, run(picks("../../fire"), ctx()));
        assertTrue(run(picks("FIRE"), ctx()).ok(), "case is not a way to cheat, it is cleaned");
    }

    @Test
    void theSameAptitudeTwiceIsRefused() {
        refused(ForgeValidator.Code.DUPLICATE_APTITUDE, run(picks("fire", "fire"), ctx()));
        refused(ForgeValidator.Code.DUPLICATE_APTITUDE, run(picks("fire", "FIRE"), ctx()));
    }

    // ---------------------------------------------------------------- the free roll
    private static ForgeContext rolled(String id, long token) { return new ForgeContext(0.0, 10_000, 10_000, 5000, 0.0, 2, false, id, token, 1000); }

    @Test
    void theRolledAptitudeIsFree_evenAnAntiMagicOne() {
        ForgeValidator.Verdict v = run(new ForgeChoices(List.of(), 42L, "", "", false), rolled("anti_magic", 42L));
        assertTrue(v.ok());
        assertEquals(List.of("anti_magic"), v.plan().aptitudes());
        assertEquals("anti_magic", v.plan().rolledAptitude());
        assertEquals(0.0, v.plan().permanentTax(), 1e-12, "a rolled aptitude costs no tax");
        assertEquals(0.0, v.plan().upfrontMagicule(), 1e-12);
    }

    @Test
    void aHandPickOfTheSameRareAptitudeIsTaxed() {
        assertEquals(0.25, run(picks("anti_magic"), ctx()).plan().permanentTax(), 1e-9);
    }

    @Test
    void aForgedOrStaleTokenIsRefused() {
        refused(ForgeValidator.Code.ROLL_MISMATCH, run(new ForgeChoices(List.of(), 41L, "", "", false), rolled("anti_magic", 42L)));
        refused(ForgeValidator.Code.ROLL_MISMATCH, run(new ForgeChoices(List.of(), 42L, "", "", false), ctx()));        // no roll was ever made
        refused(ForgeValidator.Code.ROLL_MISMATCH, run(new ForgeChoices(List.of(), 42L, "", "", false), rolled("", 42L)));
        refused(ForgeValidator.Code.ROLL_MISMATCH, run(new ForgeChoices(List.of(), 7L, "", "", false), rolled("anti_magic", 0L)));
    }

    @Test
    void theRolledAptitudeCannotAlsoBeAHandPick() {
        refused(ForgeValidator.Code.DUPLICATE_APTITUDE, run(new ForgeChoices(List.of("fire"), 5L, "", "", false), rolled("fire", 5L)));
    }

    @Test
    void theRollTakesASlotOfTheLimit() {
        assertTrue(run(new ForgeChoices(List.of("spatial"), 5L, "", "", false), rolled("fire", 5L)).ok(), "one pick plus the roll fits two slots");
        refused(ForgeValidator.Code.TOO_MANY_APTITUDES, run(new ForgeChoices(List.of("spatial", "water"), 5L, "", "", false), rolled("fire", 5L)));
    }

    @Test
    void notUsingTheRollIgnoresWhatTheServerHolds() {
        ForgeValidator.Verdict v = run(picks("fire"), rolled("anti_magic", 42L));
        assertTrue(v.ok());
        assertEquals(List.of("fire"), v.plan().aptitudes());
    }

    // ---------------------------------------------------------------- skills
    private static ForgeChoices skills(String unique, String ultimate) { return new ForgeChoices(List.of(), 0L, unique, ultimate, false); }

    @Test
    void chosenSkillsAreTaxedByTheirEpTax() {
        ForgePlan p = run(skills("tensura:predator", "tensura:gluttony"), ctx()).plan();
        assertEquals("tensura:predator", p.unique());
        assertEquals("tensura:gluttony", p.ultimate());
        assertEquals(0.22, p.permanentTax(), 1e-9);
        assertEquals(2200.0, p.upfrontMagicule(), 1e-6);
        assertTrue(p.hasChosenSkill());
    }

    @Test
    void aSkillInTheWrongSlotOrUnknownIsRefused() {
        refused(ForgeValidator.Code.WRONG_SKILL_KIND, run(skills("tensura:gluttony", ""), ctx()));
        refused(ForgeValidator.Code.WRONG_SKILL_KIND, run(skills("", "tensura:predator"), ctx()));
        refused(ForgeValidator.Code.UNKNOWN_SKILL, run(skills("tensura:made_up", ""), ctx()));
        refused(ForgeValidator.Code.UNKNOWN_SKILL, run(skills("", "minecraft:stone"), ctx()));
    }

    @Test
    void aSkillNeedsTheCreatorsExistenceUnlessTheyAreInCreative() {
        ForgeContext weak = new ForgeContext(0.0, 10_000, 10_000, 1000, 0.0, 2, false, "", 0L, 0);
        refused(ForgeValidator.Code.NOT_ENOUGH_EP, run(skills("tensura:predator", ""), weak));
        ForgeContext edge = new ForgeContext(0.0, 10_000, 10_000, 1500, 0.0, 2, false, "", 0L, 0);
        assertTrue(run(skills("tensura:predator", ""), edge).ok(), "exactly the gate is enough");
        ForgeContext creative = new ForgeContext(0.0, 0, 10_000, 1, 0.0, 0, true, "", 0L, 0);
        assertTrue(run(skills("tensura:predator", "tensura:gluttony"), creative).ok());
    }

    // ---------------------------------------------------------------- the bill
    @Test
    void theTotalTaxIsCappedTogetherWithWhatTheCreatorAlreadyCarries() {
        ForgeContext heavy = new ForgeContext(0.0, 10_000, 10_000, 5000, 0.40, 2, false, "", 0L, 0);
        refused(ForgeValidator.Code.TAX_CAP, run(picks("anti_magic"), heavy));
        ForgeContext lighter = new ForgeContext(0.0, 10_000, 10_000, 5000, 0.30, 2, false, "", 0L, 0);
        assertTrue(run(picks("anti_magic"), lighter).ok(), "0.30 + 0.25 is under 0.60");
        assertTrue(run(picks("fire"), heavy).ok(), "an untaxed Avatar passes even when the creator is already over the line");
        assertEquals(0.45, run(picks("anti_magic", "time"), ctx()).plan().permanentTax(), 1e-9);
        ForgeContext some = new ForgeContext(0.0, 10_000, 10_000, 5000, 0.20, 2, false, "", 0L, 0);
        refused(ForgeValidator.Code.TAX_CAP, run(picks("anti_magic", "time"), some));        // 0.20 + 0.45 = 0.65 > 0.60
    }

    @Test
    void notEnoughMagiculeIsRefusedAtTheExactBoundary() {
        ForgeContext broke = new ForgeContext(0.0, 1999, 10_000, 5000, 0.0, 2, false, "", 0L, 1000);
        refused(ForgeValidator.Code.NOT_ENOUGH_MAGICULE, run(picks("spatial"), broke));       // 1000 (race) + 1000 (10% up front) = 2000
        ForgeContext exact = new ForgeContext(0.0, 2000, 10_000, 5000, 0.0, 2, false, "", 0L, 1000);
        assertTrue(run(picks("spatial"), exact).ok());
        ForgeContext creative = new ForgeContext(0.0, 0, 10_000, 5000, 0.0, 0, true, "", 0L, 1000);
        assertTrue(run(picks("spatial"), creative).ok(), "creative pays nothing");
    }

    @Test
    void theScalesChangeTheBill() {
        ForgeRules double2 = new ForgeRules(2, 2.0, 1.0, 0.9, 1.0, 5, 75, 0.05, 1, 3, 0.06, true);
        assertEquals(0.20, run(picks("spatial"), ctx(), double2).plan().permanentTax(), 1e-9);
        ForgeRules free = new ForgeRules(2, 0.0, 0.0, 0.9, 0.0, 5, 75, 0.05, 1, 3, 0.06, true);
        ForgePlan p = run(new ForgeChoices(List.of("anti_magic"), 0L, "tensura:predator", "tensura:gluttony", false), ctx(), free).plan();
        assertEquals(0.0, p.permanentTax(), 1e-12);
        assertEquals(0.0, p.upfrontMagicule(), 1e-12);
    }

    // ---------------------------------------------------------------- the paid awakening
    @Test
    void thePaidAwakeningCostsAnExtraCoreAndATax() {
        ForgeChoices c = new ForgeChoices(List.of(), 0L, "", "", true);
        ForgePlan p = run(c, ctx()).plan();
        assertEquals(2, p.coresNeeded());
        assertEquals(0.05, p.permanentTax(), 1e-9);
        assertTrue(p.paidAwakening());
        ForgeContext oneCore = new ForgeContext(0.0, 10_000, 10_000, 5000, 0.0, 1, false, "", 0L, 0);
        refused(ForgeValidator.Code.NEED_CORES, run(c, oneCore));
        ForgeContext creative = new ForgeContext(0.0, 0, 10_000, 0, 0.0, 0, true, "", 0L, 0);
        assertTrue(run(c, creative).ok());
    }

    @Test
    void theFirstReasonIsAlwaysTheSame() {
        ForgeContext broke = new ForgeContext(0.0, 0, 10_000, 0, 0.0, 0, false, "", 0L, 1000);
        refused(ForgeValidator.Code.UNKNOWN_APTITUDE, run(new ForgeChoices(List.of("nope", "spatial"), 0L, "tensura:gluttony", "", true), broke));
        refused(ForgeValidator.Code.WRONG_SKILL_KIND, run(new ForgeChoices(List.of("spatial"), 0L, "tensura:gluttony", "", true), broke));
        refused(ForgeValidator.Code.NOT_ENOUGH_EP, run(new ForgeChoices(List.of("spatial"), 0L, "tensura:predator", "", true), broke));
        refused(ForgeValidator.Code.NOT_ENOUGH_MAGICULE, run(new ForgeChoices(List.of("spatial"), 0L, "", "", true), broke));
        refused(ForgeValidator.Code.NEED_CORES, run(ForgeChoices.NONE, new ForgeContext(0.0, 99_999, 10_000, 5000, 0.0, 0, false, "", 0L, 0)));
    }

    @Test
    void thePlanNeverCarriesAnythingTheCatalogDidNotGive() {
        ForgePlan p = run(new ForgeChoices(List.of("SPATIAL"), 0L, "TENSURA:PREDATOR", "", false), ctx()).plan();
        assertEquals(List.of("spatial"), p.aptitudes());
        assertEquals("tensura:predator", p.unique());
    }
}
