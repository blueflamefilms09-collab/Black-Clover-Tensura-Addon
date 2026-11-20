package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.phase2.Phase2Rules.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the phase 2 contract rules (docs/devil_contract_phase2.md) are decided on the server and nothing can be played here, so every formula is pinned: chance, EP, stacking, share, caps, sever, sabotage. Pure. */
class Phase2RulesTest {
    private static final double EPS = 1e-9;

    @Test
    void theSummonChanceRisesWithARelicAndStaysInItsBounds() {
        assertEquals(0.35, Phase2Rules.chance(0.35, false, 0.35, 0.05), EPS);
        assertEquals(0.70, Phase2Rules.chance(0.35, true, 0.35, 0.05), EPS);
        assertEquals(1.0, Phase2Rules.chance(0.90, true, 0.35, 0.05), EPS, "never over 1");
        assertEquals(0.05, Phase2Rules.chance(0.0, false, 0.35, 0.05), EPS, "never under the floor");
        assertEquals(0.05, Phase2Rules.chance(Double.NaN, false, 0.35, 0.05), EPS, "a bad number reads as the floor");
        assertTrue(Phase2Rules.chance(0.35, true, 0.35, 0.05) > Phase2Rules.chance(0.35, false, 0.35, 0.05));
    }

    @Test
    void aRollSucceedsBelowTheChanceOnly() {
        assertTrue(Phase2Rules.roll(0.5, 0.0));
        assertTrue(Phase2Rules.roll(0.5, 0.4999));
        assertFalse(Phase2Rules.roll(0.5, 0.5));
        assertFalse(Phase2Rules.roll(0.0, 0.0));
        assertTrue(Phase2Rules.roll(1.0, 0.9999999));
    }

    @Test
    void theEpRequirementScalesDownByRankAndByTheServerScale() {
        Phase2Table t = Phase2Table.defaults();
        assertEquals(1.0E9, t.requiredEp("lucifero", DevilTier.SUPREME, 1.0), 1);
        assertEquals(1.0E8, t.requiredEp("liebe", DevilTier.HIGH, 1.0), 1);
        assertEquals(1.0E7, t.requiredEp("x", DevilTier.MID, 1.0), 1);
        assertEquals(1.0E6, t.requiredEp("x", DevilTier.LOW, 1.0), 1);
        assertEquals(5.0E8, t.requiredEp("x", DevilTier.SUPREME, 0.5), 1, "the server scale");
        assertEquals(0.0, t.requiredEp("x", DevilTier.SUPREME, 0.0), 0, "scale 0 = no requirement");
        assertEquals(42.0, Phase2Rules.requiredEp(42.0, 1.0E9, 1.0), EPS, "a devil's own number wins");
        for (DevilTier a : new DevilTier[] {DevilTier.LOW, DevilTier.MID, DevilTier.HIGH}) assertTrue(t.tierEp(a) < t.tierEp(DevilTier.values()[a.ordinal() + 1]), "a higher rank asks more");
        assertTrue(Phase2Rules.epOk(1.0E9, 1.0E9));
        assertFalse(Phase2Rules.epOk(1.0E9 - 1, 1.0E9));
        assertTrue(Phase2Rules.epOk(0, 0));
        assertFalse(Phase2Rules.epOk(Double.NaN, 5));
    }

    @Test
    void theStrongFactorRisesFromFiveToTenAndNeverPastIt() {
        assertEquals(1.0, Phase2Rules.strongFactor(0, 5, 10), EPS);
        assertEquals(5.0, Phase2Rules.strongFactor(1, 5, 10), EPS);
        assertEquals(7.5, Phase2Rules.strongFactor(2, 5, 10), EPS);
        assertEquals(10.0, Phase2Rules.strongFactor(3, 5, 10), EPS);
        assertEquals(10.0, Phase2Rules.strongFactor(9, 5, 10), EPS);
        assertEquals(5.0, Phase2Rules.strongFactor(4, 5, 5), EPS, "first = max is a flat factor");
        assertEquals(12.0, Phase2Rules.strongFactor(1, 12, 10), EPS, "a max under the first reads as the first");
    }

    @Test
    void weakDevilsStackFreelyUpToABoundAndStrongOnesNeedTheFactor() {
        double base = 1.0E8, rich = 1.0E10;
        assertTrue(Phase2Rules.stack(true, true, 3, 0, false, base, base, 8, 2, 5, 10).ok(), "a weak devil needs only its own requirement");
        assertFalse(Phase2Rules.stack(true, true, 8, 0, false, base, rich, 8, 2, 5, 10).ok(), "the weak bound");
        assertEquals("weak_cap", Phase2Rules.stack(true, true, 8, 0, false, base, rich, 8, 2, 5, 10).why());
        assertTrue(Phase2Rules.stack(true, false, 0, 0, true, 1.0E9, 1.0E9, 8, 2, 5, 10).ok(), "the first strong devil asks the single requirement");
        Phase2Rules.Stack second = Phase2Rules.stack(true, true, 0, 1, true, 1.0E9, 4.9E9, 8, 2, 5, 10);
        assertFalse(second.ok());
        assertEquals("ep_stack", second.why());
        assertEquals(5.0E9, second.needEp(), 1);
        assertTrue(Phase2Rules.stack(true, true, 0, 1, true, 1.0E9, 5.0E9, 8, 2, 5, 10).ok());
        assertEquals("strong_cap", Phase2Rules.stack(true, true, 0, 2, true, 1.0E9, 1.0E15, 8, 2, 5, 10).why(), "two Supreme at most, however rich");
        assertTrue(Phase2Rules.stack(true, true, 0, 2, true, 1.0E9, 1.0E11, 8, 3, 5, 10).ok(), "the cap is a setting");
        assertEquals(7.5E9, Phase2Rules.stack(true, true, 0, 2, true, 1.0E9, 1.0, 8, 3, 5, 10).needEp(), 1, "the third strong devil asks 7.5 times");
    }

    @Test
    void withStackingOffTheOldOneContractRuleHolds() {
        assertEquals("one_only", Phase2Rules.stack(false, true, 0, 0, false, 1, 1e12, 8, 2, 5, 10).why());
        assertTrue(Phase2Rules.stack(false, false, 0, 0, false, 1, 1e12, 8, 2, 5, 10).ok());
    }

    @Test
    void onlySupremeIsOneOfOneAndOnlyWhileSharingIsOn() {
        assertTrue(Phase2Rules.oneOfOne(true, DevilTier.SUPREME));
        for (DevilTier t : new DevilTier[] {DevilTier.LOW, DevilTier.MID, DevilTier.HIGH}) assertFalse(Phase2Rules.oneOfOne(true, t), t + " is shared");
        for (DevilTier t : DevilTier.values()) assertTrue(Phase2Rules.oneOfOne(false, t), "sharing off = the phase 1 rule for " + t);
        assertTrue(Phase2Rules.oneOfOne(true, null), "an unranked devil stays one-of-one");
    }

    @Test
    void theSharedShareSplitsEvenlyAndTheCapHolds() {
        assertEquals(1.0, Phase2Rules.split(1), EPS);
        assertEquals(0.25, Phase2Rules.split(4), EPS);
        assertEquals(1.0, Phase2Rules.split(0), EPS, "no holders reads as one");
        assertEquals(0.25, Phase2Rules.power(Kind.DOMINANCE, 4, 1.0), EPS, "25 percent each at four contracts");
        assertEquals(0.125, Phase2Rules.power(Kind.EVEN, 4, 1.0), EPS);
        assertEquals(0.0625, Phase2Rules.power(Kind.SUBMISSION, 4, 1.0), EPS);
        assertEquals(0.5, Phase2Rules.power(Kind.EVEN, 1, 1.0), EPS, "a lone even contract is 50-50");
        assertEquals(1.0, Phase2Rules.power(Kind.DOMINANCE, 1, 1.0), EPS, "dominance of a lone devil is the whole power");
        assertEquals(0.125, Phase2Rules.power(Kind.DOMINANCE, 4, 0.5), EPS, "the sabotage debuff halves it");
        assertTrue(Phase2Rules.power(Kind.EVEN, 3, 1.0) > Phase2Rules.power(Kind.EVEN, 4, 1.0), "the more contracts, the weaker each");
        assertTrue(Phase2Rules.power(Kind.EVEN, 4, 1.0) < Phase2Rules.power(Kind.DOMINANCE, 4, 1.0));
        assertEquals(25, Phase2Rules.percent(0.25));
        assertTrue(Phase2Rules.canJoin(3, 4));
        assertFalse(Phase2Rules.canJoin(4, 4));
        assertTrue(Phase2Rules.canJoin(0, 0), "a cap under 1 reads as 1: the first contract is always possible");
        assertFalse(Phase2Rules.canJoin(1, 0), "and the second is not");
    }

    @Test
    void theTypesNeedTheirTrialAndTheirEp() {
        // a devil that fights: nothing until impressed, dominance only after a win and with the larger EP
        assertFalse(Phase2Rules.kindAllowed(Kind.EVEN, true, true, true, true, Phase2Rules.TRIAL_NONE));
        assertTrue(Phase2Rules.kindAllowed(Kind.EVEN, true, true, true, true, Phase2Rules.TRIAL_IMPRESSED));
        assertTrue(Phase2Rules.kindAllowed(Kind.SUBMISSION, true, false, true, true, Phase2Rules.TRIAL_IMPRESSED));
        assertFalse(Phase2Rules.kindAllowed(Kind.DOMINANCE, true, true, true, true, Phase2Rules.TRIAL_IMPRESSED), "impressed is not beaten");
        assertTrue(Phase2Rules.kindAllowed(Kind.DOMINANCE, true, true, true, true, Phase2Rules.TRIAL_WON));
        assertFalse(Phase2Rules.kindAllowed(Kind.DOMINANCE, true, false, true, true, Phase2Rules.TRIAL_WON), "the win is not enough without the EP");
        // a devil that does not fight: even and submission at once, never dominance
        assertTrue(Phase2Rules.kindAllowed(Kind.EVEN, true, true, false, true, Phase2Rules.TRIAL_NONE));
        assertFalse(Phase2Rules.kindAllowed(Kind.DOMINANCE, true, true, false, true, Phase2Rules.TRIAL_NONE));
        // the trial switched off: the old way (no fight), dominance needs the fight so it is gone too
        assertTrue(Phase2Rules.kindAllowed(Kind.EVEN, true, true, true, false, Phase2Rules.TRIAL_NONE));
        assertFalse(Phase2Rules.kindAllowed(Kind.DOMINANCE, true, true, true, false, Phase2Rules.TRIAL_WON));
        assertFalse(Phase2Rules.kindAllowed(Kind.SUBMISSION, false, false, false, false, 0), "no EP, no contract of any kind");
        assertEquals("not_won", Phase2Rules.kindWhy(Kind.DOMINANCE, true, true, true, true, Phase2Rules.TRIAL_IMPRESSED));
        assertEquals("ep_dominance", Phase2Rules.kindWhy(Kind.DOMINANCE, true, false, true, true, Phase2Rules.TRIAL_WON));
        assertEquals("", Phase2Rules.kindWhy(Kind.EVEN, true, false, false, true, 0));
        assertEquals(3.0E9, Phase2Rules.dominanceEp(1.0E9, 3.0), 1);
        assertEquals(1.0E9, Phase2Rules.dominanceEp(1.0E9, 0.2), 1, "the factor is at least 1");
    }

    @Test
    void theKindNamesAndTheShareOrder() {
        assertEquals(Kind.SUBMISSION, Kind.byName("submit"));
        assertEquals(Kind.EVEN, Kind.byName("Even"));
        assertEquals(Kind.DOMINANCE, Kind.byName("dominate"));
        assertNull(Kind.byName("nonsense"));
        assertNull(Kind.byName(null));
        assertTrue(Kind.SUBMISSION.share < Kind.EVEN.share && Kind.EVEN.share < Kind.DOMINANCE.share);
        assertEquals(0.5, Kind.EVEN.share, EPS, "even is 50-50");
        assertTrue(Kind.SUBMISSION.corruption < Kind.EVEN.corruption, "submission is the safer contract");
    }

    @Test
    void theTrialResultOnlyClimbs() {
        assertEquals(Phase2Rules.TRIAL_NONE, Phase2Rules.trialResult(0, 0.9, false, 0.5));
        assertEquals(Phase2Rules.TRIAL_IMPRESSED, Phase2Rules.trialResult(0, 0.5, false, 0.5));
        assertEquals(Phase2Rules.TRIAL_WON, Phase2Rules.trialResult(0, 0.0, true, 0.5));
        assertEquals(Phase2Rules.TRIAL_IMPRESSED, Phase2Rules.trialResult(1, 0.9, false, 0.5), "healing does not take the result back");
    }

    @Test
    void killingAFellowHolderSeversOnlyTheSharedDevilsYouBothHold() {
        List<String> killer = List.of("liebe", "megicula", "lucifero"), victim = List.of("megicula", "lucifero", "fox_devil");
        java.util.function.Predicate<String> shared = d -> !d.equals("lucifero");
        assertEquals(List.of("megicula"), Phase2Rules.severed(true, false, killer, victim, shared), "lucifero is one-of-one, fox_devil is not held by the killer");
        assertTrue(Phase2Rules.severed(false, false, killer, victim, shared).isEmpty(), "the rule off severs nothing");
        assertTrue(Phase2Rules.severed(true, true, killer, victim, shared).isEmpty(), "dying by your own hand severs nothing");
        assertTrue(Phase2Rules.severed(true, false, List.of("liebe"), victim, shared).isEmpty(), "no common devil, no sever");
        assertTrue(Phase2Rules.severed(true, false, null, victim, shared).isEmpty());
    }

    @Test
    void theSabotageDebuffIsBoundedAndTheFriendlyFireCounterTrips() {
        assertEquals(1000L + 180 * 20L, Phase2Rules.debuffUntil(1000L, 180));
        assertEquals(1000L + 3600 * 20L, Phase2Rules.debuffUntil(1000L, 99999), "bounded to an hour");
        assertTrue(Phase2Rules.debuffActive(2000L, 1999L));
        assertFalse(Phase2Rules.debuffActive(2000L, 2000L));
        assertEquals(0.5, Phase2Rules.sabotageFactor(true, 0.5), EPS);
        assertEquals(1.0, Phase2Rules.sabotageFactor(false, 0.5), EPS);
        Phase2Rules.Fire f = new Phase2Rules.Fire(0, 0, false);
        for (int i = 1; i <= 4; i++) {
            f = Phase2Rules.friendlyFire(f.hits(), f.start(), 100L + i * 10, 30, 5);
            assertFalse(f.trip(), "hit " + i);
        }
        f = Phase2Rules.friendlyFire(f.hits(), f.start(), 150L, 30, 5);
        assertTrue(f.trip(), "the fifth blow inside the window trips it");
        assertEquals(0, f.hits(), "and the counter starts again");
        Phase2Rules.Fire slow = new Phase2Rules.Fire(0, 0, false);
        for (int i = 0; i < 20; i++) slow = Phase2Rules.friendlyFire(slow.hits(), slow.start(), 100L + i * 2000L, 30, 5);
        assertFalse(slow.trip(), "blows spread over a long time never trip it");
        assertFalse(Phase2Rules.friendlyFire(0, 0, 100L, 30, 0).trip(), "threshold 0 = off");
    }
}
