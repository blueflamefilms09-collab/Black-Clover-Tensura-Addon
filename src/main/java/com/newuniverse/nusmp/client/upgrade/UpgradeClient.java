package com.newuniverse.nusmp.client.upgrade;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one registration call of the visual upgrade, from the client section of the mod constructor:
 * {@code com.newuniverse.nusmp.client.upgrade.UpgradeClient.init(modEventBus, modContainer);}. It registers config/nusmp-visual-upgrade-client.toml
 * ({@link UpgradeConfig}) and the client tick of the ambient layers and swing trails ({@link UpgradeFx}). The weapon renderer, the effect layer of the act signatures and the server hook
 * call into this package themselves.
 */
public final class UpgradeClient {
    private UpgradeClient() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, UpgradeConfig.SPEC, "nusmp-visual-upgrade-client.toml");
        NeoForge.EVENT_BUS.addListener(UpgradeFx::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> UpgradeFx.clear());
    }
}
