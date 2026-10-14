package com.newuniverse.nusmp.relics.djinn;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/**
 * What a Metal Vessel's stack carries once a Djinn is bound to it: the Djinn's id ({@code djinn_bound}, "baal") and the serial of the binding
 * ({@code djinn_serial}); the bearer's {@link DjinnState} remembers the same serial, so a vessel that was released, stolen or copied stops
 * counting the moment the two disagree. Both are saved and synced (the tooltip reads them). Register {@link #COMPONENTS} on the mod bus.
 */
public final class DjinnComponents {
    private DjinnComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BOUND =
            COMPONENTS.registerComponentType("djinn_bound", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SERIAL =
            COMPONENTS.registerComponentType("djinn_serial", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /**
     * {@code nusmp:djinn_temp}: marks a Djinn weapon as the temporary item of a Half or Full Equip: {@code {owner: <uuid>, djinn: "baal", expires: <game
     * time or 0>, admin: <bool>}}. Saved and synced (the tooltip reads it). See {@code relics.djinn.weapon.DjinnTemp}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> TEMP =
            COMPONENTS.registerComponentType("djinn_temp", b -> b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

    public static void init() {}
}
