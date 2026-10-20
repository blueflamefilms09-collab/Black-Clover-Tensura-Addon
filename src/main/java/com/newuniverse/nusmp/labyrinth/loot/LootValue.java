package com.newuniverse.nusmp.labyrinth.loot;

/**
 * Why: a Hub Merchant buys dungeon loot for coins, and the one rule that must not drift is how much. Every item of the roster has a base value in BRONZE coins (100 bronze
 * make a silver coin, as the Hub shop counts: an iron sword costs 400 there), materials and foods sell for that value, and a piece of gear sells for its base value times its
 * rarity's factor and a little more for a deep floor. The result is always at least one bronze coin and is capped far below the economy's own cap, so a stack of 64 of anything
 * never overflows a wallet. Pure.
 */
public final class LootValue {
    private LootValue() {}

    /** The largest amount one sale pays out, in bronze coins. */
    public static final long MAX_SALE = 1_000_000_000L;

    /** The price of ONE of the item in bronze coins, or 0 when the merchant does not buy it. {@code roll} is the gear roll of a piece of gear (null for everything else). */
    public static long unit(LootRoster roster, String id, GearRoll roll) {
        int base = roster.value(id);
        if (base < 0) return 0L;
        boolean gear = roster.weapon(id) != null || roster.armourPiece(id) != null || roster.trinket(id) != null;
        if (!gear) return Math.max(1, base);
        LootRarity r = roll == null ? LootRarity.COMMON : roll.rarity();
        int depth = roll == null ? 0 : roll.depth();
        return GearRollMath.sellValue(base, r, depth);
    }

    /** The price of a stack: the unit price times the count, capped. */
    public static long stack(LootRoster roster, String id, GearRoll roll, int count) {
        long u = unit(roster, id, roll);
        if (u <= 0L || count <= 0) return 0L;
        return Math.min(MAX_SALE, u * count);
    }
}
