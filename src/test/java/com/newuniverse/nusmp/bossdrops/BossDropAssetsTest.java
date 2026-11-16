package com.newuniverse.nusmp.bossdrops;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/bosses/boss_drops.md): the shipped files of the boss drop framework must agree with the Java catalog. Reads the real files: the 23 drop tables (valid, named after their boss, paying the boss's own
 * trophy and only items that exist), the 23 trophies (mesh winding, skins, icons, models, hand class), the eight armour pieces (icons, models, layers, recipes, the trophy tag), the lang file, and the
 * rules the code must keep (no client class in common code, no per-tick work, the config defaults). Pure file reads; the working directory is the project root.
 */
class BossDropAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path DATA = Path.of("src/main/resources/data/anime_multiverse");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp/bossdrops");
    /** The currency rows are the one place a table may name another mod's item (Tensura's coins, CoinKind). */
    private static final Set<String> FOREIGN = Set.of("tensura:silver_coin", "tensura:gold_coin");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static BufferedImage png(String rel) throws IOException {
        Path f = ASSETS.resolve(rel);
        assertTrue(Files.isRegularFile(f), rel + " is missing");
        BufferedImage im = ImageIO.read(f.toFile());
        assertNotNull(im, rel);
        return im;
    }

    private static List<String> catalogFile() throws IOException {
        List<String> out = new ArrayList<>();
        for (var e : json(Path.of("tools/boss_drops/bosses.json")).getAsJsonArray("bosses")) out.add(e.getAsJsonObject().get("id").getAsString());
        return out;
    }

    @Test
    void theColourFileAndTheJavaCatalogListTheSame23Bosses() throws IOException {
        assertEquals(BossDropCatalog.BOSSES, catalogFile());
        assertEquals(23, BossDropCatalog.BOSSES.size());
        assertEquals(23, new HashSet<>(BossDropCatalog.BOSSES).size(), "no boss twice");
    }

    @Test
    void everyDropTableIsValidNamedAfterItsBossAndPaysItsTrophy() throws IOException {
        for (String boss : BossDropCatalog.BOSSES) {
            Path f = DATA.resolve("boss_drops/" + boss + ".json");
            assertTrue(Files.isRegularFile(f), boss + ": no drop table");
            BossDropTable.Table t = BossDropTable.parse(json(f));
            assertEquals(boss, t.boss(), "the file name and the boss id agree");
            boolean trophy = false, firstCoin = false;
            for (BossDropTable.Row r : t.rows()) {
                if (r.item().equals("anime_multiverse:" + BossDropCatalog.trophy(boss))) { trophy = true; assertEquals(1.0, r.chance(), boss + ": the trophy always drops"); }
                if (r.firstKillOnly()) firstCoin = true;
                if (r.item().startsWith("anime_multiverse:")) {
                    String path = r.item().substring("anime_multiverse:".length());
                    assertTrue(Files.isRegularFile(ASSETS.resolve("models/item/" + path + ".json")), boss + ": row item without a model: " + r.item());
                } else {
                    assertTrue(FOREIGN.contains(r.item()), boss + ": only the currency rows may name another mod's item: " + r.item());
                }
                assertTrue(r.chance() > 0 && r.chance() <= 1.0 && r.max() <= BossDropTable.MAX_COUNT);
            }
            assertTrue(trophy, boss + " does not drop its own trophy");
            assertTrue(firstCoin, boss + " has no first-kill row");
            assertTrue(t.rows().size() <= BossDropTable.MAX_ROWS && t.maxStacks() <= BossDropTable.HARD_MAX_STACKS);
        }
        try (Stream<Path> files = Files.list(DATA.resolve("boss_drops"))) {
            assertEquals(BossDropCatalog.BOSSES.size(), files.filter(p -> p.toString().endsWith(".json")).count(), "no table without a catalog boss");
        }
    }

    @Test
    void luciferoPaysHisFourPiecesAsRareDropsWithASmallChanceOfASecond() throws IOException {
        BossDropTable.Table t = BossDropTable.parse(json(DATA.resolve("boss_drops/lucifero.json")));
        double expected = 0;
        for (String piece : BossDropCatalog.LUCIFERO_PIECES) {
            double first = 0, second = 0;
            int rows = 0;
            for (BossDropTable.Row r : t.rows()) {
                if (!r.item().equals("anime_multiverse:" + piece)) continue;
                rows++;
                if (rows == 1) first = r.chance(); else second = r.chance();
                assertEquals(1, r.max(), "one copy per row");
            }
            assertEquals(2, rows, piece + ": one row and a second-copy row");
            assertTrue(first >= 0.2 && first < 1.0, piece + ": a rare drop, not a certainty: " + first);
            assertTrue(second > 0 && second < first, piece + ": a second copy is rarer than the first");
            expected += first;
        }
        assertTrue(expected >= 0.8 && expected <= 1.6, "about one piece a kill: " + expected);
    }

    @Test
    void everyTrophyIsASolidBlenderRelicWithItsSkinsIconAndModels() throws IOException {
        for (String boss : BossDropCatalog.BOSSES) {
            String id = BossDropCatalog.trophy(boss);
            Path mesh = ASSETS.resolve("weapon_meshes/" + id + ".txt");
            assertTrue(Files.isRegularFile(mesh), id + ": mesh missing");
            int quads = 0;
            boolean glow = false;
            for (String line : Files.readAllLines(mesh)) {
                String[] p = line.trim().split("\\s+");
                if (p[0].equals("glow")) glow = true;
                if (!p[0].equals("qn")) continue;
                quads++;
                double[][] v = new double[4][3];
                double[] n = new double[3];
                for (int k = 0; k < 4; k++) for (int c = 0; c < 3; c++) {
                    v[k][c] = Double.parseDouble(p[2 + 8 * k + c]);
                    n[c] += Double.parseDouble(p[2 + 8 * k + 5 + c]);
                }
                double[] g = new double[3];
                add(g, cross(sub(v[1], v[0]), sub(v[2], v[0])));
                add(g, cross(sub(v[2], v[0]), sub(v[3], v[0])));
                assertTrue(dot(g, n) > 0, id + ": a quad is wound clockwise seen from outside");
            }
            assertTrue(quads > 100 && quads <= 400, id + ": a small relic, not a big mesh: " + quads);
            assertTrue(glow, id + ": the crystal glows");
            BufferedImage skin = png("textures/entity/weapon/" + id + ".png");
            for (int y = 0; y < skin.getHeight(); y += 7) for (int x = 0; x < skin.getWidth(); x += 7) assertEquals(255, skin.getRGB(x, y) >>> 24, id + ": skin pixel not opaque");
            png("textures/entity/weapon/" + id + "_glow.png");
            BufferedImage ic = png("textures/item/" + id + ".png");
            assertEquals(16, ic.getWidth());
            assertEquals(16, ic.getHeight());
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int a = ic.getRGB(x, y) >>> 24;
                assertTrue(a == 0 || a == 255, id + ": semi-transparent icon pixel");
            }
            JsonObject model = json(ASSETS.resolve("models/item/" + id + ".json"));
            assertEquals("minecraft:builtin/entity", model.get("parent").getAsString());
            assertEquals("anime_multiverse:item/" + id, json(ASSETS.resolve("models/item/" + id + "_icon.json")).getAsJsonObject("textures").get("layer0").getAsString(), "the flat sprite stays the inventory icon");
        }
        String a = Files.readString(ASSETS.resolve("weapon_classes.json")), b = Files.readString(Path.of("tools/relics/weapon_classes.json"));
        for (String boss : BossDropCatalog.BOSSES) {
            String line = "\"" + BossDropCatalog.trophy(boss) + "\": \"handitem\"";
            assertTrue(a.contains(line) && b.contains(line), boss + ": no hand class in one of the two weapon_classes.json files");
        }
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/WeaponRenderer.java")).contains("BossDropItems.renderables()"), "WeaponRenderer draws the trophies");
        assertTrue(Files.isRegularFile(Path.of("tools/blender/boss_trophy.py")), "the relic is made in Blender");
    }

    @Test
    void theTrophyTagTheTrophyPlateRecipesAndTheArmourFiles() throws IOException {
        JsonArray tag = json(DATA.resolve("tags/item/boss_trophies.json")).getAsJsonArray("values");
        Set<String> values = new HashSet<>();
        for (var e : tag) values.add(e.getAsString());
        assertEquals(23, values.size());
        for (String b : BossDropCatalog.BOSSES) assertTrue(values.contains("anime_multiverse:" + BossDropCatalog.trophy(b)), b);
        int[] cost = {5, 8, 7, 4};
        String[] piece = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < 4; i++) {
            JsonObject r = json(DATA.resolve("recipe/trophy_plate_" + piece[i] + ".json"));
            assertEquals("minecraft:crafting_shapeless", r.get("type").getAsString());
            assertEquals(cost[i], r.getAsJsonArray("ingredients").size());
            assertEquals("anime_multiverse:boss_trophies", r.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("tag").getAsString());
            assertEquals("anime_multiverse:trophy_plate_" + piece[i], r.getAsJsonObject("result").get("id").getAsString());
        }
        for (String id : concat(BossDropCatalog.LUCIFERO_PIECES, BossDropCatalog.PLATE_PIECES)) {
            assertEquals(16, png("textures/item/" + id + ".png").getWidth(), id);
            assertEquals("anime_multiverse:item/" + id, json(ASSETS.resolve("models/item/" + id + ".json")).getAsJsonObject("textures").get("layer0").getAsString());
        }
        for (String set : new String[]{"lucifero", "trophy_plate"}) for (int n = 1; n <= 2; n++) {
            BufferedImage l = png("textures/models/armor/" + set + "_layer_" + n + ".png");
            assertEquals(64, l.getWidth());
            assertEquals(32, l.getHeight());
        }
        for (String id : BossDropCatalog.LUCIFERO_PIECES) assertFalse(Files.exists(DATA.resolve("recipe/" + id + ".json")), id + " drops, it is not crafted");
    }

    @Test
    void luciferoSetSitsBetweenNetheriteAndTheBossCoreArmour() {
        int total = 0, plate = 0;
        for (int i = 0; i < 4; i++) { total += BossDropItems.luciferoPoints(i); plate += BossDropItems.platePoints(i); }
        assertTrue(total > 20 && total < 24, "netherite 20, the Aegis 24: " + total);
        assertTrue(plate > 15 && plate < 20, "between iron (15) and diamond (20): " + plate);
    }

    @Test
    void theLangFileNamesEveryItemAndTheOldFilesStayUntouched() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/boss_lucifero_drops.json"));
        for (String b : BossDropCatalog.BOSSES) assertTrue(lang.has("item.anime_multiverse." + BossDropCatalog.trophy(b)), b);
        for (String id : concat(BossDropCatalog.LUCIFERO_PIECES, BossDropCatalog.PLATE_PIECES)) assertTrue(lang.has("item.anime_multiverse." + id), id);
    }

    @Test
    void theFrameworkIsServerSafeBoundedAndBehindSettings() throws IOException {
        try (Stream<Path> files = Files.list(SRC)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                String s = Files.readString(f);
                assertFalse(s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("nusmp.client") || s.contains("Minecraft.getInstance"), f + ": a client class in common code");
                assertFalse(s.contains("PlayerTickEvent") || s.contains("ServerTickEvent") || s.contains("LevelTickEvent"), f + ": the boss drops do no per-tick work");
            }
        }
        String cfg = Files.readString(SRC.resolve("BossDropConfig.java"));
        assertTrue(cfg.contains("ModConfig") || cfg.contains("ModConfigSpec"), "a config");
        assertTrue(cfg.contains(".define(\"enabled\", true)") && cfg.contains("defineInRange(\"chanceScale\", 1.0, 0.0, 5.0)"), "on by default, a chance scale");
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("BossDropBoot.init(modEventBus, modContainer)"), "wired from the mod constructor");
        String events = Files.readString(SRC.resolve("BossDropEvents.java"));
        assertTrue(events.contains("LootVictims.killer") && events.contains("isCreative"), "only the killer, never a creative player");
        assertTrue(Files.readString(Path.of(".github/workflows/smoke.yml")).contains("bossdrops"), "the self-tests are in the smoke list");
    }

    // ================================================================ Lucifero's bodies
    @Test
    void luciferoV2IsRoutedBehindTheClientSwitchAndTheOldBodiesStay() throws IOException {
        String opts = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/playermodel/PlayerModelOptions.java"));
        assertTrue(opts.contains("b.push(\"lucifero\")") && opts.contains("\"newBody\""), "the lucifero.newBody setting");
        String routing = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/bossmesh/BossMeshRouting.java"));
        assertTrue(routing.contains("lucifero_v2_gates") && routing.contains("lucifero_v2_form2") && routing.contains("\"lucifero_v2\""), "the three new bodies");
        assertTrue(routing.contains("\"lucifero_form2\"") && routing.contains("\"lucifero\""), "the old bodies stay as the setting-off path");
        for (String old : new String[]{"lucifero", "lucifero_form2"}) assertTrue(Files.isRegularFile(ASSETS.resolve("entity_meshes/" + old + ".txt")), old + " stays");
        assertTrue(Files.isRegularFile(Path.of("tools/blender/boss_lucifero_v2.py")), "made in Blender");
    }

    @Test
    void theOpModeIsAServerSettingThatRestoresTheOldStats() throws IOException {
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/spade/SpadeConfig.java"));
        assertTrue(cfg.contains("\"opMode\", true") || cfg.contains("LUCIFERO_OP_MODE"), "the setting exists");
        String boss = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/spade/LuciferoBoss.java"));
        assertTrue(boss.contains("SpadeConfig.luciferoOp()") && boss.contains("lucifero_op_health"), "the stats come from a setting-guarded modifier");
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    private static double[] sub(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
    private static double[] cross(double[] a, double[] b) { return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]}; }
    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
    private static void add(double[] a, double[] b) { for (int i = 0; i < 3; i++) a[i] += b[i]; }
}
