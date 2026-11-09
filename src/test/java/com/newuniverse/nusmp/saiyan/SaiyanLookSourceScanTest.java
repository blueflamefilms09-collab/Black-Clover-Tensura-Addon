package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Saiyan look layer (3.69.0) is client drawing driven by one tiny server hook; nothing can be profiled here, so its bounds, its wiring and its assets are pinned by reading the repo. */
class SaiyanLookSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";
    private static final String RES = "src/main/resources/assets/nusmp/";

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    private static boolean exists(String rel) { return Files.exists(ROOT.resolve(rel)); }

    @Test
    void theLookTableIsCommonCodeWithNoClientClass() throws IOException {
        String t = read(MAIN + "saiyan/SaiyanLook.java");
        assertFalse(t.contains("net.minecraft.client") || t.contains("com.newuniverse.nusmp.client") || t.contains("com.mojang.blaze3d") || t.contains("Minecraft.getInstance"), "pure table");
        assertFalse(t.contains("java.util.Random") || t.contains("Math.random"), "deterministic: the preview and the game agree");
    }

    @Test
    void thePainterAllocatesNothingPerFrameAndIsMeasuredAndCulled() throws IOException {
        String t = read(MAIN + "client/aura/SaiyanAura.java");
        assertFalse(t.contains("Minecraft.getInstance"), "the owner's-camera check goes through RebuildSetKit");
        Matcher m = Pattern.compile("\\bnew\\s+([A-Za-z.]+)").matcher(t);
        while (m.find()) assertTrue(m.group(1).equals("float") || m.group(1).equals("WaterAuraRig.Set"), "no allocation in the frame: new " + m.group(1));
        assertTrue(t.contains("PerfProbe.section(\"c.saiyan.aura\")") && t.contains("PerfProbe.begin()"), "measured");
        assertTrue(t.contains("SaiyanLook.FAR") && t.contains("SaiyanLook.NEAR"), "distance culled and level-of-detail");
        assertTrue(t.contains("MAX_CHIPS") && t.contains("MAX_STREAKS"), "the charge-up loops run to the table's caps");
        assertFalse(t.toLowerCase().contains("hair"), "hair is on hold: nothing of it is drawn");
    }

    @Test
    void everyTextureAndEveryArmourPieceThePainterNamesIsInTheRepo() throws IOException {
        String t = read(MAIN + "client/aura/SaiyanAura.java");
        Matcher tex = Pattern.compile("AuraRender\\.tex\\(\"([a-z_/]+)\"\\)").matcher(t);
        int n = 0;
        while (tex.find()) { n++; assertTrue(exists(RES + "textures/" + tex.group(1) + ".png"), tex.group(1)); }
        assertTrue(n >= 11, "the eleven look textures");
        Matcher piece = Pattern.compile("Piece\\.of\\(\"([a-z_]+)\"").matcher(t);
        int p = 0;
        while (piece.find()) {
            p++;
            String id = "saiyan_arm_" + piece.group(1);
            assertTrue(exists(RES + "geo/entity/" + id + ".geo.json"), id + " model");
            assertTrue(exists(RES + "textures/entity/" + id + ".png"), id + " texture");
            assertTrue(exists(RES + "textures/entity/" + id + "_glow.png"), id + " glow");
        }
        assertTrue(p == 9, "nine armour pieces");
        assertFalse(exists(RES + "textures/particle/saiyan_strand.png"), "the hair strand texture is not shipped while the hair is on hold");
    }

    @Test
    void theServerHookIsOnePacketASecondAndEndsWithTheForm() throws IOException {
        String e = read(MAIN + "saiyan/SaiyanFormEngine.java");
        assertTrue(e.contains("lookOn(p, f);") && e.contains("lookEnd(p, f);"), "starts on activate, ends on deactivate");
        assertTrue(e.contains("if ((p.tickCount % 20) == 0) lookOn(p, f);"), "the lease is renewed once a second, not on every upkeep");
        assertTrue(e.contains("p.connection != null"), "never sent for a stand-in");
        assertTrue(read(MAIN + "saiyan/SaiyanManager.java").contains("SaiyanFormEngine.lookEnd(p, SaiyanFormEngine.active(r));"), "ends when the system is switched off");
        String a = read(MAIN + "aura/Aura.java").trim();
        assertTrue(a.endsWith("SAIYAN\n}") || a.replaceAll("\\s+", "").endsWith("SAIYAN}"), "Aura.SAIYAN is the last slot (the ordinals are networked)");
        assertTrue(read(MAIN + "client/aura/AuraRegistry.java").contains("SaiyanAura.register();"), "registered");
        assertTrue(read(MAIN + "NUSMP.java").contains("SaiyanLookClientConfig.init(modEventBus, modContainer)"), "the client config is registered");
    }

    @Test
    void theSmokeListHasTheNewPrefixAndTierStaysLast() throws IOException {
        String s = read(".github/workflows/smoke.yml");
        assertTrue(s.contains("saiyan.look"), "saiyan.look is expected");
        assertTrue(s.contains(",tier\""), "tier stays last");
        assertTrue(read(MAIN + "saiyan/SaiyanBoot.java").contains("SaiyanLookSelfTests.register();"));
    }
}
