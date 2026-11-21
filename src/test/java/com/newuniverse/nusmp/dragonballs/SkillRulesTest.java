package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.dragonballs.SkillRules.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Which skills a wish may teach and what the tier ceilings are, with stand-ins for the skill type (no game, no Tensura class). */
class SkillRulesTest {
    @Test
    void theTierIsAPureFunctionOfTheSkillType() {
        assertEquals(Tier.RESISTANCE, SkillRules.tierOf("resistance"));
        assertEquals(Tier.COMMON, SkillRules.tierOf("common"));
        assertEquals(Tier.EXTRA, SkillRules.tierOf("EXTRA"));
        assertEquals(Tier.UNIQUE, SkillRules.tierOf("unique"));
        assertEquals(Tier.ULTIMATE, SkillRules.tierOf("Ultimate"));
        assertNull(SkillRules.tierOf("intrinsic"), "intrinsic skills belong to a race");
        assertNull(SkillRules.tierOf("whatever_a_new_mod_adds"));
        assertNull(SkillRules.tierOf(null));
        assertEquals(Tier.UNIQUE, Tier.byName(" unique "));
        assertNull(Tier.byName("nonsense"));
    }

    @Test
    void tiersAreOrderedLowestFirst() {
        Tier[] t = Tier.values();
        for (int i = 1; i < t.length; i++) assertTrue(t[i - 1].ordinal() < t[i].ordinal());
        assertEquals(Tier.ULTIMATE, t[t.length - 1]);
    }

    @Test
    void superReachesUniqueAndUltimateEarthStopsAtItsSetting() {
        assertEquals(Tier.ULTIMATE, SkillRules.ceiling(BallSet.SUPER, Tier.EXTRA, true));
        assertEquals(Tier.UNIQUE, SkillRules.ceiling(BallSet.SUPER, Tier.EXTRA, false), "ultimates off: Super stops at Unique");
        assertEquals(Tier.EXTRA, SkillRules.ceiling(BallSet.EARTH, Tier.EXTRA, true));
        assertEquals(Tier.EXTRA, SkillRules.ceiling(BallSet.EARTH, null, true), "no setting: Extra");
        assertEquals(Tier.ULTIMATE, SkillRules.ceiling(BallSet.EARTH, Tier.ULTIMATE, true), "Earth may be raised up to Ultimate");
        assertEquals(Tier.UNIQUE, SkillRules.ceiling(BallSet.EARTH, Tier.ULTIMATE, false), "never above what Ultimates-off allows");
        for (Tier e : Tier.values()) assertTrue(SkillRules.ceiling(BallSet.EARTH, e, true).ordinal() <= SkillRules.ceiling(BallSet.SUPER, e, true).ordinal());
    }

    @Test
    void aSkillIsAllowedOnlyAtOrBelowTheCeiling() {
        assertTrue(SkillRules.allowed(Tier.COMMON, Tier.EXTRA));
        assertTrue(SkillRules.allowed(Tier.EXTRA, Tier.EXTRA));
        assertFalse(SkillRules.allowed(Tier.UNIQUE, Tier.EXTRA));
        assertTrue(SkillRules.allowed(Tier.ULTIMATE, Tier.ULTIMATE));
        assertFalse(SkillRules.allowed(null, Tier.ULTIMATE), "no tier, never offered");
        assertFalse(SkillRules.allowed(Tier.COMMON, null));
    }

    @Test
    void skillsPerWishIsOneOnEarthAndTheSettingOnSuper() {
        assertEquals(1, SkillRules.perWish(BallSet.EARTH, 5));
        assertEquals(3, SkillRules.perWish(BallSet.SUPER, 3));
        assertEquals(1, SkillRules.perWish(BallSet.SUPER, 0));
        assertEquals(5, SkillRules.perWish(BallSet.SUPER, 99));
    }

    @Test
    void removedSoulTiedAndManagedSkillsOfThisModAreNeverTaught() {
        assertTrue(SkillRules.blockedId("anime_multiverse:six_eyes"), "a removed skill");
        assertTrue(SkillRules.blockedId("anime_multiverse:hk_provoke"), "a retired technique");
        assertTrue(SkillRules.blockedId("anime_multiverse:grimoire_fire"), "soul tied");
        assertTrue(SkillRules.blockedId("anime_multiverse:spirit_lord"));
        assertTrue(SkillRules.blockedId("anime_multiverse:ws_something"), "a weapon skill");
        assertTrue(SkillRules.blockedId("anime_multiverse:wizard_king"), "managed by a service");
        assertTrue(SkillRules.blockedId("anime_multiverse:shishou"));
        assertFalse(SkillRules.blockedId("anime_multiverse:book_maker"), "a normal Ultimate of this mod");
        assertTrue(SkillRules.blockedId(null));
    }

    @Test
    void otherModsIdsAreNeverBlockedByPath() {
        assertFalse(SkillRules.blockedId("tensura:eye_of_truth"));
        assertFalse(SkillRules.blockedId("othermod:six_eyes"), "same path in another mod is that mod's skill");
        assertFalse(SkillRules.blockedId("othermod:ws_slash"));
    }

    @Test
    void stubClassesAreRecognisedByName() {
        assertTrue(SkillRules.stubClass("LegacySkill"));
        assertTrue(SkillRules.stubClass("RetiredTechnique"));
        assertFalse(SkillRules.stubClass("EyeOfTruthSkill"));
        assertFalse(SkillRules.stubClass(null));
    }

    @Test
    void theWholeDecisionCombinesTierBlockAndStub() {
        assertTrue(SkillRules.teachable("tensura:eye_of_truth", "unique", "EyeOfTruth", Tier.ULTIMATE));
        assertFalse(SkillRules.teachable("tensura:eye_of_truth", "unique", "EyeOfTruth", Tier.EXTRA));
        assertFalse(SkillRules.teachable("tensura:tough", "intrinsic", "Tough", Tier.ULTIMATE));
        assertFalse(SkillRules.teachable("anime_multiverse:six_eyes", "unique", "SixEyes", Tier.ULTIMATE));
        assertFalse(SkillRules.teachable("othermod:gone", "unique", "LegacySkill", Tier.ULTIMATE));
        assertTrue(SkillRules.teachable("othermod:new_skill", "extra", "NewSkill", Tier.EXTRA));
    }
}
