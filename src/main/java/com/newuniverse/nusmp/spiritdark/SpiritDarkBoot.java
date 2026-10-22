package com.newuniverse.nusmp.spiritdark;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Spirit of Darkness is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the server config
 * {@code nusmp-spiritdark-server.toml} ({@code magic.spiritDark}), the sound events (the synthesised cues and the spoken lines that exist), the game events and the CI self-test
 * {@code magic.spirit_dark}. Safe to call twice. No keys since 3.0.0: the grimoire's spirit pages and /multiverse spirit call it (its handle is registered here). The prop painter is registered by the prop painter registry.
 */
public final class SpiritDarkBoot {
    private SpiritDarkBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        SpiritDarkSounds.SOUNDS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SpiritDarkConfig.SPEC, "nusmp-spiritdark-server.toml");
        SpiritDarkEvents.register(NeoForge.EVENT_BUS);
        SpiritDarkSelfTests.register();
        com.newuniverse.nusmp.spiritcontrol.SpiritControl.register(new SpiritDarkControl());                                  // 3.0.0: the spirit control plane (grimoire pages and /multiverse spirit) replaces the key
    }
}
