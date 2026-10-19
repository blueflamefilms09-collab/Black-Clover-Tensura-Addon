package com.newuniverse.nusmp.fairytail;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Why: the owner's admin hook for the Fairy Tail skill books: {@code /multiverse fairytail give <skill> [player]} puts the book of a skill
 * into the inventory, {@code learn} teaches the skill at once (for testing) and {@code list} prints the ids. Attach with
 * {@code root.then(FairyTailCommand.node())}. Operators only (permission level 2).
 */
public final class FairyTailCommand {
    private FairyTailCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("fairytail").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(FairyTailCommand::list))
                .then(Commands.literal("give").then(Commands.argument("skill", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(FtLine.values()).map(l -> l.id), b))
                        .executes(ctx -> give(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> give(ctx, EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("learn").then(Commands.argument("skill", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(FtLine.values()).map(l -> l.id), b))
                        .executes(ctx -> learn(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> learn(ctx, EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        for (FtLine l : FtLine.values()) {
            ctx.getSource().sendSuccess(() -> Component.literal(l.id + "  (" + l.modes() + " spells" + (l.ultimate ? ", Ultimate" : "") + ")").withStyle(ChatFormatting.GOLD), false);
        }
        return FtLine.values().length;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, ServerPlayer to) {
        FtLine line = FtLine.byId(StringArgumentType.getString(ctx, "skill"));
        if (line == null) { ctx.getSource().sendFailure(Component.literal("Unknown skill. Try /multiverse fairytail list")); return 0; }
        ItemStack book = FtBookItem.of(line);
        if (!to.getInventory().add(book)) to.drop(book, false);
        ctx.getSource().sendSuccess(() -> Component.literal("Gave the " + line.name + " book to " + to.getName().getString()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int learn(CommandContext<CommandSourceStack> ctx, ServerPlayer to) {
        FtLine line = FtLine.byId(StringArgumentType.getString(ctx, "skill"));
        if (line == null) { ctx.getSource().sendFailure(Component.literal("Unknown skill. Try /multiverse fairytail list")); return 0; }
        String why = FtBookItem.learn(to, line);
        if (why != null) { ctx.getSource().sendFailure(Component.literal(why)); return 0; }
        ctx.getSource().sendSuccess(() -> Component.literal(to.getName().getString() + " learned " + line.name).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
