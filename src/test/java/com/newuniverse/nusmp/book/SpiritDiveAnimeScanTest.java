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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: nothing of the ANIME Spirit Dive can be profiled here, so its rules are pinned the only way they can be: by reading the real source. (1) Server safety: the common classes name no client class
 * and the pure ones no Minecraft class at all. (2) Frame cost: the painters allocate nothing and build no location or string in their frame methods, use the level of detail, the character cap per frame
 * and the probe section, cache their render types, and send no packet. (3) The old looks are still there: the Djinn plates, the V2 remake and the classic dive are untouched and still reachable by the
 * option, and the flame arm of Fuegoleon is not redrawn. (4) The option, the self-test and the first person hook are registered where they have to be.
 */
class SpiritDiveAnimeScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    // ------------------------------------------------------------------ 1. server safety
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    @Test
    void theCommonClassesOfTheAnimeLookNameNoClientClass() {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java", "book/SpiritDiveAnimeSelfTests.java", "book/SpiritDiveBoot.java"})
            if (CLIENT.matcher(code(read(f))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
    }

    @Test
    void thePureClassesTouchNoMinecraftClassAtAll() {
        for (String f : new String[]{"book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java"}) assertFalse(code(read(f)).contains("net.minecraft"), f + " must stay pure");
    }

    @Test
    void thePerPlayerStateIsKeyedByNothingBecauseThereIsNone() {
        // the painters draw from the aura context of ONE player and keep no per-player state: no static player, no map by player
        for (String f : new String[]{"client/aura/SpiritDiveAnime.java", "client/aura/SpiritDiveAnimeShell.java", "client/aura/SpiritDiveAnimeRibbon.java"}) {
            String c = code(read(f));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|AbstractClientPlayer|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), f + ": a static current player");
            assertFalse(c.contains("UUID") && c.contains("static final Map"), f + ": a table of players");
            assertFalse(c.contains("CustomPacketPayload") || c.contains("PacketDistributor") || c.contains("StreamCodec"), f + ": the look sends nothing");
        }
    }

    // ------------------------------------------------------------------ 2. frame cost
    private static final String[] PAINTERS = {"client/aura/SpiritDiveAnime.java", "client/aura/SpiritDiveAnimeShell.java", "client/aura/SpiritDiveAnimeRibbon.java", "client/aura/DiveAnimeFirstPerson.java"};
    private static final Pattern METHOD = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final)\\s+)+[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}=]*)\\)\\s*(?:throws [\\w., ]+)?\\{");

    private static List<String[]> methods(String src) {
        String s = code(src).replaceAll("\"(?:[^\"\\\\\\n]|\\\\.)*\"", "\"\"");
        List<String[]> out = new ArrayList<>();
        Matcher m = METHOD.matcher(s);
        while (m.find()) {
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
                if (m[0].equals("register") || m[0].startsWith("<")) continue;
                Matcher a = alloc.matcher(m[1]);
                if (a.find()) bad.add(f + " " + m[0] + ": " + a.group());
            }
        assertTrue(seen > 25, "the scan really read the methods (" + seen + ")");
        assertTrue(bad.isEmpty(), "allocation in a frame method: " + bad);
    }

    @Test
    void theRenderTypesAreCachedAndTheLimitsAreUsed() {
        String shell = code(read("client/aura/SpiritDiveAnimeShell.java"));
        assertTrue(shell.contains("RenderType[][] SWIRL") && shell.contains("if (t == null)"), "one render type per (element, step), made once");
        assertTrue(shell.contains("SWIRL_STEPS") && shell.contains("NURenderTypes.spiritSwirl("));
        String a = code(read("client/aura/SpiritDiveAnime.java"));
        assertTrue(a.contains("SpiritDiveTier.lod(") && a.contains("Lod.CULLED") && a.contains("SpiritDiveFrame.admitCharacter()"), "level of detail, culling and the character cap per frame");
        assertTrue(a.contains("PerfProbe.section(\"c.spiritdive.anime\")") && a.contains("PerfProbe.begin()") && a.contains("PerfProbe.end("), "timed in /multiverse perf");
        assertTrue(a.contains("SpiritDiveAnimeRules.spec(") && a.contains("sp.plumeRows()") && a.contains("sp.locks()") && a.contains("sp.plumeSegs()"), "every count comes from the spec");
        assertTrue(a.contains("lod != Lod.FAR"), "the far level never draws the character tier");
    }

    @Test
    void theShellInflatesAndFlipsInsideOneReusedConsumer() {
        String s = code(read("client/aura/SpiritDiveAnimeShell.java"));
        assertTrue(s.contains("implements VertexConsumer") && s.contains("private static final Inflate INFLATE = new Inflate()"));
        assertTrue(s.contains("emit(0); emit(3); emit(2); emit(1);"), "the rim hull reverses the winding so only the outline survives the culling");
        assertTrue(s.contains("m.m00()") && s.contains("m.m01()") && s.contains("m.m02()") && s.contains("m.m30()"), "the part's axes are the columns of its matrix (JOML names a field m<column><row>)");
    }

    @Test
    void theRibbonsShareTheirEdgesAndTurnTheirWindingToTheViewer() {
        String r = code(read("client/aura/SpiritDiveAnimeRibbon.java"));
        assertTrue(r.contains("SpiritDiveAnimeGeometry.edges(") && r.contains("SpiritDiveAnimeGeometry.facing("), "one continuous strip, the winding chosen for the viewer");
        assertTrue(r.contains("static final float[] SPINE = new float["), "scratch arrays are statics");
    }

    @Test
    void theFlameArmIsLeftAloneAndTheHairHidesUnderAHelmet() {
        String a = code(read("client/aura/SpiritDiveAnime.java"));
        assertTrue(a.contains("FuegoleonArmAura.drawsArm("), "the Fire dive never draws a second arm");
        assertFalse(a.contains("flame_arm") || a.contains("FuegoleonArmDjinn") || a.contains("Aura.FUEGOLEON_ARM"), "the arm is its own look");
        assertTrue(a.contains("DiveHead.helmet(") && a.contains("!helmet"), "no eyes and no hair cap under a helmet");
        assertTrue(a.contains("firstPersonSelf("), "nothing on the head in first person");
        String f = code(read("client/aura/DiveAnimeFirstPerson.java"));
        assertTrue(f.contains("Aura.FUEGOLEON_ARM") && f.contains("SpiritDiveLook.anime()"), "first person: only the anime look, never the flame arm's right hand");
        assertTrue(f.contains("FIRST_PERSON_ALPHA"), "a light glow");
    }

    // ------------------------------------------------------------------ 3. the old looks are still there
    @Test
    void theOldLooksAreStillInThePainterAndTheirFilesExist() {
        String aura = code(read("client/aura/SpiritDiveAura.java"));
        assertTrue(aura.contains("SpiritDiveDjinn.paint(") && aura.contains("remade(c, el, st, SpiritDivePieces.grade(style), pulse)") && aura.contains("SpiritDiveLook.classic()"), "Djinn, V2 and classic are still dispatched");
        int anime = aura.indexOf("Look.ANIME"), djinn = aura.indexOf("Look.DJINN");
        assertTrue(anime > 0 && djinn > anime, "ANIME is tested before the others");
        assertTrue(aura.contains("SpiritDiveAnime.paint("));
        for (String f : new String[]{"client/aura/SpiritDiveDjinn.java", "client/aura/DiveFirstPerson.java", "client/aura/SpiritDiveFx.java", "book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "client/aura/FuegoleonArmAura.java"})
            assertTrue(Files.exists(JAVA.resolve(f)), f + " was removed");
        String e = code(read("book/SpiritDiveDjinnPieces.java"));
        assertTrue(e.contains("enum Look { DJINN, V2, CLASSIC, ANIME }"), "the values are append only");
    }

    // ------------------------------------------------------------------ 4. registered
    @Test
    void theOptionTheSelfTestAndTheFirstPersonHookAreRegistered() {
        String cfg = code(read("client/aura/DiveClientConfig.java"));
        assertTrue(cfg.contains("defineEnum(\"look\", Look.ANIME)") && cfg.contains("\"classicLook\"") && cfg.contains("characterEffects"), "look = ANIME | DJINN | V2 | CLASSIC; the old options stay");
        assertTrue(code(read("client/aura/SpiritDiveLook.java")).contains("Look look = Look.ANIME"));
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveAnimeSelfTests.register()"));
        assertTrue(read("book/SpiritDiveAnimeSelfTests.java").contains("\"magic.spirit_dive_anime\""));
        String c = code(read("client/aura/DiveClient.java"));
        assertTrue(c.contains("DiveAnimeFirstPerson::onRenderArm") && c.contains("DiveAnimeFirstPerson::onRenderHand"));
        assertTrue(c.contains("DiveFirstPerson::onRenderArm"), "the Djinn first person hook is still registered");
        assertTrue(code(read("client/NURenderTypes.java")).contains("spiritSwirl("), "the one place a vanilla additive type may be named");
    }

    @Test
    void theTextureGeneratorExistsAndWritesEveryTexture() throws IOException {
        String py = Files.readString(ROOT.resolve("tools/gen_spirit_dive_anime_textures.py"), StandardCharsets.UTF_8);
        for (String p : new String[]{"sda_swirl_", "sda_plume_", "sda_plume_glow_", "sda_lock_", "sda_lock_glow_", "sda_halo_", "sda_hair_", "\"sda_rim\"", "\"sda_soft\"", "\"sda_eye\"", "\"sda_burst\""})
            assertTrue(py.contains(p), "the generator does not write " + p);
        assertEquals(4 * 7 + 4, SpiritDiveAnimeRules.allTextures().size());
    }
}
