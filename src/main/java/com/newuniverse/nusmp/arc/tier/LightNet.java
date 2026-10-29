package com.newuniverse.nusmp.arc.tier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * Why: the one packet of the Death Note gag. The server decides the whole scene; the client only draws it, so the server sends a tiny packet to the players NEAR Light (never to anyone else)
 * that starts, cancels or ends the drawing: an op, Light's entity id, the target's entity id and a role (the target gets the full cinematic, a bystander gets bars and captions). Nothing else
 * travels: both sides read the same pure timeline ({@link LightScene}, {@link LightFrame}) from the moment of the start. A scene sends at most two of these to each viewer (start, then end or
 * cancel), and {@link LightScenes#MAX_ACTIVE} scenes run at once, so the traffic is at most a few small packets every ten seconds.
 *
 * <p>There is no client to server packet, so there is no sender to trust. The receiving side re-checks every field (op in range, ids positive, role 0 or 1) and ignores anything else; the packet
 * is optional (a client without the mod is never sent one). This class names no client class: the client setup puts its handler into {@link #CLIENT} (the pattern of the Spirit of Light's voice
 * packet); a dedicated server never sets it and never receives a packet to call it with.
 */
public final class LightNet {
    private LightNet() {}

    public static final byte OP_START = 1, OP_CANCEL = 2, OP_END = 3;
    public static final byte ROLE_BYSTANDER = 0, ROLE_TARGET = 1;

    /** What a valid packet says (the client's handler gets only these). */
    public record Scene(byte op, int light, int target, byte role) {
        public boolean valid() { return op >= OP_START && op <= OP_END && light > 0 && target > 0 && (role == ROLE_BYSTANDER || role == ROLE_TARGET); }
    }

    /** Set by the client setup: draws or stops the scene. A server never sets it. */
    public static volatile Consumer<Scene> CLIENT = s -> { };

    public record Payload(byte op, int light, int target, byte role) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "light_scene"));
        public static final StreamCodec<FriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.of(LightNet::write, LightNet::read);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        public Scene scene() { return new Scene(op, light, target, role); }
    }

    static void write(FriendlyByteBuf b, Payload p) {
        b.writeByte(p.op());
        b.writeVarInt(p.light());
        b.writeVarInt(p.target());
        b.writeByte(p.role());
    }

    /** Reads a packet; whatever is out of range reads as op 0 (which the receiver ignores). */
    static Payload read(FriendlyByteBuf b) {
        byte op = b.readByte();
        int light = b.readVarInt();
        int target = b.readVarInt();
        byte role = b.readByte();
        Scene s = new Scene(op, light, target, role);
        return s.valid() ? new Payload(op, light, target, role) : new Payload((byte) 0, 0, 0, (byte) 0);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Payload.TYPE, Payload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            Scene s = p.scene();
            if (s.valid()) CLIENT.accept(s);
        }));
    }
}
