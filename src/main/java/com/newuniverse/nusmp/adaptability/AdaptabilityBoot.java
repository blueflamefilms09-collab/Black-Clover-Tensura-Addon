package com.newuniverse.nusmp.adaptability;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: Adaptability (3.70.0, docs/adaptability.md) is one self-contained feature, so it has one entry point: the line the mod constructor calls. It registers the server config
 * {@code nusmp-adaptability-server.toml}, the damage and learning hooks and the CI self-test {@code adaptability.rules}. The skill itself is registered in NUSkills. Safe to call twice; names no client class.
 */
public final class AdaptabilityBoot {
    private AdaptabilityBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, AdaptabilityConfig.SPEC, "nusmp-adaptability-server.toml");
        AdaptabilityEvents.register(NeoForge.EVENT_BUS);
        AdaptabilitySelfTests.register();
    }
}
