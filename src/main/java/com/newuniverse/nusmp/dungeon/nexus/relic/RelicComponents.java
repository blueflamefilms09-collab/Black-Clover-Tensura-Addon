package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/**
 * The data a relic's stack carries (the 1.21 form of the design's NBT): who it is attuned to, and what the living ones keep. A relic with
 * no {@link #ATTUNED_TO} is unattuned. Every one of these is saved and synced, so tooltips can show them.
 */
public final class RelicComponents {
    private RelicComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** The player the relic is attuned to, their name for the tooltip, and the bond id that must also be in that player's list. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ATTUNED_TO =
            COMPONENTS.registerComponentType("attuned_to", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ATTUNED_NAME =
            COMPONENTS.registerComponentType("attuned_name", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BOND =
            COMPONENTS.registerComponentType("relic_bond", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** The Devil-Bound Sheath: charge 0 to 100, and the ticks it has sat full while worn. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
            COMPONENTS.registerComponentType("sheath_charge", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FULL_FOR =
            COMPONENTS.registerComponentType("sheath_full_for", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The Glutton's Chalice: the game time it was last fed, and how hungry it is (HungerClock.Phase ordinal) for the tooltip. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> FED_AT =
            COMPONENTS.registerComponentType("chalice_fed_at", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HUNGER =
            COMPONENTS.registerComponentType("chalice_hunger", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The Gambler's Page: the game time it can be used again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> READY_AT =
            COMPONENTS.registerComponentType("page_ready_at", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static void init() {}
}
