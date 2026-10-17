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
        // 1.55.0, the bidirectional layer (docs/labyrinth/mega_floors.md): floors and the lock first (others read floors), then the Hub, its generator, and the systems that live on the floors
        com.newuniverse.nusmp.labyrinth.mega.floors.FloorsRegistry.init(modBus);     // floor bands, the floor tracker, PlayerDungeonAlignment
        com.newuniverse.nusmp.labyrinth.mega.macro.MacroRegistry.init(modBus);       // the Sanctuary chunk generator (codec) before the Hub's dimension data names it
        com.newuniverse.nusmp.labyrinth.mega.hub.HubRegistry.init(modBus);           // Floor 0: rules, travel, plaza, gates
        com.newuniverse.nusmp.labyrinth.mega.system.SystemRegistry.init(modBus);     // the System overlay, floor quests, the Shadow Core
        com.newuniverse.nusmp.labyrinth.mega.flow.FlowRegistry.init(modBus);         // the Rukh flow, the decennial Metal Vessel
        com.newuniverse.nusmp.labyrinth.mega.gear.GearRegistry.init(modBus);         // standard Normal / Special / Rare gear drops
        var bus = NeoForge.EVENT_BUS;
        com.newuniverse.nusmp.labyrinth.mega.gen.MegaGenRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.theme.ThemeRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.safe.SafeRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.enc.EncRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.eco.EcoRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.guard.GuardRegistry.register(bus);
        bus.addListener(ExpansionTrigger::onServerTick);            // 1.55.0: starts a held-back expansion when a player reaches the Way Onward
        com.newuniverse.nusmp.labyrinth.mega.floors.FloorsRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.macro.MacroRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.hub.HubRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.system.SystemRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.flow.FlowRegistry.register(bus);
        com.newuniverse.nusmp.labyrinth.mega.gear.GearRegistry.register(bus);
    }
}
