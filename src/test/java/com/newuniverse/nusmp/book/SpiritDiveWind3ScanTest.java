package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Wind remake cannot be profiled here, so its rules are pinned by reading the real source. (1) Server safety: the common classes (rules, geometry, mesh, self-test) name no client class, the pure
 * ones no Minecraft class at all, nothing sends a packet or keeps a player's state, nothing touches the camera. (2) Frame cost: the painter and the mesh drawer allocate nothing per frame (every {@code new} is a
 * static initialiser), build no string, use the level of detail, the character cap per frame and the probe section {@code c.spiritdive.wind3}, and draw every piece from the shared meshes. (3) The older Winds are
 * untouched and still reachable by the option. (4) The self-test, the generator and the tools are registered where they have to be.
 */
class SpiritDiveWind3ScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String readRoot(String rel) {
        try { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");
    private static final String[] COMMON = {"book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java", "book/SpiritDiveWind3SelfTests.java"};

    @Test
    void theCommonClassesNameNoClientClassAndThePureOnesNoMinecraftClass() {
        List<String> bad = new ArrayList<>();
        for (String f : COMMON) if (CLIENT.matcher(code(read(f))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
        for (String f : new String[]{"book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java"}) {
            assertFalse(code(read(f)).contains("net.minecraft"), f + " must stay pure");
            assertFalse(code(read(f)).contains("SpiritDiveLook"), f + " never asks the client flags");
        }
    }

    @Test
    void theLookKeepsNoPlayerStateSendsNothingAndLeavesTheCameraAlone() {
        for (String f : new String[]{"client/aura/SpiritDiveWind3.java", "client/aura/SpiritDiveWind3Draw.java", "book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java"}) {
            String c = code(read(f));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|AbstractClientPlayer|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), f + ": a static current player");
            assertFalse(c.contains("UUID") || c.contains("HashMap<") && f.startsWith("client"), f + ": a table of players");
            assertFalse(c.contains("CustomPacketPayload") || c.contains("PacketDistributor") || c.contains("StreamCodec") || c.contains("ServerLevel") || c.contains("MinecraftServer"), f + ": the look sends nothing and decides nothing");
            assertFalse(c.contains("getCameraEntity") || c.contains("setCameraType") || c.contains("ViewportEvent") || c.contains("Minecraft.getInstance"), f + ": the camera is never touched");
        }
    }

    @Test
    void thePaintersAllocateNothingPerFrame() {
        for (String f : new String[]{"client/aura/SpiritDiveWind3.java", "client/aura/SpiritDiveWind3Draw.java"}) {
            for (String line : code(read(f)).split("\n")) {
                if (!line.contains("new ")) continue;
                assertTrue(line.contains("static final"), f + ": an allocation outside a static initialiser: " + line.trim());
            }
            String c = code(read(f));
            assertFalse(c.contains("String.format") || c.contains("new ResourceLocation") || c.contains("ResourceLocation.fromNamespaceAndPath") || c.contains("\" + "), f + ": a string or a location built in a frame method");
        }
    }

    @Test
    void thePainterUsesTheLodTheCharacterCapTheProbeAndTheSharedMeshes() {
        String p = code(read("client/aura/SpiritDiveWind3.java"));
        assertTrue(p.contains("SpiritDiveTier.lod(SpiritDiveFrame.distSq(c))") && p.contains("SpiritDiveFrame.admitCharacter()"), "level of detail and the character cap per frame");
        assertTrue(p.contains("PerfProbe.section(\"c.spiritdive.wind3\")") && p.contains("PerfProbe.begin()") && p.contains("PerfProbe.end(SECTION, t0)"), "timed in c.spiritdive.wind3");
        assertTrue(p.contains("SpiritDiveWind3Rules.spec(") && p.contains("SpiritDiveWind3Mesh.get(\"sword\")") && p.contains("SpiritDiveWind3Mesh.get(\"feather\")"), "counts from the spec, pieces from the shared meshes");
        assertTrue(p.contains("lod != Lod.FAR") && p.contains("lod == Lod.FULL"), "far dives draw only the body light, weapons only at full detail");
        assertFalse(p.contains("Math.random") && p.contains("new Random"), "no random in a frame");
        String d = code(read("client/aura/SpiritDiveWind3Draw.java"));
        assertTrue(d.contains("SafeAdditive.emissive(SKIN)") && d.contains("SafeAdditive.additive(SKIN)"), "the render types go through SafeAdditive");
        assertFalse(d.contains("RenderType.eyes") || d.contains("RenderType.entityTranslucentEmissive"), "never a render type built directly");
    }

    @Test
    void theOlderWindsAndTheOtherLooksAreStillThereAndReachable() {
        String reel = code(read("client/aura/SpiritDiveReel.java"));
        assertTrue(reel.contains("SpiritDiveWind3Rules.ver(SpiritDiveLook.windLook()) != null") && reel.contains("SpiritDiveWind3.paint("), "the remake hook");
        assertTrue(reel.contains("SpiritDiveLook.windPack()") && reel.contains("SpiritDiveWind.paint("), "the PACK Wind is still reachable");
        assertTrue(reel.indexOf("SpiritDiveWind3Rules.ver(") < reel.indexOf("PerfProbe.begin()"), "the remake is decided before the REEL probe starts");
        assertTrue(reel.contains("SpiritDiveReelPieces.sword(c, el, SpiritDiveReelGeometry.SWORD"), "the REEL sword for the other elements");
        String pack = code(read("client/aura/SpiritDiveWind.java"));
        assertTrue(pack.contains("SpiritDiveWind3.weaponsA(") && pack.contains("else if (full && sp.weapon()) weapon("), "the flat weapons of the PACK look stay for PACK");
        String cfg = code(read("client/aura/DiveClientConfig.java"));
        assertTrue(cfg.contains("defineEnum(\"windLook\", SpiritDiveWindRules.DEFAULT_LOOK)"), "the option");
        assertTrue(read("book/SpiritDiveWindRules.java").contains("PACK, REEL,"), "PACK and REEL stay the first values");
    }

    @Test
    void theSelfTestTheGeneratorAndTheToolsAreRegistered() {
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveWind3SelfTests.register()"));
        assertTrue(read("book/SpiritDiveWind3SelfTests.java").contains("\"magic.spirit_dive_wind3\""));
        for (String f : new String[]{"tools/gen_sdw3_models.py", "tools/preview_sdw3_models.py", "tools/spirit_dive_wind3_scenes.py", "tools/spirit_dive_wind3_sheets.py", "tools/render_preview/scenes.d/spirit_dive_wind3.json",
                "tools/render_preview/examples/SdwV3A.java", "tools/render_preview/examples/SdwV3B.java", "tools/render_preview/examples/SdwV3C.java", "docs/magic/spirit_dives_wind3.md"})
            assertTrue(Files.exists(ROOT.resolve(f)), f + " must exist");
        assertTrue(readRoot("tools/gen_sdw3_models.py").contains("sdw3_meshes"), "the generator writes the meshes the mod reads");
    }
}
