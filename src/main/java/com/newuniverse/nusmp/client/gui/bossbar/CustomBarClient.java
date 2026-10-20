package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.boss.bar.BossBarNet;
import com.newuniverse.nusmp.boss.bars.BarStyleIds;
import com.newuniverse.nusmp.boss.bars.BossBarState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the client's hub of the custom boss bars: it receives the states and clears from the server, owns the per-bar {@link CustomBarState}, loads the styles on resource reload, wires the overlay
 * and the client command {@code /nusmp bossbar preview <style> [hp%] [phase]} (no operator rights needed: it only draws on this machine). The vanilla boss event of a bar (name, progress,
 * visibility) is still what makes a bar exist; a state without a vanilla event is only kept briefly (it may arrive a moment before the add packet), and a bar that dies keeps its state
 * for the death animation. Client thread only.
 */
public final class CustomBarClient {
    private CustomBarClient() {}

    /** The id of the one preview bar. */
    static final UUID PREVIEW_ID = UUID.nameUUIDFromBytes("nusmp:boss_bar_preview".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final int PREVIEW_TICKS = 240;
    private static final Map<UUID, CustomBarState> BARS = new HashMap<>();

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(new BarStyles()));
        CustomBarOverlay.init();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> BARS.clear());
        NeoForge.EVENT_BUS.addListener(CustomBarClient::registerCommands);
    }

    // ================================================================ from the server
    public static void receive(BossBarState s) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        CustomBarState st = BARS.get(s.bar());
        if (st == null) {
            st = new CustomBarState(s.bar(), s, now);
            BARS.put(s.bar(), st);
        } else {
            st.apply(s, now);
        }
    }

    /** The bar is gone: forget it, unless its boss died (then the death animation runs and the overlay forgets it afterwards). */
    public static void receive(BossBarNet.Clear c) {
        CustomBarState st = BARS.get(c.bar());
        if (st == null) return;
        if (st.s != null && st.s.dead()) st.dying = true;
        else BARS.remove(c.bar());
    }

    // ================================================================ access for the overlay
    static CustomBarState get(UUID id) { return BARS.isEmpty() ? null : BARS.get(id); }

    static boolean isEmpty() { return BARS.isEmpty(); }

    static Iterator<Map.Entry<UUID, CustomBarState>> entries() { return BARS.entrySet().iterator(); }

    static void remove(UUID id) { BARS.remove(id); }

    // ================================================================ the preview
    /** Shows a bar of {@code style} with no boss behind it for twelve seconds: {@code hp} percent, {@code phase} (0 = from the health) of {@code phases}. */
    public static void preview(String style, int hp, int phase, int phases) {
        if (!BarStyleIds.known(style)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int p = Math.max(1, Math.min(5, phases));
        List<Integer> th = p >= 3 ? List.of(66, 33) : p == 2 ? List.of(50) : List.of();
        int ph = phase > 0 ? Math.min(phase, th.size() + 1) : 1 + (hp <= 33 && th.size() > 1 ? 2 : hp <= 66 && th.size() > 0 ? 1 : 0);
        int flags = BossBarState.PREVIEW;
        BossBarState s = new BossBarState(PREVIEW_ID, -1, "@nusmp.bossbar.preview.name", style, "@nusmp.bossbar.preview.title", "@nusmp.bossbar.preview.phase", ph, th.size() + 1, th, 30, flags,
                "falling_stars", 120, BossBarState.LAIR_WARNING_TICKS, 3, "@nusmp.bossbar.preview.cast", 60, 3);
        long now = mc.level.getGameTime();
        CustomBarState st = new CustomBarState(PREVIEW_ID, s, now);
        st.preview = true;
        st.previewUntil = now + PREVIEW_TICKS;
        st.previewHp = Math.max(0, Math.min(100, hp)) / 100f;
        BARS.put(PREVIEW_ID, st);
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp").then(Commands.literal("bossbar")
                .then(Commands.literal("styles").executes(ctx -> {
                    say(ctx, Component.translatableWithFallback("nusmp.bossbar.cmd.styles", "Boss bar styles: %s", String.join(", ", BarStyleIds.ALL)));
                    return BarStyleIds.ALL.size();
                }))
                .then(Commands.literal("preview").then(Commands.argument("style", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(BarStyleIds.ALL, b))
                        .executes(ctx -> show(ctx, 100, 1))
                        .then(Commands.argument("hp", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> show(ctx, IntegerArgumentType.getInteger(ctx, "hp"), 0))
                                .then(Commands.argument("phase", IntegerArgumentType.integer(1, 5))
                                        .executes(ctx -> show(ctx, IntegerArgumentType.getInteger(ctx, "hp"), IntegerArgumentType.getInteger(ctx, "phase")))))))));
    }

    private static int show(CommandContext<CommandSourceStack> ctx, int hp, int phase) {
        String style = StringArgumentType.getString(ctx, "style");
        if (!BarStyleIds.known(style) || BarStyles.get(style) == null) {
            ctx.getSource().sendFailure(Component.translatableWithFallback("nusmp.bossbar.cmd.unknown", "No boss bar style '%s'. Try /nusmp bossbar styles.", style));
            return 0;
        }
        preview(style, hp, phase, 3);
        say(ctx, Component.translatableWithFallback("nusmp.bossbar.cmd.preview", "Showing the '%s' boss bar for twelve seconds.", style));
        return 1;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, Component c) {
        ctx.getSource().sendSuccess(() -> c.copy().withStyle(ChatFormatting.GOLD), false);
    }
}
