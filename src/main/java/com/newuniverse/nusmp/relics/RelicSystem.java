package com.newuniverse.nusmp.relics;

import net.neoforged.bus.api.IEventBus;

/**
 * The one place the mod constructor calls (the lead wires it from NUSMP):
 * <pre>
 *   RelicSystem.registerContent(modEventBus);            // BEFORE NUItems.ITEMS.register(modEventBus)
 *   RelicSystem.registerGame(NeoForge.EVENT_BUS);        // the engine, the loot and Mimic hooks, and every wave's hooks()
 *   // client section: com.newuniverse.nusmp.client.relics.RelicClient.init(modEventBus);
 *   // commands: RelicCommands.addTo(root) under /multiverse
 * </pre>
 */
public final class RelicSystem {
    private RelicSystem() {}

    /** Mod bus: collects the waves' defs and registers the relic items, components, armour materials, loot modifier type and the two packets. */
    public static void registerContent(IEventBus modBus) {
        RelicCatalog.init();
        RelicComponents2.COMPONENTS.register(modBus);
        RelicArmorMaterials.MATERIALS.register(modBus);
        RelicLootModifier.register(modBus);
        modBus.addListener(RelicPayloads::register);
    }

    /** Game bus: the engine (passives, resistances, abilities, summons), the Mimic drops, and each wave's own listeners. */
    public static void registerGame(IEventBus gameBus) {
        RelicEngine.register(gameBus);
        RelicLoot.register(gameBus);
        RelicCatalog.hooks();
    }
}
