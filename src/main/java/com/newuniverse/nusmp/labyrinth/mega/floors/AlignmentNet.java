package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

/**
 * Why: the server owns the lock, but a client that wants to draw a hint on a gate, a line in a window or a tooltip needs to know which road its player is
 * bound to, and NeoForge sends no player attachment to the client on its own unless told to. So this module sends its own small server-to-client packet,
 * {@code nusmp:alignment_sync} (the id the contract fixed), on login, respawn, dimension change and every time the state changes, and the handler fills
 * {@link ClientAlignment}. The packet carries exactly what a client can use (the road id or empty, whether it is locked, the deepest distance on each road)
 * and not the lock time, which nobody on a client needs. It is the registration pattern of the discovery system's packet ({@code StreamCodec.of}, a
 * {@code RegisterPayloadHandlersEvent} listener on the mod bus, {@code optional()} so a client without the mod can still join a server that has it); the
 * handler only touches a side-neutral holder, so it is safe on a dedicated server where no client class is loaded. Register {@link #registerPayloads} on the
 * mod bus ({@code FloorsRegistry.init} does).
 */
public final class AlignmentNet {
    private AlignmentNet() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The longest road id the packet carries ({@code abyssal} is 7); a longer string is a corrupt packet and is refused by the buffer. */
    private static final int MAX_ID = 16;

    /**
     * Why: the packet itself, a record so the encode/decode pair can be tested without a connection (the self-test {@code floors.sync} pushes one through a
     * real registry-aware buffer and back). Distances are written as var-ints and clamped to 0 or more on both ends.
     */
    public record Sync(String chosen, boolean locked, int deepestHoly, int deepestAbyssal) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "alignment_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.chosen() == null ? "" : p.chosen(), MAX_ID);
                    buf.writeBoolean(p.locked());
                    buf.writeVarInt(Math.max(0, p.deepestHoly()));
                    buf.writeVarInt(Math.max(0, p.deepestAbyssal()));
                },
                buf -> {
                    String chosen = buf.readUtf(MAX_ID);
                    boolean locked = buf.readBoolean();
                    int holy = Math.max(0, buf.readVarInt());
                    int abyssal = Math.max(0, buf.readVarInt());
                    return new Sync(chosen, locked, holy, abyssal);
                });

        /** The packet that describes a state. */
        public static Sync of(PlayerDungeonAlignment a) { return new Sync(a.chosenId(), a.locked(), a.deepestHoly(), a.deepestAbyssal()); }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The handler only touches the side-neutral holder, so it is safe on every side. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Sync.TYPE, Sync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientAlignment.receive(payload.chosen(), payload.locked(), payload.deepestHoly(), payload.deepestAbyssal())));
    }

    /**
     * Sends the player their current alignment. A stand-in player without a connection (a test's fake player) is skipped, and a failed send is logged and
     * swallowed: a missing hint on a client must never break the move that caused it.
     */
    public static void send(ServerPlayer player) {
        if (player == null || player instanceof FakePlayer || player.connection == null) return;
        try {
            PacketDistributor.sendToPlayer(player, Sync.of(AlignmentAttachments.get(player)));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The alignment of {} could not be sent: {}", player.getGameProfile().getName(), e.toString());
        }
    }
}
