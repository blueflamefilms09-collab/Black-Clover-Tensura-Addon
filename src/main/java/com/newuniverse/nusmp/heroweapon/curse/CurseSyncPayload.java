package com.newuniverse.nusmp.heroweapon.curse;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the core's own sync packet carries the bond (the weapon, its level, the forms) but not the curse, and the screen tint, the corruption bar, the whispers and
 * the heartbeat all need the curse's numbers on the CLIENT. This is the curse's own small packet, server to the wielder only ({@code nusmp:hero_curse_sync}): the
 * despair and the corruption (0 to 1000), the sin of the open offer and of the active curse form (a wire number, 0 for none, see {@link CurseWire}), the ticks the
 * Overrun and the offer have left (0 for none) and the bit mask of the series embraced. It carries numbers, never text, so every player reads the messages in their
 * own language. The reader clamps everything it reads, so a damaged packet becomes a quiet screen and never an exception on the network thread.
 *
 * @param despair     0 to 1000
 * @param corruption  0 to 1000
 * @param offered     wire number of the sin whose offer is open, 0 for none
 * @param active      wire number of the sin of the wielder's active form when it is a curse form (the weapon out or not), 0 for none
 * @param overrunLeft ticks of the Overrun left, 0 when the weapon does not hold the wielder
 * @param offerLeft   ticks the open offer has left, 0 for none
 * @param series      bit mask of the embraced series (bit n = the sin at enum position n)
 */
public record CurseSyncPayload(int despair, int corruption, int offered, int active, int overrunLeft, int offerLeft, int series) implements CustomPacketPayload {
    public static final Type<CurseSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_curse_sync"));
    public static final StreamCodec<FriendlyByteBuf, CurseSyncPayload> CODEC = StreamCodec.of(CurseSyncPayload::write, CurseSyncPayload::read);

    /** The packet that says "no curse": everything off (sent when a weapon is bound or released and after a death). */
    public static CurseSyncPayload none() { return new CurseSyncPayload(0, 0, 0, 0, 0, 0, 0); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static void write(FriendlyByteBuf b, CurseSyncPayload p) {
        b.writeVarInt(CurseWire.clamp(p.despair(), 0, CurseTuning.MAX));
        b.writeVarInt(CurseWire.clamp(p.corruption(), 0, CurseTuning.MAX));
        b.writeByte(CurseWire.clamp(p.offered(), 0, 127));
        b.writeByte(CurseWire.clamp(p.active(), 0, 127));
        b.writeVarInt(Math.max(0, p.overrunLeft()));
        b.writeVarInt(Math.max(0, p.offerLeft()));
        b.writeVarInt(Math.max(0, p.series()));
    }

    private static CurseSyncPayload read(FriendlyByteBuf b) {
        int despair = CurseWire.clamp(b.readVarInt(), 0, CurseTuning.MAX);
        int corruption = CurseWire.clamp(b.readVarInt(), 0, CurseTuning.MAX);
        int offered = b.readByte();
        int active = b.readByte();
        int overrun = CurseWire.clamp(b.readVarInt(), 0, 1_000_000);
        int offer = CurseWire.clamp(b.readVarInt(), 0, 1_000_000);
        int series = CurseWire.clamp(b.readVarInt(), 0, 0xFFFF);
        return new CurseSyncPayload(despair, corruption, offered, active, overrun, offer, series);
    }
}
