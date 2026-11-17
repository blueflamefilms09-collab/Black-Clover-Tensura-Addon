package com.newuniverse.nusmp.unique;

/**
 * Why: where the one valid copy of a unique item was last seen. HELD (a player carries it, ender chest and Curios included), DROPPED (an item entity in the world), CONTAINED (in a chest, barrel, shulker box...).
 * POOL means the copy is gone for good and the item is back in its normal sources (the world's loot, the boss, the scatter), so nothing is tracked until a new valid copy shows up. Pure, no Minecraft types.
 */
public enum UniqueState {
    HELD, DROPPED, CONTAINED, POOL;

    public static UniqueState byOrdinal(int i) { return i >= 0 && i < values().length ? values()[i] : HELD; }
}
