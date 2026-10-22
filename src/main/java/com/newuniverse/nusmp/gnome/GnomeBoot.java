package com.newuniverse.nusmp.gnome;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Gnome (the Earth Spirit, docs/magic/gnome.md) is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the sound events of its own sound
 * set, the server config {@code nusmp-gnome-server.toml} ({@code magic.gnome}), the game events and the CI self-test {@code magic.gnome}. Safe to call twice. The client half (the Gnome key)
 * is gone since 3.0.0 (no key: the grimoire's spirit pages and /multiverse spirit call the Gnome, its handle is registered here); the prop painter is {@code client.prop.GnomePropPainter}.
 */
public final class GnomeBoot {
    private GnomeBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        GnomeSounds.SOUNDS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, GnomeConfig.SPEC, "nusmp-gnome-server.toml");
        GnomeEvents.register(NeoForge.EVENT_BUS);
        GnomeSelfTests.register();
        com.newuniverse.nusmp.spiritcontrol.SpiritControl.register(new GnomeSpirit());                                  // 3.0.0: the spirit control plane (grimoire pages and /multiverse spirit) replaces the key
    }
}
