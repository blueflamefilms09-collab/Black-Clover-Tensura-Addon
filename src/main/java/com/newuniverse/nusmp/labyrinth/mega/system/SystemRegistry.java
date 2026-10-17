package com.newuniverse.nusmp.labyrinth.mega.system;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the registration entry point of one builder of the bidirectional Mega-Dungeon (1.55.0): the Solo Leveling "System": the holographic overlay, floor quests, stat rewards and the Shadow Core. CONTRACT STUB: the builder who owns this
 * package REPLACES the empty bodies below with its DeferredRegisters and listeners; {@code MegaSystem} already calls both methods, so
 * nothing else changes when the bodies are filled. {@code init} runs before {@code NUItems.ITEMS.register}, so a block item may be put on
 * {@code NUItems.ITEMS} here; {@code register} receives the game bus.
 */
public final class SystemRegistry {
    private SystemRegistry() {}

    public static void init(IEventBus modBus) {}

    public static void register(IEventBus gameBus) {}
}
