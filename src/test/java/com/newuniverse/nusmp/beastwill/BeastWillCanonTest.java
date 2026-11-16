package com.newuniverse.nusmp.beastwill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (docs/beastwill_canon_rework.md): the pure rules of the canon rework decide which data the game reads, what the mastery milestones give, how far a devouring bite heals, how a thunder chain falls
 * off, which pulses set foes alight and how the rank moves the unlocks. A wrong number here would change every will at once, so each rule is pinned on its own. Pure: no game, no files.
 */
class BeastWillCanonTest {
    @Test
    void theFolderChoiceIsClassicThenCanonThenTheRemadeWills() {
        assertEquals("beast_will_classic", BeastWillCanon.rootFor(true, true));
        assertEquals("beast_will_classic", BeastWillCanon.rootFor(true, false));
        assertEquals("beast_will_canon", BeastWillCanon.rootFor(false, true));
        assertEquals("beast_will", BeastWillCanon.rootFor(false, false), "false restores the remade wills exactly");
        assertEquals(BeastWillCanon.BASE_ROOT, BeastWillLoader.ROOT);
        assertEquals(BeastWillCanon.CLASSIC_ROOT, BeastWillLoader.CLASSIC_ROOT);
    }

    @Test
    void milestonesCountAQuarterAHalfThreeQuartersAndAll() {
        assertEquals(0, BeastWillCanon.milestone(0.0));
        assertEquals(0, BeastWillCanon.milestone(0.249));
        assertEquals(1, BeastWillCanon.milestone(0.25));
        assertEquals(2, BeastWillCanon.milestone(0.5));
        assertEquals(3, BeastWillCanon.milestone(0.75));
        assertEquals(3, BeastWillCanon.milestone(0.99));
        assertEquals(4, BeastWillCanon.milestone(1.0));
        assertEquals(4, BeastWillCanon.milestone(7.0), "a mastery above 1 counts as full");
    }

    @Test
    void theCooldownFactorOnlyShortensAndNeverPassesTenPercent() {
        double last = 1.0;
        for (double m : new double[] {0.0, 0.25, 0.5, 0.75, 1.0}) {
            double f = BeastWillCanon.cooldownFactor(true, m);
            assertTrue(f <= last + 1e-12 && f >= 0.9 - 1e-12, "mastery " + m + " gives " + f);
            last = f;
            assertEquals(1.0, BeastWillCanon.cooldownFactor(false, m), "the setting off restores the old cooldowns");
        }
        assertEquals(0.90, BeastWillCanon.cooldownFactor(true, 1.0), 1e-12);
        assertEquals(1.0, BeastWillCanon.cooldownFactor(true, 0.4), 1e-12, "nothing before half mastery");
        assertEquals(0.95, BeastWillCanon.cooldownFactor(true, 0.5), 1e-12);
        assertEquals(0.92, BeastWillCanon.cooldownFactor(true, 0.75), 1e-12);
    }

    @Test
    void theDevourHealIsAShareOfTheDamageAndAlwaysCapped() {
        assertEquals(0f, BeastWillCanon.devourHeal(0));
        assertEquals(0f, BeastWillCanon.devourHeal(-10), "a negative hit heals nothing");
        assertEquals(4f, BeastWillCanon.devourHeal(10), 1e-6);
        assertEquals(BeastWillCanon.DEVOUR_MAX_HEAL, BeastWillCanon.devourHeal(1_000_000), 1e-6);
        assertEquals(6f, BeastWillCanon.DEVOUR_MAX_HEAL, "three hearts at most");
    }

    @Test
    void aThunderChainKeepsEightyPercentPerHop() {
        assertEquals(10.0, BeastWillCanon.chainDamage(10, 0), 1e-9);
        assertEquals(8.0, BeastWillCanon.chainDamage(10, 1), 1e-9);
        assertEquals(6.4, BeastWillCanon.chainDamage(10, 2), 1e-9);
        assertEquals(10.0, BeastWillCanon.chainDamage(10, -3), 1e-9, "a negative hop is the first");
        assertEquals(0.0, BeastWillCanon.chainDamage(-5, 1), 1e-9);
        double sum = 0;
        for (int hop = 0; hop < 5; hop++) sum += BeastWillCanon.chainDamage(10, hop);
        assertTrue(sum < 10 * 5, "five hops deal less than five full hits");
    }

    @Test
    void onlyFieryWillsIgniteWithTheReworkOn() {
        for (String f : List.of("Fire", "Storm Fire", "magma", "Ember heat", "BLAZE", "inferno")) assertTrue(BeastWillCanon.fiery(f), f);
        for (String f : List.of("Earth", "Wind", "Stone", "Deep Sea", "Shadow", "", "Plant")) assertFalse(BeastWillCanon.fiery(f), f);
        assertFalse(BeastWillCanon.fiery(null));
        assertTrue(BeastWillCanon.ignites(5, "Fire", true));
        assertFalse(BeastWillCanon.ignites(5, "Earth", true), "an earth pulse no longer burns");
        assertTrue(BeastWillCanon.ignites(5, "Earth", false), "with the rework off the old rule holds");
        assertFalse(BeastWillCanon.ignites(2.9, "Fire", true), "a weak pulse never ignites");
        assertFalse(BeastWillCanon.ignites(2.9, "Fire", false));
    }

    @Test
    void theRankMovesTheUnlocksBackTheHigherTheBeast() {
        assertEquals(0.10, BeastWillCanon.acquireUnlock(BeastRank.A), 1e-12);
        assertEquals(0.15, BeastWillCanon.acquireUnlock(BeastRank.S), 1e-12);
        assertEquals(0.20, BeastWillCanon.acquireUnlock(BeastRank.SS), 1e-12);
        assertEquals(0.50, BeastWillCanon.integrateMastery(BeastRank.A), 1e-12);
        assertEquals(0.60, BeastWillCanon.integrateMastery(BeastRank.S), 1e-12);
        assertEquals(0.70, BeastWillCanon.integrateMastery(BeastRank.SS), 1e-12);
        for (BeastRank r : BeastRank.values()) {
            assertTrue(BeastWillCanon.acquireUnlock(r) <= 0.5, "the validator needs the first form at half or less: " + r);
            assertTrue(BeastWillCanon.integrateMastery(r) >= 0.3, "the validator needs real mastery for the second form: " + r);
        }
        assertEquals(0.10, BeastWillCanon.acquireUnlock(null), 1e-12);
    }

    @Test
    void theNewKindsAreRegisteredAndBounded() {
        for (String k : List.of("tail_sweep", "thunder_chain", "devour")) {
            BeastWillRules.KindLimit l = BeastWillRules.KINDS.get(k);
            assertNotNull(l, k);
            assertEquals(0, l.maxDuration(), k + " is an instant hit");
            assertTrue(l.maxTargets() <= 6 && l.maxRadius() <= 6 && l.minCooldown() >= 100, k);
        }
        assertEquals(1, BeastWillRules.KINDS.get("devour").maxTargets(), "a bite hits one foe");
        assertEquals(18, BeastWillRules.KINDS.size());
    }

    @Test
    void twoWillsWithTheSameSignatureAreReported() {
        BeastWillDef.Ability a1 = ab("a_one", "shadow_dash", 1), a2 = ab("a_two", "bark_ward", 2), b1 = ab("b_one", "bark_ward", 1), b2 = ab("b_two", "shadow_dash", 2), c1 = ab("c_one", "devour", 1), c2 = ab("c_two", "devour", 2);
        BeastWillDef x = will("x_will", List.of(a1, a2), List.of(a1, a2));
        BeastWillDef y = will("y_will", List.of(b1, b2), List.of(c1));
        List<String> same = BeastWillCanon.signatureProblems(List.of(x, y));
        assertTrue(same.stream().anyMatch(s -> s.contains("same Acquire signature")), "the order of the kinds does not matter: " + same);
        BeastWillDef d = will("d_will", List.of(ab("dup", "ambush", 1)), List.of(ab("d_two", "devour", 1))), e = will("e_will", List.of(ab("dup", "devour", 1)), List.of(ab("e_two", "ambush", 1)));
        assertTrue(BeastWillCanon.signatureProblems(List.of(d, e)).stream().anyMatch(s -> s.contains("the ability id is used by")), "an ability id used by two wills is reported");
        BeastWillDef z = will("z_will", List.of(c1, c2), List.of(c1));
        assertTrue(BeastWillCanon.signatureProblems(List.of(z)).stream().anyMatch(s -> s.contains("both abilities")), "two abilities of one kind in a form share one cooldown");
        BeastWillDef p = will("p_will", List.of(ab("p_one", "ambush", 1), ab("p_two", "devour", 2)), List.of(ab("p_three", "ambush", 1)));
        BeastWillDef q = will("q_will", List.of(ab("q_one", "ambush", 1), ab("q_two", "tail_sweep", 2)), List.of(ab("q_three", "devour", 1)));
        assertEquals(List.of(), BeastWillCanon.signatureProblems(List.of(p, q)), "an Integrate with one kind each is fine when the pairs differ");
    }

    @Test
    void theProgressionRuleReadsTheRank() {
        BeastWillDef.Ability a = ab("one", "shadow_dash", 1);
        BeastWillDef s = rank("s_will", BeastRank.S, 0.15, 0.6, List.of(a));
        assertNull(BeastWillCanon.progressionProblem(s));
        assertNotNull(BeastWillCanon.progressionProblem(rank("s_will", BeastRank.S, 0.10, 0.6, List.of(a))), "an S beast does not unlock at ten percent");
        assertNotNull(BeastWillCanon.progressionProblem(rank("ss_will", BeastRank.SS, 0.20, 0.5, List.of(a))), "an SS beast needs 70 percent mastery");
        assertNull(BeastWillCanon.progressionProblem(rank("ss_will", BeastRank.SS, 0.20, 0.9, List.of(a))), "more mastery than the minimum is fine");
    }

    // ------------------------------------------------------------------ builders
    private static BeastWillDef.Ability ab(String id, String kind, int slot) {
        return new BeastWillDef.Ability(id, kind, slot, 5, 400, 5, 5, 3, 0, 1, 1, 0.5);
    }

    private static BeastWillDef.Phase phase(double unlockAssimilation, double unlockMastery, List<BeastWillDef.Ability> abilities) {
        return new BeastWillDef.Phase(unlockAssimilation, unlockMastery, 0.5, 0.1, 0, List.of(), abilities);
    }

    private static BeastWillDef will(String id, List<BeastWillDef.Ability> acquire, List<BeastWillDef.Ability> integrate) {
        return new BeastWillDef(id, "Test Will", "Beast", "Earth", false, BeastRank.A, List.of(), List.of("test"), 0.01, 5000, List.of("HUMAN"), 0.6, 3600, 1800, 0.25, 0.25, 3, 0xFFFFFF,
                List.of(), phase(0.1, 0, acquire), phase(0.95, 0.5, integrate));
    }

    private static BeastWillDef rank(String id, BeastRank r, double unlock, double mastery, List<BeastWillDef.Ability> abilities) {
        return new BeastWillDef(id, "Test Will", "Beast", "Earth", false, r, List.of(), List.of("test"), 0.01, 5000, List.of("HUMAN"), 0.6, 3600, 1800, 0.25, 0.25, 3, 0xFFFFFF,
                List.of(), phase(unlock, 0, abilities), phase(0.95, mastery, abilities));
    }
}
