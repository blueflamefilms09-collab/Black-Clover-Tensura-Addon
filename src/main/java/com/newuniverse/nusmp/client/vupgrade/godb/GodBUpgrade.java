package com.newuniverse.nusmp.client.vupgrade.godb;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one registration call of the visual upgrade of the God melee weapons (release 1.102.0, slice god_melee_b), from the client section of the mod constructor:
 * {@code com.newuniverse.nusmp.client.vupgrade.godb.GodBUpgrade.init(modEventBus, modContainer);}. It registers the client file
 * {@code nusmp-visualupgrade-god_melee_b-client.toml} ({@link UpgradeConfig}), forgets the loaded tables when the resources are re-read, and starts the ambient particle tick
 * ({@link UpgradeFx}). The drawing itself is {@code client/GodMeleeBRender}, called from {@code WeaponRenderer}.
 */
public final class GodBUpgrade {
    private GodBUpgrade() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, UpgradeConfig.SPEC, "nusmp-visualupgrade-god_melee_b-client.toml");
        modBus.addListener((ModelEvent.BakingCompleted e) -> UpgradeSpecs.clear());
        NeoForge.EVENT_BUS.addListener(UpgradeFx::onClientTick);    }
}
