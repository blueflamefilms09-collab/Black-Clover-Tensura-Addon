package com.newuniverse.nusmp.item;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * 1.97.0: the {@code nusmp:elsdocia_lineage} data component, the lineage of one Elsdocia ({@link ElsdociaLineage}) as a list of text lines: saved with the
 * stack, synced to clients (tooltips and the held motes read it) and copied with the stack. A stack's component values are shared, so writing always
 * sets a fresh list. Register {@link #COMPONENTS} on the mod bus ({@link ElsdociaEvents#registerContent}).
 */
public final class ElsdociaComponents {
    private ElsdociaComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> LINEAGE =
            COMPONENTS.registerComponentType("elsdocia_lineage", b -> b
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(2048))));

    /** The lineage of a stack ({@link ElsdociaLineage#EMPTY} if it has none). Never changes the stack. */
    public static ElsdociaLineage get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ElsdociaLineage.EMPTY;
        List<String> lines = stack.get(LINEAGE.get());
        return lines == null ? ElsdociaLineage.EMPTY : ElsdociaLineage.decode(lines);
    }

    /** Writes the lineage onto the stack. */
    public static void set(ItemStack stack, ElsdociaLineage lineage) {
        if (stack == null || stack.isEmpty()) return;
        stack.set(LINEAGE.get(), lineage.encode());
    }
}
