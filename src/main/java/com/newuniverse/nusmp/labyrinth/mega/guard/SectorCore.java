package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Why: the data a Mana Core stack carries, which is the whole difference between "the key of sector 3" and "the key of sector 4". A data
 * component (the 1.21 form of stack NBT) is saved with the stack, survives a chest, a death drop and a server restart, and is synced to
 * the client so the tooltip can read it, which an item subclass with a field could not do for stacks that are copied and merged by the
 * game. {@code sector} is the id the door compares against ({@code MegaDungeon.sectorAt(door).id}); {@code theme} is the theme id
 * ({@code gravity}, {@code blood} ...) stored only so the tooltip can colour the name on a client that cannot read the dungeon's saved
 * data. The theme is optional in the codec (an old or hand-written stack without it still opens its door), the sector is not.
 */
public record SectorCore(int sector, String theme) {
    public static final Codec<SectorCore> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("sector").forGetter(SectorCore::sector),
            Codec.STRING.optionalFieldOf("theme", "").forGetter(SectorCore::theme)).apply(i, SectorCore::new));

    public static final StreamCodec<ByteBuf, SectorCore> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SectorCore::sector,
            ByteBufCodecs.STRING_UTF8, SectorCore::theme,
            SectorCore::new);

    public SectorCore {
        if (theme == null) theme = "";
    }

    public SectorCore(int sector) { this(sector, ""); }
}
