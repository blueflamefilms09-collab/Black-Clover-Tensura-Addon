package com.newuniverse.nusmp.ultimate;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: the one place that wires Ultimate Magic into the game (one call from the mod constructor): the SERVER config {@code nusmp-ultimate-server.toml}, the server events that run and cancel a cast, and the CI
 * self-tests {@code magic.ultimate.*}. There is no packet, no keybind and no client class: the wind-up circle, the pictures and the particles are the existing effect packets ({@code VfxSpawn}) and budgeted
 * server particles, and the page is cast through the ordinary grimoire flow. Every handler returns on its first line when nothing is being cast.
 */
public final class UltimateBoot {
    private UltimateBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, UltimateConfig.SPEC, "nusmp-ultimate-server.toml");
        NeoForge.EVENT_BUS.addListener(UltimateManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(UltimateManager::onDamaged);
        NeoForge.EVENT_BUS.addListener(UltimateManager::onDeath);
        NeoForge.EVENT_BUS.addListener(UltimateManager::onDimension);
        NeoForge.EVENT_BUS.addListener(UltimateManager::onLogout);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> UltimateManager.onServerStopping());
        UltimateSelfTests.register();
    }
}
