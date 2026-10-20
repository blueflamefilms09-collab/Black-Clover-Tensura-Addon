package com.newuniverse.nusmp.boss.bars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the styles are data and art that no compiler checks. This reads the shipped files with the very parser the game runs: every style of {@link BarStyleIds} has a json that parses (a known
 * emblem, fill effect, entrance and death, all fourteen colours), a frame atlas of the size the painter assumes, no json is left over without an id, the emblem sheet and the fill-effect sheet
 * are as big as their lists say, and the layout block of {@code _index.json} (which the python mock-up and the generator read) agrees with {@link BarLayoutMath}.
 */
class BarStylesAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DATA = ASSETS.resolve("boss_bars"), TEX = ASSETS.resolve("textures/gui/boss_bar");

    private static JsonObject index() throws IOException {
        return JsonParser.parseString(Files.readString(DATA.resolve("_index.json"))).getAsJsonObject();
    }

    private static List<String> strings(JsonArray a) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : a) out.add(e.getAsString());
        return out;
    }

    @Test
    void theIdsOfTheJavaListAndTheGeneratorAreTheSame() throws IOException {
        JsonObject idx = index();
        assertEquals(BarStyleIds.ALL, strings(idx.getAsJsonArray("styles")), "BarStyleIds.ALL and tools/boss_bar_art/styles.py list the same styles in the same order");
        assertEquals(BarStyle.EMBLEMS, strings(idx.getAsJsonArray("emblems")), "BarStyle.EMBLEMS and emblems.py");
        assertEquals(BarStyle.FX, strings(idx.getAsJsonArray("fx")), "BarStyle.FX and styles.FX");
        assertTrue(BarStyleIds.ALL.size() >= 24, "the owner asked for at least 24 frames: " + BarStyleIds.ALL.size());
        assertEquals(BarStyleIds.ALL.size(), new HashSet<>(BarStyleIds.ALL).size(), "no id twice");
    }

    @Test
    void everyStyleJsonParsesAndHasItsArt() throws IOException {
        for (String id : BarStyleIds.ALL) {
            Path f = DATA.resolve(id + ".json");
            assertTrue(Files.exists(f), "missing " + f);
            BarStyle s = BarStyle.parse(Files.readString(f));
            assertEquals(id, s.id());
            assertTrue(BarStyle.EMBLEMS.contains(s.emblem()), id + " names an emblem that is not registered: " + s.emblem());
            assertTrue(s.emblemIndex() >= 0 && s.fxIndex() >= 0);
            assertEquals(BarStyle.COLOR_KEYS.size(), s.colors().size());
            Path atlas = TEX.resolve("frame_" + id + ".png");
            assertTrue(Files.exists(atlas), "no frame atlas for " + id);
            assertTrue(Files.exists(TEX.resolve("frame_" + id + ".png.mcmeta")), "the atlas of " + id + " must ask for blur (it is drawn at half size)");
            BufferedImage im = ImageIO.read(atlas.toFile());
            assertEquals(BarLayoutMath.ATLAS_W, im.getWidth(), id + " atlas width");
            assertEquals(BarLayoutMath.ATLAS_H, im.getHeight(), id + " atlas height");
            assertTrue(opaquePixels(im, BarLayoutMath.LAYER_Y[1]) > 2000, id + ": the frame layer is empty");
            assertTrue(opaquePixels(im, BarLayoutMath.LAYER_Y[0]) > 2000, id + ": the back layer is empty");
            assertTrue(opaquePixels(im, BarLayoutMath.LAYER_Y[2]) > 100, id + ": the glow layer is empty");
            // the window and the socket of the frame layer must be clear, or the fill and the portrait are hidden
            int wy = BarLayoutMath.LAYER_Y[1] + (BarLayoutMath.WIN_Y + BarLayoutMath.WIN_H / 2) * 2;
            for (int x = BarLayoutMath.WIN_X * 2 + 4; x < (BarLayoutMath.WIN_X + BarLayoutMath.WIN_W) * 2 - 4; x += 7)
                assertEquals(0, im.getRGB(x, wy) >>> 24, id + " window is covered at x=" + x);
            assertEquals(0, im.getRGB(BarLayoutMath.SOCK_X * 2, BarLayoutMath.LAYER_Y[1] + BarLayoutMath.SOCK_Y * 2) >>> 24, id + " socket is covered");
        }
    }

    private static int opaquePixels(BufferedImage im, int y0) {
        int n = 0;
        for (int y = y0; y < y0 + BarLayoutMath.LAYER_H; y += 2) for (int x = 0; x < im.getWidth(); x += 2) if ((im.getRGB(x, y) >>> 24) > 40) n++;
        return n;
    }

    @Test
    void noStyleFileWithoutAnIdAndNoUnderscoreFileIsAStyle() throws IOException {
        Set<String> expected = new HashSet<>(BarStyleIds.ALL);
        try (Stream<Path> s = Files.list(DATA)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String n = p.getFileName().toString();
                if (!n.endsWith(".json") || n.startsWith("_")) continue;
                assertTrue(expected.contains(n.substring(0, n.length() - 5)), "a style json that BarStyleIds does not list: " + n);
            }
        }
    }

    @Test
    void theSharedSheetsAreAsBigAsTheirListsSay() throws IOException {
        BufferedImage em = ImageIO.read(TEX.resolve("emblems.png").toFile());
        int rows = (BarStyle.EMBLEMS.size() + BarLayoutMath.EMBLEM_COLS - 1) / BarLayoutMath.EMBLEM_COLS;
        assertEquals(BarLayoutMath.EMBLEM_COLS * BarLayoutMath.EMBLEM_CELL, em.getWidth());
        assertEquals(rows * BarLayoutMath.EMBLEM_CELL, em.getHeight());
        for (int i = 0; i < BarStyle.EMBLEMS.size(); i++) {
            int n = 0, u = (i % BarLayoutMath.EMBLEM_COLS) * 32, v = (i / BarLayoutMath.EMBLEM_COLS) * 32;
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) if ((em.getRGB(u + x, v + y) >>> 24) > 40) n++;
            assertTrue(n > 60, "the emblem '" + BarStyle.EMBLEMS.get(i) + "' is empty (" + n + " pixels)");
        }
        BufferedImage fx = ImageIO.read(TEX.resolve("fillfx.png").toFile());
        assertEquals(BarLayoutMath.FX_W, fx.getWidth());
        assertEquals(BarLayoutMath.FX_ROW_H * BarStyle.FX.size(), fx.getHeight());
        BufferedImage chains = ImageIO.read(TEX.resolve("chains.png").toFile());
        assertEquals(BarLayoutMath.FX_W, chains.getWidth());
        assertEquals(BarLayoutMath.FX_ROW_H, chains.getHeight());
    }

    @Test
    void theLayoutBlockOfTheIndexAgreesWithTheJavaConstants() throws IOException {
        JsonObject l = index().getAsJsonObject("layout");
        assertEquals(BarLayoutMath.WIDTH, l.get("width").getAsInt());
        assertEquals(BarLayoutMath.FRAME_H, l.get("height").getAsInt());
        assertEquals(List.of(BarLayoutMath.WIN_X, BarLayoutMath.WIN_Y, BarLayoutMath.WIN_W, BarLayoutMath.WIN_H), ints(l.getAsJsonArray("window")));
        assertEquals(List.of(BarLayoutMath.SOCK_X, BarLayoutMath.SOCK_Y, BarLayoutMath.SOCK_R), ints(l.getAsJsonArray("socket")));
        assertEquals(List.of(BarLayoutMath.PLATE_X, BarLayoutMath.PLATE_Y, BarLayoutMath.PLATE_W, BarLayoutMath.PLATE_H), ints(l.getAsJsonArray("plate")));
        assertEquals(BarLayoutMath.ATLAS_W, l.get("atlas_w").getAsInt());
        assertEquals(BarLayoutMath.ATLAS_H, l.get("atlas_h").getAsInt());
        assertEquals(BarLayoutMath.LAYER_H, l.get("layer_h").getAsInt());
        assertEquals(List.of(BarLayoutMath.LAYER_Y[0], BarLayoutMath.LAYER_Y[1], BarLayoutMath.LAYER_Y[2]), ints(l.getAsJsonArray("layer_y")));
        assertEquals(BarLayoutMath.EMBLEM_COLS, l.get("emblem_cols").getAsInt());
        assertEquals(BarLayoutMath.FX_W, l.get("fx_w").getAsInt());
        assertEquals(BarLayoutMath.FX_ROW_H, l.get("fx_row_h").getAsInt());
    }

    private static List<Integer> ints(JsonArray a) {
        List<Integer> out = new ArrayList<>();
        for (JsonElement e : a) out.add(e.getAsInt());
        return out;
    }

    @Test
    void theParserRefusesWhatItShould() {
        String ok = "{\"id\":\"holy\",\"family\":\"theme\",\"emblem\":\"wing\",\"fx\":\"shimmer\",\"entrance\":\"slide\",\"death\":\"shatter\",\"colors\":{" + colors("#FFFFFF") + "}}";
        assertEquals("holy", BarStyle.parse(ok).id());
        assertThrows(ok.replace("\"wing\"", "\"banana\""), "unknown emblem");
        assertThrows(ok.replace("\"shimmer\"", "\"sparkle\""), "unknown fill effect");
        assertThrows(ok.replace("\"slide\"", "\"teleport\""), "unknown entrance");
        assertThrows(ok.replace("\"shatter\"", "\"explode\""), "unknown death");
        assertThrows(ok.replace("\"#FFFFFF\"", "\"white\""), "a colour that is not #RRGGBB");
        assertThrows(ok.replace("\"fill_top\":\"#FFFFFF\",", ""), "a missing colour");
        assertThrows("[1,2]", "not an object");
        assertThrows("{", "broken json");
    }

    private static String colors(String c) {
        StringBuilder sb = new StringBuilder();
        for (String k : BarStyle.COLOR_KEYS) sb.append(sb.length() == 0 ? "" : ",").append('"').append(k).append("\":\"").append(c).append('"');
        return sb.toString();
    }

    private static void assertThrows(String json, String why) {
        try {
            BarStyle.parse(json);
        } catch (IllegalArgumentException expected) {
            return;
        }
        assertTrue(false, "the parser accepted: " + why);
    }
}
