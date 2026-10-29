package com.newuniverse.nusmp.skilltree.unique;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.skilltree.shop.ShopCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Why: the Black Market's gold and shelf commands live under {@code /multiverse skillbook}, a path the owner and the testers already use, so the node keeps its name. The Skill Books and the unique skills they
 * taught were removed (see {@code docs/skill_books_removed.md}), and with them the node's {@code list}, {@code learn}, {@code forget} and {@code give} subcommands; what is left is the shop's {@code gold} and
 * {@code shop} nodes. All of them require permission level 2, because they create value out of nothing.
 */
public final class SkillBookCommand {
    private SkillBookCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("skillbook").requires(s -> s.hasPermission(2));
        root.then(ShopCommands.node());
        root.then(ShopCommands.shopNode());
        return root;
    }
}
