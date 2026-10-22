package com.newuniverse.nusmp.sylph;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Sylph (Yuno's wind Spirit Lord, docs/magic/sylph.md) is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers her sound
 * events, the server config {@code nusmp-sylph-server.toml} ({@code magic.sylph}), her handle on the spirit control plane, the game events and the CI self-test {@code magic.sylph}. Safe to call twice. She has no key since 3.0.0: the grimoire's spirit pages and
 * {@code /multiverse spirit} call her ({@code spiritcontrol}); her prop painter is registered by the prop painter registry.
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
        SylphEvents.register(NeoForge.EVENT_BUS);
        SylphSelfTests.register();
        com.newuniverse.nusmp.spiritcontrol.SpiritControl.register(new SylphSpirit());                                  // 3.0.0: the spirit control plane (grimoire pages and /multiverse spirit) replaces the key
    }
}
