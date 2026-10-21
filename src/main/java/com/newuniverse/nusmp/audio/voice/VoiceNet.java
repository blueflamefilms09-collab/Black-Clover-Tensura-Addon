package com.newuniverse.nusmp.audio.voice;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the server decides WHEN a spoken line is said (it owns the floors, the bosses, the NPCs and the Djinn), the client only plays the file. So the payload is tiny: the id of one line,
 * server to client only (there is no client-to-server packet, so there is no sender to trust). The id is read with a bounded string and checked against {@link VoiceCatalog} on the
 * receiving side too, so a damaged or forged packet can neither allocate without limit nor play an event that does not exist. The channel is optional, so a client without the mod is
 * never sent anything. The client handler is reached by name inside the lambda, so a dedicated server never loads a client class.
 */
public final class VoiceNet {
    private VoiceNet() {}

    /** The longest line id there is (bounded read). */
    public static final int MAX_ID = 64;

    /** Server to client: say this line. */
    public record Say(String line) implements CustomPacketPayload {
        public static final Type<Say> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "voice_say"));
        public static final StreamCodec<FriendlyByteBuf, Say> CODEC = StreamCodec.of(VoiceNet::write, VoiceNet::read);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void write(FriendlyByteBuf b, Say p) { b.writeUtf(p.line(), MAX_ID); }

    /** Reads a line id; an id that is not in the catalogue reads as the empty id (which the receiver ignores). */
    public static Say read(FriendlyByteBuf b) {
        String id = b.readUtf(MAX_ID);
        return new Say(VoiceCatalog.has(id) ? id : "");
    }

    /** Mod bus: registers the payload (an optional channel). */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Say.TYPE, Say.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (VoiceCatalog.has(p.line())) com.newuniverse.nusmp.client.voice.VoiceClient.play(p.line());
        }));
    }
}
