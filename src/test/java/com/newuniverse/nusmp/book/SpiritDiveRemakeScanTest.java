package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
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
 * Why: nothing of the Spirit Dive remake can be profiled here, so its cost and its rules are pinned the only way they can be: by reading the real files. (1) Budget: for every element, stage and tier the cubes of
 * the model files of the pieces are summed (24 vertices a cube, two passes) and must stay under {@link SpiritDiveTier#pieceCap} and, with the four skin passes and the effects, under
 * {@link SpiritDiveTier#vertexCap}. (2) Server safety: the common classes of the remake name no client class. (3) Frame cost: the painters allocate nothing and build no texture location or string in
 * their frame methods, and ask for their blend only through AuraRender (the SafeAdditive rule). (4) Every texture, model, particle theme and lang key the code names exists. (5) No hunger.
 */
class SpiritDiveRemakeScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/nusmp");

    private static String read(Path p) {
        try { return Files.readString(p, StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + p, e); }
    }

    private static String code(String src) {
        return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    // ------------------------------------------------------------------ 1. the budget of the piece lists
    private static int cubes(String id) {
        String stem = SpiritDivePieces.key(id) + "_arm_" + SpiritDivePieces.name(id);
        Matcher m = Pattern.compile("\"origin\"").matcher(read(ASSETS.resolve("geo/entity/" + stem + ".geo.json")));
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** 24 vertices a cube and two passes (the body and its glow map). */
    private static int vertices(List<String> ids) {
        int n = 0;
        for (String id : ids) n += cubes(id) * 24 * 2;
        return n;
    }

    @Test
    void everyPieceListStaysUnderTheVertexCapOfItsTier() {
        List<String> over = new ArrayList<>();
        for (Element e : Element.values())
            for (Stage s : Stage.values())
                for (Grade g : Grade.values()) {
                    boolean character = SpiritDiveTier.character(e, g);
                    int v = vertices(SpiritDivePieces.pieces(e, s, g));
                    if (v > SpiritDiveTier.pieceCap(character)) over.add(e + "/" + s + "/" + g + " pieces " + v + " > " + SpiritDiveTier.pieceCap(character));
                    int total = v + 4 * 12 * 24 + SpiritDiveTier.worstQuads(character) * 4;                    // four passes of the mana skin (twelve boxes at most) and the effects
                    if (total > SpiritDiveTier.vertexCap(character)) over.add(e + "/" + s + "/" + g + " total " + total + " > " + SpiritDiveTier.vertexCap(character));
                }
        assertTrue(over.isEmpty(), "over the budget: " + over);
    }

    @Test
    void theCharacterTierAddsPiecesAndTheStandardTierIsTheLighterOne() {
        for (Element e : new Element[]{Element.WIND, Element.FIRE, Element.WATER}) {
            Grade g = SpiritDiveTier.gradeOfElement(e);
            int standard = vertices(SpiritDivePieces.pieces(e, Stage.FULL, Grade.STANDARD)), character = vertices(SpiritDivePieces.pieces(e, Stage.FULL, g));
            assertTrue(standard <= SpiritDiveTier.pieceCap(false), e + " standard " + standard);
            assertTrue(character <= SpiritDiveTier.pieceCap(true), e + " character " + character);
            assertTrue(SpiritDivePieces.pieces(e, Stage.FULL, g).size() > 0);
        }
    }

    // ------------------------------------------------------------------ 2. server safety
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");
    private static final String[] COMMON = {"book/SpiritDiveTier.java", "book/SpiritDivePieces.java", "book/SpiritDiveForm.java", "book/SpiritDiveConfig.java", "book/SpiritDiveBoot.java",
            "book/SpiritDiveSelfTests.java", "book/FuegoleonArm.java", "book/FuegoleonArmRules.java"};

    @Test
    void theCommonClassesOfTheRemakeNameNoClientClass() {
        List<String> bad = new ArrayList<>();
        for (String f : COMMON) if (CLIENT.matcher(code(read(JAVA.resolve(f)))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
    }

    @Test
    void theArmAndTheTierStateIsPerPlayerAndNothingIsStaticallyCurrent() {
        for (String f : new String[]{"book/FuegoleonArm.java", "book/SpiritDiveForm.java", "book/SpiritDiveSelfTests.java"}) {
            String c = code(read(JAVA.resolve(f)));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), f + " keeps a static current player");
        }
        String arm = code(read(JAVA.resolve("book/FuegoleonArm.java")));
        assertFalse(arm.contains("HashMap") || arm.contains("static final Map"), "the arm keeps its own per-player table (PlayerAuras already does, keyed by UUID and dropped at logout)");
        assertTrue(arm.contains("FuegoleonArmRules.due("), "the arm is checked on its own turn, not every tick");
    }

    @Test
    void theArmIsCheckedByCharacterFirstSoEveryOtherPlayerPaysOneRead() {
        String arm = code(read(JAVA.resolve("book/FuegoleonArm.java")));
        int fuego = arm.indexOf("CharacterSpells.characterOf(p)"), contract = arm.indexOf("SpiritLordSkill.bondedType(p)"), link = arm.indexOf("FireCasting.salamanderOut(p)");
        assertTrue(fuego > 0 && fuego < contract && fuego < link, "the grimoire is read before the spirit lookups");
        assertTrue(arm.indexOf("if (!fuegoleon) return") > fuego && arm.indexOf("if (!fuegoleon) return") < contract, "every other player exits right after the grimoire read");
    }

    // ------------------------------------------------------------------ 3. the frame cost
    private static final String[] PAINTERS = {"client/aura/SpiritDiveFx.java", "client/aura/FuegoleonArmFx.java", "client/aura/FuegoleonArmAura.java", "client/aura/FireArmFirstPerson.java",
            "client/aura/SpiritDiveAura.java", "client/aura/DiveParticles.java", "client/aura/SpiritDiveFrame.java"};
    private static final Pattern METHOD = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final)\\s+)+[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}=]*)\\)\\s*(?:throws [\\w., ]+)?\\{");

    /** The body of every method of a file (a small brace matcher; strings and comments are blanked first). */
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
                + "|ResourceLocation\\.(?:fromNamespaceAndPath|parse|withDefaultNamespace|tryParse)\\(|String\\.format\\(|AuraRender\\.tex\\(|GeoSpec\\.of\\(|\\.stream\\(\\)|Axis\\.\\w+\\.rotation");
        List<String> bad = new ArrayList<>();
        for (String f : PAINTERS)
            for (String[] m : methods(read(JAVA.resolve(f)))) {
                if (m[0].equals("register") || m[0].equals("spec") || m[0].startsWith("<")) continue;                 // one-off helpers cached in statics
                Matcher a = alloc.matcher(m[1]);
                if (a.find()) bad.add(f + " " + m[0] + ": " + a.group());
            }
        // GeoSpec.of is the one cached allocation (spec(Def) keeps it in a map); FireArmFirstPerson and FuegoleonArmAura keep theirs in static fields.
        bad.removeIf(x -> x.contains("SpiritDiveAura.java spec:") || x.contains("DiveParticles.java") && x.contains("onClientTick") && x.contains("new "));
        assertTrue(bad.isEmpty(), "allocation or a built location / string in a frame method: " + bad);
    }

    @Test
    void theHandAndTheDiveHandOutNoRenderTypeOfTheirOwn() {
        for (String f : PAINTERS) {
            String c = code(read(JAVA.resolve(f)));
            assertFalse(c.contains("RenderType.eyes(") || c.contains("entityTranslucentEmissive") || c.contains("RenderType.lightning"), f + " builds an additive type itself (use AuraRender.additive: the SafeAdditive rule)");
        }
    }

    @Test
    void everyBufferAFrameMethodTakesIsAdditiveThroughAuraRender() {
        String fx = code(read(JAVA.resolve("client/aura/SpiritDiveFx.java"))), arm = code(read(JAVA.resolve("client/aura/FuegoleonArmFx.java")));
        assertTrue(fx.contains("AuraRender.additive(tex)") && arm.contains("AuraRender.additive(tex)"));
    }

    @Test
    void theDiveCountsEveryEffectFromTheBudgetAndFromNothingElse() {
        String fx = code(read(JAVA.resolve("client/aura/SpiritDiveFx.java")));
        for (String need : new String[]{"b.columns()", "b.pillars()", "b.motes()", "b.bits()", "b.strands()", "b.rings()", "b.flares()"})
            assertTrue(fx.contains(need), "the painter ignores the budget field " + need);
        assertTrue(fx.contains("SpiritDiveTier.budget(character, lod)"));
        String aura = code(read(JAVA.resolve("client/aura/SpiritDiveAura.java")));
        assertTrue(aura.contains("SpiritDiveTier.lod("), "the remade look picks its level of detail by distance");
        assertTrue(aura.contains("SpiritDiveFrame.admitCharacter()"), "the character tier is capped per frame");
        assertTrue(aura.contains("SpiritDiveLook.classic()"), "the classic look is still there behind the client option");
    }

    // ------------------------------------------------------------------ 4. the names the code uses exist
    @Test
    void everyTextureTheRemakeNamesExists() {
        List<String> missing = new ArrayList<>();
        Pattern tex = Pattern.compile("AuraRender\\.tex\\(\"([a-z0-9_/]+)\"\\)");
        for (String f : PAINTERS) {
            Matcher m = tex.matcher(read(JAVA.resolve(f)));
            while (m.find()) if (!Files.isRegularFile(ASSETS.resolve("textures/" + m.group(1) + ".png"))) missing.add(f + " " + m.group(1));
        }
        assertTrue(missing.isEmpty(), "missing textures: " + missing);
    }

    @Test
    void theFlameArmModelAndItsAnimationClipExist() throws IOException {
        for (String rel : new String[]{"geo/entity/spiritdive_arm_flame_arm_r.geo.json", "textures/entity/spiritdive_arm_flame_arm_r.png", "textures/entity/spiritdive_arm_flame_arm_r_glow.png",
                "animations/entity/spiritdive_arm_flame_arm_r.animation.json"})
            assertTrue(Files.isRegularFile(ASSETS.resolve(rel)), rel);
        assertTrue(read(ASSETS.resolve("animations/entity/spiritdive_arm_flame_arm_r.animation.json")).contains("\"idle\""), "the painters play the clip idle");
    }

    @Test
    void everyParticleThemeTheTrailNamesIsInTheThemeTable() {
        String themes = read(ROOT.resolve("src/main/resources/data/nusmp/particle_themes.json"));
        Matcher m = Pattern.compile("ThemeKey\\.of\\(\"(spirit_dive\\.[a-z]+)\"\\)").matcher(read(JAVA.resolve("client/aura/DiveParticles.java")));
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(themes.contains("\"" + m.group(1) + "\""), "no particle theme " + m.group(1));
        }
        assertEquals(7, n, "wind, water, fire, earth and the three characters");
        String gen = read(ROOT.resolve("tools/particles/gen_themes.py"));
        assertTrue(gen.contains("\"spirit_dive.fuegoleon\""), "the generator that writes the theme table knows the themes (the table is not hand edited)");
    }

    @Test
    void theLangFragmentIsFlatJsonAndHasEveryKeyTheCodeUses() throws IOException {
        String frag = read(ROOT.resolve("tools/lang/spirit_dives.json"));
        Matcher keys = Pattern.compile("\"(nusmp\\.spiritdive\\.[a-z.]+)\"\\s*:\\s*\"[^\"]+\"").matcher(frag);
        List<String> have = new ArrayList<>();
        while (keys.find()) have.add(keys.group(1));
        assertEquals(4, have.size());
        assertFalse(frag.contains("{\n  \"") && frag.indexOf('{', 1) > 0, "a flat key -> text file");
        Matcher arm = Pattern.compile("translatableWithFallback\\(\"(nusmp\\.spiritdive\\.[a-z.]+)\"").matcher(read(JAVA.resolve("book/FuegoleonArm.java")));
        assertTrue(arm.find() && have.contains(arm.group(1)), "the arm's line is not in the fragment");
        assertTrue(read(JAVA.resolve("book/SpiritDiveForm.java")).contains("\"nusmp.spiritdive.tier.\" + g.name()"), "the tier lines are keyed by the grade name");
        for (Grade g : new Grade[]{Grade.YUNO, Grade.FUEGOLEON, Grade.NOELLE}) assertTrue(have.contains("nusmp.spiritdive.tier." + g.name().toLowerCase()), "no line for " + g);
    }

    @Test
    void theNewConfigFilesHaveTheirOwnNamesAndTheRightSide() {
        String boot = code(read(JAVA.resolve("book/SpiritDiveBoot.java"))), client = code(read(JAVA.resolve("client/aura/DiveClient.java")));
        assertTrue(boot.contains("ModConfig.Type.SERVER") && boot.contains("\"nusmp-spiritdive-server.toml\""), "what changes gameplay or tells the clients what to draw is a SERVER config");
        assertTrue(client.contains("ModConfig.Type.CLIENT") && client.contains("\"nusmp-spiritdive-client.toml\""), "what only changes looks is a CLIENT config");
    }

    // ------------------------------------------------------------------ 5. no hunger, and the old look is kept
    @Test
    void theRemakeNeverTouchesFoodOrHunger() {
        List<String> files = new ArrayList<>(List.of(COMMON));
        files.addAll(List.of(PAINTERS));
        files.addAll(List.of("client/aura/DiveClient.java", "client/aura/DiveClientConfig.java", "client/aura/SpiritDiveLook.java"));
        for (String f : files) {
            String c = code(read(JAVA.resolve(f)));
            for (String bad : new String[]{"causeFoodExhaustion", "FoodData", "getFoodData", "setFoodLevel", "addExhaustion", "setSaturation"})
                assertFalse(c.contains(bad), f + " touches hunger: " + bad);
        }
    }

    @Test
    void theOldLookIsStillRegisteredAndTheOldListsStillExist() {
        for (Element e : Element.values())
            for (Stage s : Stage.values()) assertFalse(SpiritDivePieces.pieces(e, s).isEmpty(), "the classic list of " + e + "/" + s + " is gone");
        assertTrue(SpiritDivePieces.pieces(Element.WATER, Stage.FULL).contains("spiritdive:saint_halo"), "the Saint Valkyrie Dress of the classic look");
        assertTrue(SpiritDivePieces.pieces(Element.FIRE, Stage.FULL).contains("spiritdive:fire_claws_r"), "the classic fire dive keeps its claws");
        String aura = code(read(JAVA.resolve("client/aura/SpiritDiveAura.java")));
        assertTrue(aura.contains("private static void extras(") && aura.contains("shell(c, el, st, pulse, false, false, false)"), "the classic passes are still in the painter");
    }
}
