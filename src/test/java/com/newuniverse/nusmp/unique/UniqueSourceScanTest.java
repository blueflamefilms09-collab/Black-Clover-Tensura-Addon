package com.newuniverse.nusmp.unique;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Unique Guardian has to stay cheap and server safe, and nothing can be profiled here, so the rules are pinned by reading the source (comments removed, so a word in a comment never counts): the pure state machine
 * imports no game class, nothing in the package names a client class, no handler scans a world or loads a chunk, and every handler is gated by a stagger, a modulo or a budget. If one of these is replaced by something
 * better on purpose, change the pin in the same commit and say why.
 */
class UniqueSourceScanTest {
    private static final Path DIR = Path.of("src/main/java/com/newuniverse/nusmp/unique");
    private static final List<String> PURE = List.of("UniqueState", "UniqueSpot", "UniqueRecord", "UniqueRules", "UniqueData");

    private static String code(Path p) throws IOException {
        String s = Files.readString(p);
        s = s.replaceAll("(?s)/\\*.*?\\*/", " ");
        return s.replaceAll("(?m)//.*$", " ");
    }

    private static List<Path> files() throws IOException {
        try (Stream<Path> w = Files.walk(DIR)) {
            return w.filter(f -> f.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void pureClassesImportNoGameClass() throws IOException {
        for (String n : PURE) {
            String c = code(DIR.resolve(n + ".java"));
            assertFalse(c.contains("net.minecraft") || c.contains("neoforged") || c.contains("com.mojang"), n + " must stay pure (no game imports)");
        }
    }

    @Test
    void noClientClassAndNoStaticCurrentPlayer() throws IOException {
        List<Path> all = files();
        assertTrue(all.size() >= 15, "the package is there: " + all.size());
        for (Path f : all) {
            String c = code(f);
            for (String bad : List.of("net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance")) {
                assertFalse(c.contains(bad), f.getFileName() + " names a client class: " + bad);
            }
        }
    }

    @Test
    void noWorldScanAndNoChunkLoad() throws IOException {
        for (Path f : files()) {
            String c = code(f);
            for (String bad : List.of(".getChunk(", ".getEntities(", ".getAllEntities(", "getEntitiesOfClass(", ".getEntityGetter(", "forceLoad", "setChunkForced", "getChunkAt(")) {
                assertFalse(c.contains(bad), f.getFileName() + " scans or loads the world: " + bad);
            }
        }
    }

    @Test
    void everyHandlerIsGatedAndGuarded() throws IOException {
        String ev = code(DIR.resolve("UniqueEvents.java"));
        assertTrue(ev.contains("% 20 != 0"), "the player tick is staggered to once a second");
        assertEquals(7, count(ev, "SafeCast.tick("), "every game handler runs inside SafeCast.tick");
        String g = code(DIR.resolve("UniqueGuardian.java"));
        assertTrue(g.contains("t % 4 == 0") && g.contains("t % 20 == 7") && g.contains("t % 1200 == 601"), "the server tick passes are modulo gated");
        assertTrue(g.contains("RESCUE_BUDGET"), "the void rescue has a budget");
        assertTrue(g.contains("getChunkNow"), "a container is only looked at in a loaded chunk");
        String boot = code(DIR.resolve("UniqueBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER"), "the settings change gameplay, so they are server config");
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }
}
