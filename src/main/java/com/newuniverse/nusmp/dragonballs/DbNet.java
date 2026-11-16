package com.newuniverse.nusmp.dragonballs;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The one packet of the Dragon Balls: server to client only, optional (a client without the mod simply keeps its normal sky), 6 bytes. It tells the players near a summoning that the sky goes dark for
 * {@code ticks} more ticks (set 0 / 1 picks the tint, ticks 0 = the dragon left, bring the light back). The server sends it to the players within {@value #RANGE} blocks when the dragon appears, again every
 * ten seconds while it stays (for players who walked up late) and once more when it leaves, so at most one packet per nearby player per ten seconds. The client keeps the number and expires it by itself,
 * so a lost packet can never leave the sky dark. The handler only writes {@link DbSky} (plain numbers, no client class), which the client's overlay reads.
 */
public final class DbNet {
    private DbNet() {}

    public static final double RANGE = 192.0;
    public static final int MAX_TICKS = 24000;

    public record SkyPayload(int set, int ticks) implements CustomPacketPayload {
        public static final Type<SkyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "dragonballs_sky"));
        public static final StreamCodec<FriendlyByteBuf, SkyPayload> CODEC = CustomPacketPayload.codec(SkyPayload::write, SkyPayload::read);

        private void write(FriendlyByteBuf b) {
            b.writeByte(set);
            b.writeVarInt(ticks);
        }

        private static SkyPayload read(FriendlyByteBuf b) { return new SkyPayload(b.readByte(), b.readVarInt()); }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** What the client may trust: a known set and a sane number of ticks. */
    public static boolean valid(SkyPayload p) { return p.set() >= 0 && p.set() < BallSet.values().length && p.ticks() >= 0 && p.ticks() <= MAX_TICKS; }

    public static void sendSky(ServerLevel level, Vec3 at, BallSet set, int ticks) {
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, RANGE, new SkyPayload(set.ordinal(), Math.max(0, Math.min(MAX_TICKS, ticks))));
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(SkyPayload.TYPE, SkyPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> { if (valid(p)) DbSky.set(p.set(), p.ticks()); }));
    }
}
