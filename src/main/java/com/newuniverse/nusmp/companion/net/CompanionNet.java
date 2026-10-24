package com.newuniverse.nusmp.companion.net;

import com.newuniverse.nusmp.companion.CompanionConfig;
import com.newuniverse.nusmp.companion.talk.Talk;
import com.newuniverse.nusmp.companion.talk.TalkRules;
import com.newuniverse.nusmp.companion.talk.TalkView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * Why: the companion system talks over four small packets and a fifth for bubbles, all optional channels. Two go UP and are only asks: PICK (the id of a choice on the screen) and CLOSE. The server decides everything: the
 * sender must be a real player with a conversation open with that very entity, the id must be on the last screen it was sent, the entity must be alive and within 8 blocks, and picks are rate limited (see
 * {@link Talk}). Three go DOWN, only to the player concerned (or the players tracking the speaker for a bubble): SHOW (a whole screen), END and BUBBLE. The client side is hooked in by the {@code volatile} consumers
 * below (set by {@code client.companion.CompanionClient}), so this common file names no client class and a dedicated server never loads one. Every read is bounded: ids by {@link TalkRules#WIRE_ID}, a view by 40
 * options and nine pouch slots, a bubble by its text.
 */
public final class CompanionNet {
    private CompanionNet() {}

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Show> clientShow;
    public static volatile Consumer<End> clientEnd;
    public static volatile Consumer<BubbleLine> clientBubble;

    // ---------------------------------------------------------------- client to server
    /** The player picked a choice (an id on the screen they were last sent) in the conversation with this entity. */
    public record Pick(int entityId, String id) implements CustomPacketPayload {
        public static final Type<Pick> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "companion_pick"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Pick> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.entityId()); buf.writeUtf(p.id(), TalkRules.WIRE_ID); },
                buf -> new Pick(buf.readVarInt(), buf.readUtf(TalkRules.WIRE_ID)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The player closed the screen. */
    public record Close(int entityId) implements CustomPacketPayload {
        public static final Type<Close> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "companion_close"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Close> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeVarInt(p.entityId()), buf -> new Close(buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- server to client
    /** A whole screen of the conversation with this entity (the first one opens the talk screen, later ones replace its content). */
    public record Show(int entityId, TalkView view) implements CustomPacketPayload {
        public static final Type<Show> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "companion_show"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Show> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.entityId()); TalkView.STREAM_CODEC.encode(buf, p.view()); },
                buf -> new Show(buf.readVarInt(), TalkView.STREAM_CODEC.decode(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The conversation is over (the server closed it): the screen closes. */
    public record End(int entityId) implements CustomPacketPayload {
        public static final Type<End> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "companion_end"));
        public static final StreamCodec<RegistryFriendlyByteBuf, End> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeVarInt(p.entityId()), buf -> new End(buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A speech bubble over an entity for some ticks. */
    public record BubbleLine(int entityId, Component line, int ticks) implements CustomPacketPayload {
        public static final Type<BubbleLine> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "companion_bubble"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BubbleLine> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.entityId()); ComponentSerialization.STREAM_CODEC.encode(buf, p.line()); buf.writeVarInt(p.ticks()); },
                buf -> new BubbleLine(buf.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buf), Math.max(1, Math.min(buf.readVarInt(), 1200))));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- registration
    /** Mod bus: registers the five payloads (an optional channel: a client without the mod is never sent anything). */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Pick.TYPE, Pick.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) Talk.onPick(sp, p.entityId(), p.id());
        }));
        r.playToServer(Close.TYPE, Close.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) Talk.onClose(sp, p.entityId());
        }));
        r.playToClient(Show.TYPE, Show.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            Consumer<Show> c = clientShow;
            if (c != null) c.accept(p);
        }));
        r.playToClient(End.TYPE, End.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            Consumer<End> c = clientEnd;
            if (c != null) c.accept(p);
        }));
        r.playToClient(BubbleLine.TYPE, BubbleLine.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            Consumer<BubbleLine> c = clientBubble;
            if (c != null) c.accept(p);
        }));
    }

    // ---------------------------------------------------------------- the server's sends (each checks the setting)
    public static void send(ServerPlayer to, Show s) {
        if (CompanionConfig.enabled() && to != null && to.connection != null) PacketDistributor.sendToPlayer(to, s);
    }

    public static void send(ServerPlayer to, End e) {
        if (to != null && to.connection != null) PacketDistributor.sendToPlayer(to, e);
    }
}
