package com.newuniverse.nusmp.heroweapon.curse;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Why: the one entry point of the Curse Series system (despair triggers, the Embrace, corruption, the Overrun, purification, the config
 * {@code nusmp-heroweapon-curse-server.toml}, the self-tests {@code hero.curse.*}), called once from {@code HeroWeaponSystem.init}. Owned by the curse builder,
 * who replaces this stub.
 */
public final class CurseSystem {
    private CurseSystem() {}

    public static void init(IEventBus modBus, ModContainer modContainer) {}
}
