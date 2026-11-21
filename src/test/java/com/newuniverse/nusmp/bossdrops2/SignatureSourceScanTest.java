package com.newuniverse.nusmp.bossdrops2;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/boss_drops_worthwhile.md, rules 9 and 11): the worthwhile boss drops are applied from OUTSIDE the boss classes, work on a dedicated server and do no per-tick work. This pins it by reading the
 * sources: no boss class (the Zogratis family, Goku, Zagred, the arc bosses, anything) mentions the package; the package has no client class, no tick event, no catch of Throwable, no static "current player";
 * the only code outside the package that knows it is the three one-line appends (the mod boot, the creative tab and the command tree).
 */
class SignatureSourceScanTest {
    private static final Path SRC = Path.of("src/main/java");
    private static final Path PKG = SRC.resolve("com/newuniverse/nusmp/bossdrops2");
    /** The only files outside the package that may name it: one appended line each. */
    private static final Set<String> APPENDS = Set.of("NUSMP.java", "NUCreativeTab.java", "MultiverseCommands.java");

    private static List<Path> java(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void noBossClassIsTouchedAndOnlyThreeFilesOutsideThePackageNameIt() throws IOException {
        List<String> users = new ArrayList<>();
        for (Path p : java(SRC)) {
            if (p.startsWith(PKG)) continue;
            String text = Files.readString(p);
            if (text.contains("bossdrops2")) users.add(p.getFileName().toString());
        }
        assertEquals(APPENDS, Set.copyOf(users), "only the boot, the creative tab and the command tree may name the package; a boss class must not");
        for (Path p : java(SRC)) {
            String n = p.getFileName().toString();
            if (n.equals("GokuBoss.java") || n.equals("ZogratisBoss.java") || n.equals("ZagredBoss.java")) {
                String text = Files.readString(p);
                assertFalse(text.contains("bossdrops2") || text.contains("SignatureEvents") || text.contains("DamageLedger"), n + " must stay untouched");
            }
        }
    }

    @Test
    void theAppendedLinesAreSingleLines() throws IOException {
        for (String name : APPENDS) {
            Path f = java(SRC).stream().filter(p -> p.getFileName().toString().equals(name)).findFirst().orElseThrow();
            long lines = Files.readAllLines(f).stream().filter(l -> l.contains("bossdrops2")).count();
            assertEquals(1, lines, name + " names the package on exactly one line");
        }
    }

    @Test
    void thePackageHasNoClientClassNoTickEventAndNoStaticCurrentPlayer() throws IOException {
        List<Path> files = java(PKG);
        assertTrue(files.size() >= 13, "the package is there");
        for (Path p : files) {
            String t = Files.readString(p);
            String n = p.getFileName().toString();
            for (String bad : List.of("net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance", "net.neoforged.api.distmarker", "Dist.CLIENT")) {
                assertFalse(t.contains(bad), n + " uses a client-only thing: " + bad);
            }
            for (String tick : List.of("TickEvent", "ServerTickEvent", "LevelTickEvent", "EntityTickEvent", "PlayerTickEvent", "ClientTickEvent", "RenderLevelStageEvent", "RenderGuiEvent")) {
                assertFalse(t.contains(tick), n + " listens to a per-tick or per-frame event: " + tick);
            }
            assertFalse(t.contains("catch (Throwable"), n + " catches Throwable");
            assertFalse(java.util.regex.Pattern.compile("static\\s+(volatile\\s+)?(final\\s+)?(Server)?Player\\s+\\w+\\s*[;=]").matcher(t).find(), n + " keeps a static current player");
            assertFalse(t.contains("java.util.concurrent.Executors") || t.contains("new Thread("), n + " starts its own threads");
        }
    }

    @Test
    void onlyTheThreeEventsAreListenedTo() throws IOException {
        String t = Files.readString(PKG.resolve("SignatureEvents.java"));
        assertTrue(t.contains("LivingDamageEvent.Post") && t.contains("LivingDeathEvent") && t.contains("ServerStoppedEvent"));
        int listeners = t.split("addListener", -1).length - 1;
        assertEquals(3, listeners, "exactly the damage, death and server-stopped listeners");
    }

    @Test
    void theOnlySettingIsTheWorthwhileSwitchAndItDefaultsToTrue() throws IOException {
        String t = Files.readString(PKG.resolve("SignatureConfig.java"));
        assertEquals(1, t.split("\\.define", -1).length - 1, "one setting");
        assertTrue(t.contains("define(\"worthwhile\", true)"));
        assertTrue(t.contains("b.push(\"bossDrops\")"));
        assertTrue(Files.readString(PKG.resolve("BossSigBoot.java")).contains("\"nusmp-bossdrops2-server.toml\""));
        assertTrue(Files.readString(PKG.resolve("BossSigBoot.java")).contains("ModConfig.Type.SERVER"), "gameplay setting: a SERVER config");
    }

    @Test
    void everyHookChecksTheSettingFirst() throws IOException {
        String t = Files.readString(PKG.resolve("SignatureEvents.java"));
        int dmg = t.indexOf("static void onDamage");
        int death = t.indexOf("static void onDeath");
        assertTrue(t.indexOf("SignatureConfig.worthwhile()", dmg) > 0 && t.indexOf("SignatureConfig.worthwhile()", dmg) < t.indexOf("LivingEntity victim", dmg));
        assertTrue(t.indexOf("SignatureConfig.worthwhile()", death) > 0 && t.indexOf("SignatureConfig.worthwhile()", death) < t.indexOf("tableFor(dead)", death));
    }

    @Test
    void theOneOfOneGoesThroughTheRelicLedgerNotANewStore() throws IOException {
        String t = Files.readString(PKG.resolve("SignatureEvents.java"));
        assertTrue(t.contains("UniqueLedger.issue("), "the world-unique authority is the God of Order's ledger");
        for (Path p : java(PKG)) assertFalse(Files.readString(p).contains("extends SavedData"), p.getFileName() + ": no second saved store for the uniques");
    }
}
