package com.newuniverse.nusmp.arc.magi;

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
 * Guards the rules the Sinbad boss was built under (CLAUDE.md rules 9, 11 and 12), by reading its sources: nothing client-only in common code, no unbounded work (no entity spawns, no world
 * scans of the server's players, no direct block writes, no explosions), every particle and effect through the shared senders ({@code Tells}, {@code VfxSpawn}), the door check in the
 * spawner, and the files that wire him in.
 */
class SinbadBossSourceScanTest {
    private static final Path PKG = Path.of("src/main/java/com/newuniverse/nusmp/arc/magi");

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
            for (String banned : new String[]{"addFreshEntity", "getAllEntities", "getChunk(", "forceChunk", "setBlock(", "destroyBlock(", "removeBlock(", ".explode(", "Thread.sleep", "new Thread",
                    "getEntitiesOfClass(", "players()", "getPlayers("}) {
                if (c.contains(banned)) bad.add(p.getFileName() + " calls " + banned);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyParticleAndEffectGoesThroughTheSharedSenders() throws IOException {
        for (Path p : sources()) {
            String c = code(p);
            for (String banned : new String[]{"sendParticles(", "ParticleTypes", "DustParticleOptions", "addParticle(", "PacketDistributor"}) {
                assertFalse(c.contains(banned), p.getFileName() + " sends particles or packets itself (" + banned + "): use Tells, VfxSpawn or SdsaStrikes.fx");
            }
        }
        String moves = code(PKG.resolve("SinbadMoves.java"));
        assertTrue(moves.contains("VfxSpawn.send(") && moves.contains("Tells.ring("), "the moves use the shared senders");
    }

    @Test
    void everyHitGoesThroughTheBossDamageLaw() throws IOException {
        for (String f : new String[]{"SinbadMoves.java", "SinbadBoss.java"}) {
            String c = code(PKG.resolve(f));
            for (String banned : new String[]{".hurt(", "setHealth(", ".heal(", "kill()", "BossHealGuard"}) {
                assertFalse(c.contains(banned), f + " damages or heals outside boss.strike / the framework (" + banned + ")");
            }
        }
        assertTrue(code(PKG.resolve("SinbadMoves.java")).contains("b.strike("), "the moves hit through boss.strike");
    }

    @Test
    void theSkillIdsOfTheDraftAreNotReferenced() throws IOException {
        for (Path p : sources()) {
            String c = code(p);
            assertFalse(c.contains("lightning_domination") || c.contains("wind_domination") || c.contains("tensura:"), p.getFileName() + " names a Tensura skill id (the Charter was left out)");
        }
    }

    @Test
    void everyDoorChecksTheEnabledSettingAndTheGlobalCap() throws IOException {
        String spawner = read("src/main/java/com/newuniverse/nusmp/arc/ArcSpawner.java");
        assertTrue(spawner.contains("SinbadBosses.owns(c.id())") && spawner.contains("SinbadBosses.gate(sl, at)"), "ArcSpawner.summon asks the Sinbad gate (command, egg, arena)");
        String gate = code(PKG.resolve("SinbadBosses.java"));
        assertTrue(gate.contains("SinbadBossConfig.enabled()") && gate.contains("SinbadBossConfig.globalCap()"), "the gate reads both settings");
        String cmd = code(PKG.resolve("SinbadBossCommand.java"));
        assertTrue(cmd.contains("hasPermission(2)"), "the command is for operators");
        assertTrue(cmd.contains("ArcSpawner.summon"), "the command goes through the same summoning");
    }

    @Test
    void thePlayerCleanupAndTheSettingsFileAreWiredIn() throws IOException {
        String sys = read("src/main/java/com/newuniverse/nusmp/arc/ArcSystem.java");
        assertTrue(sys.contains("SinbadBossConfig.SPEC, \"nusmp-sinbad-boss-server.toml\""), "the server settings file");
        assertTrue(sys.contains("SinbadBossEvents.register("), "the cleanup events");
        assertTrue(sys.contains("SinbadBossSelfTests.register()"), "the self-tests");
        String ev = code(PKG.resolve("SinbadBossEvents.java"));
        for (String e : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(e), "cleanup on " + e);
        assertTrue(read("src/main/java/com/newuniverse/nusmp/arc/ArcCharacters.java").contains("SinbadBosses.registerAll()"), "the roster entry");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java").contains("SinbadBossCommand.node()"), "the command node");
    }

    @Test
    void theFightEndAndRemovalCleanUp() throws IOException {
        String boss = code(PKG.resolve("SinbadBoss.java"));
        assertTrue(boss.contains("LIVE.remove(this)"), "removal leaves the live set");
        assertTrue(boss.contains("lockedSpots.clear()"), "the locked spots are dropped when a fight ends");
        assertTrue(boss.contains("super.onFightEnd(won)"), "the framework's end of fight (and the story credit) still runs");
    }

    @Test
    void theSelfTestKeysAreTheOnesTheDocNames() throws IOException {
        String tests = code(PKG.resolve("SinbadBossSelfTests.java"));
        for (String key : new String[]{"sinbad.boss.data", "sinbad.boss.spawn", "sinbad.boss.cap"}) assertTrue(tests.contains("\"" + key + "\""), key);
    }

    @Test
    void theNameIsInTheLanguageFile() throws IOException {
        assertTrue(read("src/main/resources/assets/anime_multiverse/lang/en_us.json").contains("\"entity.anime_multiverse.sinbad_boss\": \"Sinbad\""));
        String frag = read("tools/lang/sinbad_boss.json").trim();
        assertTrue(frag.startsWith("{") && frag.endsWith("}") && frag.indexOf('{', 1) < 0, "the fragment is flat json");
        assertTrue(frag.contains("entity.anime_multiverse.sinbad_boss"));
    }

    @Test
    void theDocExistsAndSaysWhatIsNewReplacedKeptAndNotChecked() throws IOException {
        String doc = read("docs/sinbad_boss.md");
        for (String must : new String[]{"nusmp-sinbad-boss-server.toml", "globalCap", "sinbad.boss", "NEW", "REPLACED", "KEPT", "worst case", "Not checked", "Furfur", "Crocell", "Seven Seas Sovereign",
                "lightning_domination", "wind_domination", "BossHealGuard"}) {
            assertTrue(doc.contains(must), "docs/sinbad_boss.md does not mention " + must);
        }
        assertTrue(Files.exists(Path.of("docs/sinbad_boss_lore.md")), "the lore file of the other session is kept next to the doc");
    }
}
