package com.newuniverse.nusmp.boss.bars;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the owner cannot summon every boss to look at its bar. {@code /multiverse bossbar preview <style> [hp%] [phase]} (operators) sends the caller a bar of that style with no boss behind it
 * (twelve seconds, three phases, a shield, a wind-up and a lair warning to show the parts); {@code /multiverse bossbar styles} lists the ids. It adds a {@code bossbar} child to the
 * {@code /multiverse} root: the dispatcher merges two registrations of the same literal. The client twin {@code /nusmp bossbar preview} needs no operator rights (it only draws on this machine).
 */
public final class BossBarCommands {
    private BossBarCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("multiverse").then(Commands.literal("bossbar").requires(s -> s.hasPermission(2))
                .then(Commands.literal("styles").executes(BossBarCommands::styles))
                .then(Commands.literal("preview").then(Commands.argument("style", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(BarStyleIds.ALL, b))
                        .executes(ctx -> preview(ctx, 100, 1))
                        .then(Commands.argument("hp", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> preview(ctx, IntegerArgumentType.getInteger(ctx, "hp"), 0))
                                .then(Commands.argument("phase", IntegerArgumentType.integer(1, 5))
                                        .executes(ctx -> preview(ctx, IntegerArgumentType.getInteger(ctx, "hp"), IntegerArgumentType.getInteger(ctx, "phase")))))))));
    }

    private static int styles(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("nusmp.bossbar.cmd.styles", "Boss bar styles: %s", String.join(", ", BarStyleIds.ALL)).withStyle(ChatFormatting.AQUA), false);
        return BarStyleIds.ALL.size();
    }

    private static int preview(CommandContext<CommandSourceStack> ctx, int hp, int phase) {
        String style = StringArgumentType.getString(ctx, "style");
        if (!BarStyleIds.known(style)) {
            ctx.getSource().sendFailure(Component.translatableWithFallback("nusmp.bossbar.cmd.unknown", "No boss bar style '%s'. Try /multiverse bossbar styles.", style));
            return 0;
        }
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        PacketDistributor.sendToPlayer(p, new BossBarStatePayload.Preview(style, hp, phase, 3));
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("nusmp.bossbar.cmd.preview", "Showing the '%s' boss bar for twelve seconds.", style).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}
