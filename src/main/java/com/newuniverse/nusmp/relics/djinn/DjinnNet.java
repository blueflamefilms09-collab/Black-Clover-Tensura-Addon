package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The two packets of the Djinn system. {@link Act} (client to server) is a press of the Djinn keys; the server decides what it means. {@link Sync}
 * (server to the bearer and everyone who can see them) is the state the client draws: the phase, the Djinn, the time left, the bars. Register
 * {@link #registerPayloads} on the mod bus.
 */
public final class DjinnNet {
    private DjinnNet() {}

    public static final int ACT_UP = 0, ACT_DOWN = 1, ACT_OFF = 2, ACT_RUKH = 3;

    /** The bits of {@link Sync#flags()}: a lent Djinn, the bearer's mark, the vessel missing (set by the client), and the Dark Djinn (the black rukh). */
    public static final int FLAG_LENT = 1, FLAG_MARKED = 2, FLAG_VESSEL_MISSING = 4, FLAG_DARK = 8;

    /** A key press: 0 = up one phase (or the Extreme Magic from a Full Equip), 1 = down one, 2 = back to none, 3 = draw the Rukh. */
    public record Act(int action) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "djinn_act"));
        public static final StreamCodec<FriendlyByteBuf, Act> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeByte(p.action()),
                buf -> new Act(buf.readByte()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * What a client needs to draw a player's Djinn. {@code phase} 0-4 (4 while an Extreme Magic charges), {@code djinn} the ordinal + 1 (0 none),
     * {@code left} the ticks left of a timed phase (a Lesser Summon, a Maso, a charge), {@code magoi} 0-100, {@code assim} tenths of a percent,
     * {@code trust} 0-100, {@code lock} ticks of Maso lockout left, {@code cooldown} ticks of Extreme Magic cooldown left, {@code pulse} the
     * ticks a strike's flash has left, {@code flags}: bit 0 a lent Djinn, bit 1 the bearer wears the Djinn's mark, bit 2 the vessel is missing,
     * bit 3 (value 8, {@link #FLAG_DARK}) the Djinn is dark: it has drunk the black rukh (relics.djinn.dark), so the equip is drawn black and gold.
     */
    public record Sync(int entity, int phase, int djinn, int left, int magoi, int assim, int trust, int lock, int cooldown, int pulse, int flags)
            implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "djinn_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entity());
                    buf.writeByte(p.phase());
                    buf.writeByte(p.djinn());
                    buf.writeVarInt(p.left());
                    buf.writeByte(p.magoi());
                    buf.writeVarInt(p.assim());
                    buf.writeByte(p.trust());
                    buf.writeVarInt(p.lock());
                    buf.writeVarInt(p.cooldown());
                    buf.writeByte(p.pulse());
                    buf.writeByte(p.flags());
                },
                buf -> new Sync(buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readVarInt(), buf.readByte(), buf.readVarInt(), buf.readByte(),
                        buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readByte()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * 1.105.0 (the Tiny Djinn): the Lesser Summon of {@code owner} is about to strike the foe {@code foe} (entity ids). Sent to everyone who sees the owner,
     * 16 ticks before the bolt lands ({@link TinyDjinnCue}), so the little figure can point, dash and arrive with it. Purely visual: it changes nothing on the server.
     */
    public record Cue(int owner, int foe) implements CustomPacketPayload {
        public static final Type<Cue> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "djinn_cue"));
        public static final StreamCodec<FriendlyByteBuf, Cue> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.owner()); buf.writeVarInt(p.foe()); },
                buf -> new Cue(buf.readVarInt(), buf.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.receive(payload)));
        r.playToClient(Cue.TYPE, Cue.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.relics.djinn.tiny.TinyDjinnClient.cue(payload.owner(), payload.foe())));
        r.playToServer(Act.TYPE, Act.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) DjinnEngine.onKey(sp, payload.action()); }));
    }
}
