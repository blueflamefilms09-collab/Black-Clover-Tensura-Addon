package com.newuniverse.nusmp.shishou;

import java.util.List;
import java.util.Locale;

/**
 * Why: Skill Eater steals a skill from a monster that Shishou (or the wielder holding him) kills. Which monster gives which skill is ONE bounded table, pure and tested: exact vanilla entity ids first, then
 * keywords in the path of any other mod's mob (Tensura and Black Clover mobs are caught by their names: a "fire_" or "flame" in the id, "frost", "thunder", "shadow" ...). The first matching row wins; a mob
 * with no row gives experience only. The chance is a percentage, scaled by a server config percentage ({@link #chance}); the roll is passed in so the table is deterministic under test.
 *
 * <p>The table has a fixed number of rows ({@link #size()}), so a kill costs a short string scan once, and only for kills by the sword (the handler's gate decides that before this is called).</p>
 */
public final class ShishouStealTable {
    private ShishouStealTable() {}

    /** One row: {@code exact} (a full id such as {@code minecraft:blaze}) or {@code word} (a part of the path of an id of any namespace), the skill it gives and the base percentage. */
    public record Row(String exact, String word, ShishouSkill skill, int percent, boolean needsPowered) {
        boolean matches(String id, String path, boolean powered) {
            if (needsPowered && !powered) return false;
            return exact != null ? exact.equals(id) : path.contains(word);
        }
    }

    private static Row ex(String id, ShishouSkill s, int pct) { return new Row(id, null, s, pct, false); }

    private static Row word(String w, ShishouSkill s, int pct) { return new Row(null, w, s, pct, false); }

    private static final List<Row> ROWS = List.of(
            // vanilla, exact (the owner's table)
            ex("minecraft:blaze", ShishouSkill.FIRE_MAGIC, 40),
            ex("minecraft:magma_cube", ShishouSkill.FIRE_MAGIC, 25),
            ex("minecraft:ghast", ShishouSkill.FIRE_MAGIC, 30),
            ex("minecraft:stray", ShishouSkill.WATER_ICE_MAGIC, 40),
            ex("minecraft:snow_golem", ShishouSkill.WATER_ICE_MAGIC, 40),
            ex("minecraft:polar_bear", ShishouSkill.WATER_ICE_MAGIC, 15),
            ex("minecraft:drowned", ShishouSkill.WATER_ICE_MAGIC, 25),
            ex("minecraft:guardian", ShishouSkill.WATER_ICE_MAGIC, 30),
            ex("minecraft:elder_guardian", ShishouSkill.WATER_ICE_MAGIC, 60),
            new Row("minecraft:creeper", null, ShishouSkill.WIND_MAGIC, 60, true),               // a charged creeper: the storm
            ex("minecraft:enderman", ShishouSkill.SPATIAL_MOTION_SHORT, 35),
            ex("minecraft:spider", ShishouSkill.POISON_COAT, 35),
            ex("minecraft:cave_spider", ShishouSkill.POISON_COAT, 45),
            ex("minecraft:wither_skeleton", ShishouSkill.DARKNESS_MAGIC, 35),
            ex("minecraft:phantom", ShishouSkill.DARKNESS_MAGIC, 30),
            ex("minecraft:vex", ShishouSkill.DARKNESS_MAGIC, 30),
            ex("minecraft:witch", ShishouSkill.HEALING_MAGIC, 40),
            ex("minecraft:zombie_villager", ShishouSkill.PURIFICATION, 40),
            ex("minecraft:iron_golem", ShishouSkill.EARTH_MAGIC, 30),
            ex("minecraft:ravager", ShishouSkill.EARTH_MAGIC, 40),
            // any other namespace (Tensura, Black Clover, this mod): the words of the id
            word("flame", ShishouSkill.FIRE_MAGIC, 20),
            word("fire", ShishouSkill.FIRE_MAGIC, 20),
            word("ifrit", ShishouSkill.FIRE_MAGIC, 30),
            word("lava", ShishouSkill.FIRE_MAGIC, 20),
            word("magma", ShishouSkill.FIRE_MAGIC, 20),
            word("frost", ShishouSkill.WATER_ICE_MAGIC, 20),
            word("blizzard", ShishouSkill.WATER_ICE_MAGIC, 20),
            word("snow", ShishouSkill.WATER_ICE_MAGIC, 20),
            word("ice", ShishouSkill.WATER_ICE_MAGIC, 15),
            word("water", ShishouSkill.WATER_ICE_MAGIC, 15),
            word("thunder", ShishouSkill.WIND_MAGIC, 20),
            word("lightning", ShishouSkill.WIND_MAGIC, 20),
            word("storm", ShishouSkill.WIND_MAGIC, 20),
            word("wind", ShishouSkill.WIND_MAGIC, 20),
            word("sylph", ShishouSkill.WIND_MAGIC, 25),
            word("shadow", ShishouSkill.DARKNESS_MAGIC, 20),
            word("dark", ShishouSkill.DARKNESS_MAGIC, 15),
            word("wraith", ShishouSkill.DARKNESS_MAGIC, 20),
            word("holy", ShishouSkill.PURIFICATION, 15),
            word("angel", ShishouSkill.PURIFICATION, 15),
            word("heal", ShishouSkill.HEALING_MAGIC, 20),
            word("golem", ShishouSkill.EARTH_MAGIC, 20),
            word("stone", ShishouSkill.EARTH_MAGIC, 15),
            word("rock", ShishouSkill.EARTH_MAGIC, 15),
            word("gnome", ShishouSkill.EARTH_MAGIC, 25),
            word("poison", ShishouSkill.POISON_COAT, 20),
            word("venom", ShishouSkill.POISON_COAT, 20),
            word("viper", ShishouSkill.POISON_COAT, 20),
            word("scorpion", ShishouSkill.POISON_COAT, 20));

    public static int size() { return ROWS.size(); }

    public static List<Row> rows() { return ROWS; }

    /** The row that applies to the entity type id (for example {@code minecraft:blaze}); null when there is none. {@code powered} is true for a charged creeper. */
    public static Row lookup(String typeId, boolean powered) {
        if (typeId == null || typeId.isEmpty()) return null;
        String id = typeId.toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        for (Row r : ROWS) if (r.matches(id, path, powered)) return r;
        return null;
    }

    /** The chance (0..1) of a row after the config scale (percent of the table: 100 = as listed, 0 = nothing is stolen), never above 1. */
    public static double chance(Row row, int scalePercent) {
        if (row == null || scalePercent <= 0) return 0.0;
        return Math.min(1.0, row.percent() / 100.0 * scalePercent / 100.0);
    }

    /** Does a roll in [0, 1) steal? */
    public static boolean steals(Row row, int scalePercent, double roll) { return roll >= 0.0 && roll < chance(row, scalePercent); }

    /**
     * The elements a strike carries for the skills learned (Elemental Synthesis): Fire, Ice, Lightning (the storm), Poison, Darkness. Returned as the bit set
     * {@code 1 << element.ordinal()}.
     */
    public enum Element { FIRE, ICE, LIGHTNING, POISON, DARK }

    public static int elements(long learned) {
        int m = 0;
        if (bit(learned, ShishouSkill.FIRE_MAGIC)) m |= 1 << Element.FIRE.ordinal();
        if (bit(learned, ShishouSkill.WATER_ICE_MAGIC)) m |= 1 << Element.ICE.ordinal();
        if (bit(learned, ShishouSkill.WIND_MAGIC)) m |= 1 << Element.LIGHTNING.ordinal();
        if (bit(learned, ShishouSkill.POISON_COAT)) m |= 1 << Element.POISON.ordinal();
        if (bit(learned, ShishouSkill.DARKNESS_MAGIC)) m |= 1 << Element.DARK.ordinal();
        return m;
    }

    private static boolean bit(long learned, ShishouSkill s) { return (learned >> s.bit() & 1L) != 0L; }
}
