package com.newuniverse.nusmp.elite;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Why: one entry point for everything built from the owner's "EliteTensuraAddon" feature inventory and the Hub economy brief (1.83.0 and later, docs/elite/plan.md). The mod class gains one line;
 * each subsystem owns ONE registry class in its own package under {@code elite/<area>/} with {@code init(IEventBus, ModContainer)} (DeferredRegisters, mod-bus listeners, config registration)
 * and registers its own game-bus handlers from there. The sections below are separated on purpose so that parallel builders each add their one line without touching each other's lines.
 * A subsystem that is not built yet has no line.
 */
public final class EliteSystem {
    private EliteSystem() {}

    public static void init(IEventBus modBus, ModContainer container) {
        // ---- hub: Tensura-coin economy, Hub NPCs, quests, Dungeon Map (elite/hub)




        // ---- skills: aspectual magic, extra/intrinsic/unique/ultimate skills, evolution triggers (elite/skills)




        // ---- races: Saiyan tiers, Primordial race (elite/races)




        // ---- forge: stations, minigames, quality, engraving (elite/forge)




        // ---- records: records, achievements, titles, synergies (elite/records)




        // ---- world: calamities, bosses, the magicule world engine (elite/world)




        // ---- items: weapons, armor, consumables, crates, plushies (elite/items)




        // ---- nations: nations, economy, contracts, reputation, wars, alliances (elite/nations)
        com.newuniverse.nusmp.elite.nations.NationsRegistry.init(modBus, container);



        // ---- council: Walpurgis, governance, duels, dimension (elite/council)




        // ---- seasons: chronicles, season finale, legacy store (elite/seasons)




        // ---- voice: TTS providers and announcers, third-party integrations (elite/voice)




    }
}
