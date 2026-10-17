package com.newuniverse.nusmp.labyrinth.mega.gear;

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

/**
 * Why: standard gear must be ORDINARY items that carry a grade, and in 1.21 the way to put saved, synced data on an item is a data component. The
 * component {@code nusmp:gear_grade} is the whole mark of "standard gear": no relic system looks for it, so a stack that carries it is outside the Order's
 * ledger and has no world limit, by construction (the relics keep their own components, {@code relic_serial} and the others). It holds a
 * {@link GearInfo}: the grade, the slot and the magic resistance of the piece. It is saved as {@code {grade:"rare", slot:"chestplate", resist:9}} (fields
 * are optional with safe defaults so an edited or future stack still loads) and synced to clients with a compact stream codec, because the tooltip and
 * any later client code read it. The {@code DeferredRegister} is registered on the mod bus by {@link GearRegistry#init}, which runs before the registries
 * freeze; the id is fixed by the contract.
 */
public final class GearComponents {
    private GearComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** Saved form: three optional fields, so a stack written by hand with only a grade still loads. */
    public static final Codec<GearInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("grade", GearGrade.NORMAL.id()).forGetter(GearInfo::gradeId),
            Codec.STRING.optionalFieldOf("slot", GearSlot.SWORD.id()).forGetter(GearInfo::slotId),
            Codec.INT.optionalFieldOf("resist", 0).forGetter(GearInfo::magicResistPercent)).apply(i, GearInfo::of));

    /** Network form: the two ids as strings and the resistance as a variable-length int. */
    public static final StreamCodec<ByteBuf, GearInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GearInfo::gradeId,
            ByteBufCodecs.STRING_UTF8, GearInfo::slotId,
            ByteBufCodecs.VAR_INT, GearInfo::magicResistPercent,
            GearInfo::of);

    /** {@code nusmp:gear_grade}: a stack that has it is standard gear. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GearInfo>> GEAR =
            COMPONENTS.registerComponentType("gear_grade", b -> b.persistent(CODEC).networkSynchronized(STREAM_CODEC));
}
