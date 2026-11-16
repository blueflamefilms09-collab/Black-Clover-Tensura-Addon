package com.newuniverse.nusmp.attune;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: meditation attunement (Beast Wills, Djinns, Devils and Spirits held together, clashing elements rejected) is one self-contained feature with one entry point, the single line the mod constructor
 * calls: the attachment, the server config {@code nusmp-attune-server.toml}, the two payloads, the events and the command, the CI self-tests ({@code attune.rules}, {@code attune.player}). The
 * client half (the meditation screen) is {@code client.attune.AttuneClient}, called from the client block of the constructor. Safe to call twice.
 */
public final class AttuneBoot {
    private AttuneBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        AttuneStore.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, AttuneConfig.SPEC, "nusmp-attune-server.toml");
        modBus.addListener(AttuneNet::registerPayloads);
        AttuneEvents.register(NeoForge.EVENT_BUS);
        AttuneSelfTests.register();
    }
}
