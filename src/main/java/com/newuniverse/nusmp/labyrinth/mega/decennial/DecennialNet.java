package com.newuniverse.nusmp.labyrinth.mega.decennial;

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
 * Why: a client decides alone whether it may start cracking a block, so it has to be told which blocks of the hall it stands in are the interactive elements. The server sends one small packet,
 * {@code nusmp:decennial_breakables}, with the cells of the elements of the hall the player is in (and an empty one when the player leaves), and the handler fills {@link BreakableCells}, which is
 * side-neutral. It is the registration pattern of the alignment packet ({@code StreamCodec.of}, a {@code RegisterPayloadHandlersEvent} listener on the mod bus, {@code optional()} so a client without
 * the mod still joins; such a client simply cannot start the crack, and the server still refuses it correctly). A hall has at most a few thousand element cells, so the list is bounded.
 */
public final class DecennialNet {
    private DecennialNet() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The most cells one packet carries (a hall has fewer than 3000; a longer list is a corrupt packet and is refused by the buffer). */
    public static final int MAX_CELLS = 8192;

    /** The cells (packed block positions) the receiving client may break. */
    public record Breakables(long[] cells) implements CustomPacketPayload {
        public static final Type<Breakables> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "decennial_breakables"));
        public static final StreamCodec<FriendlyByteBuf, Breakables> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeLongArray(p.cells().length > MAX_CELLS ? java.util.Arrays.copyOf(p.cells(), MAX_CELLS) : p.cells()),
                buf -> new Breakables(buf.readLongArray(null, MAX_CELLS)));

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The handler only touches the side-neutral holder, so it is safe on every side. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Breakables.TYPE, Breakables.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> BreakableCells.clientSet(payload.cells())));
    }

    /** Sends the player the cells they may break (an empty array clears the list). A stand-in without a connection is skipped, and a failed send is logged and swallowed. */
    public static void send(ServerPlayer player, long[] cells) {
        if (player == null || player instanceof FakePlayer || player.connection == null) return;
        try {
            PacketDistributor.sendToPlayer(player, new Breakables(cells));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The breakable cells of a hall could not be sent to {}: {}", player.getGameProfile().getName(), e.toString());
        }
    }
}
