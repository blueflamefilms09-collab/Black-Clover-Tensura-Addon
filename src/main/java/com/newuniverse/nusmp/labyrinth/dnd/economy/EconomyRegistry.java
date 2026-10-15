package com.newuniverse.nusmp.labyrinth.dnd.economy;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: one entry point for the economy and hunger handlers, the same shape as the other Labyrinth packages, so the lead adds a single line.
 * It registers game-bus handlers with {@code addListener} (no {@code @SubscribeEvent} scanning, like the rest of the mod); it registers no
 * blocks or items of its own (the stones are Tensura's, the meat is {@code DungeonFood}'s), so unlike the other registries it has no
 * ordering constraint against {@code NUItems.ITEMS.register}.
 */
public final class EconomyRegistry {
    private EconomyRegistry() {}

    /** Pass {@code NeoForge.EVENT_BUS}. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener(EconomyDrops::onDrops);
        gameBus.addListener(EconomyDrops::onExperience);
        gameBus.addListener(DungeonHunger::onPlayerTick);
        gameBus.addListener(DungeonHunger::onLogout);
    }
}
