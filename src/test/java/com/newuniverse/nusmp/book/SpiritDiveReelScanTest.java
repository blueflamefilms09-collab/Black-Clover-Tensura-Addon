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
 * Why: nothing of the REEL Spirit Dive can be profiled here, so its rules are pinned the only way they can be: by reading the real source. (1) Server safety: the common classes name no client class
 * and the pure ones no Minecraft class at all; the look sends no packet and keeps no per-player state. (2) Frame cost: the painters allocate nothing and build no location or string in their frame methods,
 * use the level of detail, the character cap per frame and the probe section, take every count from the spec, and never touch the camera. (3) The old looks are untouched and still reachable by the option.
 * (4) The option, the self-test and the first person hook are registered where they have to be.
 */
class SpiritDiveReelScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    // ------------------------------------------------------------------ 1. server safety
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    @Test
    void theCommonClassesOfTheReelLookNameNoClientClass() {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveReelSelfTests.java"})
            if (CLIENT.matcher(code(read(f))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
        // the one reference to the client class the self-test needs would be a Look value: that is common (SpiritDiveDjinnPieces.Look)
        assertFalse(code(read("book/SpiritDiveReelRules.java")).contains("SpiritDiveLook"), "the rules never ask the client flags");
    }

    @Test
    void thePureClassesTouchNoMinecraftClassAtAll() {
        for (String f : new String[]{"book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java"}) assertFalse(code(read(f)).contains("net.minecraft"), f + " must stay pure");
    }

    @Test
    void theLookKeepsNoPlayerStateAndSendsNothing() {
        for (String f : new String[]{"client/aura/SpiritDiveReel.java", "client/aura/SpiritDiveReelPieces.java", "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java"}) {
            String c = code(read(f));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|AbstractClientPlayer|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), f + ": a static current player");
            assertFalse(c.contains("UUID") && c.contains("static final Map"), f + ": a table of players");
            assertFalse(c.contains("CustomPacketPayload") || c.contains("PacketDistributor") || c.contains("StreamCodec") || c.contains("ServerLevel") || c.contains("MinecraftServer"), f + ": the look sends nothing and decides nothing");
            assertFalse(c.contains("getCameraEntity") || c.contains("setCameraType") || c.contains("CameraType.THIRD") || c.contains("ViewportEvent") || c.contains("ComputeFovModifierEvent"), f + ": the camera is never touched");
        }
    }

    // ------------------------------------------------------------------ 2. frame cost
    private static final String[] PAINTERS = {"client/aura/SpiritDiveReel.java", "client/aura/SpiritDiveReelPieces.java"};
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
    void thePaintersAllocateNothingAndBuildNoLocationOrStringInTheirFrameMethods() {
        Pattern alloc = Pattern.compile("new\\s+(?:Vec3|Vector3f|Vector4f|Quaternionf|Matrix4f|Matrix3f|ResourceLocation|ArrayList|HashMap|HashSet|int\\[|float\\[|double\\[|String\\b|StringBuilder|long\\[)"
                + "|ResourceLocation\\.(?:fromNamespaceAndPath|parse|withDefaultNamespace|tryParse)\\(|String\\.format\\(|AuraRender\\.tex\\(|\\.stream\\(\\)|Axis\\.\\w+\\.rotation|List\\.of\\(|Map\\.of\\(");
        List<String> bad = new ArrayList<>();
        int seen = 0;
        for (String f : PAINTERS)
            for (String[] m : methods(read(f))) {
                seen++;
                Matcher a = alloc.matcher(m[1]);
                if (a.find()) bad.add(f + " " + m[0] + ": " + a.group());
            }
        assertTrue(seen >= 20, "the scan really read the methods (" + seen + ")");
        assertTrue(bad.isEmpty(), "allocation in a frame method: " + bad);
    }

    @Test
    void theStaticScratchAndCachedTexturesAreTheOnlyAllocations() {
        for (String f : PAINTERS) {
            String c = code(read(f));
            for (Matcher m = Pattern.compile("new\\s+(float\\[|int\\[|Quaternionf|ResourceLocation\\[)").matcher(c); m.find(); ) {
                int ls = Math.max(c.lastIndexOf(';', m.start()), c.lastIndexOf('}', m.start()));                   // the start of the declaration it belongs to
                assertTrue(c.substring(ls + 1, m.start()).contains("static"), f + ": an allocation that is not a static field: " + c.substring(Math.max(0, m.start() - 30), Math.min(c.length(), m.end() + 20)).trim());
            }
        }
    }

    @Test
    void theEntryPointUsesTheLevelOfDetailTheCharacterCapTheProbeAndTheSpec() {
        String a = code(read("client/aura/SpiritDiveReel.java"));
        assertTrue(a.contains("SpiritDiveTier.lod(") && a.contains("Lod.CULLED") && a.contains("SpiritDiveFrame.admitCharacter()"), "level of detail, culling and the character cap per frame");
        assertTrue(a.contains("PerfProbe.section(\"c.spiritdive.reel\")") && a.contains("PerfProbe.begin()") && a.contains("PerfProbe.end("), "timed in /multiverse perf");
        assertTrue(a.contains("SpiritDiveReelRules.spec(") && a.contains("sp.ribbons()") && a.contains("sp.orbit()") && a.contains("sp.sparks()") && a.contains("sp.cage()") && a.contains("sp.crownCubes()"), "every count comes from the spec");
        assertTrue(a.contains("lod != Lod.FAR") && a.contains("lod != Lod.FULL"), "the far level never draws the character tier, only full detail draws the entry sprites");
        assertTrue(a.contains("FuegoleonArmAura.drawsArm(") && !a.contains("Aura.FUEGOLEON_ARM"), "the flame arm is its own look: no second arm");
        assertTrue(a.contains("firstPersonSelf("), "nothing on the head in first person");
        String p = code(read("client/aura/SpiritDiveReelPieces.java"));
        assertTrue(p.contains("SpiritDiveReelGeometry.CROWN") && p.contains("SpiritDiveReelGeometry.BARS") && p.contains("sp.crownBars()"), "the crown reads its tables");
    }

    // ------------------------------------------------------------------ 3. the old looks are untouched
    @Test
    void theOldLooksAreStillInThePainterAndTheirFilesExist() {
        String aura = code(read("client/aura/SpiritDiveAura.java"));
        assertTrue(aura.contains("SpiritDiveReel.paint(") && aura.contains("SpiritDiveAnime.paint(") && aura.contains("SpiritDiveDjinn.paint(") && aura.contains("remade(c, el, st, SpiritDivePieces.grade(style), pulse)") && aura.contains("SpiritDiveLook.classic()"),
                "REEL, ANIME, Djinn, V2 and classic are all still dispatched");
        assertTrue(aura.indexOf("Look.REEL") > 0 && aura.indexOf("Look.REEL") < aura.indexOf("Look.ANIME"), "REEL is tested first");
        for (String f : new String[]{"client/aura/SpiritDiveAnime.java", "client/aura/SpiritDiveDjinn.java", "client/aura/DiveFirstPerson.java", "client/aura/SpiritDiveFx.java", "book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "client/aura/FuegoleonArmAura.java"})
            assertTrue(Files.exists(JAVA.resolve(f)), f + " was removed");
        assertTrue(code(read("book/SpiritDiveDjinnPieces.java")).contains("enum Look { DJINN, V2, CLASSIC, ANIME, REEL }"), "the values are append only");
        assertTrue(code(read("book/SpiritDiveAnimeGeometry.java")).contains("enum Shape { LEAF, FLAME, RIBBON, SHARD, SPIKE, BLADE }"), "the ribbon shapes are append only");
    }

    // ------------------------------------------------------------------ 4. registered
    @Test
    void theOptionTheSelfTestAndTheFirstPersonHookAreRegistered() {
        String cfg = code(read("client/aura/DiveClientConfig.java"));
        assertTrue(cfg.contains("defineEnum(\"look\", Look.REEL)") && cfg.contains("\"classicLook\"") && cfg.contains("characterEffects"), "look = REEL | ANIME | DJINN | V2 | CLASSIC; the old options stay");
        assertTrue(code(read("client/aura/SpiritDiveLook.java")).contains("Look look = Look.REEL"));
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveReelSelfTests.register()") && code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveAnimeSelfTests.register()"));
        assertTrue(read("book/SpiritDiveReelSelfTests.java").contains("\"magic.spirit_dive_reel\""));
        assertTrue(code(read("client/aura/DiveAnimeFirstPerson.java")).contains("SpiritDiveLook.reel()"), "first person wears the light arms in the REEL look too");
    }

    @Test
    void theTextureGeneratorAndThePreviewToolsExist() {
        for (String f : new String[]{"tools/gen_spirit_dive_reel_textures.py", "tools/spirit_dive_reel_scenes.py", "tools/spirit_dive_reel_previews.py", "tools/render_preview/scenes.d/spirit_dive_reel.json", "tools/render_preview/examples/SdrReel.java"})
            assertTrue(Files.exists(ROOT.resolve(f)), f);
    }
}
