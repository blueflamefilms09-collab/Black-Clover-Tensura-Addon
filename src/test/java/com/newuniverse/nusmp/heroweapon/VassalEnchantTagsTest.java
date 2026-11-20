package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Why: vassal weapons are enchantable only through item tags (docs/vassal_enchanting.md); a missing or misspelt id silently leaves one weapon un-enchantable. */
class VassalEnchantTagsTest {
    private static final Path TAGS = Path.of("src/main/resources/data/minecraft/tags/item/enchantable");

    private static List<String> expected() {
        List<String> ids = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) if (k.enchantable()) ids.add("anime_multiverse:" + k.itemId());
        return ids;
    }

    @Test
    void everyVassalIsEnchantableAndOthersAreNot() {
        assertEquals(17, expected().size());
        for (HeroKind k : HeroKind.values()) assertEquals(k.itemId().startsWith("vassal_"), k.enchantable(), k.itemId());
        assertFalse(HeroKind.values()[0].enchantable() && !HeroKind.values()[0].itemId().startsWith("vassal_"));
    }

    @Test
    void tagFilesListExactlyTheVassals() throws IOException {
        for (String tag : new String[] {"weapon", "sharp_weapon", "sword", "fire_aspect"}) {
            String json = Files.readString(TAGS.resolve(tag + ".json"), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"replace\": false"), tag + " must not replace the vanilla tag");
            List<String> found = new ArrayList<>();
            Matcher m = Pattern.compile("\"(anime_multiverse:[a-z_]+)\"").matcher(json);
            while (m.find()) found.add(m.group(1));
            assertEquals(expected(), found, tag);
        }
    }
}
