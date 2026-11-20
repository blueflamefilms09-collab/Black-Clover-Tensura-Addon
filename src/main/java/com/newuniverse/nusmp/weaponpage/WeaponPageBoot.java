package com.newuniverse.nusmp.weaponpage;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the Weapon page of the Codex (docs/ui_weapon_page.md) is one small feature with one entry point, the single line the mod constructor calls: it registers the two packets, drops a player's page bookkeeping
 * at logout and everything at server stop, and registers the CI self-tests {@code weaponpage.rules} and {@code weaponpage.world}. It registers no config (the one setting, {@code weaponpage.show}, is a client
 * setting added to {@code nusmp-multiverse-client.toml}) and no tick handler. Safe to call twice. The client half is {@code client.weaponpage.WeaponPageClient}.
 */
public final class WeaponPageBoot {
    private WeaponPageBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        modBus.addListener(WeaponPageNet::registerPayloads);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> WeaponPageService.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> WeaponPageService.clearAll());
        WeaponPageSelfTests.register();
    }
}
