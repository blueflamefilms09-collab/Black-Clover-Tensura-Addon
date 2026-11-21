package com.newuniverse.nusmp.arc.goku;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Guards Goku Omega's build rules (CLAUDE.md 9 and 11) by reading its sources: one switch, server-only, bounded, cleaned up, wired in, documented. */
class GokuOmegaSourceScanTest {
    private static final Path PKG = Path.of("src/main/java/com/newuniverse/nusmp/arc/goku");
    private static final List<String> OMEGA = List.of("GokuOmega.java", "GokuOmegaMath.java", "GokuWorld.java");

    private static String code(Path p) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) continue;
            int c = line.indexOf("//");
            sb.append(c >= 0 ? line.substring(0, c) : line).append('\n');
        }
        return sb.toString();
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    @Test
    void noClientCodeNoSpawnsNoWorldScansNoStaticPlayersInTheNewFiles() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String f : OMEGA) {
            String c = code(PKG.resolve(f));
            for (String banned : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance", "addFreshEntity", "getAllEntities", "getEntitiesOfClass", "getEntities(",
                    "getChunk(", "forceChunk", "setBlock(", "destroyBlock(", ".explode(", "Thread.sleep", "new Thread", "getPlayers(", "players()", "sendParticles", "FoodData", "setFoodLevel", "getFoodData", "causeFoodExhaustion", "MobEffects.HUNGER"}) {
                if (c.contains(banned)) bad.add(f + " uses " + banned);
            }
            if (c.matches("(?s).*static\\s+(?!final)[\\w.<>]*\\s*(ServerPlayer|Player)\\s+\\w+\\s*[;=].*")) bad.add(f + " keeps a static player");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thePerPlayerTableIsBoundedAndKeyedByUuid() throws IOException {
        String c = code(PKG.resolve("GokuOmega.java"));
        assertTrue(c.contains("UUID"), "per-player rows are keyed by UUID");
        assertTrue(c.contains("MAX_PLAYER_ROWS") || c.contains("size() >="), "the per-player map has a hard row cap");
        assertTrue(c.contains("forgetPlayer") && c.contains("void clear("), "rows are dropped on logout/death and everything on boss removal");
    }

    @Test
    void everyEntryPointIsBehindTheSwitch() throws IOException {
        String c = code(PKG.resolve("GokuOmega.java"));
        assertTrue(c.contains("GokuBossConfig.omega()"), "the master switch is read in the brain");
        String w = code(PKG.resolve("GokuWorld.java"));
        assertTrue(w.contains("GokuBossConfig.omega()") || w.contains("GokuBossConfig.omegaTraining()"), "the timer honours the switch");
        String cfg = code(PKG.resolve("GokuBossConfig.java"));
        for (String key : new String[]{"\"goku_boss.omega.enabled\"", "\"enabled\"", "epSkills", "hacks", "secondWind", "healthBonus", "damageBonus", "maxHitPercent", "uiDodgeCap", "levelCap", "trainingXpPerHour", "trainingDailyCap", "fightDailyCap"}) {
            if (key.startsWith("\"goku_boss")) continue;
            assertTrue(cfg.contains(key), "setting " + key);
        }
        assertTrue(cfg.contains("omega"), "the omega section");
    }

    @Test
    void theTimerIsHourlyNotPerTickAndTheCleanupIsWired() throws IOException {
        String ev = code(PKG.resolve("GokuBossEvents.java"));
        assertTrue(ev.contains("ServerTickEvent.Post") && ev.contains("GokuWorld.TIMER_TICKS") && ev.contains("GokuWorld.timer"), "an hourly timer");
        assertTrue(ev.contains("noteKill") && ev.contains("forgetPlayer"), "kills are counted and per-player rows are dropped");
        String boss = code(PKG.resolve("GokuBoss.java"));
        for (String call : new String[]{"omega.clear()", "omega.fightStart()", "omega.fightEnd(won)", "omega.tick(sl)"}) assertTrue(boss.contains(call), call);
    }

    @Test
    void theCommandAndTheSelfTestsAreWired() throws IOException {
        assertTrue(read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java").contains("GokuBossCommand.omegaNode()"), "/multiverse goku level");
        String cmd = code(PKG.resolve("GokuBossCommand.java"));
        assertTrue(cmd.contains("hasPermission(2)") && cmd.contains("\"level\"") && cmd.contains("\"set\""), "op-only level / level set");
        assertTrue(code(PKG.resolve("GokuBossSelfTests.java")).contains("GokuOmegaSelfTests.register()"));
        String tests = code(PKG.resolve("GokuOmegaSelfTests.java"));
        for (String key : new String[]{"gokuomega.data", "gokuomega.world", "gokuomega.spawn"}) assertTrue(tests.contains("\"" + key + "\""), key);
    }

    @Test
    void theSavedRecordIsSmallAndTheLadderOnlyAppendedTheNewForm() throws IOException {
        String w = code(PKG.resolve("GokuWorld.java"));
        assertTrue(w.contains("nusmp_goku_omega") && w.contains("overworld()"), "one world record");
        assertFalse(w.contains("UUID"), "no per-player data in the saved file");
        String l = code(PKG.resolve("GokuLadder.java"));
        assertTrue(l.indexOf("\"arc_goku_ui\"") < l.indexOf("\"arc_goku_mui\""), "the new skin is appended after the old ones");
        assertTrue(Files.exists(Path.of("src/main/resources/assets/anime_multiverse/textures/entity/arc_goku_mui.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/assets/anime_multiverse/textures/entity/arc_goku_mui_glow.png")));
    }

    @Test
    void theDocExistsAndSaysWhatItMustSay() throws IOException {
        String doc = read("docs/goku_omega.md");
        for (String must : new String[]{"goku_boss.omega", "nusmp-goku-boss-server.toml", "/multiverse goku level", "gokuomega", "NEW", "REPLACED", "KEPT", "worst case", "Second Wind", "Blender", "Blockbench", "wiki"}) {
            assertTrue(doc.contains(must), "docs/goku_omega.md does not mention " + must);
        }
    }
}
