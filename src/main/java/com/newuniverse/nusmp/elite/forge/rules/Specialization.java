package com.newuniverse.nusmp.elite.forge.rules;

import java.util.List;

/**
 * Why: a smith chooses one specialization (free the first time, changeable at a cost) and it decides three things, all of them this mod's own design: (1) the
 * points it adds to the roll of recipes of its own {@link Category}; (2) one more engraving slot on items of that category (the Runesmith instead engraves at one
 * level higher everywhere, because runes are the engraving craft); (3) the pool of Tensura engravings it can put on its work (ids of the {@code #tensura:engraving}
 * tag, resolved softly at run time: an id the installed Tensura does not have is skipped). A smith without a specialization (null) uses the small
 * {@link #BASIC_POOL}. Pure Java.
 */
public enum Specialization {
    WEAPONSMITH("weaponsmith", Category.WEAPON, 0, List.of("severance", "barrier_piercing", "swift", "crushing", "sturdy", "enervation")),
    ARMORSMITH("armorsmith", Category.ARMOR, 0, List.of("sturdy", "magic_protection", "vitality", "restoration", "magicule_absorption")),
    TOOLSMITH("toolsmith", Category.TOOL, 0, List.of("sturdy", "swift", "restoration", "vitality")),
    RUNESMITH("runesmith", Category.RUNE, 1, List.of("magic_weapon", "magic_interference", "magicule_absorption", "elemental_boost", "holy_weapon", "energy_steal", "soul_eater"));

    /** Points a matching specialization adds to a roll. */
    public static final int CATEGORY_POINTS = 6;

    /** What a smith with no specialization may engrave. */
    public static final List<String> BASIC_POOL = List.of("sturdy", "swift", "restoration");

    private final String id;
    private final Category category;
    private final int levelBonus;
    private final List<String> pool;

    Specialization(String id, Category category, int levelBonus, List<String> pool) {
        this.id = id;
        this.category = category;
        this.levelBonus = levelBonus;
        this.pool = pool;
    }

    public String id() { return id; }

    public Category category() { return category; }

    /** Extra engraving levels on everything (only the Runesmith has any). */
    public int levelBonus() { return levelBonus; }

    public List<String> pool() { return pool; }

    /** Roll points for a recipe of the given category. */
    public int categoryPoints(Category c) { return c == category ? CATEGORY_POINTS : 0; }

    /** Extra engraving slots on an item of the given category (the Runesmith gets levels instead of slots). */
    public int slotBonus(Category c) { return levelBonus == 0 && c == category ? 1 : 0; }

    /** The specialization of an id, or null for none or unknown. */
    public static Specialization byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Specialization s : values()) if (s.id.equalsIgnoreCase(id)) return s;
        return null;
    }
}
