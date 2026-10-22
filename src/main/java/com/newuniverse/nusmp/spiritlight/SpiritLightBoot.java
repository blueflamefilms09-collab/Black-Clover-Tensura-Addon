package com.newuniverse.nusmp.spiritlight;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Spirit of Light is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the server config {@code nusmp-spiritlight-server.toml}
 * ({@code magic.spiritLight}), its own sound events, the key packet, the game events and the CI self-test {@code magic.spirit_light}. Safe to call twice. The client half (the two keys, the prop painters)
 * is {@code client.spiritlight.SpiritLightClient} and {@code client.prop.SpiritLightPropPainter}, called from the client block of the constructor and the prop painter registry.
 */
public final class SpiritLightBoot {
    private SpiritLightBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, SpiritLightConfig.SPEC, "nusmp-spiritlight-server.toml");
        SpiritLightSounds.SOUNDS.register(modBus);
        modBus.addListener(SpiritLightNet::registerPayloads);
        SpiritLightEvents.register(NeoForge.EVENT_BUS);
        SpiritLightSelfTests.register();
    }
}
