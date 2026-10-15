package com.newuniverse.nusmp.boss.rules;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The boss rules' verdicts: the regeneration family and every Unique or Ultimate skill are forbidden, standard skills and magic are not. */
class BossSkillPolicyTest {
    @Test
    void everyRealRegenerationIdIsForbidden() {
        for (String id : BossSkillPolicy.REGENERATION_IDS)
            assertTrue(BossSkillPolicy.isForbidden(id, "extra"), id);
        assertTrue(BossSkillPolicy.isForbidden("tensura:instant_regeneration", null));
        assertTrue(BossSkillPolicy.isForbidden("tensura:self_regeneration", "common"));
        assertTrue(BossSkillPolicy.isForbidden("tensura:ultraspeed_regeneration", "extra"));
        assertTrue(BossSkillPolicy.isForbidden("othermod:Second Wind", "common"));
        assertTrue(BossSkillPolicy.isForbidden("othermod:second-wind", "common"));
        assertTrue(BossSkillPolicy.isForbidden("nusmp:revive_totem", "other"));
    }

    @Test
    void uniqueAndUltimateAreForbiddenByCategory() {
        assertTrue(BossSkillPolicy.isForbidden("tensura:sage", "unique"));
        assertTrue(BossSkillPolicy.isForbidden("tensura:anything_new", "UNIQUE"));
        assertTrue(BossSkillPolicy.isForbidden("addon:future_skill", "ultimate"));
        assertTrue(BossSkillPolicy.isForbidden("tensura:raphael", "ultimate"));
    }

    @Test
    void anUnreadableTensuraSkillFailsClosedButOtherModsAreNot() {
        assertTrue(BossSkillPolicy.isForbidden("tensura:great_sage", null));
        assertTrue(BossSkillPolicy.isForbidden("tensura:starved", "unknown"));
        assertTrue(BossSkillPolicy.isForbidden("tensura:mathematician", ""));
        assertFalse(BossSkillPolicy.isForbidden("othermod:starved", null));
    }

    @Test
    void standardSkillsInnateTraitsAndMagicStayAllowed() {
        assertFalse(BossSkillPolicy.isForbidden("tensura:mortal_fear", "extra"));
        assertFalse(BossSkillPolicy.isForbidden("tensura:heat_wave", "extra"));
        assertFalse(BossSkillPolicy.isForbidden("tensura:poison_resistance", "resistance"));
        assertFalse(BossSkillPolicy.isForbidden("tensura:body_armor", "intrinsic"));
        assertFalse(BossSkillPolicy.isForbidden("tensura:strength", "common"));
        assertFalse(BossSkillPolicy.isForbidden("tensura:fire_ball", "magic"));
        assertFalse(BossSkillPolicy.isForbidden("tensura:sword_dance", "battlewill"));
        assertFalse(BossSkillPolicy.isForbidden("nusmp:book_time", "other"));
        assertFalse(BossSkillPolicy.isForbidden(null, "unique"));
        assertFalse(BossSkillPolicy.isForbidden("", "unique"));
    }

    @Test
    void eachHalfFollowsItsSwitch() {
        BossSkillPolicy.Settings noStrip = new BossSkillPolicy.Settings(false, true, BossSkillPolicy.DEFAULT_WORDS);
        BossSkillPolicy.Settings noBan = new BossSkillPolicy.Settings(true, false, BossSkillPolicy.DEFAULT_WORDS);
        BossSkillPolicy.Settings neither = new BossSkillPolicy.Settings(false, false, "");
        assertFalse(BossSkillPolicy.isForbidden("tensura:self_regeneration", "common", noStrip));
        assertTrue(BossSkillPolicy.isForbidden("tensura:sage", "unique", noStrip));
        assertTrue(BossSkillPolicy.isForbidden("tensura:self_regeneration", "common", noBan));
        assertFalse(BossSkillPolicy.isForbidden("tensura:sage", "unique", noBan));
        assertFalse(BossSkillPolicy.isForbidden("tensura:self_regeneration", "unique", neither));
        assertFalse(BossSkillPolicy.isForbiddenEffect("tensura:instant_regeneration", noStrip));
    }

    @Test
    void theConfigWordsExtendButNeverShrinkTheBuiltInOnes() {
        BossSkillPolicy.Settings s = new BossSkillPolicy.Settings(true, true, " Vitality , mend,, ");
        assertTrue(BossSkillPolicy.isForbidden("addon:vitality_surge", "common", s));
        assertTrue(BossSkillPolicy.isForbidden("addon:mend_flesh", "common", s));
        assertTrue(BossSkillPolicy.isForbidden("tensura:self_regeneration", "common", s));
        BossSkillPolicy.Settings empty = new BossSkillPolicy.Settings(true, true, null);
        assertTrue(BossSkillPolicy.isForbidden("tensura:healing", "extra", empty));
        assertFalse(BossSkillPolicy.isForbidden("addon:vitality_surge", "common", empty));
    }

    @Test
    void effectsOfTheOldEngineAreForbidden() {
        BossSkillPolicy.Settings s = BossSkillPolicy.Settings.DEFAULT;
        assertTrue(BossSkillPolicy.isForbiddenEffect("tensura:instant_regeneration", s));
        assertTrue(BossSkillPolicy.isForbiddenEffect("tensura:self_regeneration", s));
        assertTrue(BossSkillPolicy.isForbiddenEffect("minecraft:regeneration", s));
        assertTrue(BossSkillPolicy.isForbiddenEffect("minecraft:instant_health", s));
        assertFalse(BossSkillPolicy.isForbiddenEffect("tensura:magic_barrier", s));
        assertFalse(BossSkillPolicy.isForbiddenEffect("tensura:physical_barrier", s));
        assertFalse(BossSkillPolicy.isForbiddenEffect("tensura:haki_coat", s));
    }

    @Test
    void theEngineThemeSkillsAreSortedAsTheOwnerWants() {
        List<String> kept = BossSkillPolicy.allowed(List.of("tensura:starved", "tensura:mathematician", "tensura:mortal_fear"),
                id -> null, BossSkillPolicy.Settings.DEFAULT);
        assertEquals(List.of("tensura:mortal_fear"), kept);
    }

    @Test
    void healPolicyOnlyBlocksABossInCombat() {
        assertTrue(BossHealPolicy.inCombat(true, 10, 0));
        assertTrue(BossHealPolicy.inCombat(false, 300, 150));
        assertFalse(BossHealPolicy.inCombat(false, 400, 150));
        assertFalse(BossHealPolicy.inCombat(false, 400, 0));
        assertTrue(BossHealPolicy.blocksHeal(true, true, true, false));
        assertFalse(BossHealPolicy.blocksHeal(false, true, true, false));
        assertFalse(BossHealPolicy.blocksHeal(true, false, true, false));
        assertFalse(BossHealPolicy.blocksHeal(true, true, false, false));
        assertFalse(BossHealPolicy.blocksHeal(true, true, true, true));
    }
}
