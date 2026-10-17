package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registrations of the Shadow Core, in one class so {@link SystemRegistry#init} can force them with one call before {@code NUItems.ITEMS.register}
 * (the way {@code AvatarItems} does for the Genesis Core): the item {@code nusmp:shadow_core}, put on the mod's shared item register, and the one data component
 * it carries, {@code nusmp:shadow_core_charges}, its remaining extractions. A data component, not a damage value or NBT, because it is the 1.21 form of "a number
 * on a stack": saved, synced to the client for the tooltip, and copied with the stack. The component has NO default in the item's properties on purpose: a stack
 * without it reads as a full core ({@link ShadowCoreItem#charges}), so a core from a creative tab, a command or a loot table needs no setup, and no code
 * depends on the order the registries are filled.
 */
public final class ShadowItems {
    private ShadowItems() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** The extractions a core has left (0 to {@link CoreCharges#MAX}); absent means a full core. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGES =
            COMPONENTS.registerComponentType("shadow_core_charges", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** {@code nusmp:shadow_core}: one to a stack, never destroyed by fire (a dungeon item). */
    public static final DeferredItem<ShadowCoreItem> SHADOW_CORE = NUItems.ITEMS.register("shadow_core",
            () -> new ShadowCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Forces this class to load (and so the registrations above to be made) from the registry's {@code init}. */
    public static void init() {}
}
