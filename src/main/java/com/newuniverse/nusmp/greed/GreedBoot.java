package com.newuniverse.nusmp.greed;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Black Sword Greed (Berserk of Gluttony) is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the eight form items, the two data components, the
 * attachment of the devoured stats, the server config {@code nusmp-greed-server.toml}, the two payloads, the events and the CI self-tests ({@code greed.rules}, {@code greed.world}, {@code greed.forms}). The Ultimate skill is
 * registered with the others in {@code skill/NUSkills}. Safe to call twice. The client half is {@code client.greed.GreedClient}, called from the client block of the constructor; the weapon renderer lists
 * {@link GreedItems#renderables()}.
 */
public final class GreedBoot {
    private GreedBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        GreedItems.REGISTER.register(modBus);
        GreedComponents.COMPONENTS.register(modBus);
        GreedStore.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, GreedConfig.SPEC, "nusmp-greed-server.toml");
        modBus.addListener(GreedNet::registerPayloads);
        GreedEvents.register(NeoForge.EVENT_BUS);
        GreedSelfTests.register();
        GreedFx.registerSelfTests();                      // VFX wave 2: the anime effects (GreedLayer): greed.vfx
    }
}
