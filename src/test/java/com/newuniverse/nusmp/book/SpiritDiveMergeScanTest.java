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
 * Why: the merge cannot be profiled here, so its rules are pinned the only way they can be: by reading the real source. (1) Server safety: the common classes name no client class and the pure ones no
 * Minecraft class at all; the layer sends no packet, decides nothing and keeps no per-player state. (2) Frame cost: the painter allocates nothing and builds no location or string in its frame methods, uses the
 * level of detail, the 32 block range, the per-frame cap and the probe section. (3) It is hooked ONCE in the dive painter, before the looks, so it runs for every look and element, and the old looks are untouched.
 * (4) The option, the self-test and the self-test registration are where they have to be.
 */
class SpiritDiveMergeScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");
    private static final String PAINTER = "client/aura/SpiritDiveMerge.java";

    // ------------------------------------------------------------------ 1. server safety
    @Test
    void theCommonClassesNameNoClientClassAndThePureOnesNoMinecraftClass() {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"book/SpiritDiveMergeRules.java", "book/SpiritDiveMergeMesh.java", "book/SpiritDiveMergeSelfTests.java"})
            if (CLIENT.matcher(code(read(f))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
        for (String f : new String[]{"book/SpiritDiveMergeRules.java", "book/SpiritDiveMergeMesh.java"}) assertFalse(code(read(f)).contains("net.minecraft"), f + " must stay pure");
    }

    @Test
    void theLayerSendsNothingDecidesNothingAndKeepsNoPlayerState() {
        for (String f : new String[]{PAINTER, "book/SpiritDiveMergeRules.java", "book/SpiritDiveMergeMesh.java"}) {
            String c = code(read(f));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|AbstractClientPlayer|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), f + ": a static current player");
            assertFalse(c.contains("UUID") && c.contains("static final Map") && !f.endsWith("Mesh.java"), f + ": a table of players");
            assertFalse(c.contains("CustomPacketPayload") || c.contains("PacketDistributor") || c.contains("StreamCodec") || c.contains("ServerLevel") || c.contains("MinecraftServer"), f + ": the merge sends nothing and decides nothing");
            assertFalse(c.contains("getCameraEntity") || c.contains("setCameraType") || c.contains("ViewportEvent"), f + ": the camera is never touched");
        }
    }

    @Test
    void theOnlyMinecraftGetInstanceIsTheFirstPersonQuestionInTheClientPackage() {
        String c = code(read(PAINTER));
        assertTrue(c.indexOf("Minecraft.getInstance()") == c.lastIndexOf("Minecraft.getInstance()") && c.contains("firstPersonSelf"), "one getInstance, in firstPersonSelf");
    }

    // ------------------------------------------------------------------ 2. frame cost
    private static final Pattern METHOD = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final)\\s+)*[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}=]*)\\)\\s*(?:throws [\\w., ]+)?\\{");

    private static List<String[]> methods(String src) {
        String s = code(src).replaceAll("\"(?:[^\"\\\\\\n]|\\\\.)*\"", "\"\"");
        List<String[]> out = new ArrayList<>();
        Matcher m = METHOD.matcher(s);
        while (m.find()) {
            String n = m.group(1);
            if (n.equals("if") || n.equals("for") || n.equals("while") || n.equals("switch") || n.equals("catch")) continue;
            int depth = 1, i = m.end();
            while (i < s.length() && depth > 0) {
                char ch = s.charAt(i++);
                if (ch == '{') depth++;
                else if (ch == '}') depth--;
            }
            out.add(new String[]{n, s.substring(m.end(), i)});
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
        assertTrue(seen >= 8, "the scan really read the methods (" + seen + ")");
        assertTrue(bad.isEmpty(), "allocation in a frame method: " + bad);
        String c = code(read(PAINTER));
        for (Matcher m = Pattern.compile("new\\s+(float\\[|int\\[|Quaternionf|Vector3f|ResourceLocation\\[|PerfBudget)").matcher(c); m.find(); ) {
            int ls = Math.max(c.lastIndexOf(';', m.start()), c.lastIndexOf('}', m.start()));
            assertTrue(c.substring(ls + 1, m.start()).contains("static"), "an allocation that is not a static field: " + c.substring(Math.max(0, m.start() - 30), Math.min(c.length(), m.end() + 20)).trim());
        }
    }

    @Test
    void theEntryPointUsesTheRangeTheLevelOfDetailTheFrameCapAndTheProbe() {
        String a = code(read(PAINTER));
        assertTrue(a.contains("SpiritDiveTier.lod(") && a.contains("Lod.CULLED") && a.contains("Lod.FULL"), "level of detail, culling and full detail for the silhouette");
        assertTrue(a.contains("SpiritDiveMergeRules.GHOST_RANGE2") && a.contains("MERGES_PER_FRAME") && a.contains("BUDGET.tryTake("), "the 32 block range and the three merges per frame");
        assertTrue(a.contains("PerfProbe.section(\"c.spiritdive.merge\")") && a.contains("PerfProbe.begin()") && a.contains("PerfProbe.end("), "timed in /multiverse perf");
        assertTrue(a.contains("firstPersonSelf(c)"), "nothing is drawn in first person for the wearer");
        assertTrue(a.contains("FuegoleonArmAura.drawsArm(") && !a.contains("Aura.FUEGOLEON_ARM"), "the flame arm is its own look: no second arm");
        assertTrue(a.contains("if (!enabled) return;"), "the option switches the whole layer off");
        assertTrue(a.contains("SpiritDiveMergeRules.clock(c.t(), c.left())"), "everything is a function of the dive's own clock");
    }

    // ------------------------------------------------------------------ 3. hooked once, the old looks untouched
    @Test
    void theHookIsOneLineBeforeTheLooksAndTheOldLooksAreStillDispatched() {
        String aura = code(read("client/aura/SpiritDiveAura.java"));
        int hooks = 0;
        for (Matcher m = Pattern.compile("SpiritDiveMerge\\.paint\\(").matcher(aura); m.find(); ) hooks++;
        assertTrue(hooks == 1, "exactly one hook in the dive painter, found " + hooks);
        assertTrue(aura.indexOf("SpiritDiveMerge.paint(") < aura.indexOf("SpiritDiveReel.paint("), "the merge is painted before any look is chosen");
        assertTrue(aura.contains("SpiritDiveReel.paint(") && aura.contains("SpiritDiveAnime.paint(") && aura.contains("SpiritDiveDjinn.paint(") && aura.contains("SpiritDiveLook.classic()"), "REEL, ANIME, Djinn and classic are still dispatched");
        for (String f : new String[]{"client/aura/SpiritDiveAnime.java", "client/aura/SpiritDiveDjinn.java", "client/aura/SpiritDiveReel.java", "client/aura/SpiritDiveWind3.java", "client/aura/SpiritDiveFx.java"})
            assertTrue(Files.exists(JAVA.resolve(f)), f + " was removed");
    }

    // ------------------------------------------------------------------ 4. registered
    @Test
    void theOptionAndTheSelfTestAreRegistered() {
        String cfg = code(read("client/aura/DiveClientConfig.java"));
        assertTrue(cfg.contains("define(\"merge\", true)") && cfg.contains("SpiritDiveMerge.setEnabled(MERGE.get())"), "spiritDive.merge, default true, mirrored into the painter");
        assertTrue(cfg.contains("defineEnum(\"look\", Look.REEL)") && cfg.contains("\"classicLook\"") && cfg.contains("characterEffects"), "the old options stay");
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveMergeSelfTests.register()"), "the self-test is registered");
        assertTrue(code(read("book/SpiritDiveMergeSelfTests.java")).contains("\"magic.spirit_dive_merge\""), "the prefix");
    }
}
