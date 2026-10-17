package com.newuniverse.nusmp.arc;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: thirty-odd characters must not mean thirty spawn egg items with thirty models and thirty registrations. There is ONE item, {@code nusmp:arc_spawn_egg},
 * and the character it makes is a data component on the stack ({@code nusmp:arc_character}, the roster id), exactly as the characters themselves are one
 * NPC entity with a synced id. A stack for every character of the roster is offered to the creative tab by {@link #creativeEggs()}; the egg is tinted with
 * the character's colours on the client. The component is persistent and network-synchronised, so a stack survives a world save and shows the right tint
 * and name on the client. The item goes on {@code NUItems.ITEMS}, so {@link ArcSystem#init} must run before that register is attached to the mod bus.
 */
public final class ArcItems {
    private ArcItems() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** The roster id of the character a spawn egg makes. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CHARACTER =
            COMPONENTS.registerComponentType("arc_character", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredItem<ArcSpawnEggItem> SPAWN_EGG = NUItems.ITEMS.register("arc_spawn_egg", () -> new ArcSpawnEggItem(new Item.Properties()));

    /** Registers the data component type; call from {@link ArcSystem#init}. */
    public static void init(IEventBus modBus) { COMPONENTS.register(modBus); }

    /** A spawn egg for one character. */
    public static ItemStack eggFor(String characterId) {
        ItemStack s = new ItemStack(SPAWN_EGG.get());
        s.set(CHARACTER.get(), characterId);
        return s;
    }

    /** One egg per character of the roster, in roster order, for the creative tab (call after registries are built). */
    public static List<ItemStack> creativeEggs() {
        List<ItemStack> out = new ArrayList<>();
        for (ArcCharacter c : ArcRoster.all()) out.add(eggFor(c.id()));
        return out;
    }

    /** The character id on a stack of the egg, or empty. */
    public static String characterOf(ItemStack stack) {
        String id = stack.isEmpty() ? null : stack.get(CHARACTER.get());
        return id == null ? "" : id;
    }
}
