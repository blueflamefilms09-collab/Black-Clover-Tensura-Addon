package com.newuniverse.nusmp.entity.veldanava.skills;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Hercules' pure rules (docs/veldanava_skills.md): the ten-mode table, the costs and cooldowns, the body, the strength, the momentum, the vigor, the tremor, Invictus, the throw, the roar and the Strike. */
class HerculesRulesTest {
    private static final JudgmentRules.Spec S = HerculesRules.HERCULES;
    /** The longest a field may stand (JudgmentFields.MAX_TICKS, 1200 ticks); read here as a number so this test needs no Minecraft class. */
    private static final int MAX_FIELD_TICKS = 1200;

    @Test
    void theSkillHasTenModesInTheOwnersOrder() {
        assertEquals(10, S.count());
        String[] ids = {"adamantine_body", "god_of_strength", "unstoppable_momentum", "colossal_vigor", "earth_shattering_tremor", "invictus", "titans_grip", "devastating_throw", "battle_roar", "world_breaking_strike"};
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
        assertTrue(S.mode(5).crouch() && S.mode(7).crouch(), "Invictus and Devastating Throw are crouch + press");
        assertFalse(S.mode(4).crouch() || S.mode(6).crouch() || S.mode(8).crouch() || S.mode(9).crouch());
        assertEquals(JudgmentRules.Kind.PRESS, S.mode(9).kind());
        assertEquals(S.mode(9).cost(), S.mode(9).masteredCost(), "World-Breaking Strike has one price");
        assertEquals(180, S.mode(9).cooldownSeconds());
    }

    @Test
    void costsFollowTheOwnersTable() {
        int[][] want = {{3000, 2000}, {8000, 5500}, {2500, 1500}, {4000, 2500}, {5000, 3200}};
        for (int i = 0; i < 5; i++) {
            assertEquals(want[i][0], JudgmentRules.cost(S.mode(4 + i), false, 1.0), 1e-9);
            assertEquals(want[i][1], JudgmentRules.cost(S.mode(4 + i), true, 1.0), 1e-9);
        }
        assertEquals(15000, JudgmentRules.cost(S.mode(9), false, 1.0), 1e-9);
    }

    @Test
    void herculesIsClaimableWithItsOwnCodeAndTable() {
        assertTrue(JudgmentRules.claimable("hercules"));
        assertEquals(5, HerculesRules.CODE);
        assertEquals(HerculesRules.CODE, JudgmentRules.code("hercules"));
        assertSame(S, JudgmentRules.spec("hercules"));
    }

    @Test
    void adamantineBodyTakesATenthOfAPhysicalAndHalfOfAMagicalBlow() {
        assertEquals(10f, HerculesRules.adamantine(100f, false), 1e-4f);
        assertEquals(50f, HerculesRules.adamantine(100f, true), 1e-4f);
        assertEquals(0f, HerculesRules.adamantine(-5f, false));
        assertEquals(0f, HerculesRules.adamantine(0f, true));
    }

    @Test
    void fireWordsAreNamedAndNothingElseIsFire() {
        for (String w : new String[]{"in_fire", "on_fire", "lava", "hot_floor", "fireball", "unattributed_fireball"}) assertTrue(HerculesRules.fire(w), w);
        for (String w : new String[]{"mob_attack", "fall", "arrow", "magic", "player_attack", "", null}) assertFalse(HerculesRules.fire(w), String.valueOf(w));
    }

    @Test
    void godOfStrengthMultipliesAndBreaksAnythingBreakable() {
        assertEquals(30f, HerculesRules.strength(10f, false), 1e-4f);
        assertEquals(45f, HerculesRules.strength(10f, true), 1e-4f);
        assertEquals(0f, HerculesRules.strength(-1f, true));
        assertTrue(HerculesRules.instantBreak(0f));
        assertTrue(HerculesRules.instantBreak(50f), "obsidian breaks");
        assertFalse(HerculesRules.instantBreak(-1f), "bedrock never breaks");
        assertFalse(HerculesRules.instantBreak(51f));
        for (float h : new float[]{0f, 0.5f, 3f, 50f}) assertTrue(HerculesRules.instantSpeed(h) > h * 100f, "the speed beats the block at " + h);
        assertEquals(35f, HerculesRules.shockDamage(100f), 1e-4f);
        assertEquals(0f, HerculesRules.shockDamage(0f));
    }

    @Test
    void momentumCountsSecondsOfMovementAndGrowsInThreeStages() {
        assertEquals(0, HerculesRules.momentumSeconds(7, 0.5), "standing still resets the run");
        assertEquals(8, HerculesRules.momentumSeconds(7, 6.0));
        assertEquals(HerculesRules.SECONDS_MAX, HerculesRules.momentumSeconds(HerculesRules.SECONDS_MAX, 6.0), "the count stops");
        assertEquals(1, HerculesRules.momentumSeconds(-4, 6.0), "a bad count starts again");
        assertEquals(0, HerculesRules.momentumStage(0));
        assertEquals(0, HerculesRules.momentumStage(2));
        assertEquals(1, HerculesRules.momentumStage(3), "more than 3 seconds of movement starts the scaling");
        assertEquals(2, HerculesRules.momentumStage(6));
        assertEquals(3, HerculesRules.momentumStage(9));
        assertEquals(HerculesRules.STAGE_MAX, HerculesRules.momentumStage(500));
    }

    @Test
    void armourPierceReturnsAShareOfWhatTheArmourTookOffAndNoMore() {
        assertEquals(0f, HerculesRules.armourPierce(10f, 4f, 0, false));
        assertEquals(0f, HerculesRules.armourPierce(10f, 12f, 3, true), "no armour saving, nothing to take back");
        assertEquals(0.15f * 6f, HerculesRules.armourPierce(10f, 4f, 1, false), 1e-4f);
        assertTrue(HerculesRules.armourPierce(10f, 4f, 3, true) > HerculesRules.armourPierce(10f, 4f, 3, false), "mastery pierces more");
        assertTrue(HerculesRules.armourPierce(10f, 4f, 99, true) <= 6f, "never more than the armour took off");
    }

    @Test
    void vigorHealsWhenCalmAndStealsLifeInAFightWithinACap() {
        assertTrue(HerculesRules.calm(1000L, 100L, 200L));
        assertFalse(HerculesRules.calm(250L, 100L, 200L));
        assertFalse(HerculesRules.calm(299L, 0L, 200L));
        assertEquals(0.4f, HerculesRules.regen(20f, false), 1e-4f);
        assertEquals(0.8f, HerculesRules.regen(20f, true), 1e-4f);
        assertEquals(0f, HerculesRules.regen(-5f, true));
        assertEquals(3f, HerculesRules.lifesteal(20f, 400f, false), 1e-4f);
        assertEquals(400f * HerculesRules.LIFESTEAL_CAP, HerculesRules.lifesteal(1.0E6f, 400f, true), 1e-3f, "one blow heals at most a share of the maximum");
        assertEquals(0f, HerculesRules.lifesteal(0f, 400f, false));
    }

    @Test
    void theTremorFallsOffFromAllToFortyPercent() {
        assertEquals(1.0, HerculesRules.tremorFalloff(0.0, 14.0), 1e-9);
        assertEquals(0.4, HerculesRules.tremorFalloff(14.0, 14.0), 1e-9);
        assertEquals(0.4, HerculesRules.tremorFalloff(500.0, 14.0), 1e-9, "past the radius it never goes under the rim");
        assertEquals(0.7, HerculesRules.tremorFalloff(7.0, 14.0), 1e-9);
        assertEquals(0.0, HerculesRules.tremorFalloff(3.0, 0.0), 1e-9);
    }

    @Test
    void invictusKeepsOneHitPointAndGrowsTheBlowAsHealthFalls() {
        assertEquals(19f, HerculesRules.invictusFloor(500f, 20f, 0f), 1e-4f);
        assertEquals(5f, HerculesRules.invictusFloor(5f, 20f, 0f), 1e-4f);
        assertEquals(0f, HerculesRules.invictusFloor(500f, 1f, 0f), 1e-4f, "at one point nothing more is taken");
        assertEquals(14f, HerculesRules.invictusFloor(500f, 10f, 5f), 1e-4f, "absorption counts");
        assertEquals(0f, HerculesRules.invictusFloor(-3f, 10f, 0f));
        assertEquals(1.0, HerculesRules.invictusMultiplier(20f, 20f, false), 1e-9);
        assertEquals(2.0, HerculesRules.invictusMultiplier(10f, 20f, false), 1e-9);
        assertEquals(3.0, HerculesRules.invictusMultiplier(0f, 20f, false), 1e-9);
        assertEquals(4.0, HerculesRules.invictusMultiplier(0f, 20f, true), 1e-9);
        assertEquals(1.0, HerculesRules.invictusMultiplier(5f, 0f, true), 1e-9, "no maximum, no bonus");
        assertEquals(10, HerculesRules.INVICTUS_TICKS / 20, "Invictus lasts ten seconds");
    }

    @Test
    void theThrowFliesAShortBoundedPath() {
        assertEquals(0.0, HerculesRules.throwPath(-4), 1e-9);
        assertEquals(HerculesRules.THROW_SPEED * 5, HerculesRules.throwPath(5), 1e-9);
        assertEquals(HerculesRules.THROW_SPEED * HerculesRules.THROW_MAX_TICKS, HerculesRules.throwPath(100000), 1e-9);
        assertTrue(HerculesRules.throwPath(HerculesRules.THROW_MAX_TICKS) < 60.0, "the path is a short flight, not a chunk-loading one");
        assertTrue(HerculesRules.THROW_MAX_TICKS + 2 * HerculesRules.THROW_PULSE_TICKS < MAX_FIELD_TICKS);
    }

    @Test
    void theStrikeBreaksArmourForHalfAsLongOnPlayersAndBossesAndHitsTheFirstHardest() {
        assertEquals(HerculesRules.ARMOR_BREAK_TICKS, HerculesRules.armorBreakTicks(false, false));
        assertEquals(HerculesRules.ARMOR_BREAK_TICKS / 2, HerculesRules.armorBreakTicks(true, false));
        assertEquals(HerculesRules.ARMOR_BREAK_TICKS / 2, HerculesRules.armorBreakTicks(false, true));
        assertEquals(HerculesRules.STRIKE_DAMAGE, HerculesRules.strikeDamage(0), 1e-9);
        assertEquals(HerculesRules.STRIKE_DAMAGE * 0.5, HerculesRules.strikeDamage(3), 1e-9);
        assertEquals(HerculesRules.STRIKE_DAMAGE, HerculesRules.strikeDamage(-1), 1e-9);
    }

    @Test
    void everyPerPressCapIsSmall() {
        assertTrue(HerculesRules.TREMOR_MAX_CREATURES <= 24 && HerculesRules.TREMOR_MAX_BLOCKS <= 240);
        assertTrue(HerculesRules.THROW_MAX_HITS <= 8 && HerculesRules.THROW_BLAST_MAX <= 12 && HerculesRules.THROW_CRATER_BLOCKS <= 60 && HerculesRules.CHUNK_BLOCKS <= 12);
        assertTrue(HerculesRules.ROAR_MAX <= 32 && HerculesRules.ROAR_CLEANSE_MAX <= 24 && HerculesRules.STRIKE_MAX <= 8 && HerculesRules.SHOCK_MAX <= 6);
        assertTrue(HerculesRules.INVICTUS_TICKS <= MAX_FIELD_TICKS);
    }
}
