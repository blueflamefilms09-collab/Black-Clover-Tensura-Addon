package com.newuniverse.nusmp.relics.tier;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The data component of tier ascension: {@code nusmp:tier}, the absolute grade ordinal (0 NORMAL ... 6 GENESIS) a relic has been raised to.
 * Saved with the stack, synced to clients (tooltips and renderers read it), and copied by {@code ItemStack.copy()} like every component.
 * Register {@link #COMPONENTS} on the mod bus (see {@link TierSystem}).
 */
public final class TierComponents {
    private TierComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIER =
            COMPONENTS.registerComponentType("tier", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
}
