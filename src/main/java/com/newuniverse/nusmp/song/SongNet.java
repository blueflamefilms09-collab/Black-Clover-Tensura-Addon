package com.newuniverse.nusmp.song;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: two small packets carry Song Magic across the wire. The Sing key (client to server) stands in for a microphone when Simple
 * Voice Chat is not installed; the meter packet (server to client) tells the HUD the singing level the server measured, because only
 * the server hears the voice. Register {@link #registerPayloads} on the mod bus.
 */
public final class SongNet {
    private SongNet() {}

    /** The Sing key went down (true) or up (false). */
    public record Key(boolean down) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "song_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBoolean(p.down()), buf -> new Key(buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Level 0..100, mode id, Crescendo charge 0..100 (255 = not armed). */
    public record Meter(int level, int mode, int charge) implements CustomPacketPayload {
        public static final Type<Meter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "song_meter"));
        public static final StreamCodec<FriendlyByteBuf, Meter> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeByte(p.level()); buf.writeByte(p.mode()); buf.writeByte(p.charge()); },
                buf -> new Meter(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** True when Simple Voice Chat is installed on this side: then the microphone is the instrument and the key does nothing. */
    public static boolean voiceChatLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("voicechat");
    }

    static void onKey(ServerPlayer p, boolean down) {
        if (voiceChatLoaded() || !SongConfig.keyFallback()) return;
        if (SongSkill.cachedMode(p) == SongMode.NONE) return;
        SingingState.setSteady(p.getUUID(), down ? SongConfig.keyLevel() : 0f, System.currentTimeMillis());
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Key.TYPE, Key.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.down()); }));
        r.playToClient(Meter.TYPE, Meter.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.song.SongHudState.receive(payload.level(), payload.mode(), payload.charge())));
    }
}
