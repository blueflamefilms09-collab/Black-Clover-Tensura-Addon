package com.newuniverse.nusmp.mana.method;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * The one place that wires the Mana Method into the game (one call from the mod constructor): the server config, the one packet, the tick and clean-up handlers, the
 * self-tests, and on a client the drawing. Every handler returns at once when there is nothing to do.
 */
public final class ManaMethodBoot {
    private ManaMethodBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ManaMethodConfig.SPEC, "nusmp-mana-method-server.toml");
        modBus.addListener(RuneNet::registerPayloads);
        NeoForge.EVENT_BUS.addListener(RuneManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(RuneManager::onDeath);
        NeoForge.EVENT_BUS.addListener(RuneManager::onDimension);
        NeoForge.EVENT_BUS.addListener(RuneManager::onLogout);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> RuneManager.onServerStopping(e.getServer()));
        // 3.5.0: the Air Step ledger and the Dash cooldowns (each handler returns at once when it has nothing)
        NeoForge.EVENT_BUS.addListener(BodyRunes::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(AirStep::onServerTick);
        NeoForge.EVENT_BUS.addListener(AirStep::onDeath);
        NeoForge.EVENT_BUS.addListener(AirStep::onDimension);
        NeoForge.EVENT_BUS.addListener(AirStep::onLogout);
        NeoForge.EVENT_BUS.addListener(Dash::onLogout);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> { AirStep.onServerStopping(e.getServer()); Dash.clearAll(); });
        // 3.8.0: the TIMED rune state (each handler returns at once when no one is ON)
        NeoForge.EVENT_BUS.addListener(RuneState::onServerTick);
        NeoForge.EVENT_BUS.addListener(RuneState::onDeath);
        NeoForge.EVENT_BUS.addListener(RuneState::onDimension);
        NeoForge.EVENT_BUS.addListener(RuneState::onLogout);
        NeoForge.EVENT_BUS.addListener(RuneState::onLogin);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> RuneState.onServerStopping(e.getServer()));
        ManaMethodSelfTests.register();
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.manamethod.RuneClient.init();
    }
}
