package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the final Wind A cannot be profiled here, so its rules are pinned by reading the real source. (1) Server safety: the common classes (rules, self-test) name no client class and no Minecraft class
 * but the server one in the self-test; the cast clock keeps no UUID table, no static current player, nothing is sent. (2) Frame cost: the 3D crown painter allocates nothing per frame (every {@code new}
 * is a static initialiser), builds no string and resolves its meshes once; the quiet state is decided by one function. (3) The calm option is a CLIENT config (looks only), default true. (4) The self-test is
 * registered and the older Winds are still reachable.
 */
class SpiritDiveWind4ScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    @Test
    void theCommonClassesAreServerSafeAndKeepNoPlayerState() {
        String rules = code(read("book/SpiritDiveWind4Rules.java"));
        assertFalse(CLIENT.matcher(rules).find() || CLIENT.matcher(code(read("book/SpiritDiveWind4SelfTests.java"))).find(), "a client class in common code");
        assertFalse(rules.contains("net.minecraft"), "the rules stay pure");
        assertFalse(rules.contains("UUID") || rules.contains("HashMap") || rules.contains("ConcurrentHashMap"), "the cast clock is an array of 16 slots, no map of players");
        assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|AbstractClientPlayer|LivingEntity)\\s+\\w+\\s*[;=]").matcher(rules).find(), "a static current player");
        assertFalse(rules.contains("CustomPacketPayload") || rules.contains("PacketDistributor") || rules.contains("ServerLevel"), "nothing is sent or decided");
    }

    @Test
    void theCrownPainterAllocatesNothingPerFrameAndResolvesItsMeshesOnce() {
        String c = code(read("client/aura/SpiritDiveWind4.java"));
        for (String line : c.split("\n")) {
            if (!line.contains("new ")) continue;
            assertTrue(line.contains("static final"), "an allocation outside a static initialiser: " + line.trim());
        }
        assertFalse(c.contains("String.format") || c.contains("new ResourceLocation") || c.contains("\" + ") || c.contains("Minecraft.getInstance") || c.contains("getCameraEntity"), "no string, no camera");
        assertTrue(c.contains("CROWN_YUNO = SpiritDiveWind4Rules.crown(true, false)"), "the meshes are resolved once");
        assertTrue(c.contains("SpiritDiveWind4Rules.CastClock CLOCK") && c.contains("SafeAdditive.additive(SOFT)"), "cast clock, render types through SafeAdditive");
        assertFalse(c.contains("SpiritDiveWind3Mesh.getIn(") || c.contains("SpiritDiveWind3Mesh.get("), "no mesh lookup (it builds a key) inside a frame method");
    }

    @Test
    void theFinalWindAIsWiredIntoThePackLookAndOnlyForDesignA() {
        String w = code(read("client/aura/SpiritDiveWind.java"));
        assertTrue(w.contains("real3d ? SpiritDiveWind4.energy(c, casting) : 1f"), "the energy exists only for design A");
        assertTrue(w.contains("if (real3d && lod == Lod.FULL) SpiritDiveWind4.crown("), "the 3D crown at full detail only; the sprite crown at mid distance");
        assertTrue(w.contains("SpiritDiveWind4Rules.calmSpec(sp)"), "the quiet set");
        assertTrue(w.contains("firstPersonSelf(c)") && w.contains("!self1p && SpiritDiveReelRules.crowned(el, st)"), "hidden in first person as before");
        String cfg = code(read("client/aura/DiveClientConfig.java"));
        assertTrue(cfg.contains(".define(\"calm\", true)") && cfg.contains("SpiritDiveLook.setCalm(CALM.get())"), "spiritDive.calm is a client option, default true");
        assertTrue(code(read("client/aura/SpiritDiveLook.java")).contains("particles = true, calm = true"), "default true before the config loads");
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveWind4SelfTests.register()"));
        assertTrue(read("book/SpiritDiveWind4SelfTests.java").contains("\"magic.spirit_dive_windfinal\""));
        assertTrue(code(read("client/aura/SpiritDiveReel.java")).contains("SpiritDiveWind.paint(c, st, grade, pulse)"), "PACK still draws the old Wind unchanged");
    }
}
