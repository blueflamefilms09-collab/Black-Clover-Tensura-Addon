package com.newuniverse.nusmp.relics.wave;

import net.minecraft.world.item.ItemStack;

/**
 * Why: three weapons of wave 21 (Silver-Wind Mountain Glaive, Sun Gift, The Last Horseman) are printed with charges that come back over time. A charge is
 * a plain counter, not a stat, so it lives on the item stack (custom data): the number held (absent means full) and the progress toward the next one.
 * The numbers are in {@link Wave21Rules}.
 */
final class Wave21Charges {
    private Wave21Charges() {}

    private static String have(String key) { return "w21_" + key + "_have"; }

    private static String prog(String key) { return "w21_" + key + "_prog"; }

    /** Charges held (a new weapon is full). */
    static int held(ItemStack s, String key, int max) { return Math.max(0, Math.min(max, Wave0Util.stackInt(s, have(key), max))); }

    /** Pays {@code cost} charges; false (nothing spent) if there are not enough. */
    static boolean spend(ItemStack s, String key, int cost, int max) {
        int h = held(s, key, max);
        if (!Wave21Rules.canSpend(h, cost)) return false;
        Wave0Util.edit(s, t -> t.putInt(have(key), h - cost));
        return true;
    }

    /** Adds charging time (ticks) and banks the charges earned. */
    static void charge(ItemStack s, String key, int addTicks, int perCharge, int max) {
        int h = held(s, key, max);
        if (h >= max) return;
        int[] n = Wave21Rules.charge(h, Wave0Util.stackInt(s, prog(key), 0), addTicks, perCharge, max);
        Wave0Util.edit(s, t -> {
            t.putInt(have(key), n[0]);
            t.putInt(prog(key), n[1]);
        });
    }
}
