package com.newuniverse.nusmp.heroweapon.forms;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Why: the one entry point of the forms system (the datapack loader, the absorption, the Weapon Book packets, the config {@code nusmp-heroweapon-forms-server.toml},
 * the self-tests {@code hero.forms.*}), called once from {@code HeroWeaponSystem.init} so the shared start-up never changes when this system grows. Owned by the
 * forms builder, who replaces this stub.
 */
public final class FormsSystem {
    private FormsSystem() {}

    public static void init(IEventBus modBus, ModContainer modContainer) {}
}
