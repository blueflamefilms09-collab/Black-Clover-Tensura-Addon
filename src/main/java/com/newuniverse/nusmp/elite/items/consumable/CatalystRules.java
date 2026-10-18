package com.newuniverse.nusmp.elite.items.consumable;

/**
 * Why: the arithmetic of the Potential Catalyst with no Minecraft in it. The catalyst is in the style of the SP Books (a consumable that raises the player for good), but unlike a book it also gives a
 * PERMANENT health step, and because that is unbounded by nature the total number of catalysts one player can use is limited by the config. The bonus is derived from the count of uses (not stored as
 * a modifier), so it can be rebuilt after a death, a login or a respawn without drift.
 */
public final class CatalystRules {
    private CatalystRules() {}

    /** May a player who has used {@code uses} catalysts use another, when the limit is {@code max}? */
    public static boolean canUse(int uses, int max) { return uses >= 0 && uses < max; }

    /** The total maximum-health bonus (in health points; 2 = a heart) of {@code uses} catalysts at {@code perUse} each; never negative. */
    public static double healthBonus(int uses, int perUse) { return (double) Math.max(0, uses) * Math.max(0, perUse); }

    /** The uses left, never negative. */
    public static int left(int uses, int max) { return Math.max(0, max - Math.max(0, uses)); }
}
