package com.newuniverse.nusmp.perf;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the one call that wires the performance safety nets into the mod (one line in {@code NUSMP}): the common config file nusmp-performance-common.toml, the copy of its values into
 * {@link PerfSettings} (when the file loads, when it is edited, and every five seconds as a safety net), the server probe events and the {@code /multiverse perf} command, and the CI self-test
 * {@code perf.core}. The client half (the client config file, the {@code /nusmp perf} command, the client probe events) is {@code client.perf.PerfClient}, called from the client block of
 * the constructor.
 */
public final class PerfBoot {
    private PerfBoot() {}

    private static final int REFRESH_EVERY = 100;

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, PerfConfig.COMMON, "nusmp-performance-common.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> PerfConfig.apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> PerfConfig.apply());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % REFRESH_EVERY == 0) PerfConfig.apply();     // a value edited with a config mod that fires no reload event still takes effect
        });
        NeoForge.EVENT_BUS.addListener(PerfCommands::register);
        PerfEvents.register(NeoForge.EVENT_BUS);
        PerfSelfTests.register();
    }
}
