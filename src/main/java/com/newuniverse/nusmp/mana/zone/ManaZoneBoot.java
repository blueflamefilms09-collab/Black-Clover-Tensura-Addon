package com.newuniverse.nusmp.mana.zone;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * The one place that wires the Mana Zone and the Grand Magic Zones into the game (one call from the mod constructor): the server config, the three packets,
 * the data reload listeners (data/nusmp/mana_zone.json and data/&lt;namespace&gt;/grand_magic_zones/*.json), the server and player events, the self-tests, and on a
 * client the renderer. Each event handler returns at once when there is nothing to do (no open zone, no loaded region), so an idle server pays almost nothing.
 */
public final class ManaZoneBoot {
    private ManaZoneBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ManaZoneConfig.SPEC, "nusmp-mana-zone-server.toml");
        modBus.addListener(ManaZoneNet::registerPayloads);
        modBus.addListener(ZoneCastNet::registerPayloads);                                // the upgrade's one client-to-server packet (the aim)
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new ManaZoneData.Loader());
            e.addListener(new GrandZones.Loader());
        });
        NeoForge.EVENT_BUS.addListener(ManaZoneManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(ManaZoneManager::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ManaZoneManager::onDeath);
        NeoForge.EVENT_BUS.addListener(ManaZoneManager::onDimension);
        NeoForge.EVENT_BUS.addListener(ManaZoneManager::onLogout);
        NeoForge.EVENT_BUS.addListener(ManaZoneManager::onLogin);
        NeoForge.EVENT_BUS.addListener(ManaZoneManager::onRespawn);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ManaZoneManager.onServerStopping());
        NeoForge.EVENT_BUS.addListener(ZoneEvasion::onImpact);                            // the upgrade's evasion backstop (returns at once while no zone is open)
        NeoForge.EVENT_BUS.addListener(GrandZones::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GrandZones::onLogout);
        ManaZoneSelfTests.register();
        ZoneUpSelfTests.register();
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.manazone.ManaZoneClient.init(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.manazone.ZoneCastClient.init();
    }
}
