package com.newuniverse.nusmp.manacore;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/**
 * Why: the one list the client's 3D hand renderer reads ({@code client.WeaponRenderer}, like {@code LootItems.renderables()}): the 24 rank Mana Cores. It only forwards to {@link ManaCoreItems}, so the logic side owns the
 * registry and the renderer side names no registry detail.
 */
public final class CoreItems {
    private CoreItems() {}

    public static List<Supplier<? extends Item>> renderables() { return ManaCoreItems.suppliers(); }
}
