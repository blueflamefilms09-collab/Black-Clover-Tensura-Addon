package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.luck.LuckMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the creator's luck decides the dice and tilts the outcome, so its arithmetic (sum, per-source caps, clamping) must be exact. */
class LuckMathTest {
    private static final double EPS = 1e-9;

    @Test
    void noSourcesIsZero() { assertEquals(0.0, LuckMath.total(0, 0, false, false, false), EPS); }

    @Test
    void theFourSourcesAdd() {
        // vanilla 2 + two skills 4 + fate 3 + dice 2
        assertEquals(11.0, LuckMath.total(2.0, 2, true, false, true), EPS);
    }

    @Test
    void rougeArmedAndRougeOutDoNotStack() {
        assertEquals(3.0, LuckMath.total(0, 0, true, true, false), EPS);
        assertEquals(3.0, LuckMath.total(0, 0, false, true, false), EPS);
    }

    @Test
    void skillBonusIsCapped() {
        assertEquals(LuckMath.SKILL_MAX, LuckMath.skillBonus(50), EPS);
        assertEquals(0.0, LuckMath.skillBonus(-3), EPS);
    }

    @Test
    void totalIsClamped() {
        assertEquals(LuckMath.MAX, LuckMath.total(1000, 0, false, false, false), EPS);
        assertEquals(LuckMath.MIN, LuckMath.total(-1000, 0, false, false, false), EPS);
        assertEquals(0.0, LuckMath.total(Double.NaN, 0, false, false, false), EPS);
    }

    @Test
    void luckSkillIdsAreRecognised() {
        assertTrue(LuckMath.looksLikeLuckSkill("probability_manipulation"));
        assertTrue(LuckMath.looksLikeLuckSkill("Lucky_Charm"));
        assertTrue(LuckMath.looksLikeLuckSkill("fortune_teller"));
        assertFalse(LuckMath.looksLikeLuckSkill("sage"));
        assertFalse(LuckMath.looksLikeLuckSkill(null));
    }

    @Test
    void tensurasProbabilityLivesInTheTunerSkill() {
        // the 2.0.1.3 id list has no skill with "probability" in its id: the Unique skill Tuner is the one that "manipulates probability"
        assertTrue(LuckMath.looksLikeLuckSkill("tuner"));
        assertTrue(LuckMath.looksLikeLuckSkill("tensura:tuner"));
        assertTrue(LuckMath.looksLikeLuckSkill("TENSURA:TUNER"));
        assertFalse(LuckMath.looksLikeLuckSkill("tensura:tunerx_not_a_skill"), "only the exact path counts");
        assertFalse(LuckMath.looksLikeLuckSkill("tensura:commander"));
    }
}
