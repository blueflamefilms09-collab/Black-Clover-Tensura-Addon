package com.newuniverse.nusmp.sylph;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Sylph (Yuno's wind Spirit Lord, docs/magic/sylph.md) is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers her sound
 * events, the server config {@code nusmp-sylph-server.toml} ({@code magic.sylph}), the key packet, the game events and the CI self-test {@code magic.sylph}. Safe to call twice. The client half
 * (the Sylph key) is {@code client.sylph.SylphClient}, called from the client block of the constructor; her prop painter is registered by the prop painter registry.
 */
public final class SylphBoot {
    private SylphBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        SylphSounds.SOUNDS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SylphConfig.SPEC, "nusmp-sylph-server.toml");
        modBus.addListener(SylphNet::registerPayloads);
        SylphEvents.register(NeoForge.EVENT_BUS);
        SylphSelfTests.register();
    }
}
