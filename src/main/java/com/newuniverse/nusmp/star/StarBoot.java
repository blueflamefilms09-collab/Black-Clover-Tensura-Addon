package com.newuniverse.nusmp.star;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Star Magic nodes: the one entry point (a single line of the mod constructor, both sides): the server config {@code nusmp-star-server.toml} ({@link StarConfig}), the cleanup of a player's stars and pane
 * on logout, death, respawn and dimension change, the clean-up of everything when the server stops, and the CI self-test {@code magic.star_nodes}. Safe to call twice. The client half (the look and its two
 * options) is {@code vfx.client.StarV2Config}, called from the client block of the constructor.
 */
public final class StarBoot {
    private StarBoot() {}

    private static boolean done;

    public static synchronized void init(ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, StarConfig.SPEC, "nusmp-star-server.toml");
        NeoForge.EVENT_BUS.addListener(StarBoot::onLogout);
        NeoForge.EVENT_BUS.addListener(StarBoot::onDeath);
        NeoForge.EVENT_BUS.addListener(StarBoot::onRespawn);
        NeoForge.EVENT_BUS.addListener(StarBoot::onDimension);
        NeoForge.EVENT_BUS.addListener(StarBoot::onStopping);
        StarSelfTests.register();
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) StarGame.forget(p); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) StarGame.forget(p); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) StarGame.forget(p); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) StarGame.forget(p); }

    static void onStopping(ServerStoppingEvent e) { StarGame.clearAll(e.getServer()); }
}
