package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.cast.AvatarCastMath;
import com.newuniverse.nusmp.avatar.cast.AvatarCastPlanner;
import com.newuniverse.nusmp.avatar.cast.AvatarManaPool;
import com.newuniverse.nusmp.avatar.cast.AvatarSkillList;
import com.newuniverse.nusmp.avatar.chatter.Rng;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What an Avatar may cast, what it costs, how fast it recasts, which page it prefers. */
class AvatarCastTest {
    // ---------------------------------------------------------------- whitelist
    @Test
    void plainAttackPagesAreAllowed() {
        for (String id : new String[]{"exploding_fireball", "sol_linea", "thunder_lance", "iron_strike", "boulder_shot", "frost_ball", "slash_wave",
                "hex_shards", "glass_lance", "crystal_shards", "rapid_barrage", "mercury_rain", "waterball", "sekke_magnum_cannonball", "bubble_barrage",
                "curse_strike", "barrier_strike", "key_strike", "eye_strike"})
            assertEquals(AvatarSkillList.Kind.ATTACK, AvatarSkillList.kind(id, 8), id);
    }

    @Test
    void worldAlteringOwnerTargetingAndItemPagesAreNeverCast() {
        for (String id : new String[]{"summon_grimoire", "store_weapon", "draw_weapon", "mana_zone", "devil_union", "spirit_dive", "reversal", "black_hole",
                "chrono_stasis", "stolen_time", "random_seal", "domination", "corpse_reanimate", "key_armory", "gate_of_keys", "banquet_table", "healing_ray",
                "healing_mist", "bronze_armour", "barrier_step", "door_step", "clones_of_the_beautiful_me", "real_double", "voodoo_kingdom", "pawn_march",
                "bleeding_thorn_wall", "iron_wall", "tar_pit", "elemental_dice", "life_exchange", "eye_swap", "spirit_beast_summon", "titan_form"})
            assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind(id, 8), id);
    }

    @Test
    void aSealOrHealWordKillsEvenAStrikeName() {
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("seal_strike", 8));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("heal_strike", 8));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("summon_strike", 8));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("time_strike", 8));
    }

    @Test
    void expensiveSignaturePagesAreNeverCast() {
        assertEquals(AvatarSkillList.Kind.ATTACK, AvatarSkillList.kind("exploding_fireball", AvatarSkillList.MAX_COST_PERCENT));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("exploding_fireball", AvatarSkillList.MAX_COST_PERCENT + 0.1));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("leo_rugiens", 35));
    }

    @Test
    void utilityIsAShortExplicitList() {
        assertEquals(AvatarSkillList.Kind.UTILITY, AvatarSkillList.kind("acceleration", 8));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("acceleration", 50), "still capped by cost");
    }

    @Test
    void junkIdsAreRefused() {
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind(null, 1));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("", 1));
        assertEquals(AvatarSkillList.Kind.NONE, AvatarSkillList.kind("unheard_of", 1));
        assertFalse(AvatarSkillList.allowedPage("exploding_fireball", 50));
        assertTrue(AvatarSkillList.allowedPage("EXPLODING_FIREBALL", 8), "ids are matched case-insensitively");
    }

    @Test
    void tensuraAllowlistIsAttackMagicOnly() {
        assertTrue(AvatarSkillList.allowedTensuraSkill("fire_ball"));
        assertTrue(AvatarSkillList.allowedTensuraSkill("thunder_lance"));
        assertFalse(AvatarSkillList.allowedTensuraSkill("fire_wall"));
        assertFalse(AvatarSkillList.allowedTensuraSkill("summon_basilisk"));
        assertFalse(AvatarSkillList.allowedTensuraSkill("barrier"));
        assertFalse(AvatarSkillList.allowedTensuraSkill("heal"));
        assertFalse(AvatarSkillList.allowedTensuraSkill(null));
        assertTrue(AvatarSkillList.tensuraKeywords().length >= 8);
    }

    // ---------------------------------------------------------------- mana pool
    @Test
    void poolHasAFloorThatGrowsWithIntelligence() {
        assertEquals(AvatarManaPool.FLOOR, AvatarManaPool.max(0, 10), 1e-9);
        assertEquals(AvatarManaPool.FLOOR, AvatarManaPool.max(0, 3), 1e-9, "low intelligence does not shrink it");
        assertEquals(AvatarManaPool.FLOOR + 8 * AvatarManaPool.PER_INT, AvatarManaPool.max(0, 18), 1e-9);
        assertEquals(50_000, AvatarManaPool.max(50_000, 10), 1e-9, "Tensura's own maximum wins when larger");
    }

    @Test
    void regenerationFillsTheWholePoolInTwoMinutesAndNeverOverfills() {
        double max = 1200;
        assertEquals(max, AvatarManaPool.regen(0, max, 2400), 1e-6);
        assertEquals(max, AvatarManaPool.regen(0, max, 99999), 1e-9);
        assertEquals(0.5, AvatarManaPool.regenPerTick(max), 1e-9);
        assertEquals(0.5, AvatarManaPool.regen(-100, max, 1) - 0, 1e-9, "a negative stored value counts as empty");
    }

    @Test
    void payingNeverGoesNegativeAndAFrenzyPaysNothing() {
        assertTrue(AvatarManaPool.canPay(100, 100));
        assertFalse(AvatarManaPool.canPay(99.9, 100));
        assertTrue(AvatarManaPool.canPay(0, 0), "a free cast is always affordable");
        assertEquals(0, AvatarManaPool.pay(50, 100), 1e-9);
        assertEquals(40, AvatarManaPool.pay(100, 60), 1e-9);
        assertEquals(100, AvatarManaPool.pay(100, 0), 1e-9);
        assertEquals(100, AvatarManaPool.pay(100, -5), 1e-9, "a negative cost never refunds");
    }

    // ---------------------------------------------------------------- cooldowns
    @Test
    void cooldownScalesWithMindPersonalityAndEntityMultiplier() {
        assertEquals(100, AvatarCastMath.pageCooldownTicks(100, 1.0, 1.0, 1.0, false));
        assertEquals(70, AvatarCastMath.pageCooldownTicks(100, 1.0, 0.7, 1.0, false), "lazy x0.7");
        assertEquals(50, AvatarCastMath.pageCooldownTicks(100, 0.5, 1.0, 1.0, false));
        assertEquals(10, AvatarCastMath.pageCooldownTicks(100, 1.0, 1.0, 0.1, true), "frenzy x0.1");
        assertEquals(0, AvatarCastMath.pageCooldownTicks(0, 1.0, 1.0, 1.0, false));
    }

    @Test
    void frenzyMultiplierIsNeverAppliedTwice() {
        int once = AvatarCastMath.pageCooldownTicks(1000, 1.0, 1.0, 1.0, true);
        int twice = AvatarCastMath.pageCooldownTicks(1000, 1.0, 1.0, 0.1, true);
        assertEquals(100, once);
        assertEquals(100, twice, "an entity that already reports 0.1 does not stack with the frenzy flag");
        assertEquals(100, AvatarCastMath.pageCooldownTicks(1000, 1.0, 1.0, 0.1, false));
    }

    @Test
    void theGapBetweenCastsHasAFloor() {
        assertEquals(60, AvatarCastMath.gapTicks(60, 1.0, 1.0, 1.0, false));
        assertEquals(8, AvatarCastMath.gapTicks(60, 0.5, 0.7, 0.1, true), "never below 8 ticks");
        assertEquals(8, AvatarCastMath.gapTicks(5, 1.0, 1.0, 1.0, false));
    }

    // ---------------------------------------------------------------- planner
    private static AvatarCastPlanner.Candidate c(int mode, String id, double cost) {
        return new AvatarCastPlanner.Candidate(mode, id, cost, AvatarCastPlanner.looksMelee(id), AvatarSkillList.kind(id, cost));
    }

    @Test
    void plannerPicksNothingFromNothingAndSkipsUtility() {
        assertEquals(-1, AvatarCastPlanner.choose(List.of(), 5, EnumSet.noneOf(AvatarPersonality.class), Rng.seeded(1)));
        List<AvatarCastPlanner.Candidate> util = List.of(c(0, "acceleration", 8));
        assertEquals(-1, AvatarCastPlanner.choose(util, 5, EnumSet.noneOf(AvatarPersonality.class), Rng.seeded(1)));
    }

    @Test
    void plannerPrefersTheRightRange() {
        List<AvatarCastPlanner.Candidate> l = List.of(c(0, "iron_strike", 8), c(1, "boulder_shot", 8));
        int close = 0, far = 0;
        for (long s = 0; s < 200; s++) {
            if (AvatarCastPlanner.choose(l, 2.0, EnumSet.noneOf(AvatarPersonality.class), Rng.seeded(s)) == 0) close++;
            if (AvatarCastPlanner.choose(l, 15.0, EnumSet.noneOf(AvatarPersonality.class), Rng.seeded(s)) == 1) far++;
        }
        assertTrue(close > 150 && far > 150, "close " + close + ", far " + far);
    }

    @Test
    void aggressiveGoesBigAndSadisticGoesSmall() {
        List<AvatarCastPlanner.Candidate> l = List.of(c(0, "cheap_bolt", 4), c(1, "big_bolt", 20));
        int aggBig = 0, sadSmall = 0;
        for (long s = 0; s < 200; s++) {
            if (AvatarCastPlanner.choose(l, 12.0, EnumSet.of(AvatarPersonality.AGGRESSIVE), Rng.seeded(s)) == 1) aggBig++;
            if (AvatarCastPlanner.choose(l, 12.0, EnumSet.of(AvatarPersonality.SADISTIC), Rng.seeded(s)) == 0) sadSmall++;
        }
        assertTrue(aggBig > 120, "aggressive picked the big one " + aggBig + "/200");
        assertTrue(sadSmall > 120, "sadistic picked the small one " + sadSmall + "/200");
    }

    @Test
    void plannerIsDeterministicForASeed() {
        List<AvatarCastPlanner.Candidate> l = List.of(c(0, "a_bolt", 8), c(1, "b_bolt", 8), c(2, "c_bolt", 8));
        for (long s = 0; s < 30; s++)
            assertEquals(AvatarCastPlanner.choose(l, 9, Set.of(), Rng.seeded(s)), AvatarCastPlanner.choose(l, 9, Set.of(), Rng.seeded(s)));
    }
}
