package com.newuniverse.nusmp.rebirth;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Rebirth rites: the one entry point (a single line of the mod constructor): the two sigil items, the server config {@code nusmp-rebirth-server.toml}, the clean-up of a player's confirm window on logout,
 * death and dimension change (and of the whole map when the server stops), and the CI self-test {@code rebirth.rules}. Safe to call twice.
 */
public final class RebirthBoot {
    private RebirthBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        RebirthItems.ITEMS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, RebirthConfig.SPEC, "nusmp-rebirth-server.toml");
        NeoForge.EVENT_BUS.addListener(RebirthBoot::onLogout);
        NeoForge.EVENT_BUS.addListener(RebirthBoot::onDeath);
        NeoForge.EVENT_BUS.addListener(RebirthBoot::onDimension);
        NeoForge.EVENT_BUS.addListener(RebirthBoot::onStopping);
        RebirthSelfTests.register();
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) RebirthState.forget(p.getUUID()); }

    static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) RebirthState.forget(p.getUUID()); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) RebirthState.forget(p.getUUID()); }

    static void onStopping(ServerStoppingEvent e) { RebirthState.clearAll(); }
}
