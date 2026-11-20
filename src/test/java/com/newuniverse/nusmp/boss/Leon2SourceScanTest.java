package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the reactive guard and the attack timeline (docs/boss_leon_combat.md) cannot be profiled or watched here, so what keeps them safe is pinned by reading the source: the two tables are pure
 * (the client reads the same one, rule 9), the guard runs in {@code hurt} AHEAD of the power-step clamp and the budget, the settings are SERVER keys with a range and a switch, nothing scans the
 * world, and the hooks are wired. Pure (file reads); the working directory is the project root.
 */
class Leon2SourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String Z = "src/main/java/com/newuniverse/nusmp/entity/zogratis/";

    private static String read(String rel) throws IOException {
        return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8);
    }

    private static String code(String rel) throws IOException {
        return read(rel).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    @Test
    void theTwoTablesArePureSoTheClientMayReadThem() throws IOException {
        for (String f : new String[]{"ReactiveGuard.java", "AttackTimeline.java"}) {
            String c = code(Z + f);
            assertFalse(c.contains("net.minecraft") || c.contains("net.neoforged") || c.contains("com.mojang") || c.contains("com.newuniverse.nusmp.client"), f + " is pure");
        }
        String pose = code("src/main/java/com/newuniverse/nusmp/client/zogratis/ZogratisPose.java");
        assertTrue(pose.contains("AttackTimeline.clipSeconds"), "the pose clock reads the shared table");
        assertTrue(code("src/main/java/com/newuniverse/nusmp/client/zogratis/ZogratisRenderer.java").contains("swingRow()"));
    }

    @Test
    void theGuardRunsAheadOfTheClampAndTheBudgetAndOnlyOnFoes() throws IOException {
        String c = code(Z + "ZogratisBoss.java");
        int hurt = c.indexOf("public boolean hurt(DamageSource source, float amount)");
        assertTrue(hurt > 0);
        String body = c.substring(hurt, c.indexOf("private void noteFighter"));
        int guard = body.indexOf("guardHurt(source, a)"), clamp = body.indexOf("PowerCurve.clampDamage"), budget = body.indexOf("dpsBudget.hurt(this, source, a, x");
        assertTrue(guard > 0 && guard < clamp && clamp < budget, "guard, then the power-step clamp, then the budget");
        assertTrue(body.indexOf("mitigate(source, a)") < guard, "after the boss's own filters");
        String g = c.substring(c.indexOf("private float guardHurt"), c.indexOf("public AttackTimeline.Swing swingRow"));
        assertTrue(g.contains("BossRulesConfig.reactiveGuard()") && g.contains("isFoe(foe)") && g.contains("BYPASSES_ARMOR"), "switch, foes only, no true damage");
        assertTrue(g.contains("!exposed()") && g.contains("casting == null") && g.contains("!transforming()") && g.contains("refill.active()"), "never while exposed, winding up, transforming or refilling");
        assertFalse(g.contains("heal(") || g.contains("setHealth"), "the guard never heals");
        String leon = c.substring(c.indexOf("private boolean startTimedSwing"), c.indexOf("public static boolean isFamily(Entity e)"));
        assertFalse(leon.contains("getEntitiesOfClass") || leon.contains("foes(") || leon.contains("addFreshEntity") || leon.contains("getChunk"), "no scan, no spawn, no chunk load");
        assertTrue(leon.contains("BossRulesConfig.attackTimeline()"), "the timeline has its switch");
        assertTrue(c.contains("if (startTimedSwing(t)) return;") && c.contains("nextMelee = t + Math.max(16, Math.round(20 *"), "the old fixed cooldown is still there as the fallback");
    }

    @Test
    void theSettingsAreServerKeysWithRangesAndTheOptInIsByCharacterId() throws IOException {
        String cfg = code("src/main/java/com/newuniverse/nusmp/boss/rules/BossRulesConfig.java");
        assertTrue(cfg.contains("\"reactiveGuard\"") && cfg.contains("\"attackTimeline\""));
        assertTrue(cfg.contains("\"guardBlockChance\", 0.70, 0.0, 0.9") && cfg.contains("\"guardDodgeChance\", 0.30, 0.0, 0.9"), "Leon's numbers, never certain");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.boss.rules.BossRulesConfig.SPEC"));
        String guard = code(Z + "ReactiveGuard.java");
        assertTrue(guard.contains("\"meliodas\", \"ban\", \"heath_grice\", \"vetto\", \"mars\", \"udaeus\""));
        assertFalse(guard.contains("\"escanor\""), "Escanor has his own counter");
        String arc = code("src/main/java/com/newuniverse/nusmp/arc/ArcBoss.java");
        assertTrue(arc.contains("ReactiveGuard.OPT_IN.contains(character().id())") && arc.contains("timelineKey() { return character().id(); }"));
    }

    @Test
    void theHooksAreWiredAndTheSelfTestKeysUseThePrefix() throws IOException {
        assertTrue(code("src/main/java/com/newuniverse/nusmp/boss/BossHooks.java").contains("Leon2SelfTests.register()"));
        String t = code("src/main/java/com/newuniverse/nusmp/boss/Leon2SelfTests.java");
        assertTrue(t.contains("\"boss.leon2.tables\"") && t.contains("\"boss.leon2.live\""));
        assertFalse(t.contains("net.minecraft.client") || t.contains("com.newuniverse.nusmp.client"), "common code");
    }
}
