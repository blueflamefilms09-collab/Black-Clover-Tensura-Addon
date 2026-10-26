package com.newuniverse.nusmp.entity.veldanava.skills;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Solomon's pure rules (docs/veldanava_skills.md): the ten-mode table, the costs and cooldowns, Mana Skin, the hazards, the chant and price hooks, the convergence and the 72 pillars' plan. */
class SolomonRulesTest {
    private static final JudgmentRules.Spec S = SolomonRules.SOLOMON;

    @Test
    void theSkillHasTenModesInTheOwnersOrder() {
        assertEquals(10, S.count());
        String[] ids = {"infinite_grimoire", "omnipotent_magic", "mana_skin", "elemental_sovereignty", "grand_spell_copy", "arcane_convergence", "realm_of_magic", "spell_chain", "mana_zone_erasure", "the_72_pillars"};
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < ids.length; i++) {
            assertEquals(ids[i], S.mode(i).id());
            assertEquals(i, S.mode(i).index());
            assertTrue(seen.add(S.mode(i).id()), "duplicate id " + ids[i]);
        }
    }

    @Test
    void fourTogglesFiveActivesAndAMasteryPress() {
        for (int i = 0; i < 4; i++) { assertTrue(S.mode(i).toggle()); assertEquals(0, S.mode(i).cost()); }
        for (int i = 4; i < 9; i++) { assertFalse(S.mode(i).toggle()); assertTrue(S.mode(i).masteredCost() < S.mode(i).cost(), "mastery makes " + S.mode(i).id() + " cheaper"); }
        assertEquals(JudgmentRules.Kind.HELD, S.mode(6).kind(), "Realm of Magic is held");
        assertTrue(S.mode(5).crouch() && S.mode(8).crouch(), "Arcane Convergence and Mana Zone Erasure are crouch + press");
        assertFalse(S.mode(4).crouch() || S.mode(6).crouch() || S.mode(7).crouch() || S.mode(9).crouch());
    }

    @Test
    void costsFollowTheOwnersTable() {
        int[][] want = {{4500, 3000}, {2000, 1200}, {2500, 1500}, {12000, 8500}, {15000, 10000}};
        for (int i = 0; i < 5; i++) {
            assertEquals(want[i][0], JudgmentRules.cost(S.mode(4 + i), false, 1.0), 1e-9);
            assertEquals(want[i][1], JudgmentRules.cost(S.mode(4 + i), true, 1.0), 1e-9);
        }
        assertEquals(80000, JudgmentRules.cost(S.mode(9), false, 1.0), 1e-9);
        assertEquals(80000, JudgmentRules.cost(S.mode(9), true, 1.0), 1e-9, "The 72 Pillars has one price");
    }

    @Test
    void solomonIsClaimableWithItsOwnCodeAndTable() {
        assertTrue(JudgmentRules.claimable("solomon"));
        assertEquals(4, SolomonRules.CODE);
        assertEquals(SolomonRules.CODE, JudgmentRules.code("solomon"));
        assertSame(S, JudgmentRules.spec("solomon"));
    }

    @Test
    void manaSkinPaysALethalBlowWithTenMagiculeAPoint() {
        SolomonRules.Skin full = SolomonRules.manaSkin(30f, 20f, 0f, 1000.0);
        assertEquals(0f, full.damage(), 1e-6);
        assertEquals(300.0, full.cost(), 1e-9);
        SolomonRules.Skin part = SolomonRules.manaSkin(30f, 20f, 0f, 100.0);
        assertEquals(20f, part.damage(), 1e-6, "ten points are paid, twenty hurt");
        assertEquals(100.0, part.cost(), 1e-9);
        SolomonRules.Skin notLethal = SolomonRules.manaSkin(5f, 20f, 0f, 1000.0);
        assertEquals(5f, notLethal.damage(), 1e-6);
        assertEquals(0.0, notLethal.cost(), 1e-9);
        assertEquals(30f, SolomonRules.manaSkin(30f, 20f, 0f, 0.0).damage(), 1e-6, "no Magicule, no shield");
        assertEquals(25f, SolomonRules.manaSkin(25f, 20f, 6f, 0.0).damage(), 1e-6);
        assertEquals(10f, SolomonRules.manaSkin(10f, 20f, 0f, 0.0).damage(), 1e-6);
        assertEquals(0f, SolomonRules.manaSkin(-3f, 20f, 0f, 100.0).damage(), 1e-6, "a negative blow is nothing");
        assertEquals(0.0, SolomonRules.manaSkin(25f, 20f, 6f, 1000.0).cost(), 1e-9, "the absorption counts as health: 25 does not kill 20 + 6");
    }

    @Test
    void elementalSovereigntyKnowsItsHazardsAndProjectiles() {
        for (String h : new String[]{"in_fire", "on_fire", "lava", "hot_floor", "freeze", "fireball"}) assertTrue(SolomonRules.hazard(h), h);
        assertTrue(SolomonRules.hazard("minecraft_poison"));
        assertFalse(SolomonRules.hazard("player_attack"));
        assertFalse(SolomonRules.hazard("fall"));
        assertFalse(SolomonRules.hazard(null));
        assertTrue(SolomonRules.elementalProjectile("fireball"));
        assertTrue(SolomonRules.elementalProjectile("lightning_bolt"));
        assertTrue(SolomonRules.elementalProjectile("spell_ball"));
        assertFalse(SolomonRules.elementalProjectile("arrow"));
        assertFalse(SolomonRules.elementalProjectile("ender_pearl"));
        assertFalse(SolomonRules.elementalProjectile("snowball"));
        assertFalse(SolomonRules.elementalProjectile(null));
        assertEquals(50.0, SolomonRules.absorbGain(5000.0), 1e-9);
        assertEquals(SolomonRules.ABSORB_CAP, SolomonRules.absorbGain(1e9), 1e-9, "the gain is capped");
        assertEquals(0.0, SolomonRules.absorbGain(-5.0), 1e-9);
    }

    @Test
    void theChantIsCutByInfiniteGrimoireAndOneTickInARealm() {
        assertEquals(40, SolomonRules.chantTicks(40, false, false, false));
        assertEquals(20, SolomonRules.chantTicks(40, true, false, false));
        assertEquals(14, SolomonRules.chantTicks(40, true, true, false));
        assertEquals(SolomonRules.CHANT_MIN_TICKS, SolomonRules.chantTicks(3, true, true, false), "never below the shortest chant");
        assertEquals(1, SolomonRules.chantTicks(40, false, false, true));
        assertEquals(1, SolomonRules.chantTicks(40, true, true, true));
        assertEquals(1, SolomonRules.chantTicks(1, true, false, false), "a chant that is already one tick stays one");
    }

    @Test
    void spellsAreFreeInARealmAndHalfPriceOnceMastered() {
        assertEquals(100.0, SolomonRules.spellCost(100.0, false, false), 1e-9);
        assertEquals(50.0, SolomonRules.spellCost(100.0, true, false), 1e-9);
        assertEquals(0.0, SolomonRules.spellCost(100.0, false, true), 1e-9);
        assertEquals(0.0, SolomonRules.spellCost(100.0, true, true), 1e-9);
    }

    @Test
    void theConvergenceIsPaidPerThingAndCappedByTheMaximum() {
        assertEquals(0.0, SolomonRules.convergenceGain(0, 0, 0, 10000.0), 1e-9);
        assertEquals(10 * SolomonRules.GAIN_PROJECTILE, SolomonRules.convergenceGain(10, 0, 0, 100000.0), 1e-9);
        assertEquals(SolomonRules.GAIN_CLOUD + SolomonRules.GAIN_FIELD, SolomonRules.convergenceGain(0, 1, 1, 100000.0), 1e-9);
        assertEquals(0.25 * 4000.0, SolomonRules.convergenceGain(64, 32, 2, 4000.0), 1e-9, "one press gives back at most a quarter of the maximum");
        assertEquals(0.0, SolomonRules.convergenceGain(-5, -1, -1, 100.0), 1e-9);
        assertEquals(0.0, SolomonRules.convergenceGain(5, 5, 5, -100.0), 1e-9);
    }

    @Test
    void theSeventyTwoPillarsAreSharedOutWithoutLosingOne() {
        for (int targets = 1; targets <= SolomonRules.PILLAR_TARGETS_MAX; targets++) {
            int sum = 0;
            for (int i = 0; i < targets; i++) sum += SolomonRules.pillarsOnTarget(i, targets);
            assertEquals(72, sum, targets + " targets");
            assertEquals(0, SolomonRules.pillarsOnTarget(targets, targets));
        }
        assertEquals(72, SolomonRules.pillarsOnTarget(0, 1));
        assertEquals(11, SolomonRules.pillarsOnTarget(0, 7), "72 = 7 * 10 + 2: the first two targets get one more");
        assertEquals(10, SolomonRules.pillarsOnTarget(2, 7));
        assertEquals(9, SolomonRules.pillarsOnTarget(7, 8));
        assertEquals(72, SolomonRules.pillarsOnTarget(0, 0), "no target count is read as one");
        assertEquals(0, SolomonRules.pillarsOnTarget(-1, 3));
        assertEquals(9, SolomonRules.pillarsOnTarget(0, 99), "more targets than the cap are read as the cap");
        assertEquals(0, SolomonRules.pillarsOnTarget(8, 99));
    }

    @Test
    void pillarsFallEvenlyOverTheSpanAndTheDamageIsCappedPerCreature() {
        assertEquals(0, SolomonRules.pillarSpawnTick(0, 72));
        int last = SolomonRules.pillarSpawnTick(71, 72);
        assertTrue(last < SolomonRules.PILLAR_SPAN_TICKS && last >= SolomonRules.PILLAR_SPAN_TICKS - 3, "the last pillar falls at the end of the span, was " + last);
        int prev = -1;
        for (int k = 0; k < 72; k++) { int t = SolomonRules.pillarSpawnTick(k, 72); assertTrue(t >= prev, "ticks never go back"); prev = t; }
        assertEquals(0, SolomonRules.pillarSpawnTick(5, 0));
        assertEquals(SolomonRules.PILLAR_DAMAGE, SolomonRules.pillarDamage(0), 1e-9);
        assertEquals(SolomonRules.PILLAR_DAMAGE, SolomonRules.pillarDamage(SolomonRules.PILLAR_MAX_HITS - 1), 1e-9);
        assertEquals(0.0, SolomonRules.pillarDamage(SolomonRules.PILLAR_MAX_HITS), 1e-9, "after ten hits a creature is left alone");
        assertTrue(SolomonRules.PILLAR_MAX_HITS * SolomonRules.PILLAR_DAMAGE <= 700.0, "the worst blow on one creature is bounded");
    }

    @Test
    void theSeedFixesTheElementsAndTheLandingPointsOnBothSides() {
        long seed = SolomonRules.packSeed(0xABCDEFL, 36);
        assertEquals(36, SolomonRules.pillarsOfSeed(seed));
        assertEquals(72, SolomonRules.pillarsOfSeed(SolomonRules.packSeed(123456789L, 72)));
        for (int k = 0; k < 72; k++) {
            int e = SolomonRules.pillarElement(seed, k);
            assertTrue(e >= 0 && e < SolomonRules.PILLAR_ELEMENTS);
            assertEquals(e, SolomonRules.pillarElement(seed, k), "pure");
            double[] a = SolomonRules.pillarOffset(seed, k, SolomonRules.PILLAR_SPREAD);
            double[] b = SolomonRules.pillarOffset(seed, k, SolomonRules.PILLAR_SPREAD);
            assertArrayEquals(a, b, 0.0);
            assertTrue(Math.hypot(a[0], a[1]) <= SolomonRules.PILLAR_SPREAD + 1e-9, "inside the spread");
            if (k % 4 == 0) assertArrayEquals(new double[]{0.0, 0.0}, a, 0.0, "every fourth pillar lands on the target");
        }
        Set<Integer> elements = new HashSet<>();
        for (int k = 0; k < 8; k++) elements.add(SolomonRules.pillarElement(seed, k));
        assertEquals(8, elements.size(), "eight pillars in a row show all eight elements");
        assertFalse(java.util.Arrays.equals(SolomonRules.pillarOffset(seed, 1, 6.0), SolomonRules.pillarOffset(SolomonRules.packSeed(0x123L, 36), 1, 6.0)), "two casts differ");
        assertArrayEquals(new double[]{0.0, 0.0}, SolomonRules.pillarOffset(seed, 1, 0.0), 1e-12, "no spread, no scatter");
    }

    @Test
    void everyWorstCaseIsBounded() {
        assertTrue(SolomonRules.TERRAIN_MAX_BLOCKS <= 1000 && SolomonRules.TERRAIN_PER_PILLAR <= 20);
        assertTrue(SolomonRules.LOG_SIZE <= 128 && SolomonRules.OWN_LOG <= 5 && SolomonRules.CHAIN_COUNT <= 5);
        assertTrue(SolomonRules.REALM_MAX_ENEMIES <= 32 && SolomonRules.REALM_MAX_TICKS <= 1200);
        assertTrue(SolomonRules.ERASURE_MAX_PROJECTILES <= 128 && SolomonRules.ERASURE_MAX_CASTERS <= 32);
        assertTrue(SolomonRules.CONVERGENCE_MAX_PROJECTILES <= 96 && SolomonRules.CONVERGENCE_MAX_CLOUDS <= 48);
        assertTrue(SolomonRules.PILLAR_TARGETS_MAX <= 8);
    }
}
