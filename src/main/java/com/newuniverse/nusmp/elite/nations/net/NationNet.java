package com.newuniverse.nusmp.elite.nations.net;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.world.Msg;
import com.newuniverse.nusmp.elite.nations.world.NationActions;
import com.newuniverse.nusmp.elite.nations.world.NationService;
import com.newuniverse.nusmp.elite.nations.world.NationsConfig;
import com.newuniverse.nusmp.elite.nations.world.NationsData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Why: the network surface of the Nations system and the "sync mechanism": three payloads registered optional on the mod bus (protocol "1"). SERVER to CLIENT: {@link SnapshotPayload}. CLIENT to SERVER:
 * {@link RequestPayload} (watch / unwatch / refresh) and {@link ActionPayload} (an action word for {@link NationActions}). A player only receives snapshots while their screen is open ("watching"), at most
 * one per {@code syncMinInterval} ticks, and only when something that concerns them changed (the dirty set of the saved data) or they asked. Handlers run on the main thread, never throw (NeoForge would
 * disconnect the player) and answer the sender with a chat line. Nothing here touches a client class, so a dedicated server loads it safely.
 */
public final class NationNet {
    private NationNet() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String PROTOCOL = "1";

    /** Players whose screen is open. */
    private static final Set<UUID> WATCHING = new HashSet<>();
    private static final Map<UUID, Long> LAST_SENT = new HashMap<>();
    private static final Set<UUID> PENDING = new HashSet<>();

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL).optional();
        r.playToClient(SnapshotPayload.TYPE, SnapshotPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> clientReceive(payload)));
        r.playToServer(RequestPayload.TYPE, RequestPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> safe(ctx.player(), p -> handleRequest(p, payload))));
        r.playToServer(ActionPayload.TYPE, ActionPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> safe(ctx.player(), p -> handleAction(p, payload))));
    }

    /** Set by the client at startup to receive snapshots (a dedicated server never sets it). */
    public static volatile Consumer<SnapshotPayload> clientSink = p -> { };

    private static void clientReceive(SnapshotPayload payload) {
        try { clientSink.accept(payload); } catch (RuntimeException e) { LOG.warn("[nusmp] nation snapshot could not be shown: {}", e.toString()); }
    }

    private static void safe(net.minecraft.world.entity.player.Player pl, Consumer<ServerPlayer> body) {
        try {
            if (pl instanceof ServerPlayer sp && !(sp instanceof FakePlayer) && NationsConfig.enabled()) body.accept(sp);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] nation packet failed: {}", e.toString());
        }
    }

    private static void handleRequest(ServerPlayer p, RequestPayload req) {
        if (req.watch()) {
            WATCHING.add(p.getUUID());
            send(p, -1);
        } else {
            WATCHING.remove(p.getUUID());
            PENDING.remove(p.getUUID());
        }
    }

    private static void handleAction(ServerPlayer p, ActionPayload a) {
        Res r = NationActions.run(p, a.action(), a.a(), a.b(), a.n());
        Msg.tell(p, r);
        if (WATCHING.contains(p.getUUID())) send(p, -1);
    }

    /** Opens the screen on a tab (right click on a block, or the key through the request) and starts watching. */
    public static void open(ServerPlayer p, int tab) {
        if (!NationsConfig.enabled()) { Msg.tell(p, "nusmp.nation.m.disabled"); return; }
        WATCHING.add(p.getUUID());
        send(p, tab);
    }

    private static void send(ServerPlayer p, int tab) {
        try {
            MinecraftServer s = p.getServer();
            LAST_SENT.put(p.getUUID(), NationService.now(s));
            PENDING.remove(p.getUUID());
            PacketDistributor.sendToPlayer(p, new SnapshotPayload(NationSnapshot.build(s, p), tab));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] nation snapshot failed for {}: {}", p.getGameProfile().getName(), e.toString());
        }
    }

    /** The throttled push: players watching whose nation changed (or who have no nation and anything changed) get a fresh snapshot. */
    public static void flush(MinecraftServer s, long now) {
        NationsData d = NationService.data(s);
        int min = NationsConfig.get(NationsConfig.SYNC_MIN_INTERVAL);
        boolean any = !d.dirty.isEmpty();
        if (!any && PENDING.isEmpty()) return;
        for (Iterator<UUID> it = WATCHING.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null) { it.remove(); continue; }
            String nid = d.nationIdOf(id);
            boolean concerned = PENDING.contains(id) || (any && (nid.isEmpty() || d.dirty.contains(nid) || anyWarOrAllyConcern(d, nid)));
            if (!concerned) continue;
            Long last = LAST_SENT.get(id);
            if (last != null && now >= last && now - last < min) { PENDING.add(id); continue; }
            send(p, -1);
        }
        d.dirty.clear();
    }

    private static boolean anyWarOrAllyConcern(NationsData d, String nid) {
        // a change to another nation matters to this one when they are in a war or alliance with it (the screen shows their state)
        for (String other : d.dirty) {
            if (d.alliances.areAllied(nid, other) || d.wars.find(nid, other) != null) return true;
        }
        return false;
    }

    public static void forget(UUID id) {
        WATCHING.remove(id);
        LAST_SENT.remove(id);
        PENDING.remove(id);
    }

    public static void forgetAll() {
        WATCHING.clear();
        LAST_SENT.clear();
        PENDING.clear();
    }
}
