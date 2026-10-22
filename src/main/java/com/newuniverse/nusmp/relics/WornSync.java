package com.newuniverse.nusmp.relics;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the client drew a worn relic only if it could find it by itself: the armour slots (vanilla syncs them), a hand, and the Curios inventory read by reflection. Two things it could
 * never find: a named mob's pack (the rings, charms, cloaks and belts of the arcs' characters and the player's Avatars live in the entity's persistent data, which never leaves the
 * server, so the client always saw an empty pack), and anything at all when its own read of Curios failed (one exception pauses that read for ten seconds, and a wrong method name
 * pauses it for good). So the server now says it: the stacks a wearer carries OUTSIDE the vanilla equipment (its Curios slots, its pack) go to everybody who can see it, whenever they
 * change, to a player who starts seeing it, and to the wearer itself. The client merges that list with what it finds on its own ({@link WornMerge}); a relic that both know is drawn once.
 * Read only: nothing on the wearer is written. Wiring: {@link RelicPayloads#register} registers the packet; {@link RelicSystem#registerGame} calls {@link #register}.
 */
public record WornSync(UUID wearer, List<ItemStack> stacks) implements CustomPacketPayload {
    public static final Type<WornSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_worn_sync"));
    /** The most stacks one list carries (a player has five Curios slot types and a mob's pack is small). */
    public static final int MAX = 40;

    public static final StreamCodec<RegistryFriendlyByteBuf, WornSync> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.wearer());
                int n = Math.min(MAX, p.stacks().size());
                buf.writeVarInt(n);
                for (int i = 0; i < n; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.stacks().get(i));
            },
            buf -> {
                UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                if (n < 0 || n > MAX) throw new IllegalStateException("worn sync list too long: " + n);
                List<ItemStack> out = new ArrayList<>(n);
                for (int i = 0; i < n; i++) out.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                return new WornSync(id, out);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // ================================================================ the server: who sent what
    /** The signature of the list last sent for each wearer, and the list itself (for a viewer who starts seeing it). */
    private static final Map<UUID, String> SIG = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> SENT = new HashMap<>();

    /** Game-bus listeners. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener(WornSync::onPlayerTick);
        gameBus.addListener(WornSync::onEntityTick);
        gameBus.addListener(WornSync::onStartTracking);
        gameBus.addListener(WornSync::onLogout);
        gameBus.addListener(WornSync::onLeave);
        gameBus.addListener(WornSync::onLogin);                 // relics_wear: login, respawn and a change of dimension send the list again at the next tick (WornSyncRules.Moment)
        gameBus.addListener(WornSync::onRespawn);
        gameBus.addListener(WornSync::onDimension);
    }

    /** The key of a stack for {@link WornMerge}: its item's registry id. */
    public static String keyOf(ItemStack s) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id == null ? null : id.toString();
    }

    /** The key list of some stacks, in order. */
    public static List<String> keysOf(List<ItemStack> stacks) {
        List<String> out = new ArrayList<>(stacks.size());
        for (ItemStack s : stacks) out.add(keyOf(s));
        return out;
    }

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_RELIC_WORN_SYNC = com.newuniverse.nusmp.perf.PerfProbe.section("s.relic.worn_sync");   // 1.99.0: performance probe (off by default: one flag read)

    static void onPlayerTick(PlayerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_RELIC_WORN_SYNC, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean forced = !FORCE.isEmpty() && FORCE.remove(p.getUUID());
        if (!WornSyncRules.looksNow(p.tickCount, p.getId(), forced)) return;
        check(p, true);
    }

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_RELIC_WORN_SYNC_MOB = com.newuniverse.nusmp.perf.PerfProbe.section("s.relic.worn_sync_mob");   // 1.99.0: performance probe (off by default: one flag read)

    static void onEntityTick(EntityTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onEntityTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_RELIC_WORN_SYNC_MOB, t0);
        }
    }

    private static void onEntityTick0(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof LivingEntity mob) || mob instanceof Player || mob.level().isClientSide() || (mob.tickCount + mob.getId()) % 20 != 0) return;
        if (!MobRelics.hasPack(mob) && !SIG.containsKey(mob.getUUID())) return;             // the cheap exit for every mob that never carried a relic
        check(mob, false);
    }

    /** Reads what {@code e} carries outside the vanilla equipment and sends the list if it is not the one sent last. */
    private static void check(LivingEntity e, boolean withSelf) {
        List<ItemStack> now = new ArrayList<>(4);
        for (Relics.Located l : Relics.scanExtra(e)) if (now.size() < MAX) now.add(l.stack().copyWithCount(1));
        String sig = WornMerge.signature(keysOf(now));
        String before = SIG.getOrDefault(e.getUUID(), "");
        if (!WornSyncRules.shouldSend(before, sig)) return;
        if (now.isEmpty()) { SIG.remove(e.getUUID()); SENT.remove(e.getUUID()); }
        else { SIG.put(e.getUUID(), sig); SENT.put(e.getUUID(), now); }
        WornSync payload = new WornSync(e.getUUID(), now);
        if (withSelf) PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, payload);
        else PacketDistributor.sendToPlayersTrackingEntity(e, payload);
    }

    static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer viewer)) return;
        List<ItemStack> last = SENT.get(e.getTarget().getUUID());
        if (last != null && !last.isEmpty()) PacketDistributor.sendToPlayer(viewer, new WornSync(e.getTarget().getUUID(), last));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        forget(e.getEntity().getUUID());
        FORCE.remove(e.getEntity().getUUID());
    }

    /** Players whose list must be looked at (and sent if there is one) at their next tick, whatever the period says: a login, a respawn, a change of dimension. */
    private static final java.util.Set<UUID> FORCE = ConcurrentHashMap.newKeySet();

    /** The client's idea of this player is gone (new entity, new level): forget what was sent and send it again at the next tick. */
    static void resync(Player p, WornSyncRules.Moment why) {
        if (p == null || p.level().isClientSide() || !WornSyncRules.forcesResend(why)) return;
        forget(p.getUUID());
        if (FORCE.size() < 4096) FORCE.add(p.getUUID());
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { resync(e.getEntity(), WornSyncRules.Moment.LOGIN); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { resync(e.getEntity(), WornSyncRules.Moment.RESPAWN); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { resync(e.getEntity(), WornSyncRules.Moment.DIMENSION); }

    /** A wearer leaving a level (death of a mob, a player changing dimension, an unload): the next tick says it again if it is still carrying. */
    static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        forget(e.getEntity().getUUID());
    }

    private static void forget(UUID id) {
        SIG.remove(id);
        SENT.remove(id);
    }

    /** What the server last sent for a wearer (empty if nothing); for the debug command. */
    public static List<ItemStack> lastSent(UUID id) {
        List<ItemStack> l = SENT.get(id);
        return l == null ? List.of() : l;
    }

    // ================================================================ the client: what the server said
    private static final Map<UUID, List<ItemStack>> CLIENT = new ConcurrentHashMap<>();
    private static volatile long lastReceivedAt;
    private static volatile int received;

    /** Client: the server's list for a wearer arrived. */
    public static void put(UUID wearer, List<ItemStack> stacks) {
        received++;
        lastReceivedAt = System.currentTimeMillis();
        if (stacks.isEmpty()) CLIENT.remove(wearer);
        else {
            if (CLIENT.size() > 256) CLIENT.clear();
            CLIENT.put(wearer, List.copyOf(stacks));
        }
        Relics.invalidateClient(wearer);
    }

    /** Client: the server's list for a wearer (empty if it never sent one). */
    public static List<ItemStack> client(UUID wearer) {
        List<ItemStack> l = CLIENT.get(wearer);
        return l == null ? List.of() : l;
    }

    /** Client: how many lists have arrived since the world was joined (for the debug report). */
    public static int receivedCount() { return received; }

    /** Client: milliseconds since the last list arrived, or -1 if none did. */
    public static long sinceLastReceived() { return received == 0 ? -1L : System.currentTimeMillis() - lastReceivedAt; }

    /**
     * Client: the wearer's entity left this client's level (out of range, another dimension, respawn, death): its list is dropped ({@link WornSyncRules#keepListAfterLeave}). The server sends
     * the current list again when the entity is tracked again, so a list can never outlive the entity it was about.
     */
    public static void forgetClient(UUID wearer) {
        if (wearer == null || WornSyncRules.keepListAfterLeave()) return;
        if (CLIENT.remove(wearer) != null) Relics.invalidateClient(wearer);
    }

    /** Client: forget everything (leaving a world). */
    public static void clearClient() {
        CLIENT.clear();
        received = 0;
    }
}
