package com.newuniverse.nusmp.compat.maps;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the one entry point of the map-mod compatibility (docs/compat/maps.md), so the mod class gains a single line. It registers the server config
 * ({@code nusmp-compat-maps-server.toml}), the FTB Chunks start-up check and the operators' login warning, the clearing of the rate limiter when a server stops, and the
 * self-test {@code compat.maps}. The client config is registered by the client part of the mod class. Soft by design: nothing here references a map mod's classes.
 */
public final class MapsCompat {
    private MapsCompat() {}

    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, MapsConfig.SPEC, "nusmp-compat-maps-server.toml");
        NeoForge.EVENT_BUS.addListener(FtbChunksGuard::onStarted);
        NeoForge.EVENT_BUS.addListener(FtbChunksGuard::onLogin);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> MapShare.resetLimiter());
        MapsSelfTests.register();
    }
}
