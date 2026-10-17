package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.ForgeChoices;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The Soul Forge screen's rule logic: which pages are free to visit, how the two aptitude slots are shared, what Finalize would send, and what blocks it. */
class ForgeStateTest {
    private static final Map<String, Aptitude> APT = new HashMap<>();
    private static final Map<String, ForgeSkill> SKILL = new HashMap<>();
    private static final ForgeRules RULES = ForgeRules.DEFAULT;
    private static final ForgeSkill PREDATOR = new ForgeSkill("tensura:predator", ForgeSkill.Kind.UNIQUE, "Predator", 8, 1500, 0, "", "");
    private static final ForgeSkill GREAT_SAGE = new ForgeSkill("tensura:great_sage", ForgeSkill.Kind.UNIQUE, "Great Sage", 6, 0, 0, "", "");
    private static final ForgeSkill GLUTTONY = new ForgeSkill("tensura:gluttony", ForgeSkill.Kind.ULTIMATE, "Gluttony", 14, 3500, 0, "", "");

    static {
        for (Aptitude a : new Aptitude[]{
                new Aptitude("fire", "Fire", Aptitude.Rarity.COMMON, 0, "flame", List.of()),
                new Aptitude("water", "Water", Aptitude.Rarity.COMMON, 0, "water", List.of()),
                new Aptitude("spatial", "Spatial", Aptitude.Rarity.RARE, 10, "spatial", List.of()),
                new Aptitude("time", "Time", Aptitude.Rarity.LEGENDARY, 20, "time", List.of())}) APT.put(a.id(), a);
        for (ForgeSkill s : new ForgeSkill[]{PREDATOR, GREAT_SAGE, GLUTTONY}) SKILL.put(s.id(), s);
    }

    /** 10000 magicule, 5000 EP, no existing tax, two cores. */
    private static ForgeContext ctx() { return new ForgeContext(0.0, 10_000, 10_000, 5000, 0.0, 2, false, "", 0L, 0); }

    /** A state that has everything Finalize needs, nothing optional. */
    private static ForgeState ready() {
        ForgeState s = new ForgeState();
        s.onRolled();
        s.chooseRace("human", true);
        s.choosePrimary(AvatarPersonality.LOYAL);
        return s;
    }

    private static List<ForgeState.Problem> problems(ForgeState s, ForgeContext c, double raceCost) { return s.problems(RULES, c, raceCost, APT::get, SKILL::get); }

    // ================================================================ pages
    @Test
    void anyPageCanBeVisitedInAnyOrderAndTheEndsAreRespected() {
        ForgeState s = new ForgeState();
        assertEquals(ForgeState.PAGE_IDENTITY, s.page());
        assertTrue(s.goTo(ForgeState.PAGE_APTITUDES));
        assertEquals(4, s.page());
        assertFalse(s.goTo(5));
        assertFalse(s.goTo(-1));
        assertFalse(s.next(), "no page after the last");
        assertTrue(s.back());
        assertEquals(3, s.page());
        s.goTo(0);
        assertFalse(s.back(), "no page before the first");
        assertTrue(s.next());
        assertEquals(1, s.page());
    }

    @Test
    void whileBusyNoPageChangeIsAllowed() {
        ForgeState s = new ForgeState();
        s.setBusy(true);
        assertFalse(s.goTo(2));
        assertFalse(s.next());
        s.setBusy(false);
        assertTrue(s.goTo(2));
    }

    // ================================================================ personality
    @Test
    void thePrimaryAndSecondaryPersonalityNeverCoincide() {
        ForgeState s = new ForgeState();
        s.choosePrimary(AvatarPersonality.TSUNDERE);
        s.chooseSecondary(AvatarPersonality.YANDERE);
        assertEquals(AvatarPersonality.YANDERE, s.secondary());
        s.chooseSecondary(AvatarPersonality.TSUNDERE);
        assertNull(s.secondary(), "the same trope twice is not a hybrid");
        s.chooseSecondary(AvatarPersonality.LAZY);
        s.choosePrimary(AvatarPersonality.LAZY);
        assertNull(s.secondary(), "promoting the secondary to primary empties the secondary");
        assertEquals(AvatarPersonality.LAZY, s.primary());
    }

    // ================================================================ skills
    @Test
    void aSkillGoesToItsOwnSlotAndTogglesOff() {
        ForgeState s = new ForgeState();
        assertTrue(s.toggleSkill(PREDATOR));
        assertEquals("tensura:predator", s.unique());
        assertEquals("", s.ultimate());
        assertTrue(s.toggleSkill(GLUTTONY));
        assertEquals("tensura:gluttony", s.ultimate());
        assertTrue(s.toggleSkill(GREAT_SAGE), "another unique replaces the first");
        assertEquals("tensura:great_sage", s.unique());
        assertFalse(s.toggleSkill(GREAT_SAGE), "choosing it again clears the slot");
        assertEquals("", s.unique());
        assertEquals("tensura:gluttony", s.ultimate(), "the other slot is untouched");
        s.clearSkill(ForgeSkill.Kind.ULTIMATE);
        assertEquals("", s.ultimate());
        assertFalse(s.toggleSkill(null));
    }

    // ================================================================ aptitudes
    @Test
    void handPicksFillTheSlotsAndThenRefuse() {
        ForgeState s = new ForgeState();
        assertEquals(ForgeState.Pick.ADDED, s.togglePick("spatial", RULES));
        assertEquals(ForgeState.Pick.ADDED, s.togglePick("fire", RULES));
        assertEquals(ForgeState.Pick.FULL, s.togglePick("water", RULES), "two slots by default");
        assertEquals(List.of("spatial", "fire"), s.picks());
        assertEquals(ForgeState.Pick.REMOVED, s.togglePick("spatial", RULES));
        assertEquals(ForgeState.Pick.ADDED, s.togglePick("water", RULES));
        assertEquals(ForgeState.Pick.FULL, s.togglePick("", RULES));
        assertEquals(ForgeState.Pick.FULL, s.togglePick(null, RULES));
    }

    @Test
    void theFreeRollSharesTheSlotsWithHandPicks() {
        ForgeState s = new ForgeState();
        s.togglePick("spatial", RULES);
        s.beginRoll();
        assertTrue(s.rollPending());
        s.onRolledAptitude("time", 77L);
        assertFalse(s.rollPending());
        assertTrue(s.useRoll());
        assertEquals(2, s.aptitudeCount());
        assertEquals(ForgeState.Pick.FULL, s.togglePick("fire", RULES), "pick + roll fill both slots");
        assertEquals(ForgeState.Pick.ROLLED, s.togglePick("time", RULES), "you already hold it from the roll");
        assertEquals(ForgeState.Pick.REMOVED, s.togglePick("spatial", RULES));
        assertEquals(ForgeState.Pick.ADDED, s.togglePick("fire", RULES), "a freed slot is usable again");
    }

    @Test
    void aRollThatLandsOnAnAlreadyPickedOneTakesItOverFromTheHandPicks() {
        ForgeState s = new ForgeState();
        s.togglePick("fire", RULES);
        s.onRolledAptitude("fire", 5L);                    // a click made while the answer was on its way
        assertEquals(List.of(), s.picks());
        assertEquals("fire", s.rolledId());
        assertEquals(1, s.aptitudeCount());
    }

    @Test
    void theRollButtonIsLitOnlyWhenARollCanSucceed() {
        ForgeState s = new ForgeState();
        assertTrue(s.canRoll(RULES, 3));
        assertFalse(s.canRoll(RULES, 0), "no rolls left");
        s.beginRoll();
        assertFalse(s.canRoll(RULES, 3), "waiting for the server");
        s.rollFailed();
        assertTrue(s.canRoll(RULES, 3));
        s.setReelBusy(true);
        assertFalse(s.canRoll(RULES, 3), "the reel is still spinning");
        s.setReelBusy(false);
        s.togglePick("fire", RULES);
        s.togglePick("water", RULES);
        assertFalse(s.canRoll(RULES, 3), "two hand picks fill every slot");
        s.togglePick("water", RULES);
        assertTrue(s.canRoll(RULES, 3));
        assertFalse(s.canRoll(new ForgeRules(0, 1, 1, 0.6, 1, 5, 75, 0.05, 1, 3, 0.06, true), 3), "a world without aptitudes");
    }

    @Test
    void anAnsweredRollStopsTheWaitingButTheReelStillHoldsFinalizeBack() {
        ForgeState s = ready();
        s.beginRoll();
        assertFalse(s.canRoll(RULES, 3));
        s.rollAnswered();
        assertFalse(s.rollPending());
        s.setReelBusy(true);
        assertFalse(s.canRoll(RULES, 3), "the reel is spinning");
        assertFalse(s.canFinalize(problems(s, ctx(), 0)), "Finalize waits for the reel");
        s.setReelBusy(false);
        s.onRolledAptitude("time", 5L);
        assertTrue(s.canFinalize(problems(s, ctx(), 0)));
    }

    @Test
    void aRerollReplacesTheOldRollAndDiscardFreesTheSlot() {
        ForgeState s = new ForgeState();
        s.onRolledAptitude("fire", 5L);
        s.onRolledAptitude("time", 9L);
        assertEquals("time", s.rolledId());
        assertEquals(9L, s.rolledToken());
        assertEquals(1, s.aptitudeCount());
        s.discardRoll();
        assertFalse(s.useRoll());
        assertEquals(0, s.aptitudeCount());
        assertEquals("", s.rolledId());
        assertEquals(0L, s.choices().rollToken());
        s.onRolledAptitude("", 5L);
        s.onRolledAptitude("fire", 0L);
        assertFalse(s.useRoll(), "an empty answer changes nothing");
    }

    @Test
    void syncingWithTheServerFollowsItsRecord() {
        ForgeState s = new ForgeState();
        s.syncRoll("time", 42L);
        assertTrue(s.useRoll());
        assertEquals(42L, s.rolledToken());
        s.syncRoll("time", 42L);
        assertEquals(1, s.aptitudeCount());
        s.syncRoll("", 0L);
        assertFalse(s.useRoll(), "the server has no roll for us any more");
        s.syncRoll("", 0L);
        assertFalse(s.useRoll());
    }

    // ================================================================ what is sent
    @Test
    void choicesCarryTheTokenOnlyWhileTheRollIsKept() {
        ForgeState s = ready();
        s.togglePick("spatial", RULES);
        s.onRolledAptitude("time", 123L);
        s.toggleSkill(PREDATOR);
        s.toggleSkill(GLUTTONY);
        s.setPaidAwakening(true);
        ForgeChoices c = s.choices();
        assertEquals(List.of("spatial"), c.aptitudePicks());
        assertEquals(123L, c.rollToken());
        assertEquals("tensura:predator", c.unique());
        assertEquals("tensura:gluttony", c.ultimate());
        assertTrue(c.paidAwakening());
        s.discardRoll();
        assertEquals(0L, s.choices().rollToken());
    }

    // ================================================================ the bill
    @Test
    void theLedgerUsesTheServersCostMaths() {
        ForgeState s = ready();
        s.togglePick("spatial", RULES);              // 10%
        s.toggleSkill(PREDATOR);                     // 8%
        ForgeState.Ledger l = s.ledger(RULES, ctx(), 1000, APT::get, SKILL::get);
        assertEquals(0.18, l.quote().permanentTax(), 1e-9);
        assertEquals(1800.0, l.quote().upfrontMagicule(), 1e-6);
        assertEquals(2800.0, l.magiculeNeeded(), 1e-6, "the race price counts too");
        assertEquals(10_000.0, l.magiculeHeld(), 1e-6);
        assertEquals(0.18, l.totalTax(), 1e-9);
        assertEquals(0.60, l.taxCap(), 1e-9);
        assertFalse(l.magiculeShort());
        assertFalse(l.taxOver());
        assertFalse(l.coresShort());
        assertEquals(1, l.coresNeeded());
        assertEquals(0.04, l.awakeningChance(), 1e-9, "luck 0: strictly below 5 on a d100 is 1..4");
    }

    @Test
    void thePaidAwakeningCostsACoreAndTaxAndIsCertain() {
        ForgeState s = ready();
        s.setPaidAwakening(true);
        ForgeState.Ledger l = s.ledger(RULES, ctx(), 0, APT::get, SKILL::get);
        assertEquals(2, l.coresNeeded());
        assertEquals(0.05, l.quote().permanentTax(), 1e-9);
        assertEquals(1.0, l.awakeningChance(), 0.0);
        ForgeContext oneCore = new ForgeContext(0.0, 10_000, 10_000, 5000, 0.0, 1, false, "", 0L, 0);
        assertTrue(s.ledger(RULES, oneCore, 0, APT::get, SKILL::get).coresShort());
    }

    @Test
    void luckRaisesTheFreeAwakeningChance() {
        ForgeState s = ready();
        ForgeContext lucky = new ForgeContext(30.0, 10_000, 10_000, 5000, 0.0, 2, false, "", 0L, 0);
        assertEquals(0.34, s.ledger(RULES, lucky, 0, APT::get, SKILL::get).awakeningChance(), 1e-9);
    }

    @Test
    void theLedgerFlagsAShortPoolAndATaxOverTheCap() {
        ForgeState s = ready();
        s.togglePick("time", RULES);                 // 20%
        s.toggleSkill(GLUTTONY);                     // 14%
        s.toggleSkill(PREDATOR);                     // 8%
        ForgeContext tired = new ForgeContext(0.0, 100, 10_000, 5000, 0.30, 2, false, "", 0L, 0);
        ForgeState.Ledger l = s.ledger(RULES, tired, 500, APT::get, SKILL::get);
        assertTrue(l.magiculeShort());
        assertEquals(0.30 + 0.42, l.totalTax(), 1e-9);
        assertTrue(l.taxOver());
    }

    // ================================================================ problems
    @Test
    void anEmptyScreenListsTheFourThingsYouMustDoInPageOrder() {
        ForgeState s = new ForgeState();
        List<ForgeState.Problem> p = problems(s, ctx(), 0);
        assertEquals(3, p.size(), "stats, race, personality: a name is optional");
        assertEquals(ForgeState.PAGE_IDENTITY, p.get(0).page());
        assertEquals(ForgeState.PAGE_APPEARANCE, p.get(1).page());
        assertEquals(ForgeState.PAGE_TRAITS, p.get(2).page());
        assertFalse(s.canFinalize(p));
        for (ForgeState.Problem x : p) { assertFalse(x.soft()); assertFalse(x.text().isBlank()); }
    }

    @Test
    void aCompleteScreenHasNoProblemAndCanFinalize() {
        ForgeState s = ready();
        List<ForgeState.Problem> p = problems(s, ctx(), 1000);
        assertEquals(List.of(), p);
        assertTrue(s.canFinalize(p));
    }

    @Test
    void anUnavailableRaceAndABadNameBlock() {
        ForgeState s = ready();
        s.chooseRace("dragon", false);
        assertTrue(problems(s, ctx(), 0).stream().anyMatch(x -> x.code().equals("race") && x.page() == ForgeState.PAGE_APPEARANCE));
        s.chooseRace("human", true);
        s.setName("x");
        assertTrue(problems(s, ctx(), 0).stream().anyMatch(x -> x.code().equals("name") && x.page() == ForgeState.PAGE_IDENTITY));
        s.setName("Lia-Mae");
        assertEquals(List.of(), problems(s, ctx(), 0));
        s.setName(null);
        assertEquals("", s.name());
    }

    @Test
    void theValidatorsRefusalsBecomeProblemsOnTheRightPage() {
        ForgeState s = ready();
        s.toggleSkill(GLUTTONY);
        ForgeContext weak = new ForgeContext(0.0, 10_000, 10_000, 100, 0.0, 2, false, "", 0L, 0);
        ForgeState.Problem ep = problems(s, weak, 0).get(0);
        assertEquals("not_enough_ep", ep.code());
        assertEquals(ForgeState.PAGE_SKILLS, ep.page());
        assertFalse(ep.soft());

        ForgeState t = ready();
        t.setPaidAwakening(true);
        ForgeContext oneCore = new ForgeContext(0.0, 10_000, 10_000, 5000, 0.0, 1, false, "", 0L, 0);
        ForgeState.Problem cores = problems(t, oneCore, 0).get(0);
        assertEquals("need_cores", cores.code());
        assertEquals(ForgeState.PAGE_SKILLS, cores.page());
        assertFalse(cores.soft());
    }

    @Test
    void aShortPoolIsOnlyAWarningBecauseItCanChangeWhileTheScreenIsOpen() {
        ForgeState s = ready();
        s.togglePick("spatial", RULES);
        ForgeContext tired = new ForgeContext(0.0, 50, 10_000, 5000, 0.0, 2, false, "", 0L, 0);
        List<ForgeState.Problem> p = problems(s, tired, 0);
        assertEquals(1, p.size());
        assertEquals("not_enough_magicule", p.get(0).code());
        assertTrue(p.get(0).soft());
        assertEquals(-1, p.get(0).page());
        assertTrue(s.canFinalize(p), "the server gives the final answer; the button stays lit");
    }

    @Test
    void aTaxOverTheCapBlocksAndPointsNowhereInParticular() {
        ForgeState s = ready();
        s.togglePick("time", RULES);
        s.toggleSkill(GLUTTONY);
        s.toggleSkill(PREDATOR);
        ForgeContext taxed = new ForgeContext(0.0, 100_000, 10_000, 5000, 0.30, 2, false, "", 0L, 0);
        List<ForgeState.Problem> p = problems(s, taxed, 0);
        assertEquals("tax_cap", p.get(0).code());
        assertEquals(-1, p.get(0).page());
        assertFalse(s.canFinalize(p));
    }

    @Test
    void aSpinningReelOrAPendingRollBlocksFinalize() {
        ForgeState s = ready();
        s.beginRoll();
        List<ForgeState.Problem> p = problems(s, ctx(), 0);
        assertEquals("reel", p.get(0).code());
        assertFalse(s.canFinalize(p));
        s.rollFailed();
        s.setReelBusy(true);
        assertFalse(s.canFinalize(problems(s, ctx(), 0)));
        s.setReelBusy(false);
        assertTrue(s.canFinalize(problems(s, ctx(), 0)));
    }

    @Test
    void busyBlocksFinalizeEvenWithNoProblem() {
        ForgeState s = ready();
        s.setBusy(true);
        assertFalse(s.canFinalize(problems(s, ctx(), 0)));
    }

    @Test
    void aStaleRollTokenIsCaughtByTheSameValidatorTheServerUses() {
        ForgeState s = ready();
        s.onRolledAptitude("time", 77L);
        ForgeContext other = new ForgeContext(0.0, 10_000, 10_000, 5000, 0.0, 2, false, "fire", 99L, 0);
        // the state sends 'time'/77 to the validator as the server's record, so it is consistent; an unknown id is not
        assertEquals(List.of(), problems(s, ctx(), 0));
        s.onRolledAptitude("gone", 78L);
        List<ForgeState.Problem> p = problems(s, other, 0);
        assertEquals("unknown_aptitude", p.get(0).code());
        assertEquals(ForgeState.PAGE_APTITUDES, p.get(0).page());
    }

    // ================================================================ marks on the round buttons
    @Test
    void thePageMarksFollowTheChoices() {
        ForgeState s = new ForgeState();
        for (int pg = 0; pg < ForgeState.PAGES; pg++) assertFalse(s.pageDone(pg));
        List<ForgeState.Problem> p = problems(s, ctx(), 0);
        assertTrue(ForgeState.pageNeedsAttention(ForgeState.PAGE_IDENTITY, p));
        assertTrue(ForgeState.pageNeedsAttention(ForgeState.PAGE_APPEARANCE, p));
        assertTrue(ForgeState.pageNeedsAttention(ForgeState.PAGE_TRAITS, p));
        assertFalse(ForgeState.pageNeedsAttention(ForgeState.PAGE_SKILLS, p));
        assertFalse(ForgeState.pageNeedsAttention(ForgeState.PAGE_APTITUDES, p));
        ForgeState r = ready();
        assertTrue(r.pageDone(ForgeState.PAGE_IDENTITY));
        assertTrue(r.pageDone(ForgeState.PAGE_APPEARANCE));
        assertTrue(r.pageDone(ForgeState.PAGE_TRAITS));
        assertFalse(r.pageDone(ForgeState.PAGE_SKILLS));
        r.toggleSkill(PREDATOR);
        assertTrue(r.pageDone(ForgeState.PAGE_SKILLS));
        r.toggleSkill(PREDATOR);
        r.setPaidAwakening(true);
        assertTrue(r.pageDone(ForgeState.PAGE_SKILLS), "the awakening is on the skills page");
        assertFalse(r.pageDone(ForgeState.PAGE_APTITUDES));
        r.onRolledAptitude("fire", 1L);
        assertTrue(r.pageDone(ForgeState.PAGE_APTITUDES));
        assertFalse(r.pageDone(99));
    }
}
