package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: nothing of the PACK Wind can be profiled here, so its rules are pinned the only way they can be: by reading the real source. (1) Server safety: the common classes name no client class and the pure
 * ones no Minecraft class at all; the look sends no packet and keeps no per-player state. (2) Frame cost: the painter allocates nothing and builds no location or string in its frame methods, uses the level of
 * detail, the character cap per frame and the probe section {@code c.spiritdive.wind}, takes every count from the spec and never touches the camera. (3) The REEL Wind and the other looks are untouched and still
 * reachable by the option. (4) The option, the self-test and the tools are registered where they have to be.
 */
class SpiritDiveWindScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    // ------------------------------------------------------------------ 1. server safety
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    @Test
    void theCommonClassesOfTheWindLookNameNoClientClass() {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java", "book/SpiritDiveWindSelfTests.java"})
            if (CLIENT.matcher(code(read(f))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
        assertFalse(code(read("book/SpiritDiveWindRules.java")).contains("SpiritDiveLook"), "the rules never ask the client flags");
    }

    @Test
    void thePureClassesTouchNoMinecraftClassAtAll() {
        for (String f : new String[]{"book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java"}) assertFalse(code(read(f)).contains("net.minecraft"), f + " must stay pure");
    }

    @Test
    void theLookKeepsNoPlayerStateAndSendsNothing() {
        for (String f : new String[]{"client/aura/SpiritDiveWind.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java"}) {
            String c = code(read(f));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|AbstractClientPlayer|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), f + ": a static current player");
            assertFalse(c.contains("UUID") || c.contains("static final Map") || c.contains("HashMap"), f + ": a table of players");
            assertFalse(c.contains("CustomPacketPayload") || c.contains("PacketDistributor") || c.contains("StreamCodec") || c.contains("ServerLevel") || c.contains("MinecraftServer"), f + ": the look sends nothing and decides nothing");
            assertFalse(c.contains("getCameraEntity") || c.contains("setCameraType") || c.contains("CameraType.THIRD") || c.contains("ViewportEvent") || c.contains("ComputeFovModifierEvent"), f + ": the camera is never touched");
        }
    }

    // ------------------------------------------------------------------ 2. frame cost
    private static final String PAINTER = "client/aura/SpiritDiveWind.java";
    private static final Pattern METHOD = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final)\\s+)*[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}=]*)\\)\\s*(?:throws [\\w., ]+)?\\{");

    private static List<String[]> methods(String src) {
        String s = code(src).replaceAll("\"(?:[^\"\\\\\\n]|\\\\.)*\"", "\"\"");
        List<String[]> out = new ArrayList<>();
        Matcher m = METHOD.matcher(s);
        while (m.find()) {
            if (m.group(1).equals("if") || m.group(1).equals("for") || m.group(1).equals("while") || m.group(1).equals("switch") || m.group(1).equals("catch")) continue;
            int depth = 1, i = m.end();
            while (i < s.length() && depth > 0) {
                char ch = s.charAt(i++);
                if (ch == '{') depth++;
                else if (ch == '}') depth--;
            }
            out.add(new String[]{m.group(1), s.substring(m.end(), i)});
        }
        return out;
    }

    @Test
    void thePainterAllocatesNothingAndBuildsNoLocationOrStringInItsFrameMethods() {
        Pattern alloc = Pattern.compile("new\\s+(?:Vec3|Vector3f|Vector4f|Quaternionf|Matrix4f|Matrix3f|ResourceLocation|ArrayList|HashMap|HashSet|int\\[|float\\[|double\\[|String\\b|StringBuilder|long\\[)"
                + "|ResourceLocation\\.(?:fromNamespaceAndPath|parse|withDefaultNamespace|tryParse)\\(|String\\.format\\(|AuraRender\\.tex\\(|\\.stream\\(\\)|Axis\\.\\w+\\.rotation|List\\.of\\(|Map\\.of\\(");
        List<String> bad = new ArrayList<>();
        int seen = 0;
        for (String[] m : methods(read(PAINTER))) {
            seen++;
            Matcher a = alloc.matcher(m[1]);
            if (a.find()) bad.add(m[0] + ": " + a.group());
        }
        assertTrue(seen >= 12, "the scan really read the methods (" + seen + ")");
        assertTrue(bad.isEmpty(), "allocation in a frame method: " + bad);
    }

    @Test
    void theStaticScratchAndCachedTexturesAreTheOnlyAllocations() {
        String c = code(read(PAINTER));
        for (Matcher m = Pattern.compile("new\\s+(float\\[|int\\[|Quaternionf|ResourceLocation\\[)").matcher(c); m.find(); ) {
            int ls = Math.max(c.lastIndexOf(';', m.start()), c.lastIndexOf('}', m.start()));
            assertTrue(c.substring(ls + 1, m.start()).contains("static"), "an allocation that is not a static field: " + c.substring(Math.max(0, m.start() - 30), Math.min(c.length(), m.end() + 20)).trim());
        }
    }

    @Test
    void theEntryPointUsesTheLevelOfDetailTheCharacterCapTheProbeAndTheSpec() {
        String a = code(read(PAINTER));
        assertTrue(a.contains("SpiritDiveTier.lod(") && a.contains("Lod.CULLED") && a.contains("SpiritDiveFrame.admitCharacter()"), "level of detail, culling and the character cap per frame");
        assertTrue(a.contains("PerfProbe.section(\"c.spiritdive.wind\")") && a.contains("PerfProbe.begin()") && a.contains("PerfProbe.end("), "timed in /multiverse perf");
        assertTrue(a.contains("SpiritDiveWindRules.spec(") && a.contains("sp.bars()") && a.contains("sp.sparks()") && a.contains("sp.ribbons()") && a.contains("sp.wrists()") && a.contains("sp.leafSegs()") && a.contains("sp.weapon()")
                && a.contains("sp.backCircle()"), "every count comes from the spec");
        assertTrue(a.contains("lod == Lod.FAR") && a.contains("lod != Lod.FULL"), "the far level draws no detail, only full detail draws the entry sprites");
        assertTrue(a.contains("firstPersonSelf("), "nothing on the head in first person");
        assertTrue(a.contains("DiveCast.casting("), "the bow follows the cast animation the server already sends: no packet");
        assertTrue(a.contains("SpiritDiveWindGeometry.BARS") && a.contains("SpiritDiveWindGeometry.ribbonSpine("), "the crown and the ribbons read their tables");
    }

    // ------------------------------------------------------------------ 3. the REEL Wind and the other looks are untouched
    @Test
    void theReelWindIsStillThereAndReachableByTheOption() {
        String reel = code(read("client/aura/SpiritDiveReel.java"));
        assertTrue(reel.contains("SpiritDiveLook.windPack()") && reel.contains("SpiritDiveWind.paint("), "the hook");
        assertTrue(reel.indexOf("windPack()") < reel.indexOf("PerfProbe.begin()"), "the PACK Wind is decided before the REEL probe starts: its own section");
        assertTrue(reel.contains("SpiritDiveReelPieces.sword(c, el, SpiritDiveReelGeometry.SWORD") && reel.contains("SpiritDiveReelPieces.crown(") && reel.contains("SpiritDiveReelPieces.wings("), "the REEL Wind's crown, wings and sword are all still drawn for windLook = REEL");
        for (String f : new String[]{"client/aura/SpiritDiveReel.java", "client/aura/SpiritDiveReelPieces.java", "client/aura/SpiritDiveAnime.java", "client/aura/SpiritDiveDjinn.java", "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java"})
            assertTrue(Files.exists(JAVA.resolve(f)), f + " was removed");
        String aura = code(read("client/aura/SpiritDiveAura.java"));
        assertTrue(aura.contains("SpiritDiveReel.paint(") && aura.contains("SpiritDiveAnime.paint(") && aura.contains("SpiritDiveDjinn.paint("), "REEL, ANIME and Djinn are still dispatched");
        assertTrue(code(read("book/SpiritDiveDjinnPieces.java")).contains("enum Look { DJINN, V2, CLASSIC, ANIME, REEL }"), "the look values are append only");
        assertTrue(code(read("book/SpiritDiveAnimeGeometry.java")).contains("enum Shape { LEAF, FLAME, RIBBON, SHARD, SPIKE, BLADE }"), "the ribbon shapes are append only");
    }

    // ------------------------------------------------------------------ 4. registered
    @Test
    void theOptionTheSelfTestAndTheToolsAreRegistered() {
        String cfg = code(read("client/aura/DiveClientConfig.java"));
        assertTrue(cfg.contains("defineEnum(\"windLook\", SpiritDiveWindRules.DEFAULT_LOOK)") && cfg.contains("SpiritDiveLook.setWindLook(WIND_LOOK.get())"), "windLook = PACK | REEL | A_ANIME | B_CLIP | C_SAINT, default B_CLIP");
        assertTrue(cfg.contains("defineEnum(\"look\", Look.REEL)") && cfg.contains("\"classicLook\""), "the old options stay");
        assertTrue(code(read("client/aura/SpiritDiveLook.java")).contains("WindLook windLook = SpiritDiveWindRules.DEFAULT_LOOK"));
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveWindSelfTests.register()") && code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveReelSelfTests.register()"));
        assertTrue(read("book/SpiritDiveWindSelfTests.java").contains("\"magic.spirit_dive_wind\""));
        for (String f : new String[]{"tools/gen_spirit_dive_wind_textures.py", "tools/spirit_dive_wind_scenes.py", "tools/spirit_dive_wind_previews.py", "tools/render_preview/scenes.d/spirit_dive_wind.json",
                "tools/render_preview/examples/SdwPack.java", "tools/render_preview/examples/SdwReelWind.java", "docs/magic/spirit_dives_wind.md"})
            assertTrue(Files.exists(ROOT.resolve(f)), f);
    }
}
