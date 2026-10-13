package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What the Dungeon Delving Gear keeps on its stacks (docs/living_dungeon.md section 3), the 1.21 form of item NBT. All three are saved
 * and synced to the client, which reads them for the item properties {@code nusmp:flame} and {@code nusmp:angle}:
 * <ul>
 *   <li>{@link #FUEL}: the ticks of burning the Ki-Resonance Lantern has left (absent when empty);</li>
 *   <li>{@link #FLAME}: the lantern's flame as a {@link FlameMode} ordinal (absent when it is out);</li>
 *   <li>{@link #TARGET}: where the Anti-Bird Compass points, the middle of the nearest cluster of life (absent when there is none).</li>
 * </ul>
 */
public final class NavComponents {
    private NavComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FUEL =
            COMPONENTS.registerComponentType("fuel", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLAME =
            COMPONENTS.registerComponentType("flame", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> TARGET =
            COMPONENTS.registerComponentType("target", b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    public static void init() {}
}
