package com.newuniverse.nusmp.relics;

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
 * What a relic stack carries (the Relics of Many Worlds; the 1.22 Dungeon relics keep theirs in {@code nexus.relic.RelicComponents}).
 * <ul>
 *   <li>{@link #SERIAL}: the world-unique serial the God of Order's ledger stamped on this copy (written by {@code UniqueLedger.issue}).</li>
 *   <li>{@link #ATTUNED}: the player this relic is attuned to.</li>
 *   <li>{@link #USES}: the per-day counters of its acts, {@code {act_key: {d: day, n: used}}}.</li>
 *   <li>{@link #STATE}: a small integer a relic may keep for itself (charges, a mode).</li>
 * </ul>
 * All four are saved and synced so tooltips can read them. Register {@link #COMPONENTS} on the mod bus.
 */
public final class RelicComponents2 {
    private RelicComponents2() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SERIAL =
            COMPONENTS.registerComponentType("relic_serial", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ATTUNED =
            COMPONENTS.registerComponentType("relic_attuned", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> USES =
            COMPONENTS.registerComponentType("relic_uses", b -> b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STATE =
            COMPONENTS.registerComponentType("relic_state", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static void init() {}
}
