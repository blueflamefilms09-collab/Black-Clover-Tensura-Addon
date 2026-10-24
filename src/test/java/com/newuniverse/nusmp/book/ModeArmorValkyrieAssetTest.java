package com.newuniverse.nusmp.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Piece;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Tex;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the files of the Valkyrie armour v2 and of the Valkyrie Drill Lance, and the tables that name them, must stay in step: the piece table of the code is the piece table of the preview generator (the json that
 * ships with the meshes), every mesh file is in the jar with the quads the table says, the atlas is 256 x 256 with the cells in the order of the code and nothing under a transparent texel, the scale sheet repeats,
 * the lance item has its model, mesh with a rigid and a turning part, skins, icon, class, grip, engravings, Better Combat row, franchise row and lang text, and nothing hands it out.
 */
class ModeArmorValkyrieAssetTest {
    private static final Path RES = Path.of("src", "main", "resources"), ASSETS = RES.resolve("assets/nusmp"), MESHES = ASSETS.resolve("valkyrie_meshes");
    private static final String ID = "valkyrie_drill_lance";

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(read(p)).getAsJsonObject(); }

    @Test
    void thePieceTableOfTheCodeIsThePieceTableOfThePreviewJson() throws IOException {
        JsonObject root = json(MESHES.resolve("pieces.json"));
        JsonArray pieces = root.getAsJsonArray("pieces");
        assertEquals(Piece.values().length, pieces.size(), "the same number of pieces");
        for (int i = 0; i < pieces.size(); i++) {
            JsonObject j = pieces.get(i).getAsJsonObject();
            Piece p = Piece.values()[i];
            assertEquals(j.get("name").getAsString(), p.name, "piece " + i + " name (the order is the draw order)");
            assertEquals(j.get("copies").getAsInt(), p.copies, p.name + " copies");
            String bone = j.get("bone").getAsString();
            assertEquals(p.bone, switch (bone) {
                case "wing_hinge" -> ModeArmorValkyrieRules.Bone.WING;
                case "wing_hinge_low" -> ModeArmorValkyrieRules.Bone.WING_LOW;
                case "saint_hinge" -> ModeArmorValkyrieRules.Bone.SAINT_WING;
                case "body" -> ModeArmorValkyrieRules.Bone.BODY;
                case "head" -> ModeArmorValkyrieRules.Bone.HEAD;
                case "arm" -> ModeArmorValkyrieRules.Bone.ARM;
                case "leg" -> ModeArmorValkyrieRules.Bone.LEG;
                default -> throw new AssertionError("unknown bone " + bone);
            }, p.name + " bone");
            for (Form f : Form.values()) {
                boolean listed = false;
                for (JsonElement l : j.getAsJsonArray("looks")) listed |= l.getAsString().equals(f.name());
                assertEquals(listed, p.wornIn(f), p.name + " worn in " + f);
            }
            JsonObject tex = j.getAsJsonObject("tex");
            assertEquals(tex.has("3") ? name(tex.get("3")) : null, p.card == null ? null : p.card.name().toLowerCase(), p.name + " layer 3 sheet");
            assertEquals(tex.has("4") ? name(tex.get("4")) : null, p.veil == null ? null : p.veil.name().toLowerCase(), p.name + " layer 4 sheet");
            assertEquals(tex.has("5") ? name(tex.get("5")) : null, p.glow == null ? null : p.glow.name().toLowerCase(), p.name + " layer 5 sheet");
            JsonObject lv = j.getAsJsonObject("levels");
            String[] keys = {"near", "mid", "far"};
            for (int l = 0; l < 3; l++) {
                assertEquals(lv.getAsJsonObject(keys[l]).get("quads").getAsInt(), p.quads(l), p.name + " quads " + keys[l]);
                assertEquals(lv.getAsJsonObject(keys[l]).get("file").getAsString(), p.file(l) + ".txt");
            }
        }
        JsonObject lance = root.getAsJsonObject("lance_AB");
        String[] keys = {"near", "mid", "far"};
        for (int l = 0; l < 3; l++) {
            assertEquals(lance.getAsJsonObject("levels").getAsJsonObject(keys[l]).get("quads").getAsInt(), ModeArmorValkyrieRules.LANCE_QUADS[l]);
            assertEquals(lance.getAsJsonObject("levels").getAsJsonObject(keys[l]).get("file").getAsString(), ModeArmorValkyrieRules.lanceFile(l) + ".txt");
        }
        assertEquals(ModeArmorValkyrieRules.LANCE_LENGTH, lance.get("length_px").getAsFloat(), 1e-6f);
        assertEquals(ModeArmorValkyrieRules.LANCE_GRIP, lance.get("grip_px").getAsFloat(), 1e-6f);
        JsonArray atlas = root.getAsJsonArray("atlas");
        for (int i = 0; i < atlas.size(); i++) assertEquals(atlas.get(i).getAsString(), Tex.values()[i].name().toLowerCase(), "atlas cell " + i);
        assertEquals(Tex.SCALES.ordinal(), atlas.size(), "the scale sheet is the only one outside the atlas");
        assertEquals(Tex.ATLAS_SIZE, root.get("atlas_size").getAsInt());
        assertEquals(Tex.CELL, root.get("cell").getAsInt());
    }

    private static String name(JsonElement e) { return e.getAsString(); }

    @Test
    void everyMeshFileIsInTheJarAndNoOtherMeshIsLeftBehind() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String f : ModeArmorValkyrieMesh.allFiles()) if (!Files.exists(MESHES.resolve(f + ".txt"))) missing.add(f);
        assertTrue(missing.isEmpty(), "missing " + missing);
        List<String> known = ModeArmorValkyrieMesh.allFiles();
        try (var s = Files.list(MESHES)) {
            s.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.endsWith(".txt")) assertTrue(known.contains(n.substring(0, n.length() - 4)), "an unknown mesh file " + n);
            });
        }
    }

    @Test
    void theAtlasIs256WithTenSheetsAndAWhiteCellAndBlackUnderEveryTransparentTexel() throws IOException {
        BufferedImage im = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/mode/valk2_atlas.png").toFile());
        assertEquals(Tex.ATLAS_SIZE, im.getWidth());
        assertEquals(Tex.ATLAS_SIZE, im.getHeight());
        assertTrue(im.getColorModel().hasAlpha());
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int argb = im.getRGB(x, y);
                if ((argb >>> 24) < 4) assertTrue((argb & 0xFFFFFF) == 0, "colour under a transparent texel at " + x + "," + y + " (it is drawn additively: docs/vfx_square_bug.md)");
            }
        }
        int white = Tex.WHITE.ordinal();
        for (int y = 0; y < Tex.CELL; y++) for (int x = 0; x < Tex.CELL; x++) assertEquals(0xFFFFFFFF, im.getRGB(Tex.WHITE.column() * Tex.CELL + x, Tex.WHITE.row() * Tex.CELL + y), "the white cell is solid white");
        assertTrue(white < 12, "the cells fit the 4 x 4 grid");
        // every sheet that a piece uses is not empty
        for (Tex t : Tex.values()) {
            if (!t.inAtlas() || t == Tex.WHITE) continue;
            int opaque = 0;
            for (int y = 0; y < Tex.CELL; y++) for (int x = 0; x < Tex.CELL; x++) if ((im.getRGB(t.column() * Tex.CELL + x, t.row() * Tex.CELL + y) >>> 24) > 16) opaque++;
            assertTrue(opaque > 200, t + " cell is empty");
        }
        BufferedImage sc = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/mode/valk2_scales.png").toFile());
        assertEquals(64, sc.getWidth());
        assertEquals(64, sc.getHeight());
    }

    @Test
    void everyAtlasUvOfEveryMeshInsetsIntoItsOwnCell() {
        for (Piece p : Piece.values()) {
            ModeArmorValkyrieMesh m = ModeArmorValkyrieMesh.armour(p, 0, false);
            for (int pass = 0; pass < ModeArmorValkyrieMesh.PASSES; pass++) {
                if (pass == ModeArmorValkyrieMesh.SCALES) continue;
                for (int i = m.passFrom[pass]; i < m.passFrom[pass + 1]; i++) {
                    float u = m.uv[i * 2] * Tex.ATLAS_SIZE, v = m.uv[i * 2 + 1] * Tex.ATLAS_SIZE;
                    int cu = (int) (u / Tex.CELL), cv = (int) (v / Tex.CELL);
                    assertTrue(cu >= 0 && cu < Tex.COLUMNS && cv >= 0 && cv < 3, p.name + " uv in the grid");
                    float fu = u - cu * Tex.CELL, fv = v - cv * Tex.CELL;
                    assertTrue(fu >= 0.49f && fu <= Tex.CELL - 0.49f && fv >= 0.49f && fv <= Tex.CELL - 0.49f, p.name + " uv half a texel inside its cell: " + fu + " " + fv);
                    int cell = cv * Tex.COLUMNS + cu;
                    assertTrue(cell == Tex.WHITE.ordinal() || cell == p.texOf(ModeArmorValkyrieRules.L_CARD).ordinal() || cell == p.texOf(ModeArmorValkyrieRules.L_VEIL).ordinal()
                            || cell == p.texOf(ModeArmorValkyrieRules.L_GLOW).ordinal(), p.name + " samples a sheet it does not own: cell " + cell);
                }
            }
        }
    }

    // ------------------------------------------------------------------ the lance item
    @Test
    void theLanceHasItsModelsMeshWithAGripAndADrillSkinsIconAndRows() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"models/item/" + ID + ".json", "models/item/" + ID + "_icon.json", "weapon_meshes/" + ID + ".txt", "textures/entity/weapon/" + ID + ".png",
                "textures/entity/weapon/" + ID + "_glow.png", "textures/item/" + ID + ".png"})
            if (!Files.exists(ASSETS.resolve(f))) missing.add(f);
        assertTrue(missing.isEmpty(), "missing " + missing);
        assertEquals("minecraft:builtin/entity", json(ASSETS.resolve("models/item/" + ID + ".json")).get("parent").getAsString(), "the 3D model draws through the weapon renderer");
        assertEquals("nusmp:item/" + ID, json(ASSETS.resolve("models/item/" + ID + "_icon.json")).getAsJsonObject("textures").get("layer0").getAsString(), "the sprite is the inventory icon");
        BufferedImage icon = javax.imageio.ImageIO.read(ASSETS.resolve("textures/item/" + ID + ".png").toFile());
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
        int opaque = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((icon.getRGB(x, y) >>> 24) > 100) opaque++;
        assertTrue(opaque > 60 && opaque < 200, "a drill, not a box or a speck: " + opaque + " opaque pixels");

        List<String> lines = Files.readAllLines(ASSETS.resolve("weapon_meshes/" + ID + ".txt"));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("place ")), "placed on the sprite diagonal");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("glow ")), "an additive glow skin");
        assertTrue(lines.contains("orient manual"), "never normalised: the axis stays +y");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("gripxyz ")), "the hand holds the guard: a gripxyz line");
        int grip = lines.indexOf("part grip"), drill = lines.indexOf("part drill");
        assertTrue(grip > 0 && drill > grip, "the rigid part first, the turning drill last");
        long quads = lines.stream().filter(l -> l.startsWith("q ")).count();
        assertTrue(quads > 250, "a real model: " + quads + " quads");
        long rigid = lines.subList(grip, drill).stream().filter(l -> l.startsWith("q ")).count();
        assertTrue(rigid > 10 && rigid < 60, "the grip, the guard and the trailing ribbons are the small rigid part: " + rigid);
        for (int i = drill + 1; i < lines.size(); i++) assertFalse(lines.get(i).startsWith("part "), "nothing after the drill part but quads (every quad after it turns)");
        for (int i = grip; i < drill; i++) {
            String[] v = lines.get(i).trim().split("\\s+");
            if (!v[0].equals("q")) continue;
            float top = -1e9f;
            for (int k = 0; k < 4; k++) top = Math.max(top, Float.parseFloat(v[3 + k * 5]));
            assertTrue(top <= 8.0f + 1e-3f, "a rigid quad above the guard: y " + top);
        }
        for (int i = drill; i < lines.size(); i++) {                                       // the drill is symmetric about the axis: its quads stay inside the lance's radius
            String[] v = lines.get(i).trim().split("\\s+");
            if (!v[0].equals("q")) continue;
            for (int k = 0; k < 4; k++) assertTrue(Math.hypot(Float.parseFloat(v[2 + k * 5]), Float.parseFloat(v[4 + k * 5])) < 20.0, "a drill vertex far from the axis");
        }
        BufferedImage skin = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/weapon/" + ID + ".png").toFile());
        BufferedImage glow = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/weapon/" + ID + "_glow.png").toFile());
        assertEquals(128, skin.getWidth());
        assertEquals(128, glow.getWidth());
        for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) if ((glow.getRGB(x, y) >>> 24) < 4) assertTrue((glow.getRGB(x, y) & 0xFFFFFF) == 0, "colour under a transparent glow texel at " + x + "," + y);
        for (String l : lines) {
            if (!l.startsWith("q ")) continue;
            String[] v = l.trim().split("\\s+");
            for (int k = 0; k < 4; k++) {
                float u = Float.parseFloat(v[5 + k * 5]), w = Float.parseFloat(v[6 + k * 5]);
                assertTrue(u >= 0f && u <= 128f && w >= 0f && w <= 128f, "uv outside the skin: " + u + " " + w);
            }
        }
    }

    @Test
    void theLanceClassGripEngravingsBetterCombatFranchiseAndLangRowsExist() throws IOException {
        assertTrue(read(ASSETS.resolve("weapon_classes.json")).contains("\"" + ID + "\": \"spear\""), "class row (game copy)");
        assertTrue(read(Path.of("tools", "relics", "weapon_classes.json")).contains("\"" + ID + "\": {"), "class row (tools copy)");
        assertEquals(0.25, json(ASSETS.resolve("weapon_grips.json")).getAsJsonObject("fractions").get(ID).getAsDouble(), 1e-9, "grip fraction 0.25");
        assertTrue(read(Path.of("tools", "weapon_grip", "grip_golden.json")).contains("\"" + ID + "\""), "the grip golden knows the lance");
        assertEquals("bettercombat:spear", json(RES.resolve("data/nusmp/weapon_attributes/" + ID + ".json")).get("parent").getAsString());
        assertTrue(read(Path.of("tools", "bettercombat", "manifest.json")).contains("\"" + ID + "\": {"), "the lance is in the Better Combat inventory");
        assertEquals("black_clover", json(RES.resolve("data/nusmp/relics/franchise_map.json")).getAsJsonObject("items").get(ID).getAsString());
        String eng = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "item", "WeaponEngravings.java"));
        int at = eng.indexOf("Map.entry(\"" + ID + "\"");
        assertTrue(at > 0);
        String line = eng.substring(at, eng.indexOf('\n', at));
        assertTrue(line.contains("e(\"barrier_piercing\", 3)") && line.contains("e(\"elemental_boost\", 2)") && line.contains("e(\"crushing\", 1)") && line.contains("e(\"magic_weapon\", 1)"), "the engravings the owner named: " + line);
        List<String> tensura = Files.readAllLines(Path.of("src", "test", "resources", "mega", "gear", "tensura_enchantments.txt"));
        for (String e : new String[]{"barrier_piercing", "elemental_boost", "crushing", "magic_weapon"}) assertTrue(tensura.contains("tensura:" + e), e + " is a real Tensura engraving id");
        JsonObject lang = json(Path.of("tools", "lang", "valkyrie_armor.json"));
        assertEquals("Valkyrie Drill Lance", lang.get("item.nusmp." + ID).getAsString());
        assertTrue(lang.has("item.nusmp." + ID + ".lore"));
    }

    @Test
    void theLanceIsCreativeOnlyNothingHandsItOut() throws IOException {
        String tab = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "NUCreativeTab.java"));
        assertTrue(tab.contains("ValkyrieLanceBoot.creativeItems()"), "in the creative tab");
        List<String> hits = new ArrayList<>();
        try (var s = Files.walk(Path.of("src", "main", "resources", "data"))) {
            s.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    if (p.toString().contains("weapon_attributes") || p.toString().contains("franchise_map")) return;
                    if (read(p).contains(ID)) hits.add(p.toString());
                } catch (IOException e) { throw new AssertionError(e); }
            });
        }
        assertTrue(hits.isEmpty(), "the lance appears in a loot table, recipe or trade: " + hits);
        String boot = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "valkyrielance", "ValkyrieLanceBoot.java"));
        assertFalse(boot.toLowerCase().contains("loot") && boot.contains("LootTable"), "no loot");
    }
}
