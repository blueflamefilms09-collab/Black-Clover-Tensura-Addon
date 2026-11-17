package com.newuniverse.nusmp.arc.goku;

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
 * Guards the rules the Goku boss was built under (CLAUDE.md rules 9 and 11), by reading its sources: nothing client-only in common code, no unbounded work (no entity spawns, no world
 * scans beyond the one rate-limited player query, no direct block writes, no explosions), the rate limit of the strength reading, the door check in the spawner, and the files that wire it in.
 */
class GokuBossSourceScanTest {
    private static final Path PKG = Path.of("src/main/java/com/newuniverse/nusmp/arc/goku");

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
    void noUnboundedWorkNoEntitySpawnsNoDirectBlockWrites() throws IOException {
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
    void theOnlyWorldQueryForPlayersIsTheRateLimitedScan() throws IOException {
        String boss = code(PKG.resolve("GokuBoss.java"));
        assertEquals(1, boss.split("getEntitiesOfClass\\(Player\\.class", -1).length - 1, "one player query, in scan()");
        assertTrue(boss.contains("nextScan = tickCount + GokuBossMath.RESCAN_TICKS"), "the scan reschedules itself every RESCAN_TICKS");
        assertTrue(boss.contains("if (tickCount >= nextScan) scan(sl);"), "the scan runs only when it is due");
        assertTrue(boss.contains("if (n >= SCAN_MAX) break;"), "the count of players read is capped");
        assertEquals(1, boss.split("TensuraBridge\\.maxEp", -1).length - 1, "the repo's one EP reader, used once per player per scan, no second reader");
        assertFalse(boss.contains("getPlayers(") || boss.contains("players()"), "no walk over every player of the server");
    }

    @Test
    void particlesGoThroughTheSharedBudgetOnly() throws IOException {
        String fx = code(PKG.resolve("GokuFx.java"));
        assertTrue(fx.contains("PerfBudgets"), "the particles are budgeted");
        for (Path p : sources()) {
            if (p.getFileName().toString().equals("GokuFx.java")) continue;
            assertFalse(code(p).contains("sendParticles("), p.getFileName() + " sends particles outside GokuFx");
        }
    }

    @Test
    void everyDoorChecksTheEnabledSettingAndTheGlobalCap() throws IOException {
        String spawner = read("src/main/java/com/newuniverse/nusmp/arc/ArcSpawner.java");
        assertTrue(spawner.contains("GokuBosses.owns(c.id())") && spawner.contains("GokuBosses.gate(sl, at)"), "ArcSpawner.summon asks the Goku gate (command, egg, arena)");
        String gate = code(PKG.resolve("GokuBosses.java"));
        assertTrue(gate.contains("GokuBossConfig.enabled()") && gate.contains("GokuBossConfig.globalCap()"), "the gate reads both settings");
        String cmd = code(PKG.resolve("GokuBossCommand.java"));
        assertTrue(cmd.contains("hasPermission(2)"), "the command is for operators");
        assertTrue(cmd.contains("ArcSpawner.summon"), "the command goes through the same summoning");
    }

    @Test
    void thePlayerCleanupAndTheSettingsFileAreWiredIn() throws IOException {
        String sys = read("src/main/java/com/newuniverse/nusmp/arc/ArcSystem.java");
        assertTrue(sys.contains("GokuBossConfig.SPEC, \"nusmp-goku-boss-server.toml\""), "the server settings file");
        assertTrue(sys.contains("GokuBossEvents.register("), "the cleanup events");
        assertTrue(sys.contains("GokuBossSelfTests.register()"), "the self-tests");
        String ev = code(PKG.resolve("GokuBossEvents.java"));
        for (String e : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(e), "cleanup on " + e);
        assertTrue(read("src/main/java/com/newuniverse/nusmp/arc/ArcCharacters.java").contains("GokuBosses.registerAll()"), "the roster entry");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java").contains("GokuBossCommand.node()"), "the command node");
    }

    @Test
    void theSelfTestPrefixIsInTheSmokeListBeforeScalingAndTheTailIsKept() throws IOException {
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains(",goku.boss,scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "goku.boss sits before scaling and the tail stays");
        String tests = code(PKG.resolve("GokuBossSelfTests.java"));
        for (String key : new String[]{"goku.boss.data", "goku.boss.spawn", "goku.boss.cap"}) assertTrue(tests.contains("\"" + key + "\""), key);
    }

    @Test
    void theNameIsInTheLanguageFile() throws IOException {
        assertTrue(read("src/main/resources/assets/anime_multiverse/lang/en_us.json").contains("\"entity.anime_multiverse.goku_boss\": \"Son Goku\""));
    }

    @Test
    void theDocExistsAndNamesTheSettingsAndTheFormula() throws IOException {
        String doc = read("docs/goku_boss.md");
        for (String must : new String[]{"nusmp-goku-boss-server.toml", "epScaling", "epWeight", "epCap", "perPlayerBonus", "healthMultiplier", "damageMultiplier", "blockDamage", "difficulty",
                "goku.boss", "NEW", "REPLACED", "KEPT", "worst case"}) {
            assertTrue(doc.contains(must), "docs/goku_boss.md does not mention " + must);
        }
    }
}
