package com.newuniverse.nusmp.skilltree.shop;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the Black Market is several small parts (the price and stock data and their reload listener, the config, the five packets, the one-action-per-tick gate, the self-tests)
 * that must be started together, once, from the single line that {@code SkillBookSystem} owns, and each of them has a lifecycle that is easy to get wrong: the data must be
 * re-read on every {@code /reload} (so it is a reload listener, not a read at startup), the snapshots and the gate must be emptied when the server stops (so a second world in the
 * same game session starts clean), and a player's gate entry must go when they log out. This class is exactly that wiring and nothing else: it registers the config file
 * {@code nusmp-skilltree-shop-server.toml} itself (so there is no separate {@code registerConfig} line anywhere), the payloads on the mod bus (the NeoForge 21.1 way; there is no
 * {@code SimpleChannel}), the listeners on the game bus, and the self-tests {@code skillbook.shop.*}. Safe to call twice.
 */
public final class ShopSystem {
    private ShopSystem() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        modContainer.registerConfig(ModConfig.Type.SERVER, ShopConfig.SPEC, "nusmp-skilltree-shop-server.toml");
        modBus.addListener(ShopNet::register);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new MarketData()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            MarketData.clear();
            ShopGate.clear();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ShopGate.forget(e.getEntity().getUUID()));
        ShopSelfTests.register();
    }
}
