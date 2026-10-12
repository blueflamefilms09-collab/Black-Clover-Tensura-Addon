package com.newuniverse.nusmp.dungeon.nexus.nav;

import java.util.Map;

/**
 * What the Ki-Resonance Lantern burns and for how long (docs/living_dungeon.md section 3): raw Magic Stones, shards and crystals of
 * Tensura, slime. The tag {@code nusmp:lantern_fuel} says what counts as fuel; this table says how many ticks each is worth (a stone
 * about 8 minutes, a slime ball 2, the crystals 15, 20 and 30); an item a modpack adds to the tag is worth {@link #DEFAULT_TICKS}. The
 * lantern holds {@link #CAP} ticks at most. Pure.
 */
public final class LanternFuel {
    private LanternFuel() {}

    /** One hour of burning: the most a lantern holds. */
    public static final int CAP = 72000;
    /** What an item of the tag that this table does not know is worth (2 minutes). */
    public static final int DEFAULT_TICKS = 2400;

    private static final Map<String, Integer> TABLE = Map.of(
            "tensura:magic_stone", 9600,                        // 8 minutes
            "tensura:magic_ore_shard", 4800,                    // 4 minutes
            "tensura:low_quality_magic_crystal", 18000,         // 15 minutes
            "tensura:medium_quality_magic_crystal", 24000,      // 20 minutes
            "tensura:high_quality_magic_crystal", 36000,        // 30 minutes
            "minecraft:slime_ball", 2400,                       // 2 minutes
            "tensura:chilled_slime", 3600);                     // 3 minutes

    /** What one item with this registry id ("tensura:magic_stone") is worth in ticks of burning. */
    public static int ticksFor(String itemId) {
        Integer t = itemId == null ? null : TABLE.get(itemId);
        return t == null ? DEFAULT_TICKS : t;
    }

    /** How many of {@code available} items of {@code perItem} ticks each the lantern takes when {@code wanted} are offered: as many as fit under the cap. */
    public static int howMany(int fuel, int perItem, int available, int wanted) {
        if (perItem <= 0 || available <= 0 || wanted <= 0) return 0;
        int room = Math.max(0, CAP - Math.max(0, fuel));
        return Math.max(0, Math.min(Math.min(wanted, available), room / perItem));
    }

    /** The fuel after burning for {@code elapsed} ticks (never below zero). */
    public static int burn(int fuel, int elapsed) { return Math.max(0, fuel - Math.max(0, elapsed)); }

    /** The fuel as a fraction of what the lantern holds, 0 to 1 (for its bar). */
    public static float fill(int fuel) { return Math.max(0f, Math.min(1f, fuel / (float) CAP)); }

    /** Time left as "7m 05s" (rounded up to the second). */
    public static String clock(int ticks) {
        int seconds = (Math.max(0, ticks) + 19) / 20;
        return (seconds / 60) + "m " + (seconds % 60 < 10 ? "0" : "") + (seconds % 60) + "s";
    }
}
