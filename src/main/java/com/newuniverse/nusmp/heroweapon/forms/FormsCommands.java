package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Why: the forms system's part of {@code /multiverse hero ...} (list the forms of a weapon, unlock or set a form of a player, absorb, reset mastery), attached by
 * the core's {@code HeroCommand} so that no two builders edit one command class. Owned by the forms builder, who replaces this stub; the stub is an empty literal.
 */
public final class FormsCommands {
    private FormsCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() { return Commands.literal("form"); }
}
