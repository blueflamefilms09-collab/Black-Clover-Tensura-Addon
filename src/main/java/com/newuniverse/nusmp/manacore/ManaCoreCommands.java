package com.newuniverse.nusmp.manacore;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/**
 * Why: the operator commands of the rank Mana Cores (3.51.0), a root command of their own: {@code /manacore give <player> <rank> <variant>} gives one core (permission 2) and {@code /manacore list} names the 24 ids.
 * Nothing here touches anything outside the cores.
 */
public final class ManaCoreCommands {
    private ManaCoreCommands() {}

    public static void register(IEventBus bus) { bus.addListener(ManaCoreCommands::onRegister); }

    private static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("manacore").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ManaCoreCommands::list))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("rank", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.asList(ManaCoreRules.RANKS), b))
                                .then(Commands.argument("variant", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.asList(ManaCoreRules.VARIANTS), b))
                                        .executes(c -> give(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "rank"), StringArgumentType.getString(c, "variant"))))))));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        StringBuilder sb = new StringBuilder();
        for (ManaCoreRules.Core core : ManaCoreRules.all()) sb.append(core.id()).append(' ');
        c.getSource().sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return ManaCoreRules.all().size();
    }

    private static int give(CommandContext<CommandSourceStack> c, ServerPlayer to, String rank, String variant) {
        ManaCoreRules.Core core = ManaCoreRules.of(ManaCoreRules.rankByName(rank), ManaCoreRules.variantByName(variant));
        ItemStack stack = ManaCoreItems.stack(core);
        if (stack.isEmpty()) {
            c.getSource().sendFailure(Component.literal("no such core: " + rank + " " + variant));
            return 0;
        }
        if (!to.getInventory().add(stack)) to.drop(stack, false);
        c.getSource().sendSuccess(() -> Component.literal("gave " + core.name() + " to " + to.getGameProfile().getName()), true);
        return 1;
    }
}
