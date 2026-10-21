package com.newuniverse.nusmp.labyrinth.loot;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Why: the registration entry point of the functional anime loot, the one class {@code MegaSystem} calls (an append-only pair of lines, like every other system of the
 * Mega-Dungeon). {@code init} (mod bus, before the registries freeze) registers what must exist in a registry: the eighty items ({@link LootItems}), the data component
 * {@code nusmp:gear_roll}, the {@code mana_surge} effect, the two armour materials, the codec of the global loot modifier {@code nusmp:anime_loot}, the creative tab, and the in-game
 * self-test {@code loot.functional}. {@code register} (game bus, {@code addListener} with no annotation scanning, like the rest of the mod) adds the handlers: the anvil
 * uses and the venom strikes, the critical-chance lines, the trinket bridge, and the sale to the Hub Merchant (before the Merchant's own click, at high priority).
 * The lead registers {@link LootConfig#SPEC} as {@code nusmp-labyrinth-loot-server.toml}.
 */
public final class LootRegistry {
    private LootRegistry() {}

    public static void init(IEventBus modBus) {
        String problem = LootRoster.get().problem();
        if (!problem.isEmpty()) LogUtils.getLogger().warn("[nusmp] {}", problem);
        LootComponents.COMPONENTS.register(modBus);
        LootEffects.EFFECTS.register(modBus);
        LootArmorMaterials.MATERIALS.register(modBus);
        LootItems.REGISTER.register(modBus);
        AnimeLootModifier.register(modBus);
        LuckLootModifier.register(modBus);                                        // 1.107.0: the Luck-scaled counts, armour rolls and backpack drops (docs/labyrinth/luck_loot_backpack.md)
        com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackRegistry.init(modBus);   // 1.107.0: the placeable, upgradable Backpack (block, block entity, menu, upgrades, recipe)
        LuckLootSelfTests.register();
        LootCreative.TABS.register(modBus);
        LootSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AnvilUpdateEvent e) -> AnvilUses.onAnvil(e));
        gameBus.addListener(AnvilUses::onHit);
        gameBus.addListener(CritHandler::onCrit);
        gameBus.addListener(TrinketBridge::onTick);
        gameBus.addListener(MaterialSell.PRIORITY, MaterialSell::onInteract);
    }
}
