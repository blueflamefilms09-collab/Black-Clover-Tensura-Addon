package com.newuniverse.nusmp.greed;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import io.github.manasmods.manascore.skill.api.ManasSkill;

import java.util.List;

/**
 * Why: {@code /multiverse greed ...}, attached to the root by {@code MultiverseCommands} with one line. For operators (permission 2): {@code status}, {@code give} (the creative stack: it becomes THE sword only if none
 * exists) and {@code reset} (the sword leaves the world, its tier is kept). For the wielder: {@code form <0-7>} (as the form key), {@code recover} (a lost sword is made whole again, every older stack dissolves) and
 * {@code vault} (the Gluttony Skill Vault as a clickable list: equip, favourite, sacrifice). All validated and server side.
 */
public final class GreedCommand {
    private GreedCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("greed")
                .then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(GreedCommand::status))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2)).executes(GreedCommand::give))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(GreedCommand::reset))
                .then(Commands.literal("form").then(Commands.argument("n", IntegerArgumentType.integer(0, GreedRules.FORMS - 1)).executes(GreedCommand::form)))
                .then(Commands.literal("recover").executes(GreedCommand::recover))
                .then(Commands.literal("vault").executes(GreedCommand::vault)
                        .then(Commands.literal("equip").then(Commands.argument("n", IntegerArgumentType.integer(1, GreedRules.VAULT_MAX)).executes(c -> act(c, 0))))
                        .then(Commands.literal("favourite").then(Commands.argument("n", IntegerArgumentType.integer(1, GreedRules.VAULT_MAX)).executes(c -> act(c, 1))))
                        .then(Commands.literal("sacrifice").then(Commands.argument("n", IntegerArgumentType.integer(1, GreedRules.VAULT_MAX)).executes(c -> act(c, 2)))));
    }

    private static int say(CommandContext<CommandSourceStack> c, Component t) {
        c.getSource().sendSuccess(() -> t, false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String t) {
        c.getSource().sendFailure(Component.literal(t));
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        if (!GreedData.bound()) return fail(c, "The server has not started.");
        GreedData d = GreedData.get();
        return say(c, Component.literal("Black Sword Greed: " + (d.spawned() ? "in the world" : "not in the world") + ", wielder " + (d.owner() == null ? "none" : d.owner()) + ", tier " + d.tier() + ", form " + d.form()));
    }

    private static int give(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only a player can be given the sword.");
        p.getInventory().add(GreedItems.creativeStack());
        return say(c, Component.literal("A bare black sword is in your inventory: it becomes THE sword only if none exists."));
    }

    private static int reset(CommandContext<CommandSourceStack> c) {
        if (!GreedData.bound()) return fail(c, "The server has not started.");
        GreedData d = GreedData.get();
        java.util.UUID o = d.owner();
        if (o != null) GreedService.release(c.getSource().getServer(), o, null, null);
        d.vanish();
        return say(c, Component.literal("The Black Sword Greed has left the world (its tier is kept); every stack of it dissolves."));
    }

    private static int form(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only the wielder can switch forms.");
        return GreedService.switchForm(p, IntegerArgumentType.getInteger(c, "n")) ? 1 : fail(c, "The form did not change (not the wielder, not open yet, or too soon).");
    }

    private static int recover(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only the wielder can recover the sword.");
        return GreedService.recover(p) ? say(c, Component.literal("The black sword returns to your hand.")) : fail(c, "You are not the wielder.");
    }

    private static int vault(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null || !GreedData.bound() || !GreedData.get().isOwner(p.getUUID())) return fail(c, "Only the wielder has a Gluttony Skill Vault.");
        GreedState st = GreedStore.of(p);
        List<String> ids = st.vaultIds();
        if (ids.isEmpty()) return say(c, Component.translatableWithFallback("greed.vault.empty", "The Gluttony Skill Vault is empty. Kill to devour skills.").withStyle(ChatFormatting.GRAY));
        say(c, Component.translatableWithFallback("greed.vault.title", "Gluttony Skill Vault (%s)", ids.size()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        for (int i = 0; i < ids.size(); i++) {
            ManasSkill sk = GreedService.skillOf(ids.get(i));
            Component name = sk == null ? Component.literal(ids.get(i)) : sk.getName();
            int n = i + 1;
            MutableComponent line = Component.literal(n + ". ").withStyle(ChatFormatting.DARK_GRAY).append(Component.empty().append(name).withStyle(st.favourited(ids.get(i)) ? ChatFormatting.GOLD : ChatFormatting.WHITE)).append(" ")
                    .append(button("[Equip]", "/multiverse greed vault equip " + n, ChatFormatting.GREEN)).append(" ")
                    .append(button("[Favourite]", "/multiverse greed vault favourite " + n, ChatFormatting.YELLOW)).append(" ")
                    .append(button("[Sacrifice]", "/multiverse greed vault sacrifice " + n, ChatFormatting.RED));
            say(c, line);
        }
        return 1;
    }

    private static MutableComponent button(String label, String command, ChatFormatting color) {
        return Component.literal(label).withStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static int act(CommandContext<CommandSourceStack> c, int what) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only the wielder has a Gluttony Skill Vault.");
        int i = IntegerArgumentType.getInteger(c, "n") - 1;
        String key = switch (what) {
            case 0 -> GreedService.vaultEquip(p, i);
            case 1 -> GreedService.vaultFavourite(p, i);
            default -> GreedService.vaultSacrifice(p, i);
        };
        return switch (key) {
            case "greed.vault.ok" -> say(c, Component.translatableWithFallback(key, "Done.").withStyle(ChatFormatting.GREEN));
            case "greed.vault.have" -> fail(c, "You already have that skill.");
            case "greed.vault.fav" -> fail(c, "A favourite cannot be sacrificed: remove the favourite first.");
            default -> fail(c, "No such vault entry.");
        };
    }
}
