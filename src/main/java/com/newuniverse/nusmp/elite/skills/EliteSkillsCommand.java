package com.newuniverse.nusmp.elite.skills;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.newuniverse.nusmp.elite.skills.evolution.EvolutionRuntime;
import com.newuniverse.nusmp.elite.skills.evolution.KillTracker;
import com.newuniverse.nusmp.elite.skills.evolution.PlayerEvolutionContext;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Why: the evolution engine reads two things that no other system sets yet (the awakening flags and the reset count), and an admin or a tester needs to see what the engine sees. These are the only
 * commands of the skills subsystem, admin only (permission 2), under the shared {@code /nusmp} root like the other systems': {@code /nusmp eliteskills status <player>} (the elite skills owned, the
 * counters, the requirements of every path from them), {@code awaken <player> <kind> [true|false]} (the flag the {@code awakening_evolution} trigger reads) and {@code resets <player> add|set <n>}
 * (the reset ledger). Nothing here changes a skill or a balance number.
 */
public final class EliteSkillsCommand {
    private EliteSkillsCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp").requires(s -> s.hasPermission(2))
                .then(Commands.literal("eliteskills").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("status").then(Commands.argument("player", EntityArgument.player()).executes(c -> status(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("awaken").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("kind", StringArgumentType.word())
                                .executes(c -> awaken(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "kind"), true))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> awaken(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "kind"), BoolArgumentType.getBool(c, "value")))))))
                        .then(Commands.literal("resets").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("add").executes(c -> addReset(c.getSource(), EntityArgument.getPlayer(c, "player"))))
                                .then(Commands.literal("set").then(Commands.argument("n", IntegerArgumentType.integer(0, 1000000))
                                        .executes(c -> setResets(c.getSource(), EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "n")))))))));
    }

    private static int status(net.minecraft.commands.CommandSourceStack src, ServerPlayer p) {
        StringBuilder sb = new StringBuilder(p.getName().getString()).append(": ");
        int owned = 0;
        for (ManasSkillInstance inst : SkillAPI.getSkillsFrom(p).getLearnedSkills()) {
            if (!(inst.getSkill() instanceof EliteSkill)) continue;
            owned++;
            sb.append(inst.getSkillId().getPath()).append(inst.isMastered(p) ? "*" : "").append(' ');
        }
        final int count = owned;
        src.sendSuccess(() -> Component.literal(sb.toString().trim() + (count == 0 ? "no elite skills" : "  (* = mastered)")), false);
        src.sendSuccess(() -> Component.literal("resets " + ResetLedger.count(p.getUUID()) + ", kills " + KillTracker.get(p, KillTracker.totalKey("any")) + " (boss "
                + KillTracker.get(p, KillTracker.totalKey("boss")) + "), awakened " + new PlayerEvolutionContext(p).awakened("any")), false);
        for (ManasSkillInstance inst : SkillAPI.getSkillsFrom(p).getLearnedSkills()) {
            for (String line : EvolutionRuntime.describe(inst.getSkillId().toString())) src.sendSuccess(() -> Component.literal("  " + inst.getSkillId().getPath() + " -> " + line), false);
        }
        return count;
    }

    private static int awaken(net.minecraft.commands.CommandSourceStack src, ServerPlayer p, String kind, boolean value) {
        PlayerEvolutionContext.setAwakened(p, kind, value);
        src.sendSuccess(() -> Component.literal(p.getName().getString() + " awakened as " + kind + ": " + value), true);
        return 1;
    }

    private static int addReset(net.minecraft.commands.CommandSourceStack src, ServerPlayer p) {
        int n = ResetLedger.record(p, "command");
        src.sendSuccess(() -> Component.literal(p.getName().getString() + " has now reset " + n + " times"), true);
        return n;
    }

    private static int setResets(net.minecraft.commands.CommandSourceStack src, ServerPlayer p, int n) {
        ResetLedger.set(p, n);
        src.sendSuccess(() -> Component.literal(p.getName().getString() + " reset count set to " + n), true);
        return n;
    }
}
