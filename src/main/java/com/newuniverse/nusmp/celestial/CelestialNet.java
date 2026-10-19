package com.newuniverse.nusmp.celestial;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the ledger lives on the server, but the skill's mode wheel runs on both sides (it skips the spirits the player has no key for and the Star Dresses that are still
 * closed), so the client needs a small read-only copy: the collection mask, the contract mask, the bond level per key and the king's flags. This is the one packet
 * {@code nusmp:celestial_sync} (server to client, sent on login, respawn and after every change) and the side-neutral cache it fills. The cache is only ever a view: nothing
 * the client holds can grant a key or a mode, the server re-checks everything when a mode is used.
 */
public final class CelestialNet {
    private CelestialNet() {}

    /** The read-only copy: masks, a bond level (0..5) per key ordinal, the flags of the king's modes. */
    public record View(int owned, int bound, int[] bond, boolean blessing, boolean summon) {
        public static final View EMPTY = new View(0, 0, new int[CelestialKey.values().length], false, false);

        public boolean has(CelestialKey k) { return CelestialRules.has(owned, k); }

        public boolean isBound(CelestialKey k) { return CelestialRules.has(bound, k); }

        public int bondLevel(CelestialKey k) { return k.ordinal() < bond.length ? bond[k.ordinal()] : 0; }
    }

    private static volatile View client = View.EMPTY;

    public record Sync(int owned, int bound, int[] bond, int flags) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "celestial_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeInt(p.owned());
                    buf.writeInt(p.bound());
                    buf.writeVarInt(p.flags());
                    buf.writeVarInt(p.bond().length);
                    for (int b : p.bond()) buf.writeByte(b);
                },
                buf -> {
                    int owned = buf.readInt(), bound = buf.readInt(), flags = buf.readVarInt();
                    int n = Math.max(0, Math.min(buf.readVarInt(), 64));
                    int[] bond = new int[n];
                    for (int i = 0; i < n; i++) bond[i] = buf.readByte();
                    return new Sync(owned, bound, bond, flags);
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The handler only writes the side-neutral cache. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Sync.TYPE, Sync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    int[] bond = new int[CelestialKey.values().length];
                    System.arraycopy(payload.bond(), 0, bond, 0, Math.min(bond.length, payload.bond().length));
                    client = new View(payload.owned(), payload.bound(), bond, (payload.flags() & 1) != 0, (payload.flags() & 2) != 0);
                }));
    }

    /** The server's current record of this player, sent to them. */
    public static void sync(ServerPlayer p) {
        CelestialData d = CelestialAttachments.of(p);
        int[] bond = new int[CelestialKey.values().length];
        for (CelestialKey k : CelestialKey.values()) bond[k.ordinal()] = d.bondLevel(k);
        PacketDistributor.sendToPlayer(p, new Sync(d.owned(), d.bound(), bond, (d.blessingMode() ? 1 : 0) | (d.summonMode() ? 2 : 0)));
    }

    /** The view the mode wheel reads: the live record on the server, the synced copy on the client. */
    public static View view(LivingEntity e) {
        if (e == null) return View.EMPTY;
        if (!e.level().isClientSide && e instanceof net.minecraft.world.entity.player.Player p) {
            CelestialData d = CelestialAttachments.of(p);
            int[] bond = new int[CelestialKey.values().length];
            for (CelestialKey k : CelestialKey.values()) bond[k.ordinal()] = d.bondLevel(k);
            return new View(d.owned(), d.bound(), bond, d.blessingMode(), d.summonMode());
        }
        return client;
    }
}
