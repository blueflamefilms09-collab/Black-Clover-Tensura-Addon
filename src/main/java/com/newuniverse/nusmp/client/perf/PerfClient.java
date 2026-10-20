package com.newuniverse.nusmp.client.perf;

import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfCommands;
import com.newuniverse.nusmp.perf.PerfConfig;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.vfx.client.VfxStats;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the client half of the performance safety nets (see {@code perf.PerfBoot} for the server half). It registers the client file nusmp-performance-client.toml, the probe's two wide client
 * sections (everything that hangs on the client tick, everything that draws after the particles: the VFX layers, the FX pieces, the zone, the floating grimoires), and the command
 * {@code /nusmp perf on|off|report|reset}, which prints the ten most expensive client sections plus what the effect system did last frame ({@link VfxStats}: effects drawn, culled, thinned,
 * vertices). Client-side commands need no operator rights: they only read this machine.
 */
public final class PerfClient {
    private PerfClient() {}

    private static final PerfProbe.Section CLIENT_TICK = PerfProbe.section("c.event.client_tick");
    private static final PerfProbe.Section RENDER_AFTER_PARTICLES = PerfProbe.section("c.event.render_after_particles");
    private static long tTick, tRender;
    private static final int REFRESH_EVERY = 100;
    private static int refresh;

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, PerfConfig.CLIENT, "nusmp-performance-client.toml");
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ClientTickEvent.Post e) -> tTick = PerfProbe.begin());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ClientTickEvent.Post e) -> {
            PerfProbe.end(CLIENT_TICK, tTick);
            tTick = 0L;
            if (++refresh >= REFRESH_EVERY) {
                refresh = 0;
                PerfConfig.apply();
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) tRender = PerfProbe.begin();
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                PerfProbe.end(RENDER_AFTER_PARTICLES, tRender);
                tRender = 0L;
            }
        });
        NeoForge.EVENT_BUS.addListener(PerfClient::registerCommands);
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp").then(Commands.literal("perf")
                .executes(PerfClient::report)
                .then(Commands.literal("on").executes(ctx -> {
                    PerfProbe.setEnabled(true);
                    sayKey(ctx, "nusmp.perf.client.on", "Client performance probe ON. Play for a minute, then use /nusmp perf report.", ChatFormatting.GOLD);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    PerfProbe.setEnabled(false);
                    sayKey(ctx, "nusmp.perf.client.off", "Client performance probe OFF.", ChatFormatting.GOLD);
                    return 1;
                }))
                .then(Commands.literal("report").executes(PerfClient::report))
                .then(Commands.literal("reset").executes(ctx -> {
                    PerfProbe.reset();
                    VfxStats.reset();
                    PerfBudgets.resetStats();
                    sayKey(ctx, "nusmp.perf.client.reset", "Client performance probe cleared.", ChatFormatting.GOLD);
                    return 1;
                }))
                .then(Commands.literal("settings").executes(ctx -> {
                    for (String line : PerfCommands.settingsLines()) say(ctx, line, ChatFormatting.GRAY);
                    return 1;
                }))));
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        for (String line : clientReport()) say(ctx, line, ChatFormatting.AQUA);
        return 1;
    }

    /** The lines of the client report: the ten most expensive client sections, the effect system's last frame, the client budget. */
    public static List<String> clientReport() {
        List<String> out = new ArrayList<>(PerfProbe.report(10, "c."));
        out.add(VfxStats.line());
        out.add(PerfCommands.budgetLine(PerfBudgets.CLIENT_PARTICLES));
        return out;
    }

    private static void sayKey(CommandContext<CommandSourceStack> ctx, String key, String fallback, ChatFormatting color) {
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(key, fallback).withStyle(color), false);
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String line, ChatFormatting color) {
        ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(color), false);
    }
}
