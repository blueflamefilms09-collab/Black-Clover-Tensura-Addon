package com.newuniverse.nusmp.skilltree;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Why: {@code /multiverse tree ...}: the administration of the skill trees (show a player's tree, give SP, unlock or lock a route, set a node, set the class, reset, reload the routes, list the routes and
 * their checks), attached to the root by {@code MultiverseCommands} with one line. Owned by the core builder, who replaces this stub (an empty literal).
 */
public final class SkillTreeCommand {
    private SkillTreeCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() { return Commands.literal("tree"); }
}
