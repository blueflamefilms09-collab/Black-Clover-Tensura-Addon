package com.newuniverse.nusmp.spiritlight;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.BiConsumer;

/**
 * Why: the two packets of the Spirit of Light. {@link Key}, client to server: the client only asks (a key went down); the server decides everything: the sender must be a real player, the action must be
 * one of the four it knows, at most one ask per {@link SpiritLightRules#PACKET_GAP} ticks is answered, and every action then checks the link, the cooldowns and the caps itself
 * ({@link SpiritLight#onKey}). {@link Say}, server to client: the id of ONE spoken line of the seraph and the entity id of its speaker, sent only to the players within 32 blocks of it, at most one line
 * every four seconds each ({@link SpiritLightVoice}); there is no client to server packet for it, so there is no sender to trust, the id is a bounded string checked against the line catalogue again on
 * the receiving side, and a client without the mod is never sent anything (optional channel). Everything the spirit draws travels as ordinary props, and its other sounds as ordinary level sounds.
 * Register {@link #registerPayloads} on the mod bus. The client handler is set by the client setup ({@code SpiritLightClient}) into {@link #CLIENT_VOICE}, so this class names no client class.
 */
public final class SpiritLightNet {
    private SpiritLightNet() {}

    /** The longest line id (bounded read). */
    public static final int MAX_ID = 64;

    /** Set by the client setup: plays a spoken line (the line id, the speaker's entity id). A server never sets it, and never gets a packet to call it with. */
    public static volatile BiConsumer<String, Integer> CLIENT_VOICE = (line, speaker) -> { };

    /** A key of the spirit went down: 0 link toggle, 1 beam, 2 glow pulse, 3 powered form toggle (see {@link SpiritLightRules#ACT_LINK}). */
    public record Key(int action) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "spirit_light_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.action()), buf -> new Key(buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client: the seraph (entity id {@code speaker}, a player) says this line. */
    public record Say(String line, int speaker) implements CustomPacketPayload {
        public static final Type<Say> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "spirit_light_say"));
        public static final StreamCodec<FriendlyByteBuf, Say> STREAM_CODEC = StreamCodec.of(SpiritLightNet::write, SpiritLightNet::read);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void write(FriendlyByteBuf b, Say p) {
        b.writeUtf(p.line(), MAX_ID);
        b.writeVarInt(p.speaker());
    }

    /** Reads a line; an id that is not in the catalogue reads as the empty id (which the receiver ignores). */
    static Say read(FriendlyByteBuf b) {
        String id = b.readUtf(MAX_ID);
        int speaker = b.readVarInt();
        return new Say(SpiritLightVoiceLines.has(id) ? id : "", speaker);
    }

    static void onKey(ServerPlayer p, int action) {
        if (!SpiritLightRules.validAction(action)) return;                 // an unknown action is ignored, never trusted
        SpiritLight.onKey(p, action);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Key.TYPE, Key.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.action()); }));
        r.playToClient(Say.TYPE, Say.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (SpiritLightVoiceLines.has(payload.line())) CLIENT_VOICE.accept(payload.line(), payload.speaker()); }));
    }
}
