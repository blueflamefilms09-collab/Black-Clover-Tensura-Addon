package com.newuniverse.nusmp.heroweapon.tracker;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one entry point of the Weapon Tracker (the two payloads, the logout and server-stop cleanup, the self-tests {@code weapons.tracker.*}), called once from {@code HeroCore.init} on both sides. Its setting
 * {@code trackerShowNames} lives in the core's own file {@code nusmp-heroweapon-server.toml}, so there is no new config file. The client half is {@code client.heroweapon.WeaponTrackerClient}, started by
 * {@code HeroClient.init}. Safe to call twice.
 */
public final class WeaponTrackerBoot {
    private WeaponTrackerBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        modBus.addListener(WeaponTrackerNet::register);
        NeoForge.EVENT_BUS.addListener(WeaponTrackerService::onLogout);
        NeoForge.EVENT_BUS.addListener(WeaponTrackerService::onStopping);
        WeaponTrackerSelfTests.register();
    }
}
