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

    @Test
    void verseTabsCoverEveryEngravingFranchiseAndHaveTitles() throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"));
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java"));
        assertTrue(src.contains("FranchiseTabs.VERSES"), "verse tabs are not registered");
        for (String id : FranchiseTabs.VERSES) assertTrue(lang.contains("\"itemGroup.anime_multiverse.franchise_" + id + "\""), "no title for verse " + id);
        int seen = 0;
        for (FranchiseData.Franchise f : FranchiseData.table().franchises()) {
            seen++;
            assertTrue(FranchiseTabs.TABS.contains(f.id()) || FranchiseTabs.VERSES.contains(f.id()), "franchise without a tab: " + f.id());
        }
        assertTrue(seen >= 30, "franchise list not read: " + seen);
    }

    @Test
    void dragonBallAndShieldHeroGetTheirOwnTabsAndKeepTheOldOnes() {
        assertEquals(java.util.List.of("dragon_ball"), FranchiseTabs.tabsOf("dragon_ball_earth_4", null));
        assertEquals(java.util.List.of("dragon_ball"), FranchiseTabs.tabsOf("dragon_radar", null));
        assertEquals(java.util.List.of("anime_worlds", "dragon_ball"), FranchiseTabs.tabsOf("saiyan_alloy_helmet", null));
        assertEquals(java.util.List.of("hero_weapons", "shield_hero"), FranchiseTabs.tabsOf("legendary_shield", "shield_hero"));
        assertEquals(java.util.List.of("anime_worlds", "jujutsu_kaisen"), FranchiseTabs.tabsOf("sukunas_finger", "jujutsu_kaisen"));
        assertEquals(java.util.List.of("berserk"), FranchiseTabs.tabsOf("black_sword_greed", null));
        assertEquals(java.util.List.of("hero_weapons"), FranchiseTabs.tabsOf("calling_gem", null));
        assertTrue(FranchiseTabs.tabsOf("demon_slayer", null).isEmpty());
    }

    @Test
    void theReportCallsEmptyThinAndHeavyVerses() {
        assertTrue(FranchiseTabs.status(0).startsWith("EMPTY"));
        assertTrue(FranchiseTabs.status(3).startsWith("thin"));
        assertEquals("ok", FranchiseTabs.status(50));
        assertTrue(FranchiseTabs.status(356).startsWith("heavy"));
    }

    @Test
    void compactMenuKeepsTheFranchiseTabsAndMiscAndHasAWayBack() throws Exception {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java"));
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/franchise/FranchiseTabsConfig.java"));
        assertTrue(cfg.contains("\"compactTabs\", true"), "the compact menu setting is missing or not the default");
        assertTrue(src.contains("FranchiseTabsConfig.compact()") && src.contains("fillOther") && src.contains("verseOnly"), "the compact fill is missing");
        assertTrue(src.contains("if (verse && compact) return;"), "verse tabs are not hidden in the compact menu");
    }
}
