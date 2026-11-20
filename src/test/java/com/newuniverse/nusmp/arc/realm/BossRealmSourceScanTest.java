package com.newuniverse.nusmp.arc.realm;

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
 * Guards the rules the boss realm was built under (CLAUDE.md rules 9 and 11) by reading its sources: nothing client-only in common code, no static current player, no unbounded work (no entity
 * spawns, no block writes, no chunk loads, no world-wide entity scans, no threads), the only world look is the one a door and the once-a-second pass make, and the doors and the referee are wired
 * to it. The realm adds no packet of its own, so there is no sender to validate.
 */
class BossRealmSourceScanTest {
    private static final Path PKG = Path.of("src/main/java/com/newuniverse/nusmp/arc/realm");
    private static final Path ARENA = Path.of("src/main/java/com/newuniverse/nusmp/arc/arena");
    private static final Path ARC = Path.of("src/main/java/com/newuniverse/nusmp/arc");

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
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(PKG)) {
            s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(out::add);
        }
        for (String f : new String[] {"RealmShapes.java", "SevenSeasIsleShape.java", "TournamentGroundsShape.java"}) out.add(ARENA.resolve(f));
        return out;
    }

    @Test
    void nothingClientOnlyAndNoStaticCurrentPlayerInTheCommonCode() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : sources()) {
            String c = code(p);
            for (String banned : new String[] {"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance", "net.neoforged.neoforge.client", "Dist.CLIENT"}) {
                if (c.contains(banned)) bad.add(p.getFileName() + " uses " + banned);
            }
            if (c.matches("(?s).*static\\s+(?!final)[\\w.<>]*\\s*(ServerPlayer|Player)\\s+\\w+\\s*[;=].*")) bad.add(p.getFileName() + " keeps a static player");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noUnboundedWorkNoEntitySpawnsNoBlockWritesNoChunkLoads() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : sources()) {
            if (p.getFileName().toString().endsWith("SelfTests.java")) continue;
            String c = code(p);
            for (String banned : new String[] {"addFreshEntity", "getAllEntities", "getChunk(", "forceChunk", "setBlock(", "destroyBlock(", ".explode(", "Thread.sleep", "new Thread", "getPlayers(", "PacketDistributor"}) {
                if (c.contains(banned)) bad.add(p.getFileName() + " calls " + banned);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theOnlyWorldLooksAreTheDoorsNearbyPlayersAndTheSecondlyPassOverTheRealmDimension() throws IOException {
        String c = code(PKG.resolve("BossRealm.java"));
        assertEquals(1, count(c, "getEntitiesOfClass("), "one bounded box look (the door's companions)");
        assertTrue(count(c, ".players()") <= 3, "the pass, its early exit and the active-sites count look at the players of the realm dimension only");
        assertTrue(c.contains("server.getTickCount() % STEP != 0"), "the pass runs once a second");
        assertTrue(c.contains("MAX_DOOR_ENTRIES") && c.contains("DOORS.remove("), "the door table is capped and forgotten at logout");
        assertTrue(c.contains("CLOCKS.clear()") && c.contains("ServerStoppedEvent"), "everything is dropped on server stop");
        assertTrue(c.contains("BossRealmConfig.enabled()"), "the setting turns it off");
        assertTrue(c.contains("catch (RuntimeException"), "a failing pass never leaves an event");
    }

    @Test
    void everyDoorAsksTheRealmAndTheRefereeAsksForTheGrace() throws IOException {
        assertTrue(code(ARC.resolve("ArcSpawnEggItem.java")).contains("BossRealm.open("), "the spawn egg");
        assertTrue(code(ARC.resolve("ArcCommand.java")).contains("BossRealm.open("), "/multiverse arc summon");
        assertTrue(code(ARC.resolve("goku/GokuBossCommand.java")).contains("BossRealm.open("), "/multiverse goku_boss spawn");
        assertTrue(code(ARC.resolve("magi/SinbadBossCommand.java")).contains("BossRealm.open("), "/multiverse sinbad_boss spawn");
        assertTrue(code(ARENA.resolve("ArcArenaDirector.java")).contains("BossRealm.presentWithGrace("), "the referee");
        String sys = code(ARC.resolve("ArcSystem.java"));
        assertTrue(sys.contains("BossRealmConfig.SPEC") && sys.contains("BossRealm.register(") && sys.contains("BossRealmSelfTests.register()"), "wired in ArcSystem.init");
        String shapes = code(ARENA.resolve("ArenaShapes.java"));
        assertTrue(shapes.contains("RealmShapes.forCharacter(c)"), "the arena registry asks the realm's shapes");
    }

    @Test
    void theSettingsAreServerSettingsWithHardRanges() throws IOException {
        String c = code(PKG.resolve("BossRealmConfig.java"));
        assertFalse(c.contains("Type.CLIENT"));
        assertEquals(7, count(c, "defineInRange("), "every number has a range");
        assertTrue(c.contains(".define(\"enabled\", true)"), "on by default");
        assertTrue(code(ARC.resolve("ArcSystem.java")).contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.arc.realm.BossRealmConfig.SPEC"), "registered as a SERVER config");
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }
}
