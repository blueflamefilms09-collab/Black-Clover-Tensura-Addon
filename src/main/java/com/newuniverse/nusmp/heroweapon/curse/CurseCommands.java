package com.newuniverse.nusmp.heroweapon.curse;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Why: the curse system's part of {@code /multiverse hero ...} (show or set despair and corruption, unlock or clear a series, purify), attached by the core's
 * {@code HeroCommand}. Owned by the curse builder, who replaces this stub; the stub is an empty literal.
 */
public final class CurseCommands {
    private CurseCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() { return Commands.literal("curse"); }
}
