package com.newuniverse.nusmp.tooltip;

import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules.Category;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules.Hints;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules.Style;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure decisions of the unified tooltip, and a scan of the sources: every item the mod registers resolves to a style. */
class UnifiedTooltipRulesTest {
    private static Hints h(String path, String... classes) { return new Hints(path, List.of(classes), false, false, false, false, false); }

    @Test
    void whoGetsTheTooltip() {
        assertTrue(UnifiedTooltipRules.applies("nusmp", false, false));
        assertFalse(UnifiedTooltipRules.applies("minecraft", false, false), "other mods' items are untouched");
        assertFalse(UnifiedTooltipRules.applies("nusmp", true, false), "spawn eggs are excluded");
        assertFalse(UnifiedTooltipRules.applies("nusmp", false, true), "relics keep their own tooltip");
    }

    @Test
    void categoriesFromClassNamesFlagsAndPaths() {
        assertEquals(Category.HERO_WEAPON, UnifiedTooltipRules.classify(h("x", "HeroWeaponItem")));
        assertEquals(Category.WEAPON, UnifiedTooltipRules.classify(h("x", "DjinnWeaponItem")));
        assertEquals(Category.DJINN, UnifiedTooltipRules.classify(h("djinn_seal", "DjinnSealItem")));
        assertEquals(Category.GRIMOIRE, UnifiedTooltipRules.classify(h("x", "GrimoireItem")));
        assertEquals(Category.BOOK, UnifiedTooltipRules.classify(h("x", "SkillBookItem")));
        assertEquals(Category.KEY, UnifiedTooltipRules.classify(h("x", "MagicKeyItem")));
        assertEquals(Category.CORE, UnifiedTooltipRules.classify(h("x", "ManaCoreItem")));
        assertEquals(Category.ARMOUR, UnifiedTooltipRules.classify(h("x", "LootArmorItem", "ArmorItem")));
        assertEquals(Category.WEAPON, UnifiedTooltipRules.classify(new Hints("x", List.of(), false, false, false, true, false)));
        assertEquals(Category.TOOL, UnifiedTooltipRules.classify(new Hints("x", List.of(), false, false, false, false, true)));
        assertEquals(Category.FOOD, UnifiedTooltipRules.classify(new Hints("x", List.of(), false, true, false, false, false)));
        assertEquals(Category.BLOCK, UnifiedTooltipRules.classify(new Hints("x", List.of(), true, false, false, false, false)));
        assertEquals(Category.MATERIAL, UnifiedTooltipRules.classify(h("x", "LootMaterialItem")));
        assertEquals(Category.CHARM, UnifiedTooltipRules.classify(h("x", "PurificationCharmItem")));
        assertEquals(Category.KEY, UnifiedTooltipRules.classify(h("dungeon_key")));
        assertEquals(Category.CORE, UnifiedTooltipRules.classify(h("soul_core")));
        assertEquals(Category.MISC, UnifiedTooltipRules.classify(h("mystery")));
    }

    @Test
    void gradeIsTheHigherOfCategoryAndRarity() {
        assertEquals(Grade.NORMAL, UnifiedTooltipRules.grade(Category.FOOD, 0));
        assertEquals(Grade.SPECIAL, UnifiedTooltipRules.grade(Category.FOOD, 1));
        assertEquals(Grade.RARE, UnifiedTooltipRules.grade(Category.WEAPON, 0), "a weapon is at least Rare");
        assertEquals(Grade.UNIQUE, UnifiedTooltipRules.grade(Category.WEAPON, 3), "an epic weapon is Unique");
        assertEquals(Grade.LEGEND, UnifiedTooltipRules.grade(Category.HERO_WEAPON, 0));
        assertEquals(Grade.LEGEND, UnifiedTooltipRules.grade(Category.BLOCK, 99), "a very high rarity is Legend");
        assertEquals(Grade.NORMAL, UnifiedTooltipRules.grade(Category.BLOCK, -5), "a negative rarity does not throw");
        for (Category c : Category.values()) for (int r = 0; r < 6; r++) assertTrue(UnifiedTooltipRules.grade(c, r).ordinal() >= c.baseGrade().ordinal());
    }

    @Test
    void bannerHasTheRelicShape() {
        Style s = UnifiedTooltipRules.style(h("x", "MagicToolSword"), 0, null);
        assertEquals(Grade.RARE.stars() + "  Rare Grade", s.banner());
        assertEquals("Type: Weapon", s.typeLine());
        assertEquals("Multiverse of Anime  ·  Weapon", s.footer());
        assertEquals(Grade.RARE.rgb(), s.rgb());
        assertEquals("Type: Waffe", UnifiedTooltipRules.style(h("x", "MagicToolSword"), 0, "Waffe").typeLine(), "a translated label is used");
        assertEquals("Type: Weapon", UnifiedTooltipRules.style(h("x", "MagicToolSword"), 0, "  ").typeLine(), "a blank label falls back to English");
    }

    @Test
    void everyCategoryHasALabelAndAKey() {
        for (Category c : Category.values()) {
            assertFalse(c.label().isBlank());
            assertTrue(c.langKey().startsWith("tooltip.nusmp.unified.cat."));
        }
    }

    @Test
    void attributeLinesGetTheRelicPalette() {
        assertEquals(UnifiedTooltipRules.COLOUR_STAT_PLUS, UnifiedTooltipRules.statColour("attribute.modifier.plus.0"));
        assertEquals(UnifiedTooltipRules.COLOUR_STAT_PLUS, UnifiedTooltipRules.statColour("attribute.modifier.equals.0"));
        assertEquals(UnifiedTooltipRules.COLOUR_STAT_MINUS, UnifiedTooltipRules.statColour("attribute.modifier.take.1"));
        assertEquals(UnifiedTooltipRules.COLOUR_STAT_HEAD, UnifiedTooltipRules.statColour("item.modifiers.mainhand"));
        assertEquals(0, UnifiedTooltipRules.statColour("item.nusmp.something"));
        assertEquals(0, UnifiedTooltipRules.statColour(null));
    }

    @Test
    void widthFollowsTheRelicRule() {
        assertEquals(120, UnifiedTooltipRules.maxWidth(100));
        assertEquals(270, UnifiedTooltipRules.maxWidth(600));
        assertEquals(280, UnifiedTooltipRules.maxWidth(4000));
    }

    @Test
    void langFragmentCoversEveryCategory() throws IOException {
        String json = Files.readString(Path.of("tools/lang/unified_tooltips.json"));
        for (Category c : Category.values()) assertTrue(json.contains("\"" + c.langKey() + "\""), c.langKey());
    }

    /** Every id the sources register on a DeferredRegister.Items resolves to a style (the registry itself is covered by the in-game self-test ui.unified_tooltips). */
    @Test
    void everyRegisteredItemIdResolvesToAStyle() throws IOException {
        Pattern reg = Pattern.compile("\\b[A-Z_]+\\.register(?:Item|SimpleItem)?\\(\\s*\"([a-z0-9_]+)\"");
        TreeSet<String> ids = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            List<Path> java = new ArrayList<>(files.filter(p -> p.toString().endsWith(".java")).toList());
            for (Path p : java) {
                String text = Files.readString(p);
                if (!text.contains("createItems(")) continue;
                Matcher m = reg.matcher(text);
                while (m.find()) ids.add(m.group(1));
            }
        }
        assertTrue(ids.size() > 20, "the scan found the item registrations: " + ids.size());
        for (String id : ids) {
            Style s = UnifiedTooltipRules.style(h(id), 0, null);
            assertNotNull(s.category(), id);
            assertNotNull(s.grade(), id);
            assertTrue(s.banner().contains("Grade"), id);
            assertTrue(s.rgb() != 0, id);
        }
    }
}
