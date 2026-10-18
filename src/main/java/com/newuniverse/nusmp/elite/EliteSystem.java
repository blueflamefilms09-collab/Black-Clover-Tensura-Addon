package com.newuniverse.nusmp.elite;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Why: one entry point for the Hub economy brief (docs/elite/plan.md): the Tensura-coin economy, Hub NPCs, quests, shop and Dungeon Map live in {@code elite/hub/}. The other areas that were once
 * built from the owner's "EliteTensuraAddon" inventory (races, records, nations, forge, skills, world, items) were removed at the owner's request in 1.88.0; their git history is the way back.
 */
public final class EliteSystem {
    private EliteSystem() {}

    public static void init(IEventBus modBus, ModContainer container) {
        // ---- hub: Tensura-coin economy, Hub NPCs, quests, Dungeon Map (elite/hub)
        com.newuniverse.nusmp.elite.hub.HubRegistry.init(modBus, container);
    }
}
