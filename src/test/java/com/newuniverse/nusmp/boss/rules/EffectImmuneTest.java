package com.newuniverse.nusmp.boss.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 4.62.2: the hook, the switch and the rule stay wired (the rule needs a running game to be called, so the source is pinned). */
class EffectImmuneTest {
    private static String read(String f) throws Exception {
        return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/boss/rules/" + f));
    }

    @Test
    void effectDamageIsCancelledOnlyWhenNothingAttackedAndAHarmfulEffectIsActive() throws Exception {
        String env = read("BossEnvironment.java");
        assertTrue(env.contains("static boolean effectDamage"));
        assertTrue(env.contains("source.getEntity() != null || source.getDirectEntity() != null"));
        assertTrue(env.contains("BYPASSES_INVULNERABILITY"), "the void and /kill stay real");
        assertTrue(env.contains("MobEffectCategory.HARMFUL"));
    }

    @Test
    void theHookUsesTheRuleBehindItsOwnSwitch() throws Exception {
        assertTrue(read("BossRulesHooks.java").contains("BossRulesConfig.EFFECT_IMMUNE.get() && BossEnvironment.effectDamage"));
        assertTrue(read("BossRulesConfig.java").contains("\"effectImmune\", true"));
    }
}
