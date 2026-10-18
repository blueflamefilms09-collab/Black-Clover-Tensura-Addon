package com.newuniverse.nusmp.compat;

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
 * {@code /multiverse compat book} gives the Compatibility Codex (the grimoire-style item; the written-book copy stays as {@link CompatBookItem#stack}); {@code /multiverse compat status} prints the live Loaded / Not installed
 * list in chat. Both are for everyone (no permission). Attach with {@code root.then(CompatCommand.node())}.
 */
public final class CompatCommand {
    private CompatCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("compat")
                .then(Commands.literal("book").executes(ctx -> book(ctx, false))
                        .then(Commands.literal("written").executes(ctx -> book(ctx, true))))          // 1.34.0: the old vanilla written book, kept as the fallback
                .then(Commands.literal("status").executes(CompatCommand::status))
                .then(com.newuniverse.nusmp.compat.maps.MapsCommand.compatMapsNode());                  // compat-maps: /multiverse compat maps (op)
    }

    private static int book(CommandContext<CommandSourceStack> ctx, boolean written) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException notAPlayer) {
            ctx.getSource().sendFailure(Component.translatable("nusmp.compat.book.console"));
            return 0;
        }
        ItemStack book = written ? CompatBookItem.stack() : CompatItems.codexStack();                       // 1.34.0: the grimoire-style item (CompatBookItem.stack() is the written-book fallback)
        if (!player.getInventory().add(book)) player.drop(book, false);
        ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.compat.book.given"), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.compat.status.header").withStyle(ChatFormatting.GOLD), false);
        int loadedCount = 0;
        for (CompatCatalog.Status s : CompatCatalog.statuses(CompatBookItem::isLoaded)) {
            if (s.loaded()) loadedCount++;
            Component word = Component.translatable(s.loaded() ? "nusmp.compat.loaded" : "nusmp.compat.absent")
                    .withStyle(s.loaded() ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.compat.status.line", s.entry().name(), word), false);
        }
        boolean pack = CompatCatalog.packActive(CompatBookItem::isLoaded);
        ctx.getSource().sendSuccess(() -> Component.translatable(pack ? "nusmp.compat.pack.on" : "nusmp.compat.pack.off", CompatCatalog.PACK_ID), false);
        return loadedCount;
    }
}
