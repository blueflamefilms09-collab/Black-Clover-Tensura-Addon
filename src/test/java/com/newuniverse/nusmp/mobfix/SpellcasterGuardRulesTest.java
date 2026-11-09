package com.newuniverse.nusmp.mobfix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the guard may only wrap the two vanilla goals that read the target while casting; the other spell goals (the wololo spell needs NO target, the summon and mirror spells never read it) must be left alone. */
class SpellcasterGuardRulesTest {
    @Test
    void onlyTheTwoTargetReadingSpellGoalsAreWrapped() {
        assertTrue(SpellcasterGuardRules.needsTarget("net.minecraft.world.entity.monster.Illusioner$IllusionerBlindnessSpellGoal"));
        assertTrue(SpellcasterGuardRules.needsTarget("net.minecraft.world.entity.monster.Evoker$EvokerAttackSpellGoal"));
        assertFalse(SpellcasterGuardRules.needsTarget("net.minecraft.world.entity.monster.Evoker$EvokerWololoSpellGoal"));
        assertFalse(SpellcasterGuardRules.needsTarget("net.minecraft.world.entity.monster.Evoker$EvokerSummonSpellGoal"));
        assertFalse(SpellcasterGuardRules.needsTarget("net.minecraft.world.entity.monster.Illusioner$IllusionerMirrorSpellGoal"));
        assertFalse(SpellcasterGuardRules.needsTarget("net.minecraft.world.entity.ai.goal.MeleeAttackGoal"));
        assertFalse(SpellcasterGuardRules.needsTarget(null));
    }

    @Test
    void aSpellMayOnlyGoOnWhileTheCasterHasALivingTarget() {
        assertTrue(SpellcasterGuardRules.mayCast(true, true));
        assertFalse(SpellcasterGuardRules.mayCast(false, false));
        assertFalse(SpellcasterGuardRules.mayCast(true, false));
        assertFalse(SpellcasterGuardRules.mayCast(false, true));
    }
}
