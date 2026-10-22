package com.newuniverse.nusmp.engraving.codex;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /multiverse engravings} gives the Engravement Codex (the book of every engraving of the mod) to the player who runs it and says where else it comes from: crafted from a book, two
 * amethyst shards and two ink sacs, or taken from the Multiverse creative tab. Everyone may run it (no permission): the book is plain information. The player is always the one running
 * the command (no target argument). Attach with {@code root.then(EngravingCodexCommand.node())}.
 */
public final class EngravingCodexCommand {
    private EngravingCodexCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("engravings").executes(EngravingCodexCommand::give);
    }

    private static int give(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException notAPlayer) {
            ctx.getSource().sendFailure(Component.translatableWithFallback("nusmp.engraving_codex.cmd.console", "Only a player can be handed the Engravement Codex."));
            return 0;
        }
        ItemStack book = EngravingCodexItems.codexStack();
        if (!player.getInventory().add(book)) player.drop(book, false);
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("nusmp.engraving_codex.cmd.given", "You were given the Engravement Codex. Use it to open the list of every engraving.").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("nusmp.engraving_codex.cmd.where",
                "You can also craft it (a book, 2 amethyst shards and 2 ink sacs in a cross) or take it from the Multiverse of Anime creative tab.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
