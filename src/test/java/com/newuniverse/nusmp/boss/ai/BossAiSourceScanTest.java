package com.newuniverse.nusmp.boss.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the boss AI engage rule (docs/boss_ai_engage.md) cannot be watched in a game from here, so what keeps it true is pinned by reading the source: no boss class puts a look-at goal at or
 * above its attack or chase goal, every boss class that builds its own goals adds the engage goal first, the two settings are SERVER keys with the stated defaults, the engage code is common-safe and
 * cheap (one box query behind a ten-tick gate, no tick event, no packet), and the God of Order stays a judge. Pure file reads; the working directory is the project root.
 */
class BossAiSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";
    /** Every class of a boss that registers its own goals (the framework base, the legacy Zagred, the Floor Warden and the Charybdis Spawn; the rest inherit). */
    private static final String[] GOAL_BOSSES = {"entity/zogratis/ZogratisBoss.java", "entity/ZagredBossEntity.java", "labyrinth/seal/FloorWardenEntity.java", "dungeon/floor/charybdis/CharybdisSpawnBoss.java"};

    private static String code(String rel) throws IOException {
        return Files.readString(ROOT.resolve(MAIN + rel), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    @Test
    void noBossLooksBeforeItAttacks() throws IOException {
        Pattern add = Pattern.compile("goalSelector\\.addGoal\\((\\d+),\\s*([^;]*);");
        for (String f : GOAL_BOSSES) {
            String c = code(f);
            int attack = Integer.MAX_VALUE, look = Integer.MIN_VALUE;
            Matcher m = add.matcher(c);
            while (m.find()) {
                String what = m.group(2);
                int prio = Integer.parseInt(m.group(1));
                if (what.contains("MeleeAttackGoal") || what.contains("Positioning") || what.contains("Chase")) attack = Math.min(attack, prio);
                if (what.contains("LookAtPlayerGoal") || what.contains("BossEngageGoal.lookAt")) look = Math.max(look, prio);
            }
            if (attack != Integer.MAX_VALUE && look != Integer.MIN_VALUE) assertTrue(look > attack, f + ": look-at priority " + look + " must be above (a larger number than) the attack priority " + attack);
        }
    }

    @Test
    void everyGoalBuildingBossAddsTheEngageGoalFirstAndLookGoalsStandDown() throws IOException {
        for (String f : GOAL_BOSSES) {
            String c = code(f);
            assertTrue(c.contains("targetSelector.addGoal(0,") && c.contains("BossEngageGoal.create(") || c.contains("attachEngage();"), f + " adds the engage goal at priority 0");
            assertFalse(c.contains("new LookAtPlayerGoal(") || c.contains("new RandomLookAroundGoal("), f + " uses the engage-aware look goals");
        }
        assertTrue(code("entity/zogratis/ZogratisBoss.java").contains("targetSelector.addGoal(0,"), "the framework attaches it");
    }

    @Test
    void theGodOfOrderStaysAJudge() throws IOException {
        String g = code("relics/order/GodOfOrder.java");
        assertTrue(g.contains("engagesOnSight() { return false; }"));
        assertTrue(g.contains("removeAllGoals(g -> g instanceof NearestAttackableTargetGoal)"), "the old no-hunt rule is untouched");
    }

    @Test
    void theSettingsAreServerKeysWithTheStatedDefaults() throws IOException {
        String cfg = code("boss/rules/BossRulesConfig.java");
        assertTrue(cfg.contains("\"aggroOnSight\", true"));
        assertTrue(cfg.contains("defineInRange(\"aggroRange\", 32, 8, 96)"));
        assertTrue(code("NUSMP.java").contains("BossRulesConfig.SPEC, \"nusmp-boss-rules-server.toml\""), "a SERVER config");
    }

    @Test
    void theEngageCodeIsCommonSafeAndCheap() throws IOException {
        String c = code("boss/ai/BossEngageGoal.java");
        assertFalse(c.contains("net.minecraft.client") || c.contains("com.newuniverse.nusmp.client") || c.contains("Minecraft.getInstance") || c.contains("com.mojang.blaze3d"), "common code");
        assertFalse(c.contains("PacketDistributor") || c.contains("ServerTickEvent") || c.contains("addFreshEntity") || c.contains("sendParticles"), "no packet, tick event or spawn");
        assertEquals(1, c.split("getEntitiesOfClass", -1).length - 1, "exactly one box query");
        assertTrue(c.indexOf("BossEngage.due(") < c.indexOf("getEntitiesOfClass"), "the query sits behind the ten-tick gate");
        assertTrue(c.contains("Labyrinth.inSanctuary("), "the Hub is never engaged");
        String p = code("boss/ai/BossEngage.java");
        assertFalse(p.contains("net.minecraft") || p.contains("net.neoforged"), "the chooser is pure");
    }

    @Test
    void theFrameworkHasTheChaseFallbackBehindTheSetting() throws IOException {
        String z = code("entity/zogratis/ZogratisBoss.java");
        assertTrue(z.contains("BossEngage.rush(t, lastCast)") && z.contains("BossRulesConfig.aggroOnSight()"), "rush after four quiet seconds, behind the setting");
        assertTrue(z.contains("getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.0)"), "a straight walk when no path exists");
    }
}
