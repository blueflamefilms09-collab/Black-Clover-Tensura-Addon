package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Why: the enchanting-menu fix (4.100.0) is four small hooks that must stay joined; this source scan fails if one is cut, and the settings must keep the new behaviour on by default. */
class HeroEnchantMenusTest {
    private static String read(String path) throws Exception { return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/" + path)); }

    @Test
    void settingsAreOnByDefault() {
        assertEquals(true, HeroConfig.ENCHANT_MENUS.getDefault());
        assertEquals(true, HeroConfig.ENCHANT_HOLY.getDefault());
    }

    @Test
    void theLoanKeepsAProjectionOnAnEnchantingMenu() throws Exception {
        String loan = read("heroweapon/bond/HeroLoan.java");
        assertTrue(loan.contains("HeroEnchants.allowsMenu(p)"), "scanMenu must let the player's own weapon stay on an enchanting menu");
        assertTrue(loan.contains("HeroEnchants.inMenu(p, b)"), "reconcile must not make a second projection while the menu holds one");
        assertTrue(loan.contains("HeroEnchants.capture(p, b)"), "the enchantments must be kept before the projection is rebuilt");
        assertTrue(loan.contains("HeroEnchants.apply(p, b, st)"), "make must put the kept enchantments back");
        assertTrue(loan.contains("HeroEnchants.wipeMenu(p)"), "the end of the loan must empty the menu of the weapon");
    }

    @Test
    void theTableAcceptsAnEngravedStack() throws Exception {
        assertTrue(read("mixin/HeroWeaponEnchantableMixin.java").contains("HeroConfig.enchantMenus()"));
        assertTrue(Files.readString(Path.of("src/main/resources/anime_multiverse.mixins.json")).contains("HeroWeaponEnchantableMixin"));
    }

    @Test
    void theMenusAreTheTableTheAnvilAndTheResearcher() throws Exception {
        String src = read("heroweapon/bond/HeroEnchants.java");
        assertTrue(src.contains("EnchantmentMenu") && src.contains("AnvilMenu") && src.contains("ResearcherEnchantingMenu"));
        assertTrue(src.contains("s.index != 2"), "the anvil's preview slot must never be read as the weapon");
    }
}
