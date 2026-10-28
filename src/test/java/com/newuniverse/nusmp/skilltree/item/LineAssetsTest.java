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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every unique skill book (254 of them) names a book cover and a skill icon, and nothing but the game's missing-texture checker tells you one is absent: a book shows the generic cover, or Tensura draws a purple
 * and black square for the skill. This guard reads the SHIPPED route files and fails when, for any route: it declares a {@code book_texture} whose franchise has no cover; it has no 32 by 32 skill icon
 * ({@code textures/skill/line/<id>.png}); or it is missing from, or disagrees with, the shipped table {@code assets/nusmp/skill_books.json} (book id, colour, name, franchise, description, lore, the names of its
 * arts in file order and of its passives), which is what the registry and both sides read. It also fails when the table lists a book that has no route file (a skill whose data never loads). The folders can be moved
 * with the system properties {@code nusmp.test.skilltrees} and {@code nusmp.test.assets}. 1.96.0: the background pictures of the removed tree screen were deleted with it, so their checks are gone.
 */
class LineAssetsTest {
    private static final String TREES = System.getProperty("nusmp.test.skilltrees", "src/main/resources/data/nusmp/unique_skills");
    private static final String ASSETS = System.getProperty("nusmp.test.assets", "src/main/resources/assets/nusmp") + "/";

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

    private static List<String> names(JsonObject route, boolean arts) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : route.getAsJsonArray("nodes")) {
            JsonObject n = e.getAsJsonObject();
            boolean hasKind = n.has("kind") && !n.get("kind").getAsString().isEmpty();
            String type = n.has("type") ? n.get("type").getAsString() : "";
            boolean hasSkill = n.has("skill") && !n.get("skill").getAsString().isEmpty();
            boolean hasPassives = n.has("passives") && n.getAsJsonArray("passives").size() > 0;
            boolean art = type.equals("active_ability") || (!hasKind && hasSkill);
            boolean passive = type.equals("passive_toggle") || type.equals("attribute_modifier") || (!hasKind && !hasSkill && hasPassives);
            if (arts ? art : passive) out.add(n.has("name") ? n.get("name").getAsString() : n.get("id").getAsString());
        }
        return out;
    }

    private static List<String> strings(JsonElement arr) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : arr.getAsJsonArray()) out.add(e.getAsString());
        return out;
    }

    @Test
    void thereAreRoutesToCheck() {
        assertTrue(routeFiles().size() >= 200, "the shipped books: " + routeFiles().size());
    }

    @Test
    void everyLineHasAThirtyTwoPixelSkillIcon() throws Exception {
        for (File route : routeFiles()) {
            File png = new File(ASSETS + "textures/skill/line/" + id(route) + ".png");
            assertTrue(png.isFile(), id(route) + " has no skill icon " + png + " (run python3 tools/tensura_menu/gen_line_icons.py)");
            assertTrue(isPng(png), png + " is not a PNG");
            BufferedImage img = ImageIO.read(png);
            assertEquals(32, img.getWidth(), id(route) + " icon width");
            assertEquals(32, img.getHeight(), id(route) + " icon height");
        }
    }

    @Test
    void everySkillIconBelongsToABook() {
        File[] icons = new File(ASSETS + "textures/skill/line").listFiles((d, n) -> n.endsWith(".png"));
        assertNotNull(icons);
        Set<String> ids = new HashSet<>();
        for (File f : routeFiles()) ids.add(id(f));
        for (File icon : icons) assertTrue(ids.contains(icon.getName().substring(0, icon.getName().length() - 4)), icon.getName() + " is the icon of a book that does not exist (a removed magic's icon was left behind)");
    }

    @Test
    void declaredBookTexturesExist() throws Exception {
        Set<String> franchises = new HashSet<>();
        for (JsonElement e : json(new File(ASSETS + "skill_books.json")).getAsJsonArray("franchises")) franchises.add(e.getAsString());
        for (File route : routeFiles()) {
            JsonObject r = json(route);
            if (!r.has("book_texture")) continue;
            String ref = r.get("book_texture").getAsString();
            assertTrue(ref.startsWith("nusmp:item/skill_book_3d/"), id(route) + ": book_texture " + ref);
            String franchise = ref.substring("nusmp:item/skill_book_3d/".length());
            assertTrue(franchises.contains(franchise), id(route) + ": book_texture names a franchise with no cover: " + franchise);
            assertTrue(new File(ASSETS + "textures/item/skill_book_3d/" + franchise + ".png").isFile(), id(route) + ": missing cover texture of " + franchise);
            String own = r.has("franchise") ? r.get("franchise").getAsString() : r.has("magic") ? r.get("magic").getAsString() : "";
            assertEquals(own, franchise, id(route) + ": its book_texture is another franchise's cover");
        }
    }

    @Test
    void theShippedBookTableCoversEveryRouteAndAgreesWithIt() throws Exception {
        JsonObject table = json(new File(ASSETS + "skill_books.json"));
        JsonObject lines = table.getAsJsonObject("lines");
        JsonArray franchises = table.getAsJsonArray("franchises");
        Set<String> known = new HashSet<>();
        for (JsonElement f : franchises) known.add(f.getAsString());
        Set<String> files = new HashSet<>();
        for (File route : routeFiles()) {
            files.add(id(route));
            JsonObject r = json(route);
            assertTrue(lines.has(id(route)), id(route) + " is missing from assets/nusmp/skill_books.json: run python3 tools/lines/gen_book_index.py");
            JsonObject e = lines.getAsJsonObject(id(route));
            assertEquals(r.get("book").getAsString(), e.get("book").getAsString(), id(route) + ": book id");
            assertEquals(r.get("color").getAsString().toUpperCase(), e.get("color").getAsString().toUpperCase(), id(route) + ": colour");
            assertEquals(r.get("name").getAsString(), e.get("name").getAsString(), id(route) + ": name");
            assertEquals(r.has("desc") ? r.get("desc").getAsString() : "", e.get("desc").getAsString(), id(route) + ": description");
            assertEquals(r.has("book_lore") ? r.get("book_lore").getAsString() : "", e.get("book_lore").getAsString(), id(route) + ": lore");
            assertEquals(names(r, true), strings(e.get("arts")), id(route) + ": the arts (the skill's modes) must be the route's active nodes in file order");
            assertEquals(names(r, false), strings(e.get("passives")), id(route) + ": the passives");
            String own = r.has("franchise") ? r.get("franchise").getAsString() : r.has("magic") ? r.get("magic").getAsString() : "";
            String want = known.contains(own) ? own : "";
            assertEquals(want, e.get("franchise").getAsString(), id(route) + ": franchise");
        }
        for (String key : lines.keySet()) assertTrue(files.contains(key), key + " is in the table but has no route file, so its skill would never have data");
    }
}
