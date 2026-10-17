package com.newuniverse.nusmp.labyrinth.mega.eco;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the registration entry point of one builder of the Mega-Dungeon (factions and the economy). CONTRACT STUB (1.54.0): the builder who owns this
 * package REPLACES the empty bodies below with its DeferredRegisters and listeners; {@code MegaSystem} already calls both methods, so
 * nothing else changes when the bodies are filled. {@code init} runs before {@code NUItems.ITEMS.register}, so a block item may be put on
 * {@code NUItems.ITEMS} here; {@code register} receives the game bus.
 */
public final class EcoRegistry {
    private EcoRegistry() {}

    public static void init(IEventBus modBus) {}

    public static void register(IEventBus gameBus) {}
}
