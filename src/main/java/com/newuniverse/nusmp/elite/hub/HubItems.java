package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Hub adds three items: the Dungeon Map ({@code nusmp:dungeon_map}, the brief's item) and a spawn egg for each NPC (so an admin can place a second Merchant or put one back with a
 * click; the automatic placement does the rest). They live on the Hub's own item register, registered on the mod bus before the registry freezes, so no existing item list is touched; the
 * creative tab entries are added with the tab-contents event (the Dungeon Map under Tools and Utilities, the eggs under Spawn Eggs).
 */
public final class HubItems {
    private HubItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<DungeonMapItem> DUNGEON_MAP = ITEMS.register("dungeon_map", () -> new DungeonMapItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<DeferredSpawnEggItem> QUEST_GIVER_EGG = ITEMS.register("quest_giver_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(HubNpcs.QUEST_GIVER, 0x2F4B8C, 0xE0B040, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MERCHANT_EGG = ITEMS.register("merchant_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(HubNpcs.MERCHANT, 0x4F7A3A, 0xC08A4A, new Item.Properties()));

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(HubItems::tabs);
    }

    private static void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(DUNGEON_MAP.get());
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(QUEST_GIVER_EGG.get());
            e.accept(MERCHANT_EGG.get());
        }
    }
}
