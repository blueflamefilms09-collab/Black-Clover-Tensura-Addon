package com.newuniverse.nusmp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every creative tab has a title, and each group of the split menu is filled by the source. */
class CreativeTabsTest {
    @Test
    void everyTabHasATitleAndItsGroupIsUsed() throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"));
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java"));
        for (String id : new String[]{"main", "weapons", "armor", "relics", "eggs", "dungeon", "misc"})
            assertTrue(lang.contains("\"itemGroup.nusmp." + id + "\""), id);
        for (String g : new String[]{"GRIM", "WEAPONS", "RELICS", "EGGS", "DUNGEON", "MISC"})
            assertTrue(src.contains("o.apply(G." + g + ")"), "nothing goes to " + g);
    }
}
