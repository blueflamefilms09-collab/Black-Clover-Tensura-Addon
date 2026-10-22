package com.newuniverse.nusmp.book;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the parts of the Light and Bone Magic remake that are data and source, pinned by reading the real files (nothing here needs the game): the lang fragment names every page of both books in
 * English and Japanese, the textures the painters ask for exist, the preview scenes cover every variant of both prop kinds, the config switch exists, the armour policy lists the appended
 * pages, the painters and casts keep the cost rules (no allocation while painting, no direct render types, a guard before every delayed part, one search per pulse, no per-tick world scan in a
 * prop behaviour), nothing touches the hunger bar, and no common code reaches a client class.
 */
class LightBoneJaDataTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String src(String rel) throws IOException { return read(SRC.resolve(rel)); }

    /** The body of the first method called {@code name} (a brace matcher that skips strings and comments is not needed here: the painters keep their braces plain). */
    private static String method(String text, String name) {
        Matcher m = Pattern.compile("(?m)^\\s*(?:public |private |static |final )*[\\w<>\\[\\]]+\\s+" + name + "\\(").matcher(text);
        assertTrue(m.find(), "no method " + name);
        int open = text.indexOf('{', m.end()), depth = 0, i = open;
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return text.substring(open, i + 1);
    }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    // ================================================================ lang
    @Test
    void theLangFragmentNamesEveryPageInEnglishAndJapanese() throws IOException {
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/light_bone_ja.json"))).getAsJsonObject();
        for (String magic : new String[]{"LIGHT", "BONE"}) {
            String book = magic.equals("LIGHT") ? "book_light" : "book_bone";
            for (LightBoneJa.Spell s : LightBoneJa.spells(magic)) {
                String key = "nusmp.page." + book + "." + s.id();
                assertTrue(lang.has(key), "missing tooltip " + key);
                String text = lang.get(key).getAsString();
                assertTrue(text.contains(s.english()), key + " does not say the English name " + s.english());
                assertTrue(text.contains(s.kanji()), key + " does not say the Japanese name " + s.kanji());
                assertTrue(text.contains(s.romaji()), key + " does not say the romaji " + s.romaji());
            }
        }
        assertTrue(lang.has("nusmp.skill.book_light.description") && lang.has("nusmp.skill.book_bone.description"));
        lang.entrySet().forEach(e -> assertTrue(e.getValue().isJsonPrimitive() && !e.getValue().getAsString().isBlank(), e.getKey()));
        assertFalse(read(ROOT.resolve("tools/lang/light_bone_ja.json")).toLowerCase().contains("claude"), "no assistant names in lang text");
    }

    @Test
    void theDocumentTablesEverySpellWithItsJapaneseNameStatusAndTheUnverifiedList() throws IOException {
        String doc = read(ROOT.resolve("docs/magic/light_bone_ja.md"));
        for (String magic : new String[]{"LIGHT", "BONE"})
            for (LightBoneJa.Spell s : LightBoneJa.spells(magic)) {
                assertTrue(doc.contains("| `" + s.id() + "` | " + s.english() + " | " + s.kanji()), "the document has no row for " + s.id());
                assertTrue(doc.contains(s.status().name()), s.status().name());
            }
        assertTrue(doc.contains("Not verified") && doc.contains("could NOT be fetched") && doc.contains("magic.lightBoneJa"));
        assertTrue(Files.isDirectory(ROOT.resolve("docs/previews/light_bone_ja")), "the preview PNGs");
    }

    // ================================================================ assets
    @Test
    void everyTextureThePaintersAskForExistsAndTheSceneFileCoversEveryVariant() throws IOException {
        Path tex = ROOT.resolve("src/main/resources/assets/nusmp/textures");
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"client/prop/LightJaPropPainter.java", "client/prop/BoneJaPropPainter.java"}) {
            Matcher m = Pattern.compile("AuraRender\\.tex\\(\"([a-z0-9_/]+)\"\\)").matcher(src(f));
            int n = 0;
            while (m.find()) {
                n++;
                if (!Files.isRegularFile(tex.resolve(m.group(1) + ".png"))) missing.add(m.group(1));
            }
            assertTrue(n >= 4, f + " asks for its textures");
        }
        assertTrue(missing.isEmpty(), "missing textures: " + missing);
        String scenes = read(ROOT.resolve("tools/render_preview/scenes.d/light_bone_ja.json"));
        JsonObject o = JsonParser.parseString(scenes).getAsJsonObject();
        boolean[] light = new boolean[10], bone = new boolean[4];
        for (var e : o.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject s = e.getValue().getAsJsonObject();
            int v = s.get("param").getAsInt() & 0xFF;
            if (s.get("propKind").getAsString().equals("LIGHT_JA")) light[v] = true;
            else bone[v] = true;
        }
        for (int v = 0; v < light.length; v++) assertTrue(light[v], "no preview scene for the Light prop variant " + v);
        for (int v = 0; v < bone.length; v++) assertTrue(bone[v], "no preview scene for the Bone prop variant " + v);
    }

    @Test
    void theConfigSwitchAndTheArmourPolicyListsExist() throws IOException {
        String cfg = src("NUConfig.java");
        assertTrue(cfg.contains("LIGHT_BONE_JA") && cfg.contains("\"lightBoneJa\""), "the server config magic.lightBoneJa");
        assertTrue(cfg.contains("b.push(\"magic\")"));
        String policy = read(ROOT.resolve("src/main/resources/data/nusmp/armour_policy.json"));
        for (String id : List.of("bright_judgment_whip", "heaven_splitting_flash", "arrows_of_judgment", "avior_lamp", "absolute_monarch", "demon_sword_dainsleif", "spatial_rupture"))
            assertTrue(policy.contains("\"" + id + "\""), "the armour policy lists the appended page " + id);
        String kinds = src("prop/PropKind.java");
        assertTrue(kinds.indexOf("LIGHT_JA") > kinds.indexOf("CELESTIAL_2") && kinds.indexOf("BONE_JA") > kinds.indexOf("LIGHT_JA"), "new prop kinds are only appended");
    }

    // ================================================================ cost rules
    @Test
    void thePaintersAllocateNothingAndAskForNoRenderTypeDirectly() throws IOException {
        for (String f : new String[]{"client/prop/LightJaPropPainter.java", "client/prop/BoneJaPropPainter.java"}) {
            String text = code(src(f));
            String register = method(text, "register");
            String rest = text.replace(register, "").replaceAll("(?m)^\\s*private static final[^\\n]*$", "");        // the static scratch fields are made once
            assertFalse(rest.contains("fromNamespaceAndPath") || rest.contains("withDefaultNamespace") || rest.contains("AuraRender.tex("), f + ": a location is built outside register() (per frame)");
            assertFalse(rest.contains("new float[") || rest.contains("new double[") || rest.contains("new int[") || rest.contains("new Vec3") || rest.contains("new Vector3f"), f + ": an allocation while painting");
            assertFalse(rest.contains("RenderType.eyes") || rest.contains("entityTranslucentEmissive") || rest.contains("RenderType.lightning"), f + ": asks for an additive type directly (SafeAdditive only)");
            assertTrue(rest.contains("SafeAdditive.additive"), f);
            assertTrue(rest.contains("LightBoneJa.lod(") && rest.contains("LightBoneJa.seg("), f + ": no level of detail");
            assertTrue(rest.contains("PerfSettings.vfxQuality()"), f + ": ignores the video quality");
        }
        String light = code(src("client/prop/LightJaPropPainter.java"));
        for (String v : new String[]{"SWORD", "CROSS", "WHIP", "BEAM", "COLUMN", "ARROW", "ORB", "HEAL", "MONARCH", "GLORIA"})
            assertTrue(light.contains("case LightJaVariant." + v), "the painter draws " + v);
        String bone = code(src("client/prop/BoneJaPropPainter.java"));
        for (String v : new String[]{"FANG", "BULLET", "DAINSLEIF", "SLASH"}) assertTrue(bone.contains("case BoneJaVariant." + v), "the painter draws " + v);
    }

    @Test
    void theCastsGuardEveryDelayedPartSearchOncePerPulseAndNeverMoveAPropFromTheServer() throws IOException {
        for (String f : new String[]{"book/LightJaArts.java", "book/BoneJaArts.java"}) {
            String text = code(src(f));
            assertFalse(text.contains("SpellRuntime.later("), f + ": a delayed part that skips the probe and the guard");
            int later = count(text, "LightBoneJaKit.later("), here = count(text, "LightBoneJaKit.here(");
            assertTrue(here >= later - 1, f + ": " + later + " delayed parts but " + here + " guards");
            assertFalse(text.contains("Minecraft.getInstance") || text.contains("net.minecraft.client") || text.contains("nusmp.client"), f);
            assertFalse(text.contains("setDeltaMovement(0, -") || text.contains("e.setPos("), f + ": the server moves a flying prop");
            assertTrue(text.contains("LightBoneJa.MAX_HITS") || text.contains("nearest("), f + ": a cast without a hit cap");
        }
        for (String f : new String[]{"prop/LightJaProps.java", "prop/BoneJaProps.java"}) {
            String text = code(src(f));
            assertFalse(text.contains("getEntitiesOfClass") || text.contains("getEntities(") || text.contains("clip("), f + ": a world scan inside a prop behaviour");
        }
        assertTrue(src("book/LightBoneJaKit.java").contains("LightBoneJaBudget.admit"), "every prop goes through the budget");
        String budget = src("book/LightBoneJaBudget.java");
        assertTrue(budget.contains("LivingDeathEvent") && budget.contains("PlayerLoggedOutEvent") && budget.contains("PlayerChangedDimensionEvent"), "cleanup on death, logout and dimension change");
    }

    @Test
    void nothingTouchesTheHungerBarAndNoCommonClassReachesTheClient() throws IOException {
        for (String f : new String[]{"book/LightBoneJa.java", "book/LightBoneJaKit.java", "book/LightBoneJaBudget.java", "book/LightBoneJaSelfTests.java", "book/LightJaArts.java", "book/BoneJaArts.java",
                "book/BoneBook.java", "prop/LightJaProps.java", "prop/BoneJaProps.java", "prop/LightJaVariant.java", "prop/BoneJaVariant.java"}) {
            String text = code(src(f)).toLowerCase();
            assertFalse(text.contains("hunger") || text.contains("exhaust") || text.contains("fooddata") || text.contains("saturation"), f + ": touches the hunger bar");
            assertFalse(text.contains("net.minecraft.client") || text.contains("com.mojang.blaze3d") || text.contains("nusmp.client") || text.contains("minecraft.getinstance"), f + ": reaches a client class");
        }
    }

    @Test
    void theBooksAreSwitchedByTheConfigAndRebuiltWhenItChanges() throws IOException {
        String light = src("book/LightBook.java"), bone = src("book/BoneBook.java");
        assertTrue(light.contains("LightBoneJaKit.enabled()") && light.contains("pagesVersion()"));
        assertTrue(bone.contains("LightBoneJaKit.enabled()") && bone.contains("pagesVersion()"));
        assertTrue(light.contains("BookPage.starter(\"judgment\"") && light.contains("LightBook::rays"), "the old pages stay in the source");
        assertTrue(src("book/ElementBooks.java").contains("legacyBonePages()") && src("book/ElementBooks.java").contains("\"ossuary\""), "the old Bone pages stay in the source");
        String selfTest = src("book/LightBoneJaSelfTests.java");
        assertTrue(selfTest.contains("\"magic.light_bone_ja\""));
        assertTrue(src("prop/LightJaProps.java").contains("LightBoneJaSelfTests.register()"));
    }

    private static int count(String text, String what) {
        int n = 0, at = 0;
        while ((at = text.indexOf(what, at)) >= 0) { n++; at += what.length(); }
        return n;
    }
}
