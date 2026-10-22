package com.newuniverse.nusmp.corpse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data tests of Rades's undead: every model, animation, skin and glow map exists and fits together (bones the animations move exist, the texture size is the model's, the glow map has no
 * colour under alpha 0, the stage skins differ), the lang fragment has the names and the page texts, the undead / smite / immune tags list every body, the entity types and the pages are
 * wired in the sources, and the owner's rules (old ids kept, new pages last, zombies behind the config flag) are still in the code.
 */
class RadesUndeadAssetsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/nusmp");
    private static final Path DATA = ROOT.resolve("src/main/resources/data");
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final String[] MODELS = {"jimmy", "david", "alfredo", "carl", "michael"};

    private static String text(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(text(p)).getAsJsonObject(); }

    @Test
    void everyModelHasItsFilesAndFourSkinsWithGlow() {
        for (String m : MODELS) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("geo/entity/rades_" + m + ".geo.json")), m + " geo");
            assertTrue(Files.isRegularFile(ASSETS.resolve("animations/entity/rades_" + m + ".animation.json")), m + " animation");
            for (String suffix : new String[]{"", "_s1", "_s2", "_s3"}) {
                assertTrue(Files.isRegularFile(ASSETS.resolve("textures/entity/rades_" + m + suffix + ".png")), m + suffix + " texture");
                assertTrue(Files.isRegularFile(ASSETS.resolve("textures/entity/rades_" + m + suffix + "_glow.png")), m + suffix + " glow");
            }
        }
    }

    @Test
    void theModelsAreRealBoxModelsAndTheAnimationsMoveOnlyTheirBones() throws IOException {
        for (String m : MODELS) {
            JsonObject geo = json(ASSETS.resolve("geo/entity/rades_" + m + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            assertEquals("geometry.rades_" + m, geo.getAsJsonObject("description").get("identifier").getAsString());
            int tw = geo.getAsJsonObject("description").get("texture_width").getAsInt(), th = geo.getAsJsonObject("description").get("texture_height").getAsInt();
            Set<String> bones = new HashSet<>();
            int cubes = 0;
            for (JsonElement b : geo.getAsJsonArray("bones")) {
                JsonObject bo = b.getAsJsonObject();
                bones.add(bo.get("name").getAsString());
                if (bo.has("parent")) assertTrue(bones.contains(bo.get("parent").getAsString()), m + ": the parent comes before the child");
                cubes += bo.getAsJsonArray("cubes").size();
            }
            assertTrue(cubes >= 25, m + " is built from boxes: " + cubes);
            for (String suffix : new String[]{"", "_s1", "_s2", "_s3"}) {
                BufferedImage tex = ImageIO.read(ASSETS.resolve("textures/entity/rades_" + m + suffix + ".png").toFile());
                BufferedImage glow = ImageIO.read(ASSETS.resolve("textures/entity/rades_" + m + suffix + "_glow.png").toFile());
                assertEquals(tw, tex.getWidth(), m + suffix);
                assertEquals(th, tex.getHeight(), m + suffix);
                assertEquals(tw, glow.getWidth(), m + suffix + " glow");
                assertEquals(th, glow.getHeight(), m + suffix + " glow");
            }
            JsonObject anims = json(ASSETS.resolve("animations/entity/rades_" + m + ".animation.json")).getAsJsonObject("animations");
            for (String clip : new String[]{"idle", "walk", "run", "attack", "hurt", "death"}) assertTrue(anims.has(clip), m + " has the clip " + clip);
            if (!m.equals("jimmy")) assertTrue(anims.has("cast"), m + " has the cast clip (its signature wind-up)");
            for (String clip : anims.keySet()) {
                JsonObject c = anims.getAsJsonObject(clip);
                assertTrue(c.get("animation_length").getAsDouble() > 0, m + "/" + clip);
                for (String bone : c.getAsJsonObject("bones").keySet()) assertTrue(bones.contains(bone), m + "/" + clip + " moves a bone that does not exist: " + bone);
            }
        }
    }

    @Test
    void theGlowMapsAreSafeForTheAdditivePass() throws IOException {
        // the white-square rule: a pixel that is fully transparent carries no colour (alpha 0 adds nothing in any blend); and a glow map is not a solid card
        for (String m : MODELS) {
            for (String suffix : new String[]{"", "_s1", "_s2", "_s3"}) {
                BufferedImage g = ImageIO.read(ASSETS.resolve("textures/entity/rades_" + m + suffix + "_glow.png").toFile());
                int lit = 0, total = g.getWidth() * g.getHeight();
                for (int y = 0; y < g.getHeight(); y++)
                    for (int x = 0; x < g.getWidth(); x++) {
                        int argb = g.getRGB(x, y);
                        if ((argb >>> 24) == 0) assertEquals(0, argb & 0xFFFFFF, m + suffix + " glow has colour under alpha 0 at " + x + "," + y);
                        else lit++;
                    }
                assertTrue(lit > 0, m + suffix + " glow is empty");
                assertTrue(lit < total * 0.5, m + suffix + " glow lights more than half of the sheet (a solid card): " + lit + "/" + total);
            }
        }
    }

    @Test
    void theStageSkinsDifferFromTheFirst() throws IOException {
        for (String m : MODELS) {
            BufferedImage base = ImageIO.read(ASSETS.resolve("textures/entity/rades_" + m + ".png").toFile());
            for (String suffix : new String[]{"_s1", "_s2", "_s3"}) {
                BufferedImage s = ImageIO.read(ASSETS.resolve("textures/entity/rades_" + m + suffix + ".png").toFile());
                long diff = 0;
                for (int y = 0; y < base.getHeight(); y++) for (int x = 0; x < base.getWidth(); x++) if (base.getRGB(x, y) != s.getRGB(x, y)) diff++;
                assertTrue(diff > 20, m + suffix + " is the same as the first skin");
            }
        }
    }

    @Test
    void theLangFragmentNamesEveryBodyAndPage() throws IOException {
        JsonObject lang = json(ROOT.resolve("tools/lang/rades_undead.json"));
        for (String k : new String[]{"entity.nusmp.soul_zombie", "entity.nusmp.corpse_david", "entity.nusmp.corpse_michael_caesal",
                "nusmp.page.book_soul_corpse.corpse_jimmy", "nusmp.page.book_soul_corpse.corpse_david", "nusmp.page.book_soul_corpse.corpse_alfredo",
                "nusmp.page.book_soul_corpse.corpse_carl", "nusmp.page.book_soul_corpse.corpse_michael_caesal", "nusmp.page.book_soul_corpse.corpse_reanimate",
                "nusmp.page.book_soul_corpse.corpse_marionette", "nusmp.page.book_soul_corpse.corpse_legion", "nusmp.skill.book_soul_corpse.description"})
            assertTrue(lang.has(k) && !lang.get(k).getAsString().isBlank(), "missing lang key " + k);
        assertTrue(lang.get("nusmp.page.book_soul_corpse.corpse_michael_caesal").getAsString().startsWith("No.0 - Michael Caesal"));
        assertTrue(lang.get("nusmp.page.book_soul_corpse.corpse_david").getAsString().startsWith("No.3 - David"));
        for (String k : lang.keySet()) assertFalse(lang.get(k).getAsString().toLowerCase().contains("claude"), k);
    }

    @Test
    void theTagsListEveryBody() throws IOException {
        String[] bodies = {"nusmp:soul_zombie", "nusmp:corpse_jimmy", "nusmp:corpse_david", "nusmp:corpse_alfredo", "nusmp:corpse_carl", "nusmp:corpse_michael_caesal"};
        for (String tag : new String[]{"minecraft/tags/entity_type/undead.json", "minecraft/tags/entity_type/sensitive_to_smite.json"}) {
            JsonObject t = json(DATA.resolve(tag));
            assertFalse(t.get("replace").getAsBoolean(), tag + " must only add");
            Set<String> have = new HashSet<>();
            for (JsonElement v : t.getAsJsonArray("values")) have.add(v.getAsString());
            for (String b : bodies) assertTrue(have.contains(b), tag + " misses " + b);
        }
        String immune = text(DATA.resolve("nusmp/tags/entity_type/corpse_immune.json"));
        for (String b : bodies) assertTrue(immune.contains(b), "corpse_immune misses " + b);
        for (String old : new String[]{"minecraft:ender_dragon", "minecraft:wither", "nusmp:floor_warden"}) assertTrue(immune.contains(old), "an old entry of corpse_immune was removed: " + old);
    }

    @Test
    void theEntitiesAndPagesAreWiredAndTheOldIdsStay() throws IOException {
        String entities = text(SRC.resolve("corpse/CorpseEntities.java"));
        for (String id : new String[]{"\"soul_zombie\"", "NamedCorpses.DAVID", "NamedCorpses.MICHAEL_CAESAL", "NamedCorpses.JIMMY", "NamedCorpses.ALFREDO", "NamedCorpses.CARL"}) assertTrue(entities.contains(id), id);
        String ext = text(SRC.resolve("book/ext/SoulCorpseExt.java"));
        for (String id : new String[]{"\"corpse_reanimate\"", "\"corpse_jimmy\"", "\"corpse_alfredo\"", "\"corpse_carl\"", "\"true_soul_corpse\"", "\"corpse_david\"", "\"corpse_michael_caesal\""}) assertTrue(ext.contains(id), id);
        assertTrue(ext.indexOf("\"true_soul_corpse\"") < ext.indexOf("\"corpse_david\"") && ext.indexOf("\"corpse_david\"") < ext.indexOf("\"corpse_michael_caesal\""), "David and Michael Caesal are appended last");
        for (String name : new String[]{"No.4 - Jimmy", "No.3 - David", "No.2 - Alfredo", "No.1 - Carl", "No.0 - Michael Caesal"}) assertTrue(ext.contains("\"" + name + "\""), name);
        String book = text(SRC.resolve("book/SoulCorpseBook.java"));
        assertTrue(book.indexOf("\"corpse_legion\"") < book.indexOf("latePages()"), "the two late pages come after every page that already existed, so no mode number moves");
        String locks = text(SRC.resolve("book/CharacterSpells.java"));
        for (String id : new String[]{"corpse_jimmy", "corpse_david", "corpse_alfredo", "corpse_carl", "corpse_michael_caesal", "true_soul_corpse"}) assertTrue(locks.contains("\"book_soul_corpse\", \"" + id + "\", CanonBook.RADES"), id);
        String policy = text(DATA.resolve("nusmp/armour_policy.json"));
        assertTrue(policy.contains("\"corpse_david\"") && policy.contains("\"corpse_michael_caesal\""), "the armour policy lists the appended pages");
    }

    @Test
    void theZombiesAreBehindTheFlagAndNeverBurn() throws IOException {
        String cfg = text(SRC.resolve("corpse/CorpseConfig.java"));
        assertTrue(cfg.contains("\"zombiesInsteadOfPuppets\"") && cfg.contains(".define(\"zombiesInsteadOfPuppets\", true)"), "the flag exists and defaults to true");
        String book = text(SRC.resolve("book/SoulCorpseBook.java"));
        assertTrue(book.contains("CorpseConfig.zombiesInsteadOfPuppets()") && book.contains("SongRebuildKit.dismiss("), "the old puppet props stay reachable behind the flag");
        String corpses = text(SRC.resolve("corpse/Corpses.java"));
        assertTrue(corpses.contains("CorpseConfig.zombiesInsteadOfPuppets()") && corpses.contains("Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional"), "Reanimation keeps its old path behind the flag");
        String z = text(SRC.resolve("corpse/SoulZombieEntity.java"));
        assertTrue(z.contains("isSunSensitive() { return SoulZombieRules.SUN_BURNS; }") && z.contains("isSunBurnTick() { return false; }"), "no sun burn");
        assertTrue(z.contains("shouldDropLoot() { return false; }") && z.contains("shouldBeSaved() { return false; }") && z.contains("convertsInWater() { return false; }"));
        assertFalse(z.contains("ParticleTypes"), "custom particles only");
        assertFalse(text(SRC.resolve("corpse/SoulZombies.java")).contains("ParticleTypes"), "custom particles only");
    }

    @Test
    void theNewCodeKeepsToTheRules() throws IOException {
        for (String f : new String[]{"corpse/SoulZombieEntity.java", "corpse/SoulZombies.java", "corpse/SoulZombieRules.java", "corpse/RadesUndeadSelfTests.java", "book/CanonSkillSync.java", "grimoire/CanonNaming.java"}) {
            String s = text(SRC.resolve(f));
            assertFalse(s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("nusmp.client."), f + " is common code: no client class");
            assertFalse(s.contains("Minecraft.getInstance()"), f);
            assertFalse(s.contains("RenderType.eyes"), f);
            assertFalse(s.toLowerCase().contains("hunger") || s.contains("FoodData") || s.contains("causeFoodExhaustion"), f + ": no hunger effects");
        }
        String named = text(SRC.resolve("corpse/NamedCorpseEntity.java"));
        assertFalse(named.contains("getMaxHealth() *") && named.contains("percent"), "no percent-of-max-health damage in the named undead");
        assertTrue(text(SRC.resolve("client/corpse/RadesUndeadRenderer.java")).contains("ZogratisGlow.grey"), "the glow pass goes through the geo drawer's additive helper");
        assertFalse(text(SRC.resolve("client/corpse/RadesUndeadRenderer.java")).contains("RenderType.eyes"), "never RenderType.eyes");
    }
}
