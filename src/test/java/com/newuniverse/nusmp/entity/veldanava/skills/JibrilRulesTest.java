package com.newuniverse.nusmp.entity.veldanava.skills;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Jibril's pure rules (docs/veldanava_skills.md): the ten-mode table, the costs, and the helper formulas the skill and the events read. */
class JibrilRulesTest {
    private static final JudgmentRules.Spec S = JibrilRules.JIBRIL;

    @Test
    void theSkillHasTenModesInTheOwnersOrder() {
        assertEquals(10, S.count());
        String[] ids = {"divine_rigor", "heavenly_presence", "strict_governance", "wings_of_revelation", "absolute_command", "judgment_of_heaven", "heavenly_purge", "holy_shackles", "word_of_law", "kingdom_of_god"};
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < ids.length; i++) {
            assertEquals(ids[i], S.mode(i).id());
            assertEquals(i, S.mode(i).index());
            assertTrue(seen.add(S.mode(i).id()), "duplicate id " + ids[i]);
        }
    }

    @Test
    void fourTogglesFiveActivesOneHeldMastery() {
        for (int i = 0; i < 4; i++) { assertTrue(S.mode(i).toggle()); assertEquals(0, S.mode(i).cost()); }
        for (int i = 4; i < 9; i++) { assertFalse(S.mode(i).toggle()); assertTrue(S.mode(i).masteredCost() < S.mode(i).cost(), "mastery makes " + S.mode(i).id() + " cheaper"); }
        assertEquals(JudgmentRules.Kind.HELD, S.mode(9).kind());
        assertTrue(S.mode(5).crouch() && S.mode(7).crouch(), "Judgment of Heaven and Holy Shackles are crouch + press");
        assertFalse(S.mode(4).crouch() || S.mode(6).crouch() || S.mode(8).crouch());
    }

    @Test
    void costsFollowTheOwnersTable() {
        int[][] want = {{5000, 3500}, {8500, 6000}, {7000, 5000}, {4500, 3000}, {6000, 4000}};
        for (int i = 0; i < 5; i++) {
            assertEquals(want[i][0], JudgmentRules.cost(S.mode(4 + i), false, 1.0), 1e-9);
            assertEquals(want[i][1], JudgmentRules.cost(S.mode(4 + i), true, 1.0), 1e-9);
        }
        assertEquals(4000, JudgmentRules.cost(S.mode(9), false, 1.0), 1e-9);
        assertEquals(2500, JudgmentRules.cost(S.mode(9), true, 1.0), 1e-9);
    }

    @Test
    void jibrilIsClaimableWithItsOwnCodeAndTable() {
        assertTrue(JudgmentRules.claimable("jibril"));
        assertEquals(3, JudgmentRules.code("jibril"));
        assertSame(S, JudgmentRules.spec("jibril"));
    }

    @Test
    void divineRigorHalvesAnAlliesDebuffsAndKeepsEndlessOnesEndless() {
        assertEquals(100, JibrilRules.rigorAllyDuration(200));
        assertEquals(1, JibrilRules.rigorAllyDuration(1));
        assertEquals(1, JibrilRules.rigorAllyDuration(0));
        assertEquals(-1, JibrilRules.rigorAllyDuration(-1));
    }

    @Test
    void demonsAreMatchedByWordNotBySubstring() {
        assertTrue(JibrilRules.demonic("demon_lord"));
        assertTrue(JibrilRules.demonic("greater_imp"));
        assertTrue(JibrilRules.demonic("hellhound"));
        assertFalse(JibrilRules.demonic("chimp"));
        assertFalse(JibrilRules.demonic("simple_golem"));
        assertFalse(JibrilRules.demonic(null));
        assertFalse(JibrilRules.demonic(""));
    }

    @Test
    void presenceBurnsHarderWithDebtUpToACap() {
        assertEquals(14.0, JibrilRules.presenceDamage(0), 1e-9);
        assertEquals(14.0 + 5.0, JibrilRules.presenceDamage(10), 1e-9);
        assertEquals(14.0 + 20.0, JibrilRules.presenceDamage(1000), 1e-9);
        assertEquals(14.0, JibrilRules.presenceDamage(-5), 1e-9);
    }

    @Test
    void governanceStretchesEveryOtherSecond() {
        int on = 0;
        for (long t = 0; t < 400; t += 20) if (JibrilRules.governanceSecond(t)) on++;
        assertEquals(10, on, "half of twenty seconds");
        assertTrue(JibrilRules.governanceSecond(0));
        assertFalse(JibrilRules.governanceSecond(20));
        assertTrue(JibrilRules.governanceSecond(39) == JibrilRules.governanceSecond(20), "the whole second counts as one");
    }

    @Test
    void wingsDodgeAChanceNotACertainty() {
        assertEquals(70, JibrilRules.dodgePercent(false));
        assertEquals(85, JibrilRules.dodgePercent(true));
        int dodged = 0, dodgedMastered = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (JibrilRules.dodges(roll, false)) dodged++;
            if (JibrilRules.dodges(roll, true)) dodgedMastered++;
        }
        assertEquals(70, dodged);
        assertEquals(85, dodgedMastered);
        assertFalse(JibrilRules.dodges(-1, true));
        assertFalse(JibrilRules.dodges(100, true));
    }

    @Test
    void theParryWindowAndTheReflectionAreFixed() {
        assertEquals(12, JibrilRules.parryTicks(false));
        assertEquals(20, JibrilRules.parryTicks(true));
        assertEquals(300.0, JibrilRules.reflected(100.0), 1e-9);
        assertEquals(0.0, JibrilRules.reflected(-5.0), 1e-9);
    }

    @Test
    void theKingdomNeverTakesAnAllyBelowOneHealth() {
        assertEquals(9.0, JibrilRules.kingdomLimited(50.0, 10.0, 0.0), 1e-9);
        assertEquals(5.0, JibrilRules.kingdomLimited(5.0, 10.0, 0.0), 1e-9);
        assertEquals(0.0, JibrilRules.kingdomLimited(50.0, 1.0, 0.0), 1e-9, "on one health a blow does nothing");
        assertEquals(0.0, JibrilRules.kingdomLimited(50.0, 0.5, 0.0), 1e-9);
        assertEquals(14.0, JibrilRules.kingdomLimited(50.0, 10.0, 5.0), 1e-9, "absorption counts as room");
        assertEquals(9.0, JibrilRules.kingdomLimited(50.0, 10.0, -4.0), 1e-9, "negative absorption adds nothing");
    }

    @Test
    void stolenEffectsAreCappedAndEndlessOnesBecomeTheCap() {
        assertEquals(100, JibrilRules.stolenTicks(100));
        assertEquals(6000, JibrilRules.stolenTicks(99999));
        assertEquals(6000, JibrilRules.stolenTicks(-1));
        assertEquals(1, JibrilRules.stolenTicks(0));
    }

    @Test
    void theHotbarHoldNeverOutlastsItsCap() {
        assertEquals(60, JibrilRules.hotbarTicks(60));
        assertEquals(200, JibrilRules.hotbarTicks(5000));
        assertEquals(0, JibrilRules.hotbarTicks(-3));
    }

    @Test
    void everyAreaWorkCapIsPositiveAndSmall() {
        assertTrue(JibrilRules.PRESENCE_MAX_TARGETS > 0 && JibrilRules.PRESENCE_MAX_TARGETS <= 16);
        assertTrue(JibrilRules.GOVERNANCE_MAX_PLAYERS > 0 && JibrilRules.GOVERNANCE_MAX_PLAYERS <= 32);
        assertTrue(JibrilRules.PURGE_MAX_CLOUDS > 0 && JibrilRules.PURGE_MAX_CLOUDS <= 128);
        assertTrue(JibrilRules.KINGDOM_MAX_ENEMIES > 0 && JibrilRules.KINGDOM_MAX_ENEMIES <= 32);
        assertTrue(JibrilRules.JUDGMENT_MAX_SPLASH > 0 && JibrilRules.JUDGMENT_MAX_SPLASH <= 8);
        assertTrue(JibrilRules.KINGDOM_MAX_TICKS <= 1200, "a Kingdom ends within the field manager's own limit");
        assertTrue(JibrilRules.KINGDOM_GRACE_TICKS >= 2 * JibrilRules.KINGDOM_PULSE_TICKS || JibrilRules.KINGDOM_GRACE_TICKS >= JibrilRules.KINGDOM_PULSE_TICKS, "a late pulse never opens a gap");
    }
}
