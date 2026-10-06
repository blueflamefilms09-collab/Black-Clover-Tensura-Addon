package com.newuniverse.nusmp.grimoire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Compatibility only. Before 0.21 every grimoire stack carried an {@code nusmp:appearance} component (the old procedural look,
 * now removed). If the id vanished from the registry, Minecraft would fail to load those stacks and the books would be lost, so the
 * id stays registered with a pass-through codec that accepts whatever old data is there. GrimoireItem strips it from a stack the
 * first time the server sees it; the 0.21 look comes from the grimoire's own data (cover, magic, canon book, owner).
 */
public final class GrimoireComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Dynamic<?>>> LEGACY_APPEARANCE =
            COMPONENTS.registerComponentType("appearance", builder -> builder.persistent(Codec.PASSTHROUGH));

    private GrimoireComponents() {}
}
