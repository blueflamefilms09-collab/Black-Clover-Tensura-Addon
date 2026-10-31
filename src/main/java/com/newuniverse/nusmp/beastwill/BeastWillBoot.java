package com.newuniverse.nusmp.beastwill;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Beast Will system (docs/beastwill.md) is one self-contained feature, so it has one entry point: the single line the mod constructor calls (before {@code NUItems.ITEMS.register}). It registers the
 * save attachment, the core's data component and item, the server config {@code nusmp-beastwill-server.toml}, the three packets, the game events (which include the data reload listener and the
 * commands) and the CI self-tests {@code beastwill.*}. Safe to call twice. The client half (keys, HUD, absorption screen, aura painter) is {@code client.beastwill.BeastWillClient}, wired by the client
 * setup; this class names no client class.
 */
public final class BeastWillBoot {
    private BeastWillBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        BeastWillStore.ATTACHMENTS.register(modBus);
        BeastWillComponents.COMPONENTS.register(modBus);
        BeastWillItems.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, BeastWillConfig.SPEC, "nusmp-beastwill-server.toml");
        modBus.addListener(BeastWillNet::registerPayloads);
        BeastWillEvents.register(NeoForge.EVENT_BUS);
        BeastWillCommands.register(NeoForge.EVENT_BUS);
        BeastWillSelfTests.register();
    }
}
