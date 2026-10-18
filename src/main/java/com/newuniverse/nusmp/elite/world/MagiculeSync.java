package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.elite.world.rules.MagiculeModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the client needs the density of the chunk it stands in (the Magicule Gauge's HUD and tooltip) but must not be flooded. The sync is throttled three ways: it looks at a player only every
 * {@code syncTicks}, it sends only when the player changed chunk or a value moved by a few steps of 255 ({@link MagiculeModel#needsSync}), and the packet is 15 bytes (the chunk, the player's own
 * density with the depth bonus, and the 3 x 3 neighbourhood's stored density as bytes). A player in a level where the engine is off gets one "off" packet and then nothing.
 * The packet is registered optional, so a client without the mod's networking is not kicked.
 */
public final class MagiculeSync {
    private MagiculeSync() {}

    /** One packet: the chunk, own density (0..255, or -1 = the engine is off here), and nine cells row by row (dz -1..1, dx -1..1). */
    public record Payload(int cx, int cz, int own, byte[] cells) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "elite_magicule"));
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeInt(p.cx());
            buf.writeInt(p.cz());
            buf.writeShort(p.own());
            for (int i = 0; i < 9; i++) buf.writeByte(i < p.cells().length ? p.cells()[i] : 0);
        }, buf -> {
            int cx = buf.readInt(), cz = buf.readInt(), own = buf.readShort();
            byte[] cells = new byte[9];
            for (int i = 0; i < 9; i++) cells[i] = buf.readByte();
            return new Payload(cx, cz, own, cells);
        });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> MagiculeClientCache.accept(p.cx(), p.cz(), p.own(), p.cells())));
    }

    private static final class Last {
        int cx = Integer.MIN_VALUE, cz = Integer.MIN_VALUE, own = -1;
        final int[] cells = new int[9];
        long sentAt = Long.MIN_VALUE;
        boolean off;
    }

    private static final Map<UUID, Last> LAST = new HashMap<>();
    /** Even without a change the state is repeated this often, so a client that missed one (dimension change, reconnect) recovers. */
    private static final int REFRESH_TICKS = 1200;

    public static void forget(UUID player) { LAST.remove(player); }

    public static void clear() { LAST.clear(); }

    /** Looks at every player once; called by the engine every {@code syncTicks}. Returns the number of packets sent (for the self-test). */
    public static int tick(MinecraftServer server) {
        int sent = 0;
        long now = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Last l = LAST.computeIfAbsent(p.getUUID(), k -> new Last());
            ServerLevel level = p.serverLevel();
            if (!WorldGuard.allowed(level)) {
                if (!l.off) {
                    l.off = true;
                    l.own = -1;
                    PacketDistributor.sendToPlayer(p, new Payload(0, 0, -1, new byte[9]));
                    sent++;
                }
                continue;
            }
            int cx = p.blockPosition().getX() >> 4, cz = p.blockPosition().getZ() >> 4;
            int own = MagiculeModel.quantize(MagiculeApi.effectiveAt(p));
            int[] cells = new int[9];
            boolean moved = l.off || cx != l.cx || cz != l.cz || MagiculeModel.needsSync(l.own, own, 3);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int i = (dz + 1) * 3 + (dx + 1);
                    cells[i] = MagiculeModel.quantize(MagiculeApi.stored(level, new BlockPos((cx + dx) * 16 + 8, level.getSeaLevel(), (cz + dz) * 16 + 8)));
                    if (MagiculeModel.needsSync(l.cells[i], cells[i], 5)) moved = true;
                }
            }
            if (!moved && now - l.sentAt < REFRESH_TICKS) continue;
            byte[] b = new byte[9];
            for (int i = 0; i < 9; i++) b[i] = (byte) cells[i];
            PacketDistributor.sendToPlayer(p, new Payload(cx, cz, own, b));
            sent++;
            l.cx = cx;
            l.cz = cz;
            l.own = own;
            System.arraycopy(cells, 0, l.cells, 0, 9);
            l.sentAt = now;
            l.off = false;
        }
        return sent;
    }
}
