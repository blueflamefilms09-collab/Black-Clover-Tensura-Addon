package com.newuniverse.nusmp.labyrinth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: pre-rolled stats must live ON the item, saved with it and sent to the client for the tooltip, and in 1.21 that is a data component (the 1.20 NBT blob is gone).
 * {@code nusmp:gear_roll} holds a {@link GearRoll}: the rarity, the seed and floor it was rolled from and its stat lines. The saved form is
 * {@code {rarity:"rare", seed:123, depth:40, lines:[{stat:"armor", value:1.5}], tempered:false}} with every field optional and a safe default, and a line whose stat id this
 * version does not know is dropped on load rather than failing the stack (a stack written by a future version must still open in this one: an item that cannot load is an
 * item lost). The network form is compact because the component is synced to every client that can see the stack. The id is fixed by the contract and the
 * {@code DeferredRegister} is registered on the mod bus by {@link LootRegistry#init}, before the registries freeze.
 */
public final class LootComponents {
    private LootComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** A line as stored: the stat's id and the number (unknown ids are filtered when the roll is built). */
    record Raw(String stat, double value) {}

    private static final Codec<Raw> RAW = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("stat").forGetter(Raw::stat),
            Codec.DOUBLE.fieldOf("value").forGetter(Raw::value)).apply(i, Raw::new));

    private static List<Raw> raw(GearRoll g) {
        List<Raw> out = new ArrayList<>(g.lines().size());
        for (GearRoll.Line l : g.lines()) out.add(new Raw(l.kind().id(), l.value()));
        return out;
    }

    static GearRoll build(String rarity, long seed, int depth, List<Raw> lines, boolean tempered) {
        List<GearRoll.Line> out = new ArrayList<>(lines.size());
        for (Raw r : lines) {
            StatKind k = StatKind.fromId(r.stat());
            if (k != null && r.value() > 0.0) out.add(new GearRoll.Line(k, r.value()));
        }
        LootRarity r = LootRarity.fromId(rarity);
        return new GearRoll(r == null ? LootRarity.COMMON : r, seed, depth, out, tempered);
    }

    public static final Codec<GearRoll> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("rarity", LootRarity.COMMON.id()).forGetter(g -> g.rarity().id()),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(GearRoll::seed),
            Codec.INT.optionalFieldOf("depth", 0).forGetter(GearRoll::depth),
            RAW.listOf().optionalFieldOf("lines", List.of()).forGetter(LootComponents::raw),
            Codec.BOOL.optionalFieldOf("tempered", false).forGetter(GearRoll::tempered)).apply(i, LootComponents::build));

    private static final StreamCodec<ByteBuf, Raw> RAW_STREAM = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Raw::stat, ByteBufCodecs.DOUBLE, Raw::value, Raw::new);

    public static final StreamCodec<ByteBuf, GearRoll> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, (GearRoll g) -> g.rarity().id(),
            ByteBufCodecs.VAR_LONG, GearRoll::seed,
            ByteBufCodecs.VAR_INT, GearRoll::depth,
            RAW_STREAM.apply(ByteBufCodecs.list()), LootComponents::raw,
            ByteBufCodecs.BOOL, GearRoll::tempered,
            LootComponents::build);

    /** {@code nusmp:gear_roll}: a stack that has it is a pre-rolled piece of dungeon gear. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GearRoll>> GEAR_ROLL =
            COMPONENTS.registerComponentType("gear_roll", b -> b.persistent(CODEC).networkSynchronized(STREAM_CODEC));
}
