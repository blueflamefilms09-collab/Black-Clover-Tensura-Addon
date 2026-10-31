package com.newuniverse.nusmp.entity.veldanava.skills;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the Veldanava boss that used to hand out the five ultimate skills is gone, so an admin gives them: {@code /multiverse ultimate give <player> <id>} (permission 2) and
 * {@code /multiverse ultimate take <player>}. Both end in the one grant / revoke of {@link VeldanavaSkills}, so the rules do not change: one skill of the five per player, the id is checked
 * against {@link JudgmentRules#claimable}, nothing else is ever named. No ticking, no packet, no state of its own. Server side.
 */
public final class UltimateCommand {
    private UltimateCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("ultimate").requires(s -> s.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(JudgmentRules.CARDS.stream().filter(JudgmentRules.Card::available).map(JudgmentRules.Card::id), b))
                                .executes(UltimateCommand::give))))
                .then(Commands.literal("take").then(Commands.argument("player", EntityArgument.player()).executes(UltimateCommand::take)));
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        String id = StringArgumentType.getString(ctx, "id");
        if (!JudgmentRules.claimable(id)) return fail(ctx, "'" + id + "' is not one of the five ultimate skills (sandalphon, israfil, jibril, solomon, hercules).");
        if (VeldanavaSkills.received(p)) return fail(ctx, p.getName().getString() + " already holds " + VeldanavaSkills.chosen(p) + ": take it first.");
        if (!VeldanavaSkills.grant(p, id)) return fail(ctx, id + " could not be given to " + p.getName().getString() + ".");
        ctx.getSource().sendSuccess(() -> Component.literal("Gave " + JudgmentRules.card(id).title() + " to " + p.getName().getString() + "."), true);
        return 1;
    }

    private static int take(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        if (!VeldanavaSkills.received(p)) return fail(ctx, p.getName().getString() + " holds no ultimate skill.");
        VeldanavaSkills.revoke(p);
        ctx.getSource().sendSuccess(() -> Component.literal("Took the ultimate skill back from " + p.getName().getString() + "."), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
