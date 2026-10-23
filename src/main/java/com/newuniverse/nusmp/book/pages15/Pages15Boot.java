package com.newuniverse.nusmp.book.pages15;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Why: the "15 spells for every grimoire" project has one entry point, called once from {@code charset.CharacterSetBoot.init} (the file that already owns the server config {@code magic.pagesFifteen}): it registers
 * the shared CI self-test {@code magic.pages15} and the logout cleanup of the kit's per-player zone slots. Safe to call twice. Server and client both run it (the self-test is only used by the dedicated
 * server smoke run; the listener only touches a map keyed by player UUID).
 */
public final class Pages15Boot {
    private Pages15Boot() {}

    private static boolean done;

    public static synchronized void init() {
        if (done) return;
        done = true;
        Pages15SelfTests.register();
        NeoForge.EVENT_BUS.addListener(Pages15Boot::onLogout);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Pages15Kit.forget(p.getUUID());
    }
}
