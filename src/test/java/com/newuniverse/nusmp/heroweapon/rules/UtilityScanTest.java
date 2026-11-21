package com.newuniverse.nusmp.heroweapon.rules;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** 4.89.4: pins that no weapon rule can cancel, shock or reject a held utility item: every rejection goes through {@code HeroRules.rejectedClass}, which checks the exemption first. */
class UtilityScanTest {
    private static final String ROOT = "src/main/java/com/newuniverse/nusmp/";

    private static String read(String rel) throws Exception { return Files.readString(Path.of(ROOT + rel)); }

    @Test
    void everyCancelInTheRuleEventsIsBehindTheRejectionCheck() throws Exception {
        String src = read("heroweapon/rules/HeroRuleEvents.java");
        // split into methods at "private static void on"; the experience handlers (onPickupXp, onKillXp) and the shock-damage cap are not rejections
        String[] parts = src.split("(?=\\n    private static void on)");
        int checked = 0;
        for (String m : parts) {
            Matcher name = Pattern.compile("private static void (on\\w+)\\(").matcher(m);
            if (!name.find()) continue;
            String n = name.group(1);
            if (n.equals("onPickupXp") || n.equals("onKillXp") || n.equals("onShockDamage")) continue;
            if (!m.contains("setCanceled(true)")) continue;
            checked++;
            assertTrue(m.indexOf("HeroRules.rejectedClass(") >= 0 && m.indexOf("HeroRules.rejectedClass(") < m.indexOf("setCanceled(true)"), n + " cancels without asking HeroRules.rejectedClass first");
        }
        assertEquals(5, checked, "onAttack, onPhysicalDamage, onUseStart, onArrow and onRightClick cancel; update this pin if a handler is added");
    }

    @Test
    void rejectedClassChecksTheExemptionBeforeClassifying() throws Exception {
        String s = read("heroweapon/rules/HeroRules.java");
        int fn = s.indexOf("public static WeaponClass rejectedClass(");
        int ex = s.indexOf("UtilityItems.exempt(", fn);
        int cls = s.indexOf("classOf(held)", fn);
        assertTrue(fn > 0 && ex > fn && cls > ex, "the utility exemption must come before classOf in rejectedClass");
        assertTrue(s.contains("hold_use_items"));
        assertTrue(s.contains("UtilityItems.ignoreUseAnimation("), "classOf must not read a utility's channel animation as a bow");
    }

    @Test
    void theSettingExistsDefaultsOnAndTheYieldHandlerReadsIt() throws Exception {
        assertTrue(read("heroweapon/HeroConfig.java").contains("define(\"utilityItemsAlwaysWork\", true)"));
        assertTrue(read("heroweapon/rules/UtilityYieldEvents.java").contains("HeroConfig.utilityItemsAlwaysWork()"));
        assertTrue(read("heroweapon/HeroCore.java").contains("UtilityYieldEvents.register("));
    }

    @Test
    void theTagListsTheHourglassTheSigilTheGemAndTheRadar() throws Exception {
        String tag = Files.readString(Path.of("src/main/resources/data/anime_multiverse/tags/item/hold_use_items.json"));
        for (String id : new String[]{"zogratis_hourglass", "chronicle_sigil", "calling_gem", "dragon_radar", "dungeon_map", "grimoire_summons"})
            assertTrue(tag.contains("anime_multiverse:" + id), id);
    }

    @Test
    void theHourglassItselfHasNoWeaponCondition() throws Exception {
        String s = read("arena/ZogratisHourglassItem.java") + read("arena/HourglassRules.java") + read("arena/ArenaTravel.java");
        for (String banned : new String[]{"HeroBonds", "HeroRules", "getMainHandItem", "getOffhandItem", "getInventory"})
            assertFalse(s.contains(banned), "the hourglass path must not look at weapons or the pack: " + banned);
    }
}
