package com.newuniverse.nusmp.elite.items;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: three stack-level facts other systems need, kept as data components (persistent, so they survive a save; network-synchronised, so a tooltip and a model can read them), with the readers at the
 * bottom as the one way to look at them (a missing or wrong component always answers with a harmless default):
 * <ul>
 *   <li>{@code nusmp:elite_ref}: a free string id on the Walpurgis Orb and Seal (the council builder decides what an id means; this subsystem only stores it);</li>
 *   <li>{@code nusmp:chronicle_id}: the chronicle a Chronicle Book belongs to (the seasons builder reads it);</li>
 *   <li>{@code nusmp:runes}: the runes slotted into a Runic Sword, each an engraving id ({@code severance}) in slot order.</li>
 * </ul>
 */
public final class EliteComponents {
    private EliteComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ELITE_REF =
            COMPONENTS.registerComponentType("elite_ref", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CHRONICLE_ID =
            COMPONENTS.registerComponentType("chronicle_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> RUNES =
            COMPONENTS.registerComponentType("runes", b -> b.persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    public static void init(IEventBus modBus) { COMPONENTS.register(modBus); }

    // ------------------------------------------------------------------ readers and writers

    /** The {@code elite_ref} of a stack, or "" when it has none. */
    public static String eliteRef(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String s = stack.get(ELITE_REF.get());
        return s == null ? "" : s;
    }

    /** Sets the {@code elite_ref} of a stack (an empty id removes it). */
    public static void setEliteRef(ItemStack stack, String ref) {
        if (stack == null || stack.isEmpty()) return;
        if (ref == null || ref.isEmpty()) stack.remove(ELITE_REF.get());
        else stack.set(ELITE_REF.get(), ref);
    }

    /** The chronicle id of a Chronicle Book stack, or "". */
    public static String chronicleId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String s = stack.get(CHRONICLE_ID.get());
        return s == null ? "" : s;
    }

    /** Sets the chronicle id of a Chronicle Book (an empty id removes it). */
    public static void setChronicleId(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty()) return;
        if (id == null || id.isEmpty()) stack.remove(CHRONICLE_ID.get());
        else stack.set(CHRONICLE_ID.get(), id);
    }

    /** The runes slotted into a Runic Sword stack (empty when none). */
    public static List<String> runes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<String> l = stack.get(RUNES.get());
        return l == null ? List.of() : l;
    }

    /** Replaces the runes of a Runic Sword stack. */
    public static void setRunes(ItemStack stack, List<String> runes) {
        if (stack == null || stack.isEmpty()) return;
        if (runes == null || runes.isEmpty()) stack.remove(RUNES.get());
        else stack.set(RUNES.get(), List.copyOf(runes));
    }
}
