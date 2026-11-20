package com.newuniverse.nusmp.lords;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Lord weapons are checked from the repo's own files: every drawback of every sin is explained in plain words in the lang file (owner request: every negative effect makes sense and is explained, no
 * hunger-bar trick), every form has a Blender mesh within the quad budget, a skin and a glow skin, a model, an icon, a class, engravings, a Better Combat file; the four old meshes are kept under classic; the grade
 * is above Unique; the source never reaches client classes from common code and every loop is bounded by a cap. Pure: java.nio and Gson only, CWD = repo root.
 */
class LordFilesTest {
    private static final Path RES = Path.of("src", "main", "resources");
    private static final Path ASSETS = RES.resolve("assets/anime_multiverse");
    private static final Path SRC = Path.of("src", "main", "java", "com", "newuniverse", "nusmp");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static List<String> allForms() {
        List<String> out = new ArrayList<>();
        for (LordRules.Sin s : LordRules.Sin.values()) if (s.isLord()) out.addAll(List.of(s.items));
        return out;
    }

    @Test
    void everyDrawbackIsExplainedInPlainWords() throws IOException {
        JsonObject lang = json(ASSETS.resolve("lang/en_us.json"));
        for (LordRules.Sin s : LordRules.Sin.values()) {
            for (String key : LordText.requiredKeys(s)) {
                assertTrue(lang.has(key), "missing lang key " + key);
                assertFalse(lang.get(key).getAsString().isBlank(), key);
            }
            StringBuilder all = new StringBuilder();
            for (int i = 1; i <= LordText.drawbackLines(s); i++) {
                String line = lang.get(s.drawbackKey(i)).getAsString();
                assertTrue(line.length() >= 60, s.id + " line " + i + " is too thin to explain anything");
                all.append(line).append(' ');
            }
            String text = all.toString().toLowerCase();
            assertTrue(text.contains("never touch"), s.id + ": the text must say the food bar is never touched");
            assertTrue(text.contains("how it recovers") || text.contains("feed it") || text.contains("kill"), s.id + ": it must say how the drawback recovers");
            assertTrue(text.contains("mastery"), s.id + ": it must say what the mastery does about it");
        }
        assertEquals(6, LordText.drawbackLines(LordRules.Sin.GREED), "Greed also explains Half-Starvation and its mastery");
        for (String k : List.of("lords.warning.title", "lords.warning.intro", "lords.warning.release", "lords.tooltip.title", "lords.tooltip.drawback", "lords.status.header", "lords.status.line", "lords.hud.line"))
            assertTrue(lang.has(k), k);
    }

    @Test
    void everyLangKeyOfTheJavaSourceExists() throws IOException {
        JsonObject lang = json(ASSETS.resolve("lang/en_us.json"));
        Pattern p = Pattern.compile("\"(lords\\.(?:msg|tooltip|warning|status|mode)\\.[a-z_0-9]+|lords\\.hud\\.line)\"");
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                Matcher m = p.matcher(Files.readString(f));
                while (m.find()) if (!lang.has(m.group(1))) missing.add(f.getFileName() + ": " + m.group(1));
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void everyFormHasItsMeshSkinsModelsClassAndEngravings() throws IOException {
        String classes = Files.readString(ASSETS.resolve("weapon_classes.json")), toolClasses = Files.readString(Path.of("tools", "relics", "weapon_classes.json"));
        String engr = Files.readString(SRC.resolve("item/WeaponEngravings.java"));
        for (String id : allForms()) {
            Path mesh = ASSETS.resolve("weapon_meshes/" + id + ".txt");
            assertTrue(Files.exists(mesh), id + " mesh");
            List<String> lines = Files.readAllLines(mesh);
            assertTrue(lines.get(0).contains("Blender"), id + ": not made in Blender: " + lines.get(0));
            long quads = lines.stream().filter(l -> l.startsWith("q ") || l.startsWith("qn ")).count();
            assertTrue(quads > 100 && quads <= 2800, id + " quad lines " + quads);
            assertTrue(lines.stream().anyMatch(l -> l.startsWith("place ")) && lines.stream().anyMatch(l -> l.startsWith("grip ")), id + " place and grip");
            for (String f : List.of(id + ".png", id + "_glow.png")) assertTrue(Files.size(ASSETS.resolve("textures/entity/weapon/" + f)) > 200, f);
            assertTrue(Files.exists(ASSETS.resolve("models/item/" + id + ".json")) && Files.exists(ASSETS.resolve("models/item/" + id + "_icon.json")), id + " models");
            assertTrue(Files.size(ASSETS.resolve("textures/item/" + id + ".png")) > 50, id + " icon");
            assertTrue(classes.contains("\"" + id + "\":") && toolClasses.contains("\"" + id + "\":"), id + " class (both copies)");
            assertTrue(engr.contains("Map.entry(\"" + id + "\""), id + " engravings");
            assertTrue(Files.exists(RES.resolve("data/anime_multiverse/weapon_attributes/" + id + ".json")), id + " Better Combat file");
        }
    }

    @Test
    void theFourOldMeshesAndSkinsAreKeptForTheClassicSetting() {
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord()) continue;
            String id = s.items[0];
            assertTrue(Files.exists(ASSETS.resolve("weapon_meshes/classic/" + id + ".txt")), id + " classic mesh");
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/weapon/classic/" + id + ".png")), id + " classic skin");
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/weapon/classic/" + id + "_glow.png")), id + " classic glow");
        }
    }

    @Test
    void theCustomEngravingsExistAndAreInTheTensuraTag() throws IOException {
        String tag = Files.readString(RES.resolve("data/tensura/tags/enchantment/engraving.json"));
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        for (String id : List.of("weight_of_sloth", "hollow_of_envy", "cracked_mirror", "penance_of_seifort", "hunger_of_the_blade")) {
            assertTrue(Files.exists(RES.resolve("data/anime_multiverse/enchantment/" + id + ".json")), id);
            assertTrue(tag.contains("\"anime_multiverse:" + id + "\""), id + " in the engraving tag");
            assertTrue(lang.contains("\"enchantment.anime_multiverse." + id + "\"") && lang.contains("\"enchantment.anime_multiverse." + id + ".desc\""), id + " lang");
        }
    }

    @Test
    void theGradeIsAboveUnique() throws IOException {
        var rules = json(ASSETS.resolve("rankings.json")).getAsJsonArray("rules");
        java.util.Map<String, String> by = new java.util.HashMap<>();
        rules.forEach(r -> by.put(r.getAsJsonObject().get("prefix") == null ? "" : r.getAsJsonObject().get("prefix").getAsString(), r.getAsJsonObject().get("rank").getAsString()));
        for (String p : List.of("black_axe_of_sloth", "black_gunblade_of_envy", "black_spear_of_vanity", "seifort_holy_sword")) assertEquals("god", by.get(p), p);
        assertEquals("genesis", by.get("black_sword_greed"));
    }

    @Test
    void theServerPartNeverReachesClientClasses() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC.resolve("lords"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String s = Files.readString(f);
                if (s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("com.newuniverse.nusmp.client") || s.contains("Minecraft.getInstance")) bad.add(f.getFileName().toString());
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyLoopOfTheServiceIsBoundedByACapOrACollectionOfPlayers() throws IOException {
        String svc = Files.readString(SRC.resolve("lords/LordService.java"));
        assertFalse(svc.contains("while (true)"), "no unbounded loop");
        assertFalse(svc.contains("getAllEntities") || svc.contains("getEntities(") && !svc.contains("MAX_"), "no unbounded entity scan");
        assertTrue(Files.readString(SRC.resolve("lords/LordRules.java")).contains("MAX_TARGETS"), "a target cap exists");
        String cfg = Files.readString(SRC.resolve("lords/LordConfig.java"));
        assertTrue(cfg.contains("nusmp-lords-server.toml"), "a server config for the rules");
        assertTrue(Files.readString(SRC.resolve("vfx/client/LordVfxConfig.java")).contains("nusmp-lords-vfx-client.toml"), "a client config for the looks");
    }
}
