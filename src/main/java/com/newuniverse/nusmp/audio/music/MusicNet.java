package com.newuniverse.nusmp.audio.music;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the music follows facts only the server knows (which room, who is hunting the player, a boss close by), so the server sends the client ONE small payload, {@link State}
 * (a layer code, the theme id, the sign of the road: a few bytes, at most once a second and only when something changed, plus a slow keep-alive). The channel is optional, so a
 * client without the mod is never sent anything and a server without it sends nothing; the string is bounded, so a damaged packet cannot make the receiver allocate without limit.
 * The client handler is reached by name inside the lambda, so a dedicated server never loads a client class.
 */
public final class MusicNet {
    private MusicNet() {}

    /** Server to client: the current music state of the player. */
    public record State(MusicState state) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "music_state"));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of(MusicNet::write, MusicNet::read);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void write(FriendlyByteBuf b, State p) {
        MusicState s = p.state();
        b.writeByte(s.layer().code());
        b.writeUtf(s.themeId(), MusicState.MAX_THEME);
        b.writeByte(s.polarity());
    }

    public static State read(FriendlyByteBuf b) {
        MusicLayer layer = MusicLayer.fromCode(b.readByte());
        String theme = b.readUtf(MusicState.MAX_THEME);
        int polarity = b.readByte();
        return new State(new MusicState(layer, theme, polarity));
    }

    /** Mod bus: registers the payload (an optional channel). */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(State.TYPE, State.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.music.MusicClientState.receive(p.state())));
    }
}
