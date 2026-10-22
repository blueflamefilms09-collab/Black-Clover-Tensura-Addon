package com.newuniverse.nusmp.undine;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Undine (Noelle's water Spirit Lord: the tall woman of water, her companion and full forms, her own sound set) is one self-contained feature, so it has one entry point: the single line the
 * mod constructor calls. It registers the sound events ({@link UndineSounds}), the server config {@code nusmp-undine-server.toml} ({@code magic.undine}), the key packet, the game events and the CI
 * self-test {@code magic.undine}. Safe to call twice. The client half (the Undine key, the prop painter) is {@code client.undine.UndineClient}, called from the client block of the constructor.
 */
public final class UndineBoot {
    private UndineBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        UndineSounds.SOUNDS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, UndineConfig.SPEC, "nusmp-undine-server.toml");
        modBus.addListener(UndineNet::registerPayloads);
        UndineEvents.register(NeoForge.EVENT_BUS);
        UndineSelfTests.register();
    }
}
