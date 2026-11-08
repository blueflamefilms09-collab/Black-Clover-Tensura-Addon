package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Black Clover boss buff (3.64.0) must stay server friendly and cheap (CLAUDE.md rules 9 and 11) and nothing here can be profiled, so what keeps it so is pinned by reading the source: the math is
 * pure, the glue touches no client class, the health is one permanent multiplied-total modifier with a fixed id (saved with the boss, replaced by a changed setting, never stacked), the heal goes
 * through the heal guard and the budget bypass, the damage goes through the incoming-damage event and never lands on another mod boss, the setting is a SERVER config with a range, it is wired in the one boss hook list, and
 * the self-test key is in the smoke list before {@code tier}. Pure (file reads); the working directory is the project root.
 */
class BossStatSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static String read(String rel) throws IOException {
        return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8);
    }

    private static String code(String rel) throws IOException {
        return read(rel).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static final String DIR = "src/main/java/com/newuniverse/nusmp/boss/";

    @Test
    void theMathIsPureAndTheGlueTouchesNoClientClass() throws IOException {
        String math = code(DIR + "BossStatMath.java");
        assertFalse(math.contains("net.minecraft") || math.contains("net.neoforged"), "the math is pure");
        for (String f : new String[]{"BossStatBuff.java", "BossStatSelfTests.java"}) {
            String c = code(DIR + f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.newuniverse.nusmp.client") || c.contains("Minecraft.getInstance"), f + " is common code");
        }
    }

    @Test
    void theHealthIsOnePermanentFixedIdModifierWrittenThroughTheGuards() throws IOException {
        String c = code(DIR + "BossStatBuff.java");
        assertTrue(c.contains("\"boss_stat/health\"") && c.contains("addOrReplacePermanentModifier") && c.contains("ADD_MULTIPLIED_TOTAL"), "one saved, replaceable modifier");
        assertFalse(c.contains("setBaseValue"), "the base value of a boss is never overwritten");
        assertTrue(c.contains("BossHealGuard.allow") && c.contains("dpsBudget().bypass"), "the heal is a scripted write, not a heal and not damage");
        assertTrue(c.contains("removeModifier(HEALTH_ID)"), "turning the setting to 1 takes it off again");
    }

    @Test
    void theDamageIsOneEventNeverOnAnotherBossAndNothingIsScannedOrSpawned() throws IOException {
        String c = code(DIR + "BossStatBuff.java");
        assertTrue(c.contains("LivingIncomingDamageEvent") && c.contains("BossEntities.isModBoss(victim)"));
        assertTrue(c.contains("BossStatMath.damage("), "the one-blow ceiling is applied");
        assertFalse(c.contains("getEntitiesOfClass") || c.contains("getEntities(") || c.contains("addFreshEntity") || c.contains("ServerTickEvent") || c.contains("PlayerTickEvent"), "no scan, no spawn, no tick");
    }

    @Test
    void theSettingIsAServerConfigWithARangeAndTheHooksAreWired() throws IOException {
        String cfg = code(DIR + "rules/BossRulesConfig.java");
        assertTrue(cfg.contains("\"blackCloverStatMultiplier\", 3.0, 1.0, 10.0"), "default 3, 1 = off, at most 10");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.boss.rules.BossRulesConfig.SPEC"), "a SERVER config");
        String hooks = code(DIR + "BossHooks.java");
        assertTrue(hooks.contains("BossStatBuff::onJoin") && hooks.contains("BossStatBuff::onIncomingDamage") && hooks.contains("BossStatSelfTests.register()"));
        assertTrue(BossStatMath.SOURCE.equals("Black Clover"), "the work the buff follows");
    }

    @Test
    void theSelfTestKeyIsInTheSmokeListBeforeTier() throws IOException {
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains(",boss.stats,tier\""), "boss.stats sits just before the final tier");
        assertTrue(code(DIR + "BossStatSelfTests.java").contains("\"boss.stats\""));
    }
}
