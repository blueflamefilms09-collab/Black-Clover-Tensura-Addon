package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the hair and tail painter (docs/saiyan_hair.md) is client drawing; nothing can be profiled here, so its bounds, its settings and its wiring are pinned by reading the source (rule 11). */
class SaiyanHairSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    /** The text of the method that starts at {@code header}, up to its closing brace. */
    private static String body(String src, String header) {
        int at = src.indexOf(header);
        assertTrue(at >= 0, "no method " + header);
        int open = src.indexOf('{', at), depth = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return src.substring(open, i + 1);
        }
        return src.substring(open);
    }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    @Test
    void theHairTableIsCommonCodeWithNoClientClass() throws IOException {
        String t = read(MAIN + "saiyan/SaiyanHair.java");
        assertFalse(t.contains("net.minecraft.client") || t.contains("com.newuniverse.nusmp.client") || t.contains("com.mojang.blaze3d") || t.contains("Minecraft.getInstance"), "pure table");
        assertFalse(t.contains("java.util.Random") || t.contains("Math.random"), "deterministic");
        assertFalse(t.contains("HashMap"), "no per-player map: the table is a short list");
    }

    @Test
    void thePainterAllocatesNothingPerFrameIsMeasuredCulledAndBudgeted() throws IOException {
        String s = read(MAIN + "client/aura/SaiyanHairDraw.java");
        assertFalse(s.contains("Minecraft.getInstance"), "the owner's camera is asked through the aura context");
        for (String m : new String[]{"static boolean draw(", "public float place("}) {
            String b = code(body(s, m));
            assertFalse(b.contains("new "), m + " must not allocate (the matrices and the rotation are fields)");
            assertFalse(b.contains("ResourceLocation"), m + " builds no location");
        }
        assertTrue(s.contains("PerfProbe.section(\"c.saiyan.hair\")") && s.contains("PerfProbe.begin()") && s.contains("PerfProbe.end("), "measured");
        assertTrue(s.contains("NoelleFormRules.budgeted(") && s.contains("SaiyanHair.wanted(d2)"), "distance levels and the crowd budget (4 full, 6 far, nothing else)");
        assertTrue(s.contains("self1p"), "not in first person");
        assertTrue(s.contains("SaiyanLookClientConfig.hair()") && s.contains("SaiyanLookClientConfig.tail3d()"), "both parts have their setting");
        assertTrue(s.contains("RenderLevelStageEvent") && s.contains("full = 0; far = 0;"), "the budget starts again every frame");
        assertTrue(s.contains("drawParts("), "drawn through the shared mesh drawer");
    }

    @Test
    void theSettingsAreClientOnlyDefaultOnAndTheOldRibbonStaysBehindTheSwitch() throws IOException {
        String c = read(MAIN + "client/aura/SaiyanLookClientConfig.java");
        assertTrue(c.contains("ModConfig.Type.CLIENT"), "looks only: a client config");
        assertTrue(c.contains(".define(\"hair\", true)") && c.contains(".define(\"tail3d\", true)"), "saiyan.hair and saiyan.tail3d default on");
        assertTrue(c.contains("hair = HAIR.get()") && c.contains("tail3d = TAIL3D.get()") && c.contains("SaiyanHairDraw.init(modBus)"), "mirrored into the fields, the reset hook registered");
        String a = read(MAIN + "client/aura/SaiyanAura.java");
        assertTrue(a.contains("private static void tail(AuraContext c") && a.contains("if (!self1p && !tail3d) tail(c, k, age, f);"), "the old ribbon is kept and drawn when the Blender tail is off or missing");
        assertTrue(a.indexOf("SaiyanHairDraw.draw(") < a.indexOf("SealDraw.camera(pose);"), "the meshes are drawn before the pose is turned to the camera");
    }

    @Test
    void theSelfTestIsRegisteredAndInTheSmokeList() throws IOException {
        assertTrue(read(MAIN + "saiyan/SaiyanLookSelfTests.java").contains("\"saiyan.hair.rules\""));
        assertTrue(read(".github/workflows/smoke.yml").contains("saiyan.look,saiyan.hair,"), "saiyan.hair is expected by the smoke test");
        assertTrue(read("src/test/java/com/newuniverse/nusmp/perf/PerfSourceScanTest.java").contains("client/aura/SaiyanHairDraw.java"), "a hot render file: no location built in its frame methods");
    }
}
