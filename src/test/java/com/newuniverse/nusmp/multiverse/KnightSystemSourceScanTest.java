package com.newuniverse.nusmp.multiverse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnightSystemSourceScanTest {
    private static String read(String p) throws Exception { return Files.readString(Path.of(p)); }

    @Test
    void theHooksAreRegisteredAndTheSelfTestIsInTheSmokeList() throws Exception {
        String mod = read("src/main/java/com/newuniverse/nusmp/NUSMP.java");
        for (String s : new String[]{"MissionBoard::onDeath", "KnightConduct::onDeath", "StarAwards::onTick", "KnightSystemSelfTests.register()"}) assertTrue(mod.contains(s), s);
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains("knights.rules") && smoke.contains("knights.player"));
        assertTrue(smoke.indexOf("knights.rules") < smoke.indexOf(",tier"), "tier must stay last");
    }

    @Test
    void nothingTicksPerPlayerNoStaticPlayerStateAndNoEntitySpawns() throws Exception {
        for (String f : new String[]{"MissionBoard", "KnightConduct", "StarAwards", "MissionBoardRules", "StarAwardRules", "SquadStyle"}) {
            String src = read("src/main/java/com/newuniverse/nusmp/multiverse/" + f + ".java");
            assertFalse(src.contains("PlayerTickEvent") || src.contains("Minecraft.getInstance") || src.contains("addFreshEntity") || src.contains("net.minecraft.client"), f);
            assertFalse(src.contains("static Map<") || src.contains("static final Map<") || src.contains("static HashMap"), f + " keeps player state in a static map");
        }
        String awards = read("src/main/java/com/newuniverse/nusmp/multiverse/StarAwards.java");
        assertTrue(awards.contains("% 200"), "the festival is checked every 10 seconds, not every tick");
    }

    @Test
    void theSettingsAreBoundedAndTheCommandsExist() throws Exception {
        String cfg = read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseConfig.java");
        for (String s : new String[]{"\"perDay\", 5, 1, 100", "\"minutes\", 30, 1, 1440", "\"days\", 28, 1, 3650", "\"villagerBlackStars\"", "\"first\", 5, 0, 1000"}) assertTrue(cfg.contains(s), s);
        String cmd = read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java");
        for (String s : new String[]{"Commands.literal(\"mission\")", "Commands.literal(\"take\")", "Commands.literal(\"abandon\")", "Commands.literal(\"style\")", "Commands.literal(\"awards\").requires(MultiverseCommands::admin)"}) assertTrue(cmd.contains(s), s);
    }
}
