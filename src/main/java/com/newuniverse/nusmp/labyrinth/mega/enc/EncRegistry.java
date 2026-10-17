package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the registration entry point of the Dungeon Master (encounters, noise and light, wandering bosses), the one class {@code MegaSystem}
 * calls. It registers no block, item or entity (everything the Director makes is an existing mob), so {@code init} only adds the in-game
 * self-tests, and {@code register} wires the game-bus handlers with {@code addListener} like the rest of the mod (no annotation scanning): the
 * two tick handlers (the encounter dice, the wandering bosses), the noise sources (vanilla game events and damage), the guards of the
 * wandering bosses (who they may target, no healing in combat, no Spire key), the housekeeping (join, logout, dimension change, server stop,
 * which clear every in-memory table so nothing outlives a server) and the reload listener that reads the encounter tables and the roster of
 * bosses from the data packs. The lead registers {@link EncConfig#SPEC} as {@code nusmp-labyrinth-mega-enc-server.toml} and attaches
 * {@link EncCommand#command()} under {@code /multiverse mega}.
 */
public final class EncRegistry {
    private EncRegistry() {}

    public static void init(IEventBus modBus) {
        EncSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EncounterDirector::onServerTick);
        gameBus.addListener(WandererDirector::onServerTick);
        gameBus.addListener(NoiseEvents::onGameEvent);
        gameBus.addListener(NoiseEvents::onDamage);
        gameBus.addListener(EncounterDirector::onJoin);
        gameBus.addListener(WandererDirector::onJoin);
        gameBus.addListener(WandererGuard::onChangeTarget);
        gameBus.addListener(WandererGuard::onHeal);
        gameBus.addListener(WandererGuard::onDrops);
        gameBus.addListener(EncounterDirector::onLogout);
        gameBus.addListener(EncounterDirector::onChangedDimension);
        gameBus.addListener(WandererDirector::onLogout);
        gameBus.addListener(WandererDirector::onChangedDimension);
        gameBus.addListener(EncounterDirector::onServerStopped);
        gameBus.addListener(WandererDirector::onServerStopped);
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new EncData.Reload()));
    }
}
