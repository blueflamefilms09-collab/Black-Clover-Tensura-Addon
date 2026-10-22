package com.newuniverse.nusmp.spiritcontrol;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the decisions of combat mode are pure functions, so the rules of the owner's brief are pinned without a game: only hostile mobs (or ones hunting the owner) are targets, a player only where the server
 * already counts them an enemy, never the owner, an ally or the owner's side; the spell is the strongest one that is ready, in range and affordable in BOTH pools, and the refusal says which wall was hit; the
 * power follows the bond and the pools within bounds; the damage is capped; and the decisions of different players fall on different ticks.
 */
class CombatRulesTest {
    // validTarget(self, alive, spectator, allied, ownerSide, isPlayer, playerEnemy, hostileMob, huntingOwner)
    @Test
    void onlyHostileMobsAndMobsHuntingTheOwnerAreTargets() {
        assertTrue(CombatRules.validTarget(false, true, false, false, false, false, false, true, false), "a monster");
        assertTrue(CombatRules.validTarget(false, true, false, false, false, false, false, false, true), "a passive mob that hunts the owner");
        assertFalse(CombatRules.validTarget(false, true, false, false, false, false, false, false, false), "a cow");
    }

    @Test
    void neverTheOwnerTheDeadAlliesOrTheOwnersSide() {
        assertFalse(CombatRules.validTarget(true, true, false, false, false, false, false, true, true), "the owner");
        assertFalse(CombatRules.validTarget(false, false, false, false, false, false, false, true, true), "a dead thing");
        assertFalse(CombatRules.validTarget(false, true, true, false, false, false, false, true, true), "a spectator");
        assertFalse(CombatRules.validTarget(false, true, false, true, false, false, false, true, true), "an ally");
        assertFalse(CombatRules.validTarget(false, true, false, false, true, false, false, true, true), "a pet, a summon or a teammate's");
    }

    @Test
    void aPlayerIsATargetOnlyWhereTheServerCountsThemAnEnemy() {
        assertFalse(CombatRules.validTarget(false, true, false, false, false, true, false, false, false), "a player with no enmity");
        assertFalse(CombatRules.validTarget(false, true, false, false, false, true, false, true, true), "a player is never a target for being 'hostile'");
        assertTrue(CombatRules.validTarget(false, true, false, false, false, true, true, false, false), "a PvP enemy");
        assertFalse(CombatRules.validTarget(false, true, false, true, false, true, true, false, false), "an allied player even when enmity is claimed");
    }

    private static final List<SpiritSpellDef> SPELLS = List.of(
            new SpiritSpellDef("small", "Small", 7f, 8, 30, 20, SpiritSpellDef.Effect.NONE, "FLAME_TRAIL", 0xFFFFFFFF, "fire"),
            new SpiritSpellDef("big", "Big", 12f, 16, 80, 12, SpiritSpellDef.Effect.IGNITE, "FLAMEFX_BURST", 0xFFFFFFFF, "fire"));

    @Test
    void thePickIsTheStrongestSpellThatIsReadyInRangeAndAffordableInBothPools() {
        long[] ready = {0, 0};
        double[] user = {10, 30};
        // everything fine: the big one (raw 12), it needs 8 spirit units at half
        CombatRules.Pick p = CombatRules.pick(SPELLS, ready, 100, 5, 50, 0.5, user, 100);
        assertTrue(p.cast());
        assertEquals(1, p.index());
        // too far for the big one: the small one
        assertEquals(0, CombatRules.pick(SPELLS, ready, 100, 15, 50, 0.5, user, 100).index());
        // the big one is on cooldown
        assertEquals(0, CombatRules.pick(SPELLS, new long[]{0, 200}, 100, 5, 50, 0.5, user, 100).index());
        // the spirit can pay 4 but not 8: the small one
        assertEquals(0, CombatRules.pick(SPELLS, ready, 100, 5, 5, 0.5, user, 100).index());
        // the owner can pay 10 but not 30
        assertEquals(0, CombatRules.pick(SPELLS, ready, 100, 5, 50, 0.5, user, 12).index());
    }

    @Test
    void aRefusalSaysWhichWallWasHit() {
        long[] ready = {0, 0};
        double[] user = {10, 30};
        assertEquals(CombatRules.Refusal.NO_SPELL_READY, CombatRules.pick(SPELLS, new long[]{500, 500}, 100, 5, 50, 0.5, user, 100).why());
        assertEquals(CombatRules.Refusal.OUT_OF_RANGE, CombatRules.pick(SPELLS, ready, 100, 40, 50, 0.5, user, 100).why());
        assertEquals(CombatRules.Refusal.NO_SPIRIT_MANA, CombatRules.pick(SPELLS, ready, 100, 5, 1, 0.5, user, 100).why());
        assertEquals(CombatRules.Refusal.NO_USER_MANA, CombatRules.pick(SPELLS, ready, 100, 5, 50, 0.5, user, 3).why());
        assertFalse(CombatRules.pick(SPELLS, ready, 100, 5, 1, 0.5, user, 100).cast());
    }

    @Test
    void whenTheOwnerPaysEverythingTheSpiritsPoolDoesNotMatterAndTheOtherWayRound() {
        long[] ready = {0, 0};
        // share 1: the spirit pays nothing, so an empty spirit pool still casts
        assertTrue(CombatRules.pick(SPELLS, ready, 100, 5, 0, 1.0, new double[]{10, 30}, 100).cast());
        // share 0: the owner pays nothing, so an empty owner still lets the spirit cast
        assertTrue(CombatRules.pick(SPELLS, ready, 100, 5, 50, 0.0, new double[]{0, 0}, 0).cast());
    }

    @Test
    void thePowerFollowsTheBondAndThePoolsWithinBounds() {
        double low = CombatRules.powerMult(0, 0, 0), high = CombatRules.powerMult(100, 1, 1);
        assertTrue(low >= 0.6 && high <= 1.6 && low < 1.0 && high > 1.0);
        assertTrue(CombatRules.powerMult(80, 1, 1) > CombatRules.powerMult(20, 1, 1), "a closer bond casts harder");
        assertTrue(CombatRules.powerMult(50, 1, 1) > CombatRules.powerMult(50, 0.2, 0.2), "full pools cast harder than empty ones");
        assertTrue(CombatRules.powerMult(500, 9, 9) <= 1.6 && CombatRules.powerMult(-5, -9, Double.NaN) >= 0.6, "wild inputs stay inside the bounds");
        assertEquals(CombatRules.powerMult(50, 0.5, 0.5), CombatRules.powerMult(50, 0.25, 0.75), 1e-9);
    }

    @Test
    void theDamageIsCapped() {
        assertEquals(60f, CombatRules.clampDamage(500f, 60f));
        assertEquals(12f, CombatRules.clampDamage(12f, 60f));
        assertEquals(0f, CombatRules.clampDamage(-4f, 60f));
        assertEquals(0f, CombatRules.clampDamage(Float.NaN, 60f));
        assertEquals(0f, CombatRules.clampDamage(10f, -1f));
    }

    @Test
    void decisionsOfDifferentPlayersSpreadOverThePeriodAndEachHappensOnceAPeriod() {
        int period = 10, players = 200;
        int[] perTick = new int[period];
        for (int h = 0; h < players; h++) {
            int hits = 0;
            for (long t = 0; t < period * 5; t++) {
                if (CombatRules.decisionTick(t, h * 31, period)) { hits++; perTick[(int) (t % period)]++; }
            }
            assertEquals(5, hits, "once a period for player " + h);
        }
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < period; i++) if (perTick[i] > 0) used.add(i);
        assertTrue(used.size() >= 8, "the players do not all decide on one tick");
        assertTrue(CombatRules.decisionTick(7, 0, 0), "a period of zero is read as one, never a division by zero");
    }

    @Test
    void theFightWindow() {
        assertTrue(CombatRules.inFight(1000, 900, -99999, 200));
        assertTrue(CombatRules.inFight(1000, -99999, 850, 200));
        assertFalse(CombatRules.inFight(1000, 700, 600, 200));
    }
}
