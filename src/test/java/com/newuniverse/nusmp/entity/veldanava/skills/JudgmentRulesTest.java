package com.newuniverse.nusmp.entity.veldanava.skills;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Sandalphon's pure rules (docs/veldanava_skills.md): the ten-mode table, the five cards, the costs, the damage formulas and the geometry of the aims. */
class JudgmentRulesTest {
    private static final JudgmentRules.Spec S = JudgmentRules.SANDALPHON;

    @Test
    void theSkillHasTenModesInTheOwnersOrder() {
        assertEquals(10, S.count());
        String[] ids = {"executioners_authority", "absolute_severance", "inevitable_demise", "judgment_gaze", "judgment_bullet", "condemnation", "supreme_edict", "guillotine_cleave", "verdict_of_ruin", "final_judgment"};
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < ids.length; i++) {
            assertEquals(ids[i], S.mode(i).id());
            assertEquals(i, S.mode(i).index());
            assertTrue(seen.add(S.mode(i).id()), "duplicate id " + ids[i]);
        }
        assertEquals(S.mode(0), S.mode(10), "modes wrap around");
        assertEquals(S.mode(9), S.mode(-1));
    }

    @Test
    void fourTogglesFiveActivesOneHeldMastery() {
        for (int i = 0; i < 4; i++) { assertTrue(S.mode(i).toggle()); assertEquals(0, S.mode(i).cost()); }
        for (int i = 4; i < 9; i++) { assertFalse(S.mode(i).toggle()); assertTrue(S.mode(i).cost() > 0); assertTrue(S.mode(i).masteredCost() < S.mode(i).cost(), "mastery makes " + S.mode(i).id() + " cheaper"); }
        assertEquals(JudgmentRules.Kind.HELD, S.mode(9).kind());
        assertEquals(45000, S.mode(9).cost());
        assertEquals(120, S.mode(9).cooldownSeconds());
        assertTrue(S.mode(5).crouch() && S.mode(7).crouch(), "Condemnation and Guillotine Cleave are crouch + press");
        assertFalse(S.mode(4).crouch() || S.mode(6).crouch() || S.mode(8).crouch());
    }

    @Test
    void costsFollowTheOwnersTableAndTheServerScale() {
        int[][] want = {{2500, 1800}, {4200, 3100}, {5500, 4000}, {3800, 2500}, {8000, 5500}};
        for (int i = 0; i < 5; i++) {
            assertEquals(want[i][0], JudgmentRules.cost(S.mode(4 + i), false, 1.0), 1e-9);
            assertEquals(want[i][1], JudgmentRules.cost(S.mode(4 + i), true, 1.0), 1e-9);
        }
        assertEquals(1250, JudgmentRules.cost(S.mode(4), false, 0.5), 1e-9);
        assertEquals(0, JudgmentRules.cost(S.mode(4), false, 0), 1e-9);
        assertEquals(0, JudgmentRules.cost(S.mode(4), false, -2), 1e-9, "a negative scale never pays the player");
    }

    @Test
    void onlySandalphonCanBeClaimedInThisRelease() {
        assertEquals(5, JudgmentRules.CARDS.size());
        assertEquals("sandalphon", JudgmentRules.CARDS.get(0).id());
        assertTrue(JudgmentRules.claimable("sandalphon"));
        for (String id : new String[]{"israfil", "jibril", "solomon", "hercules"}) {
            assertNotNull(JudgmentRules.card(id), id + " is on the screen");
            assertFalse(JudgmentRules.claimable(id), id + " is sealed");
        }
        assertFalse(JudgmentRules.claimable(null));
        assertFalse(JudgmentRules.claimable(""));
        assertFalse(JudgmentRules.claimable("SANDALPHON"), "ids are exact");
        assertFalse(JudgmentRules.claimable("sandalphon "));
        assertNull(JudgmentRules.card("anything_else"));
    }

    @Test
    void inevitableDemiseGrowsAsHealthFalls() {
        assertEquals(1.0, JudgmentRules.demiseMultiplier(1.0), 1e-9);
        assertEquals(1.5, JudgmentRules.demiseMultiplier(0.5), 1e-9);
        assertEquals(2.0, JudgmentRules.demiseMultiplier(0.0), 1e-9);
        assertEquals(2.0, JudgmentRules.demiseMultiplier(-10), 1e-9);
        assertEquals(1.0, JudgmentRules.demiseMultiplier(10), 1e-9);
    }

    @Test
    void debtMakesMagicHurtMoreButNotWithoutLimit() {
        assertEquals(1.0, JudgmentRules.magicVulnerability(0), 1e-9);
        assertEquals(1.1, JudgmentRules.magicVulnerability(10), 1e-9);
        assertEquals(1.5, JudgmentRules.magicVulnerability(50), 1e-9);
        assertEquals(1.5, JudgmentRules.magicVulnerability(100000), 1e-9);
        assertEquals(1.0, JudgmentRules.magicVulnerability(-5), 1e-9);
    }

    @Test
    void armourPierceIsClampedBothWays() {
        assertEquals(0, JudgmentRules.armourPierce(0), 1e-9);
        assertEquals(7.0, JudgmentRules.armourPierce(20), 1e-9);
        assertEquals(JudgmentRules.ARMOUR_PIERCE_MAX, JudgmentRules.armourPierce(1000), 1e-9);
        assertEquals(0, JudgmentRules.armourPierce(-8), 1e-9);
    }

    @Test
    void theDetonationHasAFloorAndACap() {
        assertEquals(1000, JudgmentRules.detonation(10, 20, 5, 200, 50000), 1e-9);
        assertEquals(200, JudgmentRules.detonation(1, 20, 5, 200, 50000), 1e-9, "the floor");
        assertEquals(200, JudgmentRules.detonation(0, 20, 5, 200, 50000), 1e-9, "no debt still does the floor");
        assertEquals(50000, JudgmentRules.detonation(100000, 20, 5, 200, 50000), 1e-9, "the cap");
        assertEquals(150, JudgmentRules.detonation(0, 20, 5, 200, 150), 1e-9, "the cap beats the floor");
    }

    @Test
    void theExecuteThresholdIsTwentyPercent() {
        assertTrue(JudgmentRules.atOrBelow(20, 100, JudgmentRules.EXECUTE_BELOW));
        assertFalse(JudgmentRules.atOrBelow(20.1, 100, JudgmentRules.EXECUTE_BELOW));
        assertTrue(JudgmentRules.atOrBelow(0, 100, JudgmentRules.EXECUTE_BELOW));
        assertFalse(JudgmentRules.atOrBelow(5, 0, JudgmentRules.EXECUTE_BELOW), "no maximum, no execution");
        assertTrue(JudgmentRules.DEMISE_BELOW < JudgmentRules.EXECUTE_BELOW);
    }

    @Test
    void theGazeConeIsNarrow() {
        assertTrue(JudgmentRules.looksAt(0, 0, 1, 0, 0, 20, JudgmentRules.GAZE_COSINE));
        assertTrue(JudgmentRules.looksAt(0, 0, 2, 0.5, 0, 20, JudgmentRules.GAZE_COSINE));
        assertFalse(JudgmentRules.looksAt(0, 0, 1, 5, 0, 5, JudgmentRules.GAZE_COSINE));
        assertFalse(JudgmentRules.looksAt(0, 0, 1, 0, 0, -5, JudgmentRules.GAZE_COSINE), "behind the watcher");
        assertFalse(JudgmentRules.looksAt(0, 0, 0, 0, 0, 5, JudgmentRules.GAZE_COSINE), "no view direction");
        assertFalse(JudgmentRules.looksAt(0, 0, 1, 0, 0, 0, JudgmentRules.GAZE_COSINE), "the same spot");
    }

    @Test
    void theCleaveConeIsInFrontAndInReach() {
        assertTrue(JudgmentRules.inCone(0, 1, 0, 3, JudgmentRules.CLEAVE_REACH, JudgmentRules.CLEAVE_HALF_ANGLE_DEG));
        assertTrue(JudgmentRules.inCone(0, 1, 2, 2, JudgmentRules.CLEAVE_REACH, JudgmentRules.CLEAVE_HALF_ANGLE_DEG));
        assertFalse(JudgmentRules.inCone(0, 1, 3, 0, JudgmentRules.CLEAVE_REACH, JudgmentRules.CLEAVE_HALF_ANGLE_DEG), "to the side");
        assertFalse(JudgmentRules.inCone(0, 1, 0, -3, JudgmentRules.CLEAVE_REACH, JudgmentRules.CLEAVE_HALF_ANGLE_DEG), "behind");
        assertFalse(JudgmentRules.inCone(0, 1, 0, 6, JudgmentRules.CLEAVE_REACH, JudgmentRules.CLEAVE_HALF_ANGLE_DEG), "out of reach");
        assertTrue(JudgmentRules.inCone(0, 1, 0, 0, JudgmentRules.CLEAVE_REACH, JudgmentRules.CLEAVE_HALF_ANGLE_DEG), "on the caster");
        assertFalse(JudgmentRules.inCone(0, 0, 1, 1, JudgmentRules.CLEAVE_REACH, JudgmentRules.CLEAVE_HALF_ANGLE_DEG), "no facing");
    }

    @Test
    void theToggleBitsAreFourAndFlipBack() {
        assertEquals(1, JudgmentRules.bit(0));
        assertEquals(8, JudgmentRules.bit(3));
        assertEquals(0, JudgmentRules.bit(4));
        assertEquals(0, JudgmentRules.bit(-1));
        int m = JudgmentRules.flip(0, 2);
        assertTrue(JudgmentRules.on(m, 2));
        assertFalse(JudgmentRules.on(m, 1));
        assertEquals(0, JudgmentRules.flip(m, 2));
        assertEquals(m, JudgmentRules.flip(m, 7), "flipping an active changes nothing");
    }

    @Test
    void everyBoundIsPositiveAndSmall() {
        int[] bounds = {JudgmentRules.MAX_BULLET_HITS, JudgmentRules.MAX_EDICT_TARGETS, JudgmentRules.MAX_CLEAVE_TARGETS, JudgmentRules.MAX_GAZE_TARGETS, JudgmentRules.MAX_PRESENCE_TARGETS, JudgmentRules.MAX_FIELD_TARGETS, JudgmentRules.STRIP_MAX};
        for (int b : bounds) { assertTrue(b > 0); assertTrue(b <= 64, "a per-press cap of " + b + " is not small"); }
        assertTrue(JudgmentRules.VERDICT_TICKS <= 400, "a field lasts at most twenty seconds");
        assertTrue(JudgmentRules.FIELD_PULSE_TICKS >= 5, "a field pulses at most four times a second");
        assertTrue(JudgmentRules.FINAL_CHANNEL_TICKS <= JudgmentRules.FINAL_MAX_HELD);
        assertTrue(JudgmentRules.ACTIVE_GAP_TICKS >= 4, "actives cannot be spammed every tick");
        assertTrue(JudgmentRules.BULLET_RANGE <= 128 && JudgmentRules.FINAL_RANGE <= 128 && JudgmentRules.EDICT_RADIUS <= 64);
    }
}
