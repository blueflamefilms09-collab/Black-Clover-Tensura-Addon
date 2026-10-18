package com.newuniverse.nusmp.heroweapon.gem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Why: the one entry point of the acquisition system, called once from {@code HeroWeaponSystem.init} so the shared start-up never changes: how a player comes to
 * hold a hero weapon without an operator's command. The owner did not say how, so this is a default in the spirit of the series (a hero is summoned and the
 * weapon CHOOSES them): a held ritual with a Calling Gem. The Holy Calling Gem makes one of the four Legendary Weapons answer, a Vassal Seal makes the named
 * Seven Star or Vassal Weapon answer, and both are crafted from vanilla materials (nineteen recipes in the data pack, ingredients in {@link GemRecipes}).
 * Registered here: the config {@code nusmp-heroweapon-gem-server.toml} ({@link GemConfig}), the nineteen items ({@link GemItems}), the event listeners
 * ({@link GemEvents}) and the self-tests {@code hero.gem} and {@code hero.gem.ritual} ({@link GemSelfTests}). The commands are {@link GemCommands}, attached by the
 * core's {@code HeroCommand}. Safe to call twice.
 */
public final class GemSystem {
    private GemSystem() {}

    private static boolean started;

    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (started) return;
        started = true;
        modContainer.registerConfig(ModConfig.Type.SERVER, GemConfig.SPEC, "nusmp-heroweapon-gem-server.toml");
        GemItems.REGISTER.register(modBus);
        GemEvents.register(NeoForge.EVENT_BUS);
        GemSelfTests.register();
    }

    /** The items this system adds to the creative tab (after the weapons): the Holy Calling Gem, the Gem Fragment and the seventeen Vassal Seals. */
    public static List<Item> creativeItems() { return GemItems.all(); }
}
