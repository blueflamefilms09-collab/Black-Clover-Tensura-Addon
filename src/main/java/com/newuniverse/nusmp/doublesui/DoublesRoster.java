package com.newuniverse.nusmp.doublesui;

import com.newuniverse.nusmp.avatar.net.RollLimiter;
import com.newuniverse.nusmp.entity.MirrorDoubleEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Why: the server's book of who owns which Real Doubles and which one each owner has selected (docs/doubles_ui.md). A double adds itself when it enters the world and leaves when it
 * goes, so nothing ever scans the world: a player who has no double costs nothing, and the only per-tick work is one counter. Every {@code doublesUi.syncEveryTicks} ticks the
 * players who HAVE doubles are looked at; a packet goes to that player only when something changed (a double came or went, its health moved by 5 percent, the selection moved).
 * The selection is per owner UUID, lives only on the server, and is forgotten when the owner logs out, dies, changes dimension or respawns. Server thread only.
 */
public final class DoublesRoster {
    private DoublesRoster() {}

    /** How far (blocks) a double may be to be picked by id (the doubles' own roaming range is 48). */
    public static final double REACH = 64.0;

    private static final class Owned {
        final LinkedHashMap<UUID, MirrorDoubleEntity> list = new LinkedHashMap<>();
        UUID active;
        int sig;
        boolean dirty = true, sent;
    }

    private static final Map<UUID, Owned> OWNED = new java.util.HashMap<>();
    private static final RollLimiter LIMIT = new RollLimiter();
    private static int counter;

    // ---------------------------------------------------------------- the double's side
    /** Called by the double when it enters a ticking level (server side only). */
    public static void register(MirrorDoubleEntity d) {
        UUID owner = d.getOwnerUUID();
        if (owner == null || d.level().isClientSide) return;
        Owned o = OWNED.computeIfAbsent(owner, k -> new Owned());
        if (o.list.size() < DoublesUiMath.MAX_ROWS || o.list.containsKey(d.getUUID())) o.list.put(d.getUUID(), d);
        o.dirty = true;
    }

    /** Called by the double when it leaves the level (falls, times out, unloads). */
    public static void unregister(MirrorDoubleEntity d) {
        UUID owner = d.getOwnerUUID();
        Owned o = owner == null ? null : OWNED.get(owner);
        if (o == null || o.list.remove(d.getUUID()) == null) return;
        if (d.getUUID().equals(o.active)) o.active = null;
        o.dirty = true;
    }

    // ---------------------------------------------------------------- queries
    public static int count(UUID owner) { Owned o = OWNED.get(owner); return o == null ? 0 : o.list.size(); }

    public static UUID activeOf(UUID owner) { Owned o = OWNED.get(owner); return o == null ? null : o.active; }

    /** The double the owner selected, when it is alive in {@code level} and the setting lets it trade; else null (the caller then takes the nearest, as it always did). */
    public static MirrorDoubleEntity chosen(UUID owner, Level level) {
        if (!DoublesUiConfig.selectedDoubleTrades()) return null;
        Owned o = OWNED.get(owner);
        MirrorDoubleEntity d = o == null || o.active == null ? null : o.list.get(o.active);
        return d != null && d.isAlive() && d.level() == level ? d : null;
    }

    // ---------------------------------------------------------------- selection
    /**
     * Moves the owner's selection. {@code op} is a {@link C2S_DoubleSelectPayload} op; {@code id} is only read for SET, which must name one of THIS owner's living doubles in
     * {@code level} within {@link #REACH} of {@code pos}. Returns true when the selection changed. Pure of any packet, so the self-test drives it.
     */
    public static boolean apply(UUID owner, Level level, Vec3 pos, int op, UUID id) {
        Owned o = OWNED.get(owner);
        if (o == null) return false;
        prune(o);
        int n = o.list.size();
        if (n == 0) return false;
        int cur = o.active == null ? DoublesUiMath.AUTO : indexOf(o, o.active);
        int next;
        if (op == C2S_DoubleSelectPayload.NEXT) next = DoublesUiMath.step(cur, 1, n);
        else if (op == C2S_DoubleSelectPayload.PREV) next = DoublesUiMath.step(cur, -1, n);
        else if (op == C2S_DoubleSelectPayload.SET) {
            MirrorDoubleEntity d = id == null ? null : o.list.get(id);
            if (d == null || !d.isAlive() || d.level() != level || d.position().distanceToSqr(pos) > REACH * REACH) return false;
            next = indexOf(o, id);
        } else return false;
        UUID chosen = next < 0 ? null : keyAt(o, next);
        if (Objects.equals(chosen, o.active)) return false;
        o.active = chosen;
        o.dirty = true;
        return true;
    }

    /** The packet entry point: the sender is the owner (never a value from the packet), the rate is limited, then the selection is applied and the new picture sent at once. */
    public static void select(ServerPlayer p, byte op, UUID id) {
        if (!DoublesUiConfig.enabled() || !LIMIT.allow(p.getUUID(), System.currentTimeMillis(), DoublesUiConfig.selectGapMillis())) return;
        if (apply(p.getUUID(), p.level(), p.position(), op, id)) {
            Owned o = OWNED.get(p.getUUID());
            if (o != null) send(p, o);
        }
    }

    // ---------------------------------------------------------------- housekeeping
    public static void forget(UUID owner) { OWNED.remove(owner); LIMIT.forget(owner); }

    /** Death, respawn or a new dimension: the choice is forgotten (the doubles themselves shatter with their owner, or stay in their level). */
    public static void resetActive(UUID owner) {
        Owned o = OWNED.get(owner);
        if (o != null) { o.active = null; o.dirty = true; }
    }

    public static void clearAll() { OWNED.clear(); LIMIT.clear(); counter = 0; }

    public static void registerEvents(IEventBus bus) {
        bus.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) forget(sp.getUUID()); });
        bus.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) resetActive(sp.getUUID()); });
        bus.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) resetActive(sp.getUUID()); });
        bus.addListener((LivingDeathEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) resetActive(sp.getUUID()); });
        bus.addListener((ServerStoppingEvent e) -> clearAll());
    }

    /** One counter per tick; only every {@code syncEveryTicks} the owners that have doubles are looked at (at most one pass over the online players who cast Real Double). */
    static void tick(MinecraftServer server) {
        if (OWNED.isEmpty() || ++counter < DoublesUiConfig.syncEveryTicks()) return;
        counter = 0;
        boolean on = DoublesUiConfig.enabled();
        for (Iterator<Map.Entry<UUID, Owned>> it = OWNED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Owned> en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            Owned o = en.getValue();
            if (p == null) { it.remove(); continue; }
            if (!on) { if (o.sent) PacketDistributor.sendToPlayer(p, new S2C_DoublesPayload(S2C_DoublesPayload.NONE, List.of())); it.remove(); continue; }
            prune(o);
            int here = rowsIn(o, p.level());
            if (here == 0) {
                if (o.sent) PacketDistributor.sendToPlayer(p, new S2C_DoublesPayload(S2C_DoublesPayload.NONE, List.of()));
                it.remove();
                continue;
            }
            if (o.dirty || signature(o, p.level()) != o.sig) send(p, o);
        }
    }

    // ---------------------------------------------------------------- internals
    private static void send(ServerPlayer p, Owned o) {
        List<DoubleRow> rows = new ArrayList<>(o.list.size());
        UUID active = S2C_DoublesPayload.NONE;
        for (MirrorDoubleEntity d : o.list.values()) {
            if (d.level() != p.level()) continue;
            rows.add(new DoubleRow(d.getUUID(), d.getId(), d.getHealth(), d.getMaxHealth(), d.lifeLeft(), d.lifeMax()));
            if (d.getUUID().equals(o.active)) active = o.active;
        }
        PacketDistributor.sendToPlayer(p, new S2C_DoublesPayload(active, rows));
        o.sig = signature(o, p.level());
        o.dirty = false;
        o.sent = true;
    }

    /** Changes only when a double comes, goes, moves its health bar by a twentieth, or the selection moves (time left is counted down on the client, so it is not part of it). */
    private static int signature(Owned o, Level level) {
        int h = o.active == null ? 0 : o.active.hashCode();
        for (MirrorDoubleEntity d : o.list.values())
            if (d.level() == level) h = h * 31 + d.getUUID().hashCode() * 7 + Math.round(DoublesUiMath.frac(d.getHealth(), d.getMaxHealth()) * 20f);
        return h;
    }

    private static int rowsIn(Owned o, Level level) {
        int n = 0;
        for (MirrorDoubleEntity d : o.list.values()) if (d.level() == level) n++;
        return n;
    }

    /** Drops doubles that are gone without telling us, and the selection of one that is gone. */
    private static void prune(Owned o) {
        boolean cut = o.list.values().removeIf(d -> d.isRemoved() || !d.isAlive());
        if (o.active != null && !o.list.containsKey(o.active)) { o.active = null; cut = true; }
        if (cut) o.dirty = true;
    }

    private static int indexOf(Owned o, UUID id) {
        int i = 0;
        for (UUID k : o.list.keySet()) { if (k.equals(id)) return i; i++; }
        return DoublesUiMath.AUTO;
    }

    private static UUID keyAt(Owned o, int index) {
        int i = 0;
        for (UUID k : o.list.keySet()) { if (i++ == index) return k; }
        return null;
    }
}
