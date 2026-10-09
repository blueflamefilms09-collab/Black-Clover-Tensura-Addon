package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivenCombatTest {
    @Test
    void phaseTwoCanStartByTimeOrHealthAndRiftStartsAtSixtySeconds() {
        assertEquals(1, RivenCombat.phase(0.9f, 399, 1));
        assertEquals(2, RivenCombat.phase(0.9f, 400, 1));
        assertEquals(2, RivenCombat.phase(0.75f, 100, 1));
        assertEquals(3, RivenCombat.phase(0.9f, 1200, 2));
        assertEquals(4, RivenCombat.phase(0.25f, 1200, 3));
        assertEquals(3, RivenCombat.nextPhase(2, 4));
        assertEquals(4, RivenCombat.nextPhase(3, 4));
    }

    @Test
    void combatRollCadenceAndStylesMatchTheSpec() {
        assertEquals(240, RivenCombat.rollInterval(2));
        assertEquals(160, RivenCombat.rollInterval(3));
        assertEquals("Caster", RivenCombat.combatType(0));
        assertEquals("Hack", RivenCombat.combatType(5));
        assertEquals("Caster", RivenCombat.combatType(6));
    }

    @Test
    void grimoireDrawUsesOnlyValidatedBlackCloverPages() {
        AnimeSkill blackClover = skill("voltage_page", 1, "black_clover");
        AnimeSkill antiMagic = skill("anti_magic_lord", 1, "black_clover");
        AnimeSkill generic = skill("generic_page", 1, "original");
        assertEquals(List.of(blackClover), RivenCombat.grimoirePages(List.of(blackClover, antiMagic, generic), 1));
    }

    @Test
    void damageBandsMatchTheRaisedCeilingSheet() {
        assertEquals(12f, RivenCombat.damage(1, false, 0f));
        assertEquals(18f, RivenCombat.damage(1, false, 1f));
        assertEquals(22f, RivenCombat.damage(2, false, 0f));
        assertEquals(34f, RivenCombat.damage(2, false, 1f));
        assertEquals(34f, RivenCombat.damage(3, false, 0f));
        assertEquals(48f, RivenCombat.damage(3, false, 1f));
        assertEquals(48f, RivenCombat.damage(4, false, 0f));
        assertEquals(64f, RivenCombat.damage(4, false, 1f));
        assertEquals(22f, RivenCombat.damage(1, true, 0.5f));
        assertEquals(42f, RivenCombat.damage(2, true, 0.5f));
        assertEquals(62f, RivenCombat.damage(3, true, 0.5f));
        assertEquals(85f, RivenCombat.damage(4, true, 0.5f));
    }

    @Test
    void everyCombatTellIsBetweenSevenTenthsAndFourteenTenthsOfASecond() {
        for (int phase = 1; phase <= 4; phase++) {
            for (int cast = 0; cast <= 100; cast++) {
                int ticks = RivenCombat.castTicks(cast, phase);
                assertTrue(ticks >= 14 && ticks <= 28, "cast=" + cast + " phase=" + phase);
            }
        }
    }

    @Test
    void phaseFourSignatureSpacingIsFourSeconds() {
        long readyAt = RivenCombat.nextSignatureAt(100);
        assertEquals(180, readyAt);
        assertFalse(RivenCombat.signatureReady(179, readyAt));
        assertTrue(RivenCombat.signatureReady(180, readyAt));
    }

    @Test
    void castSpeedIncreasesEightPercentPerPhase() {
        assertEquals(28, RivenCombat.castTicks(28, 1));
        assertEquals(26, RivenCombat.castTicks(28, 2));
        assertEquals(24, RivenCombat.castTicks(28, 3));
        assertEquals(23, RivenCombat.castTicks(28, 4));
    }

    @Test
    void multiHitAttackDelaysMatchTheirReadableTelegraphs() {
        assertEquals(0, RivenCombat.eldritchBoltDelay(0));
        assertEquals(4, RivenCombat.eldritchBoltDelay(1));
        assertEquals(8, RivenCombat.eldritchBoltDelay(2));
        assertEquals(0, RivenCombat.hexbladeSwingDelay(0));
        assertEquals(4, RivenCombat.hexbladeSwingDelay(1));
        assertEquals(8, RivenCombat.hexbladeSwingDelay(2));
        assertEquals(2, RivenCombat.firstPhase(skill("hexblade_waltz", 3)));
    }

    @Test
    void audienceCollapseScalesByLivingPartySizeAndCrownShardsStayNormal() {
        assertEquals(48f, RivenCombat.audienceDamage(0));
        assertEquals(48f, RivenCombat.audienceDamage(1));
        assertEquals(56f, RivenCombat.audienceDamage(2));
        assertEquals(72f, RivenCombat.audienceDamage(4));
        AnimeSkill crownBreak = skill("crown_break", 5);
        AnimeSkill finalPage = skill("final_page", 5);
        assertFalse(RivenCombat.signature(crownBreak));
        assertTrue(RivenCombat.signature(finalPage));
    }

    private static AnimeSkill skill(String nativeId, int tier) {
        return skill(nativeId, tier, "original");
    }

    private static AnimeSkill skill(String nativeId, int tier, String anime) {
        return new AnimeSkill("nusmp:" + nativeId, anime, nativeId, tier, 20, 10, List.of(), List.of(), List.of(),
                "cast_grimoire", "", nativeId, "weapon", 100);
    }
}
