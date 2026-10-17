package com.newuniverse.nusmp.labyrinth.mega;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: one entry point for the whole Mega-Dungeon (1.54.0), the way {@link com.newuniverse.nusmp.labyrinth.LabyrinthSystem} is for the
 * Labyrinth: the mod class gains no line at all (this is called from the END of {@code LabyrinthSystem.init}, so it runs before
 * {@code NUItems.ITEMS.register}, which is where every block item of the Labyrinth packages must have been added). Each builder owns ONE
 * registry class with {@code init(IEventBus)} (DeferredRegisters, mod-bus listeners) and {@code register(IEventBus)} (game-bus handlers);
 * the order below is dependency order (the generator first because it registers the {@link Expander}, the themes before the systems that
 * read a theme, the guard last because its door kind talks to the generator and the crucible). A builder that has nothing to register
 * leaves its methods empty.
 */
public final class MegaSystem {
    private MegaSystem() {}

    public static void init(IEventBus modBus) {
        com.newuniverse.nusmp.labyrinth.mega.gen.MegaGenRegistry.init(modBus);       // generator: processors? no: pool element types, tick handlers, command
        com.newuniverse.nusmp.labyrinth.mega.theme.ThemeRegistry.init(modBus);       // themes + the Crucible: region_theme processor, theme data, catastrophes
        com.newuniverse.nusmp.labyrinth.mega.safe.SafeRegistry.init(modBus);         // safe zones: mod_compat processor, rest state
        com.newuniverse.nusmp.labyrinth.mega.enc.EncRegistry.init(modBus);           // the Dungeon Master: encounters, noise, wandering bosses
        com.newuniverse.nusmp.labyrinth.mega.eco.EcoRegistry.init(modBus);           // factions, turf wars, the stone-and-meat economy
        com.newuniverse.nusmp.labyrinth.mega.guard.GuardRegistry.init(modBus);       // the Labyrinth Guardian, the Mana Core, the sector Gatekeeper
        var bus = NeoForge.EVENT_BUS;
        com.newuniverse.nusmp.labyrinth.mega.gen.MegaGenRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.theme.ThemeRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.safe.SafeRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.enc.EncRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.eco.EcoRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.guard.GuardRegistry.register(bus);
    }
}
