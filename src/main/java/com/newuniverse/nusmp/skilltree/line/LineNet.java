package com.newuniverse.nusmp.skilltree.line;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.rules.RateLimiter;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Why: the trust boundary of the line engine in one place. The three requests a client can make (use an art, switch a passive, ask to inspect a line) are registered here as server-bound payloads, and
 * the handler of each one starts from the same three rules: the sender must be a live server player, the request is counted against a per-second limit (counted in server ticks, like the purchase limit,
 * so a modified client cannot make the server do hundreds of casts or lookups a second), and then the real decision is made by the same code the rest of the engine uses ({@link LineCast},
 * {@code SkillTreeApi.setPassiveEnabled}, {@link LineSync}), never by the client: the packet carries ids and one flag and no number. The three messages the server sends (the cards, extra routes, the
 * state of an art) are registered client-bound; their handlers only store into common classes, or reach the client class by name inside a lambda, so a dedicated server never loads a client class. All are
 * optional payloads with protocol "1" like every other payload of the mod, so a client without them is not refused. Handlers run inside {@code enqueueWork} and never throw: NeoForge disconnects a player
 * whose handler throws, which is far too harsh for a refused cast.
 */
public final class LineNet {
    private LineNet() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String PROTOCOL = "1";
    /** Casts, switches and inspect requests a player may send per second. */
    public static final int CASTS_PER_SECOND = 6, REQUESTS_PER_SECOND = 10;
    private static final RateLimiter<UUID> CASTS = new RateLimiter<>(), REQUESTS = new RateLimiter<>();

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL).optional();
        r.playToServer(LinePayloads.CastArt.TYPE, LinePayloads.CastArt.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> handleCast(payload, ctx)));
        r.playToServer(LinePayloads.TogglePassive.TYPE, LinePayloads.TogglePassive.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> handleToggle(payload, ctx)));
        r.playToServer(LinePayloads.Inspect.TYPE, LinePayloads.Inspect.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> handleInspect(payload, ctx)));
        r.playToClient(LinePayloads.Cards.TYPE, LinePayloads.Cards.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> RouteRegistry.setClientCards(payload.cards())));
        r.playToClient(LinePayloads.More.TYPE, LinePayloads.More.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> RouteRegistry.addClient(payload.routes())));
        r.playToClient(LinePayloads.ArtState.TYPE, LinePayloads.ArtState.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.skilltree.line.LineClientHooks.onArtState(payload.key(), payload.cooldownTicks(), payload.stance())));
    }

    private static ServerPlayer sender(IPayloadContext ctx) { return ctx.player() instanceof ServerPlayer p && p.isAlive() ? p : null; }

    private static boolean allow(RateLimiter<UUID> limiter, ServerPlayer p, int perSecond) {
        return limiter.allow(p.getUUID(), p.getServer().getTickCount(), perSecond);
    }

    private static void handleCast(LinePayloads.CastArt payload, IPayloadContext ctx) {
        try {
            ServerPlayer p = sender(ctx);
            if (p == null) return;
            if (!allow(CASTS, p, CASTS_PER_SECOND)) {
                p.displayClientMessage(LineText.tr(LineText.CAST_TOO_FAST).withStyle(ChatFormatting.RED), true);
                return;
            }
            LineCast.cast(p, payload.route(), payload.node());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] lines: a cast request failed", e);
        }
    }

    private static void handleToggle(LinePayloads.TogglePassive payload, IPayloadContext ctx) {
        try {
            ServerPlayer p = sender(ctx);
            if (p == null || !allow(REQUESTS, p, REQUESTS_PER_SECOND)) return;
            if (!SkillTreeApi.setPassiveEnabled(p, payload.route(), payload.node(), payload.on())) {
                p.displayClientMessage(LineText.tr(LineText.TOGGLE_REFUSED).withStyle(ChatFormatting.RED), true);
                return;
            }
            LineAttrs.sync(p);                                    // do not wait for the next beat: the stat bonus follows the switch at once
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] lines: a toggle request failed", e);
        }
    }

    private static void handleInspect(LinePayloads.Inspect payload, IPayloadContext ctx) {
        try {
            ServerPlayer p = sender(ctx);
            if (p == null || !allow(REQUESTS, p, REQUESTS_PER_SECOND)) return;
            if (!LineSync.isAdmin(p)) {
                p.displayClientMessage(LineText.tr(LineText.INSPECT_REFUSED).withStyle(ChatFormatting.RED), true);
                return;
            }
            if (payload.route().isEmpty()) LineSync.sendCards(p);
            else LineSync.sendInspect(p, payload.route());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] lines: an inspect request failed", e);
        }
    }

    /** A player left: forget their rate-limit history and sync bookkeeping. */
    public static void forget(UUID id) {
        CASTS.forget(id);
        REQUESTS.forget(id);
        LineSync.forget(id);
    }

    /** The server stopped: nothing here may outlive it. */
    public static void clear() {
        CASTS.clear();
        REQUESTS.clear();
        LineSync.clear();
    }
}
