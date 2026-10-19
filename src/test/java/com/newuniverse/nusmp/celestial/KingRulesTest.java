package com.newuniverse.nusmp.celestial;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Gate's rules, the king's scaling, phases, legendary actions, the re-fight cooldown and the rewards. */
class KingRulesTest {
    private static final int ALL = CelestialKey.GOLD_MASK;

    // ------------------------------------------------------------------ the Gate
    @Test
    void theGateOpensOnlyWithAllTwelveBoundKeysAndTheGateKey() {
        assertEquals(KingRules.Gate.OK, KingRules.canOpen(ALL, ALL, true, 100, 0, false));
        assertEquals(KingRules.Gate.NO_GATE_KEY, KingRules.canOpen(ALL, ALL, false, 100, 0, false));
        assertEquals(KingRules.Gate.NOT_ALL_KEYS, KingRules.canOpen(ALL & ~CelestialKey.LEO.bit(), ALL, true, 100, 0, false));
        assertEquals(KingRules.Gate.NOT_BOUND, KingRules.canOpen(ALL, ALL & ~CelestialKey.VIRGO.bit(), true, 100, 0, false));
        assertEquals(KingRules.Gate.COOLDOWN, KingRules.canOpen(ALL, ALL, true, 100, 101, false));
        assertEquals(KingRules.Gate.OK, KingRules.canOpen(ALL, ALL, true, 101, 101, false), "the cooldown ends at its time");
        assertEquals(KingRules.Gate.ALREADY_OPEN, KingRules.canOpen(ALL, ALL, true, 100, 0, true));
    }

    @Test
    void elevenOfTwelveNeverOpensItWhichKeyIsMissing() {
        for (CelestialKey missing : CelestialKey.gold())
            assertEquals(KingRules.Gate.NOT_ALL_KEYS, KingRules.canOpen(ALL & ~missing.bit(), ALL, true, 0, 0, false), missing.id);
    }

    // ------------------------------------------------------------------ recognition
    @Test
    void recognitionNeedsTwelveBondedSpiritsAndHalfMastery() {
        int[] bond = new int[CelestialKey.values().length];
        for (CelestialKey k : CelestialKey.gold()) bond[k.ordinal()] = 8;                       // level 2
        assertTrue(KingRules.recognitionEarned(ALL, bond, 0.5, false));
        assertFalse(KingRules.recognitionEarned(ALL, bond, 0.49, false), "mastery");
        assertFalse(KingRules.recognitionEarned(ALL, bond, 1.0, true), "already recognised");
        assertFalse(KingRules.recognitionEarned(ALL & ~CelestialKey.ARIES.bit(), bond, 1.0, false), "a contract is missing");
        bond[CelestialKey.GEMINI.ordinal()] = 7;                                                  // level 1
        assertFalse(KingRules.recognitionEarned(ALL, bond, 1.0, false), "one spirit is not close enough");
        assertFalse(KingRules.recognitionEarned(ALL, null, 1.0, false));
        assertFalse(KingRules.recognitionEarned(ALL, new int[3], 1.0, false));
    }

    // ------------------------------------------------------------------ cooldowns
    @Test
    void theWaitAfterAVictoryIsLongerThanAfterADefeat() {
        assertTrue(KingRules.refightCooldown(true) > KingRules.refightCooldown(false) * 5);
        assertEquals(20L * 60 * 60, KingRules.refightCooldown(true));
        assertEquals(20L * 60 * 5, KingRules.refightCooldown(false));
    }

    // ------------------------------------------------------------------ scaling
    @Test
    void healthAndDamageScaleWithEpAndParty() {
        assertEquals(1200.0, KingRules.health(1.0, 1), 1e-9, "the floor");
        assertEquals(600.0 * 6.0, KingRules.health(6.0, 1), 1e-9);
        assertEquals(KingRules.health(6.0, 1) * 1.75, KingRules.health(6.0, 2), 1e-6, "+75% per extra player");
        assertEquals(KingRules.health(6.0, 1) * 4.75, KingRules.health(6.0, 6), 1e-6);
        assertEquals(KingRules.health(6.0, 6), KingRules.health(6.0, 40), 1e-6, "the party counts to six");
        assertEquals(KingRules.health(12.0, 1), KingRules.health(99.0, 1), 1e-6, "EP scale is capped at 12");
        assertEquals(1.0, KingRules.partyDamage(1), 1e-9);
        assertEquals(1.12, KingRules.partyDamage(2), 1e-9);
        assertEquals(1.6, KingRules.partyDamage(6), 1e-9);
        assertTrue(KingRules.attack(12.0) > KingRules.attack(1.0) && KingRules.attack(12.0) < KingRules.attack(1.0) * 4, "attack grows with the square root");
        assertEquals(9f, KingRules.cap(1000f, 20f), 1e-4f, "a blow is capped at 45% of the target's health");
        assertEquals(5f, KingRules.cap(5f, 20f), 1e-4f);
    }

    // ------------------------------------------------------------------ phases and actions
    @Test
    void phasesFollowTheHealthFraction() {
        assertEquals(1, KingRules.phase(1.0));
        assertEquals(1, KingRules.phase(0.67));
        assertEquals(2, KingRules.phase(0.66));
        assertEquals(2, KingRules.phase(0.34));
        assertEquals(3, KingRules.phase(0.33));
        assertEquals(3, KingRules.phase(0.0));
    }

    @Test
    void legendaryPointsGrowWithPhaseAndPartyAndPaySomeAction() {
        assertEquals(3, KingRules.points(1, 1));
        assertEquals(4, KingRules.points(2, 1));
        assertEquals(5, KingRules.points(3, 1));
        assertEquals(6, KingRules.points(3, 3));
        assertEquals(5, KingRules.points(99, 1), "phases above three count as three");
        for (int phase = 1; phase <= 3; phase++) {
            assertFalse(KingRules.available(phase).isEmpty());
            assertNotNull(KingRules.pick(phase, 1, 0), "one point always buys something (Star Fall)");
        }
    }

    @Test
    void theActionsOpenPhaseByPhaseAndTheJudgementIsLast() {
        List<KingRules.Action> p1 = KingRules.available(1), p2 = KingRules.available(2), p3 = KingRules.available(3);
        assertTrue(p1.size() < p2.size() && p2.size() < p3.size());
        assertTrue(p3.stream().anyMatch(a -> a.id().equals("judgement")));
        assertFalse(p2.stream().anyMatch(a -> a.id().equals("judgement")), "the Judgement is only in the last phase");
        assertEquals(3, KingRules.ACTIONS.stream().filter(a -> a.id().equals("judgement")).findFirst().orElseThrow().cost());
        assertNull(KingRules.pick(3, 0, 0), "no points, no action");
        assertEquals("star_fall", KingRules.pick(1, 1, 0).id());
        assertEquals("judgement", KingRules.pick(3, 3, 4).id(), "from the start index the first affordable action");
        for (KingRules.Action a : KingRules.ACTIONS) assertTrue(a.cost() >= 1 && a.cost() <= 3 && a.fromPhase() >= 1 && a.fromPhase() <= 3, a.id());
        assertTrue(KingRules.resists(3) && !KingRules.resists(0));
        assertEquals(3, KingRules.LAIR_ACTIONS.size());
    }

    // ------------------------------------------------------------------ rewards
    @Test
    void aVictoryGivesAllTheRewardsAndRecognitionTheSmallerOnes() {
        KingRules.Reward win = KingRules.reward(KingRules.Outcome.VICTORY), rec = KingRules.reward(KingRules.Outcome.RECOGNITION);
        assertTrue(win.hp() > rec.hp() && win.magicule() > rec.magicule() && win.aura() > rec.aura() && win.bond() > rec.bond());
        assertTrue(win.sceptre() && !rec.sceptre());
        assertTrue(win.summonMode() && !rec.summonMode());
        assertTrue(win.blessingMode() && rec.blessingMode());
        assertTrue(win.magicule() >= 0.25, "a large share of the maximum magicules");
        KingRules.Reward both = KingRules.total(true, true);
        assertEquals(win.hp() + rec.hp(), both.hp());
        assertEquals(win.magicule() + rec.magicule(), both.magicule(), 1e-9);
        assertEquals(win, KingRules.total(true, false));
        assertEquals(rec, KingRules.total(false, true));
        KingRules.Reward none = KingRules.total(false, false);
        assertEquals(0, none.hp());
        assertFalse(none.sceptre() || none.blessingMode() || none.summonMode());
    }

    @Test
    void theSummonIsExpensiveAndHasALongCooldown() {
        assertTrue(KingRules.SUMMON_COST_PERMILLE >= 300);
        assertTrue(KingRules.SUMMON_COOLDOWN >= 20 * 60 * 10);
        assertTrue(KingRules.SUMMON_STAY < 20 * 30);
    }
}
