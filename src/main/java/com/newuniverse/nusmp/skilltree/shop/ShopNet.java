package com.newuniverse.nusmp.skilltree.shop;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.skilltree.ShopClient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

/**
 * Why: this registers the shop's five packets in one place, on the mod bus, which is how NeoForge 21.1 wants payloads declared ({@code RegisterPayloadHandlersEvent}; there is no
 * {@code SimpleChannel} any more), in the same way as the core's {@code TreeNet}: protocol version "1" and {@code optional()} like every other payload of this mod, handlers that hop to
 * the main thread with {@code enqueueWork} and never throw (NeoForge disconnects a player whose handler throws, which is far too harsh for a refused purchase or a stale screen). The two
 * server-to-client packets only hand data to the client class {@link ShopClient}, inside a lambda, so the client class is never loaded on a dedicated server (the lambda body is not
 * resolved until it runs, and it only runs on a client); the three client-to-server packets do exactly three things in order: make sure the sender is a real, living player, let at most
 * one shop action per tick through ({@link ShopGate}), and call the matching {@link ShopService} method, which re-validates everything. Whatever the verdict, the answer is a fresh whole
 * picture ({@link ShopService#reply}), so the screen can never keep showing something the server has just refused.
 */
public final class ShopNet {
    private ShopNet() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The payload protocol version (the same as the core's). */
    public static final String PROTOCOL = "1";

    /** Mod-bus listener. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL).optional();
        r.playToClient(OpenShopPayload.TYPE, OpenShopPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            try {
                ShopClient.open(payload.snapshot());
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] black market: the shop screen could not open", e);
            }
        }));
        r.playToClient(ShopSyncPayload.TYPE, ShopSyncPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            try {
                ShopClient.sync(payload);
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] black market: the shop screen could not take the new picture", e);
            }
        }));
        r.playToServer(BuyPayload.TYPE, BuyPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> onBuy(ctx.player(), payload)));
        r.playToServer(SellPayload.TYPE, SellPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> onSell(ctx.player(), payload)));
        r.playToServer(SellAllPayload.TYPE, SellAllPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> onSellAll(ctx.player(), payload)));
    }

    private static void onBuy(Player player, BuyPayload p) {
        try {
            if (!(player instanceof ServerPlayer sp) || !sp.isAlive() || !ShopGate.allow(sp)) return;
            ShopService.reply(sp, ShopService.buy(sp, p.offer(), p.price()));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: a purchase request failed", e);
        }
    }

    private static void onSell(Player player, SellPayload p) {
        try {
            if (!(player instanceof ServerPlayer sp) || !sp.isAlive() || !ShopGate.allow(sp)) return;
            ShopService.reply(sp, ShopService.sell(sp, p.slot(), p.item(), p.count()));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: a sale request failed", e);
        }
    }

    private static void onSellAll(Player player, SellAllPayload p) {
        try {
            if (!(player instanceof ServerPlayer sp) || !sp.isAlive() || !ShopGate.allow(sp)) return;
            ShopService.reply(sp, ShopService.sellAll(sp, p.category()));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: a bulk sale request failed", e);
        }
    }
}
