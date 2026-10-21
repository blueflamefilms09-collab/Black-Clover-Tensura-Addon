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
        GradeDamageSelfTests.register();                        // the self-test relic.grade_damage (every relic weapon's damage is inside its grade range)
        RelicWornSelfTests.register();                          // the self-test relic.worn_render (every worn relic has a worn model)
        com.newuniverse.nusmp.relics.ArmorSlotSelfTests.register();   // 1.110.0: the self-test visual.armor_slot_relics (every armour-slot relic and armour item has a way onto the body)
        com.newuniverse.nusmp.relics.ArmorVisibleSelfTests.register();   // 1.111.0: the self-test relic.armor_visible (the decision table: a worn armour relic is never invisible; painted layers non-empty)
        com.newuniverse.nusmp.relics.vu.DuelDiskSelfTests.register();    // 1.110.0: the self-test visual.duel_disk (the deck relics: disk models, textures, kept floating cards, override table, fallback)
        com.newuniverse.nusmp.relics.vu.DeckSummonSelfTests.register();  // 1.110.0: the self-test visual.deck_summon (the summon timeline, geometry, atlas)
        com.newuniverse.nusmp.relics.vu.VuSelfTests.register();       // the self-test visual.charms_genesis_worn (the upgraded charms, gauntlet, crowns and crest: models, textures, fx, act effects, fallback)
        WornSync.register(gameBus);                             // 1.99.0: the server tells every client what a wearer carries outside the vanilla equipment (Curios, a mob's pack)
        WornRender2SelfTests.register();                        // the self-test relic.worn_render2 (the plan, the sync codec, the Curios probe)
        RelicPowerSelfTests.register();                         // 1.104.0: the self-test relic.attune_lock (an unattuned relic deals a fifth of its damage)
        com.newuniverse.nusmp.relics.upgrade.UpgradeSelfTests.register();   // 1.102.0: the self-test visual.god_worn (the upgraded God relics: models, skins, effect tables, flag fallback)
    }
}
