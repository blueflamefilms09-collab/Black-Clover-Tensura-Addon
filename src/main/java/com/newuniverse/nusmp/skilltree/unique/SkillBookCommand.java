package com.newuniverse.nusmp.skilltree.unique;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.shop.ShopCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;

/**
 * Why: the owner and the testers need to hand out a unique skill, take it back, hand out its book and see whether the line data loaded, without finding a book in the dungeon; and the Black Market's gold and
 * shelf commands need a home now that {@code /multiverse tree} is gone. One node hangs under {@code /multiverse} (the existing command class adds one line for it): {@code skillbook list} (the loaded data and
 * its problems), {@code skillbook learn|forget|give <player> <line>} and the shop's {@code gold} and {@code shop} nodes. All of them require permission level 2, because they create value out of nothing.
 * Teaching goes through {@link UniqueBooks#grant}, the same call a book makes, so a command and a book can never disagree.
 */
public final class SkillBookCommand {
    private SkillBookCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("skillbook").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("list").executes(SkillBookCommand::list));
        root.then(Commands.literal("learn").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("line", StringArgumentType.word()).suggests(SkillBookCommand::suggest).executes(SkillBookCommand::learn))));
        root.then(Commands.literal("forget").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("line", StringArgumentType.word()).suggests(SkillBookCommand::suggest).executes(SkillBookCommand::forget))));
        root.then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("line", StringArgumentType.word()).suggests(SkillBookCommand::suggest).executes(SkillBookCommand::give))));
        root.then(ShopCommands.node());
        root.then(ShopCommands.shopNode());
        return root;
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(UniqueSkills.bares(), b);
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static void fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text).withStyle(ChatFormatting.RED));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        RouteIndex index = RouteRegistry.snapshot();
        say(ctx, UniqueSkills.count() + " unique skill book(s) are registered; the line data of " + index.all().size() + " route(s) is loaded from " + index.report().size() + " file(s), "
                + index.problemFiles() + " with problems.");
        int shown = 0;
        for (RouteIndex.Entry e : index.report()) {
            if (e.clean()) continue;
            if (shown++ >= 8) break;
            say(ctx, " " + (e.loaded() ? "[loaded] " : "[SKIPPED] ") + e.id() + (e.problems().isEmpty() ? "" : ": " + e.problems().get(0)));
        }
        return 1;
    }

    private static LineIndex.Line lineOf(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "line");
        LineIndex.Line line = UniqueSkills.line(id);
        if (line == null) fail(ctx, "No skill book '" + id + "'. Use /multiverse skillbook list and tab-complete the line.");
        return line;
    }

    private static int learn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        LineIndex.Line line = lineOf(ctx);
        if (line == null) return 0;
        UniqueBooks.Grant g = UniqueBooks.grant(p, line.bare(), true);
        switch (g) {
            case GRANTED -> say(ctx, p.getGameProfile().getName() + " learned " + line.name() + ".");
            case ALREADY -> say(ctx, p.getGameProfile().getName() + " already has " + line.name() + ".");
            default -> fail(ctx, "Could not teach " + line.name() + " (" + g + ").");
        }
        return g == UniqueBooks.Grant.GRANTED ? 1 : 0;
    }

    private static int forget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        LineIndex.Line line = lineOf(ctx);
        if (line == null) return 0;
        boolean had = UniqueBooks.owns(p, line.bare());
        boolean gone = UniqueBooks.forget(p, line.bare());
        say(ctx, had ? (gone ? p.getGameProfile().getName() + " no longer has " + line.name() + "." : "Could not take " + line.name() + " away.") : p.getGameProfile().getName() + " did not have " + line.name() + ".");
        return had && gone ? 1 : 0;
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        LineIndex.Line line = lineOf(ctx);
        if (line == null) return 0;
        ItemStack book = SkillBookItem.stackFor(line);
        if (!p.getInventory().add(book)) p.drop(book, false);
        say(ctx, "Gave " + line.bookName() + " to " + p.getGameProfile().getName() + ".");
        return 1;
    }
}
