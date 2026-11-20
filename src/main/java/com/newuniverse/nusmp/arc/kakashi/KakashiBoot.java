package com.newuniverse.nusmp.arc.kakashi;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Kakashi boss needs four small things at start-up (its server settings file, the cleanup events, the self-tests and the sound register of its recorded voice lines), and the shared
 * {@code ArcSystem.init} should gain exactly one line for all of them. Called once from {@code ArcSystem.init}, before the registry events.
 */
public final class KakashiBoot {
    private KakashiBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        modContainer.registerConfig(ModConfig.Type.SERVER, KakashiConfig.SPEC, "nusmp-kakashi-boss-server.toml");   // the Kakashi boss: its own server settings file
        KakashiEvents.register(NeoForge.EVENT_BUS);                                                                  // he forgets a player on logout, death, dimension change and server stop
        KakashiSelfTests.register();                                                                                 // keys kakashi.boss.*
        KakashiSounds.register(modBus);                                                                              // sound events of the recorded voice lines (none until recordings exist)
    }
}
