package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.client.relics.item.HolderFlags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tells clients two render flags of a player, for the item layers (client/relics/item/ItemStateVariants): whether the player's body is Genesis (the
 * persistent-data flag {@code TensuraGenesisBody}) and how many leaves the player's grimoire has. Entity persistent data never leaves the server, so
 * without this the client could not know either for any player but itself. Read only: nothing on the player is written. The flags are checked once
 * a second per player (staggered by entity id), sent to everyone who can see the player only when they change, and to a player who starts
 * seeing another. Wiring (the lead): {@code modEventBus.addListener(RelicHolderSync::registerPayload)} and {@code RelicHolderSync.register(NeoForge.EVENT_BUS)}.
 */
public record RelicHolderSync(int entity, int packed) implements CustomPacketPayload {
    public static final Type<RelicHolderSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_holder"));
    public static final StreamCodec<FriendlyByteBuf, RelicHolderSync> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeVarInt(p.packed()); },
            buf -> new RelicHolderSync(buf.readVarInt(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server-side flag key (same as client/relics/item/ItemStateVariants.GENESIS_KEY). */
    private static final String GENESIS_KEY = "TensuraGenesisBody";

    private static final Map<UUID, Integer> LAST = new HashMap<>();

    /** Mod-bus listener. The client handler only runs on a client. */
    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> HolderFlags.put(p.entity(), p.packed())));
    }

    /** Game-bus listeners. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener(RelicHolderSync::onPlayerTick);
        gameBus.addListener(RelicHolderSync::onStartTracking);
        gameBus.addListener(RelicHolderSync::onLogout);
    }

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_RELIC_HOLDER_SYNC = com.newuniverse.nusmp.perf.PerfProbe.section("s.relic.holder_sync");   // 1.99.0: performance probe (off by default: one flag read)

    static void onPlayerTick(PlayerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_RELIC_HOLDER_SYNC, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 0) return;
        int now = packedFor(p);
        Integer before = LAST.put(p.getUUID(), now);
        if ((before == null ? 0 : before) != now) PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new RelicHolderSync(p.getId(), now));
    }

    static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getTarget() instanceof ServerPlayer target) || !(e.getEntity() instanceof ServerPlayer viewer)) return;
        Integer last = LAST.get(target.getUUID());
        if (last != null && last != 0) PacketDistributor.sendToPlayer(viewer, new RelicHolderSync(target.getId(), last));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST.remove(e.getEntity().getUUID()); }

    /** genesis flag and grimoire leaves, packed the way {@link HolderFlags#pack} does. */
    static int packedFor(ServerPlayer p) {
        return HolderFlags.pack(genesisBody(p), leaves(p));
    }

    static boolean genesisBody(Player p) {
        var t = p.getPersistentData();
        return t.getBoolean(GENESIS_KEY) || t.getCompound("PlayerPersisted").getBoolean(GENESIS_KEY);
    }

    /** The leaves of the player's grimoire cover (3 common, 4, 5 and above are the forbidden covers), 0 if there is none. */
    static int leaves(Player p) {
        try {
            return GrimoirePages.grimoireOf(p).map(i -> GrimoirePages.coverOf(i).tier).orElse(0);
        } catch (RuntimeException unavailable) {
            return 0;
        }
    }
}
