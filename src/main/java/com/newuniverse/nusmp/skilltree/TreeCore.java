package com.newuniverse.nusmp.skilltree;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Why: the one entry point of the core system: the datapack reload listener for {@code data/nusmp/skill_trees/*.json}, the route registry and its sync to clients, the payloads (the full tree to the client,
 * the routes to the client, the node purchase and the class choice to the server), the login / respawn / dimension events, the quest hooks, the config {@code nusmp-skilltree-server.toml} and the
 * self-tests {@code tree.*}; called once from {@code SkillTreeSystem.init}. Owned by the core builder, who replaces this stub.
 */
public final class TreeCore {
    private TreeCore() {}

    public static void init(IEventBus modBus, ModContainer modContainer) {}
}
