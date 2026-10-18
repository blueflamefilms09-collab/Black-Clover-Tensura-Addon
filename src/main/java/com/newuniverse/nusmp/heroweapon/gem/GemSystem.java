package com.newuniverse.nusmp.heroweapon.gem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.List;

/**
 * Why: the one entry point of the acquisition system (the Calling Gem, the ritual that binds a weapon, the recipes, the config
 * {@code nusmp-heroweapon-gem-server.toml}, the self-tests {@code hero.gem.*}), called once from {@code HeroWeaponSystem.init}. Owned by the gem builder, who
 * replaces this stub. {@link #creativeItems} lists the items of the system for the creative tab.
 */
public final class GemSystem {
    private GemSystem() {}

    public static void init(IEventBus modBus, ModContainer modContainer) {}

    /** The items this system adds to the creative tab (after the weapons). */
    public static List<Item> creativeItems() { return List.of(); }
}
