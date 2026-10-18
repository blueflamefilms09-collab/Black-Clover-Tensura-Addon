package com.newuniverse.nusmp.heroweapon;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Why: the one entry point of the core system (the bond sync, the loan of the weapon into the hand, the input handler and the dispatcher, the restrictions, the
 * experience rule, the item behaviour, the config {@code nusmp-heroweapon-server.toml}, the self-tests {@code hero.catalog}, {@code hero.bond}, {@code hero.use},
 * {@code hero.rules}), called once from {@code HeroWeaponSystem.init}. Owned by the core builder, who replaces this stub.
 */
public final class HeroCore {
    private HeroCore() {}

    public static void init(IEventBus modBus, ModContainer modContainer) {}
}
