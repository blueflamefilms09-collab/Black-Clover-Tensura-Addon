package com.newuniverse.nusmp.spiritspace;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Spirit of Space is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the server config
 * {@code nusmp-spiritspace-server.toml} ({@code magic.spiritSpace}), its own sound register, the key packet, the game events (and the {@code /spiritspace} commands) and the CI self-test
 * {@code magic.spirit_space}. Safe to call twice. The client half (the keys, the prop painters) is {@code client.spiritspace.SpiritSpaceClient}, called from the client block of the constructor.
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
        modBus.addListener(SpiritSpaceNet::registerPayloads);
        SpiritSpaceEvents.register(NeoForge.EVENT_BUS);
        SpiritSpaceSelfTests.register();
    }
}
