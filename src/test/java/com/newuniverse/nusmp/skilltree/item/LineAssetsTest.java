package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every line (283 of them once the content lands) names a background picture and a book cover in its route json, and nothing but the game's missing-texture checker tells you
 * one is absent: the tree opens on a purple-and-black panel, or a book shows the generic cover. This guard reads the SHIPPED route files and fails when, for any route:
 * it declares {@code gui_background} and that file does not exist or is not a real PNG of a sane size (256 or 512 wide, 256 high, small enough that 283 of them do not bloat the jar);
 * it has no background picture at all (the generator makes one per route: {@code python3 tools/lines/gen_backgrounds.py}); it declares a {@code background_theme} that is not one of the
 * fifteen; it declares a {@code book_texture} whose franchise has no cover; or it is missing from, or disagrees with, the shipped client table
 * {@code assets/nusmp/skill_books.json} (book id, colour, franchise: what a client without the route shows). It also fails when two lines share a byte-identical background (the
 * generator seeds every picture from its line id, so identical files mean a copy was committed by hand). The folders can be moved with the system properties
 * {@code nusmp.test.skilltrees} and {@code nusmp.test.assets} (the author runs it against a temporary data set before the content rows exist).
 */
class LineAssetsTest {
    private static final String TREES = System.getProperty("nusmp.test.skilltrees", "src/main/resources/data/nusmp/skill_trees");
    private static final String ASSETS = System.getProperty("nusmp.test.assets", "src/main/resources/assets/nusmp") + "/";
    private static final long MAX_BYTES = 60_000;

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static List<File> routeFiles() {
        File[] files = new File(TREES).listFiles((d, n) -> n.endsWith(".json"));
        assertNotNull(files, "no route folder at " + TREES);
        Arrays.sort(files);
        return new ArrayList<>(Arrays.asList(files));
    }

    private static String id(File f) { return f.getName().substring(0, f.getName().length() - 5); }

    private static boolean isPng(File f) throws Exception {
        byte[] sig = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] head = new byte[8];
        try (InputStream in = new FileInputStream(f)) {
            if (in.readNBytes(head, 0, 8) != 8) return false;
        }
        return Arrays.equals(sig, head);
    }

    @Test
    void thereAreRoutesToCheck() {
        assertTrue(routeFiles().size() >= 8, "the 8 hand-built routes at least: " + routeFiles().size());
    }

    @Test
    void everyDeclaredBackgroundExistsAndIsARealSmallPng() throws Exception {
        for (File route : routeFiles()) {
            JsonObject r = json(route);
            if (!r.has("gui_background")) continue;
            String ref = r.get("gui_background").getAsString();
            assertEquals("nusmp:textures/gui/skill_trees/" + id(route) + ".png", ref, id(route) + ": the background is named by the line id (one picture per line)");
            File png = new File(ASSETS + ref.substring("nusmp:".length()));
            assertTrue(png.isFile(), id(route) + " declares " + ref + " but " + png + " does not exist: run python3 tools/lines/gen_backgrounds.py");
            assertTrue(isPng(png), id(route) + ": " + png + " is not a PNG file");
            BufferedImage img = ImageIO.read(png);
            assertNotNull(img, id(route) + ": the PNG cannot be decoded");
            assertTrue(img.getWidth() == 256 || img.getWidth() == 512, id(route) + " width " + img.getWidth());
            assertEquals(256, img.getHeight(), id(route) + " height");
            assertTrue(png.length() <= MAX_BYTES, id(route) + ": " + png.length() + " bytes is too big for one of 283 backgrounds");
        }
    }

    @Test
    void everyRouteHasABackgroundPictureEvenIfItDoesNotDeclareOne() throws Exception {
        for (File route : routeFiles()) {
            File png = new File(ASSETS + "textures/gui/skill_trees/" + id(route) + ".png");
            assertTrue(png.isFile(), id(route) + " has no background picture " + png + ": run python3 tools/lines/gen_backgrounds.py");
            assertTrue(isPng(png), png + " is not a PNG");
        }
    }

    @Test
    void noTwoLinesShareAByteIdenticalBackground() throws Exception {
        Map<String, String> seen = new HashMap<>();
        for (File route : routeFiles()) {
            File png = new File(ASSETS + "textures/gui/skill_trees/" + id(route) + ".png");
            if (!png.isFile()) continue;
            byte[] hash = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(png.toPath()));
            String key = Arrays.toString(hash);
            String other = seen.put(key, id(route));
            assertNull(other, id(route) + " and " + other + " have the same background file: the generator seeds each picture from its line id, so a copy was made by hand");
        }
    }

    @Test
    void declaredThemesAndBookTexturesExist() throws Exception {
        Set<String> themes = new HashSet<>();
        for (JsonElement e : json(new File("tools/lines/catalog.json")).getAsJsonArray("backgrounds")) themes.add(e.getAsString());
        assertEquals(15, themes.size(), "the 15 themes of catalog.BACKGROUNDS");
        Set<String> franchises = new HashSet<>();
        for (JsonElement e : json(new File(ASSETS + "skill_books.json")).getAsJsonArray("franchises")) franchises.add(e.getAsString());
        for (File route : routeFiles()) {
            JsonObject r = json(route);
            if (r.has("background_theme")) assertTrue(themes.contains(r.get("background_theme").getAsString()), id(route) + ": unknown background_theme " + r.get("background_theme"));
            if (r.has("book_texture")) {
                String ref = r.get("book_texture").getAsString();
                assertTrue(ref.startsWith("nusmp:item/skill_book_3d/"), id(route) + ": book_texture " + ref);
                String franchise = ref.substring("nusmp:item/skill_book_3d/".length());
                assertTrue(franchises.contains(franchise), id(route) + ": book_texture names a franchise with no cover: " + franchise);
                assertTrue(new File(ASSETS + "textures/item/skill_book_3d/" + franchise + ".png").isFile(), id(route) + ": missing cover texture of " + franchise);
                String own = r.has("franchise") ? r.get("franchise").getAsString() : r.has("magic") ? r.get("magic").getAsString() : "";
                assertEquals(own, franchise, id(route) + ": its book_texture is another franchise's cover");
            }
        }
    }

    @Test
    void theShippedBookTableCoversEveryRouteAndAgreesWithIt() throws Exception {
        JsonObject table = json(new File(ASSETS + "skill_books.json"));
        JsonObject lines = table.getAsJsonObject("lines");
        JsonArray franchises = table.getAsJsonArray("franchises");
        Set<String> known = new HashSet<>();
        for (JsonElement f : franchises) known.add(f.getAsString());
        for (File route : routeFiles()) {
            JsonObject r = json(route);
            assertTrue(lines.has(id(route)), id(route) + " is missing from assets/nusmp/skill_books.json: run python3 tools/lines/gen_book_index.py (a client that has not unlocked the line would show a grey cover and 'blank pages')");
            JsonObject e = lines.getAsJsonObject(id(route));
            assertEquals(r.get("book").getAsString(), e.get("book").getAsString(), id(route) + ": book id");
            assertEquals(r.get("color").getAsString().toUpperCase(), e.get("color").getAsString().toUpperCase(), id(route) + ": colour");
            assertEquals(r.get("name").getAsString(), e.get("name").getAsString(), id(route) + ": name");
            String own = r.has("franchise") ? r.get("franchise").getAsString() : r.has("magic") ? r.get("magic").getAsString() : "";
            String want = known.contains(own) ? own : "";
            assertEquals(want, e.get("franchise").getAsString(), id(route) + ": franchise");
        }
    }
}
