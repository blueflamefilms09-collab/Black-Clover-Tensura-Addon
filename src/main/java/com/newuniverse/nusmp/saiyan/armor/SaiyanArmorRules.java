package com.newuniverse.nusmp.saiyan.armor;

import java.util.List;

/**
 * Why: the numbers of the craftable Saiyan alloy armour (docs/saiyan_armor.md) in one pure place (no game type), so the material, the recipes, the set bonus, the tests and the self-test read the same
 * table. The set sits between iron and netherite on every figure (the tests pin it against the vanilla numbers written below): 17 armour points, toughness 1.5, a little knockback resistance, a
 * durability factor of 27, and the enchantability of a good iron. The set bonus is small and server-side: four worn pieces add a share of the maximum Ki per second to a Saiyan's own Ki regeneration.
 */
public final class SaiyanArmorRules {
    private SaiyanArmorRules() {}

    public static final String MATERIAL = "saiyan_alloy";
    public static final String INGOT = "saiyan_alloy_ingot";
    public static final String SCOUTER = "saiyan_alloy_scouter", BATTLE_ARMOR = "saiyan_alloy_battle_armor", WAIST_GUARD = "saiyan_alloy_waist_guard", GREAVES = "saiyan_alloy_greaves";
    /** The four pieces, head to feet (the order of {@link #DEFENSE}). */
    public static final List<String> PIECES = List.of(SCOUTER, BATTLE_ARMOR, WAIST_GUARD, GREAVES);

    /** Armour points of the helmet, chestplate, leggings and boots, and of the body slot (animal armour). */
    public static final int[] DEFENSE = {2, 7, 5, 3};
    public static final int BODY_DEFENSE = 11;
    public static final float TOUGHNESS = 1.5f, KNOCKBACK = 0.05f;
    public static final int ENCHANTABILITY = 17, DURABILITY_FACTOR = 27;

    // the vanilla 1.21.1 figures this material sits between (helmet, chestplate, leggings, boots / durability factor / toughness / knockback)
    public static final int[] IRON_DEFENSE = {2, 6, 5, 2}, NETHERITE_DEFENSE = {3, 8, 6, 3};
    public static final int IRON_FACTOR = 15, NETHERITE_FACTOR = 37;
    public static final float IRON_TOUGHNESS = 0f, NETHERITE_TOUGHNESS = 3f, IRON_KNOCKBACK = 0f, NETHERITE_KNOCKBACK = 0.1f;

    /** Pieces of the set that make the bonus. */
    public static final int SET_PIECES = 4;
    /** Default and hard cap of the set bonus, in percent of the maximum Ki regained per second. */
    public static final double DEFAULT_KI_REGEN = 1.0, MAX_KI_REGEN = 10.0;

    /** Ingots a recipe of each piece costs (the vanilla shapes: helmet 5, chestplate 8, leggings 7, boots 4). */
    public static final int[] COST = {5, 8, 7, 4};

    /** The Ki regeneration the worn pieces add (percent of the maximum per second): the whole set or nothing; never negative, never past the cap. */
    public static double kiRegenBonus(int piecesWorn, boolean enabled, double configured) {
        if (!enabled || piecesWorn < SET_PIECES || Double.isNaN(configured)) return 0.0;
        return Math.max(0.0, Math.min(MAX_KI_REGEN, configured));
    }

    public static int total(int[] v) {
        int n = 0;
        for (int x : v) n += x;
        return n;
    }
}
