package com.newuniverse.nusmp.franchise;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: one hook from the mod constructor for the franchise engravings: the server config {@code nusmp-franchise-server.toml}, the game-bus handlers of {@link FranchiseEvents}
 * and the CI self-test {@code item.franchise_engravings}. The engraving of the items themselves is a one-line call from {@code item.WeaponEngravings}' once-a-second pass, and the
 * spell hook is one line in {@code book.GrimoireBook}.
 */
public final class FranchiseBoot {
    private FranchiseBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, FranchiseConfig.SERVER, "nusmp-franchise-server.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> FranchiseConfig.apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> FranchiseConfig.apply());
        NeoForge.EVENT_BUS.addListener(FranchiseEvents::onIncoming);
        NeoForge.EVENT_BUS.addListener(FranchiseEvents::onPost);
        NeoForge.EVENT_BUS.addListener(FranchiseEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(FranchiseEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(FranchiseEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(FranchiseEvents::onServerStopping);
        FranchiseSelfTests.register();
    }
}
