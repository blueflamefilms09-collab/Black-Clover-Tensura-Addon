package com.newuniverse.nusmp.elite.races.net;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.races.runtime.RaceTransforms;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the trust boundary of the race transformations in one place. The hotkey's request is registered as a server-bound payload and starts from the same three rules as every other
 * request of this mod: the sender must be a live server player, the requests are rate-limited (one every 5 server ticks), and the decision is made by {@link RaceTransforms}
 * (the same code the commands use), never by the client: the packet carries one of three words and no number. The state message is client-bound; its handler reaches the client
 * class by name inside a lambda so a dedicated server never loads it. Both payloads are optional with protocol "1", like every other payload of the mod, so a client without them is
 * not refused. Handlers never throw (NeoForge disconnects a player whose handler throws).
 */
public final class RaceNet {
    private RaceNet() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String PROTOCOL = "1";
    private static final long MIN_GAP_TICKS = 5L;
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL).optional();
        r.playToServer(RacePayloads.Request.TYPE, RacePayloads.Request.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> handle(payload, ctx)));
        r.playToClient(RacePayloads.State.TYPE, RacePayloads.State.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.elite.races.client.RaceClientHooks.onState(payload.state(), payload.remaining(), payload.color())));
    }

    private static void handle(RacePayloads.Request payload, IPayloadContext ctx) {
        try {
            if (!(ctx.player() instanceof ServerPlayer p) || !p.isAlive()) return;
            long now = p.serverLevel().getGameTime();
            Long last = LAST.put(p.getUUID(), now);
            if (last != null && now - last < MIN_GAP_TICKS) return;
            RaceTransforms.Mode mode = switch (payload.want()) {
                case "auto" -> RaceTransforms.Mode.AUTO;
                case "cycle" -> RaceTransforms.Mode.CYCLE;
                case "off" -> RaceTransforms.Mode.OFF;
                default -> null;
            };
            if (mode == null) return;
            RaceTransforms.Outcome o = RaceTransforms.request(p, mode, null, false);
            if (!o.ok()) p.displayClientMessage(o.message(), true);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] elite races: a transformation request failed", e);
        }
    }

    /** Tells one client which form it is in (and for how long); a player without the mod simply ignores the optional payload. */
    public static void sendState(ServerPlayer p, String stateId, int remainingTicks, int argb) {
        try {
            PacketDistributor.sendToPlayer(p, new RacePayloads.State(stateId, remainingTicks, argb));
        } catch (RuntimeException e) {
            LOG.debug("[nusmp] elite races: could not send the state to {}", p.getGameProfile().getName());
        }
    }

    public static void forget(UUID id) { LAST.remove(id); }
}
