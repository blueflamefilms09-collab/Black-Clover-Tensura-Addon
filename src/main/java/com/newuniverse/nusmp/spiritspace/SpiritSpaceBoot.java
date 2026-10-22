package com.newuniverse.nusmp.spiritspace;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Spirit of Space is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the server config
 * {@code nusmp-spiritspace-server.toml} ({@code magic.spiritSpace}), its own sound register, the game events (and the {@code /spiritspace} commands) and the CI self-test
 * {@code magic.spirit_space}. Safe to call twice. No keys since 3.0.0: the grimoire's spirit pages and /multiverse spirit call it (its handle is registered here). The prop painters are registered by the prop painter registry.
 */
public final class SpiritSpaceBoot {
    private SpiritSpaceBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        SpiritSpaceSounds.SOUNDS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SpiritSpaceConfig.SPEC, "nusmp-spiritspace-server.toml");
        SpiritSpaceEvents.register(NeoForge.EVENT_BUS);
        SpiritSpaceSelfTests.register();
        com.newuniverse.nusmp.spiritcontrol.SpiritControl.register(new SpiritSpaceControl());                                  // 3.0.0: the spirit control plane (grimoire pages and /multiverse spirit) replaces the key
    }
}
