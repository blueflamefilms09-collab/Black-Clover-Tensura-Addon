package com.newuniverse.nusmp.spiritblades;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the two Wind Spirit blades must stay server friendly (CLAUDE.md rule 9) and cheap (rule 11), and nothing can be profiled here, so the pins are read from the source: the common package
 * touches no client class, scans no world, spawns nothing, loads no chunk; every particle call is a bounded one; the client painter skips far weapons and allocates nothing per frame.
 */
class SpiritBladesSourceScanTest {
    private static final Path PKG = Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "spiritblades");
    private static final Path FX = Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "client", "SpiritBladesFx.java");

    private static String strip(String s) {
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    private static List<Path> sources() throws IOException {
        try (Stream<Path> f = Files.list(PKG)) { return f.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void theCommonPackageTouchesNoClientClass() throws IOException {
        for (Path p : sources()) {
            String t = strip(Files.readString(p));
            for (String bad : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance", "net.neoforged.neoforge.client", "Dist.CLIENT", "OnlyIn"})
                assertFalse(t.contains(bad), p.getFileName() + " uses " + bad);
        }
    }

    @Test
    void nothingScansTheWorldSpawnsOrLoadsChunks() throws IOException {
        for (Path p : sources()) {
            if (p.getFileName().toString().endsWith("SelfTests.java")) continue;                // the self-test spawns a stand-in zombie in a test level on purpose
            String t = strip(Files.readString(p));
            for (String bad : new String[]{"getEntitiesOfClass", "getEntities(", "getNearbyEntities", "addFreshEntity", "getChunk(", "forceChunk", "setBlock(", "explode(", "LightningBolt", "getBlockState"})
                assertFalse(t.contains(bad), p.getFileName() + " uses " + bad);
            assertFalse(t.contains("static Player") || t.contains("static ServerPlayer"), p.getFileName() + " keeps a 'current player'");
        }
    }

    @Test
    void everyParticleCallUsesTheCappedCounts() throws IOException {
        int calls = 0;
        Pattern call = Pattern.compile("sendParticles\\(([^;]*?)\\);", Pattern.DOTALL);
        for (Path p : sources()) {
            String t = strip(Files.readString(p));
            Matcher m = call.matcher(t);
            while (m.find()) {
                calls++;
                String args = m.group(1);
                // the count argument is a constant of SpiritBladesRules (minus a small literal) or a small literal
                Matcher n = Pattern.compile("(?:,\\s*)(SpiritBladesRules\\.[A-Z_]+(?:\\s*-\\s*\\d+)?|\\d+)(?:\\s*,\\s*[-\\d.]+\\s*,\\s*[-\\d.]+\\s*,\\s*[-\\d.]+\\s*,\\s*[-\\d.]+\\s*$)").matcher(args);
                assertTrue(n.find(), p.getFileName() + ": cannot read the count of " + args);
                String c = n.group(1);
                if (c.matches("\\d+")) assertTrue(Integer.parseInt(c) <= 3, p.getFileName() + ": count " + c);
                else assertTrue(c.startsWith("SpiritBladesRules.HIT_PARTICLES") || c.startsWith("SpiritBladesRules.TRAIL_PARTICLES"), p.getFileName() + ": count " + c);
            }
        }
        assertEquals(4, calls, "two hit calls and two trail calls: a new particle call needs a new pin");
    }

    @Test
    void theTrailAndTheBurstsAreBudgetedAndCleanedUp() throws IOException {
        String trail = strip(Files.readString(PKG.resolve("SpiritBladesTrail.java")));
        assertTrue(trail.contains("SpiritBladesRules.canTrack("), "the trail is capped");
        assertTrue(trail.contains("TRAIL_PUFFS_PER_TICK"), "puffs are capped per tick");
        assertTrue(trail.contains("isRemoved()"), "a removed arrow ends its trail");
        assertTrue(trail.contains("clearAll"), "the list is cleared on server stop");
        String sword = strip(Files.readString(PKG.resolve("ZephyrSwordItem.java")));
        assertTrue(sword.contains("hitBurstAllowed("), "bursts are throttled per attacker");
        assertTrue(sword.contains("HIT_BURSTS_PER_TICK"), "bursts are capped per tick");
        String boot = strip(Files.readString(PKG.resolve("SpiritBladesBoot.java")));
        assertTrue(boot.contains("PlayerLoggedOutEvent") && boot.contains("ServerStoppingEvent"), "state is dropped on logout and server stop");
        assertTrue(boot.contains("magic.spirit_blades") || Files.readString(PKG.resolve("SpiritBladesSelfTests.java")).contains("\"magic.spirit_blades\""), "the self-test is registered");
    }

    @Test
    void theClientPainterSkipsFarWeaponsAndAllocatesNothingPerFrame() throws IOException {
        String t = strip(Files.readString(FX));
        assertTrue(t.contains("FAR_BLOCKS * FAR_BLOCKS"), "far weapons are skipped");
        int from = t.indexOf("public static void extras("), to = t.indexOf("private static void vert(");
        assertTrue(from > 0 && to > from);
        String body = t.substring(from, to);
        assertFalse(Pattern.compile("\\bnew\\s+[A-Z]").matcher(body).find(), "no allocation in the per-frame methods");
        assertTrue(t.contains("SpiritBladesFxMath.MAX_LEAVES"), "the leaves are capped");
        assertTrue(t.contains("SafeAdditive.additive("), "the additive pass goes through SafeAdditive");
    }
}
