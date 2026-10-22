package com.newuniverse.nusmp.shishou;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The files Shishou needs exist and say the same: the 3D mesh, the two skins, the inventory icon, the item models, the animation clips of the four states, the far-LOD proxy and the lang text of every skill. */
class ShishouAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void theMeshTheSkinsAndTheIconExist() throws IOException {
        Path mesh = ASSETS.resolve("weapon_meshes/shishou.txt");
        assertTrue(Files.isRegularFile(mesh), "the 3D mesh");
        List<String> lines = Files.readAllLines(mesh);
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("place ")) && lines.stream().anyMatch(l -> l.startsWith("qn ")), "the mesh has its placement and its quads");
        assertTrue(Files.size(mesh) < 2_500_000L, "the mesh stays small: " + Files.size(mesh));
        BufferedImage skin = ImageIO.read(ASSETS.resolve("textures/entity/weapon/shishou.png").toFile());
        BufferedImage glow = ImageIO.read(ASSETS.resolve("textures/entity/weapon/shishou_glow.png").toFile());
        assertEquals(skin.getWidth(), glow.getWidth());
        assertEquals(skin.getHeight(), glow.getHeight());
        BufferedImage icon = ImageIO.read(ASSETS.resolve("textures/item/shishou.png").toFile());
        assertEquals(32, icon.getWidth());
        assertEquals(32, icon.getHeight());
        int opaque = 0;
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) if ((icon.getRGB(x, y) >>> 24) > 200) opaque++;
        assertTrue(opaque > 40, "the icon draws a sword: " + opaque);
    }

    @Test
    void theItemModelsAreTheOldSpriteInTheInventoryAndTheMeshInHand() throws IOException {
        JsonObject m = json(ASSETS.resolve("models/item/shishou.json"));
        assertEquals("minecraft:builtin/entity", m.get("parent").getAsString(), "the in-hand model is drawn by the mod's weapon renderer (the mesh)");
        assertTrue(m.getAsJsonObject("display").has("gui") && m.getAsJsonObject("display").has("thirdperson_righthand"), "display transforms for the hand and the inventory");
        JsonObject icon = json(ASSETS.resolve("models/item/shishou_icon.json"));
        assertEquals("nusmp:item/shishou", icon.getAsJsonObject("textures").get("layer0").getAsString(), "the icon model is the sprite");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/nusmp/weapon_attributes/shishou.json")), "the Better Combat attributes");
    }

    @Test
    void theAnimationFileHasTheFourStateClipsWithTheSwordBone() throws IOException {
        JsonObject a = json(ASSETS.resolve("animations/entity/shishou_sword.animation.json")).getAsJsonObject("animations");
        for (String state : ShishouRules.STATE_CLIPS) {
            JsonObject clip = a.getAsJsonObject("animation.shishou." + state);
            assertNotNull(clip, "clip " + state);
            assertTrue(clip.getAsJsonObject("bones").has("sword"), state + " moves the sword bone");
        }
        assertTrue(a.getAsJsonObject("animation.shishou.idle").get("loop").getAsBoolean(), "idle loops");
        assertEquals("hold_on_last_frame", a.getAsJsonObject("animation.shishou.dormant").get("loop").getAsString(), "dormant settles into the ground and holds");
        assertEquals("hold_on_last_frame", a.getAsJsonObject("animation.shishou.attack").get("loop").getAsString(), "the attack plays once");
    }

    @Test
    void theFarProxyModelAndItsTexturesExist() throws IOException {
        JsonObject geo = json(ASSETS.resolve("geo/entity/shishou_sword.geo.json"));
        assertTrue(geo.toString().contains("minecraft:geometry"), "a geo file");
        assertTrue(Files.isRegularFile(ASSETS.resolve("textures/entity/shishou_sword.png")) && Files.isRegularFile(ASSETS.resolve("textures/entity/shishou_sword_glow.png")));
    }

    @Test
    void everySkillHasItsLangTextAndAJapaneseName() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/shishou.json"));
        for (String k : new String[]{"item.nusmp.shishou", "entity.nusmp.shishou", "effect.nusmp.black_flame", "shishou.grade", "key.nusmp.shishou_throw", "key.nusmp.shishou_cast", "key.nusmp.shishou_next"})
            assertTrue(lang.has(k), "lang: " + k);
        assertEquals("Akashic Relic - Grade: EX-Singularity", lang.get("shishou.grade").getAsString().replace("–", "-").replace("—", "-"));
        for (ShishouSkill s : ShishouSkill.values()) {
            assertTrue(lang.has(s.langKey()), "lang: " + s.langKey());
            assertTrue(lang.has(s.langKey() + ".jp"), "lang: " + s.langKey() + ".jp");
            assertEquals(s.english(), lang.get(s.langKey()).getAsString(), s.name() + ": the English name is the enum's");
        }
        for (String v : lang.keySet()) {
            String t = lang.get(v).getAsString().toLowerCase();
            assertFalse(t.contains("claude") || t.contains("anthropic") || t.contains("assistant"), v + " names no model or assistant");
        }
    }

    @Test
    void theMovableHudHasItsGuiMoveText() throws IOException {
        assertTrue(json(Path.of("tools/lang/gui_move.json")).has("gui_move.element.shishou_hud"));
    }
}
