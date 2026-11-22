package com.newuniverse.nusmp.arc;

import net.minecraft.world.item.Rarity;

/** Why: the rarity of a character's spawn egg, as a pure rule: a friendly character COMMON, a boss by tier (1 UNCOMMON, 2 and 3 RARE, 4 and above EPIC). */
public final class ArcEggRarity {
    private ArcEggRarity() {}

    public static Rarity of(boolean boss, int tier) {
        if (!boss) return Rarity.COMMON;
        return tier <= 1 ? Rarity.UNCOMMON : tier <= 3 ? Rarity.RARE : Rarity.EPIC;
    }
}
