package com.newuniverse.nusmp.labyrinth.squad;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the registration entry point of the squads, the one class {@code MegaSystem} calls (an append-only pair of lines, like every other system of the Mega-Dungeon).
 * It registers no block, item or entity: a squad is made of the Dungeon Master's ordinary monsters, so {@code init} only adds the in-game self-test ({@code enc.squads}),
 * and {@code register} wires the game-bus handlers with {@code addListener} like the rest of the mod (no annotation scanning): the squad tick, the join event that attaches and
 * tags the members, the death event that counts them, and the server stop that clears the registry. The lead registers {@link SquadConfig#SPEC} as
 * {@code nusmp-labyrinth-squad-server.toml}.
 */
public final class SquadRegistry {
    private SquadRegistry() {}

    public static void init(IEventBus modBus) {
        SquadSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SquadManager::onServerTick);
        gameBus.addListener(SquadEvents::onJoin);
        gameBus.addListener(SquadEvents::onDeath);
        gameBus.addListener(SquadEvents::onServerStopped);
    }
}
