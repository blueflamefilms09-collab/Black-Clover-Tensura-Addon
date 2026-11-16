package com.newuniverse.nusmp.franchise;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 3.88.0: the franchise creative tabs: the pure item-to-tab rules, a title per tab, the client option, and the older tabs left alone. */
class FranchiseTabsTest {
    @Test
    void franchiseIdsPickTheirTab() {
        assertEquals("black_clover", FranchiseTabs.tabOf("grimoire", "black_clover"));
        assertEquals("tensura", FranchiseTabs.tabOf("void_origin_katana", "tensura"));
        assertEquals("berserk", FranchiseTabs.tabOf("behelit_egg", "berserk"));
        assertEquals("fairy_tail", FranchiseTabs.tabOf("stormcaller_staff", "fairy_tail"));
        assertEquals("magi", FranchiseTabs.tabOf("djinn_seal_amon", "magi"));
        assertEquals("hero_weapons", FranchiseTabs.tabOf("legendary_sword", "shield_hero"));
        assertEquals("anime_worlds", FranchiseTabs.tabOf("sukunas_finger", "jujutsu_kaisen"));
    }

    @Test
    void unmappedItemsFallBackToIdRules() {
        assertEquals("berserk", FranchiseTabs.tabOf("black_sword_greed", null));
        assertEquals("tensura", FranchiseTabs.tabOf("beast_will_core", null));
        assertEquals("tensura", FranchiseTabs.tabOf("core_ss_ancient", null));
        assertEquals("hero_weapons", FranchiseTabs.tabOf("calling_gem", null));
        assertEquals("black_clover", FranchiseTabs.tabOf("devil_core_zagred", null));
        assertNull(FranchiseTabs.tabOf("demon_slayer", null));
        assertNull(FranchiseTabs.tabOf("", null));
        assertNull(FranchiseTabs.tabOf(null, null));
    }

    @Test
    void everyShippedMapFranchiseLandsInAKnownTab() throws Exception {
        for (String f : FranchiseMap.parse(Files.readString(Path.of("src/main/resources/data/anime_multiverse/relics/franchise_map.json"))).items().values())
            assertTrue(FranchiseTabs.TABS.contains(FranchiseTabs.tabOf("x", f)), f);
    }

    @Test
    void everyTabHasATitleAndARegistrationAndTheOldTabsStay() throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"));
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java"));
        for (String id : FranchiseTabs.TABS) {
            assertTrue(lang.contains("\"itemGroup.anime_multiverse.franchise_" + id + "\""), "no title for " + id);
            assertTrue(src.contains("franchiseTab(\"" + id + "\""), "tab not registered: " + id);
        }
        for (String id : new String[]{"main", "weapons", "armor", "relics", "eggs", "dungeon", "misc"})
            assertTrue(src.contains("tab(\"" + id + "\""), "old tab gone: " + id);
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("\"nusmp-franchise-tabs-client.toml\""), "client option not registered");
    }
}
