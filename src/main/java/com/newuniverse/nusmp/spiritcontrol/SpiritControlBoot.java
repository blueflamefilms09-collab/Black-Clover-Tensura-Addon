package com.newuniverse.nusmp.spiritcontrol;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the spirit control plane (docs/magic/spirit_control.md) is one self-contained feature, so it has one entry point: the single line the mod constructor calls (after the spirit packages' own Boots, each of
 * which registers its {@link SpiritHandle}). It registers the server config {@code nusmp-spirit-control-server.toml} ({@code magic.spiritControl}), the Spirit Lord's handle, the game events and the CI
 * self-tests {@code magic.spirit_control} and {@code magic.spirit_combat}. It has no packet and no key: the grimoire pages travel as ordinary page casts and the command is a vanilla command. Safe to call twice.
 */
public final class SpiritControlBoot {
    private SpiritControlBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, SpiritControlConfig.SPEC, "nusmp-spirit-control-server.toml");
        SpiritControl.register(new LordSpirit());
        SpiritControlEvents.register(NeoForge.EVENT_BUS);
        SpiritControlSelfTests.register();
    }
}
