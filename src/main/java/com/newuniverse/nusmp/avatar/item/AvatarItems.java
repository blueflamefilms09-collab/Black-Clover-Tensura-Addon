package com.newuniverse.nusmp.avatar.item;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Why: the Avatar system's items, registered in the mod's shared item register. Like {@code CompatItems}, {@link #init()} must run from the mod
 * constructor before {@code NUItems.ITEMS.register(modBus)}; {@code AvatarSystem.init} calls it, so the lead places that one call at the same spot.
 */
public final class AvatarItems {
    private AvatarItems() {}

    /** {@code nusmp:genesis_core}: the key to creating an Avatar. */
    public static final DeferredItem<GenesisCoreItem> GENESIS_CORE = NUItems.ITEMS.register("genesis_core",
            () -> new GenesisCoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static void init() {}

    public static ItemStack coreStack() { return new ItemStack(GENESIS_CORE.get()); }
}
