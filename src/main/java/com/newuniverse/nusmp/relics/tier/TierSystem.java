package com.newuniverse.nusmp.relics.tier;

import net.neoforged.bus.api.IEventBus;

/**
 * The entry points the mod constructor calls (the lead wires them):
 * <pre>
 *   TierSystem.registerContent(modEventBus);        // the nusmp:tier component and the two catalyst items
 *   TierSystem.registerGame(NeoForge.EVENT_BUS);    // the attribute event and the ritual
 *   // commands: TierCommands.addTo(root) next to RelicCommands.addTo(root)
 * </pre>
 * The aura scaling registers itself through {@code relics.scaling.ScalingSystem}.
 */
public final class TierSystem {
    private TierSystem() {}

    public static void registerContent(IEventBus modBus) {
        TierComponents.COMPONENTS.register(modBus);
        TierItems.ITEMS.register(modBus);
    }

    public static void registerGame(IEventBus gameBus) {
        TierEvents.register(gameBus);
        TierRitual.register(gameBus);
    }
}
