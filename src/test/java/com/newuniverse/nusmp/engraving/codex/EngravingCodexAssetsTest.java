package com.newuniverse.nusmp.engraving.codex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleRegistry;
import com.newuniverse.nusmp.grimoire.BookMotif;
import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** The item's assets and data: the 3D book model (the grimoire geometry with its own look), the sprite icon, the recipe, the lang text and the GUI-size entry. */
class EngravingCodexAssetsTest {
    private static final File RES = new File("src/main/resources");
    private static final File TEX = new File(RES, "assets/nusmp/textures/item/grimoire_book");

    @Test
    void theLookIsAnEntryOfTheGrimoireCoreWithItsOwnTextures() {
        var look = EngravingCodexLook.LOOK;
        assertEquals(BookMotif.ENGRAVING, look.motif());
        assertEquals("cover_art_engraving", BookMotif.ENGRAVING.texture());
        assertEquals("cover_base_engraving", BookMotif.ENGRAVING.base());
        assertEquals(EngravingCodexLook.COVER, look.tint(0));
        assertEquals(EngravingCodexLook.TRIM, look.tint(1));
        assertEquals(EngravingCodexLook.EMBLEM, look.tint(2));
        assertEquals(0xFF000000 | EngravingCodexLook.COVER, EngravingCodexLook.argb(0));
        assertEquals(-1, EngravingCodexLook.argb(9));
        assertFalse(look.tattered());
        var plan = GrimoireBookPlan.build(look.key(true));
        assertFalse(plan.isEmpty());
        assertTrue(GrimoireBookPlan.textures().contains("emblem_engraving"));
        boolean emblem = false, art = false, base = false;
        for (var q : plan) {
            emblem |= q.texture().equals("emblem_engraving");
            art |= q.texture().equals("cover_art_engraving");
            base |= q.texture().equals("cover_base_engraving");
            assertTrue(GrimoireBookPlan.textures().contains(q.texture()), "unknown texture " + q.texture());
        }
        assertTrue(emblem && art && base, "the rune ring emblem and the cover design are on the book");
        assertEquals("COMPAT", BookMotif.values()[BookMotif.ENGRAVING.ordinal() - 1].name(), "the new motif is appended after the older ones");
        assertEquals(BookMotif.ENGRAVING, BookMotif.values()[BookMotif.values().length - 1], "enum entries are only ever appended");
    }

    @Test
    void theTexturesAreThereAndTheIconIsA16x16Sprite() throws Exception {
        for (String t : new String[]{"emblem_engraving", "cover_art_engraving", "cover_base_engraving"}) assertTrue(new File(TEX, t + ".png").isFile(), "missing texture " + t);
        BufferedImage emblem = ImageIO.read(new File(TEX, "emblem_engraving.png"));
        assertEquals(32, emblem.getWidth());
        BufferedImage art = ImageIO.read(new File(TEX, "cover_art_engraving.png"));
        assertEquals(64, art.getWidth());
        assertEquals(80, art.getHeight());
        BufferedImage icon = ImageIO.read(new File(RES, "assets/nusmp/textures/item/engraving_codex.png"));
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
        int opaque = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((icon.getRGB(x, y) >>> 24) > 0) opaque++;
        assertTrue(opaque > 100, "the sprite icon is drawn: " + opaque);
    }

    @Test
    void theModelIsTheGrimoireGeometryInHandAndTheSpriteIsTheFlatVariant() throws Exception {
        JsonObject model = JsonParser.parseString(Files.readString(new File(RES, "assets/nusmp/models/item/engraving_codex.json").toPath())).getAsJsonObject();
        assertEquals("nusmp:grimoire", model.get("loader").getAsString());
        JsonObject display = model.getAsJsonObject("display");
        for (String k : new String[]{"gui", "ground", "fixed", "head", "thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand"})
            assertTrue(display.has(k), "display " + k);
        JsonObject icon = JsonParser.parseString(Files.readString(new File(RES, "assets/nusmp/models/item/engraving_codex_icon.json").toPath())).getAsJsonObject();
        assertEquals("nusmp:item/engraving_codex", icon.getAsJsonObject("textures").get("layer0").getAsString());
        String baked = Files.readString(new File("src/main/java/com/newuniverse/nusmp/client/grimoire/GrimoireBakedModel.java").toPath());
        assertTrue(baked.contains("EngravingCodexItem") && baked.contains("EngravingCodexLook.LOOK"), "the grimoire model resolves the codex's fixed look");
    }

    @Test
    void theRecipeCraftsTheCodexFromABookAmethystAndInk() throws Exception {
        JsonObject r = JsonParser.parseString(Files.readString(new File(RES, "data/nusmp/recipe/engraving_codex.json").toPath())).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", r.get("type").getAsString());
        assertEquals("nusmp:engraving_codex", r.getAsJsonObject("result").get("id").getAsString());
        JsonObject key = r.getAsJsonObject("key");
        assertEquals("minecraft:book", key.getAsJsonObject("B").get("item").getAsString());
        assertEquals("minecraft:amethyst_shard", key.getAsJsonObject("A").get("item").getAsString());
        assertEquals("minecraft:ink_sac", key.getAsJsonObject("I").get("item").getAsString());
        var pattern = r.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        for (var row : pattern) assertEquals(3, row.getAsString().length());
    }

    @Test
    void theGuiSizeRegistryKnowsTheScreen() {
        var e = GuiScaleRegistry.byId("engraving_codex");
        assertNotNull(e);
        assertEquals("com.newuniverse.nusmp.client.engraving.EngravingCodexScreen", e.className());
        assertTrue(new File("src/main/java/com/newuniverse/nusmp/client/engraving/EngravingCodexScreen.java").isFile());
    }

    @Test
    void theItemCommandAndTheSelfTestAreWired() throws Exception {
        String nusmp = Files.readString(new File("src/main/java/com/newuniverse/nusmp/NUSMP.java").toPath());
        assertTrue(nusmp.contains("EngravingCodexItems.init()"), "the item is registered before the item register");
        assertTrue(nusmp.indexOf("EngravingCodexItems.init()") < nusmp.indexOf("NUItems.ITEMS.register("), "init runs before NUItems.ITEMS.register");
        assertTrue(nusmp.contains("EngravingCodexSelfTests.register()") && nusmp.contains("EngravingCodexClient.init(modEventBus)"));
        String tab = Files.readString(new File("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java").toPath());
        assertTrue(tab.contains("EngravingCodexItems.codexStack()"), "creative tab entry");
        String cmds = Files.readString(new File("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java").toPath());
        assertTrue(cmds.contains("EngravingCodexCommand.node()"), "/multiverse engravings");
        String selftest = Files.readString(new File("src/main/java/com/newuniverse/nusmp/engraving/codex/EngravingCodexSelfTests.java").toPath());
        assertTrue(selftest.contains("\"item.engraving_codex\""));
    }
}
