package com.newuniverse.nusmp.tooltip;

import com.newuniverse.nusmp.relics.Grade;

import java.util.List;
import java.util.Locale;

/**
 * Why: the owner wants every item of the mod to show the same custom tooltip as the relics (the name in the grade's colour, a grade banner with its stars, a type line,
 * a hairline, the stat and lore lines, a footer). The relics know their grade from their card; every other item has none, so this pure class decides what grade and what
 * type an item shows, from a few plain facts about it (its registry path, the class names of its type chain, a handful of flags the game side reads). No Minecraft type is
 * used here, so the decisions are unit tested; the game side is {@link UnifiedTooltipItems} (reads the item), the drawing is {@code client.ui.tooltip.UnifiedTooltip}.
 * <p>
 * The grade of an item is the HIGHER of (a) the default grade of its category (a weapon is at least Rare, a grimoire Unique, a hero weapon Legend, a meal Normal) and
 * (b) what the item's own rarity says (the vanilla rarity: common Normal, uncommon Special, rare Rare, epic Unique), so a rolled dungeon weapon keeps its roll's rarity and a
 * plain stone block stays Normal. Nothing here changes a stat; it only chooses words and colours.
 */
public final class UnifiedTooltipRules {
    private UnifiedTooltipRules() {}

    /** What kind of item it is, for the type line, the footer and the default grade. Entries are only appended. */
    public enum Category {
        WEAPON("Weapon", Grade.RARE),
        ARMOUR("Armour", Grade.RARE),
        TOOL("Tool", Grade.SPECIAL),
        FOOD("Food", Grade.NORMAL),
        MATERIAL("Material", Grade.NORMAL),
        BOOK("Skill Book", Grade.RARE),
        GRIMOIRE("Grimoire", Grade.UNIQUE),
        KEY("Key", Grade.RARE),
        CORE("Core", Grade.UNIQUE),
        CHARM("Charm", Grade.RARE),
        HERO_WEAPON("Hero Weapon", Grade.LEGEND),
        DJINN("Djinn", Grade.LEGEND),
        BLOCK("Block", Grade.NORMAL),
        CURRENCY("Currency", Grade.NORMAL),
        MISC("Item", Grade.NORMAL);

        private final String label;
        private final Grade base;

        Category(String label, Grade base) {
            this.label = label;
            this.base = base;
        }

        public String label() { return label; }

        public Grade baseGrade() { return base; }

        /** The lang key of the label (the fragment {@code unified_tooltips} ships it; the English label is the fallback). */
        public String langKey() { return "tooltip.nusmp.unified.cat." + name().toLowerCase(Locale.ROOT); }
    }

    /** The plain facts about an item that the game side reads. {@code classNames} are the simple class names from the item's class up to (not including) {@code Item}. */
    public record Hints(String path, List<String> classNames, boolean block, boolean food, boolean armour, boolean weapon, boolean tool) {
        public Hints {
            path = path == null ? "" : path;
            classNames = classNames == null ? List.of() : classNames;
        }
    }

    /** What the tooltip shows for one stack. */
    public record Style(Category category, Grade grade, String banner, String typeLine, String footer) {
        /** The colour of the name, the banner and the border as 0xRRGGBB. */
        public int rgb() { return grade.rgb(); }

        public boolean genesis() { return grade == Grade.GENESIS; }
    }

    /** The mod id whose items get the tooltip. */
    public static final String NAMESPACE = "nusmp";

    /** The footer's first words. */
    public static final String FOOTER_NAME = "Multiverse of Anime";

    // ---------------------------------------------------------------- who gets it
    /**
     * Does this item get the unified tooltip? Items of this mod only; a spawn egg is left alone (the owner excluded them); a relic keeps the relic tooltip it always had (it IS
     * the model).
     */
    public static boolean applies(String namespace, boolean spawnEgg, boolean relic) {
        return NAMESPACE.equals(namespace) && !spawnEgg && !relic;
    }

    // ---------------------------------------------------------------- category
    /** The category of an item from its hints. Flags the game side is sure about win; then the class names; then the registry path. Never null. */
    public static Category classify(Hints h) {
        String names = String.join(" ", h.classNames()).toLowerCase(Locale.ROOT);
        String p = h.path().toLowerCase(Locale.ROOT);
        if (names.contains("heroweapon")) return Category.HERO_WEAPON;
        if (names.contains("djinn") && (names.contains("weapon") || h.weapon())) return Category.WEAPON;
        if (names.contains("djinn")) return Category.DJINN;
        if (names.contains("grimoire")) return Category.GRIMOIRE;
        if (names.contains("skillbook") || names.contains("spbook") || names.contains("ftbook") || names.contains("codex")) return Category.BOOK;
        if (names.contains("key")) return Category.KEY;
        if (names.contains("core")) return Category.CORE;
        if (h.armour() || names.contains("armor") || names.contains("armour") || names.contains("robe")) return Category.ARMOUR;
        if (h.weapon() || names.contains("weapon") || names.contains("sword") || names.contains("bow") || names.contains("trident") || names.contains("spear")
                || names.contains("staff") || names.contains("blade")) return Category.WEAPON;
        if (h.tool() || names.contains("tool") || names.contains("pickaxe") || names.contains("axe") || names.contains("paint")) return Category.TOOL;
        if (h.food() || names.contains("food") || names.contains("meal")) return Category.FOOD;
        if (names.contains("trinket") || names.contains("charm") || names.contains("seal") || names.contains("relic")) return Category.CHARM;
        if (names.contains("material") || names.contains("gem") || names.contains("fragment") || names.contains("shard") || names.contains("magicstone")) return Category.MATERIAL;
        if (h.block() || names.contains("blockitem")) return Category.BLOCK;
        // a plain Item: the registry path is all there is (matched as whole words between underscores, so "string" is not a ring and "score" is not an ore)
        if (word(p, "grimoire")) return Category.GRIMOIRE;
        if (word(p, "book") || word(p, "tome") || word(p, "codex")) return Category.BOOK;
        if (word(p, "key")) return Category.KEY;
        if (word(p, "core")) return Category.CORE;
        if (word(p, "coin") || word(p, "coins")) return Category.CURRENCY;
        if (word(p, "sword") || word(p, "blade") || word(p, "spear") || word(p, "staff") || word(p, "bow") || word(p, "trident") || word(p, "dagger") || word(p, "axe")) return Category.WEAPON;
        if (word(p, "helmet") || word(p, "chestplate") || word(p, "leggings") || word(p, "boots") || word(p, "robe")) return Category.ARMOUR;
        if (word(p, "charm") || word(p, "ring") || word(p, "amulet") || word(p, "seal") || word(p, "trinket")) return Category.CHARM;
        if (word(p, "ingot") || word(p, "shard") || word(p, "fragment") || word(p, "dust") || word(p, "essence") || word(p, "gem") || word(p, "crystal") || word(p, "scale")
                || word(p, "bone") || word(p, "fang") || word(p, "hide") || word(p, "ore")) return Category.MATERIAL;
        if (word(p, "stew") || word(p, "meal") || word(p, "bread") || word(p, "soup") || word(p, "elixir") || word(p, "potion") || word(p, "meat") || word(p, "fruit")) return Category.FOOD;
        return Category.MISC;
    }

    /** Is {@code w} one of the underscore-separated words of the path? */
    private static boolean word(String path, String w) { return ("_" + path + "_").contains("_" + w + "_"); }

    // ---------------------------------------------------------------- grade
    /** The grade that a vanilla rarity ordinal (0 common, 1 uncommon, 2 rare, 3 epic) stands for; anything above is Legend. */
    public static Grade gradeOfRarity(int vanillaRarityOrdinal) {
        return switch (Math.max(0, vanillaRarityOrdinal)) {
            case 0 -> Grade.NORMAL;
            case 1 -> Grade.SPECIAL;
            case 2 -> Grade.RARE;
            case 3 -> Grade.UNIQUE;
            default -> Grade.LEGEND;
        };
    }

    /** The grade shown: the higher of the category's default and the rarity's. */
    public static Grade grade(Category category, int vanillaRarityOrdinal) {
        return Grade.max(category.baseGrade(), gradeOfRarity(vanillaRarityOrdinal));
    }

    // ---------------------------------------------------------------- the style
    /** The whole style of an item. {@code categoryLabel} is the (translated) label of the category; null means the English one. */
    public static Style style(Hints h, int vanillaRarityOrdinal, String categoryLabel) {
        return style(classify(h), vanillaRarityOrdinal, categoryLabel);
    }

    /** The same for a category that was already worked out (1.112.0, lag audit: {@code UnifiedTooltipItems} keeps the category and the finished style per item, the tooltip is gathered every frame). */
    public static Style style(Category c, int vanillaRarityOrdinal, String categoryLabel) {
        Grade g = grade(c, vanillaRarityOrdinal);
        String label = categoryLabel == null || categoryLabel.isBlank() ? c.label() : categoryLabel;
        return new Style(c, g, banner(g), typeLine(label), footer(label));
    }

    /** "stars  Rare Grade", the same words as a relic's grade row. */
    public static String banner(Grade g) { return g.stars() + "  " + g.label(); }

    /** "Type: Weapon", the same shape as a relic's "Slot: Main hand" row. */
    public static String typeLine(String categoryLabel) { return "Type: " + categoryLabel; }

    /** "Multiverse of Anime  -  Weapon", the dim footer line. */
    public static String footer(String categoryLabel) { return FOOTER_NAME + "  ·  " + categoryLabel; }

    // ---------------------------------------------------------------- the colours (the relic tooltip's palette)
    public static final int COLOUR_TYPE = 0xFF8899AA, COLOUR_FOOTER = 0xFF777788, COLOUR_STAT_PLUS = 0xFF7CE08A, COLOUR_STAT_MINUS = 0xFFFF6666, COLOUR_STAT_HEAD = 0xFF8899AA;

    /** The colour of a vanilla attribute line by its lang key ({@code attribute.modifier.plus.0} and so on), or 0 when the line is not one of them. */
    public static int statColour(String translationKey) {
        if (translationKey == null) return 0;
        if (translationKey.startsWith("attribute.modifier.plus") || translationKey.startsWith("attribute.modifier.equals")) return COLOUR_STAT_PLUS;
        if (translationKey.startsWith("attribute.modifier.take")) return COLOUR_STAT_MINUS;
        if (translationKey.startsWith("item.modifiers.")) return COLOUR_STAT_HEAD;
        return 0;
    }

    /** The text width the tooltip wraps to: 45% of the screen, 120 to 280 pixels (the relic tooltip's rule). */
    public static int maxWidth(int screenWidth) { return Math.max(120, Math.min(280, (int) (screenWidth * 0.45))); }
}
