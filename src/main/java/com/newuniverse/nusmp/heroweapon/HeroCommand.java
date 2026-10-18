package com.newuniverse.nusmp.heroweapon;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.heroweapon.curse.CurseCommands;
import com.newuniverse.nusmp.heroweapon.forms.FormsCommands;
import com.newuniverse.nusmp.heroweapon.gem.GemCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Why: {@code /multiverse hero ...} is one command tree built from four parts so that four builders never edit one class: the core's own sub-commands (status,
 * list, summon, dismiss, bind, release, level) and the nodes of the forms, curse and gem systems. Attached to the root by {@code MultiverseCommands} with one line.
 * Owned by the core builder, who adds its sub-commands here; the three other nodes are theirs.
 */
public final class HeroCommand {
    private HeroCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("hero")
                .then(FormsCommands.node())
                .then(CurseCommands.node())
                .then(GemCommands.node());
    }
}
