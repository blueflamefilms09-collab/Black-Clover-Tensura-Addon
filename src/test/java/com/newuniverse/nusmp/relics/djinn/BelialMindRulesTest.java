package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of Belial's mind magic (owner notes): Belior Zakera, the five senses of Belior Zauto, the dead weight of Belior Goldoreza. */
class BelialMindRulesTest {
    private static final BelialSeverRules.Kind MOB = BelialSeverRules.Kind.MOB, PLAYER = BelialSeverRules.Kind.PLAYER, BOSS = BelialSeverRules.Kind.BOSS;

    @Test
    void theFateFollowsTheTargetAndADefenceWinsOverEverything() {
        assertEquals(BelialMindRules.Fate.MIND, BelialMindRules.fate(MOB, false));
        assertEquals(BelialMindRules.Fate.SENSES, BelialMindRules.fate(PLAYER, false));
        assertEquals(BelialMindRules.Fate.BOSS, BelialMindRules.fate(BOSS, false));
        for (BelialSeverRules.Kind k : BelialSeverRules.Kind.values()) assertEquals(BelialMindRules.Fate.BLOCKED, BelialMindRules.fate(k, true), k + ": a Borg or a raised shield blocks it");
    }

    @Test
    void aBossIsNeverMindControlledAndOnlyTakesTheSmallStrain() {
        assertNotEquals(BelialMindRules.Fate.MIND, BelialMindRules.fate(BOSS, false));
        assertTrue(BelialMindRules.strain(BelialMindRules.Fate.BOSS, 100.0) < BelialMindRules.strain(BelialMindRules.Fate.BLOCKED, 100.0), "a boss takes less strain than a defended mage");
        assertEquals(0.0, BelialMindRules.strain(BelialMindRules.Fate.MIND, 100.0), 1.0E-9);
        assertEquals(0.0, BelialMindRules.strain(BelialMindRules.Fate.SENSES, 100.0), 1.0E-9);
        assertEquals(0.0, BelialMindRules.strain(BelialMindRules.Fate.BLOCKED, -5.0), 1.0E-9, "no negative strain");
        assertTrue(BelialMindRules.strain(BelialMindRules.Fate.BLOCKED, 10.0) > 0.0, "a blocked target still takes the strain");
    }

    @Test
    void theReachIsTenInAHalfEquipAndTwentyFourInAFullOne() {
        assertEquals(10.0, BelialMindRules.zakeraRange(false), 1.0E-9);
        assertEquals(24.0, BelialMindRules.zakeraRange(true), 1.0E-9);
        assertTrue(BelialMindRules.zakeraRange(true) <= 32.0, "inside the relics' foe-scan ceiling");
    }

    @Test
    void aMobForgetsForThreeToSixSecondsAndANauseaStaysShort() {
        for (boolean full : new boolean[]{false, true})
            for (boolean dark : new boolean[]{false, true}) {
                int f = BelialMindRules.forgetTicks(full, dark), n = BelialMindRules.nauseaTicks(full, dark);
                assertTrue(f >= 60 && f <= 120, "forget " + f);
                assertTrue(n >= 80 && n <= 120, "nausea " + n);
                assertTrue(BelialMindRules.RETARGET_BLOCK <= f, "the re-target block never outlasts the state");
            }
        assertTrue(BelialMindRules.forgetTicks(true, true) > BelialMindRules.forgetTicks(false, false));
    }

    @Test
    void theTablesAreBoundedAndARenewalNeverNeedsRoom() {
        assertTrue(BelialMindRules.accept(false, 0, 0));
        assertFalse(BelialMindRules.accept(false, BelialMindRules.MAX_PER_CASTER, 0));
        assertFalse(BelialMindRules.accept(false, 0, BelialMindRules.MAX_MINDS));
        assertTrue(BelialMindRules.accept(true, BelialMindRules.MAX_PER_CASTER, BelialMindRules.MAX_MINDS), "a mob that already forgot only has its time renewed");
        assertTrue(BelialMindRules.MAX_TARGETS <= 6);
        assertTrue(BelialMindRules.shouldRedirect(0.0) && !BelialMindRules.shouldRedirect(0.99));
    }

    @Test
    void fiveSensesFiveVanillaEffectsAndNoFoodEffect() {
        assertEquals(5, BelialMindRules.SENSES.size());
        Set<String> names = new HashSet<>(), ids = new HashSet<>();
        for (BelialMindRules.Sense s : BelialMindRules.SENSES) {
            assertTrue(names.add(s.name()) && ids.add(s.effectId()), s.name());
            assertTrue(s.effectId().startsWith("minecraft:"), s.effectId());
            assertFalse(s.effectId().contains("hung"), "the mod's rule: no food effects");
            assertTrue(s.amp() >= 0 && s.amp() <= 1);
        }
        assertEquals(Set.of("sight", "hearing", "touch", "taste", "smell"), names);
    }

    @Test
    void theSensesAreTimeLimitedAndABossLosesNone() {
        assertEquals(0, BelialMindRules.senseTicks(BOSS, false));
        assertEquals(0, BelialMindRules.senseTicks(BOSS, true));
        for (BelialSeverRules.Kind k : new BelialSeverRules.Kind[]{MOB, PLAYER})
            for (boolean dark : new boolean[]{false, true}) {
                int t = BelialMindRules.senseTicks(k, dark);
                assertTrue(t > 0 && t <= BelialMindRules.SENSE_MAX, k + " " + t);
            }
        assertTrue(BelialMindRules.senseTicks(PLAYER, false) < BelialMindRules.senseTicks(MOB, false), "a player is blind for less time than a mob");
        assertTrue(BelialMindRules.SENSE_MAX <= 200, "never more than ten seconds");
    }

    @Test
    void theDeadWeightIsTwentySecondsAtMostAndNeverOnABoss() {
        assertEquals(0, BelialMindRules.deadWeightTicks(BOSS, true));
        assertEquals(0, BelialMindRules.deadWeightTicks(BOSS, false));
        for (BelialSeverRules.Kind k : new BelialSeverRules.Kind[]{MOB, PLAYER})
            for (boolean full : new boolean[]{false, true}) {
                int t = BelialMindRules.deadWeightTicks(k, full);
                assertTrue(t > 0 && t <= 400, k + " " + t + ": at most 20 s");
            }
        assertEquals(400, BelialMindRules.deadWeightTicks(MOB, true));
        assertTrue(BelialMindRules.deadWeightTicks(PLAYER, true) < 400 / 2 + 1, "a player is numb for about half as long");
        assertTrue(BelialMindRules.DEAD_MAX <= 128);
    }
}
