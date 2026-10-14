package com.newuniverse.nusmp.relics.scaling;

import net.neoforged.bus.api.IEventBus;

/** The entry point the mod constructor calls on the game bus: {@code ScalingSystem.registerGame(NeoForge.EVENT_BUS)}. */
public final class ScalingSystem {
    private ScalingSystem() {}

    public static void registerGame(IEventBus gameBus) { AuraEvents.register(gameBus); }
}
