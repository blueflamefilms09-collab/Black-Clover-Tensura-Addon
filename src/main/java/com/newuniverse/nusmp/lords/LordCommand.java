package com.newuniverse.nusmp.lords;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Why: {@code /multiverse lords ...}, attached to the root by {@code MultiverseCommands} with one line. For everybody: {@code info <sin>} (what the weapon takes from you, how much, how it recovers, how to avoid it: the same
 * words as the equip warning) and {@code mastery} (the mastery levels of the player). For the wielder: {@code form <sin> <n>} and {@code recover <sin>} (a lost weapon is made whole again, every older stack dissolves).
 * For operators (permission 2): {@code status}, {@code give <sin>} (a bare stack: it becomes THE weapon only if none exists) and {@code reset <sin>} (the weapon leaves the world). All validated and server side.
 */
public final class LordCommand {
    private LordCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("lords")
                .then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(LordCommand::status))
                .then(Commands.literal("mastery").executes(LordCommand::mastery))
                .then(Commands.literal("info").then(sinArg().executes(LordCommand::info)))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2)).then(sinArg().executes(LordCommand::give)))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).then(sinArg().executes(LordCommand::reset)))
                .then(Commands.literal("recover").then(sinArg().executes(LordCommand::recover)))
                .then(Commands.literal("form").then(sinArg().then(Commands.argument("n", IntegerArgumentType.integer(0, 2)).executes(LordCommand::form))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> sinArg() {
        return Commands.argument("sin", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"sloth", "envy", "vanity", "holy", "greed"}, b));
    }

    private static LordRules.Sin sin(CommandContext<CommandSourceStack> c) {
        String id = StringArgumentType.getString(c, "sin");
        for (LordRules.Sin s : LordRules.Sin.values()) if (s.id.equalsIgnoreCase(id)) return s;
        return null;
    }

    private static LordRules.Sin lord(CommandContext<CommandSourceStack> c) {
        LordRules.Sin s = sin(c);
        if (s == null || !s.isLord()) {
            c.getSource().sendFailure(Component.literal("Use one of: sloth, envy, vanity, holy."));
            return null;
        }
        return s;
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
        if (!LordData.bound()) return fail(c, "The server has not started.");
        LordData d = LordData.get();
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord()) continue;
            UUID o = d.owner(s);
            say(c, Component.literal(s.english + ": " + (d.spawned(s) ? "in the world" : "not in the world") + ", wielder " + (o == null ? "none" : o) + ", form " + d.form(s)));
        }
        return 1;
    }

    private static int mastery(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only a player has a mastery.");
        LordState st = LordStore.of(p);
        for (LordRules.Sin s : LordRules.Sin.values()) {
            say(c, Component.translatable(s.masteryKey()).append(Component.literal(": " + st.level(s) + " / " + LordRules.MAX_LEVEL + " (" + LordRules.progress(st.xp(s)) + "%)")).withStyle(ChatFormatting.GOLD));
        }
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        LordRules.Sin s = sin(c);
        if (s == null) return fail(c, "Use one of: sloth, envy, vanity, holy, greed.");
        say(c, Component.translatable("lords.name." + s.id).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        say(c, Component.translatable("lords.warning.intro", Component.translatable(s.drawbackNameKey())).withStyle(ChatFormatting.RED));
        for (int i = 1; i <= LordText.drawbackLines(s); i++) say(c, Component.translatable(s.drawbackKey(i)).withStyle(ChatFormatting.YELLOW));
        return say(c, Component.translatable("lords.warning.release").withStyle(ChatFormatting.GRAY));
    }

    private static int give(CommandContext<CommandSourceStack> c) {
        LordRules.Sin s = lord(c);
        if (s == null) return 0;
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only a player can be given the weapon.");
        p.getInventory().add(LordItems.bare(s));
        return say(c, Component.literal("A bare " + s.english + " weapon is in your inventory: it becomes THE weapon only if none exists."));
    }

    private static int reset(CommandContext<CommandSourceStack> c) {
        LordRules.Sin s = lord(c);
        if (s == null) return 0;
        if (!LordData.bound()) return fail(c, "The server has not started.");
        LordData d = LordData.get();
        UUID o = d.owner(s);
        if (o != null) LordService.release(c.getSource().getServer(), s, o, null, null);
        d.vanish(s);
        return say(c, Component.literal("The weapon has left the world (the mastery stays with its wielder); every stack of it dissolves."));
    }

    private static int recover(CommandContext<CommandSourceStack> c) {
        LordRules.Sin s = lord(c);
        if (s == null) return 0;
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only the wielder can recover the weapon.");
        return LordService.recover(p, s) ? say(c, Component.literal("The weapon returns to your hand.")) : fail(c, "You are not the wielder.");
    }

    private static int form(CommandContext<CommandSourceStack> c) {
        LordRules.Sin s = lord(c);
        if (s == null) return 0;
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, "Only the wielder can switch forms.");
        return LordService.switchForm(p, s, IntegerArgumentType.getInteger(c, "n")) ? 1 : fail(c, "The form did not change (not the wielder, not open yet, or too soon).");
    }
}
