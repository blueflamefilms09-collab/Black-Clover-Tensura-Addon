package com.newuniverse.nusmp.saiyan;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

/**
 * Why: the commands of the Saiyan system (3.63.0), a root command of their own. For everybody: {@code /saiyan status} (your own Battle Power, rank and bonuses in words). For operators (permission 2):
 * {@code race <player> on|off} (make a player a Saiyan, or take the race away), {@code awaken <player>} (skip the meditation), {@code bp <player> set|add <value>} (clamped to the rules), {@code status <player>}.
 * Stage 1 has no other way to become a Saiyan; the questionnaire and the race pool are not touched.
 */
public final class SaiyanCommands {
    private SaiyanCommands() {}

    public static void register(IEventBus bus) { bus.addListener(SaiyanCommands::onRegister); }

    private static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("saiyan")
                .then(Commands.literal("status").executes(c -> status(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2)).executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("race").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("on").executes(c -> race(c, EntityArgument.getPlayer(c, "player"), true)))
                                .then(Commands.literal("off").executes(c -> race(c, EntityArgument.getPlayer(c, "player"), false)))))
                .then(Commands.literal("awaken").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> awaken(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("bp").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set").then(Commands.argument("value", DoubleArgumentType.doubleArg(SaiyanRules.START_BP, SaiyanRules.BP_HARD_CAP))
                                        .executes(c -> bp(c, EntityArgument.getPlayer(c, "player"), DoubleArgumentType.getDouble(c, "value"), false))))
                                .then(Commands.literal("add").then(Commands.argument("value", DoubleArgumentType.doubleArg(-SaiyanRules.BP_HARD_CAP, SaiyanRules.BP_HARD_CAP))
                                        .executes(c -> bp(c, EntityArgument.getPlayer(c, "player"), DoubleArgumentType.getDouble(c, "value"), true)))))));
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int no(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int race(CommandContext<CommandSourceStack> c, ServerPlayer p, boolean on) {
        SaiyanState s = SaiyanStore.peek(p);
        if (on == (s != null && s.saiyan)) return no(c, p.getName().getString() + (on ? " is already a Saiyan" : " is not a Saiyan"));
        SaiyanManager.setSaiyan(p, on);
        return say(c, p.getName().getString() + (on ? " is now a Saiyan" : " is no longer a Saiyan (Battle Power is kept)"));
    }

    private static int awaken(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return no(c, p.getName().getString() + " is not a Saiyan");
        if (s.awakened) return no(c, p.getName().getString() + " is already awakened");
        SaiyanManager.awaken(p, s, SaiyanManager.runOf(p));
        return say(c, "Awakened the Ki of " + p.getName().getString());
    }

    private static int bp(CommandContext<CommandSourceStack> c, ServerPlayer p, double v, boolean add) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return no(c, p.getName().getString() + " is not a Saiyan");
        s.setBattlePower(Math.min(SaiyanConfig.maxBp(), add ? s.battlePower + v : v));
        SaiyanManager.markDirty(p);
        return say(c, p.getName().getString() + " now has " + Math.round(s.battlePower) + " Battle Power (" + s.rank().name().toLowerCase(Locale.ROOT) + ")");
    }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return say(c, p.getName().getString() + " is not a Saiyan.");
        SaiyanRules.Table t = SaiyanConfig.bonuses(s.battlePower);
        long now = p.level().getGameTime();
        StringBuilder sb = new StringBuilder(p.getName().getString() + ": " + s.rank().name().toLowerCase(Locale.ROOT) + ", Battle Power " + Math.round(s.battlePower)
                + (s.awakened ? ", Ki awakened" : ", Ki not awakened (sneak still)"));
        sb.append("\n Ki ").append(Math.round(SaiyanKi.current(p))).append(" / ").append(Math.round(SaiyanKi.max(p)))
                .append(", stamina ").append(Math.round(Math.max(0.0, s.stamina < 0.0 ? SaiyanManager.staminaMax(s) : s.stamina))).append(" / ").append(Math.round(SaiyanManager.staminaMax(s)));
        sb.append("\n strength +").append(pct(t.strength())).append("%, durability +").append(pct(t.durability())).append("%, speed +").append(pct(t.speed())).append("%, attack speed +").append(pct(t.attackSpeed()))
                .append("%, jump +").append(pct(t.jump())).append("%, knockback resistance ").append(pct(t.knockback())).append("%, combat experience +").append(pct(t.combatXp())).append("%");
        double room = s.roomToday(now, SaiyanConfig.dailyFraction(), SaiyanConfig.dailyFloor());     // also rolls the day over
        sb.append("\n gained today ").append(Math.round(s.bpToday)).append(" of ").append(Math.round(s.bpToday + room));
        return say(c, sb.toString());
    }

    private static long pct(double v) { return Math.round(v * 100.0); }
}
