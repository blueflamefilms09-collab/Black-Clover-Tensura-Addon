package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the {@code nusmp:appearance} data component (persistent + synced to clients). Register {@link #COMPONENTS} on the mod bus. */
public final class GrimoireComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GrimoireAppearance>> APPEARANCE =
            COMPONENTS.registerComponentType("appearance", builder -> builder
                    .persistent(GrimoireAppearance.CODEC)
                    .networkSynchronized(GrimoireAppearance.STREAM_CODEC)
                    .cacheEncoding());

    private GrimoireComponents() {}
}
