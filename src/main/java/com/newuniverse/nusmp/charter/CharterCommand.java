package com.newuniverse.nusmp.charter;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.core.discovery.DiscoveryKeys;
import com.newuniverse.nusmp.core.discovery.DiscoveryRules;
import com.newuniverse.nusmp.core.discovery.DiscoveryTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * {@code /multiverse charter ...}: attached to the multiverse root as {@code CharterCommand.addTo(root)}.
 * <ul>
 *   <li>{@code status}: who claimed which Charter Grimoire (anyone);</li>
 *   <li>{@code give <player> <bossId>} (permission 2): gives a grimoire without touching the world's lock;</li>
 *   <li>{@code reset <bossId>} (permission 4): clears the lock of one boss, for a grimoire that was lost for good. Logged as a warning.</li>
 * </ul>
 */
public final class CharterCommand {
    private CharterCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("charter")
                .then(Commands.literal("status").executes(CharterCommand::status))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("boss", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(CharterTable.ids(), b))
                                        .executes(CharterCommand::give))))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(4))
                        .then(Commands.argument("boss", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(CharterTable.ids(), b))
                                .executes(CharterCommand::reset))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ModWorldDropData data = ModWorldDropData.get(server);
        ctx.getSource().sendSuccess(() -> Component.literal("Charter Grimoires:").withStyle(ChatFormatting.GOLD), false);
        for (CharterTable.Boss b : CharterTable.all()) {
            DropLedger.Claim c = data.claim(b.id());
            // 1.27.0 (discovery): a boss the player has not met (no grimoire of it found) stays "???"; operators and the console see all
            if (!DiscoveryTracker.canSee(ctx.getSource(), DiscoveryKeys.modItem(b.itemId())) && !DiscoveryTracker.canSee(ctx.getSource(), DiscoveryRules.bossKey(b.id()))) {
                ctx.getSource().sendSuccess(() -> Component.literal(" " + DiscoveryRules.QUESTION), false);
                continue;
            }
            ctx.getSource().sendSuccess(() -> Component.literal(" " + b.id() + ": " + (c == null ? "unclaimed" : "claimed by " + c.name() + " on day " + c.day())), false);
        }
        return data.all().size();
    }

    private static int give(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
            String boss = StringArgumentType.getString(ctx, "boss");
            if (CharterTable.byId(boss) == null || !CharterEvents.give(p, CharterTable.byId(boss).id())) {
                ctx.getSource().sendFailure(Component.literal("Unknown boss '" + boss + "'. Known: " + String.join(", ", CharterTable.ids())));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal("Gave the Charter Grimoire of " + boss + " to " + p.getGameProfile().getName() + " (the world's lock is untouched)."), true);
            return 1;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException bad) {
            ctx.getSource().sendFailure(Component.literal("No such player."));
            return 0;
        }
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        String boss = StringArgumentType.getString(ctx, "boss");
        CharterTable.Boss b = CharterTable.byId(boss);
        if (b == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown boss '" + boss + "'. Known: " + String.join(", ", CharterTable.ids())));
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
        ModWorldDropData data = ModWorldDropData.get(server);
        DropLedger.Claim old = data.adminClear(b.id(), ctx.getSource().getTextName());
        if (old == null) {
            ctx.getSource().sendFailure(Component.literal("The Charter Grimoire of " + b.id() + " is not claimed."));
            return 0;
        }
        CharterNet.sendAll(data);
        ctx.getSource().sendSuccess(() -> Component.literal("The lock of the Charter Grimoire of " + b.id() + " is cleared (it was " + old.name() + "'s). The next kill rolls again."), true);
        return 1;
    }
}
