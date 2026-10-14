package com.newuniverse.nusmp.charter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one packet of the Charter Grimoires, {@code nusmp:charter_sync} (server to client): the set of claimed grimoires, whole, at login and
 * whenever a claim is made or cleared. Register {@link #registerPayloads} on the mod bus.
 */
public final class CharterNet {
    private CharterNet() {}

    /** One claimed grimoire: the boss id, the first recipient's name and the day. */
    public record Entry(String boss, String name, long day) {}

    public record Sync(List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "charter_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entries().size());
                    for (Entry e : p.entries()) {
                        buf.writeUtf(e.boss(), 64);
                        buf.writeUtf(e.name(), 64);
                        buf.writeVarLong(e.day());
                    }
                },
                buf -> {
                    int n = Math.min(buf.readVarInt(), 64);
                    List<Entry> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) list.add(new Entry(buf.readUtf(64), buf.readUtf(64), buf.readVarLong()));
                    return new Sync(list);
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The handler only touches a plain static, so it is safe on every side. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            Map<String, CharterClaims.Claim> now = new LinkedHashMap<>();
            for (Entry e : payload.entries()) now.put(e.boss(), new CharterClaims.Claim(e.name(), e.day()));
            CharterClaims.replace(now);
        }));
    }

    private static Sync snapshot(ModWorldDropData data) {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, DropLedger.Claim> e : data.all().entrySet()) list.add(new Entry(e.getKey(), e.getValue().name(), e.getValue().day()));
        return new Sync(list);
    }

    /** To one player (at login). */
    public static void sendTo(ServerPlayer p, ModWorldDropData data) { PacketDistributor.sendToPlayer(p, snapshot(data)); }

    /** To everybody online (a claim was made or cleared). */
    public static void sendAll(ModWorldDropData data) { PacketDistributor.sendToAllPlayers(snapshot(data)); }
}
