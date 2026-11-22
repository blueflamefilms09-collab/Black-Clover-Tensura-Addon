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

/**
 * Why: hero weapons are enchantable only through item tags (docs/vassal_enchanting.md, docs/heroweapon/enchanting_menus.md); a missing or misspelt id silently leaves one weapon
 * un-enchantable. The 17 vassal ids are in all four melee tags; since 4.100.0 the Legendary Sword and Spear join them, the Legendary Shield is in "weapon" and the Legendary Bow has its own "bow" tag.
 */
class VassalEnchantTagsTest {
    private static final Path TAGS = Path.of("src/main/resources/data/minecraft/tags/item/enchantable");

    private static List<String> vassals() {
        List<String> ids = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) if (k.enchantable()) ids.add("anime_multiverse:" + k.itemId());
        return ids;
    }

    private static List<String> idsOf(String tag) throws IOException {
        String json = Files.readString(TAGS.resolve(tag + ".json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"replace\": false"), tag + " must not replace the vanilla tag");
        List<String> found = new ArrayList<>();
        Matcher m = Pattern.compile("\"(anime_multiverse:[a-z_]+)\"").matcher(json);
        while (m.find()) found.add(m.group(1));
        return found;
    }

    @Test
    void everyVassalIsEnchantableAndOthersAreNotByKind() {
        assertEquals(17, vassals().size());
        for (HeroKind k : HeroKind.values()) assertEquals(k.itemId().startsWith("vassal_"), k.enchantable(), k.itemId());
        assertFalse(HeroKind.SHIELD.enchantable());
    }

    @Test
    void meleeTagsListTheLegendaryBladesAndEveryVassal() throws IOException {
        for (String tag : new String[] {"sharp_weapon", "sword", "fire_aspect"}) {
            List<String> expected = new ArrayList<>(List.of("anime_multiverse:legendary_sword", "anime_multiverse:legendary_spear"));
            expected.addAll(vassals());
            assertEquals(expected, idsOf(tag), tag);
        }
        List<String> weapon = new ArrayList<>(List.of("anime_multiverse:legendary_shield", "anime_multiverse:legendary_sword", "anime_multiverse:legendary_spear"));
        weapon.addAll(vassals());
        assertEquals(weapon, idsOf("weapon"));
    }

    @Test
    void theLegendaryBowHasTheBowTag() throws IOException {
        assertEquals(List.of("anime_multiverse:legendary_bow"), idsOf("bow"));
    }
}
