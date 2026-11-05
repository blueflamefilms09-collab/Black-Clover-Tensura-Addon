package com.newuniverse.nusmp.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** The in-game wiki: every entry file is valid, titled, in order, and the book item has its model, texture, recipe and lang. */
class WikiBookAssetsTest {
    @Test
    void everyWikiEntryHasATitleTextAndAUniqueOrder() throws Exception {
        File[] files = new File("src/main/resources/assets/nusmp/codex/wiki").listFiles((d, n) -> n.endsWith(".json"));
        assertNotNull(files);
        assertTrue(files.length >= 20, "the wiki should cover every system");
        java.util.Set<Integer> orders = new java.util.HashSet<>();
        for (File f : files) {
            JsonObject o = JsonParser.parseString(Files.readString(f.toPath())).getAsJsonObject();
            assertFalse(o.get("title").getAsString().isBlank(), f.getName());
            assertTrue(o.get("text").getAsString().length() > 40, f.getName());
            assertTrue(orders.add(o.get("order").getAsInt()), "duplicate order in " + f.getName());
        }
    }

    @Test
    void theBookItemHasItsAssets() throws Exception {
        for (String p : new String[]{"textures/item/multiverse_wiki.png", "models/item/multiverse_wiki.json"})
            assertTrue(new File("src/main/resources/assets/nusmp/" + p).isFile(), p);
        assertTrue(new File("src/main/resources/data/nusmp/recipe/multiverse_wiki.json").isFile());
        String lang = Files.readString(new File("src/main/resources/assets/nusmp/lang/en_us.json").toPath());
        assertTrue(lang.contains("\"item.nusmp.multiverse_wiki\"") && lang.contains("\"nusmp.wiki.tip\""));
    }
}
