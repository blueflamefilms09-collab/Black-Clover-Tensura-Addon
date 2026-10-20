package com.newuniverse.nusmp.perf;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner cannot guess which part of the mod is slow. {@code /multiverse perf on|off|report|reset} (operators, permission level 2) switches the server-side {@link PerfProbe} and
 * prints its ten most expensive sections (name, calls, average, rolling average, maximum, milliseconds of work per second), the state of the per-tick budgets (how much was asked, granted and
 * dropped) and the active settings. It adds a {@code perf} child to the {@code /multiverse} root that {@code MultiverseCommands} registers: the command dispatcher merges two registrations of
 * the same literal, so that file does not change. The client has the twin {@code /nusmp perf} ({@code client.perf.PerfClient}) for the client sections.
 */
public final class PerfCommands {
    private PerfCommands() {}

    /** Game-bus listener. */
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("multiverse").then(Commands.literal("perf").requires(s -> s.hasPermission(2))
                .executes(PerfCommands::report)
                .then(Commands.literal("on").executes(ctx -> switchTo(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> switchTo(ctx, false)))
                .then(Commands.literal("report").executes(PerfCommands::report))
                .then(Commands.literal("reset").executes(PerfCommands::reset))
                .then(Commands.literal("settings").executes(PerfCommands::settings))));
    }

    private static int switchTo(CommandContext<CommandSourceStack> ctx, boolean on) {
        PerfProbe.setEnabled(on);
        say(ctx, on ? "Performance probe ON. Let the server run for a minute, then use /multiverse perf report." : "Performance probe OFF.", ChatFormatting.GOLD);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        PerfProbe.reset();
        PerfBudgets.resetStats();
        say(ctx, "Performance probe cleared.", ChatFormatting.GOLD);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        for (String line : serverReport()) say(ctx, line, ChatFormatting.AQUA);
        return 1;
    }

    private static int settings(CommandContext<CommandSourceStack> ctx) {
        for (String line : settingsLines()) say(ctx, line, ChatFormatting.GRAY);
        return 1;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String line, ChatFormatting color) {
        ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(color), false);
    }

    /** The lines of the server report: the ten most expensive server sections, then the budgets. Public so the self-test and the client twin can reuse the shape. */
    public static List<String> serverReport() {
        List<String> out = new ArrayList<>(PerfProbe.report(10, "s."));
        out.addAll(budgetLines());
        return out;
    }

    public static List<String> budgetLines() {
        List<String> out = new ArrayList<>(3);
        for (PerfBudget b : new PerfBudget[]{PerfBudgets.PARTICLES, PerfBudgets.EFFECT_PACKETS}) out.add(budgetLine(b));
        return out;
    }

    public static String budgetLine(PerfBudget b) {
        return "budget " + b.name() + ": cap " + (b.cap() <= 0 ? "none" : String.valueOf(b.cap())) + " per tick, asked " + b.asked() + ", granted " + b.granted()
                + ", dropped " + b.denied() + ", busiest tick " + b.peak();
    }

    public static List<String> settingsLines() {
        List<String> out = new ArrayList<>(6);
        out.add("vfxQuality " + PerfSettings.vfxQuality().name().toLowerCase(java.util.Locale.ROOT) + " (common " + PerfSettings.commonQuality().name().toLowerCase(java.util.Locale.ROOT) + ")");
        out.add("maxParticlesPerTick " + PerfSettings.maxParticlesPerTick() + ", maxEffectPacketsPerTick " + PerfSettings.maxEffectPacketsPerTick());
        out.add("entityScanInterval " + PerfSettings.entityScanInterval() + ", relicScanInterval " + PerfSettings.relicScanInterval() + ", packetRefreshTicks " + PerfSettings.packetRefreshTicks());
        out.add("auraLodNear " + PerfSettings.auraLodNear() + ", auraLodFar " + PerfSettings.auraLodFar() + ", auraMidCount " + PerfSettings.auraMidCount());
        out.add("resource locations cached: " + RlCache.stats());
        return out;
    }
}
