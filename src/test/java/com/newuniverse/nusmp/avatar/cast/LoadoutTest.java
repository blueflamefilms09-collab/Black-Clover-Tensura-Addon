package com.newuniverse.nusmp.avatar.cast;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.chatter.Rng;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The virtual skill loadout: which Tensura skills a mob presses and which it leaves alone, how many pages an Avatar's book has open, and which entry wins. */
class LoadoutTest {
    // ---------------------------------------------------------------- skill roles
    @Test
    void fightingSkillsAreAttacks() {
        for (String id : new String[]{"tensura:absolute_severance", "tensura:gluttony", "tensura:black_flame", "tensura:sniper", "tensura:oppressor", "predator"})
            assertEquals(SkillRoles.Role.ATTACK, SkillRoles.role(id), id);
    }

    @Test
    void buffsAndHealsAreRecognised() {
        assertEquals(SkillRoles.Role.BUFF, SkillRoles.role("tensura:berserk"));
        assertEquals(SkillRoles.Role.BUFF, SkillRoles.role("tensura:royal_beast"));
        assertEquals(SkillRoles.Role.BUFF, SkillRoles.role("tensura:thought_acceleration"));
        assertEquals(SkillRoles.Role.HEAL, SkillRoles.role("tensura:healer"));
        assertEquals(SkillRoles.Role.HEAL, SkillRoles.role("tensura:infinite_regeneration"));
        assertEquals(SkillRoles.Role.HEAL, SkillRoles.role("tensura:chef"));
    }

    @Test
    void screenCameraAndTeleportSkillsAreNeverPressed() {
        for (String id : new String[]{"tensura:creator", "tensura:degenerate", "tensura:great_sage", "tensura:all_seeing_eye", "tensura:traveler", "tensura:spatial_motion",
                "tensura:telepathy", "tensura:analytical_appraisal", "tensura:universal_perception", "tensura:magic_sense", "tensura:researcher", "tensura:suppressor"})
            assertEquals(SkillRoles.Role.NONE, SkillRoles.role(id), id);
        assertEquals(SkillRoles.Role.NONE, SkillRoles.role(null));
        assertEquals(SkillRoles.Role.NONE, SkillRoles.role(""));
    }

    @Test
    void theNamespaceAndTheCaseDoNotMatter() {
        assertEquals(SkillRoles.role("tensura:berserk"), SkillRoles.role("BERSERK"));
        assertEquals(SkillRoles.role("nusmp:berserk"), SkillRoles.role("berserk"));
    }

    @Test
    void cooldownsByRole() {
        assertTrue(SkillRoles.baseCooldown(SkillRoles.Role.ATTACK, true) > SkillRoles.baseCooldown(SkillRoles.Role.ATTACK, false), "an Ultimate waits longer");
        assertTrue(SkillRoles.baseCooldown(SkillRoles.Role.BUFF, false) > SkillRoles.baseCooldown(SkillRoles.Role.ATTACK, false));
        assertTrue(SkillRoles.baseCooldown(SkillRoles.Role.NONE, false) > 1_000_000, "never");
    }

    // ---------------------------------------------------------------- pages
    @Test
    void aFreshAvatarHasTheStarterPageAndAStrongOneMore() {
        assertEquals(1, PageTier.unlockedPages(10, 0, 10));
        assertEquals(1, PageTier.unlockedPages(10, 100, 10));
        assertEquals(2, PageTier.unlockedPages(10, 150, 10));
        assertEquals(3, PageTier.unlockedPages(10, 600, 10));
        assertEquals(6, PageTier.unlockedPages(10, 3750, 10));
    }

    @Test
    void theBookIsNeverMoreThanItsLengthAndACleverMindOpensOneMore() {
        assertEquals(10, PageTier.unlockedPages(10, 1_000_000, 10));
        assertEquals(3, PageTier.unlockedPages(3, 1_000_000, 10));
        assertEquals(2, PageTier.unlockedPages(10, 100, 18), "INT 16 or more opens one more page");
        assertEquals(0, PageTier.unlockedPages(0, 5000, 10));
        assertEquals(1, PageTier.unlockedPages(5, -50, 3), "garbage in, the starter page out");
    }

    @Test
    void theMaskCoversExactlyThoseModes() {
        assertEquals(0, PageTier.mask(0));
        assertEquals(1, PageTier.mask(1));
        assertEquals(0b111, PageTier.mask(3));
        assertEquals(0x7FFFFFFF, PageTier.mask(40));
    }

    // ---------------------------------------------------------------- ordering
    private static LoadoutPlanner.Entry e(LoadoutPlanner.Kind k, String id, SkillRoles.Role role, double cost, boolean melee, int index) {
        return new LoadoutPlanner.Entry(k, id, id, 0, 0, role, cost, 100, melee, LoadoutPlanner.rankOf(k, index));
    }

    @Test
    void theCreatorsPicksComeBeforeTheFallbacks() {
        assertTrue(LoadoutPlanner.rankOf(LoadoutPlanner.Kind.ULTIMATE, 0) < LoadoutPlanner.rankOf(LoadoutPlanner.Kind.UNIQUE, 0));
        assertTrue(LoadoutPlanner.rankOf(LoadoutPlanner.Kind.UNIQUE, 0) < LoadoutPlanner.rankOf(LoadoutPlanner.Kind.PERK, 0));
        assertTrue(LoadoutPlanner.rankOf(LoadoutPlanner.Kind.PERK, 0) < LoadoutPlanner.rankOf(LoadoutPlanner.Kind.APTITUDE_SKILL, 0));
        assertTrue(LoadoutPlanner.rankOf(LoadoutPlanner.Kind.APTITUDE_SKILL, 9) < LoadoutPlanner.rankOf(LoadoutPlanner.Kind.APTITUDE_PAGE, 0));
        assertTrue(LoadoutPlanner.rankOf(LoadoutPlanner.Kind.APTITUDE_PAGE, 9) < LoadoutPlanner.rankOf(LoadoutPlanner.Kind.CREATOR_PAGE, 0));
        assertTrue(LoadoutPlanner.rankOf(LoadoutPlanner.Kind.CREATOR_PAGE, 0) < LoadoutPlanner.rankOf(LoadoutPlanner.Kind.TENSURA_MAGIC, 0));
        assertTrue(LoadoutPlanner.rankOf(LoadoutPlanner.Kind.APTITUDE_SKILL, 1) < LoadoutPlanner.rankOf(LoadoutPlanner.Kind.APTITUDE_SKILL, 2), "aptitudes keep the order they were chosen in");
    }

    @Test
    void orderSortsByRankThenIdAndLeavesTheInputAlone() {
        List<LoadoutPlanner.Entry> in = List.of(e(LoadoutPlanner.Kind.TENSURA_MAGIC, "fire_ball", SkillRoles.Role.ATTACK, 6, false, 0),
                e(LoadoutPlanner.Kind.ULTIMATE, "tensura:z", SkillRoles.Role.ATTACK, 15, false, 0), e(LoadoutPlanner.Kind.UNIQUE, "tensura:b", SkillRoles.Role.ATTACK, 8, false, 0),
                e(LoadoutPlanner.Kind.UNIQUE, "tensura:a", SkillRoles.Role.ATTACK, 8, false, 0));
        List<LoadoutPlanner.Entry> out = LoadoutPlanner.order(in);
        assertEquals("tensura:z", out.get(0).id());
        assertEquals("tensura:a", out.get(1).id());
        assertEquals("tensura:b", out.get(2).id());
        assertEquals("fire_ball", out.get(3).id());
        assertEquals("fire_ball", in.get(0).id(), "the input list is not changed");
    }

    // ---------------------------------------------------------------- the choice
    private static final LoadoutPlanner.Ctx FIGHT = new LoadoutPlanner.Ctx(10.0, 40.0, 1.0, false);

    @Test
    void nothingReadyChoosesNothing() {
        assertEquals(-1, LoadoutPlanner.pick(List.of(), FIGHT, Set.of(), Rng.seeded(1)));
        assertEquals(-1, LoadoutPlanner.pick(null, FIGHT, Set.of(), Rng.seeded(1)));
        assertEquals(-1, LoadoutPlanner.pick(List.of(e(LoadoutPlanner.Kind.UNIQUE, "x", SkillRoles.Role.BUFF, 5, false, 0)), FIGHT, Set.of(), Rng.seeded(1)),
                "a buff is not an attack, and the fight has not just begun");
    }

    @Test
    void aHurtAvatarHealsFirstAndAFreshFightBuffsFirst() {
        List<LoadoutPlanner.Entry> l = List.of(e(LoadoutPlanner.Kind.ULTIMATE, "atk", SkillRoles.Role.ATTACK, 10, false, 0),
                e(LoadoutPlanner.Kind.UNIQUE, "heal", SkillRoles.Role.HEAL, 5, false, 0), e(LoadoutPlanner.Kind.APTITUDE_SKILL, "buff", SkillRoles.Role.BUFF, 5, false, 0));
        assertEquals(1, LoadoutPlanner.pick(l, new LoadoutPlanner.Ctx(10, 40, 0.3, false), Set.of(), Rng.seeded(1)));
        assertEquals(2, LoadoutPlanner.pick(l, new LoadoutPlanner.Ctx(10, 40, 1.0, true), Set.of(), Rng.seeded(1)));
        assertEquals(1, LoadoutPlanner.pick(l, new LoadoutPlanner.Ctx(10, 40, 0.3, true), Set.of(), Rng.seeded(1)), "healing beats buffing");
    }

    @Test
    void theUltimateIsSavedForABigEnemyOrATightSpot() {
        List<LoadoutPlanner.Entry> l = List.of(e(LoadoutPlanner.Kind.ULTIMATE, "ultimate", SkillRoles.Role.ATTACK, 15, false, 0),
                e(LoadoutPlanner.Kind.TENSURA_MAGIC, "fire_ball", SkillRoles.Role.ATTACK, 6, false, 0));
        int smallFightUltimate = 0, bossFightUltimate = 0, hurtFightUltimate = 0;
        for (long s = 0; s < 200; s++) {
            if (LoadoutPlanner.pick(l, new LoadoutPlanner.Ctx(10, 20, 1.0, false), Set.of(), Rng.seeded(s)) == 0) smallFightUltimate++;
            if (LoadoutPlanner.pick(l, new LoadoutPlanner.Ctx(10, 300, 1.0, false), Set.of(), Rng.seeded(s)) == 0) bossFightUltimate++;
            if (LoadoutPlanner.pick(l, new LoadoutPlanner.Ctx(10, 20, 0.45, false), Set.of(), Rng.seeded(s)) == 0) hurtFightUltimate++;
        }
        assertEquals(0, smallFightUltimate, "no Ultimate on a rabbit");
        assertTrue(bossFightUltimate > 150, "on a boss " + bossFightUltimate);
        assertTrue(hurtFightUltimate > 150, "when in trouble " + hurtFightUltimate);
    }

    @Test
    void thePersonalityTiltsTheChoice() {
        List<LoadoutPlanner.Entry> l = List.of(e(LoadoutPlanner.Kind.APTITUDE_PAGE, "small_bolt", SkillRoles.Role.ATTACK, 2, false, 1),
                e(LoadoutPlanner.Kind.APTITUDE_PAGE, "big_bolt", SkillRoles.Role.ATTACK, 20, false, 1));
        int aggBig = 0, lazySmall = 0;
        Set<AvatarPersonality> agg = EnumSet.of(AvatarPersonality.AGGRESSIVE), lazy = EnumSet.of(AvatarPersonality.LAZY);
        for (long s = 0; s < 200; s++) {
            if (LoadoutPlanner.pick(l, FIGHT, agg, Rng.seeded(s)) == 1) aggBig++;
            if (LoadoutPlanner.pick(l, FIGHT, lazy, Rng.seeded(s)) == 0) lazySmall++;
        }
        assertTrue(aggBig > 120 && lazySmall > 120, "aggressive big " + aggBig + ", lazy small " + lazySmall);
    }

    @Test
    void aCowardPrefersWhatItCanThrowFromAfar() {
        List<LoadoutPlanner.Entry> l = List.of(e(LoadoutPlanner.Kind.CREATOR_PAGE, "iron_strike", SkillRoles.Role.ATTACK, 8, true, 0),
                e(LoadoutPlanner.Kind.CREATOR_PAGE, "boulder_shot", SkillRoles.Role.ATTACK, 8, false, 1));
        int ranged = 0;
        for (long s = 0; s < 200; s++) if (LoadoutPlanner.pick(l, new LoadoutPlanner.Ctx(6.0, 40, 1.0, false), EnumSet.of(AvatarPersonality.COWARDLY), Rng.seeded(s)) == 1) ranged++;
        assertTrue(ranged > 150, "ranged " + ranged);
    }

    @Test
    void theChoiceIsRepeatableForASeed() {
        List<LoadoutPlanner.Entry> l = List.of(e(LoadoutPlanner.Kind.APTITUDE_PAGE, "a_bolt", SkillRoles.Role.ATTACK, 8, false, 1), e(LoadoutPlanner.Kind.APTITUDE_PAGE, "b_bolt", SkillRoles.Role.ATTACK, 8, false, 1),
                e(LoadoutPlanner.Kind.CREATOR_PAGE, "c_bolt", SkillRoles.Role.ATTACK, 8, false, 0));
        for (long s = 0; s < 30; s++) assertEquals(LoadoutPlanner.pick(l, FIGHT, Set.of(), Rng.seeded(s)), LoadoutPlanner.pick(l, FIGHT, Set.of(), Rng.seeded(s)));
    }
}
