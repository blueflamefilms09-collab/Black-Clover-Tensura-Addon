package com.newuniverse.nusmp.particle.fx;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one call that wires the custom particle library into the mod (one line in {@code NUSMP}): the particle types on the registry, the server config
 * {@code nusmp-particles-server.toml}, the copy of both config files into {@link FxConfig}'s fast fields (when a file loads or changes), the {@code /multiverse particles} command and the CI
 * self-test {@code particles.custom}. The client half (the client config file and the particle providers) is {@code client.particle.fx.FxClient}, called from the client block of the
 * constructor.
 */
public final class FxBoot {
    private FxBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        FxTypes.init(modBus);
        container.registerConfig(ModConfig.Type.SERVER, FxConfig.SERVER, "nusmp-particles-server.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> FxConfig.apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> FxConfig.apply());
        NeoForge.EVENT_BUS.addListener(FxCommands::register);
        FxSelfTests.register();
    }
}
