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
 * Why: the Djinn-style flame arm cannot be profiled or seen here, so the source is read. (1) Server safety (rule 9): the common classes name no client class, and nothing is statically "the current
 * player". (2) Frame cost (rule 11): the painters allocate nothing and build no location or string in their frame methods, ask for their blends only through AuraRender, take every count from the
 * budget table, cap the full arms per frame, skip far arms, and sit in probe sections. (3) Rule 2: the 3.0.0 arm (the molten model, the flame sheets, the mane, the first person hand of it) and its
 * assets are all still there behind the client option {@code fuegoleonArm.look}. (4) Wiring: the config key, the self-test prefix, the public predicate the dive painters check.
 */
class FuegoleonArmDjinnScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/nusmp");

    private static String read(Path p) {
        try { return Files.readString(p, StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + p, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static String src(String rel) { return code(read(JAVA.resolve(rel))); }

    // ------------------------------------------------------------------ 1. server safety
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    @Test
    void theCommonClassesNameNoClientClassAndKeepNoStaticState() {
        for (String f : new String[]{"book/FuegoleonArmDjinnRules.java", "book/FuegoleonArmDjinnSelfTests.java"}) {
            String c = src(f);
            assertFalse(CLIENT.matcher(c).find(), f + " names a client class");
            assertFalse(c.contains("static final Map") || c.contains("HashMap") || c.contains("static volatile") || c.contains("static Player") || c.contains("static ServerPlayer"), f + " keeps static state");
        }
    }

    @Test
    void theLookHolderAndThePredicateAreClientOnlyHelpersWithNoPerPlayerState() {
        String look = src("client/aura/FuegoleonArmLook.java");
        assertTrue(look.contains("static volatile Look look"), "one flag, mirrored from the client config");
        assertFalse(look.contains("Map<") || look.contains("UUID"), "no per-player table");
        String aura = src("client/aura/FuegoleonArmAura.java");
        assertTrue(aura.contains("public static boolean drawsArm("), "the dive painters check this predicate so the Fire dive does not draw a second arm");
        assertTrue(aura.indexOf("p.getId()") > aura.indexOf("drawsArm("), "the predicate reads the wearer's own lease (keyed by entity id)");
    }

    // ------------------------------------------------------------------ 2. the frame cost
    private static final String[] PAINTERS = {"client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmSparks.java", "client/aura/FuegoleonArmLook.java"};
    private static final Pattern METHOD = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final)\\s+)+[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}=]*)\\)\\s*(?:throws [\\w., ]+)?\\{");

    private static List<String[]> methods(String source) {
        String s = code(source).replaceAll("\"(?:[^\"\\\\\\n]|\\\\.)*\"", "\"\"");
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
                + "|ResourceLocation\\.(?:fromNamespaceAndPath|parse|withDefaultNamespace|tryParse)\\(|String\\.format\\(|AuraRender\\.tex\\(|GeoSpec\\.of\\(|\\.stream\\(\\)|Axis\\.\\w+\\.rotation");
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (String f : PAINTERS)
            for (String[] m : methods(read(JAVA.resolve(f)))) {
                checked++;
                Matcher a = alloc.matcher(m[1]);
                if (a.find()) bad.add(f + " " + m[0] + ": " + a.group());
            }
        assertTrue(checked >= 8, "the scan found the methods (" + checked + ")");
        assertTrue(bad.isEmpty(), "allocation or a built location / string in a frame method: " + bad);
    }

    @Test
    void theBlendsGoThroughAuraRenderAndNoPainterBuildsARenderTypeOfItsOwn() {
        for (String f : PAINTERS) {
            String c = src(f);
            assertFalse(c.contains("RenderType.eyes(") || c.contains("entityTranslucentEmissive") || c.contains("RenderType.lightning") || c.contains("RenderType.entityTranslucent("), f + " builds a render type itself (use AuraRender: the SafeAdditive rule)");
        }
        String fx = src("client/aura/FuegoleonArmDjinnFx.java");
        assertTrue(fx.contains("AuraRender.additive(tex)"), "sparks, haze and flashes are additive through AuraRender");
        assertTrue(fx.contains("AuraRender.translucent(TONGUE)"), "the tongues are alpha blended (additive would drop their dark red outline)");
        assertFalse(fx.contains("AdditiveTypes.") || fx.contains("SafeAdditive."), "the SafeAdditive types are reached only through AuraRender");
    }

    @Test
    void everyCountComesFromTheBudgetTableAndNothingElse() {
        String fx = src("client/aura/FuegoleonArmDjinnFx.java");
        for (String need : new String[]{"FuegoleonArmDjinnRules.tonguesAt(", "FuegoleonArmDjinnRules.segmentsAt(", "FuegoleonArmDjinnRules.sparksAt(", "FuegoleonArmDjinnRules.tongueLength(", "FuegoleonArmDjinnRules.haze(",
                "FuegoleonArmDjinnRules.MAX_FLASHES", "FuegoleonArmDjinnRules.TONGUE", "if (lod == Lod.CULLED) return;"})
            assertTrue(fx.contains(need), "the painter ignores " + need);
        String painter = src("client/aura/FuegoleonArmDjinn.java");
        assertTrue(painter.contains("FuegoleonArmDjinnRules.lod(distSq(c))"), "level of detail by distance");
        assertTrue(painter.contains("if (lod == Lod.CULLED) return;"), "the effects of far players are skipped");
        assertTrue(painter.contains("FULL.admit(frame, id)") && painter.contains("lod = Lod.HALF"), "the per-frame cap of full arms");
        assertTrue(painter.contains("EquipAnimation.assembled(") && painter.contains("EquipAnimation.begin("), "the forge-in is the Djinn's own animation");
        assertTrue(painter.contains("PropDraw.FULL_BRIGHT"), "the cel-shaded limb is drawn at full brightness");
        assertTrue(painter.contains("lod == Lod.FULL ? SealDraw.argb("), "the glow pass is only drawn at the full level");
    }

    @Test
    void theSparkParticlesGoThroughTheClientParticleBudgetAndTheViewersSwitch() {
        String s = src("client/aura/FuegoleonArmSparks.java");
        assertTrue(s.contains("Fx.spray("), "every particle goes through Fx (the client particle budget, the quality, the distance bands)");
        assertTrue(s.contains("SpiritDiveLook.particles()"), "the viewer can switch the particles off");
        assertTrue(s.contains("served < FuegoleonArmDjinnRules.SPARK_ARMS") && s.contains("SPARK_RANGE"), "bounded by arms per tick and by range");
        assertTrue(s.contains("sparkNow("), "staggered by entity id");
        assertTrue(s.contains("PerfProbe.section(\"c.fuegoleon.arm.sparks\")") && s.contains("PerfProbe.begin()") && s.contains("PerfProbe.end("), "timed by the probe");
        assertTrue(s.contains("mc.isPaused()"), "nothing spawns while paused");
        String themes = read(ROOT.resolve("src/main/resources/data/nusmp/particle_themes.json"));
        assertTrue(themes.contains("\"fuegoleon_arm.sparks\""), "the theme exists in the table");
        assertTrue(read(ROOT.resolve("tools/particles/gen_themes.py")).contains("\"fuegoleon_arm.sparks\""), "and in its generator");
    }

    @Test
    void thePainterIsStillInsideItsProbeSection() {
        String aura = read(JAVA.resolve("client/aura/FuegoleonArmAura.java"));
        assertTrue(aura.contains("PerfProbe.section(\"c.fuegoleon.arm\")") && aura.contains("PerfProbe.begin()") && aura.contains("PerfProbe.end("), "the arm painter lost its probe section (the new look runs inside it)");
    }

    // ------------------------------------------------------------------ 3. rule 2: the old looks are kept
    @Test
    void theOldArmIsStillThereAndStillDrawnByTheOtherLooks() {
        String aura = src("client/aura/FuegoleonArmAura.java"), fp = src("client/aura/FireArmFirstPerson.java"), fx = src("client/aura/FuegoleonArmFx.java");
        assertTrue(aura.contains("GeoSpec.of(\"spiritdive\", \"arm_flame_arm_r\")") && fp.contains("GeoSpec.of(\"spiritdive\", \"arm_flame_arm_r\")"), "the molten model is still painted (V3 and CLASSIC)");
        assertTrue(aura.contains("FuegoleonArmFx.draw(") && aura.contains("FuegoleonArmFx.mane(") && fp.contains("FuegoleonArmFx.draw("), "the flame sheets and the mane are still drawn by V3");
        assertTrue(fx.contains("public static void draw(") && fx.contains("public static void mane(") && fx.contains("public static void cameraFirstPerson("), "the old effect class is whole");
        for (String rel : new String[]{"geo/entity/spiritdive_arm_flame_arm_r.geo.json", "textures/entity/spiritdive_arm_flame_arm_r.png", "textures/entity/spiritdive_arm_flame_arm_r_glow.png",
                "animations/entity/spiritdive_arm_flame_arm_r.animation.json", "textures/aura/sdv_flame_sheet.png", "textures/aura/sdv_wisp.png"})
            assertTrue(Files.isRegularFile(ASSETS.resolve(rel)), "an asset of the 3.0.0 arm was removed: " + rel);
        assertTrue(aura.contains("FuegoleonArmDjinnRules.drawsLimb(look)") && aura.contains("FuegoleonArmDjinnRules.drawsSheets(look)"), "the look decides what the old painter draws");
        assertTrue(fp.contains("FuegoleonArmDjinnRules.drawsLimb(look)"), "the first person hand dispatches on the look too");
    }

    @Test
    void theServersRulesOfTheArmWereNotTouched() {
        String arm = src("book/FuegoleonArm.java");
        assertFalse(arm.contains("Djinn") || arm.contains("Look"), "the server decides who has the arm; the look is the client's choice");
        String rules = src("book/FuegoleonArmRules.java");
        assertFalse(rules.contains("Djinn"), "FuegoleonArmRules is unchanged");
    }

    // ------------------------------------------------------------------ 4. wiring
    @Test
    void theConfigKeyTheSelfTestAndTheFirstPersonHandAreWired() {
        String cfg = src("client/aura/DiveClientConfig.java");
        assertTrue(cfg.contains("defineEnum(\"look\", FuegoleonArmDjinnRules.Look.DJINN)") && cfg.contains("defineEnum(\"view\", FuegoleonArmRules.View.ALWAYS)"), "fuegoleonArm.look beside fuegoleonArm.view");
        assertTrue(cfg.contains("FuegoleonArmLook.set(ARM_LOOK.get())"), "the file is mirrored into the look flag");
        String main = src("NUSMP.java");
        assertTrue(main.contains("FuegoleonArmSparks.init()"), "the spark tick is wired in the client block");
        assertTrue(src("book/SpiritDiveBoot.java").contains("FuegoleonArmDjinnSelfTests.register()"), "the self-test is registered");
        assertTrue(src("book/FuegoleonArmDjinnSelfTests.java").contains("\"magic.fuegoleon_arm_djinn\""), "the self-test prefix");
        String fp = src("client/aura/FireArmFirstPerson.java");
        assertTrue(fp.contains("WaterAuraRig.slim(") && src("client/aura/FuegoleonArmDjinn.java").contains("slim"), "the slim (Alex) arm is handled in both views");
        assertTrue(fp.contains("RenderArmEvent") && fp.contains("RenderHandEvent"), "the first person hand still uses the arm and hand events");
    }

    @Test
    void noHungerIsTouched() {
        for (String f : new String[]{"book/FuegoleonArmDjinnRules.java", "book/FuegoleonArmDjinnSelfTests.java", "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmSparks.java"}) {
            String c = src(f);
            assertFalse(c.contains("causeFoodExhaustion") || c.contains("getFoodData") || c.contains("setFoodLevel"), f + " touches hunger");
        }
    }
}
