package com.newuniverse.nusmp.heroweapon.gem;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Why: the acquisition system's part of {@code /multiverse hero ...} (give a Calling Gem, bind or release a weapon of a player, list who holds what), attached by
 * the core's {@code HeroCommand}. Owned by the gem builder, who replaces this stub; the stub is an empty literal.
 */
public final class GemCommands {
    private GemCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() { return Commands.literal("gem"); }
}
