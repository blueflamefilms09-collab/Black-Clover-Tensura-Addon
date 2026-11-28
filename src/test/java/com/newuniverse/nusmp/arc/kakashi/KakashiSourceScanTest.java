package com.newuniverse.nusmp.arc.kakashi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the rules the Kakashi boss was built under (CLAUDE.md rules 9 and 11) by reading its sources: nothing client-only in common code, no entity spawns, no block writes, no explosions,
 * every particle through the shared budget, the door checks and the files that wire him in.
 */
class KakashiSourceScanTest {
    private static final Path PKG = Path.of("src/main/java/com/newuniverse/nusmp/arc/kakashi");

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

    private static List<Path> sources() throws IOException {
        try (Stream<Path> s = Files.list(PKG)) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    @Test
    void nothingClientOnlyAndNoStaticCurrentPlayerInTheCommonCode() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : sources()) {
            String c = code(p);
            for (String banned : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance", "net.neoforged.neoforge.client", "Dist.CLIENT"}) {
                if (c.contains(banned)) bad.add(p.getFileName() + " uses " + banned);
            }
            if (c.matches("(?s).*static\\s+(?!final)[\\w.<>]*\\s*(ServerPlayer|Player)\\s+\\w+\\s*[;=].*")) bad.add(p.getFileName() + " keeps a static player");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noEntitySpawnsNoBlockWritesNoExplosionsNoChunkWork() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : sources()) {
            if (p.getFileName().toString().endsWith("SelfTests.java")) continue;
            String c = code(p);
            for (String banned : new String[]{"addFreshEntity", "getAllEntities", "getChunk(", "forceChunk", "setBlock(", "destroyBlock(", "removeBlock(", ".explode(", "Thread.sleep", "new Thread"}) {
                if (c.contains(banned)) bad.add(p.getFileName() + " calls " + banned);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void particlesGoThroughTheSharedBudgetOnly() throws IOException {
        assertTrue(code(PKG.resolve("KakashiFx.java")).contains("PerfBudgets"), "the particles are budgeted");
        for (Path p : sources()) {
            if (p.getFileName().toString().equals("KakashiFx.java")) continue;
            assertFalse(code(p).contains("sendParticles("), p.getFileName() + " sends particles outside KakashiFx");
        }
    }

    @Test
    void everyDoorChecksTheEnabledSettingAndTheGlobalCap() throws IOException {
        String spawner = read("src/main/java/com/newuniverse/nusmp/arc/ArcSpawner.java");
        assertTrue(spawner.contains("KakashiBosses.owns(c.id())") && spawner.contains("KakashiBosses.gate(sl, at)"), "ArcSpawner.summon asks the Kakashi gate (command, egg, arena)");
        String gate = code(PKG.resolve("KakashiBosses.java"));
        assertTrue(gate.contains("KakashiConfig.enabled()") && gate.contains("KakashiConfig.globalCap()"), "the gate reads both settings");
        String cmd = code(PKG.resolve("KakashiCommand.java"));
        assertTrue(cmd.contains("hasPermission(2)") && cmd.contains("ArcSpawner.summon"), "operators only, same summoning");
    }

    @Test
    void theCleanupAndTheSettingsFileAreWiredIn() throws IOException {
        String boot = code(PKG.resolve("KakashiBoot.java"));
        assertTrue(boot.contains("KakashiConfig.SPEC, \"nusmp-kakashi-boss-server.toml\"") && boot.contains("KakashiEvents.register(") && boot.contains("KakashiSelfTests.register()") && boot.contains("KakashiSounds.register("));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/arc/ArcSystem.java").contains("KakashiBoot.init(modBus, modContainer)"));
        String ev = code(PKG.resolve("KakashiEvents.java"));
        for (String e : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(e), "cleanup on " + e);
        assertTrue(read("src/main/java/com/newuniverse/nusmp/arc/ArcCharacters.java").contains("KakashiBosses.registerAll()"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java").contains("KakashiCommand.node()"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/boss/bars/BossBarSources.java").contains("KakashiBarSource(k)"));
    }

    @Test
    void heIsNotADungeonBossAndEveryPerHitCapIsApplied() throws IOException {
        String boss = code(PKG.resolve("KakashiBoss.java"));
        assertFalse(boss.contains(".floors("), "not a dungeon boss: the coordinator connects him later");
        assertTrue(boss.contains("KakashiMath.capHit("), "the per-hit cap");
        assertTrue(boss.contains("KakashiConfig.maxHitPercent()") || boss.contains("maxHitPercent"), "the cap is a setting");
    }

    @Test
    void theSelfTestPrefixIsInTheSmokeListAndTheKeysExist() throws IOException {
        assertTrue(read(".github/workflows/smoke.yml").contains(",kakashi.boss,"));
        String tests = code(PKG.resolve("KakashiSelfTests.java"));
        for (String key : new String[]{"kakashi.boss.data", "kakashi.boss.voice", "kakashi.boss.spawn", "kakashi.boss.moves", "kakashi.boss.kamui", "kakashi.boss.cap", "kakashi.boss.miss", "kakashi.boss.eyerip"}) {
            assertTrue(tests.contains("\"" + key + "\""), key);
        }
    }

    @Test
    void theDocExistsAndNamesTheSettingsTheCostsAndTheLists() throws IOException {
        String doc = read("docs/bosses/kakashi.md");
        for (String must : new String[]{"nusmp-kakashi-boss-server.toml", "healthMultiplier", "damageMultiplier", "maxHitPercent", "globalCap", "kakashi.boss", "NEW", "REPLACED", "KEPT",
                "worst case", "Blender", "not verified", "Kamui", "Susanoo", "kakashi_lines.json"}) {
            assertTrue(doc.contains(must), "docs/bosses/kakashi.md does not mention " + must);
        }
    }
}
