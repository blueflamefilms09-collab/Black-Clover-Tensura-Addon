package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the three Leon study fixes (docs/boss_leon_scaling.md, docs/boss_data_files.md) must stay cheap, server-side and switchable (CLAUDE.md rules 9, 11, 12), and nothing here can be profiled, so what keeps them so is pinned by
 * reading the source: the pure classes are pure, the settings are SERVER keys with the stated defaults, the party scale is read once at fight start (not per tick), Lucius and the Tier List opt out, the data loader is a reload listener with
 * no tick, scan or packet, and the self-test keys are wired. Pure (file reads); the working directory is the project root.
 */
class LeonFixesSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    private static String code(String rel) throws IOException {
        return read(rel).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static void common(String rel) throws IOException {
        String c = code(MAIN + rel);
        assertFalse(c.contains("net.minecraft.client") || c.contains("com.newuniverse.nusmp.client") || c.contains("Minecraft.getInstance") || c.contains("com.mojang.blaze3d"), rel + " is common code");
        assertFalse(c.contains("ServerTickEvent") || c.contains("PlayerTickEvent") || c.contains("getEntitiesOfClass") || c.contains("addFreshEntity") || c.contains("PacketDistributor"),
                rel + " has no tick, scan, spawn or packet");
    }

    @Test
    void thePureClassesArePure() throws IOException {
        for (String f : new String[]{"boss/data/BossDataTable.java", "boss/god/ArenaScaling.java", "boss/budget/BudgetPolicy.java"}) {
            String c = code(MAIN + f);
            assertFalse(c.contains("net.minecraft") || c.contains("net.neoforged"), f + " is pure");
        }
    }

    @Test
    void fix1TheCapReadsTheBaseHealthBehindAServerKey() throws IOException {
        String cfg = code(MAIN + "boss/budget/BossBudgetConfig.java");
        assertTrue(cfg.contains("\"dpsCapUsesBaseHealth\", true"), "default on");
        assertTrue(cfg.contains("USE_BASE_HEALTH") && cfg.contains("baseHealth()"));
        String b = code(MAIN + "boss/budget/BossBudget.java");
        assertTrue(b.contains("BudgetPolicy.capBasis(") && b.contains("BossStatBuff.healthFactor(e)") && b.contains("BossBudgetConfig.baseHealth()"), "the cap is taken through the basis");
        assertTrue(code(MAIN + "boss/BossStatBuff.java").contains("HEALTH_ID") && code(MAIN + "boss/BossStatBuff.java").contains("public static double healthFactor"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("BossBudgetConfig.SPEC, \"nusmp-boss-budget-server.toml\""), "a SERVER config (already registered)");
    }

    @Test
    void fix5ThePartyScaleIsReadOnceAtFightStartAndLuciusAndTheTierListOptOut() throws IOException {
        String cfg = code(MAIN + "boss/rules/BossRulesConfig.java");
        assertTrue(cfg.contains("\"partyScaling\", true") && cfg.contains("partyScaling()"), "default on");
        String z = code(MAIN + "entity/zogratis/ZogratisBoss.java");
        int start = z.indexOf("private void startFight(");
        assertTrue(start > 0 && z.substring(start, z.indexOf("onFightStart();", start)).contains("applyPartyScale(partyCount(sl))"), "read at fight start");
        assertTrue(z.substring(z.indexOf("private void resetFight(")).contains("clearPartyScale()"), "an abandoned fight takes it off");
        assertTrue(z.contains("damageScale() * partyDamage") && z.indexOf("damageScale() * partyDamage") != z.lastIndexOf("damageScale() * partyDamage"), "bossDamage and strike both use it");
        assertTrue(z.contains("ArenaScaling.healthCombined(") && z.contains("ArenaScaling.damageCombined("), "one combined cap");
        assertTrue(z.contains("BossRulesConfig.partyScaling()") && z.contains("protected boolean partyScaled()"));
        assertTrue(code(MAIN + "entity/zogratis/LuciusBoss.java").contains("boolean partyScaled() { return false; }"), "Lucius keeps his own scaling");
        assertTrue(code(MAIN + "arc/tier/TierBoss.java").contains("boolean partyScaled() { return false; }"), "the Tier List rows keep their numbers");
        assertTrue(code(MAIN + "entity/zogratis/LuciusGod.java").contains("ArenaScaling.healthModifier(n)"), "Lucius's own use is untouched");
    }

    @Test
    void fix9TheLoaderIsAReloadListenerWithNoTickAndDefaultsOn() throws IOException {
        common("boss/data/BossData.java");
        common("boss/data/BossDataTable.java");
        String d = code(MAIN + "boss/data/BossData.java");
        assertTrue(d.contains("extends SimpleJsonResourceReloadListener") && d.contains("\"arc_bosses\"") && d.contains("Map.copyOf"), "one immutable map swapped in per reload");
        assertTrue(d.contains("BossRulesConfig.bossData()") && d.contains("EntityJoinLevelEvent") && d.contains("isClientSide()"), "server side, behind the key");
        assertTrue(d.contains("MAX_LOGGED"), "bounded logging");
        assertTrue(code(MAIN + "boss/rules/BossRulesConfig.java").contains("\"bossData\", true"));
        String hooks = code(MAIN + "boss/BossHooks.java");
        assertTrue(hooks.contains("BossData::onJoin") && hooks.contains("AddReloadListenerEvent") && hooks.contains("new com.newuniverse.nusmp.boss.data.BossData()"));
    }

    @Test
    void theSelfTestKeysAreRegistered() throws IOException {
        String t = code(MAIN + "boss/BossLeonSelfTests.java");
        assertTrue(t.contains("\"boss.leon1\"") && t.contains("\"boss.leon3\""));
        assertTrue(code(MAIN + "boss/BossHooks.java").contains("BossLeonSelfTests.register()"));
        common("boss/BossLeonSelfTests.java");
    }

    @Test
    void theModShipsNoBossDataFile() {
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/data/anime_multiverse/arc_bosses")) && ROOT.resolve("src/main/resources/data/anime_multiverse/arc_bosses").toFile().list().length > 0);
    }
}
