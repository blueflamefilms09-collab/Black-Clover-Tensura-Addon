package com.newuniverse.nusmp.arena;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Why: the arena needs a handful of commands that the items cannot be: {@code /zogratis_arena leave} (anyone; it is what the clickable prompt after the
 * victory runs, and it is refused while the boss fights so it is no free exit), and, for operators (permission 2), {@code enter} (go there without
 * the hourglass, to test), {@code reset} (clear the state and remove the boss), {@code repair} (put the courtyard back) and {@code status}. It is a
 * root command of its own so that the shared multiverse command class does not change.
 */
public final class ArenaCommand {
    private ArenaCommand() {}

    public static void register(IEventBus bus) { bus.addListener(ArenaCommand::onRegister); }

    private static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("zogratis_arena")
                .then(Commands.literal("leave").executes(ArenaCommand::leave))
                .then(Commands.literal("enter").requires(s -> s.hasPermission(2)).executes(ArenaCommand::enter))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(ArenaCommand::reset))
                .then(Commands.literal("repair").requires(s -> s.hasPermission(2)).executes(ArenaCommand::repair))
                .then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(ArenaCommand::status)));
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return fail(ctx, "Only a player can leave the arena.");
        if (!ZogratisArena.in(p)) return fail(ctx, "You are not in the arena of the Final Hour.");
        if (ArenaDirector.fightRunning(p.getServer()) && !ctx.getSource().hasPermission(2)) return fail(ctx, "Lucius is fighting. Use the Hourglass of the Final Hour (ten seconds) to leave.");
        return ArenaTravel.leave(p) ? 1 : 0;
    }

    private static int enter(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return fail(ctx, "Only a player can enter the arena.");
        return ArenaTravel.enter(p, java.util.List.of()) ? 1 : 0;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        ArenaDirector.reset(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("The arena is reset: no boss, no rest."), true);
        return 1;
    }

    private static int repair(CommandContext<CommandSourceStack> ctx) {
        net.minecraft.server.level.ServerLevel sl = ctx.getSource().getServer().getLevel(ZogratisArena.DIMENSION);
        if (sl == null) return fail(ctx, "The arena dimension is not loaded.");
        int n = ArenaBuilder.repair(sl);
        ctx.getSource().sendSuccess(() -> Component.literal("The courtyard is repaired: " + n + " blocks set."), true);
        return n;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ArenaSave save = ArenaSave.get(ctx.getSource().getServer());
        ArenaState st = save.state();
        net.minecraft.server.level.ServerLevel sl = ctx.getSource().getServer().getLevel(ZogratisArena.DIMENSION);
        long now = sl == null ? 0 : sl.getGameTime();
        ctx.getSource().sendSuccess(() -> Component.literal("Arena: built=" + save.built() + ", phase=" + st.phase() + ", defeated=" + st.defeated() + ", rest left="
                + st.cooldownSecondsLeft(now) + " s, boss=" + save.bossId() + ", return records=" + save.returns().size() + ", luciusArenaOnly=" + ArenaConfig.arenaOnly()), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
